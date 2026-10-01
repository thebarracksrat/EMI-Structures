package com.silentvector.emistructures.analysis;

import com.mojang.logging.LogUtils;
import com.silentvector.emistructures.data.MaterialInfo;
import com.silentvector.emistructures.platform.StructurePickBlocks;
import com.silentvector.emistructures.data.GenerationDetail;
import com.silentvector.emistructures.data.StructureInfo;
import com.silentvector.emistructures.data.StructureSnapshot;
import com.silentvector.emistructures.data.StructureSnapshots;
import com.silentvector.emistructures.mixin.JigsawStructureAccessor;
import com.silentvector.emistructures.mixin.ListPoolElementAccessor;
import com.silentvector.emistructures.mixin.RuleProcessorAccessor;
import com.silentvector.emistructures.mixin.SinglePoolElementAccessor;
import com.silentvector.emistructures.mixin.StructureTemplateAccessor;
import com.silentvector.emistructures.mixin.StructureTemplatePoolAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public final class StructureAnalysisService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TOTAL_ASSEMBLY_SAMPLE_BUDGET = 100;

    private StructureAnalysisService() {
    }

    public static void onServerStarted(MinecraftServer server) {
        long started = System.nanoTime();
        StructureSnapshot snapshot = analyze(server);
        StructureSnapshots.publish(snapshot);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        LOGGER.info(
                "Analyzed {} data-driven structures and indexed {} generated blocks in {} ms",
                snapshot.structures().size(),
                snapshot.structuresByItem().size(),
                elapsedMillis);
    }

    public static void onServerStopping() {
        StructureSnapshots.clear();
    }

    static StructureSnapshot analyze(MinecraftServer server) {
        Registry<Structure> structureRegistry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Registry<StructureTemplatePool> poolRegistry = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Map<ResourceLocation, StructureInfo> structures = new LinkedHashMap<>();
        Map<ResourceLocation, List<ResourceLocation>> reverseIndex = new HashMap<>();
        long jigsawCount = structureRegistry.stream().filter(JigsawStructure.class::isInstance).count();
        int samplesPerStructure = (int) Math.max(1L, Math.min(10L, TOTAL_ASSEMBLY_SAMPLE_BUDGET / Math.max(1L, jigsawCount)));

        List<StructureInfo> analyzedStructures = structureRegistry.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(key -> key.location().toString())))
                .filter(entry -> entry.getValue() instanceof JigsawStructure)
                .toList()
                .parallelStream()
                .map(entry -> {
                    ResourceLocation structureId = entry.getKey().location();
                    Structure structure = entry.getValue();
                    JigsawStructure jigsaw = (JigsawStructure) structure;
                    try {
                        List<MaterialInfo> materials = analyzeJigsaw(
                                server, poolRegistry, jigsaw, samplesPerStructure);
                        if (materials.isEmpty()) {
                            return null;
                        }
                        return new StructureInfo(
                                structureId,
                                readableName(structureId),
                                readableNamespace(structureId.getNamespace()),
                                generationDetails(server, entry.getKey(), structure),
                                materials);
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Could not analyze structure {}: {}", structureId, exception.toString());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(info -> info.id().toString()))
                .toList();

        for (StructureInfo info : analyzedStructures) {
            structures.put(info.id(), info);
            for (MaterialInfo material : info.materials()) {
                reverseIndex.computeIfAbsent(material.itemId(), ignored -> new ArrayList<>()).add(info.id());
            }
        }

        reverseIndex.values().forEach(ids -> ids.sort(Comparator.comparing(ResourceLocation::toString)));
        return new StructureSnapshot(structures, reverseIndex);
    }

    private static List<MaterialInfo> analyzeJigsaw(
            MinecraftServer server,
            Registry<StructureTemplatePool> poolRegistry,
            JigsawStructure structure,
            int targetSamples) {
        StructureTemplatePool startPool = ((JigsawStructureAccessor) (Object) structure).emiStructures$getStartPool().value();
        ArrayDeque<StructureTemplatePool> pendingPools = new ArrayDeque<>();
        Set<StructureTemplatePool> visitedPools = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Set<ResourceLocation> visitedTemplates = new HashSet<>();
        Map<ResourceLocation, Integer> piecesByBlock = new HashMap<>();
        pendingPools.add(startPool);

        while (!pendingPools.isEmpty() && visitedPools.size() < 4096) {
            StructureTemplatePool pool = pendingPools.removeFirst();
            if (!visitedPools.add(pool)) {
                continue;
            }
            StructureTemplatePool fallback = pool.getFallback().value();
            if (fallback != pool) {
                pendingPools.addLast(fallback);
            }
            for (var weightedElement : ((StructureTemplatePoolAccessor) pool).emiStructures$getRawTemplates()) {
                analyzeElement(
                        server,
                        poolRegistry,
                        weightedElement.getFirst(),
                        pendingPools,
                        visitedTemplates,
                        piecesByBlock);
            }
        }

        Map<ResourceLocation, List<Long>> samples = sampleAssemblies(
                server, poolRegistry, structure, piecesByBlock.keySet(), targetSamples);
        return piecesByBlock.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> {
                    List<Long> values = samples.getOrDefault(entry.getKey(), List.of());
                    ResourceLocation itemId = displayItemId(server, entry.getKey());
                    if (itemId == null) {
                        return null;
                    }
                    if (values.isEmpty()) {
                        return new MaterialInfo(
                                itemId,
                                "—",
                                "Typical quantity is not yet available for this structure.");
                    }
                    long minimum = percentile(values, 0.10D);
                    long maximum = percentile(values, 0.90D);
                    if (maximum == 0L) {
                        return new MaterialInfo(
                                itemId,
                                "Rare",
                                "Possible in this structure, but not observed in the sampled assemblies.");
                    }
                    String quantity = compactRange(minimum, maximum);
                    String explanation = "Typical structure quantity: " + exactRange(minimum, maximum)
                            + ". Actual quantities can vary.";
                    return new MaterialInfo(itemId, quantity, explanation);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static ResourceLocation displayItemId(MinecraftServer server, ResourceLocation blockId) {
        var block = BuiltInRegistries.BLOCK.get(blockId);
        var item = block.asItem();
        if (item == net.minecraft.world.item.Items.AIR) {
            try {
                item = StructurePickBlocks.itemFor(block, server.overworld());
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return item == net.minecraft.world.item.Items.AIR ? null : BuiltInRegistries.ITEM.getKey(item);
    }

    private static Map<ResourceLocation, List<Long>> sampleAssemblies(
            MinecraftServer server,
            Registry<StructureTemplatePool> poolRegistry,
            JigsawStructure structure,
            Set<ResourceLocation> knownBlocks,
            int targetSamples) {
        Map<ResourceLocation, List<Long>> samples = new HashMap<>();
        knownBlocks.forEach(id -> samples.put(id, new ArrayList<>()));
        IdentityHashMap<StructurePoolElement, ElementSummary> elementCache = new IdentityHashMap<>();
        ServerLevel level = server.overworld();
        var chunks = level.getChunkSource();
        long baseSeed = level.getSeed() ^ structure.hashCode();
        JigsawStructureAccessor settings = (JigsawStructureAccessor) (Object) structure;
        int successful = 0;
        int maxAttempts = Math.max(targetSamples, targetSamples * 2);
        for (int attempt = 0; attempt < maxAttempts && successful < targetSamples; attempt++) {
            int x = Math.floorMod(attempt * 37 + 11, 401) - 200;
            int z = Math.floorMod(attempt * 101 + 29, 401) - 200;
            long sampleSeed = baseSeed + attempt * 0x9E3779B97F4A7C15L;
            ChunkPos chunkPos = new ChunkPos(x, z);
            Structure.GenerationContext context = new Structure.GenerationContext(
                    server.registryAccess(),
                    chunks.getGenerator(),
                    chunks.getGenerator().getBiomeSource(),
                    chunks.randomState(),
                    server.getStructureManager(),
                    sampleSeed,
                    chunkPos,
                    level,
                    ignored -> true);
            int startY = settings.emiStructures$getStartHeight().sample(
                    context.random(),
                    new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
            BlockPos startPos = new BlockPos(chunkPos.getMiddleBlockX(), startY, chunkPos.getMiddleBlockZ());
            var stub = JigsawPlacement.addPieces(
                    context,
                    settings.emiStructures$getStartPool(),
                    settings.emiStructures$getStartJigsawName(),
                    settings.emiStructures$getMaxDepth(),
                    startPos,
                    settings.emiStructures$getUseExpansionHack(),
                    settings.emiStructures$getProjectStartToHeightmap(),
                    settings.emiStructures$getMaxDistanceFromCenter());
            if (stub.isEmpty()) {
                continue;
            }
            Map<ResourceLocation, Long> totals = new HashMap<>();
            for (var piece : stub.get().getPiecesBuilder().build().pieces()) {
                if (!(piece instanceof PoolElementStructurePiece poolPiece)) {
                    continue;
                }
                ElementSummary summary = summarizeElement(server, poolRegistry, poolPiece.getElement(), elementCache);
                Set<ResourceLocation> ids = new HashSet<>(summary.minimum().keySet());
                ids.addAll(summary.maximum().keySet());
                for (ResourceLocation id : ids) {
                    long low = summary.minimum().getOrDefault(id, 0L);
                    long high = summary.maximum().getOrDefault(id, low);
                    totals.merge(id, (low + high) / 2L, Long::sum);
                }
            }
            for (ResourceLocation id : knownBlocks) {
                samples.get(id).add(totals.getOrDefault(id, 0L));
            }
            successful++;
        }
        return samples;
    }

    private static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = (int) Math.round((sorted.size() - 1) * percentile);
        return sorted.get(index);
    }

    private static ElementSummary summarizeElement(
            MinecraftServer server,
            Registry<StructureTemplatePool> poolRegistry,
            StructurePoolElement element,
            IdentityHashMap<StructurePoolElement, ElementSummary> cache) {
        ElementSummary cached = cache.get(element);
        if (cached != null) {
            return cached;
        }
        ElementSummary summary;
        if (element instanceof ListPoolElement listElement) {
            Map<ResourceLocation, Long> minimum = new HashMap<>();
            Map<ResourceLocation, Long> maximum = new HashMap<>();
            Map<StructureTemplatePool, Integer> connectors = new IdentityHashMap<>();
            for (StructurePoolElement child : ((ListPoolElementAccessor) listElement).emiStructures$getElements()) {
                ElementSummary childSummary = summarizeElement(server, poolRegistry, child, cache);
                childSummary.minimum().forEach((id, count) -> minimum.merge(id, count, StructureAnalysisService::saturatingAdd));
                childSummary.maximum().forEach((id, count) -> maximum.merge(id, count, StructureAnalysisService::saturatingAdd));
                childSummary.connectors().forEach((pool, count) -> connectors.merge(pool, count, Integer::sum));
            }
            summary = new ElementSummary(Map.copyOf(minimum), Map.copyOf(maximum), Map.copyOf(connectors));
        } else if (element instanceof SinglePoolElement singleElement) {
            var reference = ((SinglePoolElementAccessor) singleElement).emiStructures$getTemplate();
            StructureTemplate template = reference.map(server.getStructureManager()::getOrCreate, direct -> direct);
            summary = summarizeTemplate(poolRegistry, template);
        } else {
            summary = ElementSummary.EMPTY;
        }
        cache.put(element, summary);
        return summary;
    }

    private static ElementSummary summarizeTemplate(
            Registry<StructureTemplatePool> poolRegistry,
            StructureTemplate template) {
        List<Map<ResourceLocation, Long>> paletteCounts = new ArrayList<>();
        List<Map<StructureTemplatePool, Integer>> paletteConnectors = new ArrayList<>();
        for (StructureTemplate.Palette palette : ((StructureTemplateAccessor) template).emiStructures$getPalettes()) {
            Map<ResourceLocation, Long> counts = new HashMap<>();
            Map<StructureTemplatePool, Integer> connectors = new IdentityHashMap<>();
            for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
                if (blockInfo.state().is(Blocks.JIGSAW)) {
                    StructureTemplatePool target = targetPool(poolRegistry, blockInfo.nbt());
                    if (target != null) {
                        connectors.merge(target, 1, Integer::sum);
                    }
                } else if (!blockInfo.state().isAir() && !blockInfo.state().is(Blocks.STRUCTURE_BLOCK)) {
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockInfo.state().getBlock());
                    counts.merge(blockId, 1L, Long::sum);
                }
            }
            paletteCounts.add(counts);
            paletteConnectors.add(connectors);
        }
        if (paletteCounts.isEmpty()) {
            return ElementSummary.EMPTY;
        }
        Set<ResourceLocation> ids = new HashSet<>();
        paletteCounts.forEach(counts -> ids.addAll(counts.keySet()));
        Map<ResourceLocation, Long> minimum = new HashMap<>();
        Map<ResourceLocation, Long> maximum = new HashMap<>();
        for (ResourceLocation id : ids) {
            minimum.put(id, paletteCounts.stream().mapToLong(map -> map.getOrDefault(id, 0L)).min().orElse(0L));
            maximum.put(id, paletteCounts.stream().mapToLong(map -> map.getOrDefault(id, 0L)).max().orElse(0L));
        }
        Map<StructureTemplatePool, Integer> connectors = new IdentityHashMap<>();
        paletteConnectors.forEach(map -> map.forEach((pool, count) -> connectors.merge(pool, count, Math::max)));
        return new ElementSummary(Map.copyOf(minimum), Map.copyOf(maximum), Map.copyOf(connectors));
    }

    private static StructureTemplatePool targetPool(Registry<StructureTemplatePool> poolRegistry, CompoundTag nbt) {
        if (nbt == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("pool"));
        return id == null ? null : poolRegistry.get(id);
    }

    private static long saturatingAdd(long left, long right) {
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static String compactRange(long minimum, long maximum) {
        return minimum == maximum ? compact(minimum) : compact(minimum) + "–" + compact(maximum);
    }

    private static String exactRange(long minimum, long maximum) {
        return minimum == maximum
                ? String.format(Locale.ROOT, "%,d", minimum)
                : String.format(Locale.ROOT, "%,d–%,d", minimum, maximum);
    }

    private static String compact(long value) {
        if (value < 1_000L) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return compactUnit(value, 1_000D, "k");
        }
        if (value < 1_000_000_000L) {
            return compactUnit(value, 1_000_000D, "m");
        }
        return compactUnit(value, 1_000_000_000D, "b");
    }

    private static String compactUnit(long value, double divisor, String suffix) {
        double scaled = value / divisor;
        String formatted = scaled >= 10D
                ? String.format(Locale.ROOT, "%.0f", scaled)
                : String.format(Locale.ROOT, "%.1f", scaled);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + suffix;
    }

    private record ElementSummary(
            Map<ResourceLocation, Long> minimum,
            Map<ResourceLocation, Long> maximum,
            Map<StructureTemplatePool, Integer> connectors) {
        private static final ElementSummary EMPTY = new ElementSummary(Map.of(), Map.of(), Map.of());
    }

    private static void analyzeElement(
            MinecraftServer server,
            Registry<StructureTemplatePool> poolRegistry,
            StructurePoolElement element,
            ArrayDeque<StructureTemplatePool> pendingPools,
            Set<ResourceLocation> visitedTemplates,
            Map<ResourceLocation, Integer> piecesByBlock) {
        if (element instanceof ListPoolElement listElement) {
            for (StructurePoolElement child : ((ListPoolElementAccessor) listElement).emiStructures$getElements()) {
                analyzeElement(server, poolRegistry, child, pendingPools, visitedTemplates, piecesByBlock);
            }
            return;
        }
        if (!(element instanceof SinglePoolElement singleElement)) {
            return;
        }

        var templateReference = ((SinglePoolElementAccessor) singleElement).emiStructures$getTemplate();
        StructureTemplate template = templateReference.map(
                id -> {
                    if (!visitedTemplates.add(id)) {
                        return null;
                    }
                    return server.getStructureManager().getOrCreate(id);
                },
                direct -> direct);
        if (template == null) {
            return;
        }

        Set<ResourceLocation> blocksInPiece = new HashSet<>();
        for (StructureTemplate.Palette palette : ((StructureTemplateAccessor) template).emiStructures$getPalettes()) {
            for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
                if (blockInfo.state().isAir() || blockInfo.state().is(Blocks.JIGSAW) || blockInfo.state().is(Blocks.STRUCTURE_BLOCK)) {
                    if (blockInfo.state().is(Blocks.JIGSAW)) {
                        enqueueTargetPool(poolRegistry, blockInfo.nbt(), pendingPools);
                    }
                    continue;
                }
                blocksInPiece.add(BuiltInRegistries.BLOCK.getKey(blockInfo.state().getBlock()));
            }
        }
        collectProcessorOutputs(singleElement, blocksInPiece);
        blocksInPiece.forEach(blockId -> piecesByBlock.merge(blockId, 1, Integer::sum));
    }

    private static void collectProcessorOutputs(
            SinglePoolElement element,
            Set<ResourceLocation> blocksInPiece) {
        for (var processor : ((SinglePoolElementAccessor) element).emiStructures$getProcessors().value().list()) {
            if (!(processor instanceof RuleProcessor ruleProcessor)) {
                continue;
            }
            for (var rule : ((RuleProcessorAccessor) ruleProcessor).emiStructures$getRules()) {
                var output = rule.getOutputState();
                if (!output.isAir() && !output.is(Blocks.JIGSAW) && !output.is(Blocks.STRUCTURE_BLOCK)) {
                    blocksInPiece.add(BuiltInRegistries.BLOCK.getKey(output.getBlock()));
                }
            }
        }
    }

    private static void enqueueTargetPool(
            Registry<StructureTemplatePool> poolRegistry,
            CompoundTag nbt,
            ArrayDeque<StructureTemplatePool> pendingPools) {
        if (nbt == null) {
            return;
        }
        ResourceLocation poolId = ResourceLocation.tryParse(nbt.getString("pool"));
        if (poolId == null) {
            return;
        }
        StructureTemplatePool target = poolRegistry.get(poolId);
        if (target != null) {
            pendingPools.addLast(target);
        }
    }

    private static String readableName(ResourceLocation id) {
        String path = id.getPath().replace('/', ' ').replace('_', ' ');
        if (path.startsWith("village ")) {
            path = path.substring("village ".length()) + " village";
        }
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (char character : path.toCharArray()) {
            if (capitalize && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
            if (character == ' ') {
                capitalize = true;
            }
        }
        return result.toString();
    }

    private static List<GenerationDetail> generationDetails(
            MinecraftServer server,
            net.minecraft.resources.ResourceKey<Structure> structureKey,
            Structure structure) {
        var structureHolder = server.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getHolderOrThrow(structureKey);
        List<String> dimensions = new ArrayList<>();
        List<String> placements = new ArrayList<>();
        for (var level : server.getAllLevels()) {
            List<StructurePlacement> levelPlacements = level.getChunkSource()
                    .getGeneratorState()
                    .getPlacementsForStructure(structureHolder);
            if (levelPlacements.isEmpty()) {
                continue;
            }
            dimensions.add(readableName(level.dimension().location()));
            levelPlacements.stream().map(StructureAnalysisService::placementSummary).forEach(placements::add);
        }

        List<String> biomeNames = structure.biomes().stream()
                .map(holder -> holder.unwrapKey().map(key -> readableName(key.location())).orElse("Unknown"))
                .distinct()
                .sorted()
                .toList();
        String biomeSummary = biomeNames.isEmpty() ? "Unknown" : String.join(", ", biomeNames);
        String dimensionSummary = dimensions.isEmpty()
                ? "Not active in this world"
                : String.join(", ", new LinkedHashSet<>(dimensions));
        String placementSummary = placements.isEmpty()
                ? "No active structure set"
                : String.join("; ", new LinkedHashSet<>(placements));
        String generationSummary = titleCase(structure.step().toString())
                + " | " + titleCase(structure.terrainAdaptation().toString()) + " terrain";
        return List.of(
                new GenerationDetail("Dimension", dimensionSummary),
                new GenerationDetail("Biomes", biomeSummary),
                new GenerationDetail("Placement", placementSummary),
                new GenerationDetail("Generation", generationSummary));
    }

    private static String placementSummary(StructurePlacement placement) {
        if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
            return "Random spread\nSpacing " + randomSpread.spacing() + " | Separation " + randomSpread.separation();
        }
        if (placement instanceof ConcentricRingsStructurePlacement rings) {
            return "Concentric rings\nDistance " + rings.distance() + " | Spread " + rings.spread() + " | Count " + rings.count();
        }
        return titleCase(placement.type().toString());
    }

    private static String titleCase(String value) {
        return readableName(new ResourceLocation("emi_structures", value.toLowerCase(Locale.ROOT)));
    }

    private static String readableNamespace(String namespace) {
        if (namespace.equals("minecraft")) {
            return "Minecraft";
        }
        return readableName(new ResourceLocation(namespace, namespace)).replaceFirst("^.*: ", "");
    }
}

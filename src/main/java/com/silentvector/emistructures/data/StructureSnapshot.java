package com.silentvector.emistructures.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record StructureSnapshot(
        Map<ResourceLocation, StructureInfo> structures,
        Map<ResourceLocation, List<ResourceLocation>> structuresByItem) {

    public static final StructureSnapshot EMPTY = new StructureSnapshot(Map.of(), Map.of());

    public StructureSnapshot {
        structures = Map.copyOf(structures);
        structuresByItem = structuresByItem.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }
}

package com.silentvector.emistructures.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Optional;

@Mixin(JigsawStructure.class)
public interface JigsawStructureAccessor {
    @Accessor("startPool")
    Holder<StructureTemplatePool> emiStructures$getStartPool();

    @Accessor("maxDepth")
    int emiStructures$getMaxDepth();

    @Accessor("startJigsawName")
    Optional<ResourceLocation> emiStructures$getStartJigsawName();

    @Accessor("useExpansionHack")
    boolean emiStructures$getUseExpansionHack();

    @Accessor("projectStartToHeightmap")
    Optional<Heightmap.Types> emiStructures$getProjectStartToHeightmap();

    @Accessor("maxDistanceFromCenter")
    int emiStructures$getMaxDistanceFromCenter();

    @Accessor("poolAliases")
    List<PoolAliasBinding> emiStructures$getPoolAliases();

    @Accessor("dimensionPadding")
    DimensionPadding emiStructures$getDimensionPadding();

    @Accessor("liquidSettings")
    LiquidSettings emiStructures$getLiquidSettings();
}

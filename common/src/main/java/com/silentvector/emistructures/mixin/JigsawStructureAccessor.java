package com.silentvector.emistructures.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(JigsawStructure.class)
public interface JigsawStructureAccessor {
    @Accessor("startPool")
    Holder<StructureTemplatePool> emiStructures$getStartPool();

    @Accessor("maxDepth")
    int emiStructures$getMaxDepth();

    @Accessor("startJigsawName")
    Optional<ResourceLocation> emiStructures$getStartJigsawName();

    @Accessor("startHeight")
    HeightProvider emiStructures$getStartHeight();

    @Accessor("useExpansionHack")
    boolean emiStructures$getUseExpansionHack();

    @Accessor("projectStartToHeightmap")
    Optional<Heightmap.Types> emiStructures$getProjectStartToHeightmap();

    @Accessor("maxDistanceFromCenter")
    int emiStructures$getMaxDistanceFromCenter();
}

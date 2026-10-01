package com.silentvector.emistructures.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StructureInfo(
        ResourceLocation id,
        String displayName,
        String sourceMod,
        List<GenerationDetail> generationDetails,
        List<MaterialInfo> materials) {

    public StructureInfo {
        generationDetails = List.copyOf(generationDetails);
        materials = List.copyOf(materials);
    }
}

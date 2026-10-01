package com.silentvector.emistructures.compat.emi;

import com.silentvector.emistructures.data.StructureSnapshots;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;

@EmiEntrypoint
public final class EmiStructuresPlugin implements EmiPlugin {
    public static final EmiRecipeCategory STRUCTURE_GENERATION = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("emi_structures", "structure_generation"),
            new EmiTexture(
                    ResourceLocation.fromNamespaceAndPath("emi_structures", "textures/gui/structure.png"),
                    0, 0, 16, 16, 16, 16, 16, 16));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(STRUCTURE_GENERATION);
        var snapshot = StructureSnapshots.current();
        snapshot.structures().values().stream()
                .map(StructureInfoRecipe::new)
                .forEach(registry::addRecipe);
    }
}

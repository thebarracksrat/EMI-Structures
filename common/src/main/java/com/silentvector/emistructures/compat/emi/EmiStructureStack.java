package com.silentvector.emistructures.compat.emi;

import com.silentvector.emistructures.data.StructureInfo;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.render.EmiTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

final class EmiStructureStack extends EmiStack {
    private static final EmiTexture ICON = new EmiTexture(
            ResourceLocation.fromNamespaceAndPath("emi_structures", "textures/gui/structure.png"),
            0, 0, 16, 16, 16, 16, 16, 16);

    private final StructureInfo structure;

    EmiStructureStack(StructureInfo structure) {
        this.structure = structure;
    }

    StructureInfo structure() {
        return structure;
    }

    @Override
    public EmiStack copy() {
        return new EmiStructureStack(structure);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public DataComponentPatch getComponentChanges() {
        return DataComponentPatch.EMPTY;
    }

    @Override
    public Object getKey() {
        return structure.id();
    }

    @Override
    public ResourceLocation getId() {
        return structure.id();
    }

    @Override
    public List<Component> getTooltipText() {
        java.util.ArrayList<Component> tooltip = new java.util.ArrayList<>();
        tooltip.add(getName());
        tooltip.add(Component.translatable("emi_structures.structure.source", structure.sourceMod()));
        structure.generationDetails().forEach(detail -> tooltip.add(Component.literal(detail.label() + ": " + detail.value())));
        tooltip.add(Component.literal(structure.id().toString()));
        return List.copyOf(tooltip);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        return getTooltipText().stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
    }

    @Override
    public Component getName() {
        return Component.literal(structure.displayName());
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        ICON.render(draw, x, y, delta);
    }
}

package com.silentvector.emistructures.compat.emi;

import com.silentvector.emistructures.data.MaterialInfo;
import com.silentvector.emistructures.data.StructureInfo;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class StructureInfoRecipe implements EmiRecipe {
    private static final int WIDTH = 190;
    private static final int HEIGHT = 142;
    private static final int DETAIL_VALUE_X = 58;
    private static final int COLUMNS = 4;
    private static final int PAGE_SIZE = 8;
    private static final int CELL_WIDTH = WIDTH / COLUMNS;

    private final StructureInfo structure;
    private final EmiStructureStack structureStack;
    private final List<MaterialInfo> materials;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    StructureInfoRecipe(StructureInfo structure) {
        this.structure = structure;
        this.structureStack = new EmiStructureStack(structure);
        this.materials = normalizeMaterials(structure.materials());
        this.inputs = materials.stream().map(material -> (EmiIngredient) stack(material)).toList();
        this.outputs = materials.stream().map(StructureInfoRecipe::stack).toList();
    }

    private static EmiStack stack(MaterialInfo material) {
        return EmiStack.of(BuiltInRegistries.ITEM.get(material.itemId()));
    }

    @Override public EmiRecipeCategory getCategory() { return EmiStructuresPlugin.STRUCTURE_GENERATION; }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation("emi_structures",
                "/structure/" + structure.id().getNamespace() + "/" + structure.id().getPath());
    }

    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public int getDisplayWidth() { return WIDTH; }
    @Override public int getDisplayHeight() { return HEIGHT; }
    @Override public boolean supportsRecipeTree() { return false; }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        boolean paginated = materials.size() > PAGE_SIZE;
        int nameWidth = paginated ? WIDTH - 74 : WIDTH - 22;
        widgets.addSlot(structureStack, 0, 0).drawBack(false).recipeContext(this);
        widgets.addText(Component.literal(fitToWidth(structure.displayName(), nameWidth)), 22, 2, 0xFF404040, false);
        widgets.addTooltipText(List.of(Component.literal(structure.displayName())), 22, 2, nameWidth, 10);
        String source = Component.translatable("emi_structures.structure.source", structure.sourceMod()).getString();
        widgets.addText(Component.literal(fitToWidth(source, nameWidth)), 22, 12, 0xFF777777, false);
        widgets.addTooltipText(List.of(Component.literal(source)), 22, 12, nameWidth, 10);

        int y = 25;
        for (int index = 0; index < Math.min(3, structure.generationDetails().size()); index++) {
            var detail = structure.generationDetails().get(index);
            String[] lines = detail.value().split("\\n", -1);
            widgets.addText(Component.literal(detail.label()), 0, y, 0xFF777777, false);
            for (int line = 0; line < lines.length; line++) {
                widgets.addText(Component.literal(fitToWidth(lines[line], WIDTH - DETAIL_VALUE_X)),
                        DETAIL_VALUE_X, y + line * 10, 0xFF404040, false);
            }
            widgets.addTooltipText(wrapTooltip(detail.label(), detail.value().replace('\n', ' ')),
                    DETAIL_VALUE_X, y, WIDTH - DETAIL_VALUE_X, lines.length * 10);
            y += lines.length * 10;
        }

        widgets.addText(Component.translatable("emi_structures.materials"), 0, 70, 0xFF404040, false);
        widgets.addText(Component.translatable("emi_structures.estimated_range"), WIDTH, 70, 0xFF777777, false)
                .horizontalAlign(TextWidget.Alignment.END);

        PageManager manager = new PageManager();
        if (paginated) {
            widgets.add(new PageIndicator(manager, WIDTH - 52, 3));
            widgets.addButton(WIDTH - 28, 2, 12, 12, 0, 0, () -> true,
                    (mouseX, mouseY, button) -> manager.scroll(-1));
            widgets.addButton(WIDTH - 14, 2, 12, 12, 12, 0, () -> true,
                    (mouseX, mouseY, button) -> manager.scroll(1));
        }
        for (int offset = 0; offset < Math.min(PAGE_SIZE, materials.size()); offset++) {
            int cellX = (offset % COLUMNS) * CELL_WIDTH;
            int slotX = cellX + (CELL_WIDTH - 18) / 2;
            int slotY = 82 + (offset / COLUMNS) * 30;
            widgets.add(new PageMaterialSlot(manager, offset, slotX, slotY, cellX + CELL_WIDTH / 2));
        }
    }

    private final class PageManager {
        private int currentPage;
        private int pageCount() { return Math.max(1, (materials.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
        private void scroll(int delta) { currentPage = Math.floorMod(currentPage + delta, pageCount()); }
        private MaterialInfo get(int offset) {
            int index = currentPage * PAGE_SIZE + offset;
            return index < materials.size() ? materials.get(index) : null;
        }
    }

    private final class PageMaterialSlot extends SlotWidget {
        private final PageManager manager;
        private final int offset;
        private final int textCenterX;

        private PageMaterialSlot(PageManager manager, int offset, int x, int y, int textCenterX) {
            super(EmiStack.EMPTY, x, y);
            this.manager = manager;
            this.offset = offset;
            this.textCenterX = textCenterX;
        }

        @Override public EmiIngredient getStack() {
            MaterialInfo material = manager.get(offset);
            return material == null ? EmiStack.EMPTY : stack(material);
        }
        @Override public EmiRecipe getRecipe() { return StructureInfoRecipe.this; }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            MaterialInfo material = manager.get(offset);
            if (material == null) return;
            super.render(graphics, mouseX, mouseY, delta);
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, material.quantity(), textCenterX - font.width(material.quantity()) / 2,
                    y + 20, 0xFF404040, false);
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            MaterialInfo material = manager.get(offset);
            if (material == null) return List.of();
            List<ClientTooltipComponent> tooltip = new ArrayList<>(super.getTooltip(mouseX, mouseY));
            tooltip.add(ClientTooltipComponent.create(Component.literal(material.explanation()).getVisualOrderText()));
            return tooltip;
        }
    }

    private final class PageIndicator extends Widget {
        private final PageManager manager;
        private final Bounds bounds;
        private PageIndicator(PageManager manager, int x, int y) {
            this.manager = manager;
            this.bounds = new Bounds(x, y, 24, 10);
        }
        @Override public Bounds getBounds() { return bounds; }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            String value = (manager.currentPage + 1) + "/" + manager.pageCount();
            graphics.drawString(Minecraft.getInstance().font, value, bounds.x(), bounds.y(), 0xFF777777, false);
        }
    }

    private static List<MaterialInfo> normalizeMaterials(List<MaterialInfo> materials) {
        LinkedHashMap<Item, MaterialInfo> byItem = new LinkedHashMap<>();
        for (MaterialInfo material : materials) {
            Item item = BuiltInRegistries.ITEM.get(material.itemId());
            if (item != Items.AIR) byItem.putIfAbsent(item, material);
        }
        return List.copyOf(byItem.values());
    }

    private static String fitToWidth(String value, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= width) return value;
        String ellipsis = "...";
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    private static List<Component> wrapTooltip(String label, String value) {
        List<Component> lines = new ArrayList<>();
        String remaining = label + ": " + value;
        while (remaining.length() > 52) {
            int split = remaining.lastIndexOf(' ', 52);
            if (split <= 0) split = 52;
            lines.add(Component.literal(remaining.substring(0, split)));
            remaining = remaining.substring(split).stripLeading();
        }
        if (!remaining.isEmpty()) lines.add(Component.literal(remaining));
        return lines;
    }
}

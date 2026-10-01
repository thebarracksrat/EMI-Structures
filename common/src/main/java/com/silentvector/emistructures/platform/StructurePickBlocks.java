package com.silentvector.emistructures.platform;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

import java.util.ServiceLoader;

public final class StructurePickBlocks {
    private static final StructurePickBlock PICK = ServiceLoader
            .load(StructurePickBlock.class, StructurePickBlocks.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No structure pick-block helper is registered."));

    private StructurePickBlocks() {
    }

    public static Item itemFor(Block block, LevelReader level) {
        return PICK.itemFor(block, level);
    }
}

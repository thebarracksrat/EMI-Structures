package com.silentvector.emistructures.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public final class FabricStructurePickBlock implements StructurePickBlock {
    @Override
    public Item itemFor(Block block, LevelReader level) {
        var stack = block.getCloneItemStack(level, BlockPos.ZERO, block.defaultBlockState());
        return stack.isEmpty() ? Items.AIR : stack.getItem();
    }
}

package com.silentvector.emistructures.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IBlockExtension;

public final class NeoForgeStructurePickBlock implements StructurePickBlock {
    @Override
    public Item itemFor(Block block, LevelReader level) {
        BlockPos pos = BlockPos.ZERO;
        BlockState state = block.defaultBlockState();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var stack = ((IBlockExtension) block).getCloneItemStack(state, hit, level, pos, null);
        return stack.isEmpty() ? Items.AIR : stack.getItem();
    }
}

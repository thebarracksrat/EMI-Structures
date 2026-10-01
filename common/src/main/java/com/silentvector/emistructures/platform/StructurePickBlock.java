package com.silentvector.emistructures.platform;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

/**
 * Loader-specific pick-block lookup. NeoForge mods override a different method than vanilla.
 */
public interface StructurePickBlock {
    Item itemFor(Block block, LevelReader level);
}

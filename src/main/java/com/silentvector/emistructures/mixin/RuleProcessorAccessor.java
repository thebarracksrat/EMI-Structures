package com.silentvector.emistructures.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RuleProcessor.class)
public interface RuleProcessorAccessor {
    @Accessor("rules")
    ImmutableList<ProcessorRule> emiStructures$getRules();
}

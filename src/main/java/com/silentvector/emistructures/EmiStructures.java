package com.silentvector.emistructures;

import com.silentvector.emistructures.analysis.StructureAnalysisService;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(EmiStructures.MOD_ID)
public final class EmiStructures {
    public static final String MOD_ID = "emi_structures";

    public EmiStructures() {
        NeoForge.EVENT_BUS.addListener(StructureAnalysisService::onServerStarted);
        NeoForge.EVENT_BUS.addListener(StructureAnalysisService::onServerStopping);
    }
}

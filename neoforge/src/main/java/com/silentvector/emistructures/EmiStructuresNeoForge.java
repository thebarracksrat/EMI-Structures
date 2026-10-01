package com.silentvector.emistructures;

import com.silentvector.emistructures.analysis.StructureAnalysisService;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(EmiStructures.MOD_ID)
public final class EmiStructuresNeoForge {
    public EmiStructuresNeoForge() {
        NeoForge.EVENT_BUS.addListener(EmiStructuresNeoForge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(EmiStructuresNeoForge::onServerStopping);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        StructureAnalysisService.onServerStarted(event.getServer());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        StructureAnalysisService.onServerStopping();
    }
}

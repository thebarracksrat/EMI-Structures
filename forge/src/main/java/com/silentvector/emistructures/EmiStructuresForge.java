package com.silentvector.emistructures;

import com.silentvector.emistructures.analysis.StructureAnalysisService;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EmiStructures.MOD_ID)
public final class EmiStructuresForge {
    public EmiStructuresForge() {
        MinecraftForge.EVENT_BUS.addListener(EmiStructuresForge::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(EmiStructuresForge::onServerStopping);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        StructureAnalysisService.onServerStarted(event.getServer());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        StructureAnalysisService.onServerStopping();
    }
}

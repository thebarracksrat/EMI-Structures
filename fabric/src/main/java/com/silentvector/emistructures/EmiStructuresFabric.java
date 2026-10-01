package com.silentvector.emistructures;

import com.silentvector.emistructures.analysis.StructureAnalysisService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class EmiStructuresFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(StructureAnalysisService::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> StructureAnalysisService.onServerStopping());
    }
}

package com.itmecasey.barelypowered.neoforge;

import com.itmecasey.barelypowered.compat.ShotTour;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only wiring, in its own class so a dedicated server never loads the client types it names. */
final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {
    }

    static void install() {
        // Development only: the screenshot tour runs when BARELYPOWERED_SHOTS is set.
        if (System.getenv("BARELYPOWERED_SHOTS") != null) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }
}

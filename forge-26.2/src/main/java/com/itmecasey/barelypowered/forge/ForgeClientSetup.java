package com.itmecasey.barelypowered.forge;

import com.itmecasey.barelypowered.compat.ShotTour;
import net.minecraftforge.event.TickEvent;

/** Client-only wiring, in its own class so a dedicated server never loads the client types it names. */
final class ForgeClientSetup {
    private ForgeClientSetup() {
    }

    static void install() {
        // Development only: the screenshot tour runs when BARELYPOWERED_SHOTS is set.
        if (System.getenv("BARELYPOWERED_SHOTS") != null) {
            TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }
}

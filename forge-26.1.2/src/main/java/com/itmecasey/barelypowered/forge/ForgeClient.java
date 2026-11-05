package com.itmecasey.barelypowered.forge;

import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.client.WindHeadRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only setup: the renderer that turns the wind turbine rotors. The event only fires on a client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void install() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.WIND_HEAD, WindHeadRenderer::new);
    }
}

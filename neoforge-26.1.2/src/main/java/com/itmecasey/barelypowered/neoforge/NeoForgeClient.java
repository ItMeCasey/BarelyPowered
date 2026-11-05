package com.itmecasey.barelypowered.neoforge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.client.WindHeadRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup: the renderer that turns the wind turbine rotors. Never loaded on a dedicated server. */
@EventBusSubscriber(modid = BarelyPowered.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.WIND_HEAD, WindHeadRenderer::new);
    }
}

package com.itmecasey.barelypowered.forge;

import com.itmecasey.barelypowered.BarelyPowered;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.client.WindHeadRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only setup: the renderer that turns the wind turbine rotors. Never loaded on a dedicated server. */
@Mod.EventBusSubscriber(modid = BarelyPowered.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClient {
    private ForgeClient() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.WIND_HEAD, WindHeadRenderer::new);
    }
}

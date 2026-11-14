package com.itmecasey.barelycables.neoforge;

import com.itmecasey.barelycables.BarelyCables;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only: the in-game config screen (Mods, then the mod, then Config). Never loaded on a dedicated server. */
@Mod(value = BarelyCables.MODID, dist = Dist.CLIENT)
public final class NeoForgeConfigScreen {
    public NeoForgeConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}

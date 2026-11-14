package com.itmecasey.barelycables.forge;

import com.itmecasey.barelycables.BarelyCables;
import com.itmecasey.barelycables.CableSettings;
import com.itmecasey.barelycables.config.ConfigSchema;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** The mod's config file (config/<modid>-common.toml), built from the one list of settings, and the push of its values into the mod. */
final class ForgeConfig {
    private static ForgeConfigSpec spec;
    private static Map<String, Supplier<Object>> readers;

    private ForgeConfig() {
    }

    static void register(FMLJavaModLoadingContext context) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        readers = ConfigSchema.define(CableSettings.defs(), new ConfigSchema.Adapter() {
            @Override
            public Supplier<Object> integer(String key, int fallback, int min, int max, String comment) {
                ForgeConfigSpec.IntValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }

            @Override
            public Supplier<Object> decimal(String key, double fallback, double min, double max, String comment) {
                ForgeConfigSpec.DoubleValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }

            @Override
            public Supplier<Object> bool(String key, boolean fallback, String comment) {
                ForgeConfigSpec.BooleanValue value = builder.comment(comment).define(key, fallback);
                return value::get;
            }
        });
        spec = builder.build();
        context.registerConfig(ModConfig.Type.COMMON, spec, BarelyCables.CONFIG_FILE);
        context.getModEventBus().addListener(ForgeConfig::loaded);
        context.getModEventBus().addListener(ForgeConfig::reloaded);
    }

    private static void loaded(ModConfigEvent.Loading event) {
        push(event.getConfig());
    }

    private static void reloaded(ModConfigEvent.Reloading event) {
        push(event.getConfig());
    }

    private static void push(ModConfig config) {
        if (config.getSpec() == spec) {
            CableSettings.apply(ConfigSchema.read(readers));
        }
    }
}

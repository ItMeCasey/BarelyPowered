package com.itmecasey.barelycables.neoforge;

import com.itmecasey.barelycables.BarelyCables;
import com.itmecasey.barelycables.CableSettings;
import com.itmecasey.barelycables.config.ConfigSchema;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The mod's config file (config/<modid>-common.toml), built from the one list of settings, and the push of its values into the mod. */
final class NeoForgeConfig {
    private static ModConfigSpec spec;
    private static Map<String, Supplier<Object>> readers;

    private NeoForgeConfig() {
    }

    static void register(IEventBus modBus, ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        readers = ConfigSchema.define(CableSettings.defs(), new ConfigSchema.Adapter() {
            @Override
            public Supplier<Object> integer(String key, int fallback, int min, int max, String comment) {
                ModConfigSpec.IntValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }

            @Override
            public Supplier<Object> decimal(String key, double fallback, double min, double max, String comment) {
                ModConfigSpec.DoubleValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }

            @Override
            public Supplier<Object> bool(String key, boolean fallback, String comment) {
                ModConfigSpec.BooleanValue value = builder.comment(comment).define(key, fallback);
                return value::get;
            }
        });
        spec = builder.build();
        container.registerConfig(ModConfig.Type.LOCAL, spec, BarelyCables.CONFIG_FILE);
        modBus.addListener(NeoForgeConfig::loaded);
        modBus.addListener(NeoForgeConfig::reloaded);
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

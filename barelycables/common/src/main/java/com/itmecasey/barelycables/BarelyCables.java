package com.itmecasey.barelycables;

import com.itmecasey.barelycables.config.ConfigMigration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** Entry point shared by every loader layer of BarelyCables. The layers call {@link #init} before they register the config. */
public final class BarelyCables {
    public static final String MODID = "barelycables";
    /** The config file the loaders register, in the loader's config folder. */
    public static final String CONFIG_FILE = MODID + "-common.toml";
    /** The hand-written file that versions up to 2026.11.3 used. After a verified import it is moved to a .back file. */
    public static final String OLD_CONFIG_FILE = MODID + ".properties";

    private static final Logger LOG = Logger.getLogger(MODID);

    private BarelyCables() {
    }

    /** The mod version, from the file the build writes into the jar ("unknown" in unit tests). */
    public static String version() {
        try (InputStream in = BarelyCables.class.getResourceAsStream("/" + MODID + ".version")) {
            return in == null ? "unknown" : new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException | RuntimeException e) {
            return "unknown";
        }
    }

    public static void init(Supplier<Path> configDir) {
        Path dir = configDir.get();
        ConfigMigration.migrate(dir.resolve(OLD_CONFIG_FILE), dir.resolve(CONFIG_FILE), CableSettings.defs(), CableSettings.oldListKeys(),
                CableSettings.retiredDefaults(), "BarelyCables " + version(), message -> LOG.info("BarelyCables: " + message));
    }
}

package com.itmecasey.barelycables;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer of BarelyCables. */
public final class BarelyCables {
    public static final String MODID = "barelycables";

    private BarelyCables() {
    }

    public static void init(Supplier<Path> configDir) {
        CableSettings.loadFile(configDir.get().resolve(MODID + ".properties"));
    }
}

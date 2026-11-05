package com.itmecasey.barelypowered;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class BarelyPowered {
    public static final String MODID = "barelypowered";

    private BarelyPowered() {
    }

    public static void init(Supplier<Path> configDir) {
        Settings.loadFile(configDir.get().resolve(MODID + ".properties"));
    }
}

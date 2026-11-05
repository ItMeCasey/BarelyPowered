package com.itmecasey.barelypowered;

/** Development switches. Kept apart from the content classes so reading them does not load any Minecraft classes. */
public final class Dev {
    /** True in dev runs (the Gradle run tasks set the property); adds the test energy sink block. */
    public static final boolean ENABLED = System.getProperty("barelypowered.dev") != null;

    private Dev() {
    }
}

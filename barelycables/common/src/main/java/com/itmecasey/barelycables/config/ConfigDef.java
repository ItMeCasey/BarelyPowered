package com.itmecasey.barelycables.config;

/** One setting: its key (dotted, the last part is the entry name and the rest is the TOML table), type, default, range and comment. */
public record ConfigDef(String key, Type type, String fallback, double min, double max, String comment) {
    public enum Type {
        INT, DOUBLE, BOOL
    }

    /** The TOML table the entry lives in, for example {@code combustion.coal}. */
    public String table() {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "" : key.substring(0, dot);
    }

    /** The entry name inside its table. */
    public String name() {
        return key.substring(key.lastIndexOf('.') + 1);
    }

    /** The text after "Range:" in the config file comment, or null for a boolean. */
    public String rangeText() {
        return switch (type) {
            case INT -> max >= Integer.MAX_VALUE ? "> " + (long) min : (long) min + " ~ " + (long) max;
            case DOUBLE -> min + " ~ " + max;
            case BOOL -> null;
        };
    }
}

package com.itmecasey.barelypowered.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes a config TOML in the shape the loaders read: one table per group of entries, a comment, the default and the range above each entry. */
public final class TomlConfig {
    private TomlConfig() {
    }

    /** One table of the file: its own entries first, then its sub-tables, as the loaders write them. */
    private static final class Node {
        final List<ConfigDef> entries = new ArrayList<>();
        final Map<String, Node> children = new LinkedHashMap<>();
    }

    /** The file text for the given values (key to text); a missing key writes the default. */
    public static String render(List<ConfigDef> defs, Map<String, String> values) {
        Node root = new Node();
        for (ConfigDef def : ConfigSchema.grouped(defs)) {
            Node node = root;
            for (String part : def.table().split("\\.")) {
                node = node.children.computeIfAbsent(part, k -> new Node());
            }
            node.entries.add(def);
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Node> child : root.children.entrySet()) {
            write(out, child.getValue(), child.getKey(), 1, values);
        }
        return out.toString();
    }

    // The same shape NeoForge writes itself (a table is indented by the dots in its name, entries one tab deeper, then the
    // comment, the default and the range above each entry), so the loader finds nothing to correct.
    private static void write(StringBuilder out, Node node, String path, int depth, Map<String, String> values) {
        if (!node.entries.isEmpty()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append("\t".repeat(depth - 1)).append('[').append(path).append("]\n");
            String indent = "\t".repeat(depth);
            for (ConfigDef def : node.entries) {
                for (String line : def.comment().split("\n")) {
                    out.append(indent).append('#').append(line).append('\n');
                }
                if (def.type() != ConfigDef.Type.BOOL) {
                    out.append(indent).append("# Default: ").append(def.fallback()).append('\n');
                    out.append(indent).append("# Range: ").append(def.rangeText()).append('\n');
                }
                out.append(indent).append(def.name()).append(" = ").append(values.getOrDefault(def.key(), def.fallback())).append('\n');
            }
        }
        for (Map.Entry<String, Node> child : node.children.entrySet()) {
            write(out, child.getValue(), path + "." + child.getKey(), depth + 1, values);
        }
    }

    /** Reads text written by {@link #render}: table and entry name joined with a dot, to the value text. */
    public static Map<String, String> parse(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        String table = "";
        for (String line : text.split("\r?\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            if (t.startsWith("[")) {
                table = t.substring(1, t.lastIndexOf(']'));
                continue;
            }
            int eq = t.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException("not a setting line: " + t);
            }
            String name = t.substring(0, eq).trim();
            out.put(table.isEmpty() ? name : table + "." + name, t.substring(eq + 1).trim());
        }
        return out;
    }
}

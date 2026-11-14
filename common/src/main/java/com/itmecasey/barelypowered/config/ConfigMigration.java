package com.itmecasey.barelypowered.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Brings the values of an old hand-written {@code .properties} file into the loader's TOML config. It runs only when the TOML does
 * not exist yet and the old file does, so it never replaces a TOML the player has edited.
 *
 * <p>Steps, in order: write the TOML, read it back and check every value, check that the old file was read cleanly, write the backup
 * ({@code <old file>.back}, with a header line and the exact original bytes), check the backup, and only then remove the original. If
 * any step fails, or the old file is corrupt or has values that could not be read, the original stays where it is. An existing backup
 * is never overwritten ({@code .back2}, {@code .back3}, ...). Nothing but the mod's own old config file is touched.
 */
public final class ConfigMigration {
    /** What happened, for the log and for tests. {@code backup} is null when the original was left in place. */
    public record Report(boolean ran, String reason, Path backup, List<String> imported, List<String> defaulted, List<String> invalid,
            List<String> clamped, List<String> retired, List<String> ignored) {
        static Report skipped(String reason) {
            return new Report(false, reason, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private ConfigMigration() {
    }

    /**
     * Reads an old properties file into flat keys. An old list setting such as {@code a=1.0,2.0,4.0} becomes one key for each tier
     * ({@code lists} maps the old key to the new keys). Keys that match no setting are added to {@code unknown}.
     */
    public static Map<String, String> expand(Properties old, List<ConfigDef> defs, Map<String, List<String>> lists, List<String> unknown) {
        Map<String, ConfigDef> byKey = new LinkedHashMap<>();
        defs.forEach(d -> byKey.put(d.key(), d));
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : old.stringPropertyNames()) {
            String value = old.getProperty(key);
            List<String> targets = lists.get(key);
            if (targets != null) {
                String[] parts = value.split(",", -1);
                for (int i = 0; i < targets.size() && i < parts.length; i++) {
                    out.put(targets.get(i), parts[i]);
                }
            } else if (byKey.containsKey(key)) {
                out.put(key, value);
            } else if (unknown != null) {
                unknown.add(key);
            }
        }
        return out;
    }

    /** As {@link #migrate(Path, Path, List, Map, Map, String, Clock, Consumer)} with today's date from the system clock. */
    public static Report migrate(Path oldFile, Path toml, List<ConfigDef> defs, Map<String, List<String>> lists,
            Map<String, List<String>> retired, String label, Consumer<String> log) {
        return migrate(oldFile, toml, defs, lists, retired, label, Clock.systemDefaultZone(), log);
    }

    /**
     * Imports {@code oldFile} into a new {@code toml} and, when everything checks out, moves the old file to a {@code .back} file.
     *
     * @param lists   old list keys mapped to the new per-tier keys
     * @param retired keys whose default changed in a release, mapped to the old defaults: a file written by that release holds the old
     *                default whether the player chose it or not, so a value equal to one of them is replaced by the new default
     * @param label   the mod and its version for the backup header, for example {@code BarelyPowered 2026.11.5}
     */
    public static Report migrate(Path oldFile, Path toml, List<ConfigDef> defs, Map<String, List<String>> lists,
            Map<String, List<String>> retired, String label, Clock clock, Consumer<String> log) {
        if (Files.exists(toml)) {
            return Report.skipped("the new config file already exists");
        }
        if (!Files.isRegularFile(oldFile)) {
            return Report.skipped("no old config file");
        }
        byte[] original;
        Properties old = new Properties();
        try {
            original = Files.readAllBytes(oldFile);
            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(original), StandardCharsets.UTF_8.newDecoder())) {
                old.load(reader);
            }
        } catch (IOException | RuntimeException e) {
            log.accept("Could not read the old config " + oldFile + " (" + e + "); the new config starts with the defaults. The old file is left where it is.");
            return Report.skipped("the old config could not be read");
        }
        List<String> unknown = new ArrayList<>();
        Map<String, String> raw = expand(old, defs, lists, unknown);
        List<String> imported = new ArrayList<>();
        List<String> defaulted = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        List<String> clamped = new ArrayList<>();
        List<String> retiredHits = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (ConfigDef def : defs) {
            String text = raw.get(def.key());
            if (text == null) {
                defaulted.add(def.key());
                continue;
            }
            Double number = parse(def, text.trim());
            if (number == null) {
                invalid.add(def.key() + "=" + text.trim());
                log.accept("Old config: invalid value '" + text.trim() + "' for " + def.key() + ", using the default " + def.fallback());
                continue;
            }
            if (def.type() != ConfigDef.Type.BOOL && isRetired(retired.get(def.key()), number)) {
                retiredHits.add(def.key() + "=" + text.trim());
                log.accept("Old config: " + def.key() + "=" + text.trim() + " is the default of an older version, using the new default " + def.fallback());
                continue;
            }
            double value = number;
            if (def.type() != ConfigDef.Type.BOOL && (value < def.min() || value > def.max())) {
                double limited = Math.max(def.min(), Math.min(def.max(), value));
                clamped.add(def.key() + "=" + text.trim() + " -> " + format(def, limited));
                log.accept("Old config: " + def.key() + "=" + text.trim() + " is outside " + def.rangeText() + ", using " + format(def, limited));
                value = limited;
            }
            values.put(def.key(), format(def, value));
            imported.add(def.key());
        }
        for (String key : unknown) {
            log.accept("Old config: ignoring unknown key " + key);
        }

        // 1. write the new file
        try {
            if (toml.getParent() != null) {
                Files.createDirectories(toml.getParent());
            }
            Files.writeString(toml, TomlConfig.render(defs, values), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException | RuntimeException e) {
            log.accept("Could not write " + toml + " (" + e + "); the loader will write its defaults. The old file is left where it is.");
            return Report.skipped("the new config could not be written");
        }
        // 2. read it back and check every value
        String problem = verify(toml, defs, values);
        if (problem != null) {
            log.accept("The new config " + toml + " did not match what was migrated (" + problem + "); removing it. The old file is left where it is.");
            try {
                Files.deleteIfExists(toml);
            } catch (IOException e) {
                log.accept("Could not remove " + toml + ": " + e);
            }
            return Report.skipped("the new config did not verify");
        }
        // 3. the old file must have been read cleanly
        if (!invalid.isEmpty()) {
            log.accept("Migrated " + imported.size() + " values from " + oldFile.getFileName() + " into " + toml.getFileName() + ", but "
                    + invalid.size() + " values could not be read (defaults used), so the old file is left where it is.");
            return new Report(true, "migrated, old file kept: unreadable values", null, imported, defaulted, invalid, clamped, retiredHits, unknown);
        }
        // 4. write the backup: a header line and the exact original bytes (never over an existing backup)
        String header = "# Migrated by " + label + " on " + LocalDate.now(clock) + " to config/" + toml.getFileName()
                + "; this file is no longer read. Kept as a backup.\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] backupBytes = new byte[headerBytes.length + original.length];
        System.arraycopy(headerBytes, 0, backupBytes, 0, headerBytes.length);
        System.arraycopy(original, 0, backupBytes, headerBytes.length, original.length);
        Path backup = backupPath(oldFile);
        try {
            Files.write(backup, backupBytes, StandardOpenOption.CREATE_NEW);
            // 5. check the backup holds the original content (also against the file on disk)
            byte[] written = Files.readAllBytes(backup);
            if (!Arrays.equals(written, backupBytes) || !Arrays.equals(Files.readAllBytes(oldFile), original)) {
                throw new IOException("the backup does not match the original");
            }
        } catch (IOException | RuntimeException e) {
            log.accept("Could not make the backup " + backup + " (" + e + "); the old file is left where it is.");
            try {
                Files.deleteIfExists(backup);
            } catch (IOException | RuntimeException ignored) {
                // a half-written backup we created ourselves; nothing else to do
            }
            return new Report(true, "migrated, old file kept: backup failed", null, imported, defaulted, invalid, clamped, retiredHits, unknown);
        }
        // 6. only now remove the original
        try {
            Files.delete(oldFile);
        } catch (IOException | RuntimeException e) {
            log.accept("Could not remove the old file " + oldFile + " (" + e + "); the backup " + backup + " exists as well.");
            return new Report(true, "migrated, old file could not be removed", backup, imported, defaulted, invalid, clamped, retiredHits, unknown);
        }
        log.accept("Migrated " + imported.size() + " values: old " + oldFile + " moved to " + backup + ", new " + toml + " ("
                + clamped.size() + " limited to the new range, " + retiredHits.size() + " equal to an older default, " + unknown.size()
                + " unknown keys ignored).");
        return new Report(true, "migrated", backup, imported, defaulted, invalid, clamped, retiredHits, unknown);
    }

    private static Path backupPath(Path oldFile) {
        Path candidate = oldFile.resolveSibling(oldFile.getFileName() + ".back");
        for (int n = 2; Files.exists(candidate); n++) {
            candidate = oldFile.resolveSibling(oldFile.getFileName() + ".back" + n);
        }
        return candidate;
    }

    /** Returns null when the written file holds exactly the migrated values (and defaults for the rest), otherwise what differed. */
    private static String verify(Path toml, List<ConfigDef> defs, Map<String, String> values) {
        Map<String, String> read;
        try {
            read = TomlConfig.parse(Files.readString(toml, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return "could not be read back: " + e;
        }
        for (ConfigDef def : defs) {
            String expected = values.getOrDefault(def.key(), def.fallback());
            String actual = read.get(def.key());
            if (actual == null) {
                return def.key() + " is missing";
            }
            boolean same = def.type() == ConfigDef.Type.BOOL ? expected.equals(actual) : sameNumber(expected, actual);
            if (!same) {
                return def.key() + " is " + actual + ", expected " + expected;
            }
        }
        return read.size() == defs.size() ? null : "it holds " + read.size() + " entries, expected " + defs.size();
    }

    private static boolean sameNumber(String a, String b) {
        try {
            return Math.abs(Double.parseDouble(a) - Double.parseDouble(b)) < 1e-9;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isRetired(List<String> olds, double value) {
        if (olds == null) {
            return false;
        }
        for (String old : olds) {
            if (Math.abs(Double.parseDouble(old) - value) < 1e-9) {
                return true;
            }
        }
        return false;
    }

    /** The value as a number (1 or 0 for a boolean), or null when the text is not valid for the type. */
    private static Double parse(ConfigDef def, String text) {
        try {
            if (def.type() == ConfigDef.Type.BOOL) {
                if (text.equalsIgnoreCase("true")) {
                    return 1.0;
                }
                return text.equalsIgnoreCase("false") ? 0.0 : null;
            }
            double v = Double.parseDouble(text);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            return def.type() == ConfigDef.Type.INT ? (double) Math.round(v) : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String format(ConfigDef def, double value) {
        return switch (def.type()) {
            case INT -> String.valueOf(Math.round(value));
            case DOUBLE -> String.valueOf(value);
            case BOOL -> value != 0 ? "true" : "false";
        };
    }
}

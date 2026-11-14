package com.itmecasey.barelycables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelycables.config.ConfigDef;
import com.itmecasey.barelycables.config.ConfigMigration;
import com.itmecasey.barelycables.config.TomlConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The old barelycables.properties of every released version (real files written by each tagged build) imported into the new TOML. */
class ConfigMigrationTest {
    private static final String[] TAGS = {"v2026.11.1", "v2026.11.2", "v2026.11.3", "v2026.11.4"};
    private static final String HEADER = "# Migrated by BarelyCables 2026.11.4 on 2026-11-14 to config/barelycables-common.toml;"
            + " this file is no longer read. Kept as a backup.\n";

    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();

    private Path old() {
        return dir.resolve("barelycables.properties");
    }

    private Path tomlFile() {
        return dir.resolve("barelycables-common.toml");
    }

    private Path backupFile() {
        return dir.resolve("barelycables.properties.back");
    }

    private Path fixture(String name) throws IOException {
        try (InputStream in = ConfigMigrationTest.class.getResourceAsStream("/old-config/" + name)) {
            if (in == null) {
                throw new IOException("missing fixture " + name);
            }
            Files.copy(in, old(), StandardCopyOption.REPLACE_EXISTING);
        }
        return old();
    }

    private ConfigMigration.Report run(Path oldFile) {
        return ConfigMigration.migrate(oldFile, tomlFile(), CableSettings.defs(), CableSettings.oldListKeys(), CableSettings.retiredDefaults(),
                "BarelyCables 2026.11.4", Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC), log::add);
    }

    private Map<String, String> toml() throws IOException {
        return TomlConfig.parse(Files.readString(tomlFile(), StandardCharsets.UTF_8));
    }

    private static byte[] join(String header, byte[] original) {
        byte[] h = header.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[h.length + original.length];
        System.arraycopy(h, 0, out, 0, h.length);
        System.arraycopy(original, 0, out, h.length, original.length);
        return out;
    }

    @Test
    void theEditedValuesOfEveryReleasedVersionLandInTheToml() throws IOException {
        for (String tag : TAGS) {
            Files.deleteIfExists(tomlFile());
            Files.deleteIfExists(backupFile());
            ConfigMigration.Report report = run(fixture("barelycables-" + tag + "-edited.properties"));
            assertTrue(report.ran(), tag);
            Map<String, String> t = toml();
            assertEquals(4, t.size(), tag);
            assertEquals("400", t.get("cable.basic.rate"), tag);
            assertEquals("2000", t.get("cable.iron.rate"), tag);
            assertEquals("6400", t.get("cable.steel.rate"), tag);
            assertEquals("4", t.get("cable.buffer_ticks"), tag);
            assertEquals(4, report.imported().size(), tag);
        }
    }

    @Test
    void defaultFilesGiveTheDefaults() throws IOException {
        for (String tag : TAGS) {
            Files.deleteIfExists(tomlFile());
            Files.deleteIfExists(backupFile());
            run(fixture("barelycables-" + tag + "-defaults.properties"));
            Map<String, String> t = toml();
            for (ConfigDef def : CableSettings.defs()) {
                assertEquals(def.fallback(), t.get(def.key()), tag + " " + def.key());
            }
        }
    }

    @Test
    void aVerifiedMigrationMovesTheOldFileToABackupWithAHeader() throws IOException {
        Path oldFile = fixture("barelycables-v2026.11.3-edited.properties");
        byte[] original = Files.readAllBytes(oldFile);
        ConfigMigration.Report report = run(oldFile);
        assertTrue(report.ran());
        assertEquals(backupFile(), report.backup());
        assertFalse(Files.exists(oldFile), "the original is gone");
        assertTrue(Files.exists(tomlFile()));
        assertArrayEquals(join(HEADER, original), Files.readAllBytes(backupFile()), "header line plus the exact original bytes");
        try (var names = Files.list(dir)) {
            assertEquals(2, names.count(), "only the toml and the backup");
        }
        assertTrue(log.stream().anyMatch(m -> m.contains(oldFile.toString()) && m.contains(backupFile().toString()) && m.contains(tomlFile().toString())),
                log.toString());
    }

    @Test
    void aSecondLaunchDoesNothingAndAnExistingTomlIsNeverReplaced() throws IOException {
        run(fixture("barelycables-v2026.11.4-edited.properties"));
        byte[] toml = Files.readAllBytes(tomlFile());
        byte[] backup = Files.readAllBytes(backupFile());
        assertFalse(run(old()).ran());
        assertArrayEquals(toml, Files.readAllBytes(tomlFile()));
        assertArrayEquals(backup, Files.readAllBytes(backupFile()));
        assertFalse(Files.exists(dir.resolve("barelycables.properties.back2")));

        // a new old file next to an existing toml is left alone
        Files.writeString(old(), "cable.iron.rate=1\n");
        assertFalse(run(old()).ran());
        assertEquals("cable.iron.rate=1\n", Files.readString(old()));
        assertArrayEquals(toml, Files.readAllBytes(tomlFile()));
    }

    @Test
    void anExistingBackupIsNeverOverwritten() throws IOException {
        Files.writeString(backupFile(), "an older backup");
        ConfigMigration.Report report = run(fixture("barelycables-v2026.11.4-edited.properties"));
        assertEquals("an older backup", Files.readString(backupFile()));
        assertEquals(dir.resolve("barelycables.properties.back2"), report.backup());
        assertTrue(Files.readString(report.backup()).startsWith(HEADER));

        Files.delete(tomlFile());
        fixture("barelycables-v2026.11.1-edited.properties");
        assertEquals(dir.resolve("barelycables.properties.back3"), run(old()).backup());
        assertEquals("an older backup", Files.readString(backupFile()));
    }

    @Test
    void missingEmptyCorruptAndPartialOldFiles() throws IOException {
        assertFalse(run(old()).ran());
        assertFalse(Files.exists(tomlFile()));

        byte[] garbage = {(byte) 0xff, (byte) 0xfe, (byte) 0xc3, 0x28, (byte) 0xa0, (byte) 0xa1};
        Files.write(old(), garbage);
        assertFalse(run(old()).ran());
        assertFalse(Files.exists(tomlFile()));
        assertFalse(Files.exists(backupFile()));
        assertArrayEquals(garbage, Files.readAllBytes(old()));
        assertTrue(log.stream().anyMatch(m -> m.contains("Could not read")), log.toString());

        Files.writeString(old(), "");
        ConfigMigration.Report empty = run(old());
        assertTrue(empty.ran());
        assertEquals(HEADER, Files.readString(empty.backup()));
        assertEquals("1600", toml().get("cable.iron.rate"));
        Files.delete(tomlFile());
        Files.delete(backupFile());

        Files.writeString(old(), "cable.steel.rate=9000\n");
        assertTrue(run(old()).ran());
        Map<String, String> t = toml();
        assertEquals("9000", t.get("cable.steel.rate"));
        assertEquals("400", t.get("cable.basic.rate"));
    }

    @Test
    void valuesThatCannotBeReadKeepTheOriginalInPlace() throws IOException {
        String text = "cable.basic.rate=fast\ncable.iron.rate=-4\ncable.buffer_ticks=99999\nmystery=1\n";
        Files.writeString(old(), text);
        ConfigMigration.Report report = run(old());
        Map<String, String> t = toml();
        assertEquals("400", t.get("cable.basic.rate"));
        assertEquals("1", t.get("cable.iron.rate"));
        assertEquals("1200", t.get("cable.buffer_ticks"));
        assertEquals(1, report.invalid().size());
        assertEquals(2, report.clamped().size());
        assertEquals(List.of("mystery"), report.ignored());
        assertNull(report.backup());
        assertEquals(text, Files.readString(old()));
        assertFalse(Files.exists(backupFile()));
        assertTrue(log.stream().anyMatch(m -> m.contains("invalid value 'fast' for cable.basic.rate")), log.toString());
    }

    @Test
    void limitedValuesAndUnknownKeysStillMigrate() throws IOException {
        String text = "cable.iron.rate=-4\nmystery=1\n";
        Files.writeString(old(), text);
        ConfigMigration.Report report = run(old());
        assertEquals("1", toml().get("cable.iron.rate"));
        assertNotNull(report.backup());
        assertFalse(Files.exists(old()));
        assertEquals(HEADER + text, Files.readString(report.backup()));
    }

    @Test
    void everyDefaultIsInsideItsRange() {
        for (ConfigDef def : CableSettings.defs()) {
            double v = Double.parseDouble(def.fallback());
            assertTrue(v >= def.min() && v <= def.max(), def.key());
        }
    }
}

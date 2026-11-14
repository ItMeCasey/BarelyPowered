package com.itmecasey.barelypowered;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelypowered.config.ConfigDef;
import com.itmecasey.barelypowered.config.ConfigMigration;
import com.itmecasey.barelypowered.config.TomlConfig;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The old barelypowered.properties of every released version (real files written by each tagged build) imported into the new TOML. */
class ConfigMigrationTest {
    private static final String[] TAGS = {"v2026.11.1", "v2026.11.2", "v2026.11.3", "v2026.11.4"};
    private static final String HEADER = "# Migrated by BarelyPowered 2026.11.5 on 2026-11-14 to config/barelypowered-common.toml;"
            + " this file is no longer read. Kept as a backup.\n";

    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();

    private Path old() {
        return dir.resolve("barelypowered.properties");
    }

    private Path tomlFile() {
        return dir.resolve("barelypowered-common.toml");
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
        return ConfigMigration.migrate(oldFile, tomlFile(), Settings.defs(), Settings.oldListKeys(), Settings.retiredDefaults(),
                "BarelyPowered 2026.11.5", Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC), log::add);
    }

    private Map<String, String> toml() throws IOException {
        return TomlConfig.parse(Files.readString(tomlFile(), StandardCharsets.UTF_8));
    }

    private static Properties props(Path file) throws IOException {
        Properties p = new Properties();
        try (var r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    private static void assertNumber(double expected, String actual, String what) {
        assertEquals(expected, Double.parseDouble(actual), 1e-9, what);
    }

    private static byte[] join(String header, byte[] original) {
        byte[] h = header.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[h.length + original.length];
        System.arraycopy(h, 0, out, 0, h.length);
        System.arraycopy(original, 0, out, h.length, original.length);
        return out;
    }

    @Test
    void everyValueOfEveryReleasedVersionLandsInTheToml() throws IOException {
        for (String tag : TAGS) {
            Files.deleteIfExists(tomlFile());
            Files.deleteIfExists(dir.resolve("barelypowered.properties.back"));
            ConfigMigration.Report report = run(fixture("barelypowered-" + tag + "-edited.properties"));
            assertTrue(report.ran(), tag);
            assertNotNull(report.backup(), tag);
            Map<String, String> got = toml();
            Properties written = props(report.backup());
            Map<String, ConfigDef> byKey = new LinkedHashMap<>();
            Settings.defs().forEach(d -> byKey.put(d.key(), d));
            assertEquals(byKey.size(), got.size(), tag + ": the toml has every setting");
            int checked = 0;
            for (String key : written.stringPropertyNames()) {
                String value = written.getProperty(key);
                if (Settings.oldListKeys().containsKey(key)) {
                    String[] parts = value.split(",");
                    List<String> keys = Settings.oldListKeys().get(key);
                    for (int i = 0; i < 3; i++) {
                        assertNumber(Double.parseDouble(parts[i]), got.get(keys.get(i)), tag + " " + keys.get(i));
                        checked++;
                    }
                    continue;
                }
                ConfigDef def = byKey.get(key);
                String shown = got.get(key);
                if (def.type() == ConfigDef.Type.BOOL) {
                    assertEquals(value.trim(), shown, tag + " " + key);
                } else if (key.equals("water_wheel.fe_per_flow") && Double.parseDouble(value) == 28.0) {
                    assertNumber(20.0, shown, tag + " the old 28.0 default becomes the new default");
                } else {
                    assertNumber(Double.parseDouble(value), shown, tag + " " + key);
                }
                checked++;
            }
            assertTrue(checked >= 53, tag + " checked " + checked);
        }
    }

    @Test
    void theEditedValuesAreTheOnesInTheToml() throws IOException {
        run(fixture("barelypowered-v2026.11.4-edited.properties"));
        Map<String, String> t = toml();
        assertEquals("false", t.get("general.push_to_neighbors"));
        assertNumber(45, t.get("combustion.coal.fe_per_tick"), "coal");
        assertNumber(80, t.get("wind.height.min_y"), "min_y");
        assertNumber(500000, t.get("battery.iron.capacity"), "capacity");
        assertNumber(1.5, t.get("combustion.tier.output_multiplier.stone"), "stone");
        assertNumber(2.5, t.get("combustion.tier.output_multiplier.iron"), "iron");
        assertNumber(5.0, t.get("combustion.tier.output_multiplier.steel"), "steel");
        assertNumber(0.5, t.get("solar.tier2.rain_factor"), "rain");
        assertNumber(35.0, t.get("water_wheel.fe_per_flow"), "a water wheel value the player chose stays");
        assertNumber(100, t.get("combustion.planks.burn_ticks"), "planks");
    }

    @Test
    void oldFilesWithoutNewerKeysUseTheDefaultsForThem() throws IOException {
        ConfigMigration.Report report = run(fixture("barelypowered-v2026.11.1-edited.properties"));
        Map<String, String> t = toml();
        assertTrue(report.defaulted().contains("combustion.planks.fe_per_tick"));
        assertNumber(12, t.get("combustion.planks.fe_per_tick"), "planks default");
        assertNumber(75, t.get("combustion.planks.burn_ticks"), "planks default");
        assertEquals(List.of("water_wheel.fe_per_flow=28.0"), report.retired());
        assertNumber(20.0, t.get("water_wheel.fe_per_flow"), "water wheel");
    }

    @Test
    void defaultFilesGiveTheDefaults() throws IOException {
        for (String tag : TAGS) {
            Files.deleteIfExists(tomlFile());
            Files.deleteIfExists(dir.resolve("barelypowered.properties.back"));
            run(fixture("barelypowered-" + tag + "-defaults.properties"));
            Map<String, String> t = toml();
            for (ConfigDef def : Settings.defs()) {
                String shown = t.get(def.key());
                if (def.type() == ConfigDef.Type.BOOL) {
                    assertEquals(def.fallback(), shown, tag + " " + def.key());
                } else {
                    assertNumber(Double.parseDouble(def.fallback()), shown, tag + " " + def.key());
                }
            }
        }
    }

    @Test
    void aVerifiedMigrationMovesTheOldFileToABackupWithAHeader() throws IOException {
        Path oldFile = fixture("barelypowered-v2026.11.3-edited.properties");
        byte[] original = Files.readAllBytes(oldFile);
        ConfigMigration.Report report = run(oldFile);
        Path backup = dir.resolve("barelypowered.properties.back");
        assertTrue(report.ran());
        assertEquals(backup, report.backup());
        assertFalse(Files.exists(oldFile), "the original is gone");
        assertTrue(Files.exists(tomlFile()));
        assertArrayEquals(join(HEADER, original), Files.readAllBytes(backup), "header line plus the exact original bytes");
        try (var names = Files.list(dir)) {
            assertEquals(2, names.count(), "only the toml and the backup");
        }
        assertTrue(log.stream().anyMatch(m -> m.contains(oldFile.toString()) && m.contains(backup.toString()) && m.contains(tomlFile().toString())),
                log.toString());
    }

    @Test
    void aSecondLaunchDoesNothing() throws IOException {
        run(fixture("barelypowered-v2026.11.4-edited.properties"));
        byte[] toml = Files.readAllBytes(tomlFile());
        byte[] backup = Files.readAllBytes(dir.resolve("barelypowered.properties.back"));
        ConfigMigration.Report again = run(old());
        assertFalse(again.ran());
        assertArrayEquals(toml, Files.readAllBytes(tomlFile()));
        assertArrayEquals(backup, Files.readAllBytes(dir.resolve("barelypowered.properties.back")));
        assertFalse(Files.exists(dir.resolve("barelypowered.properties.back2")));
    }

    @Test
    void anExistingBackupIsNeverOverwritten() throws IOException {
        Path first = dir.resolve("barelypowered.properties.back");
        Files.writeString(first, "an older backup");
        Path oldFile = fixture("barelypowered-v2026.11.4-edited.properties");
        ConfigMigration.Report report = run(oldFile);
        assertEquals("an older backup", Files.readString(first));
        assertEquals(dir.resolve("barelypowered.properties.back2"), report.backup());
        assertTrue(Files.readString(report.backup()).startsWith(HEADER));
        assertFalse(Files.exists(oldFile));

        Files.delete(tomlFile());
        fixture("barelypowered-v2026.11.1-edited.properties");
        assertEquals(dir.resolve("barelypowered.properties.back3"), run(old()).backup());
        assertEquals("an older backup", Files.readString(first));
    }

    @Test
    void anExistingTomlIsNeverReplacedAndTheOldFileStays() throws IOException {
        Files.writeString(tomlFile(), "[general]\nbuffer_ticks = 7\n");
        Path oldFile = fixture("barelypowered-v2026.11.4-edited.properties");
        byte[] original = Files.readAllBytes(oldFile);
        ConfigMigration.Report report = run(oldFile);
        assertFalse(report.ran());
        assertEquals("[general]\nbuffer_ticks = 7\n", Files.readString(tomlFile()));
        assertArrayEquals(original, Files.readAllBytes(oldFile));
        assertFalse(Files.exists(dir.resolve("barelypowered.properties.back")));
    }

    @Test
    void noOldFileMeansNothingIsWritten() {
        ConfigMigration.Report report = run(old());
        assertFalse(report.ran());
        assertFalse(Files.exists(tomlFile()));
    }

    @Test
    void anEmptyOldFileGivesAllDefaultsAndAHeaderOnlyBackup() throws IOException {
        Files.writeString(old(), "");
        ConfigMigration.Report report = run(old());
        assertTrue(report.ran());
        assertTrue(report.imported().isEmpty());
        assertEquals(HEADER, Files.readString(report.backup()));
        Map<String, String> t = toml();
        assertEquals(Settings.defs().size(), t.size());
        assertNumber(30, t.get("combustion.coal.fe_per_tick"), "default");
    }

    @Test
    void aCorruptOldFileIsLoggedAndLeftWhereItIs() throws IOException {
        byte[] garbage = {(byte) 0xff, (byte) 0xfe, (byte) 0xc3, 0x28, (byte) 0xa0, (byte) 0xa1, 0x0a, (byte) 0x80};
        Files.write(old(), garbage);
        ConfigMigration.Report report = run(old());
        assertFalse(report.ran());
        assertFalse(Files.exists(tomlFile()));
        assertFalse(Files.exists(dir.resolve("barelypowered.properties.back")));
        assertArrayEquals(garbage, Files.readAllBytes(old()), "a corrupt original stays where it is");
        assertTrue(log.stream().anyMatch(m -> m.contains("Could not read")), log.toString());

        String broken = "combustion.coal.fe_per_tick=\\u12\n";
        Files.writeString(old(), broken);
        assertFalse(run(old()).ran());
        assertFalse(Files.exists(tomlFile()));
        assertFalse(Files.exists(dir.resolve("barelypowered.properties.back")));
        assertEquals(broken, Files.readString(old()));
    }

    @Test
    void aPartiallyFilledOldFileKeepsWhatIsThere() throws IOException {
        Files.writeString(old(), "battery.steel.rate=5000\nlava.tank_buckets=8\n");
        ConfigMigration.Report report = run(old());
        Map<String, String> t = toml();
        assertEquals(List.of("lava.tank_buckets", "battery.steel.rate"), report.imported().stream().sorted((a, b) -> b.compareTo(a)).toList());
        assertNumber(5000, t.get("battery.steel.rate"), "rate");
        assertNumber(8, t.get("lava.tank_buckets"), "buckets");
        assertNumber(100, t.get("general.buffer_ticks"), "default");
    }

    @Test
    void valuesThatCannotBeReadUseDefaultsAndKeepTheOriginalInPlace() throws IOException {
        Files.writeString(old(), "wind.height.min_y=oops\nsolar.tier1.rain_factor=7\nbattery.basic.rate=-5\ngeneral.push_to_neighbors=maybe\n"
                + "unknown.key=5\ncombustion.tier.burn_multiplier=0.5\ncombustion.coal.burn_ticks=1600.4\n");
        ConfigMigration.Report report = run(old());
        Map<String, String> t = toml();
        assertNumber(64, t.get("wind.height.min_y"), "invalid -> default");
        assertNumber(1.0, t.get("solar.tier1.rain_factor"), "limited to 1");
        assertNumber(1, t.get("battery.basic.rate"), "limited to 1");
        assertEquals("true", t.get("general.push_to_neighbors"));
        assertNumber(0.5, t.get("combustion.tier.burn_multiplier.stone"), "the first element of a short list");
        assertNumber(0.75, t.get("combustion.tier.burn_multiplier.iron"), "the missing elements use the defaults");
        assertNumber(1600, t.get("combustion.coal.burn_ticks"), "a whole number setting rounds");
        assertEquals(2, report.invalid().size());
        assertEquals(2, report.clamped().size());
        assertEquals(List.of("unknown.key"), report.ignored());
        assertNull(report.backup(), "values that could not be read keep the original in place");
        assertTrue(Files.exists(old()));
        assertFalse(Files.exists(dir.resolve("barelypowered.properties.back")));
        assertTrue(log.stream().anyMatch(m -> m.contains("invalid value 'oops' for wind.height.min_y")), log.toString());
        assertTrue(log.stream().anyMatch(m -> m.contains("solar.tier1.rain_factor=7 is outside")), log.toString());
        assertTrue(log.stream().anyMatch(m -> m.contains("unknown key unknown.key")), log.toString());
    }

    @Test
    void limitedValuesAndUnknownKeysStillMigrateAndMoveTheOriginal() throws IOException {
        String text = "solar.tier1.rain_factor=7\nunknown.key=5\n";
        Files.writeString(old(), text);
        ConfigMigration.Report report = run(old());
        assertNumber(1.0, toml().get("solar.tier1.rain_factor"), "limited");
        assertNotNull(report.backup());
        assertFalse(Files.exists(old()));
        assertEquals(HEADER + text, Files.readString(report.backup()));
    }

    @Test
    void everyDefaultIsInsideItsRangeAndEveryKeyHasAComment() {
        for (ConfigDef def : Settings.defs()) {
            assertFalse(def.comment().isBlank(), def.key());
            if (def.type() != ConfigDef.Type.BOOL) {
                double v = Double.parseDouble(def.fallback());
                assertTrue(v >= def.min() && v <= def.max(), def.key() + " default " + v + " outside " + def.rangeText());
            }
        }
    }
}

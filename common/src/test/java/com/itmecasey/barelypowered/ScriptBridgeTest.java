package com.itmecasey.barelypowered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelypowered.script.ModJsBridge;
import com.itmecasey.barelypowered.script.PowerApi;
import com.itmecasey.barelypowered.script.ScriptEvents;
import com.itmecasey.modjs.script.ExtraBindings;
import com.itmecasey.modjs.script.ScriptType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Runs real JavaScript (Rhino, the engine ModJS uses) against the PowerApi through the reflective ModJS bridge. */
class ScriptBridgeTest {
    @BeforeAll
    static void install() {
        assertTrue(ModJsBridge.install(PowerApi.INSTANCE));
    }

    @BeforeEach
    @AfterEach
    void reset() {
        Settings.reset();
        ScriptEvents.clear();
        PowerApi.attach(null);
    }

    private static Object run(String source) {
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.forEach(p -> p.install(ScriptType.SERVER, cx, scope));
            return cx.evaluateString(scope, source, "test", 1, null);
        } finally {
            Context.exit();
        }
    }

    private static ScriptEvents.Event event(String name) {
        return new ScriptEvents.Event(name, "combustion_generator", 2, "barelypowered:combustion_generator_iron", "minecraft:overworld", 1, 64, 2, 60.0, "burning", "coal");
    }

    @Test
    void listenersAreRegisteredPerEventName() {
        run("BarelyPowered.on('generatorStarted', e => 1);");
        assertTrue(ScriptEvents.hasListeners("generatorStarted"));
        assertFalse(ScriptEvents.hasListeners("generatorStopped"));
    }

    @Test
    void listenerReceivesTheEventFields() {
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.forEach(p -> p.install(ScriptType.SERVER, cx, scope));
            cx.evaluateString(scope, "var got = ''; BarelyPowered.on('fuelBurned', e => { got = e.fuel + '/' + e.id + '/' + e.dimension + '/' + e.note; });", "t", 1, null);
            ScriptEvents.fire(event("fuelBurned"));
            Object got = cx.evaluateString(scope, "got", "t", 1, null);
            assertEquals("coal/barelypowered:combustion_generator_iron/minecraft:overworld/burning", Context.toString(got));
        } finally {
            Context.exit();
        }
    }

    @Test
    void aFailingListenerDoesNotReachTheGameAndTheNextOneStillRuns() {
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.forEach(p -> p.install(ScriptType.SERVER, cx, scope));
            cx.evaluateString(scope, "var n = 0; BarelyPowered.on('generatorStopped', e => { throw new Error('boom'); }); BarelyPowered.on('generatorStopped', e => { n++; });", "t", 1, null);
            ScriptEvents.fire(event("generatorStopped"));
            assertEquals(1.0, Context.toNumber(cx.evaluateString(scope, "n", "t", 1, null)));
        } finally {
            Context.exit();
        }
    }

    @Test
    void unknownEventNamesAreRefused() {
        assertEquals(Boolean.FALSE, run("BarelyPowered.on('nothing', e => 1)"));
        assertTrue(Context.toBoolean(run("BarelyPowered.events().contains('fuelBurned')")));
    }

    @Test
    void settingsCanBeReadAndChangedForTheSession() {
        assertEquals(30.0, Context.toNumber(run("BarelyPowered.settingNumber('combustion.coal.fe_per_tick')")));
        assertEquals("30", Context.toString(run("BarelyPowered.setting('combustion.coal.fe_per_tick')")));
        assertTrue(Context.toBoolean(run("BarelyPowered.setSetting('combustion.coal.fe_per_tick', 45)")));
        assertEquals(45, Fuel.COAL.fePerTick());
        assertEquals(45, PowerMath.combustionOutput(Fuel.COAL, 1));
        assertFalse(Context.toBoolean(run("BarelyPowered.setSetting('no.such.setting', 1)")));
        assertNull(PowerApi.INSTANCE.setting("no.such.setting"));
        assertTrue(Context.toBoolean(run("BarelyPowered.settingKeys().contains('battery.iron.rate')")));
    }

    @Test
    void readsGoThroughTheAttachedWorldAndAreNullWithoutOne() {
        assertNull(PowerApi.INSTANCE.generatorAt("minecraft:overworld", 0, 64, 0));
        PowerApi.attach(new PowerApi.World() {
            public PowerApi.Generator generatorAt(String dimension, int x, int y, int z) {
                return x == 1 ? new PowerApi.Generator("solar_panel", 2, "barelypowered:solar_panel_2", 24.0, 100, 2400, true, "ok", "", 0, 0, 0, 0) : null;
            }

            public PowerApi.Battery batteryAt(String dimension, int x, int y, int z) {
                throw new IllegalStateException("broken");
            }
        });
        assertEquals("solar_panel:2:24", Context.toString(run("var g = BarelyPowered.generatorAt('minecraft:overworld', 1, 64, 0); g.kind + ':' + g.tier + ':' + g.output")));
        assertTrue(Context.toBoolean(run("BarelyPowered.generatorAt('minecraft:overworld', 2, 64, 0) == null")));
        assertNull(PowerApi.INSTANCE.batteryAt("minecraft:overworld", 1, 64, 0));
    }

    @Test
    void kindsListTheGeneratorIds() {
        assertTrue(Context.toBoolean(run("BarelyPowered.kinds().contains('wind_turbine') && BarelyPowered.kinds().size() == 7")));
    }
}

package com.itmecasey.barelypowered.dev;

import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.BatteryBlock;
import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A development-only camera tour: it opens a fresh superflat world, puts each generator beside an energy sink, lets it run,
 * and saves screenshots into {@code run/screenshots} as {@code barelypowered_<name>.png}, then quits. It does nothing unless the
 * environment variable {@code BARELYPOWERED_SHOTS} is set. Progress goes to the log as {@code BARELYPOWERED-SHOTS} lines.
 *
 * <p>It runs on a desktop someone may be using, so it stays out of the way: it lets go of the mouse every tick and keeps
 * rendering while unfocused; the script that starts it mutes the game.
 */
public final class ShotTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyPowered");
    private static final boolean ENABLED = System.getenv("BARELYPOWERED_SHOTS") != null;
    private static final String WORLD = "barelypowered_shots";
    private static final int SETTLE = 50;

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action) {
    }

    private interface Setup {
        void apply(GeneratorBlockEntity be, ServerLevel level, BlockPos pos);
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static int ground;
    private static boolean pauseOnLostFocus;

    private ShotTour() {
    }

    public static void tick() {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        switch (stage) {
            case WAITING_FOR_MENU -> waitForMenu(minecraft);
            case LOADING_WORLD -> waitForWorld(minecraft);
            case TOURING -> runSteps();
            default -> {
            }
        }
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.getOverlay() != null || minecraft.level != null || minecraft.screen == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;

        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("BARELYPOWERED-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("BARELYPOWERED-SHOTS creating world");
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                WorldPresets::createFlatWorldDimensions, new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || minecraft.getSingleplayerServer() == null || ++idle < 40) {
            return;
        }
        ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        LOGGER.info("BARELYPOWERED-SHOTS in the world, ground at y={}", ground);
        planTour();
        stage = Stage.TOURING;
    }

    private static void runSteps() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // --- The tour ----------------------------------------------------------------------------

    static int ground() {
        return ground;
    }

    /** The integration check: Jade scene plus a script of viewer steps, then quit (see ViewerTour). */
    private static void planViewers() {
        then(0, () -> {
            server("gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "time set noon", "weather clear", "kill @e[type=!player]", "clear @p");
            player().getAbilities().flying = false;
        });
        ViewerTour.plan();
        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            LOGGER.info("BARELYPOWERED-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    private static void planTour() {
        if ("viewers".equals(System.getenv("BARELYPOWERED_SHOTS"))) {
            planViewers();
            return;
        }
        then(0, () -> {
            server("gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "time set noon", "weather clear", "kill @e[type=!player]", "clear @p");
            LocalPlayer player = player();
            player.getAbilities().flying = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            Minecraft.getInstance().options.hideGui = false;
        });
        int x = 0;

        // The whole family in the hotbar.
        then(5, () -> server("give @p barelypowered:hand_crank", "give @p barelypowered:combustion_generator_stone",
                "give @p barelypowered:lava_generator_iron", "give @p barelypowered:solar_panel_2",
                "give @p barelypowered:wind_turbine_1", "give @p barelypowered:water_wheel",
                "give @p barelypowered:geothermal_generator", "give @p barelypowered:steel_ingot 4"));
        then(10, () -> selectSlot(0));
        then(2, () -> moveTo(new Vec3(0.5, ground + 2.0, -2.0), new Vec3(0.5, ground + 0.5, 0.5)));
        shoot("hotbar_items", 20);
        then(2, () -> server("clear @p"));

        scene("hand_crank", "hand_crank", x, ground, 5, (be, level, pos) -> be.crank(), "crank");
        scene("combustion_stone_coal", "combustion_generator_stone", x += 4, ground, SETTLE,
                (be, level, pos) -> be.debugSet(new ItemStack(Items.COAL, 8), 0), "combustion_stone");
        scene("combustion_steel_charcoal", "combustion_generator_steel", x += 4, ground, SETTLE,
                (be, level, pos) -> be.debugSet(new ItemStack(Items.CHARCOAL, 8), 0), "combustion_steel");
        scene("lava_iron", "lava_generator_iron", x += 4, ground, SETTLE,
                (be, level, pos) -> be.debugSet(ItemStack.EMPTY, 3000), "lava_iron");
        scene("solar_tier1_noon", "solar_panel_1", x += 4, ground, SETTLE, (be, level, pos) -> {
        }, "solar_1");
        then(2, () -> server("time set midnight"));
        scene("solar_tier2_midnight", "solar_panel_2", x += 4, ground, SETTLE, (be, level, pos) -> {
        }, "solar_2");
        then(2, () -> server("time set noon"));
        scene("wind_tier1_low", "wind_turbine_1", x += 4, ground, SETTLE, (be, level, pos) -> {
        }, "wind_low");
        scene("wind_tier1_high", "wind_turbine_1", x += 8, 200, SETTLE, (be, level, pos) -> {
        }, "wind_high");
        scene("wind_tier2_high", "wind_turbine_2", x += 8, 200, SETTLE, (be, level, pos) -> {
        }, "wind_2");
        scene("geothermal", "geothermal_generator", x += 8, ground, SETTLE, (be, level, pos) -> {
        }, "geothermal", pos -> {
            server(String.format(Locale.ROOT, "setblock %d %d %d minecraft:lava", pos.getX() - 1, pos.getY(), pos.getZ() + 1),
                    String.format(Locale.ROOT, "setblock %d %d %d minecraft:lava", pos.getX(), pos.getY(), pos.getZ() + 1));
        });

        scene("water_wheel", "water_wheel", x += 8, ground, SETTLE, (be, level, pos) -> {
        }, "water", pos -> {
            server(String.format(Locale.ROOT, "setblock %d %d %d minecraft:water", pos.getX() - 1, pos.getY() + 1, pos.getZ()));
        });
        // a hopper above an iron combustion generator feeds it coal through the item capability
        scene("hopper_feed_combustion", "combustion_generator_iron", x += 8, ground, SETTLE * 2, (be, level, pos) -> {
            level.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState(), 3);
            if (level.getBlockEntity(pos.above()) instanceof net.minecraft.world.level.block.entity.HopperBlockEntity hopper) {
                hopper.setItem(0, new ItemStack(Items.COAL, 6));
            }
        }, "hopper_feed");
        batteryScene(x += 8);
        if (haveCables()) {
            cableScene(x += 8);
        }

        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            LOGGER.info("BARELYPOWERED-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    private static void scene(String name, String block, int x, int y, int settle, Setup setup, String label) {
        scene(name, block, x, y, settle, setup, label, null);
    }

    /** Builds a generator with an energy sink to its east, sets it up, lets it run, then takes a few frames and the status lines. */
    private static void scene(String name, String block, int x, int y, int settle, Setup setup, String label,
                              java.util.function.Consumer<BlockPos> extra) {
        BlockPos pos = new BlockPos(x, y, 0);
        then(4, () -> {
            if (y > ground + 50) {
                server(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:stone", x - 3, y - 1, -3, x + 3, y - 1, 3));
            }
            server(String.format(Locale.ROOT, "setblock %d %d %d barelypowered:%s[facing=north]", x, y, 0, block),
                    String.format(Locale.ROOT, "setblock %d %d %d barelypowered:test_energy_sink", x + 1, y, 0));
            if (extra != null) {
                extra.accept(pos);
            }
        });
        then(4, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity be) {
                setup.apply(be, level, pos);
            }
        }));
        then(4, () -> moveTo(new Vec3(x + 1.0, y + 1.1, -2.1), new Vec3(x + 1.0, y + 0.5, 0.5)));
        shoot(name + "_a", settle);
        shoot(name + "_b", 5);
        shoot(name + "_c", 5);
        if (block.startsWith("wind_turbine")) {
            int h = block.endsWith("_2") ? 5 : 3;
            then(2, () -> moveTo(new Vec3(x + 0.5, y + h * 0.45, -11.0), new Vec3(x + 0.5, y + h * 0.5, 0.5)));
            shoot(name + "_tall", 30);
            shoot(name + "_tall2", 12);
        } else if (block.startsWith("solar_panel")) {
            then(2, () -> moveTo(new Vec3(x + 0.5, y + 3.5, -7.0), new Vec3(x + 0.5, y + 1.0, 0.5)));
            shoot(name + "_wide", 20);
        }
        then(2, () -> onServer(server -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            if (server.overworld().getBlockEntity(pos) instanceof GeneratorBlockEntity be) {
                be.interact(p);
            }
        }));
        shoot(name + "_status", 8);
        then(2, () -> onServer(server -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            BlockEntity sink = server.overworld().getBlockEntity(pos.east());
            if (sink instanceof SinkBlockEntity s) {
                p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "Energy sink next to the " + label + ": " + s.received() + " FE received"));
            }
        }));
        shoot(name + "_sink", 8);
        then(2, () -> moveTo(new Vec3(x + 0.5, y + 0.9, -2.1), new Vec3(x + 0.5, y + 0.5, 0.5)));
        shoot(name + "_look", 40);
    }

    /** A steel combustion generator feeding an iron battery feeding the sink: charge builds, then a face is switched off. */
    private static void batteryScene(int x) {
        BlockPos gen = new BlockPos(x, ground, 0);
        BlockPos bat = gen.east();
        BlockPos sinkPos = bat.east();
        then(4, () -> {
            server(String.format(Locale.ROOT, "setblock %d %d %d barelypowered:combustion_generator_steel[facing=north]", x, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelypowered:battery_iron", x + 1, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelypowered:test_energy_sink", x + 2, ground, 0));
            onServer(srv -> {
                if (srv.overworld().getBlockEntity(gen) instanceof GeneratorBlockEntity g) {
                    g.debugSet(new ItemStack(Items.CHARCOAL, 8), 0);
                }
            });
        });
        then(4, () -> moveTo(new Vec3(x + 1.5, ground + 1.1, -2.4), new Vec3(x + 1.5, ground + 0.5, 0.5)));
        shoot("battery_chain_a", 60);
        then(2, () -> onServer(srv -> {
            ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            if (srv.overworld().getBlockEntity(bat) instanceof BatteryBlockEntity b) {
                b.cycleFace(Direction.EAST);
                b.cycleFace(Direction.EAST);
                b.cycleFace(Direction.EAST);
                p.sendOverlayMessage(Component.translatable("message.barelypowered.battery", BatteryBlock.faceName(Direction.EAST), b.core().mode(Direction.EAST.ordinal()).label(), b.core().stored(), b.core().capacity()));
            }
        }));
        shoot("battery_chain_faceoff", 70);
        then(2, () -> moveTo(new Vec3(x + 1.5, ground + 1.1, -2.4), new Vec3(x + 1.5, ground + 0.5, 0.5)));
        then(2, () -> onServer(srv -> {
            ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            if (srv.overworld().getBlockEntity(bat) instanceof BatteryBlockEntity b && srv.overworld().getBlockEntity(sinkPos) instanceof SinkBlockEntity s) {
                LOGGER.info("BARELYPOWERED-SHOTS battery chain: battery stored={} sink received={}", b.core().stored(), s.received());
            }
        }));
        then(2, () -> moveTo(new Vec3(x + 1.0, ground + 0.9, -2.1), new Vec3(x + 1.0, ground + 0.5, 0.5)));
        shoot("battery_chain_look", 40);
    }

    /** True when the BarelyCables jar is in the game (it is a separate mod). */
    private static boolean haveCables() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().stream().anyMatch(k -> k.getNamespace().equals("barelycables"));
    }

    /** A steel combustion generator, two cables, an iron battery, a cable and the sink: does the power reach the end? */
    private static void cableScene(int x) {
        BlockPos gen = new BlockPos(x, ground, 0);
        BlockPos sinkPos = gen.offset(5, 0, 0);
        BlockPos bat = gen.offset(3, 0, 0);
        then(4, () -> {
            server(String.format(Locale.ROOT, "setblock %d %d %d barelypowered:combustion_generator_steel[facing=north]", x, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelycables:energy_cable_basic", x + 1, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelycables:energy_cable_iron", x + 2, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelypowered:battery_iron", x + 3, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelycables:energy_cable_steel", x + 4, ground, 0),
                    String.format(Locale.ROOT, "setblock %d %d %d barelypowered:test_energy_sink", x + 5, ground, 0));
            onServer(srv -> {
                if (srv.overworld().getBlockEntity(gen) instanceof GeneratorBlockEntity g) {
                    g.debugSet(new ItemStack(Items.CHARCOAL, 8), 0);
                }
            });
        });
        then(4, () -> moveTo(new Vec3(x + 2.5, ground + 1.2, -3.4), new Vec3(x + 2.5, ground + 0.4, 0.5)));
        shoot("cables_chain_a", 90);
        shoot("cables_chain_b", 6);
        then(2, () -> onServer(srv -> {
            if (srv.overworld().getBlockEntity(bat) instanceof BatteryBlockEntity b && srv.overworld().getBlockEntity(sinkPos) instanceof SinkBlockEntity s) {
                LOGGER.info("BARELYPOWERED-SHOTS cables chain: battery stored={} sink received={}", b.core().stored(), s.received());
            }
        }));
        then(2, () -> moveTo(new Vec3(x + 1.5, ground + 0.9, -1.8), new Vec3(x + 1.5, ground + 0.5, 0.5)));
        shoot("cables_cable_look", 50);
    }

    static void moveTo(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = player();
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.getChat().clearMessages(false);
            minecraft.getToastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer self = minecraft.player;
            LOGGER.info("BARELYPOWERED-SHOTS {} eye={} pitch={} yaw={}", name, self.getEyePosition(), self.getXRot(), self.getYRot());
            Screenshot.grab(minecraft.gameDirectory, "barelypowered_" + name + ".png", minecraft.getMainRenderTarget(), 1,
                    message -> LOGGER.info("BARELYPOWERED-SHOTS saved {}", name));
        });
    }

    static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    static void onServer(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> action.accept(server));
        }
    }

    static void server(String... commands) {
        onServer(server -> {
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        });
    }

    private static void selectSlot(int slot) {
        player().getInventory().setSelectedSlot(slot);
    }

    static {
        // Keep the class referenced so the dev flag check above is not optimized away.
        if (Dev.ENABLED && ENABLED) {
            LOGGER.info("BARELYPOWERED-SHOTS enabled");
        }
    }
}

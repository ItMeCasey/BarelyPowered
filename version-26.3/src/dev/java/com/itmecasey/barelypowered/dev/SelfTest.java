package com.itmecasey.barelypowered.dev;

import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import com.itmecasey.barelypowered.block.BatteryItem;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.BatteryBlock;
import com.itmecasey.barelypowered.FaceMode;
import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.Settings;
import com.itmecasey.barelypowered.Tiers;
import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check that every generator really makes energy and hands it to a neighbor. Runs once when the server has started if
 * the environment variable {@code BARELYPOWERED_SELFTEST} is set, and prints {@code SELFTEST PASS/FAIL} lines ending with
 * {@code SELFTEST DONE failures=N}. It ticks the block entities by hand, so it does not need the game loop.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyPowered");
    private static final int TICKS = 200;

    private interface Setup {
        void apply(ServerLevel level, BlockPos pos, GeneratorBlockEntity be);
    }

    private interface Check {
        boolean ok(GeneratorBlockEntity be, SinkBlockEntity sink);
    }

    private static ServerLevel level;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("BARELYPOWERED_SELFTEST") != null && Dev.ENABLED;
    }

    public static int run(MinecraftServer server) {
        level = server.overworld();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
        int failures = 0;
        BlockPos spawn = level.getRespawnData().pos();
        int x = spawn.getX() + 3;
        int z = spawn.getZ() + 3;
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

        failures += check("hand_crank", Kind.HAND_CRANK, 1, ground, x, z,
                (l, p, be) -> be.crank(), (be, sink) -> sink.received() == Settings.getInt("hand_crank.fe_per_use"));
        failures += check("combustion_stone_coal", Kind.COMBUSTION, 1, ground, x + 4, z,
                (l, p, be) -> be.debugSet(new ItemStack(Items.COAL, 1), 0),
                (be, sink) -> sink.received() >= 30 * TICKS - 60 && sink.received() <= 30 * TICKS);
        failures += check("combustion_steel_charcoal", Kind.COMBUSTION, 3, ground, x + 8, z,
                (l, p, be) -> be.debugSet(new ItemStack(Items.CHARCOAL, 1), 0),
                (be, sink) -> sink.received() >= 96 * TICKS - 200 && sink.received() <= 96 * TICKS);
        failures += check("combustion_no_fuel_makes_nothing", Kind.COMBUSTION, 1, ground, x + 12, z,
                (l, p, be) -> be.debugSet(ItemStack.EMPTY, 0), (be, sink) -> sink.received() == 0);
        failures += check("lava_stone", Kind.LAVA, 1, ground, x + 4, z + 4,
                (l, p, be) -> be.debugSet(ItemStack.EMPTY, 1000),
                (be, sink) -> sink.received() >= 80 * TICKS - 160 && sink.received() <= 80 * TICKS);
        failures += check("solar_tier1_noon", Kind.SOLAR, 1, ground + 40, x + 8, z + 4,
                (l, p, be) -> {
                }, (be, sink) -> Math.abs(be.output() - 6.0) < 1e-6 && sink.received() >= 6 * TICKS - 20 && sink.received() <= 6 * TICKS);
        failures += check("solar_tier2_noon", Kind.SOLAR, 2, ground + 40, x + 12, z + 4,
                (l, p, be) -> {
                }, (be, sink) -> Math.abs(be.output() - 24.0) < 1e-6 && sink.received() >= 24 * TICKS - 40);
        failures += check("wind_tier2_high", Kind.WIND, 2, 200, x + 4, z + 8,
                (l, p, be) -> {
                }, (be, sink) -> Math.abs(be.output() - 40.0) < 1e-6 && sink.received() >= 40 * TICKS - 80);
        failures += check("wind_with_a_block_above_makes_nothing", Kind.WIND, 1, 150, x + 8, z + 8,
                (l, p, be) -> l.setBlock(p.above(), Blocks.STONE.defaultBlockState(), 3),
                (be, sink) -> be.output() == 0.0);
        failures += check("water_wheel_flowing", Kind.WATER_WHEEL, 1, ground + 20, x + 12, z + 8,
                (l, p, be) -> {
                    l.setBlock(p.north(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), 3);
                    l.setBlock(p.above(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8), 3);
                }, (be, sink) -> Math.abs(be.output() - 52.5) < 1e-6 && sink.received() >= 52 * TICKS - 100);
        failures += check("water_wheel_still_water_gives_nothing", Kind.WATER_WHEEL, 1, ground + 20, x + 16, z + 8,
                (l, p, be) -> l.setBlock(p.north(), Blocks.WATER.defaultBlockState(), 3), (be, sink) -> be.output() == 0.0);
        failures += check("geothermal_two_lava", Kind.GEOTHERMAL, 1, ground + 20, x + 16, z + 4,
                (l, p, be) -> {
                    l.setBlock(p.north(), Blocks.LAVA.defaultBlockState(), 3);
                    l.setBlock(p.west(), Blocks.LAVA.defaultBlockState(), 3);
                }, (be, sink) -> Math.abs(be.output() - 32.0) < 1e-6 && sink.received() >= 32 * TICKS - 64);
        failures += batteryChain(ground, x, z);
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    /** A generator feeding a battery that feeds the sink: energy arrives, face modes stop it, charge survives breaking. */
    private static int batteryChain(int ground, int x, int z) {
        BlockPos gen = new BlockPos(x + 20, ground, z + 12);
        BlockPos bat = gen.east();
        BlockPos sinkPos = bat.east();
        int failures = 0;
        try {
            for (int dx = -1; dx <= 4; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    level.setBlock(gen.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            level.setBlock(gen, ModContent.blocks().get(Tiers.blockId(Kind.COMBUSTION, 1)).defaultBlockState(), 3);
            level.setBlock(bat, ModContent.batteries().get("battery_basic").defaultBlockState(), 3);
            level.setBlock(sinkPos, DevContent.SINK.defaultBlockState(), 3);
            GeneratorBlockEntity g = (GeneratorBlockEntity) level.getBlockEntity(gen);
            BatteryBlockEntity b = (BatteryBlockEntity) level.getBlockEntity(bat);
            SinkBlockEntity sink = (SinkBlockEntity) level.getBlockEntity(sinkPos);
            g.debugSet(new ItemStack(Items.COAL, 2), 0);
            for (int i = 0; i < 300; i++) {
                GeneratorBlockEntity.serverTick(level, gen, level.getBlockState(gen), g);
                BatteryBlockEntity.serverTick(level, bat, level.getBlockState(bat), b);
            }
            failures += verdict("battery_passes_generator_power_to_the_sink", sink.received() >= 30 * 300 - 100 && sink.received() <= 30 * 300,
                    "received=" + sink.received() + " stored=" + b.core().stored());

            b.core().setMode(Direction.EAST.ordinal(), FaceMode.OFF);
            long before = sink.received();
            int storedBefore = b.core().stored();
            for (int i = 0; i < 100; i++) {
                GeneratorBlockEntity.serverTick(level, gen, level.getBlockState(gen), g);
                BatteryBlockEntity.serverTick(level, bat, level.getBlockState(bat), b);
            }
            failures += verdict("battery_face_off_stops_output_and_charge_builds", sink.received() == before && b.core().stored() > storedBefore,
                    "received=" + sink.received() + " stored=" + b.core().stored());

            BlockState state = level.getBlockState(bat);
            failures += verdict("battery_gauge_and_comparator_follow_charge",
                    state.getValue(BatteryBlock.CHARGE) >= 1 && state.getAnalogOutputSignal(level, bat, Direction.NORTH) >= 1,
                    "charge=" + state.getValue(BatteryBlock.CHARGE) + " comparator=" + state.getAnalogOutputSignal(level, bat, Direction.NORTH));

            int stored = b.core().stored();
            java.util.List<ItemStack> drops = Block.getDrops(state, level, bat, b);
            failures += verdict("broken_battery_keeps_its_charge_in_the_item",
                    drops.size() == 1 && drops.get(0).getItem() instanceof BatteryItem && BatteryItem.charge(drops.get(0)) == stored && stored > 0,
                    "stored=" + stored + " drops=" + drops + " dropComponents=" + (drops.isEmpty() ? "" : drops.get(0).getComponents().toString()) + " beComponents=" + b.collectComponents());
            if (!drops.isEmpty()) {
                CompoundTag saved = b.saveWithoutMetadata(level.registryAccess());
                BatteryBlockEntity copy = new BatteryBlockEntity(bat, state);
                copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved));
                failures += verdict("battery_survives_saving_and_loading",
                        copy.core().stored() == stored && copy.core().mode(Direction.EAST.ordinal()) == FaceMode.OFF,
                        "stored=" + copy.core().stored());
            }
            for (int dx = -1; dx <= 4; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    level.setBlock(gen.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error in battery chain", e);
            failures += verdict("battery_chain_ran_without_error", false, e.toString());
        }
        return failures;
    }

    private static int verdict(String name, boolean ok, String detail) {
        return ok ? pass(name, detail) : fail(name, detail);
    }

    private static void clear(BlockPos pos) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static int check(String name, Kind kind, int tier, int y, int x, int z, Setup setup, Check check) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos sinkPos = pos.east();
        try {
            clear(pos);
            BlockState gen = ModContent.blocks().get(Tiers.blockId(kind, tier)).defaultBlockState();
            level.setBlock(pos, gen, 3);
            level.setBlock(sinkPos, DevContent.SINK.defaultBlockState(), 3);
            GeneratorBlockEntity be = (GeneratorBlockEntity) level.getBlockEntity(pos);
            SinkBlockEntity sink = (SinkBlockEntity) level.getBlockEntity(sinkPos);
            if (be == null || sink == null) {
                return fail(name, "block entity missing");
            }
            setup.apply(level, pos, be);
            for (int i = 0; i < TICKS; i++) {
                GeneratorBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
            }
            boolean ok = check.ok(be, sink);
            String detail = "output=" + be.output() + " received=" + sink.received() + " stored=" + be.buffer().stored();
            clear(pos);
            return ok ? pass(name, detail) : fail(name, detail);
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error in {}", name, e);
            return fail(name, e.toString());
        }
    }

    private static int pass(String name, String detail) {
        LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        return 0;
    }

    private static int fail(String name, String detail) {
        LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        return 1;
    }
}

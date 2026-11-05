package com.itmecasey.barelycables.dev;

import com.itmecasey.barelycables.CableSettings;
import com.itmecasey.barelycables.CablesContent;
import com.itmecasey.barelycables.Dev;
import com.itmecasey.barelycables.block.CableBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of the cables: a source feeds a sink through cables of each tier, mixed tiers are limited by the weakest, a
 * switched-off face cuts the line, a gap closed with a new cable joins two networks, and energy is conserved. Runs once when the
 * server has started if the environment variable BARELYCABLES_SELFTEST is set; prints SELFTEST PASS/FAIL lines and finally
 * SELFTEST DONE failures=N. It ticks the block entities by hand, so it does not need the game loop.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyCables");
    private static ServerLevel level;
    private static BlockPos base;
    private static int row;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("BARELYCABLES_SELFTEST") != null && Dev.ENABLED;
    }

    /** One test row: source at x0, then the given cables (tier 1 to 3, or 0 for an empty gap), then the sink. */
    private static final class Row {
        final List<BlockPos> cablePositions = new ArrayList<>();
        final BlockPos sourcePos;
        final BlockPos sinkPos;

        Row(int... tiers) {
            int z = base.getZ() + 3 * row++;
            sourcePos = new BlockPos(base.getX(), base.getY(), z);
            for (int dx = -1; dx <= tiers.length + 2; dx++) {
                level.setBlock(sourcePos.offset(dx, 0, 0), Blocks.AIR.defaultBlockState(), 2);
            }
            level.setBlock(sourcePos, DevContent.SOURCE.defaultBlockState(), 3);
            for (int i = 0; i < tiers.length; i++) {
                BlockPos pos = sourcePos.offset(i + 1, 0, 0);
                if (tiers[i] > 0) {
                    place(pos, tiers[i]);
                }
                cablePositions.add(pos);
            }
            sinkPos = sourcePos.offset(tiers.length + 1, 0, 0);
            level.setBlock(sinkPos, DevContent.SINK.defaultBlockState(), 3);
        }

        void place(BlockPos pos, int tier) {
            level.setBlock(pos, CablesContent.blocks().get(CableSettings.blockId(tier)).defaultBlockState(), 3);
        }

        void tick(int times) {
            for (int i = 0; i < times; i++) {
                for (BlockPos pos : cablePositions) {
                    if (level.getBlockEntity(pos) instanceof CableBlockEntity be) {
                        CableBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
                    }
                }
            }
        }

        TestBlockEntity source() {
            return (TestBlockEntity) level.getBlockEntity(sourcePos);
        }

        TestBlockEntity sink() {
            return (TestBlockEntity) level.getBlockEntity(sinkPos);
        }

        int buffered() {
            for (BlockPos pos : cablePositions) {
                if (level.getBlockEntity(pos) instanceof CableBlockEntity be) {
                    return be.networkStored();
                }
            }
            return 0;
        }

        void clear() {
            for (int dx = -1; dx <= cablePositions.size() + 2; dx++) {
                level.setBlock(sourcePos.offset(dx, 0, 0), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    public static int run(MinecraftServer server) {
        level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        base = new BlockPos(spawn.getX() + 3, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX() + 3, spawn.getZ() + 3) + 20, spawn.getZ() + 3);
        int failures = 0;
        int ticks = 100;

        Row basic = new Row(1, 1, 1);
        basic.tick(ticks);
        long got = basic.sink().received();
        failures += verdict("basic_cables_carry_400_per_tick", got >= 400L * (ticks - 4) && got <= 400L * ticks, "received=" + got);
        failures += verdict("energy_is_conserved_through_the_cables", basic.source().sent() == got + basic.buffered(),
                "sent=" + basic.source().sent() + " received=" + got + " buffered=" + basic.buffered());
        basic.clear();

        Row iron = new Row(2, 2, 2);
        iron.tick(ticks);
        got = iron.sink().received();
        failures += verdict("iron_cables_carry_1600_per_tick", got >= 1600L * (ticks - 4) && got <= 1600L * ticks, "received=" + got);
        iron.clear();

        Row steel = new Row(3, 3);
        steel.tick(ticks);
        got = steel.sink().received();
        failures += verdict("steel_cables_carry_6400_per_tick", got >= 6400L * (ticks - 4) && got <= 6400L * ticks, "received=" + got);
        steel.clear();

        Row mixed = new Row(3, 1, 3);
        mixed.tick(ticks);
        got = mixed.sink().received();
        failures += verdict("mixed_tiers_are_limited_by_the_weakest", got >= 400L * (ticks - 4) && got <= 400L * ticks, "received=" + got);
        mixed.clear();

        Row cut = new Row(1, 1, 1);
        cut.tick(30);
        BlockPos middle = cut.cablePositions.get(1);
        CableBlockEntity middleBe = (CableBlockEntity) level.getBlockEntity(middle);
        middleBe.toggleFace(Direction.EAST);
        cut.tick(10);
        long afterCut = cut.sink().received();
        cut.tick(40);
        failures += verdict("a_switched_off_face_cuts_the_line", cut.sink().received() == afterCut,
                "received=" + cut.sink().received() + " atCut=" + afterCut);
        middleBe.toggleFace(Direction.EAST);
        cut.tick(40);
        failures += verdict("switching_the_face_back_on_restores_the_flow", cut.sink().received() > afterCut,
                "received=" + cut.sink().received());
        cut.clear();

        Row gap = new Row(1, 0, 1);
        gap.tick(30);
        long before = gap.sink().received();
        gap.place(gap.cablePositions.get(1), 1);
        gap.tick(40);
        failures += verdict("a_gap_gives_nothing_and_closing_it_joins_the_networks", before == 0 && gap.sink().received() > 0,
                "before=" + before + " after=" + gap.sink().received());
        gap.clear();

        Row idle = new Row(1, 1);
        level.setBlock(idle.sourcePos, Blocks.AIR.defaultBlockState(), 3);
        idle.tick(60);
        failures += verdict("cables_with_no_source_make_nothing", idle.sink().received() == 0 && idle.buffered() == 0,
                "received=" + idle.sink().received());
        idle.clear();

        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    private static int verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
            return 0;
        }
        LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        return 1;
    }
}

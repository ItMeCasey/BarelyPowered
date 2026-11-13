package com.itmecasey.barelypowered;

import com.itmecasey.barelypowered.block.BatteryBlock;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorBlock;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.barelypowered.script.PowerApi;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Lets scripts read generators and batteries in the running server. The server is noted from the first generator tick. */
public final class ScriptWorld implements PowerApi.World {
    private static WeakReference<MinecraftServer> server = new WeakReference<>(null);
    private static final ScriptWorld INSTANCE = new ScriptWorld();

    private ScriptWorld() {
    }

    /** Called from a generator's tick: remembers the running server and makes the API read from it. */
    public static void touch(Level level) {
        MinecraftServer s = level.getServer();
        if (s != null && server.get() != s) {
            server = new WeakReference<>(s);
            PowerApi.attach(INSTANCE);
        }
    }

    public static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }

    private static String id(net.minecraft.world.level.block.Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static ServerLevel level(String dimension, int x, int y, int z) {
        MinecraftServer s = server.get();
        if (s == null) {
            return null;
        }
        for (ServerLevel l : s.getAllLevels()) {
            if (dimensionId(l).equals(dimension)) {
                return l.hasChunkAt(new BlockPos(x, y, z)) ? l : null;
            }
        }
        return null;
    }

    @Override
    public PowerApi.Generator generatorAt(String dimension, int x, int y, int z) {
        ServerLevel l = level(dimension, x, y, z);
        if (l == null || !(l.getBlockEntity(new BlockPos(x, y, z)) instanceof GeneratorBlockEntity be)) {
            return null;
        }
        ItemStack fuel = be.fuelStack();
        return new PowerApi.Generator(be.kind().id(), be.tier(), id(be.getBlockState().getBlock()), be.output(), be.buffer().stored(),
                be.buffer().capacity(), be.getBlockState().getValue(GeneratorBlock.LIT), be.note(),
                fuel.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(fuel.getItem()).toString(), fuel.getCount(), be.lavaMillibuckets(),
                be.kind() == Kind.LAVA ? be.lavaCapacity() : 0, be.burnTicksLeft());
    }

    @Override
    public PowerApi.Battery batteryAt(String dimension, int x, int y, int z) {
        ServerLevel l = level(dimension, x, y, z);
        if (l == null || !(l.getBlockEntity(new BlockPos(x, y, z)) instanceof BatteryBlockEntity be)) {
            return null;
        }
        BatteryCore core = be.core();
        return new PowerApi.Battery(((BatteryBlock) be.getBlockState().getBlock()).tier(), id(be.getBlockState().getBlock()), core.stored(),
                core.capacity(), core.rate());
    }
}

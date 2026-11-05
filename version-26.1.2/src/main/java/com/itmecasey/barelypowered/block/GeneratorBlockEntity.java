package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.EnergyPusher;
import com.itmecasey.barelypowered.FeBuffer;
import com.itmecasey.barelypowered.Fuel;
import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/** The working part of every generator. What it does each tick depends on the block's {@link Kind}. */
public class GeneratorBlockEntity extends BlockEntity {
    /** Set by the loader layer; hands energy to neighboring blocks. */
    public static EnergyPusher pusher = (level, pos, side, amount) -> 0;

    private final Kind kind;
    private final int tier;
    private final FeBuffer buffer;
    private final PowerMath.Accumulator accumulator = new PowerMath.Accumulator();

    private ItemStack fuel = ItemStack.EMPTY;
    private int lavaMb;
    private int burnLeft;
    private int burnTotal;
    private int burnRate;

    /** Energy per tick being made right now (a fractional value for sun and wind). */
    private double rate;
    /** Why the output is what it is, as a lang key suffix. */
    private String note = "idle";
    private int crankCooldown;
    private int crankGlow;
    private int age;

    private int lastSyncKey = Integer.MIN_VALUE;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.GENERATOR, pos, state);
        GeneratorBlock block = (GeneratorBlock) state.getBlock();
        this.kind = block.kind();
        this.tier = block.tier();
        this.buffer = new FeBuffer(PowerMath.bufferSize(kind, tier));
    }

    public Kind kind() {
        return kind;
    }

    public int tier() {
        return tier;
    }

    public FeBuffer buffer() {
        return buffer;
    }

    /** Energy per tick being made, for the status line and the look-at plugin. */
    public double output() {
        return rate;
    }

    public ItemStack fuelStack() {
        return fuel;
    }

    public int lavaMillibuckets() {
        return lavaMb;
    }

    public int burnTicksLeft() {
        return burnLeft;
    }

    public String note() {
        return note;
    }

    public int lavaCapacity() {
        return Settings.getInt("lava.tank_buckets") * 1000;
    }

    // ---- Interaction ----

    /** True if this generator would take the item in hand. */
    public boolean acceptsItem(ItemStack stack) {
        if (kind == Kind.COMBUSTION) {
            Fuel f = FuelRules.of(stack);
            return f != null && (fuel.isEmpty() || ItemStack.isSameItemSameComponents(fuel, stack))
                    && fuel.getCount() < stack.getMaxStackSize();
        }
        if (kind == Kind.LAVA) {
            return stack.is(Items.LAVA_BUCKET) && lavaMb + 1000 <= lavaCapacity();
        }
        return false;
    }

    public void insertFrom(Player player, InteractionHand hand, ItemStack stack) {
        if (kind == Kind.COMBUSTION) {
            int room = stack.getMaxStackSize() - fuel.getCount();
            int move = Math.min(room, stack.getCount());
            if (fuel.isEmpty()) {
                fuel = stack.copyWithCount(move);
            } else {
                fuel.grow(move);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(move);
            }
        } else if (kind == Kind.LAVA) {
            lavaMb += 1000;
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
        }
        syncNow();
    }

    /** Right-click with an empty hand: the hand crank turns, anything else reports its status. */
    public void interact(Player player) {
        if (kind == Kind.HAND_CRANK) {
            if (crank()) {
                player.causeFoodExhaustion(0.05F);
            }
        } else if (kind == Kind.COMBUSTION && player.isShiftKeyDown() && !fuel.isEmpty()) {
            ItemStack out = takeFuel();
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            syncNow();
        }
        player.sendOverlayMessage(statusLine());
    }

    /** Turns the hand crank once; false if it is still cooling down or full. */
    public boolean crank() {
        if (kind != Kind.HAND_CRANK || crankCooldown > 0 || buffer.space() <= 0) {
            return false;
        }
        buffer.fill(Settings.getInt("hand_crank.fe_per_use"));
        crankCooldown = Settings.getInt("hand_crank.cooldown_ticks");
        crankGlow = 12;
        note = "cranking";
        syncNow();
        return true;
    }

    /** For the self test: puts fuel in or sets the lava level directly. */
    public void debugSet(ItemStack fuelStack, int lava) {
        this.fuel = fuelStack;
        this.lavaMb = lava;
    }

    public ItemStack takeFuel() {
        ItemStack out = fuel;
        fuel = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    /** One line for the action bar: output, stored energy, fuel, and why. */
    public Component statusLine() {
        MutableComponent line = Component.translatable("message.barelypowered.status", formatRate(rate),
                buffer.stored(), buffer.capacity());
        if (kind == Kind.COMBUSTION) {
            line.append(Component.translatable("message.barelypowered.fuel", fuel.isEmpty() ? Component.translatable("message.barelypowered.none") : fuel.getHoverName(),
                    fuel.getCount(), burnLeft / 20));
        } else if (kind == Kind.LAVA) {
            line.append(Component.translatable("message.barelypowered.lava", lavaMb, lavaCapacity(), burnLeft / 20));
        }
        line.append(Component.translatable("message.barelypowered.note." + note));
        return line;
    }

    public static String formatRate(double perTick) {
        return perTick >= 10 || perTick == Math.rint(perTick) ? String.valueOf(Math.round(perTick)) : String.format(java.util.Locale.ROOT, "%.1f", perTick);
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        age++;
        boolean producing;
        switch (kind) {
            case HAND_CRANK -> {
                if (crankCooldown > 0) {
                    crankCooldown--;
                }
                if (crankGlow > 0) {
                    crankGlow--;
                }
                rate = 0;
                producing = crankGlow > 0;
            }
            case COMBUSTION -> producing = tickCombustion();
            case LAVA -> producing = tickLava();
            default -> producing = tickEnvironment(level, pos);
        }
        if (state.getValue(GeneratorBlock.LIT) != producing) {
            level.setBlock(pos, state.setValue(GeneratorBlock.LIT, producing), 3);
        }
        push(level, pos);
        int key = syncKey();
        if (age % 10 == 0 && key != lastSyncKey) {
            syncNow();
        }
    }

    private boolean tickCombustion() {
        if (burnLeft <= 0 && !fuel.isEmpty()) {
            Fuel f = FuelRules.of(fuel);
            if (f == null) {
                return false;
            }
            int out = PowerMath.combustionOutput(f, tier);
            if (buffer.space() >= out) {
                fuel.shrink(1);
                burnTotal = PowerMath.combustionBurnTicks(f, tier);
                burnLeft = burnTotal;
                burnRate = out;
                setChanged();
            }
        }
        if (burnLeft > 0 && buffer.space() >= burnRate) {
            buffer.fill(burnRate);
            burnLeft--;
            rate = burnRate;
            note = "burning";
            setChanged();
            return true;
        }
        rate = 0;
        note = burnLeft > 0 ? "full" : fuel.isEmpty() ? "no_fuel" : "idle";
        return false;
    }

    private boolean tickLava() {
        if (burnLeft <= 0 && lavaMb >= 1000) {
            int out = PowerMath.lavaOutput(tier);
            if (buffer.space() >= out) {
                lavaMb -= 1000;
                burnTotal = PowerMath.lavaBurnTicks(tier);
                burnLeft = burnTotal;
                burnRate = out;
                setChanged();
            }
        }
        if (burnLeft > 0 && buffer.space() >= burnRate) {
            buffer.fill(burnRate);
            burnLeft--;
            rate = burnRate;
            note = "burning";
            setChanged();
            return true;
        }
        rate = 0;
        note = burnLeft > 0 ? "full" : "no_lava";
        return false;
    }

    private int windTurbines;
    private double windBlocked;

    private boolean tickEnvironment(Level level, BlockPos pos) {
        int period = kind == Kind.SOLAR ? 20 : kind == Kind.WIND ? 40 : 20;
        if (age % period == 1 || age == 1) {
            scan(level, pos);
        }
        int made = accumulator.add(rate);
        if (made > 0) {
            buffer.fill(made);
            setChanged();
        }
        return rate > 0 && buffer.space() > 0 || rate > 0 && made > 0;
    }

    /** Nothing solid or wet above: read from the height map so the answer is exact even right after a block change. */
    private static boolean skyOpen(Level level, BlockPos pos) {
        return level.dimensionType().hasSkyLight()
                && pos.getY() + 1 >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }

    private void scan(Level level, BlockPos pos) {
        switch (kind) {
            case SOLAR -> {
                boolean sky = skyOpen(level, pos);
                rate = PowerMath.solarOutput(tier, level.getOverworldClockTime(), sky, level.isRainingAt(pos.above()), level.isThundering());
                note = !sky ? "no_sky" : rate <= 0 ? "night" : level.isRainingAt(pos.above()) ? "weather" : "ok";
            }
            case WIND -> {
                boolean sky = skyOpen(level, pos);
                if (age % 200 == 1 || age == 1) {
                    scanWind(level, pos);
                }
                rate = PowerMath.windOutput(tier, pos.getY(), sky, windBlocked, level.isRainingAt(pos.above()),
                        level.isThundering(), windTurbines);
                note = !sky ? "no_sky" : windBlocked > 0.5 ? "blocked" : windTurbines > 0 ? "crowded"
                        : PowerMath.windHeightFactor(pos.getY()) < 0.5 ? "low" : "ok";
            }
            case WATER_WHEEL -> {
                double flow = 0;
                for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    FluidState fs = level.getFluidState(pos.relative(d));
                    if (fs.is(FluidTags.WATER)) {
                        boolean falling = fs.hasProperty(FlowingFluid.FALLING) && fs.getValue(FlowingFluid.FALLING);
                        flow += PowerMath.waterFlow(fs.getAmount(), fs.isSource(), falling);
                    }
                }
                rate = PowerMath.waterWheelOutput(flow);
                note = rate > 0 ? "ok" : "no_flow";
            }
            case GEOTHERMAL -> {
                double heat = 0;
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                            FluidState fs = level.getFluidState(p);
                            if (fs.is(FluidTags.LAVA)) {
                                heat += Settings.getDouble(fs.isSource() ? "geothermal.lava_source_heat" : "geothermal.lava_flow_heat");
                            } else if (level.getBlockState(p).is(Blocks.MAGMA_BLOCK)) {
                                heat += Settings.getDouble("geothermal.magma_heat");
                            }
                        }
                    }
                }
                rate = PowerMath.geothermalOutput(heat);
                note = rate > 0 ? "ok" : "no_heat";
            }
            default -> rate = 0;
        }
    }

    /** Counts solid blocks near the blades and other turbines nearby; slow, so it runs every ten seconds. */
    private void scanWind(Level level, BlockPos pos) {
        int solid = 0;
        int total = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // The centre column and the four side neighbors are where cables and machines sit, so they do not count.
                if (Math.abs(dx) + Math.abs(dz) <= 1) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    total++;
                    if (level.hasChunkAt(p) && !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                        solid++;
                    }
                }
            }
        }
        windBlocked = total == 0 ? 0 : (double) solid / total;
        int radius = Settings.getInt("wind.turbine_radius");
        int others = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.hasChunkAt(p) && level.getBlockState(p).getBlock() instanceof GeneratorBlock g && g.kind() == Kind.WIND) {
                        others++;
                    }
                }
            }
        }
        windTurbines = others;
    }

    private void push(Level level, BlockPos pos) {
        if (buffer.stored() <= 0 || !Settings.getBool("general.push_to_neighbors")) {
            return;
        }
        int budget = PowerMath.transferRate(kind, tier);
        for (Direction d : Direction.values()) {
            if (budget <= 0 || buffer.stored() <= 0) {
                break;
            }
            int offer = Math.min(budget, buffer.stored());
            int taken = pusher.push(level, pos.relative(d), d.getOpposite(), offer);
            if (taken > 0) {
                buffer.drain(taken, false);
                budget -= taken;
                setChanged();
            }
        }
    }

    // ---- Saving and syncing ----

    private int syncKey() {
        return (int) (rate * 10) * 31 + buffer.stored() / 10 * 7 + fuel.getCount() * 13 + lavaMb / 50 + burnLeft / 20 + note.hashCode();
    }

    private void syncNow() {
        setChanged();
        lastSyncKey = syncKey();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", buffer.stored());
        output.putInt("lava", lavaMb);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putInt("burn_rate", burnRate);
        output.putDouble("rate", rate);
        output.putDouble("carry", accumulator.carry());
        output.putString("note", note);
        if (!fuel.isEmpty()) {
            output.store("fuel", ItemStack.CODEC, fuel);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.set(input.getIntOr("energy", 0));
        lavaMb = input.getIntOr("lava", 0);
        burnLeft = input.getIntOr("burn_left", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        burnRate = input.getIntOr("burn_rate", 0);
        rate = input.getDoubleOr("rate", 0.0);
        accumulator.set(input.getDoubleOr("carry", 0.0));
        note = input.getStringOr("note", "idle");
        fuel = input.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    /** Breaking the block drops the fuel that is still inside. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ItemStack stack = takeFuel();
        if (level != null && !stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

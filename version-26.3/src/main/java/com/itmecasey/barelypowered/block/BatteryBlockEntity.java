package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.BatteryCore;
import com.itmecasey.barelypowered.FaceMode;
import com.itmecasey.barelypowered.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The working part of a battery. The charge and face rules live in {@link BatteryCore}. */
public class BatteryBlockEntity extends BlockEntity {
    /** The key in the item custom data that holds the charge a broken battery keeps. */
    public static final String CHARGE_KEY = "charge";

    private final BatteryCore core;
    private int age;
    private int lastComparator = -1;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BATTERY, pos, state);
        this.core = new BatteryCore(((BatteryBlock) state.getBlock()).tier());
    }

    public BatteryCore core() {
        return core;
    }

    public FaceMode cycleFace(Direction face) {
        FaceMode mode = core.cycle(face.ordinal());
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return mode;
    }

    /** Takes energy in through a side (null for no particular side); used by the loader energy interfaces. */
    public int receive(@Nullable Direction side, int amount, boolean simulate) {
        int taken = core.receive(side == null ? -1 : side.ordinal(), amount, simulate);
        if (taken > 0 && !simulate) {
            setChanged();
        }
        return taken;
    }

    /** Gives energy out through a side (null for no particular side). */
    public int extract(@Nullable Direction side, int amount, boolean simulate) {
        int given = core.extract(side == null ? -1 : side.ordinal(), amount, simulate);
        if (given > 0 && !simulate) {
            setChanged();
        }
        return given;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        age++;
        core.startTick();
        if (core.stored() > 0) {
            for (Direction d : Direction.values()) {
                int offer = core.extract(d.ordinal(), core.rate(), true);
                if (offer <= 0) {
                    continue;
                }
                int taken = GeneratorBlockEntity.pusher.push(level, pos.relative(d), d.getOpposite(), offer);
                if (taken > 0) {
                    core.extract(d.ordinal(), taken, false);
                    setChanged();
                }
            }
        }
        int fill = BatteryCore.fillLevel(core.stored(), core.capacity(), 8);
        if (state.getValue(BatteryBlock.CHARGE) != fill) {
            level.setBlock(pos, state.setValue(BatteryBlock.CHARGE, fill), 3);
        }
        int comparator = BatteryCore.comparator(core.stored(), core.capacity());
        if (comparator != lastComparator) {
            if (lastComparator >= 0) {
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            lastComparator = comparator;
        }
    }

    // ---- Charge kept in the item ----

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        CompoundTag tag = new CompoundTag();
        tag.putInt(CHARGE_KEY, core.stored());
        builder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            core.setStored(data.copyTag().getIntOr(CHARGE_KEY, 0));
        }
    }

    // ---- Saving and syncing ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", core.stored());
        output.putInt("faces", core.packModes());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        core.setStored(input.getIntOr("energy", 0));
        core.unpackModes(input.getIntOr("faces", 0));
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

package com.itmecasey.barelycables.block;

import com.itmecasey.barelycables.CableLink;
import com.itmecasey.barelycables.CableMath;
import com.itmecasey.barelycables.CableSettings;
import com.itmecasey.barelycables.CablesContent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A cable. It keeps its face switches, joins its neighbors into a {@link CableNetwork} and exposes the network buffer as energy. */
public class CableBlockEntity extends BlockEntity {
    /** Set by the loader layer. */
    public static CableLink link = (level, pos, face) -> null;

    private final CableMath.Faces faces = new CableMath.Faces();
    private final int tier;
    CableNetwork network;
    private int carry;
    private int age;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(CablesContent.CABLE, pos, state);
        this.tier = ((CableBlock) state.getBlock()).tier();
    }

    public int tier() {
        return tier;
    }

    public boolean faceDisabled(Direction d) {
        return faces.isDisabled(d.ordinal());
    }

    /** Energy in the network buffer, for tooltips. */
    public int networkStored() {
        return network == null ? 0 : network.buffer.stored();
    }

    public int networkCapacity() {
        return network == null ? 0 : network.buffer.capacity();
    }

    /** Puts the network buffer back to an earlier amount (a transaction that was rolled back). */
    public void restoreStored(int amount) {
        if (network != null) {
            network.buffer.set(amount);
        }
    }

    public int networkCables() {
        return network == null ? 1 : network.cables;
    }

    public int networkEndpoints() {
        return network == null ? 0 : network.endpoints.size();
    }

    public int networkRate() {
        return network == null ? CableSettings.rate(tier) : network.rate;
    }

    // ---- Connections ----

    /** Works out which faces have an arm: a cable that is not switched off, or a neighbor that offers Forge Energy. */
    public void refreshConnections() {
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        BlockState next = state;
        for (Direction d : Direction.values()) {
            next = next.setValue(CableBlock.property(d), connects(level, d));
        }
        if (next != state) {
            level.setBlock(worldPosition, next, 3);
            markNetworkDirty();
            wakeNeighborCables();
        }
    }

    private boolean connects(Level level, Direction d) {
        if (faces.isDisabled(d.ordinal())) {
            return false;
        }
        BlockPos n = worldPosition.relative(d);
        if (level.getBlockState(n).getBlock() instanceof CableBlock) {
            return !(level.getBlockEntity(n) instanceof CableBlockEntity other) || !other.faces.isDisabled(d.getOpposite().ordinal());
        }
        return link.portAt(level, n, d.getOpposite()) != null;
    }

    /** Flips one face on or off (right-click on an arm). Returns true if the face is now switched off. */
    public boolean toggleFace(Direction d) {
        boolean off = faces.toggle(d.ordinal());
        setChanged();
        refreshConnections();
        markNetworkDirty();
        wakeNeighborCables();
        return off;
    }

    void markNetworkDirty() {
        if (network != null) {
            network.dirty = true;
        }
    }

    private void wakeNeighborCables() {
        if (level == null) {
            return;
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof CableBlockEntity other) {
                other.markNetworkDirty();
            }
        }
    }

    private boolean isCableFace(Direction d) {
        return getBlockState().getValue(CableBlock.property(d)) && level.getBlockEntity(worldPosition.relative(d)) instanceof CableBlockEntity other
                && !other.faces.isDisabled(d.getOpposite().ordinal()) && other.getBlockState().getValue(CableBlock.property(d.getOpposite()));
    }

    // ---- Network ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, CableBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        if (age++ == 0) {
            refreshConnections();
        }
        if (network == null || network.dirty || network.leader == null || network.leader.isRemoved()) {
            rebuild(level);
        }
        if (network.leader == this) {
            network.run(level);
        }
    }

    /** Finds the connected cables, takes over the energy of the networks they came from, and elects the leader. */
    private void rebuild(ServerLevel level) {
        List<CableBlockEntity> members = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<CableBlockEntity> queue = new ArrayDeque<>();
        queue.add(this);
        seen.add(worldPosition);
        while (!queue.isEmpty()) {
            CableBlockEntity be = queue.poll();
            members.add(be);
            for (Direction d : Direction.values()) {
                if (!be.isCableFace(d)) {
                    continue;
                }
                BlockPos n = be.worldPosition.relative(d);
                if (seen.add(n) && level.getBlockEntity(n) instanceof CableBlockEntity next) {
                    queue.add(next);
                }
            }
        }
        int rate = Integer.MAX_VALUE;
        CableBlockEntity leader = this;
        long best = worldPosition.asLong();
        int energy = 0;
        IdentityHashMap<CableNetwork, Boolean> olds = new IdentityHashMap<>();
        CableNetwork fresh = new CableNetwork(0, 0);
        List<CableNetwork.Endpoint> endpoints = new ArrayList<>();
        for (CableBlockEntity m : members) {
            rate = Math.min(rate, CableSettings.rate(m.tier));
            if (m.worldPosition.asLong() < best) {
                best = m.worldPosition.asLong();
                leader = m;
            }
            energy += m.carry;
            m.carry = 0;
            if (m.network != null && olds.put(m.network, Boolean.TRUE) == null) {
                energy += m.network.buffer.stored();
                m.network.buffer.set(0);
            }
            for (Direction d : Direction.values()) {
                if (m.getBlockState().getValue(CableBlock.property(d)) && !m.isCableFace(d)) {
                    endpoints.add(new CableNetwork.Endpoint(m.worldPosition, d));
                }
            }
        }
        CableNetwork net = new CableNetwork(rate, members.size());
        net.buffer.keep(energy);
        net.endpoints.addAll(endpoints);
        net.leader = leader;
        for (CableBlockEntity m : members) {
            m.network = net;
        }
    }

    // ---- Energy through the cable, for the loader energy interfaces ----

    /** Energy another mod's pipe or machine puts into the network buffer through a face. */
    public int receive(@Nullable Direction side, int amount, boolean simulate) {
        if (network == null || (side != null && faces.isDisabled(side.ordinal()))) {
            return 0;
        }
        int taken = network.buffer.insert(Math.min(amount, network.rate), simulate);
        if (taken > 0 && !simulate) {
            network.wake();
        }
        return taken;
    }

    /** Energy another mod's pipe or machine draws from the network buffer through a face. */
    public int extract(@Nullable Direction side, int amount, boolean simulate) {
        if (network == null || (side != null && faces.isDisabled(side.ordinal()))) {
            return 0;
        }
        return network.buffer.extract(Math.min(amount, network.rate), simulate);
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("off", faces.bits());
        // The leader keeps the network buffer so a reload does not lose it.
        output.putInt("carry", network != null && network.leader == this ? network.buffer.stored() : carry);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        faces.setBits(input.getIntOr("off", 0));
        carry = input.getIntOr("carry", 0);
    }
}

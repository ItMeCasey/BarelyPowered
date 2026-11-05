package com.itmecasey.barelycables.block;

import com.itmecasey.barelycables.CableMath;
import com.itmecasey.barelycables.CableSettings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * One connected run of cables. The lowest-positioned cable is the leader and moves the energy for everyone once per run, so a
 * long cable costs no more per tick than a short one. A quiet network checks less often (see {@link #run}).
 */
final class CableNetwork {
    /** A face of a cable that touches something other than another cable. */
    record Endpoint(BlockPos cable, Direction face) {
    }

    final CableMath.Buffer buffer;
    final List<Endpoint> endpoints = new ArrayList<>();
    final int rate;
    final int cables;
    CableBlockEntity leader;
    boolean dirty;
    private int idle;
    private int interval = 1;
    private int sinceRun;
    private int rotation;

    CableNetwork(int rate, int cables) {
        this.rate = rate;
        this.cables = cables;
        this.buffer = new CableMath.Buffer(rate * Math.max(1, CableSettings.getInt("cable.buffer_ticks")));
    }

    /** Moves energy among the network's neighbors and its own buffer. */
    void run(ServerLevel level) {
        if (++sinceRun < interval) {
            return;
        }
        sinceRun = 0;
        List<CableMath.Port> ports = new ArrayList<>(endpoints.size() + 1);
        for (Endpoint e : endpoints) {
            CableMath.Port port = CableBlockEntity.link.portAt(level, e.cable().relative(e.face()), e.face().getOpposite());
            if (port != null) {
                ports.add(port);
            }
        }
        ports.add(buffer);
        CableMath.Result result = CableMath.move(ports, rate * interval, rotation++);
        buffer.keep(result.spill());
        if (result.moved() > 0 || buffer.stored() > 0) {
            idle = 0;
            interval = 1;
        } else {
            idle++;
            interval = idle > 40 ? 10 : idle > 10 ? 4 : 1;
        }
    }

    /** Called when something near the network changed: it runs every tick again until it is quiet. */
    void wake() {
        idle = 0;
        interval = 1;
    }
}

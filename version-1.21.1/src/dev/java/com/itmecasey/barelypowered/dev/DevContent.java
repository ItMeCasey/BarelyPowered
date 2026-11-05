package com.itmecasey.barelypowered.dev;

import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.ModContent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The test energy sink: a block that accepts any energy and counts it. Built only with -PdevTools. */
public final class DevContent {
    public static SinkBlock SINK;
    public static BlockEntityType<SinkBlockEntity> SINK_ENTITY;

    private DevContent() {
    }

    /** Called by ModContent while it is being built (see Dev.call). */
    public static void register() {
        SINK = new SinkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0F));
        SINK_ENTITY = BlockEntityType.Builder.of(SinkBlockEntity::new, SINK).build(null);
        ModContent.addExtra("test_energy_sink", SINK, new BlockItem(SINK, new Item.Properties()), SINK_ENTITY);
    }
}

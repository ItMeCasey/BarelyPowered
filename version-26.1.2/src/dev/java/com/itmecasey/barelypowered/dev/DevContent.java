package com.itmecasey.barelypowered.dev;

import com.itmecasey.barelypowered.Dev;
import com.itmecasey.barelypowered.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import java.util.Set;
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
        SINK = new SinkBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, ModContent.id("test_energy_sink"))).mapColor(MapColor.COLOR_PURPLE).strength(1.0F));
        SINK_ENTITY = new BlockEntityType<>(SinkBlockEntity::new, Set.of(SINK));
        ModContent.addExtra("test_energy_sink", SINK, new BlockItem(SINK, ModContent.itemProps("test_energy_sink").useBlockDescriptionPrefix()), SINK_ENTITY);
    }
}

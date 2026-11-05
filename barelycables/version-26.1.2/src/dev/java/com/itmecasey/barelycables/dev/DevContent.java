package com.itmecasey.barelycables.dev;

import com.itmecasey.barelycables.CablesContent;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The test source and sink blocks. Built only with -PdevTools. */
public final class DevContent {
    public static TestBlock SOURCE;
    public static TestBlock SINK;
    public static BlockEntityType<TestBlockEntity> TEST_ENTITY;

    private DevContent() {
    }

    /** Called by CablesContent while it is being built (see Dev.call). */
    public static void register() {
        SOURCE = new TestBlock(true, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, CablesContent.id("test_energy_source"))).mapColor(MapColor.COLOR_GREEN).strength(1.0F));
        SINK = new TestBlock(false, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, CablesContent.id("test_energy_sink"))).mapColor(MapColor.COLOR_PURPLE).strength(1.0F));
        TEST_ENTITY = new BlockEntityType<>(TestBlockEntity::new, Set.of(SOURCE, SINK));
        CablesContent.addExtra("test_energy_source", SOURCE, new BlockItem(SOURCE, CablesContent.itemProps("test_energy_source").useBlockDescriptionPrefix()), TEST_ENTITY);
        CablesContent.addExtra("test_energy_sink", SINK, new BlockItem(SINK, CablesContent.itemProps("test_energy_sink").useBlockDescriptionPrefix()), null);
    }
}

package com.itmecasey.barelypowered;

import com.itmecasey.barelypowered.block.GeneratorBlock;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorItem;
import com.itmecasey.barelypowered.block.SinkBlock;
import com.itmecasey.barelypowered.block.SinkBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every block, item and block entity type the mod adds, built once and handed to the loader's registry event. */
public final class ModContent {
    public static final String MODID = "barelypowered";

    private static final Map<String, GeneratorBlock> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final BlockEntityType<GeneratorBlockEntity> GENERATOR;
    public static final Item STEEL_INGOT;
    /** True in dev runs and self tests; adds the energy sink test block. */
    public static final boolean DEV = Dev.ENABLED;
    public static final SinkBlock SINK;
    public static final BlockEntityType<SinkBlockEntity> SINK_ENTITY;
    private static final Map<String, Block> EXTRA_BLOCKS = new LinkedHashMap<>();

    static {
        for (Kind kind : Kind.values()) {
            for (int tier = 1; tier <= kind.tierCount(); tier++) {
                String id = Tiers.blockId(kind, tier);
                BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, id(id))).mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
                GeneratorBlock block = new GeneratorBlock(kind, tier, props);
                BLOCKS.put(id, block);
                ITEMS.put(id, new GeneratorItem(block, itemProps(id).useBlockDescriptionPrefix()));
            }
        }
        STEEL_INGOT = new Item(itemProps("steel_ingot"));
        ITEMS.put("steel_ingot", STEEL_INGOT);
        if (DEV) {
            SINK = new SinkBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("test_energy_sink"))).mapColor(MapColor.COLOR_PURPLE).strength(1.0F));
            EXTRA_BLOCKS.put("test_energy_sink", SINK);
            ITEMS.put("test_energy_sink", new net.minecraft.world.item.BlockItem(SINK, itemProps("test_energy_sink").useBlockDescriptionPrefix()));
            SINK_ENTITY = new BlockEntityType<>(SinkBlockEntity::new, java.util.Set.of(SINK));
        } else {
            SINK = null;
            SINK_ENTITY = null;
        }
        GENERATOR = new BlockEntityType<>(GeneratorBlockEntity::new, new java.util.HashSet<Block>(BLOCKS.values()));
    }

    private static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private ModContent() {
    }

    /** The mod's namespaced id for a registry name. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static Map<String, GeneratorBlock> blocks() {
        return BLOCKS;
    }

    public static Map<String, Item> items() {
        return ITEMS;
    }

    /** The creative tab; created when the creative tab registry fills. */
    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.barelypowered"))
                .icon(() -> new ItemStack(ITEMS.get(Tiers.blockId(Kind.COMBUSTION, 1))))
                .displayItems((params, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }

    public static void registerBlocks(BiConsumer<String, Block> out) {
        BLOCKS.forEach(out);
        EXTRA_BLOCKS.forEach(out);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }
}

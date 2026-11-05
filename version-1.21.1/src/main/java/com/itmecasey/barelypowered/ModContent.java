package com.itmecasey.barelypowered;

import com.itmecasey.barelypowered.block.GeneratorBlock;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    static {
        for (Kind kind : Kind.values()) {
            for (int tier = 1; tier <= kind.tierCount(); tier++) {
                String id = Tiers.blockId(kind, tier);
                BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
                GeneratorBlock block = new GeneratorBlock(kind, tier, props);
                BLOCKS.put(id, block);
                ITEMS.put(id, new GeneratorItem(block, new Item.Properties()));
            }
        }
        STEEL_INGOT = new Item(new Item.Properties());
        ITEMS.put("steel_ingot", STEEL_INGOT);
        GENERATOR = BlockEntityType.Builder.of(GeneratorBlockEntity::new, BLOCKS.values().toArray(new Block[0])).build(null);
    }

    private ModContent() {
    }

    /** The mod's namespaced id for a registry name. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
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
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }
}

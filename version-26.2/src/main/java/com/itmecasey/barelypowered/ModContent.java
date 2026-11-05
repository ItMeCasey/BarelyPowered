package com.itmecasey.barelypowered;

import com.itmecasey.barelypowered.block.BatteryBlock;
import com.itmecasey.barelypowered.block.BatteryBlockEntity;
import com.itmecasey.barelypowered.block.BatteryItem;
import com.itmecasey.barelypowered.block.GeneratorBlock;
import com.itmecasey.barelypowered.block.GeneratorBlockEntity;
import com.itmecasey.barelypowered.block.GeneratorItem;
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
    public static final BlockEntityType<BatteryBlockEntity> BATTERY;
    private static final Map<String, BatteryBlock> BATTERIES = new LinkedHashMap<>();
    public static final Item STEEL_INGOT;
    /** Blocks and block entity types added by optional parts of the build (the dev tools). */
    private static final Map<String, Block> EXTRA_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, BlockEntityType<?>> EXTRA_TYPES = new LinkedHashMap<>();

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
        GENERATOR = new BlockEntityType<>(GeneratorBlockEntity::new, new java.util.HashSet<Block>(BLOCKS.values()));
        for (int tier = 1; tier <= 3; tier++) {
            String id = BatteryCore.blockId(tier);
            BatteryBlock battery = new BatteryBlock(tier, BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id(id))).mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
            BATTERIES.put(id, battery);
            ITEMS.put(id, new BatteryItem(battery, itemProps(id).useBlockDescriptionPrefix().stacksTo(1)));
        }
        BATTERY = new BlockEntityType<>(BatteryBlockEntity::new, new java.util.HashSet<Block>(BATTERIES.values()));
        Dev.call("com.itmecasey.barelypowered.dev.DevContent", "register");
    }

    public static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    /** Adds a block, its item and its block entity type (used by the dev tools). */
    public static void addExtra(String name, Block block, Item item, BlockEntityType<?> type) {
        EXTRA_BLOCKS.put(name, block);
        ITEMS.put(name, item);
        EXTRA_TYPES.put(name, type);
    }

    public static Map<String, BlockEntityType<?>> extraBlockEntityTypes() {
        return EXTRA_TYPES;
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

    public static Map<String, BatteryBlock> batteries() {
        return BATTERIES;
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
        BATTERIES.forEach(out);
        EXTRA_BLOCKS.forEach(out);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }
}

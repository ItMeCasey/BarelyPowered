package com.itmecasey.barelycables;

import com.itmecasey.barelycables.block.CableBlock;
import com.itmecasey.barelycables.block.CableBlockEntity;
import com.itmecasey.barelycables.block.CableItem;
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

/** Every block, item and block entity type of BarelyCables, built once and handed to the loader's registry event. */
public final class CablesContent {
    public static final String MODID = "barelycables";

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<String, BlockEntityType<?>> EXTRA_TYPES = new LinkedHashMap<>();
    public static final BlockEntityType<CableBlockEntity> CABLE;

    static {
        for (int tier = 1; tier <= 3; tier++) {
            String id = CableSettings.blockId(tier);
            CableBlock block = new CableBlock(tier, BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id(id))).mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.METAL).noOcclusion());
            BLOCKS.put(id, block);
            ITEMS.put(id, new CableItem(block, itemProps(id).useBlockDescriptionPrefix()));
        }
        CABLE = new BlockEntityType<>(CableBlockEntity::new, new java.util.HashSet<Block>(BLOCKS.values()));
        Dev.call("com.itmecasey.barelycables.dev.DevContent", "register");
    }

    public static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private CablesContent() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static Map<String, Block> blocks() {
        return BLOCKS;
    }

    public static Map<String, Item> items() {
        return ITEMS;
    }

    /** Adds a block, its item and its block entity type (used by the dev tools). */
    public static void addExtra(String name, Block block, Item item, BlockEntityType<?> type) {
        BLOCKS.put(name, block);
        ITEMS.put(name, item);
        if (type != null) {
            EXTRA_TYPES.put(name, type);
        }
    }

    public static Map<String, BlockEntityType<?>> extraBlockEntityTypes() {
        return EXTRA_TYPES;
    }

    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.barelycables"))
                .icon(() -> new ItemStack(ITEMS.get(CableSettings.blockId(1))))
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

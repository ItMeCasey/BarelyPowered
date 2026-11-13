package com.itmecasey.barelycables.block;

import com.itmecasey.barelycables.CableSettings;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A cable's item; the tooltip gives what a network of this cable moves. */
public class CableItem extends BlockItem {
    private final int tier;

    public CableItem(CableBlock block, Item.Properties properties) {
        super(block, properties);
        this.tier = block.tier();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.barelycables.rate", CableSettings.rate(tier)).withStyle(ChatFormatting.GOLD));
    }
}

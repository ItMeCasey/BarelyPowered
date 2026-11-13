package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.BatteryCore;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** A battery item. It carries the charge in its custom data, shows it in the tooltip and as a bar under the icon. */
public class BatteryItem extends BlockItem {
    private final int tier;

    public BatteryItem(BatteryBlock block, Item.Properties properties) {
        super(block, properties);
        this.tier = block.tier();
    }

    /** The charge stored in a battery item. */
    public static int charge(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(BatteryBlockEntity.CHARGE_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.barelypowered.charge", charge(stack), BatteryCore.capacity(tier)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.barelypowered.battery_rate", BatteryCore.rate(tier)).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return charge(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charge(stack) / BatteryCore.capacity(tier));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.1F + 0.06F * charge(stack) / BatteryCore.capacity(tier), 0.85F, 1.0F);
    }
}

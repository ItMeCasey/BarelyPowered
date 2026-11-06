package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.Fuel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Which items a combustion generator burns. */
public final class FuelRules {
    private FuelRules() {
    }

    /** The fuel kind of an item, or {@code null} if the generator will not take it. */
    public static Fuel of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.COAL)) {
            return Fuel.COAL;
        }
        if (stack.is(Items.CHARCOAL)) {
            return Fuel.CHARCOAL;
        }
        if (stack.is(ItemTags.LOGS_THAT_BURN)) {
            return Fuel.WOOD;
        }
        if (stack.is(ItemTags.PLANKS)) {
            return Fuel.PLANKS;
        }
        return null;
    }
}

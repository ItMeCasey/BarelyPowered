package com.itmecasey.barelypowered.block;

import com.itmecasey.barelypowered.Fuel;
import com.itmecasey.barelypowered.Kind;
import com.itmecasey.barelypowered.PowerMath;
import com.itmecasey.barelypowered.Settings;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A generator block's item; its tooltip says what the block does and its top output. */
public class GeneratorItem extends BlockItem {
    private final GeneratorBlock generator;

    public GeneratorItem(GeneratorBlock block, Item.Properties properties) {
        super(block, properties);
        this.generator = block;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Kind kind = generator.kind();
        int tier = generator.tier();
        String top = switch (kind) {
            case COMBUSTION -> PowerMath.combustionOutput(Fuel.COAL, tier) + " FE/t on coal";
            case LAVA -> PowerMath.lavaOutput(tier) + " FE/t";
            case HAND_CRANK -> Settings.getInt("hand_crank.fe_per_use") + " FE per crank";
            default -> "up to " + PowerMath.maxOutput(kind, tier) + " FE/t";
        };
        tooltip.add(Component.translatable("tooltip.barelypowered.output", top).withStyle(ChatFormatting.GOLD));
    }
}

package dev.coco.cocovis;

import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public final class PriceTooltip {
    private PriceTooltip() {}

    public static void onTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        if (!ModuleManager.TOOLTIP.isEnabled() || stack.isEmpty()) return;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Double price = DiamondConfig.get().prices.get(id);
        if (price == null) return;

        String line = "Цена: " + fmt(price) + " алм.";
        int count = stack.getCount();
        if (count > 1) line = line + "  |  x" + count + " = " + fmt(price * count) + " алм.";
        lines.add(Component.literal(line).withStyle(ChatFormatting.AQUA));
    }

    static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s;
    }
}

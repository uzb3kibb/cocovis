package dev.coco.cocovis;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Adds "Цена: N алм." to item tooltips using prices from cocovis.json. */
public final class PriceTooltip {
    private PriceTooltip() {}

    public static void onTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (!cfg.showTooltips || stack.isEmpty() || cfg.prices == null) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Map<String, Double> prices = cfg.prices;
        Double price = prices.get(id);
        if (price == null) {
            return;
        }
        int count = stack.getCount();
        String text = "Цена: " + fmt(price) + " алм.";
        if (count > 1) {
            text += "  |  x" + count + " = " + fmt(price * count) + " алм.";
        }
        lines.add(Component.literal(text).withStyle(ChatFormatting.AQUA));
    }

    private static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}

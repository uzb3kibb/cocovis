package dev.coco.cocovis;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DiamondCount {
    private DiamondCount() {}

    /** Diamonds in the player's inventory; a diamond block counts as 9. */
    public static int inInventory(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (item == Items.DIAMOND) {
                total += stack.getCount();
            } else if (item == Items.DIAMOND_BLOCK) {
                total += stack.getCount() * 9;
            }
        }
        return total;
    }
}

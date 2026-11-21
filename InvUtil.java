package com.nightfall.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public final class InvUtil {
    private InvUtil() {
    }

    private static Inventory inv() {
        return Minecraft.getInstance().player.getInventory();
    }

    /** Hotbar index (0-8) of the first matching stack, or -1. */
    public static int findHotbar(Predicate<ItemStack> filter) {
        Inventory inv = inv();
        for (int i = 0; i < 9; i++) {
            if (filter.test(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Inventory index (0-35, hotbar first) of the first matching stack, or -1. */
    public static int findInventory(Predicate<ItemStack> filter) {
        Inventory inv = inv();
        for (int i = 0; i < 36; i++) {
            if (filter.test(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    // NOTE: these two are the only places that touch the selected-hotbar-slot API.
    // If Mojang renames them again, this is the one spot to fix.
    public static int selectedSlot() {
        return inv().getSelectedSlot();
    }

    public static void select(int hotbarSlot) {
        inv().setSelectedSlot(hotbarSlot);
    }

    /** Converts an inventory index to the slot id used by the player inventory menu. */
    public static int menuSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }
}

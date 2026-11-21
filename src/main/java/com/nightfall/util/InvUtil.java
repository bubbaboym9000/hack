package com.nightfall.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
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

    /** The item's registry name without the namespace, e.g. "diamond_sword". */
    public static String path(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** Rough ranking of tool and weapon materials. 0 means not a tiered tool. */
    public static int toolTier(ItemStack stack) {
        String p = path(stack);
        if (p.startsWith("netherite")) return 7;
        if (p.startsWith("diamond")) return 6;
        if (p.startsWith("iron")) return 5;
        if (p.startsWith("copper")) return 4;
        if (p.startsWith("stone")) return 3;
        if (p.startsWith("golden")) return 2;
        if (p.startsWith("wooden")) return 1;
        return 0;
    }

    /** Rough ranking of armor materials. 0 means not tiered armor. */
    public static int armorTier(ItemStack stack) {
        String p = path(stack);
        if (p.startsWith("netherite")) return 16;
        if (p.startsWith("diamond")) return 14;
        if (p.startsWith("iron")) return 12;
        if (p.startsWith("chainmail")) return 10;
        if (p.startsWith("copper")) return 9;
        if (p.startsWith("golden")) return 8;
        if (p.startsWith("turtle")) return 6;
        if (p.startsWith("leather")) return 4;
        return 0;
    }

    /** Converts an inventory index to the slot id used by the player inventory menu. */
    public static int menuSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }
}

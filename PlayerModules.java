package com.nightfall.modules;

import com.nightfall.core.Category;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import com.nightfall.util.InvUtil;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PlayerModules {
    private PlayerModules() {
    }

    public static void register(ModuleManager m) {
        m.add(new AutoRespawn());
        m.add(new AutoTool());
        m.add(new AutoEat());
        m.add(new ChestStealer());
        m.add(new Scaffold());
        m.add(new Nuker());
    }

    // ------------------------------------------------------------------

    public static final class AutoRespawn extends Module {
        public AutoRespawn() {
            super("AutoRespawn", "Respawns instantly when you die.", Category.PLAYER);
        }

        @Override
        public void onTick() {
            if (mc.screen instanceof DeathScreen) {
                mc.player.respawn();
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoTool extends Module {
        private int previous = -1;

        public AutoTool() {
            super("AutoTool", "Switches to the fastest tool in your hotbar when you mine.", Category.PLAYER);
        }

        @Override
        public void onTick() {
            if (mc.screen == null && mc.options.keyAttack.isDown()
                    && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockState state = mc.level.getBlockState(hit.getBlockPos());
                int current = InvUtil.selectedSlot();
                int best = current;
                float bestSpeed = mc.player.getInventory().getItem(current).getDestroySpeed(state);
                for (int i = 0; i < 9; i++) {
                    float speed = mc.player.getInventory().getItem(i).getDestroySpeed(state);
                    if (speed > bestSpeed) {
                        best = i;
                        bestSpeed = speed;
                    }
                }
                if (best != current) {
                    if (previous == -1) {
                        previous = current;
                    }
                    InvUtil.select(best);
                }
            } else if (previous != -1) {
                InvUtil.select(previous);
                previous = -1;
            }
        }

        @Override
        protected void onDisable() {
            if (previous != -1 && mc.player != null) {
                InvUtil.select(previous);
            }
            previous = -1;
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoEat extends Module {
        private final Setting.Num hunger = num("Hunger", 14, 1, 19, 1);
        private boolean eating;
        private int previous = -1;

        public AutoEat() {
            super("AutoEat", "Eats food from your hotbar when you get hungry.", Category.PLAYER);
        }

        private static boolean isGoodFood(ItemStack stack) {
            return stack.has(DataComponents.FOOD)
                    && !stack.is(Items.ROTTEN_FLESH)
                    && !stack.is(Items.POISONOUS_POTATO)
                    && !stack.is(Items.PUFFERFISH)
                    && !stack.is(Items.SPIDER_EYE)
                    && !stack.is(Items.CHORUS_FRUIT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            boolean hungry = p.getFoodData().getFoodLevel() <= hunger.i() && !p.isCreative() && !p.isSpectator();
            if (hungry && mc.screen == null) {
                int slot = InvUtil.findHotbar(AutoEat::isGoodFood);
                if (slot != -1) {
                    if (!eating) {
                        previous = InvUtil.selectedSlot();
                        eating = true;
                    }
                    if (InvUtil.selectedSlot() != slot) {
                        InvUtil.select(slot);
                    }
                    mc.options.keyUse.setDown(true);
                    return;
                }
            }
            stop();
        }

        private void stop() {
            if (!eating) {
                return;
            }
            mc.options.keyUse.setDown(false);
            if (previous != -1) {
                InvUtil.select(previous);
            }
            previous = -1;
            eating = false;
        }

        @Override
        protected void onDisable() {
            if (mc.player != null) {
                stop();
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class ChestStealer extends Module {
        private final Setting.Num delay = num("Delay", 2, 0, 10, 1);
        private int cooldown;

        public ChestStealer() {
            super("ChestStealer", "Takes everything out of a chest as soon as you open it.", Category.PLAYER);
        }

        @Override
        public void onTick() {
            if (!(mc.screen instanceof ContainerScreen screen) || mc.gameMode == null) {
                cooldown = 0;
                return;
            }
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            ChestMenu menu = screen.getMenu();
            int chestSlots = menu.getRowCount() * 9;
            for (int i = 0; i < chestSlots; i++) {
                if (menu.getSlot(i).hasItem()) {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    cooldown = delay.i();
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Scaffold extends Module {
        private final Setting.Bool swing = bool("Swing", true);

        public Scaffold() {
            super("Scaffold", "Places blocks under your feet as you walk.", Category.PLAYER);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (mc.gameMode == null || mc.screen != null) {
                return;
            }
            int slot = InvUtil.findHotbar(stack -> stack.getItem() instanceof BlockItem);
            if (slot == -1) {
                return;
            }

            BlockPos below = BlockPos.containing(p.getX(), p.getY() - 1.0, p.getZ());
            if (!mc.level.getBlockState(below).canBeReplaced()) {
                return;
            }

            for (Direction side : Direction.values()) {
                BlockPos neighbour = below.relative(side);
                if (mc.level.getBlockState(neighbour).canBeReplaced()) {
                    continue;
                }
                Direction face = side.getOpposite();
                Vec3 hitPos = Vec3.atCenterOf(neighbour).relative(face, 0.5);

                int previous = InvUtil.selectedSlot();
                InvUtil.select(slot);
                mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(hitPos, face, neighbour, false));
                if (swing.on()) {
                    p.swing(InteractionHand.MAIN_HAND);
                }
                InvUtil.select(previous);
                return;
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Nuker extends Module {
        private final Setting.Num range = num("Range", 4.0, 1.0, 5.0, 0.5);
        private BlockPos current;

        public Nuker() {
            super("Nuker", "Breaks the closest block in range, one after another.", Category.PLAYER);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (mc.gameMode == null || mc.screen != null) {
                return;
            }
            if (current != null && mc.level.getBlockState(current).isAir()) {
                current = null;
            }

            Vec3 eye = p.getEyePosition();
            BlockPos origin = p.blockPosition();
            int reach = (int) Math.ceil(range.d());
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int x = -reach; x <= reach; x++) {
                for (int y = -reach; y <= reach; y++) {
                    for (int z = -reach; z <= reach; z++) {
                        BlockPos pos = origin.offset(x, y, z);
                        BlockState state = mc.level.getBlockState(pos);
                        if (state.isAir() || state.getDestroySpeed(mc.level, pos) < 0) {
                            continue;
                        }
                        double d = Vec3.atCenterOf(pos).distanceTo(eye);
                        if (d > range.d() || d >= bestDistance) {
                            continue;
                        }
                        best = pos;
                        bestDistance = d;
                    }
                }
            }

            if (best == null) {
                current = null;
                return;
            }
            if (!best.equals(current)) {
                mc.gameMode.startDestroyBlock(best, Direction.UP);
                current = best;
            } else {
                mc.gameMode.continueDestroyBlock(best, Direction.UP);
            }
            p.swing(InteractionHand.MAIN_HAND);
        }

        @Override
        protected void onDisable() {
            current = null;
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
        }
    }
}

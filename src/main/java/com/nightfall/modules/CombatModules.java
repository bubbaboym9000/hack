package com.nightfall.modules;

import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import com.nightfall.core.Category;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import com.nightfall.util.InvUtil;
import com.nightfall.util.TargetUtil;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class CombatModules {
    private CombatModules() {
    }

    public static void register(ModuleManager m) {
        m.add(new KillAura());
        m.add(new TriggerBot());
        m.add(new AutoClicker());
        m.add(new Criticals());
        m.add(new Reach());
        m.add(new Velocity());
        m.add(new AutoTotem());
        m.add(new AimAssist());
        m.add(new AutoWeapon());
    }

    // ------------------------------------------------------------------

    public static final class KillAura extends Module {
        private final Setting.Num range = num("Range", 4.2, 2.0, 6.0, 0.1);
        private final Setting.Bool players = bool("Players", true);
        private final Setting.Bool mobs = bool("Mobs", true);
        private final Setting.Bool animals = bool("Animals", false);
        private final Setting.Bool rotate = bool("Rotate", true);
        private final Setting.Bool cooldown = bool("Cooldown", true);

        public KillAura() {
            super("KillAura", "Automatically attacks the closest target in range.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (mc.gameMode == null || p.isUsingItem() || mc.screen != null) {
                return;
            }
            Entity target = TargetUtil.closest(range.d(), players.on(), mobs.on(), animals.on());
            if (target == null) {
                return;
            }
            if (rotate.on()) {
                TargetUtil.face(target);
            }
            if (cooldown.on() && p.getAttackStrengthScale(0.5f) < 0.92f) {
                return;
            }
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
        }

        @Override
        public String getSuffix() {
            return range.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class TriggerBot extends Module {
        private final Setting.Bool players = bool("Players", true);
        private final Setting.Bool mobs = bool("Mobs", true);
        private final Setting.Bool animals = bool("Animals", false);

        public TriggerBot() {
            super("TriggerBot", "Attacks whatever your crosshair is on once the cooldown is ready.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (mc.gameMode == null || mc.screen != null || p.isUsingItem()) {
                return;
            }
            if (!(mc.hitResult instanceof EntityHitResult hit)) {
                return;
            }
            Entity target = hit.getEntity();
            if (!TargetUtil.isValid(target, players.on(), mobs.on(), animals.on())) {
                return;
            }
            if (p.getAttackStrengthScale(0.5f) < 0.92f) {
                return;
            }
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoClicker extends Module {
        private final Setting.Num cps = num("CPS", 10, 1, 20, 1);
        private final Setting.Bool respectCooldown = bool("Cooldown", false);
        private double progress;

        public AutoClicker() {
            super("AutoClicker", "Clicks for you while you hold attack on an entity.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (mc.gameMode == null || mc.screen != null || !mc.options.keyAttack.isDown()) {
                progress = 0;
                return;
            }
            if (!(mc.hitResult instanceof EntityHitResult hit)) {
                return;
            }
            progress += cps.d() / 20.0;
            if (progress < 1.0) {
                return;
            }
            progress -= 1.0;
            if (respectCooldown.on() && p.getAttackStrengthScale(0.5f) < 0.92f) {
                return;
            }
            mc.gameMode.attack(p, hit.getEntity());
            p.swing(InteractionHand.MAIN_HAND);
        }

        @Override
        public String getSuffix() {
            return cps.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class Criticals extends Module {
        public Criticals() {
            super("Criticals", "Sends a tiny hop to the server before each hit so it lands as a crit.", Category.COMBAT);
            AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
                if (level.isClientSide() && isEnabled()) {
                    crit();
                }
                return InteractionResult.PASS;
            });
        }

        private void crit() {
            LocalPlayer p = mc.player;
            if (p == null || !p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable()) {
                return;
            }
            double x = p.getX();
            double y = p.getY();
            double z = p.getZ();
            boolean collided = p.horizontalCollision;
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, collided));
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, collided));
        }
    }

    // ------------------------------------------------------------------

    public static final class Reach extends Module {
        private static final double VANILLA_ENTITY = 3.0;
        private static final double VANILLA_BLOCK = 4.5;

        private final Setting.Num entityRange = num("Entity", 4.5, 3.0, 6.0, 0.1);
        private final Setting.Num blockRange = num("Block", 6.0, 4.5, 8.0, 0.1);

        public Reach() {
            super("Reach", "Extends how far you can hit entities and break or place blocks.", Category.COMBAT);
        }

        private void apply(double entity, double block) {
            LocalPlayer p = mc.player;
            if (p == null) {
                return;
            }
            AttributeInstance e = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null) {
                e.setBaseValue(entity);
            }
            AttributeInstance b = p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null) {
                b.setBaseValue(block);
            }
        }

        @Override
        public void onTick() {
            apply(entityRange.d(), blockRange.d());
        }

        @Override
        protected void onDisable() {
            apply(VANILLA_ENTITY, VANILLA_BLOCK);
        }
    }

    // ------------------------------------------------------------------

    public static final class Velocity extends Module {
        private final Setting.Num horizontal = num("Horizontal", 0, 0, 100, 5);
        private final Setting.Num vertical = num("Vertical", 0, 0, 100, 5);
        private boolean wasHurt;

        public Velocity() {
            super("Velocity", "Scales down the knockback you take when hit.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            boolean hurt = p.hurtTime > 0;
            if (hurt && !wasHurt) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x * horizontal.d() / 100.0, v.y * vertical.d() / 100.0, v.z * horizontal.d() / 100.0);
            }
            wasHurt = hurt;
        }

        @Override
        public String getSuffix() {
            return horizontal.display() + "% " + vertical.display() + "%";
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoTotem extends Module {
        private final Setting.Num delay = num("Delay", 1, 0, 10, 1);
        private int cooldown;

        public AutoTotem() {
            super("AutoTotem", "Keeps a Totem of Undying in your offhand.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (mc.gameMode == null || p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                return;
            }
            int slot = InvUtil.findInventory(stack -> stack.is(Items.TOTEM_OF_UNDYING));
            if (slot == -1) {
                return;
            }
            // Button 40 on a SWAP click means "swap with the offhand slot".
            mc.gameMode.handleInventoryMouseClick(
                    p.inventoryMenu.containerId, InvUtil.menuSlot(slot), 40, ClickType.SWAP, p);
            cooldown = delay.i();
        }
    }

    // ------------------------------------------------------------------

    public static final class AimAssist extends Module {
        private final Setting.Num range = num("Range", 5.0, 2.0, 8.0, 0.5);
        private final Setting.Num fov = num("FOV", 70, 10, 180, 5);
        private final Setting.Num smooth = num("Smooth", 0.25, 0.05, 1.0, 0.05);
        private final Setting.Bool players = bool("Players", true);
        private final Setting.Bool mobs = bool("Mobs", true);
        private final Setting.Bool animals = bool("Animals", false);
        private final Setting.Bool onlyClicking = bool("OnlyClicking", true);

        public AimAssist() {
            super("AimAssist", "Smoothly steers your aim towards the closest target inside your field of view.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (mc.screen != null || (onlyClicking.on() && !mc.options.keyAttack.isDown())) {
                return;
            }

            float[] bestRot = null;
            float bestScore = Float.MAX_VALUE;
            for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(range.d()), x -> true)) {
                if (!TargetUtil.isValid(e, players.on(), mobs.on(), animals.on()) || p.distanceTo(e) > range.d()) {
                    continue;
                }
                float[] rot = TargetUtil.rotationsTo(e);
                float yawDiff = Math.abs(Mth.wrapDegrees(rot[0] - p.getYRot()));
                float pitchDiff = Math.abs(rot[1] - p.getXRot());
                if (yawDiff > fov.d() / 2.0) {
                    continue;
                }
                float score = yawDiff + pitchDiff;
                if (score < bestScore) {
                    bestScore = score;
                    bestRot = rot;
                }
            }
            if (bestRot == null) {
                return;
            }

            float s = smooth.f();
            p.setYRot(p.getYRot() + Mth.wrapDegrees(bestRot[0] - p.getYRot()) * s);
            p.setXRot(Mth.clamp(p.getXRot() + (bestRot[1] - p.getXRot()) * s, -90.0f, 90.0f));
        }

        @Override
        public String getSuffix() {
            return smooth.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoWeapon extends Module {
        private final Setting.Bool axes = bool("Axes", false);

        public AutoWeapon() {
            super("AutoWeapon", "Switches to your best sword when you attack something.", Category.COMBAT);
        }

        @Override
        public void onTick() {
            if (mc.screen != null || !mc.options.keyAttack.isDown()
                    || !(mc.hitResult instanceof EntityHitResult hit)
                    || !TargetUtil.isValid(hit.getEntity(), true, true, true)) {
                return;
            }

            int best = -1;
            int bestScore = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                int score = 0;
                if (stack.is(ItemTags.SWORDS)) {
                    score = 10 + InvUtil.toolTier(stack);
                } else if (axes.on() && stack.is(ItemTags.AXES)) {
                    score = InvUtil.toolTier(stack);
                }
                if (score > bestScore) {
                    best = i;
                    bestScore = score;
                }
            }
            if (best != -1 && best != InvUtil.selectedSlot()) {
                InvUtil.select(best);
            }
        }
    }
}

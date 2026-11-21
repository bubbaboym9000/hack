package com.nightfall.modules;

import com.nightfall.core.Category;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import com.nightfall.util.MoveUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

public final class MovementModules {
    private MovementModules() {
    }

    public static void register(ModuleManager m) {
        m.add(new Fly());
        m.add(new Speed());
        m.add(new Sprint());
        m.add(new Bhop());
        m.add(new Step());
        m.add(new Jesus());
        m.add(new Spider());
        m.add(new NoFall());
        m.add(new Glide());
        m.add(new HighJump());
        m.add(new AirJump());
        m.add(new ElytraFly());
        m.add(new FastLadder());
        m.add(new AutoJump());
        m.add(new AutoWalk());
        m.add(new NoClip());
        m.add(new AutoSneak());
    }

    // ------------------------------------------------------------------

    public static final class Fly extends Module {
        private final Setting.Mode mode = mode("Mode", "Vanilla", "Vanilla", "Velocity");
        private final Setting.Num speed = num("Speed", 1.0, 0.5, 10.0, 0.5);
        private final Setting.Bool antiKick = bool("AntiKick", true);
        private int airTicks;

        public Fly() {
            super("Fly", "Fly like in creative. Vanilla uses the abilities flag, Velocity sets your motion.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            Abilities abilities = p.getAbilities();

            if (mode.is("Vanilla")) {
                abilities.mayfly = true;
                abilities.flying = true;
                abilities.setFlyingSpeed((float) (0.05 * speed.d()));
                return;
            }

            // Velocity mode: drive the motion vector directly.
            abilities.flying = false;
            double h = speed.d() * 0.5;
            Vec3 dir = MoveUtil.direction(h);
            double vy = 0;
            if (mc.options.keyJump.isDown()) {
                vy = h;
            } else if (mc.options.keyShift.isDown()) {
                vy = -h;
            }
            if (antiKick.on() && !p.onGround()) {
                airTicks++;
                if (airTicks % 30 == 0) {
                    vy = -0.06;
                }
            } else {
                airTicks = 0;
            }
            p.setDeltaMovement(dir.x, vy, dir.z);
        }

        @Override
        protected void onDisable() {
            LocalPlayer p = mc.player;
            if (p == null) {
                return;
            }
            Abilities abilities = p.getAbilities();
            if (!p.isCreative() && !p.isSpectator()) {
                abilities.mayfly = false;
                abilities.flying = false;
            }
            abilities.setFlyingSpeed(0.05f);
        }

        @Override
        public String getSuffix() {
            return mode.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class Speed extends Module {
        private final Setting.Num speed = num("Speed", 0.45, 0.2, 2.0, 0.05);
        private final Setting.Bool onlyGround = bool("OnlyGround", false);

        public Speed() {
            super("Speed", "Moves you faster (blocks per tick) while you hold a movement key.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (!MoveUtil.isMoving() || p.isInWater() || p.isInLava() || p.getAbilities().flying) {
                return;
            }
            if (onlyGround.on() && !p.onGround()) {
                return;
            }
            Vec3 v = p.getDeltaMovement();
            Vec3 dir = MoveUtil.direction(speed.d());
            p.setDeltaMovement(dir.x, v.y, dir.z);
        }

        @Override
        public String getSuffix() {
            return speed.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class Sprint extends Module {
        private final Setting.Bool omni = bool("Omni", false);

        public Sprint() {
            super("Sprint", "Always sprint. Omni also sprints sideways and backwards.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.isShiftKeyDown() || p.isUsingItem() || p.horizontalCollision || p.getFoodData().getFoodLevel() <= 6) {
                return;
            }
            boolean wants = omni.on() ? MoveUtil.isMoving() : mc.options.keyUp.isDown();
            if (wants) {
                p.setSprinting(true);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Bhop extends Module {
        private final Setting.Num height = num("Height", 0.42, 0.2, 0.8, 0.02);

        public Bhop() {
            super("Bhop", "Hops automatically while you move.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (!MoveUtil.isMoving() || !p.onGround() || p.isInWater() || p.isInLava() || p.isShiftKeyDown()) {
                return;
            }
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, height.d(), v.z);
            p.setSprinting(true);
        }
    }

    // ------------------------------------------------------------------

    public static final class Step extends Module {
        private static final double VANILLA = 0.6;
        private final Setting.Num height = num("Height", 1.0, 0.6, 2.5, 0.1);

        public Step() {
            super("Step", "Walk up blocks without jumping.", Category.MOVEMENT);
        }

        private void apply(double value) {
            LocalPlayer p = mc.player;
            if (p == null) {
                return;
            }
            AttributeInstance step = p.getAttribute(Attributes.STEP_HEIGHT);
            if (step != null) {
                step.setBaseValue(value);
            }
        }

        @Override
        public void onTick() {
            apply(height.d());
        }

        @Override
        protected void onDisable() {
            apply(VANILLA);
        }

        @Override
        public String getSuffix() {
            return height.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class Jesus extends Module {
        public Jesus() {
            super("Jesus", "Float on water and lava (you will bob slightly).", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.isShiftKeyDown() || mc.options.keyJump.isDown()) {
                return;
            }
            if (p.isInWater() || p.isInLava()) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, 0.14, v.z);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Spider extends Module {
        public Spider() {
            super("Spider", "Climb walls by running into them.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.horizontalCollision && !p.isShiftKeyDown()) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, 0.2, v.z);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class NoFall extends Module {
        private final Setting.Num distance = num("Distance", 2.5, 1.0, 10.0, 0.5);

        public NoFall() {
            super("NoFall", "Tells the server you landed so you take no fall damage.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.isFallFlying() || p.getAbilities().flying) {
                return;
            }
            if (p.fallDistance > distance.d()) {
                p.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Glide extends Module {
        private final Setting.Num fallSpeed = num("FallSpeed", 0.05, 0.01, 0.2, 0.01);

        public Glide() {
            super("Glide", "Fall slowly instead of dropping.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.onGround() || p.isInWater() || p.isInLava() || p.getAbilities().flying) {
                return;
            }
            Vec3 v = p.getDeltaMovement();
            if (v.y < -fallSpeed.d()) {
                p.setDeltaMovement(v.x, -fallSpeed.d(), v.z);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class HighJump extends Module {
        private static final double VANILLA = 0.42;
        private final Setting.Num power = num("Power", 0.84, 0.42, 2.0, 0.02);

        public HighJump() {
            super("HighJump", "Jump much higher.", Category.MOVEMENT);
        }

        private void apply(double value) {
            LocalPlayer p = mc.player;
            if (p == null) {
                return;
            }
            AttributeInstance jump = p.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null) {
                jump.setBaseValue(value);
            }
        }

        @Override
        public void onTick() {
            apply(power.d());
        }

        @Override
        protected void onDisable() {
            apply(VANILLA);
        }

        @Override
        public String getSuffix() {
            return power.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class AirJump extends Module {
        private boolean jumpWas;

        public AirJump() {
            super("AirJump", "Jump again while already in the air.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            boolean jump = mc.options.keyJump.isDown() && mc.screen == null;
            if (jump && !jumpWas && !p.onGround() && !p.isInWater() && !p.isInLava() && !p.getAbilities().flying) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, 0.42, v.z);
            }
            jumpWas = jump;
        }
    }

    // ------------------------------------------------------------------

    public static final class ElytraFly extends Module {
        private final Setting.Num speed = num("Speed", 1.5, 0.5, 4.0, 0.1);

        public ElytraFly() {
            super("ElytraFly", "While gliding: W flies where you look, Shift hovers in place.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (!p.isFallFlying()) {
                return;
            }
            if (mc.options.keyUp.isDown()) {
                p.setDeltaMovement(p.getLookAngle().scale(speed.d()));
            } else if (mc.options.keyShift.isDown()) {
                p.setDeltaMovement(Vec3.ZERO);
            }
        }

        @Override
        public String getSuffix() {
            return speed.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class FastLadder extends Module {
        private final Setting.Num speed = num("Speed", 0.28, 0.1, 0.6, 0.02);

        public FastLadder() {
            super("FastLadder", "Climb ladders and vines faster.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.onClimbable() && p.horizontalCollision && !p.isShiftKeyDown()) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, speed.d(), v.z);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoJump extends Module {
        public AutoJump() {
            super("AutoJump", "Jumps for you when you walk into a block.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.onGround() && p.horizontalCollision && MoveUtil.isMoving() && !p.isShiftKeyDown()) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, 0.42, v.z);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoWalk extends Module {
        public AutoWalk() {
            super("AutoWalk", "Holds the forward key for you.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            if (mc.screen == null) {
                mc.options.keyUp.setDown(true);
            }
        }

        @Override
        protected void onDisable() {
            mc.options.keyUp.setDown(false);
        }
    }

    // ------------------------------------------------------------------

    public static final class NoClip extends Module {
        private final Setting.Num speed = num("Speed", 0.6, 0.1, 3.0, 0.1);

        public NoClip() {
            super("NoClip", "Move through blocks. Turn it off while inside a block and you will be stuck.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            p.noPhysics = true;
            p.setOnGround(false);
            p.fallDistance = 0;

            Vec3 dir = MoveUtil.direction(speed.d());
            double vy = 0;
            if (mc.options.keyJump.isDown()) {
                vy = speed.d();
            } else if (mc.options.keyShift.isDown()) {
                vy = -speed.d();
            }
            p.setDeltaMovement(dir.x, vy, dir.z);
        }

        @Override
        protected void onDisable() {
            if (mc.player != null) {
                mc.player.noPhysics = false;
            }
        }

        @Override
        public String getSuffix() {
            return speed.display();
        }
    }

    // ------------------------------------------------------------------

    public static final class AutoSneak extends Module {
        public AutoSneak() {
            super("AutoSneak", "Holds sneak for you.", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            if (mc.screen == null) {
                mc.options.keyShift.setDown(true);
            }
        }

        @Override
        protected void onDisable() {
            mc.options.keyShift.setDown(false);
        }
    }
}

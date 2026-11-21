package com.nightfall.modules;

import java.util.Set;
import java.util.HashSet;
import net.minecraft.client.player.AbstractClientPlayer;
import com.nightfall.core.CommandManager;
import com.nightfall.core.Category;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public final class MiscModules {
    private MiscModules() {
    }

    public static void register(ModuleManager m) {
        m.add(new AntiAfk());
        m.add(new ChatSuffix());
        m.add(new DeathCoords());
        m.add(new PlayerAlert());
    }

    // ------------------------------------------------------------------

    public static final class AntiAfk extends Module {
        private final Setting.Num seconds = num("Seconds", 20, 5, 60, 5);
        private int ticks;

        public AntiAfk() {
            super("AntiAFK", "Jumps, swings and turns a little every so often so you are not kicked for idling.", Category.MISC);
        }

        @Override
        protected void onEnable() {
            ticks = 0;
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (++ticks < seconds.i() * 20) {
                return;
            }
            ticks = 0;
            if (p.onGround()) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, 0.42, v.z);
            }
            p.swing(InteractionHand.MAIN_HAND);
            p.setYRot(p.getYRot() + 10.0f);
        }
    }

    // ------------------------------------------------------------------

    public static final class ChatSuffix extends Module {
        private static final String SUFFIX = " | Nightfall";

        public ChatSuffix() {
            super("ChatSuffix", "Adds a Nightfall tag to the end of your chat messages.", Category.MISC);
            ClientSendMessageEvents.MODIFY_CHAT.register(message ->
                    isEnabled() && message.length() + SUFFIX.length() <= 250 ? message + SUFFIX : message);
        }
    }

    // ------------------------------------------------------------------

    public static final class DeathCoords extends Module {
        private boolean announced;

        public DeathCoords() {
            super("DeathCoords", "Prints the coordinates where you died in chat.", Category.MISC);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            if (p.isDeadOrDying()) {
                if (!announced) {
                    announced = true;
                    CommandManager.info(String.format("You died at X %d  Y %d  Z %d",
                            p.getBlockX(), p.getBlockY(), p.getBlockZ()));
                }
            } else {
                announced = false;
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class PlayerAlert extends Module {
        private Set<String> seen = new HashSet<>();

        public PlayerAlert() {
            super("PlayerAlert", "Tells you in chat when another player comes into render distance.", Category.MISC);
        }

        @Override
        protected void onEnable() {
            seen = new HashSet<>();
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            Set<String> now = new HashSet<>();
            for (AbstractClientPlayer other : mc.level.players()) {
                if (other == p) {
                    continue;
                }
                String name = other.getName().getString();
                now.add(name);
                if (!seen.contains(name)) {
                    CommandManager.info("\u00a7e" + name + "\u00a7r is in range (" + (int) p.distanceTo(other) + " blocks)");
                }
            }
            seen = now;
        }
    }
}

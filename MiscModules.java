package com.nightfall.modules;

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
}

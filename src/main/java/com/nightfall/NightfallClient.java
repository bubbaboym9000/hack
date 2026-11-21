package com.nightfall;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightfall.core.CommandManager;
import com.nightfall.core.ConfigManager;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.gui.ClickGuiScreen;
import com.nightfall.modules.CombatModules;
import com.nightfall.modules.MiscModules;
import com.nightfall.modules.MovementModules;
import com.nightfall.modules.PlayerModules;
import com.nightfall.modules.RenderModules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class NightfallClient implements ClientModInitializer {
    public static final String MOD_ID = "nightfall";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nightfall");

    public static KeyMapping guiKey;

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        ModuleManager modules = ModuleManager.INSTANCE;

        CombatModules.register(modules);
        MovementModules.register(modules);
        RenderModules.register(modules);
        PlayerModules.register(modules);
        MiscModules.register(modules);

        // One Controls category; every module gets an unbound toggle key you can set in
        // Options > Controls > Key Binds > Nightfall.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "modules"));

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nightfall.gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category));

        for (Module m : modules.all()) {
            m.keyMapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.nightfall.module." + m.name.toLowerCase(Locale.ROOT),
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    category));
        }

        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            modules.tick(client);
        });

        HudRenderCallback.EVENT.register((graphics, delta) ->
                modules.renderHud(graphics, net.minecraft.client.Minecraft.getInstance()));

        // Returning false cancels the message, so ".commands" never reach the server.
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !CommandManager.handle(message));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        LOGGER.info("Nightfall loaded with {} modules.", modules.all().size());
    }
}

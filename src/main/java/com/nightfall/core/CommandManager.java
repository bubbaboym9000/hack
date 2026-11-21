package com.nightfall.core;

import com.nightfall.gui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dot-commands typed in chat, e.g. ".toggle killaura". Messages that are handled
 * here are never sent to the server.
 */
public final class CommandManager {
    public static final String PREFIX = ".";

    private CommandManager() {
    }

    /** Returns true if the message was a Nightfall command and was consumed. */
    public static boolean handle(String message) {
        if (message.length() < 2 || !message.startsWith(PREFIX) || message.startsWith("..")) {
            return false;
        }

        String[] args = message.substring(PREFIX.length()).trim().split("\\s+");
        String command = args[0].toLowerCase();
        ModuleManager modules = ModuleManager.INSTANCE;

        switch (command) {
            case "help" -> {
                info("Commands:");
                info(".list - show all modules");
                info(".toggle <module> - toggle a module");
                info(".settings <module> - show a module's settings");
                info(".set <module> <setting> <value> - change a setting");
                info(".friend <add|remove|list> [name] - players Nightfall never targets");
                info(".save / .reload - write or re-read the config file");
                info(".gui - open the ClickGUI");
                info(".panic - disable every module");
            }
            case "list" -> {
                for (Category c : Category.values()) {
                    List<String> names = new ArrayList<>();
                    for (Module m : modules.inCategory(c)) {
                        names.add((m.isEnabled() ? "§a" : "§7") + m.name + "§r");
                    }
                    info("§d" + c.label + "§r: " + String.join(", ", names));
                }
            }
            case "toggle", "t" -> {
                if (args.length < 2) {
                    info("Usage: .toggle <module>");
                    break;
                }
                Module m = modules.find(args[1]);
                if (m == null) {
                    info("No module called '" + args[1] + "'.");
                    break;
                }
                m.toggle();
                info(m.name + " is now " + (m.isEnabled() ? "§aenabled" : "§cdisabled"));
            }
            case "settings" -> {
                if (args.length < 2) {
                    info("Usage: .settings <module>");
                    break;
                }
                Module m = modules.find(args[1]);
                if (m == null) {
                    info("No module called '" + args[1] + "'.");
                    break;
                }
                if (m.getSettings().isEmpty()) {
                    info(m.name + " has no settings.");
                    break;
                }
                for (Setting<?> s : m.getSettings()) {
                    info(m.name + "." + s.name + " = " + s.display());
                }
            }
            case "set" -> {
                if (args.length < 4) {
                    info("Usage: .set <module> <setting> <value>");
                    break;
                }
                Module m = modules.find(args[1]);
                if (m == null) {
                    info("No module called '" + args[1] + "'.");
                    break;
                }
                Setting<?> s = m.getSetting(args[2]);
                if (s == null) {
                    info(m.name + " has no setting '" + args[2] + "'.");
                    break;
                }
                if (s.parse(args[3])) {
                    info(m.name + "." + s.name + " = " + s.display());
                    ConfigManager.save();
                } else {
                    info("'" + args[3] + "' is not a valid value for " + s.name + ".");
                }
            }
            case "friend", "friends", "f" -> {
                if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
                    info("Friends: " + (FriendManager.all().isEmpty() ? "none" : String.join(", ", FriendManager.all())));
                    break;
                }
                if (args.length < 3) {
                    info("Usage: .friend <add|remove|list> <name>");
                    break;
                }
                switch (args[1].toLowerCase()) {
                    case "add" -> {
                        FriendManager.add(args[2]);
                        info(args[2] + " is now a friend.");
                    }
                    case "remove", "del" -> info(FriendManager.remove(args[2])
                            ? args[2] + " removed from friends."
                            : args[2] + " was not on the friend list.");
                    default -> info("Usage: .friend <add|remove|list> <name>");
                }
                ConfigManager.save();
            }
            case "save" -> {
                ConfigManager.save();
                info("Config saved.");
            }
            case "reload" -> {
                ConfigManager.load();
                info("Config reloaded.");
            }
            case "gui" -> {
                Minecraft mc = Minecraft.getInstance();
                // The chat screen closes itself right after sending, so open on the next tick.
                mc.execute(() -> mc.setScreen(new ClickGuiScreen()));
            }
            case "panic" -> {
                modules.disableAll();
                info("All modules disabled.");
            }
            default -> info("Unknown command. Try .help");
        }
        return true;
    }

    public static void info(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§d[Nightfall]§r " + text), false);
        }
    }
}

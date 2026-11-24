# Nightfall

A utility client for **Minecraft 1.21.11** (Fabric, Mojang mappings). 51 modules, a ClickGUI, a HUD, dot-commands and per-module keybinds.

## Build and run

```
gradle build             # jar ends up in build/libs/nightfall-1.0.0.jar (Gradle 9.2+; no wrapper included)
gradle runClient         # launch a dev client
```

Needs Java 21. Drop the jar in `mods/` next to Fabric Loader 0.18.1+ and Fabric API.

## Get the jar from GitHub (no local setup)

The project includes `.github/workflows/build.yml`, which builds the jar on GitHub's servers.

1. Push this project to a GitHub repository (any branch).
2. Open the **Actions** tab. The *Build Nightfall* run starts by itself. When it turns green, open it and download **nightfall-jar** from the Artifacts section at the bottom. Unzip that and you have `nightfall-1.0.0.jar`.
3. Later builds: push a change, or use **Actions > Build Nightfall > Run workflow**. Pushing a tag like `v1.0.1` also attaches the jar to a GitHub Release.

If the run turns red, the compile errors are printed as annotations at the top of the run page (file, line and message). Copy those.

> **Compiles, but not play-tested.** The project builds on GitHub, but nothing has been run in a real game yet, so some modules may misbehave.
> The mixin-based ones (ESP, XRay) are the most likely to need a tweak. See *Known risk spots*.

## Controls

| Action | How |
|---|---|
| Open ClickGUI | **Right Shift** (rebindable) |
| Toggle a module | Left click it in the GUI, or bind a key in **Options > Controls > Key Binds > Nightfall** (every module has an unbound toggle key) |
| Module settings | Right click a module in the GUI to expand it. Left click a setting = next / up, right click = previous / down. Mouse wheel scrolls the panels |
| Chat commands | `.help`, `.list`, `.toggle <module>`, `.settings <module>`, `.set <module> <setting> <value>`, `.friend <add\|remove\|list> [name]`, `.save`, `.reload`, `.gui`, `.panic` |

The ClickGUI and HUD use a night theme: midnight-blue panels, a starfield with an occasional shooting star, a crescent moon and a blue-to-violet module list. All colours live in `gui/Theme.java`.

Commands never leave your client. Settings and the friend list are saved to `config/nightfall.json`. Modules always start off (except HUD). Friends are never targeted by KillAura, TriggerBot, AimAssist, AutoWeapon or ESP.

## Modules

**Combat** KillAura, TriggerBot, AutoClicker, Criticals, Reach, Velocity, AutoTotem, AimAssist, AutoWeapon
**Movement** Fly (Vanilla / Velocity), Speed, Sprint, Bhop, Step, Jesus, Spider, NoFall, Glide, HighJump, AirJump, ElytraFly, FastLadder, AutoJump, AutoWalk, NoClip, AutoSneak
**Render** Fullbright, ESP, XRay, Zoom, Radar, HUD, Keystrokes, ArmorHud, PotionHud, Info (FPS / ping / speed / clock), NoBob
**Player** AutoRespawn, AutoTool, AutoEat, ChestStealer, Scaffold, Nuker, AutoArmor, AutoFish, AutoMine, AutoDrop
**Misc** AntiAFK, ChatSuffix, DeathCoords, PlayerAlert

Not included on purpose: server-crash and packet-exploit modules.
Not included because they need render or movement hooks that can't be checked without running the game: Freecam, Tracers, NoSlow, FastPlace, Timer, Blink.

## Layout

```
core/      Module, Setting, ModuleManager, ConfigManager, CommandManager, FriendManager
modules/   one file per category, each module is a small nested class
gui/       ClickGuiScreen, Theme
mixin/     EntityMixin (ESP glow), BlockMixin (XRay)
util/      MoveUtil, InvUtil, TargetUtil
```

Adding a module: extend `Module`, declare settings with `bool/num/mode`, override `onTick()`, and `m.add(new YourModule())` in the category's `register`.

## Known risk spots (check these first if the build or launch fails)

1. `BlockMixin` (XRay) targets `Block.shouldRenderFace(BlockState, BlockState, Direction)`. It has `require = 0`, so a mismatch only disables XRay and logs a warning instead of crashing.
2. `InvUtil.selectedSlot()` / `select()` use `Inventory.getSelectedSlot()` / `setSelectedSlot()`. These are the only two calls to fix if Mojang's names differ.
3. `NoFall` reads `Entity.fallDistance` and sends `ServerboundMovePlayerPacket.StatusOnly(boolean, boolean)`. `Criticals` sends `ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horizontalCollision)`.
4. `ClickGuiScreen.mouseClicked(MouseButtonEvent, boolean)` uses the 1.21.9+ input event API.
5. `HudRenderCallback` is deprecated in recent Fabric API but still present for 1.21.11. If a later Fabric API removes it, switch to `HudElementRegistry`.
6. `AutoFish` detects a bite by watching the bobber dip, which depends on how the server syncs it. If it misses bites or reels too early, change its Sensitivity setting.
7. `NoClip` lets you end up inside blocks. Turning it off there leaves you stuck, so move to open air first.

## Using it

Most multiplayer servers ban cheat clients. Singleplayer, your own server, and servers that explicitly allow it (anarchy servers, for example) are the safe places to use it.

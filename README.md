# Nightfall

A utility client for **Minecraft 1.21.11** (Fabric, Mojang mappings). 30 modules, a ClickGUI, a HUD, dot-commands and per-module keybinds.

## Build and run

```
gradle build             # jar ends up in build/libs/nightfall-1.0.0.jar (Gradle 9.2+; no wrapper included)
gradle runClient         # launch a dev client
```

Needs Java 21. Drop the jar in `mods/` next to Fabric Loader 0.18.1+ and Fabric API.

## Get the jar from GitHub (no local setup)

The project includes `.github/workflows/build.yml`, which builds the jar on GitHub's servers.

1. Create a new **private** repository on github.com (empty, no README).
2. Upload everything in this folder to it. Easiest: on the new repo page click **uploading an existing file**, drag in the *contents* of the unzipped folder (including the hidden `.github` folder), and commit.
3. Open the **Actions** tab. The *Build Nightfall* run starts by itself. When it turns green, open it and download **nightfall-jar** from the Artifacts section at the bottom. Unzip that and you have `nightfall-1.0.0.jar`.
4. Later builds: push a change, or use **Actions > Build Nightfall > Run workflow**. Pushing a tag like `v1.0.1` also attaches the jar to a GitHub Release.

If the run turns red, open it, click the *Build* step and copy the error lines. They show exactly which line needs fixing.

> **Not compiled yet.** This was written without access to the Minecraft jars, so it has only been syntax-checked.
> If `gradle build` reports errors, they will almost certainly be in the spots listed under *Known risk spots*.

## Controls

| Action | How |
|---|---|
| Open ClickGUI | **Right Shift** (rebindable) |
| Toggle a module | Left click it in the GUI, or bind a key in **Options > Controls > Key Binds > Nightfall** (every module has an unbound toggle key) |
| Module settings | Right click a module in the GUI to expand it. Left click a setting = next / up, right click = previous / down |
| Chat commands | `.help`, `.list`, `.toggle <module>`, `.settings <module>`, `.set <module> <setting> <value>`, `.gui`, `.panic` |

Commands never leave your client. Settings are saved to `config/nightfall.json`. Modules always start off (except HUD).

## Modules

**Combat** KillAura, TriggerBot, AutoClicker, Criticals, Reach, Velocity, AutoTotem
**Movement** Fly (Vanilla / Velocity), Speed, Sprint, Bhop, Step, Jesus, Spider, NoFall, Glide
**Render** Fullbright, ESP, XRay, Zoom, Radar, HUD
**Player** AutoRespawn, AutoTool, AutoEat, ChestStealer, Scaffold, Nuker
**Misc** AntiAFK, ChatSuffix

Not included on purpose: server-crash and packet-exploit modules.
Not included because they need render or movement hooks I couldn't verify offline: Freecam, Tracers, NoSlow, FastPlace, Timer.

## Layout

```
core/      Module, Setting, ModuleManager, ConfigManager, CommandManager
modules/   one file per category, each module is a small nested class
gui/       ClickGuiScreen
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

## Using it

Most multiplayer servers ban cheat clients. Singleplayer, your own server, and servers that explicitly allow it (anarchy servers, for example) are the safe places to use it.

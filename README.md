# YesClient 1.2.1

YesClient is a modular client-side mod for Minecraft Java Edition 1.8.9 and Forge. It combines visual tools, movement and quality-of-life options, HUD customization, inventory utilities, and server-specific tools in one in-game interface.

Some modules alter gameplay or send non-vanilla actions. Use them only where the server rules allow them.

## Requirements

- Minecraft Java Edition 1.8.9
- Minecraft Forge 1.8.9 (11.15.1.2318)
- Java 8 for the game and for building from source

## Install

1. Install and launch the Forge 1.8.9 profile once.
2. Close Minecraft.
3. Copy `YesClient-1.2.1.jar` into `.minecraft/mods`.
4. Launch the Forge 1.8.9 profile.

Keep one YesClient JAR in the `mods` folder so an older copy is not loaded alongside it. Press **Right Shift** in game to open the ClickGUI.

## 1.2.1 highlights

- **Motion Blur:** blend recent rendered frames with an adjustable strength control.
- **Chat customization:** highlight whole-word keywords, adjust chat background opacity and text shadow, and tune the keyword alert pitch from 0 to 2.
- **Blink controls:** optionally pace queued packet release across ticks and automatically disable Blink at a configurable packet limit from 0 to 50.
- **Detector refinements:** reduce Pattern Detection scan load and update repeated Griefer Detector alerts in place, including block destruction.
- **Module organization:** source folders now follow the in-game module categories; Media Overlay is listed under Visuals.
- **HUD editor:** move and resize HUD elements, adjust scaling, and customize supported elements individually.
- **Media Overlay:** show the active Windows media session with artwork, title, artist, and playback progress; customize the card appearance in the module settings.
- **FreeLook:** orbit the view around the player while movement remains character-relative.
- **Saved Hotbars:** save and load up to nine hotbars, including item NBT. The default shortcuts are `C` + `1–9` to save or overwrite and `X` + `1–9` to load.
- **Block ESP:** search for selected blocks with bounded incremental scanning and rendering.
- **Profiles and accounts:** manage client profiles and use the Microsoft account sign-in flow in the account manager.
- **Module controls:** configure settings, keybinds, HUD readouts, and visual options in the ClickGUI.

## Module areas

- **Visuals:** Block ESP, entity and storage ESP, tracers, nametags, FreeCam, FreeLook, scoreboard, and HUD readouts such as coordinates, FPS, CPS, ping, and TPS.
- **Movement and quality of life:** creative flight, sprint, SafeWalk, Zoom, GUI Move, inventory tweaks, and Anti Void Lag.
- **Building and inventory:** FastPlace, FastBreak, Ghost Blocks, saved hotbars, item tools, and NBT utilities.
- **Moderation:** crash, griefer, nickname, and pattern detection tools.
- **Client:** ClickGUI, HUD Designer, module list, chat, friends, Fancy Text, and optional Discord Rich Presence.
- **Server-sensitive tools:** projectile assistance, Blink, packet and item utilities, and other modules that may violate server rules or trigger server protections.

The in-game module descriptions and settings are the definitive reference for each module’s behavior.

## Local data

YesClient stores its settings and data under `.minecraft/yesclient/`. Item collections and saved hotbars use compressed Minecraft NBT data. Current storage files use `.dat` names; older item-storage files are migrated locally when loaded. Existing account and profile data remains in the YesClient data folder.

## Network and privacy

The current build does not include the old presence enrollment or heartbeat service. Network requests occur when a feature needs them, such as Microsoft/Xbox/Minecraft account sign-in, Mojang profile lookup, or external skin-face images. Discord Rich Presence communicates with the local Discord application over IPC when enabled. Account and profile files can contain sensitive data; do not share them publicly.

## Build from source

Use JDK 8 with the included Gradle wrapper:

```text
gradlew.bat clean assemble
```

The JAR is written to `build/libs/YesClient-1.2.1.jar`.

## Project documentation

- [Architecture notes](docs/ARCHITECTURE.md)
- [Developer documentation index](docs/README.md)
- [YesClient 1.2.1 GitHub release notes](RELEASE_NOTES_1.2.1.md)
- [Previous 1.2.0 release notes](RELEASE_NOTES_1.2.0.md)
- [License](LICENSE)

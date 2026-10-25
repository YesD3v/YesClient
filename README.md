# YesClient 1.2.6

YesClient is a client-side mod for Minecraft Java Edition 1.8.9 and Forge. It combines visual tools, movement options, moderation utilities, inventory helpers, and HUD customization in an in-game interface.

Some modules alter gameplay or send non-vanilla actions. Use them only where the server rules allow them.

## What's new in 1.2.6

- **PP Builder:** automatically builds the configured vertical or horizontal template on the block you're targeting. Choose your held block or the stained-clay preset, preview pending and placed blocks, and have the module stop when the build finishes.
- **Nick Detector:** checks the UUID version supplied with tab-list player profiles. Version 1 is marked as nicked, version 4 is treated as a real player, and version 3 NPCs are ignored. Confirmed nicks receive a client chat alert; no API key or external lookup is used.
- **YesArmor:** adds banner and wool color cycles to the head slot, with monochrome and rainbow palettes, and restores replaced armor when disabled. Survival-mode auto-toggle behavior is more predictable.
- **Startup and ClickGUI:** adds the YesClient logo draw-in and draw-out animation, and prepares ClickGUI fonts and textures during startup.
- **FreeCam and FreeLook:** improves interaction handling and camera targeting; FreeCam outlines nearby entities without the removed block overlays.
- **ESP and tracers:** adds item-frame and storage tracer targets and improves tracer/render origins and ESP integration.
- **Media Overlay:** improves playback timing and progress display, artwork handling, and long title/artist scrolling.
- **Flight and moderation:** separates Creative Flight from the survival Fly exploit, and improves repeated Griefer Detector alerts.
- **Organization and rendering:** keeps module source directories aligned with ClickGUI categories and includes related rendering fixes.

See [RELEASE_NOTES_1.2.6.md](RELEASE_NOTES_1.2.6.md) for the full release notes.

## Requirements

- Minecraft Java Edition 1.8.9
- Minecraft Forge 1.8.9 (11.15.1.2318)
- Java 8 for the game and for building from source

## Install

1. Install and launch the Forge 1.8.9 profile once.
2. Close Minecraft.
3. Copy `YesClient-1.2.6.jar` into `.minecraft/mods`.
4. Launch the Forge 1.8.9 profile.

Keep one YesClient JAR in the `mods` folder so an older copy is not loaded alongside it. Press **Right Shift** in game to open the ClickGUI.

## Module areas

- **Visuals:** ESP, storage ESP, tracers, nametags, FreeCam, FreeLook, Media Overlay, YesArmor, and HUD readouts.
- **Movement and quality of life:** Creative Flight, sprint, SafeWalk, Zoom, GUI Move, inventory helpers, and Anti Void Lag.
- **Exploit and inventory:** Fly, PP Builder, FastPlace, FastBreak, saved hotbars, and NBT utilities.
- **Moderation:** Nick Detector, Griefer Detector, Pattern Detection, and other server/player checks.
- **Client:** ClickGUI, HUD Designer, module list, chat, friends, Fancy Text, and optional Discord Rich Presence.

The in-game module descriptions and settings are the definitive reference for each module's behavior.

## Local data and privacy

YesClient stores settings and client data under `.minecraft/yesclient/`. Nick Detector reads the UUID version already present in the server's player list and makes no external API request. Network access is used only by features that need it, such as Microsoft account sign-in and external skin-face images. Discord Rich Presence communicates with the local Discord application over IPC when enabled. Account and profile files can contain sensitive data; do not share them publicly.

## Build from source

Use JDK 8 with the included Gradle wrapper:

```text
gradlew.bat clean assemble
```

The JAR is written to `build/libs/YesClient-1.2.6.jar`.

## Project documentation

- [Architecture notes](docs/ARCHITECTURE.md)
- [Developer documentation index](docs/README.md)
- [YesClient 1.2.6 release notes](RELEASE_NOTES_1.2.6.md)
- [Previous 1.2.1 release notes](RELEASE_NOTES_1.2.1.md)
- [Previous 1.2.0 release notes](RELEASE_NOTES_1.2.0.md)
- [License](LICENSE)

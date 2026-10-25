# YesClient 1.2.7

YesClient is a client-side mod for Minecraft Java Edition 1.8.9 and Forge. It combines visual tools, movement options, moderation utilities, inventory helpers, and HUD customization in an in-game interface.

Some modules alter gameplay or send non-vanilla actions. Use them only where the server rules allow them.

## What's new in 1.2.7

- **Item Vault:** a creative tab that replaces saved hotbars. Click with an item on the cursor to store a copy, take stored items as often as you like, and Ctrl+Click to remove one. It has its own search bar.
- **Packet Monitor:** a HUD readout of packets sent to the server per tick, with an optional disconnect when outgoing packets in one tick reach a limit. Its position can be set in the HUD Designer.
- **Pattern Detection:** also flags two-tone PP builds (such as PP Builder's white clay preset with a red tip) and small PPs standing on the ground.
- **Griefer Detector:** ignores blocks you break yourself and checks what a player is looking at with a block raytrace.
- **Blink:** replays every held packet in order when disabled, and released packets show up in Packet Monitor.
- **FreeCam and FreeLook:** turning one on turns the other off.
- **Active Effects:** can hide the vanilla effect cards in the inventory and keep it centered.
- **ClickGUI:** changing the font applies immediately; Snap to Grid snaps HUD elements to the screen's quarter lines.
- **Search:** tracers start at the crosshair.
- **Media Overlay:** album art detection works for any media app, not just Brave.

Full release notes are on the [Releases page](https://github.com/YesD3v/YesClient/releases).

## Requirements

- Minecraft Java Edition 1.8.9
- Minecraft Forge 1.8.9 (11.15.1.2318)
- Java 8 for the game and for building from source

## Install

1. Install and launch the Forge 1.8.9 profile once.
2. Close Minecraft.
3. Copy `YesClient-1.2.7.jar` into `.minecraft/mods`.
4. Launch the Forge 1.8.9 profile.

Keep one YesClient JAR in the `mods` folder so an older copy is not loaded alongside it. Press **Right Shift** in game to open the ClickGUI.

## Module areas

- **Visuals:** ESP, storage ESP, tracers, nametags, FreeCam, FreeLook, Media Overlay, YesArmor, Packet Monitor, and HUD readouts.
- **Movement and quality of life:** Creative Flight, sprint, SafeWalk, Zoom, GUI Move, inventory helpers, and Anti Void Lag.
- **Exploit and inventory:** Fly, PP Builder, FastPlace, FastBreak, Item Vault, and NBT utilities.
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

The JAR is written to `build/libs/YesClient-1.2.7.jar`.

## License

See [LICENSE](LICENSE).

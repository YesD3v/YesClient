# YesClient 1.2.6

YesClient 1.2.6 adds PP Builder and a local UUID-based Nick Detector, and improves startup, flight behavior, visuals, and media display.

## Added

- **PP Builder:** places a five-block template on the targeted block in vertical or horizontal orientation. Choose the held block or a preset made from white stained clay with a red tip. Yellow outlines show pending blocks and green outlines show placed blocks; the module disables after completion.
- **Branded startup animation:** draws and erases the YesClient logo and wordmark. ClickGUI fonts and textures are prepared during the intro to reduce first-open stutter.
- **Nick Detector chat alert:** reports confirmed nicked players as `[Nick Detector] {username} is nicked!`.

## Changed

- Nick Detector now reads the UUID version in each server player profile directly: version 1 is flagged, version 4 is treated as a real player, and version 3 NPCs are ignored. It marks nicked names in the tab list and nametags without Pastebin, an API key, or network requests.
- YesArmor now supports banner and wool color cycles in the head slot, with monochrome and rainbow palettes. Its item name, cycle order, update pacing, armor restoration, and survival-mode auto-toggle behavior were refined.
- Creative Flight is limited to Creative mode; the separate Fly exploit module handles survival-mode double-tap flight.
- FreeCam interaction handling and FreeLook camera targeting were refined. FreeCam keeps nearby entity outlines and no longer draws the removed block overlays.
- Tracers can target item frames and supported storage blocks, using Storage ESP colors. ESP, storage ESP, and tracer rendering received related integration fixes.
- Media Overlay playback time and progress display were stabilized, album art handling was improved, and long title/artist text scrolling was refined.
- Repeated Griefer Detector alerts continue updating their existing chat line instead of spamming duplicates.
- Module source folders remain aligned with ClickGUI categories; supporting GUI and rendering code was updated.

## Install

Requires **Minecraft Java Edition 1.8.9** with **Forge 11.15.1.2318**. Close Minecraft, place `YesClient-1.2.6.jar` in `.minecraft/mods`, then launch the Forge 1.8.9 profile. Remove older YesClient JARs first so Forge loads only one copy.

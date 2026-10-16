# YesClient 1.2.0 release notes

YesClient 1.2.0 focuses on HUD and visual customization, inventory workflows, and account handling. These notes describe the 1.2.0 changes; this repository did not contain a 1.1.0 release-notes file for a line-by-line comparison.

## Added

- **FreeLook** camera orbit with adjustable session distance and hold-to-use behavior.
- **Media Overlay** for active Windows media sessions, including album art, title, artist, progress, and card styling options.
- **Saved Hotbars** in the creative inventory, with nine save slots, item NBT preservation, and `C` + number / `X` + number shortcuts.
- Per-element HUD editing, scaling, resizing, and supported widget appearance settings.
- A Block ESP search display for locating selected blocks.

## Improved

- Projectile aim assistance and trajectory visualization, including an optional impact marker and a Blatant Mode gate for aimbot mode.
- ClickGUI module settings, keybind capture, and HUD layout controls.
- Account manager flows for saved profiles and Microsoft sign-in.
- HUD readouts and nametag options, including configurable text and display details.

## Fixed and refined

- Saved hotbar slots now overwrite the selected slot and preserve full item data.
- Media Overlay artwork, text overflow, scrolling, sizing, and playback progress handling.
- HUD editor layout and per-element interactions.
- Ping display behavior when server data is unavailable.
- Mod metadata now consistently reports version 1.2.0.

## Maintenance

- The release JAR excludes the stale decompiled bundle and its inactive presence-service code.
- Replaced the old HC-related tab label with a YesClient-specific label.
- Renamed local item-storage files to `.dat`; existing NBT files are migrated without discarding saved items.

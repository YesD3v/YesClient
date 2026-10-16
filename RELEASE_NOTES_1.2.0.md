# YesClient 1.2.0

YesClient 1.2.0 brings a redesigned HUD workflow, new visual and inventory features, and fixes across account handling and the ClickGUI.

## Highlights

- **HUD editor:** Resize and scale HUD elements, position them more precisely, and customize supported element settings from the editor.
- **Media Overlay:** Display the current Windows media session with album art, title, artist, and playback progress, with more appearance controls.
- **FreeLook:** Orbit the view around your player with adjustable camera distance while keeping movement character-relative.
- **Saved Hotbars:** Save or overwrite any of nine hotbar slots with `C` + `1–9`, then load a slot with `X` + `1–9`. Item NBT is preserved.
- **Block ESP:** Find selected blocks with a search display and incremental scanning.
- **Account handling:** Manage accounts and profiles in the client, including Microsoft sign-in and account recovery flows.

## Improvements and fixes

- Improved ClickGUI module settings, keybind capture, scrolling, and HUD element interactions.
- Refined projectile aim-assist visualization, including the optional impact marker and Blatant Mode control for aimbot mode.
- Improved HUD readouts and nametag display options.
- Fixed saved hotbar slot replacement and item data display.
- Fixed Media Overlay artwork fallback, text overflow and scrolling, sizing, and playback progress updates.
- Fixed HUD editor layout and element interaction issues.
- Improved ping display when server ping data is unavailable.
- Updated local item-storage filenames; existing saved NBT data migrates when loaded.

## Install

Requires **Minecraft Java Edition 1.8.9** with **Forge 11.15.1.2318**. Close Minecraft and place `YesClient-1.2.0.jar` in your `.minecraft/mods` folder, then launch the Forge 1.8.9 profile.

# YesClient 1.2.1

YesClient 1.2.1 adds motion blur and chat customization, improves Blink controls and detector performance, and aligns module source folders with the in-game categories.

## Added

- **Motion Blur:** a Visuals module with an adjustable strength setting.
- **Chat customization:** keyword highlighting, whole-word matching, background opacity, text-shadow control, and an orb alert with a customizable pitch from 0 to 2.
- **Blink Smart Release:** optionally spreads queued packets across following client ticks after Blink ends.
- **Blink Packet Limit:** automatically disables Blink after the selected number of buffered packets; set the limit to 0 to disable it.

## Improved

- **Pattern Detection:** lower per-tick scanning and validation work, bounded queues, and stricter pattern matching reduce false positives and performance spikes.
- **Griefer Detector:** repeated flagged-item and block-destruction alerts update their existing chat line with a count instead of flooding chat.
- **Module organization:** source folders follow the in-game category layout, and Media Overlay is categorized under Visuals.

## Install

Requires **Minecraft Java Edition 1.8.9** with **Forge 11.15.1.2318**. Close Minecraft and place `YesClient-1.2.1.jar` in your `.minecraft/mods` folder, then launch the Forge 1.8.9 profile.

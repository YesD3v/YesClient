# Project Architecture

## Package structure

```
com.yesclient/
├── YesClient.java          # Main mod entry point
├── altmanager/                 # Cookie / session alt login
├── command/                    # Chat commands (.help, .bind, etc)
├── config/                     # Config + profiles
├── event/                      # Event handlers
├── gui/                        # ClickGUI, HUD, alt GUIs
├── imagetonbt/                 # Image → NBT helpers
├── itemlog/                    # Item logging
├── mixin/                      # Mixins (including mod-list filtering)
├── module/                     # Module system
│   ├── Module.java
│   ├── ModuleManager.java
│   └── modules/                # building, client, combat, exploit, …
├── storage/                    # Item storage
└── utils/                      # Chat, render, fonts, etc.
```

## Startup flow

1. `YesClient` initializes managers, GUI, mixins wiring
2. Config, profiles, modules, keybinds, saved hotbars, and local item data load from `.minecraft/yesclient/`
3. The mod registers its client events and renders enabled modules and HUD elements

There is no separate license-key gate in the current tree. Older docs that mentioned `com.yesclient.license.*` are obsolete.

## Key classes

| Class | Purpose |
|-------|---------|
| `YesClient` | Mod entry, managers |
| `Module` / `ModuleManager` | Module base + registry |
| `ClickGUI` / `LegacyClickGUI` | In-game settings UI |
| `HudEditorGUI` / `HudEditorElement` | HUD layout editor |
| `SavedHotbarStorage` | Local hotbar snapshots with item NBT |
| `CookieAltManager` | Cookie → Microsoft / Xbox / MC session |

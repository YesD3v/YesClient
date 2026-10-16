# BUGFIX: ItemLogTab Obfuscation Issue

## Problem Summary

The `ItemLogTab` creative tab appeared in the creative menu but displayed no items, even when `ItemLogManager` had items saved. The tab name also showed its raw translation key instead of the expected "NBT Logger".

## Root Cause

**Obfuscation Mismatch**: The `ItemLogTab` class (originally in `com.yesclient.itemlog` package) was being obfuscated to `h.l1` by the build process. This caused Minecraft's `CreativeTabs` system to fail to properly call the overridden methods (`getTranslatedTabLabel()`, `displayAllReleventItems()`).

In contrast, the working `ItemStealerTab` class (in `com.yesclient.storage` package) was correctly preserved as `com.yesclient.storage.ItemStealerTab`.

## Diagnosis

1. **Log Output**: The Creative Tabs dump in `postInit()` showed:
   ```
   Index: 13 Label: yesclient_nbt_logger Class: h.l1
   Index: 14 Label: itemstealer Class: com.yesclient.storage.ItemStealerTab
   ```

2. **Symptoms**:
   - Tab icon (Diamond) worked and updated correctly.
   - Tab name showed raw translation key `itemGroup.yesclient_nbt_logger` instead of "NBT Logger".
   - `displayAllReleventItems()` was never called (no items displayed).

3. **Testing**: Hardcoding a debug item directly in `displayAllReleventItems()` still showed nothing, confirming the method was not being invoked.

## Solution

Move `ItemLogTab.java` from `com.yesclient.itemlog` package to `com.yesclient.storage` package (same location as the working `ItemStealerTab`).

### Files Changed

1. **Moved**: `ItemLogTab.java` from `itemlog/` to `storage/`
2. **Updated**: Package declaration in `ItemLogTab.java`
3. **Updated**: Import in `YesClient.java`
4. **Deleted**: Old `ItemLogTab.java` in `itemlog/`

## Prevention

When creating new `CreativeTabs` subclasses:

1. **Place in `com.yesclient.storage` package** - This package appears to be correctly handled by the obfuscator/reobfuscator.
2. **Check log output** - After building, verify the class name in the "Creative Tabs Dump" log shows the full package path, not an obfuscated name like `h.XX`.
3. **Test method overrides** - Add debug logging to verify overridden methods are actually being called.

## Related Files

- `src/main/java/com/yesclient/storage/ItemLogTab.java`
- `src/main/java/com/yesclient/itemlog/ItemLogManager.java`
- `src/main/java/com/yesclient/YesClient.java`

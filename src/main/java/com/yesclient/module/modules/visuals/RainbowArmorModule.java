package com.yesclient.module.modules.visuals;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RainbowArmorModule extends Module {

    private static final String DISPLAY_NAME = "\u00A7f\u00A7lYes\u00A77\u00A7lClient \u00A7e\u00A7lON TOP";
    private static final String BANNER_ITEM = "minecraft:banner";
    private static final String WOOL_ITEM = "minecraft:wool";
    private static final int[] MONOCHROME_COLORS = { 15, 7, 8, 0, 8, 7 };
    private static final int[] RAINBOW_COLORS = { 14, 12, 1, 4, 5, 13, 9, 3, 11, 10, 2, 6 };
    private static final String[] ARMOR_ITEMS = {
            "minecraft:leather_helmet",
            "minecraft:leather_chestplate",
            "minecraft:leather_leggings",
            "minecraft:leather_boots"
    };
    private static final int[] ARMOR_SLOTS = { 5, 6, 7, 8 };

    private final NumberSetting speed = new NumberSetting("Speed", "Gradient cycle speed", 1.0, 0.1, 5.0, 0.1);
    private final NumberSetting updateRate = new NumberSetting("Update Rate", "Ticks between color updates", 5, 1, 20);
    private final BooleanSetting excludeHelmet = new BooleanSetting("Exclude Helmet Slot", "Don't change the head slot when enabled", false);
    private final ModeSetting headSlotItem = new ModeSetting(
            "Head Slot Item", "Cycle armor, banners, or wool in the head slot", "Armor", "Armor", "Banner", "Wool");
    private final ModeSetting headPalette = new ModeSetting(
            "Head Palette", "Color sequence for banner and wool headwear", "Monochrome", "Monochrome", "Rainbow");
    private final BooleanSetting autoCreative = new BooleanSetting("Auto Toggle", "Re-enable after a survival-mode disable", false);

    private int tickCounter;
    private float phase;
    private final int[] lastColor = new int[4];
    private final ItemStack[] originalArmor = new ItemStack[4];
    private float headPaletteAccumulator;
    private int headPaletteOffset;
    private String activeHeadItem = "Armor";
    private String activeHeadPalette = "Monochrome";
    private boolean wasCreative;
    private boolean armorCaptured;
    private boolean autoEnablePending;
    private boolean survivalDisableInProgress;

    private final Object globalTicker = new Object() {
        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

            boolean isCreative = mc.thePlayer.capabilities.isCreativeMode;
            if (isCreative && !wasCreative && autoEnablePending) {
                autoEnablePending = false;
                if (autoCreative.isEnabled() && !isEnabled()) setEnabled(true);
            }
            wasCreative = isCreative;
        }
    };

    public RainbowArmorModule() {
        super("YesArmor", "Animated armor, banner, or wool gradient (Creative only)", Category.VISUALS,
                ModuleMode.BOTH);
        addSetting(speed);
        addSetting(updateRate);
        addSetting(excludeHelmet);
        addSetting(headSlotItem);
        addSetting(headPalette);
        addSetting(autoCreative);

        for (int i = 0; i < lastColor.length; i++) lastColor[i] = -1;
        MinecraftForge.EVENT_BUS.register(globalTicker);
    }

    @Override
    public boolean isAvailable() {
        return mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled && isEnabled() && !survivalDisableInProgress) {
            autoEnablePending = false;
        }
        super.setEnabled(enabled);
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            com.yesclient.utils.ChatUtils.sendClientMessage(
                    "\u00A7c\u00A7lYesArmor \u00A7r\u00A7cYou must be in Creative mode to use this module.");
            setEnabled(false);
            return;
        }

        for (int i = 0; i < originalArmor.length; i++) {
            ItemStack stack = mc.thePlayer.inventory.armorInventory[3 - i];
            originalArmor[i] = stack == null ? null : stack.copy();
            lastColor[i] = -1;
        }
        armorCaptured = true;
        tickCounter = 0;
        phase = 0.0f;
        headPaletteAccumulator = 0.0f;
        headPaletteOffset = 0;
        activeHeadItem = headSlotItem.getValue();
        activeHeadPalette = headPalette.getValue();
        sendArmorUpdate();
    }

    @Override
    protected void onDisable() {
        restoreArmor();
        armorCaptured = false;
        tickCounter = 0;
        for (int i = 0; i < lastColor.length; i++) lastColor[i] = -1;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            autoEnablePending = true;
            survivalDisableInProgress = true;
            try {
                setEnabled(false);
            } finally {
                survivalDisableInProgress = false;
            }
            return;
        }

        if (!activeHeadItem.equals(headSlotItem.getValue()) || !activeHeadPalette.equals(headPalette.getValue())) {
            activeHeadItem = headSlotItem.getValue();
            activeHeadPalette = headPalette.getValue();
            headPaletteAccumulator = 0.0f;
            headPaletteOffset = 0;
            lastColor[0] = -1;
            sendArmorUpdate();
        }

        tickCounter++;
        if (tickCounter < updateRate.getIntValue()) return;
        tickCounter = 0;

        phase += (float) (speed.getValue() * 0.1);
        if (phase >= (Math.PI * 2)) phase -= (float) (Math.PI * 2);

        int[] palette = getHeadColors();
        headPaletteAccumulator += speed.getValue().floatValue() * updateRate.getIntValue() / 20.0f;
        int steps = (int) headPaletteAccumulator;
        if (steps > 0) {
            headPaletteOffset = (headPaletteOffset + steps) % palette.length;
            headPaletteAccumulator -= steps;
        }

        sendArmorUpdate();
    }

    private int[] getHeadColors() {
        return headPalette.is("Rainbow") ? RAINBOW_COLORS : MONOCHROME_COLORS;
    }

    private void sendArmorUpdate() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (i == 0 && excludeHelmet.isEnabled()) continue;

            if (i == 0 && !headSlotItem.is("Armor")) {
                sendHeadItem();
                continue;
            }

            float slotPhaseOffset = i * 0.5f;
            float sineValue = (float) Math.sin(phase + slotPhaseOffset);
            float normalizedValue = (sineValue + 1.0f) / 2.0f;
            int intensity = Math.max(0, Math.min(255, (int) (normalizedValue * 255.0f)));
            int rgb = (intensity << 16) | (intensity << 8) | intensity;
            if (lastColor[i] == rgb) continue;

            ItemStack armorStack = createDyedArmor(i, rgb);
            if (armorStack == null) continue;
            mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[i], armorStack));
            lastColor[i] = rgb;
        }
    }

    private void sendHeadItem() {
        int[] colors = getHeadColors();
        int color = colors[headPaletteOffset % colors.length];
        Item item = Item.getByNameOrId(headSlotItem.is("Banner") ? BANNER_ITEM : WOOL_ITEM);
        if (item == null) return;

        ItemStack current = mc.thePlayer.inventory.armorInventory[3];
        if (lastColor[0] == color && isGeneratedHeadItem(current)) return;

        ItemStack stack = new ItemStack(item, 1, color);
        stack.setStackDisplayName(DISPLAY_NAME);
        mc.thePlayer.inventory.armorInventory[3] = stack.copy();
        mc.thePlayer.inventory.markDirty();
        mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[0], stack));
        lastColor[0] = color;
    }

    private boolean isGeneratedHeadItem(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        if (item == Item.getByNameOrId(BANNER_ITEM) || item == Item.getByNameOrId(WOOL_ITEM)) {
            return DISPLAY_NAME.equals(stack.getDisplayName()) || stack.getMetadata() == lastColor[0];
        }
        return item == Item.getByNameOrId(ARMOR_ITEMS[0]) && hasDyedDisplayTag(stack);
    }

    private boolean hasDyedDisplayTag(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return false;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        return display.hasKey("color");
    }

    public boolean allowsHeadWearable(ItemStack stack) {
        if (!isEnabled() || excludeHelmet.isEnabled() || stack == null) return false;
        if (headSlotItem.is("Banner")) return stack.getItem() == Item.getByNameOrId(BANNER_ITEM);
        if (headSlotItem.is("Wool")) return stack.getItem() == Item.getByNameOrId(WOOL_ITEM);
        return false;
    }

    private ItemStack createDyedArmor(int armorIndex, int color) {
        Item item = Item.getByNameOrId(ARMOR_ITEMS[armorIndex]);
        if (item == null) return null;

        ItemStack stack = new ItemStack(item, 1, 0);
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setInteger("color", color);
        display.setString("Name", DISPLAY_NAME);
        root.setTag("display", display);
        stack.setTagCompound(root);
        return stack;
    }

    private boolean isGeneratedArmorStack(int index, ItemStack stack) {
        if (stack == null) return false;
        if (index == 0 && isGeneratedHeadItem(stack)) return true;
        Item expected = Item.getByNameOrId(ARMOR_ITEMS[index]);
        return stack.getItem() == expected && hasDyedDisplayTag(stack);
    }

    private void restoreArmor() {
        if (!armorCaptured || mc.thePlayer == null) return;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack current = mc.thePlayer.inventory.armorInventory[3 - i];
            ItemStack original = originalArmor[i];
            if (current == null || isGeneratedArmorStack(i, current)
                    || (original != null && ItemStack.areItemStacksEqual(current, original))) {
                ItemStack restore = original == null ? null : original.copy();
                mc.thePlayer.inventory.armorInventory[3 - i] = restore == null ? null : restore.copy();
                if (mc.thePlayer.capabilities.isCreativeMode && mc.getNetHandler() != null) {
                    mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[i], restore));
                }
            }
        }
        mc.thePlayer.inventory.markDirty();
    }
}

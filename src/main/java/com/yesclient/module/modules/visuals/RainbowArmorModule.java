package com.yesclient.module.modules.visuals;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RainbowArmorModule extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", "Gradient cycle speed", 1.0, 0.1, 5.0, 0.1);
    private final NumberSetting updateRate = new NumberSetting("Update Rate", "Ticks between color updates", 5, 1, 20);
    private final BooleanSetting excludeHelmet = new BooleanSetting("Exclude Helmet Slot", "Don't change the helmet slot when enabled", false);
    private final BooleanSetting autoCreative = new BooleanSetting("Auto Toggle", "Automatically enable when entering creative mode", false);

    private int tickCounter = 0;
    private float phase = 0.0f;
    private final int[] lastColor = new int[4];
    private boolean wasCreative = false;

    private static final String[] ARMOR_ITEMS = {
            "minecraft:leather_helmet",
            "minecraft:leather_chestplate",
            "minecraft:leather_leggings",
            "minecraft:leather_boots"
    };

    private static final int[] ARMOR_SLOTS = { 5, 6, 7, 8 };

    private final Object globalTicker = new Object() {
        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;
            
            boolean isCreative = mc.thePlayer.capabilities.isCreativeMode;
            if (isCreative && !wasCreative && autoCreative.isEnabled() && !isEnabled()) {
                setEnabled(true);
            }
            wasCreative = isCreative;
        }
    };

    public RainbowArmorModule() {
        super("YesArmor", "Moving white-to-black leather armor gradient (Creative only)", Category.VISUALS, ModuleMode.BOTH);
        addSetting(speed);
        addSetting(updateRate);
        addSetting(excludeHelmet);
        addSetting(autoCreative);

        for (int i = 0; i < lastColor.length; i++) {
            lastColor[i] = -1;
        }
        
        MinecraftForge.EVENT_BUS.register(globalTicker);
    }

    @Override
    public boolean isAvailable() {
        return mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode;
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            com.yesclient.utils.ChatUtils.sendClientMessage(
                    "\u00A7c\u00A7lYesArmor \u00A7r\u00A7cYou must be in Creative mode to use this module.");
            setEnabled(false);
            return;
        }

        tickCounter = 0;
        phase = 0.0f;
        for (int i = 0; i < lastColor.length; i++) {
            lastColor[i] = -1;
        }
        sendArmorUpdate();
    }

    @Override
    protected void onDisable() {
        if (mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode) {
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                if (i == 0 && excludeHelmet.isEnabled())
                    continue;
                mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[i], null));
            }
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (!mc.thePlayer.capabilities.isCreativeMode) {
            setEnabled(false);
            return;
        }

        tickCounter++;
        if (tickCounter < updateRate.getIntValue()) {
            return;
        }
        tickCounter = 0;

        phase += (float) (speed.getValue() * 0.1);
        if (phase >= (Math.PI * 2)) {
            phase -= (float) (Math.PI * 2);
        }

        sendArmorUpdate();
    }

    private void sendArmorUpdate() {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (i == 0 && excludeHelmet.isEnabled())
                continue;

            float slotPhaseOffset = i * 0.5f;

            float sineValue = (float) Math.sin(phase + slotPhaseOffset);

            float normalizedValue = (sineValue + 1.0f) / 2.0f;

            int intensity = (int) (normalizedValue * 255.0f);

            intensity = Math.max(0, Math.min(255, intensity));

            int rgb = (intensity << 16) | (intensity << 8) | intensity;

            if (lastColor[i] == rgb) {
                continue;
            }

            ItemStack armorStack = createDyedArmor(i, rgb);
            if (armorStack == null)
                continue;

            mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[i], armorStack));
            lastColor[i] = rgb;
        }
    }

    private ItemStack createDyedArmor(int armorIndex, int color) {
        net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(ARMOR_ITEMS[armorIndex]);
        if (item == null)
            return null;

        ItemStack stack = new ItemStack(item, 1, 0);
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();

        display.setInteger("color", color);
        root.setTag("display", display);
        stack.setTagCompound(root);

        return stack;
    }
}

package com.yesclient.module.modules.miscellaneous;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.Random;

 





public class ChestStealerModule extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", "Ticks between steals", 3.0, 1.0, 10.0, 1.0);
    private final NumberSetting startDelay = new NumberSetting("Start Delay", "Ticks before starting", 5.0, 0.0, 20.0, 1.0);

    private int tickCounter = 0;
    private int startCounter = 0;
    private boolean started = false;
    private int nextDelay = 0;
    private final Random random = new Random();

    public ChestStealerModule() {
        super("Chest Stealer", "Automatically takes items from chests", Category.MISC, ModuleMode.BOTH);

        addSetting(delay);
        addSetting(startDelay);
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
        startCounter = 0;
        started = false;
        calculateNextDelay();
    }

    private void calculateNextDelay() {
        int baseDelay = Math.max(1, delay.getIntValue());
        
         
        int jitter = random.nextInt(4) - 1; 
        
         
        if (random.nextFloat() < 0.10f) {
            jitter += random.nextInt(6) + 4;  
        }

        nextDelay = Math.max(1, baseDelay + jitter);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

         
        if (!(mc.currentScreen instanceof GuiChest)) {
            started = false;
            startCounter = 0;
            return;
        }

        GuiChest gui = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) gui.inventorySlots;

         
        if (!started) {
            startCounter++;
            if (startCounter < startDelay.getIntValue()) {
                return;
            }
            started = true;
            tickCounter = 0;
            calculateNextDelay();
        }

         
        tickCounter++;
        if (tickCounter < nextDelay) {
            return;
        }
        tickCounter = 0;
        calculateNextDelay();

         
        int chestSize = container.getLowerChestInventory().getSizeInventory();

         
        for (int i = 0; i < chestSize; i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (stack != null) {
                     
                    mc.playerController.windowClick(
                            container.windowId,
                            i,
                            0,
                            1,  
                            mc.thePlayer
                    );
                    return;  
                }
            }
        }

         
    }

    @Override
    public String getDisplayInfo() {
        return delay.getIntValue() + "t (Human)";
    }
}

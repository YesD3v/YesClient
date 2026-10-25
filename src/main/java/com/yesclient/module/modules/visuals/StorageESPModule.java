package com.yesclient.module.modules.visuals;
import java.awt.Color;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.tileentity.*;
import net.minecraft.util.BlockPos;


public class StorageESPModule extends Module {

    private final BooleanSetting chests = new BooleanSetting("Chests", "Show regular chests", true);
    private final BooleanSetting trappedChests = new BooleanSetting("Trapped Chests", "Show trapped chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", "Show ender chests", true);
    private final BooleanSetting hoppers = new BooleanSetting("Hoppers", "Show hoppers", false);
    private final BooleanSetting droppers = new BooleanSetting("Droppers", "Show droppers/dispensers", false);

    private final NumberSetting range = new NumberSetting("Range", "Render range", 64, 10, 128);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "ESP line width", 2.0, 0.5, 5.0, 0.5);
    private final BooleanSetting filled = new BooleanSetting("Filled", "Fill boxes with color", true);

     
    private static final Color CHEST_COLOR = new Color(255, 165, 0);  
    private static final Color TRAPPED_COLOR = new Color(255, 50, 50);  
    private static final Color ENDER_COLOR = new Color(200, 0, 255);  
    private static final Color HOPPER_COLOR = new Color(100, 100, 100);  

    public StorageESPModule() {
        super("StorageESP", "Highlight storage blocks", Category.VISUALS, ModuleMode.BOTH);

        addSetting(chests);
        addSetting(trappedChests);
        addSetting(enderChests);
        addSetting(hoppers);
        addSetting(droppers);
        addSetting(range);
        addSetting(lineWidth);
        addSetting(filled);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        double maxRange = range.getValue();
        float baseWidth = lineWidth.getFloatValue();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            BlockPos pos = tileEntity.getPos();

             
            double distanceSq = mc.thePlayer.getDistanceSq(pos);
            if (distanceSq > maxRange * maxRange)
                continue;

            Color color = getColorForTileEntity(tileEntity);
            if (color == null)
                continue;

             
            float dist = (float) Math.sqrt(distanceSq);
            float scaledWidth = baseWidth;
            if (dist > 10) {
                scaledWidth = Math.max(0.1f, scaledWidth * (10f / dist));
            }

             
            if (filled.isEnabled()) {
                Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
                RenderUtils.drawBlockESPFilled(pos, fillColor, color, scaledWidth);
            } else {
                RenderUtils.drawBlockESP(pos, color, scaledWidth);
            }
        }
    }

    public Color getColorForTileEntity(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) tileEntity;
            if (chest.getChestType() == 1) {
                 
                if (trappedChests.isEnabled()) {
                    return TRAPPED_COLOR;
                }
            } else {
                if (chests.isEnabled()) {
                    return CHEST_COLOR;
                }
            }
        } else if (tileEntity instanceof TileEntityEnderChest) {
            if (enderChests.isEnabled()) {
                return ENDER_COLOR;
            }
        } else if (tileEntity instanceof TileEntityHopper) {
            if (hoppers.isEnabled()) {
                return HOPPER_COLOR;
            }
        } else if (tileEntity instanceof TileEntityDispenser || tileEntity instanceof TileEntityDropper) {
            if (droppers.isEnabled()) {
                return HOPPER_COLOR;
            }
        }

        return null;
    }
}

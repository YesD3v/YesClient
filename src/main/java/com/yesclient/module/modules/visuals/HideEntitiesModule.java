package com.yesclient.module.modules.visuals;
import java.util.HashSet;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.utils.ItemFramePacketFilter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

 







public class HideEntitiesModule extends Module {

    private final HudScale hudScale = new HudScale(this);

    private static volatile boolean discardItemFrameContentsAtNetworkBoundary;

    private final BooleanSetting hideBoats = new BooleanSetting("Hide Boats", "Hide boats", true);
    private final BooleanSetting hideFallingSand = new BooleanSetting("Hide Falling Sand", "Hide falling sand", true);
    private final BooleanSetting hideFallingGravel = new BooleanSetting("Hide Falling Gravel", "Hide falling gravel",
            true);
    private final BooleanSetting hideFallingAnvil = new BooleanSetting("Hide Falling Anvil", "Hide falling anvils",
            true);
    private final BooleanSetting hideMinecarts = new BooleanSetting("Hide Minecarts", "Hide minecarts", false);
    private final BooleanSetting hideTNT = new BooleanSetting("Hide TNT", "Hide primed TNT", false);
    private final BooleanSetting hideFireworks = new BooleanSetting("Hide Fireworks", "Hide fireworks", false);
    private final BooleanSetting hideItemFrameItems = new BooleanSetting("Hide Item Frame Items",
            "Hide only the items displayed inside item frames", false) {
        @Override
        public void setValue(Boolean value) {
            super.setValue(value);
            syncNetworkFilterState();
        }
    };
    private final BooleanSetting hideItemFrames = new BooleanSetting("Hide Item Frames",
            "Hide item frames and the items displayed inside them", false) {
        @Override
        public void setValue(Boolean value) {
            super.setValue(value);
            syncNetworkFilterState();
        }
    };
    private final BooleanSetting hideDragonEggs = new BooleanSetting("Hide Dragon Eggs", "Hide dragon egg entities",
            false);
    private final BooleanSetting hideSigns = new BooleanSetting("Hide Signs",
            "Stop rendering all signs entirely (Better FPS)", false);
    private final BooleanSetting hideSignText = new BooleanSetting("Hide Sign Text", "Stop rendering text on signs",
            false);
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 0.0D, 20.0D, 10000.0D,
            0xFF55FF55, 0xFFFF5555);

     
    private volatile Set<Integer> hiddenEntityIds = Collections.emptySet();
    private int hiddenCount = 0;

    public HideEntitiesModule() {
        super("Hide Entities", "Hides lag-causing entities", Category.VISUALS, ModuleMode.BOTH);

        addSetting(hideBoats);
        addSetting(hideFallingSand);
        addSetting(hideFallingGravel);
        addSetting(hideFallingAnvil);
        addSetting(hideMinecarts);
        addSetting(hideTNT);
        addSetting(hideFireworks);
        addSetting(hideItemFrameItems);
        addSetting(hideItemFrames);
        addSetting(hideDragonEggs);
        addSetting(hideSigns);
        addSetting(hideSignText);
    }

    public boolean isHideSignsEnabled() {
        return hideSigns.isEnabled();
    }

    public boolean isHideSignTextEnabled() {
        return hideSignText.isEnabled();
    }

    public boolean isHideItemFrameItemsEnabled() {
        return hideItemFrameItems.isEnabled();
    }

    public boolean isHideItemFramesEnabled() {
        return hideItemFrames.isEnabled();
    }

     
    public boolean shouldHideItemFrameContents() {
        return hideItemFrameItems.isEnabled() || hideItemFrames.isEnabled();
    }

    public static boolean shouldDiscardItemFrameContentsAtNetworkBoundary() {
        return discardItemFrameContentsAtNetworkBoundary;
    }

    @Override
    protected void onEnable() {
        syncNetworkFilterState();
        hiddenCount = 0;
        hiddenEntityIds = Collections.emptySet();
    }

    @Override
    protected void onDisable() {
        discardItemFrameContentsAtNetworkBoundary = false;
        hiddenCount = 0;
        hiddenEntityIds = Collections.emptySet();
    }

    @Override
    public void onTick() {
        syncNetworkFilterState();
        if (mc.theWorld == null) {
            hiddenCount = 0;
            hiddenEntityIds = Collections.emptySet();
            return;
        }

         
         
        Set<Integer> newIds = new HashSet<>();
        List<Integer> itemFrameIdsToRemove = hideItemFrames.isEnabled()
                ? new ArrayList<Integer>() : Collections.<Integer>emptyList();
        try {
            List<Entity> entities = mc.theWorld.loadedEntityList;
            for (int i = 0, size = entities.size(); i < size; i++) {
                try {
                    Entity entity = entities.get(i);
                    if (entity instanceof EntityItemFrame) {
                        ItemFramePacketFilter.recordItemFrame(entity.getEntityId());
                        if (hideItemFrames.isEnabled()) {
                            itemFrameIdsToRemove.add(entity.getEntityId());
                            newIds.add(entity.getEntityId());
                            continue;
                        }
                        if (hideItemFrameItems.isEnabled()) {
                             
                             
                            entity.getDataWatcher().updateObject(8, null);
                        }
                    }
                    if (entity != null && shouldHideEntity(entity)) {
                        newIds.add(entity.getEntityId());
                    }
                } catch (IndexOutOfBoundsException ignored) {
                    break;  
                }
            }
        } catch (Exception ignored) {
             
        }

         
         
        for (Integer entityId : itemFrameIdsToRemove) {
            mc.theWorld.removeEntityFromWorld(entityId.intValue());
        }

         
        hiddenEntityIds = newIds;
        hiddenCount = newIds.size();
    }

    private void syncNetworkFilterState() {
        discardItemFrameContentsAtNetworkBoundary = isEnabled()
                && ((hideItemFrameItems != null && hideItemFrameItems.isEnabled())
                        || (hideItemFrames != null && hideItemFrames.isEnabled()));
    }

     



    public boolean isEntityHidden(int entityId) {
        return hiddenEntityIds.contains(entityId);
    }

     



    public boolean shouldHideEntity(Entity entity) {
        if (entity == null)
            return false;

        if (hideBoats.isEnabled() && entity instanceof EntityBoat) {
            return true;
        }

        if (entity instanceof EntityFallingBlock) {
            EntityFallingBlock fallingBlock = (EntityFallingBlock) entity;
            if (fallingBlock.getBlock() != null) {
                if (hideFallingSand.isEnabled() && fallingBlock.getBlock().getBlock() == Blocks.sand) {
                    return true;
                }
                if (hideFallingGravel.isEnabled() && fallingBlock.getBlock().getBlock() == Blocks.gravel) {
                    return true;
                }
                if (hideFallingAnvil.isEnabled() && fallingBlock.getBlock().getBlock() == Blocks.anvil) {
                    return true;
                }
            }
        }

        if (hideMinecarts.isEnabled() && entity instanceof EntityMinecart) {
            return true;
        }

        if (hideTNT.isEnabled() && entity instanceof EntityTNTPrimed) {
            return true;
        }

        if (hideFireworks.isEnabled() && entity instanceof EntityFireworkRocket) {
            return true;
        }

        if (hideItemFrames.isEnabled() && entity instanceof EntityItemFrame) {
            return true;
        }

         
        if (hideDragonEggs.isEnabled() && entity instanceof EntityFallingBlock) {
            EntityFallingBlock fb = (EntityFallingBlock) entity;
            if (fb.getBlock() != null && fb.getBlock().getBlock() == Blocks.dragon_egg) {
                return true;
            }
        }

        return false;
    }

    public int getHiddenCount() {
        return hiddenCount;
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;


        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getHiddenEntitiesX() : 5;
        int y = designer != null ? designer.getHiddenEntitiesY() : 408;

        hudScale.beginAt(x, y);
        textStyle.draw("Entities Hidden", String.valueOf(hiddenCount), "", hiddenCount, x, y);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
        return hiddenCount > 0 ? String.valueOf(hiddenCount) : null;
    }
}

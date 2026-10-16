package com.yesclient.module.modules.misc;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ItemSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.Set;

 






import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;

public class GhostBlocksModule extends Module {

    private final BooleanSetting placeBlocks = new BooleanSetting("Place Blocks", "Right-click to place ghost blocks",
            true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks",
            "Left-click to remove any block client-side", false);
    private final BooleanSetting restoreOnMiddleClick = new BooleanSetting("Middle Click Restore",
            "Middle-click to restore original block state", true);

     
    private final ItemSetting blockSetting = new ItemSetting("Ghost Block", "Block to place as ghost", true, false);

     
    private Block getSelectedBlock() {
        ItemStack stack = blockSetting.getValue();
        if (stack != null) {
            Block b = Block.getBlockFromItem(stack.getItem());
            if (b != null && b != Blocks.air)
                return b;
        }
        return Blocks.glass;
    }

    private int getSelectedMeta() {
        ItemStack stack = blockSetting.getValue();
        return stack != null ? stack.getMetadata() : 0;
    }

     
    private final Set<BlockPos> ghostBlocks = new HashSet<>();
     
    private final Map<BlockPos, net.minecraft.block.state.IBlockState> brokenBlocks = new HashMap<>();

    private boolean wasRightClicking = false;
    private boolean wasLeftClicking = false;



    public GhostBlocksModule() {
        super("Ghost Blocks", "Creates ghost blocks", Category.MISC, ModuleMode.BOTH);
        addSetting(placeBlocks);
        addSetting(breakBlocks);
        addSetting(restoreOnMiddleClick);
        addSetting(blockSetting);
    }

    @Override
    protected void onEnable() {
        ghostBlocks.clear();
        brokenBlocks.clear();

         
        if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
            mc.getNetHandler().getNetworkManager().channel().pipeline().addBefore("packet_handler",
                    "ghost_blocks_packet_listener", new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof S23PacketBlockChange) {
                                S23PacketBlockChange packet = (S23PacketBlockChange) msg;
                                if (ghostBlocks.contains(packet.getBlockPosition())) {
                                    return;  
                                }
                            } else if (msg instanceof S22PacketMultiBlockChange) {
                                S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) msg;
                                for (net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData data : packet
                                        .getChangedBlocks()) {
                                    if (ghostBlocks.contains(data.getPos())) {
                                        return;  
                                    }
                                }
                            }
                            super.channelRead(ctx, msg);
                        }
                    });
        }

         
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
         
        if (mc.theWorld != null) {
            for (BlockPos pos : ghostBlocks) {
                 
                 
                 
                if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                    mc.theWorld.setBlockToAir(pos);
                }
            }

             
            for (Map.Entry<BlockPos, net.minecraft.block.state.IBlockState> entry : brokenBlocks.entrySet()) {
                mc.theWorld.setBlockState(entry.getKey(), entry.getValue());
            }
        }
        ghostBlocks.clear();
        brokenBlocks.clear();

         
        try {
            if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                mc.getNetHandler().getNetworkManager().channel().pipeline().remove("ghost_blocks_packet_listener");
            }
        } catch (Exception e) {
        }

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public void onTick() {
        if (!isEnabled())
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

         
        if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null &&
                mc.getNetHandler().getNetworkManager().channel().pipeline()
                        .get("ghost_blocks_packet_listener") == null) {
            mc.getNetHandler().getNetworkManager().channel().pipeline().addBefore("packet_handler",
                    "ghost_blocks_packet_listener", new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof S23PacketBlockChange) {
                                S23PacketBlockChange packet = (S23PacketBlockChange) msg;
                                if (ghostBlocks.contains(packet.getBlockPosition())) {
                                    return;
                                }
                            } else if (msg instanceof S22PacketMultiBlockChange) {
                                S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) msg;
                                for (net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData data : packet
                                        .getChangedBlocks()) {
                                    if (ghostBlocks.contains(data.getPos())) {
                                        return;
                                    }
                                }
                            }
                            super.channelRead(ctx, msg);
                        }
                    });
        }

        Block targetBlock = getSelectedBlock();
        int targetMeta = getSelectedMeta();

         
         
        for (BlockPos pos : ghostBlocks) {
            net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
            if (state.getBlock() != targetBlock || targetBlock.getMetaFromState(state) != targetMeta) {
                mc.theWorld.setBlockState(pos, targetBlock.getStateFromMeta(targetMeta));
            }
        }

         
         
        for (BlockPos pos : brokenBlocks.keySet()) {
             
            if (ghostBlocks.contains(pos))
                continue;

            if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                mc.theWorld.setBlockToAir(pos);
            }
        }
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onMouse(net.minecraftforge.client.event.MouseEvent event) {
        if (!isEnabled() || mc.currentScreen != null)
            return;

         
        if (event.button == 2 && event.buttonstate) {
            if (restoreOnMiddleClick.isEnabled()) {
                handleMiddleClick();
            }
        }

         
        if (event.button == 1 && event.buttonstate) {
            if (placeBlocks.isEnabled()) {
                Block targetBlock = getSelectedBlock();

                boolean placed = placeGhostBlock(targetBlock, getSelectedMeta());
                if (placed) {
                    event.setCanceled(true);  
                }
            }
        }

         
        if (event.button == 0 && event.buttonstate) {
            handleLeftClick();
        }
    }

    private void handleMiddleClick() {
         
        MovingObjectPosition mop = mc.objectMouseOver;

         
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = mop.getBlockPos();
            if (tryRestoreBlock(hitPos))
                return;
        }

         
         
         
        double reach = 5.0;
        net.minecraft.util.Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        net.minecraft.util.Vec3 lookVec = mc.thePlayer.getLook(1.0f);
        net.minecraft.util.Vec3 endPos = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach,
                lookVec.zCoord * reach);

         
         
         

         
        net.minecraft.util.Vec3 cur = eyePos;
        net.minecraft.util.Vec3 step = lookVec.normalize();
        step = new net.minecraft.util.Vec3(step.xCoord * 0.1, step.yCoord * 0.1, step.zCoord * 0.1);

        for (int i = 0; i < reach * 10; i++) {
            cur = cur.add(step);
            BlockPos pos = new BlockPos(cur.xCoord, cur.yCoord, cur.zCoord);

             
             
            if (tryRestoreBlock(pos))
                return;
        }
    }

    private boolean tryRestoreBlock(BlockPos pos) {
         
        if (ghostBlocks.contains(pos)) {
            mc.theWorld.setBlockToAir(pos);
            ghostBlocks.remove(pos);
            return true;
        }

         
        if (brokenBlocks.containsKey(pos)) {
            net.minecraft.block.state.IBlockState original = brokenBlocks.get(pos);
            mc.theWorld.setBlockState(pos, original);
            brokenBlocks.remove(pos);
            return true;
        }
        return false;
    }

    private boolean placeGhostBlock(Block targetBlock, int meta) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null)
            return false;

        BlockPos placePos = null;

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = mop.getBlockPos();
            EnumFacing side = mop.sideHit;
            placePos = hitPos.offset(side);
        } else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
             
            double reach = 4.0;
            double x = mc.thePlayer.posX + mc.thePlayer.getLookVec().xCoord * reach;
            double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + mc.thePlayer.getLookVec().yCoord * reach;
            double z = mc.thePlayer.posZ + mc.thePlayer.getLookVec().zCoord * reach;
            placePos = new BlockPos(x, y, z);
        }

        if (placePos != null) {
            Block block = mc.theWorld.getBlockState(placePos).getBlock();
             
             
             
            if ((block == Blocks.air || brokenBlocks.containsKey(placePos)) && !ghostBlocks.contains(placePos)) {
                if (brokenBlocks.containsKey(placePos)) {
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                     
                }

                 
                mc.theWorld.setBlockState(placePos, targetBlock.getStateFromMeta(meta));
                ghostBlocks.add(placePos);
                return true;
            }
        }
        return false;
    }

    private void handleLeftClick() {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null)
            return;

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = mop.getBlockPos();

             
            if (ghostBlocks.contains(hitPos)) {
                mc.theWorld.setBlockToAir(hitPos);
                ghostBlocks.remove(hitPos);
                return;
            }

             
            if (breakBlocks.isEnabled()) {
                 
                if (!brokenBlocks.containsKey(hitPos)) {
                    brokenBlocks.put(hitPos, mc.theWorld.getBlockState(hitPos));
                }

                mc.theWorld.setBlockToAir(hitPos);
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        int count = ghostBlocks.size();
        return count > 0 ? String.valueOf(count) : null;
    }
}

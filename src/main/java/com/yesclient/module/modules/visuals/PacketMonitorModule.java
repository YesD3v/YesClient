package com.yesclient.module.modules.visuals;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.MapMaker;
import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.Packet;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Counts packets the client actually sends to the server, per client tick (PPT).
 *
 * Packets are counted by a handler at the end of the client connection's netty pipeline, so every outgoing packet is
 * seen no matter which code sent it (vanilla, Blink, Packet Multiplier writing straight to the channel, FML), and it
 * only counts once netty reports the write as successful. Packets that modules send on top of normal traffic (Blink
 * releasing its held packets, Packet Multiplier copies) are tagged with {@link #markExtra} and shown separately.
 */
public class PacketMonitorModule extends Module {

    /** A burst of extra packets ends (and stops showing) this long after its last packet. */
    private static final long EXTRA_BURST_HOLD_NANOS = 1_000_000_000L;
    private static final String HANDLER_NAME = "yesclient_packet_monitor";
    private static volatile PacketMonitorModule instance;

    /** Packets queued by modules as extra traffic; weak identity keys so dropped packets don't leak. */
    private static final Set<Packet<?>> EXTRA_PACKETS =
            Collections.newSetFromMap(new MapMaker().weakKeys().<Packet<?>, Boolean>makeMap());

    private final BooleanSetting showHud = new BooleanSetting("Show HUD", "Hide the counter while monitoring stays active", true);
    private final BooleanSetting autoDisconnect = new BooleanSetting("Auto Disconnect",
            "Disconnect if outgoing packets in one tick reach the limit", false);
    private final NumberSetting disconnectLimit = new NumberSetting("Packet Limit",
            "Outgoing packets per tick sent to the server, extra packets included; 0 disables the limit",
            50, 0, 500, 1);
    private final BooleanSetting panicDisconnect = new BooleanSetting("Panic Disconnect",
            "Close the network immediately instead of leaving cleanly", false);
    private final HudScale hudScale = new HudScale(this);
    // Normal play sends about 1-2 packets per tick: green at 1, red at 4, sliders up to 50.
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 1.0D, 4.0D, 50.0D,
            0xFF55FF55, 0xFFFF5555, true);

    private final AtomicBoolean disconnectQueued = new AtomicBoolean(false);
    /** Packets written during the tick in progress (normal and extra). */
    private int currentTickNormal;
    private int currentTickExtra;
    /** Normal packets written during the last finished tick; this is the displayed PPT. */
    private int lastTickNormal;
    /** Extra packets in the current burst; keeps adding up while extras keep arriving (e.g. Smart Release). */
    private long extraBurstTotal;
    private long lastExtraNanos;

    public PacketMonitorModule() {
        super("Packet Monitor", "Shows packets sent to the server per tick and can disconnect on an outgoing packet flood",
                Category.VISUALS, ModuleMode.BOTH);
        instance = this;
        addSetting(showHud);
        addSetting(autoDisconnect);
        addSetting(disconnectLimit);
        addSetting(panicDisconnect);
        disconnectLimit.setVisibility(() -> autoDisconnect.isEnabled());
        panicDisconnect.setVisibility(() -> autoDisconnect.isEnabled());
        resetCounters();
    }

    @Override
    protected void onEnable() {
        resetCounters();
    }

    @Override
    public void onTick() {
        if (disconnectQueued.get() && (mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null)) {
            resetCounters();
        }
    }

    /** Closes the previous tick's count at the start of each client tick. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            finishTick();
        }
    }

    synchronized void finishTick() {
        lastTickNormal = currentTickNormal;
        currentTickNormal = 0;
        currentTickExtra = 0;
    }

    /**
     * Marks a packet as extra traffic (sent on top of what the client would normally send right now). Call it before
     * the packet is handed to the network; it is counted as extra once it has actually been written to the server.
     */
    public static void markExtra(Packet<?> packet) {
        if (packet != null) {
            EXTRA_PACKETS.add(packet);
        }
    }

    /** Adds the counting handler to a client connection's pipeline. Called when the connection becomes active. */
    public static void attachCounter(ChannelPipeline pipeline) {
        if (pipeline != null && pipeline.get(HANDLER_NAME) == null) {
            pipeline.addLast(HANDLER_NAME, new OutgoingCounter());
        }
    }

    private static void onPacketWritten(boolean extra) {
        PacketMonitorModule monitor = instance;
        if (monitor != null && monitor.isEnabled() && monitor.recordPacket(extra)) {
            monitor.disconnectForPacketLimit();
        }
    }

    /** Sits at the tail of the pipeline, so every packet written to the channel passes it exactly once. */
    private static final class OutgoingCounter extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Packet) {
                final boolean extra = EXTRA_PACKETS.remove(msg);
                if (promise == ctx.voidPromise()) {
                    onPacketWritten(extra);
                } else {
                    promise.addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) {
                            if (future.isSuccess()) {
                                onPacketWritten(extra);
                            }
                        }
                    });
                }
            }
            ctx.write(msg, promise);
        }
    }

    private synchronized boolean recordPacket(boolean extra) {
        if (!isEnabled()) {
            return false;
        }

        if (extra) {
            currentTickExtra++;
            long now = System.nanoTime();
            if (!isBurstActive(now)) {
                extraBurstTotal = 0L;
            }
            extraBurstTotal++;
            lastExtraNanos = now;
        } else {
            currentTickNormal++;
        }
        int limit = disconnectLimit.getIntValue();
        return autoDisconnect.isEnabled() && limit > 0 && currentTickNormal + currentTickExtra >= limit;
    }

    private synchronized void resetCounters() {
        currentTickNormal = 0;
        currentTickExtra = 0;
        lastTickNormal = 0;
        extraBurstTotal = 0L;
        lastExtraNanos = 0L;
        disconnectQueued.set(false);
    }

    private boolean isBurstActive(long now) {
        return lastExtraNanos != 0L && now - lastExtraNanos <= EXTRA_BURST_HOLD_NANOS;
    }

    /** Normal packets sent to the server during the last finished tick. */
    public synchronized int getDisplayedRate() {
        return lastTickNormal;
    }

    /**
     * Extra packets (Blink releases, Packet Multiplier copies) sent to the server in the current burst. It keeps
     * adding up while extras keep arriving and is shown until a second after the last one, so paced releases climb
     * (+3, +6, +9...) instead of flickering.
     */
    public synchronized int getExtraBurst() {
        return isBurstActive(System.nanoTime()) ? (int) Math.min(Integer.MAX_VALUE, extraBurstTotal) : 0;
    }

    public boolean isHudVisible() {
        return showHud.isEnabled();
    }

    private static String extraSuffix(int extra) {
        return extra > 0 ? " (+" + extra + ")" : "";
    }

    @Override
    public void onRender() {
        if (!showHud.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer == null ? 5 : designer.getPacketMonitorX();
        int y = designer == null ? 448 : designer.getPacketMonitorY();
        int rate = getDisplayedRate();
        int extra = getExtraBurst();

        hudScale.beginAt(x, y);
        textStyle.draw("PPT", String.valueOf(rate), extraSuffix(extra), rate, x, y);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
        return getDisplayedRate() + extraSuffix(getExtraBurst());
    }

    private void disconnectForPacketLimit() {
        if (!disconnectQueued.compareAndSet(false, true)) {
            return;
        }

        Minecraft minecraft = mc;
        if (minecraft == null) {
            disconnectQueued.set(false);
            return;
        }

        final boolean panic = panicDisconnect.isEnabled();
        final int limit = disconnectLimit.getIntValue();
        try {
            minecraft.addScheduledTask(() -> {
                if (!isEnabled() || minecraft.theWorld == null || minecraft.getNetHandler() == null) {
                    disconnectQueued.set(false);
                    return;
                }

                ChatComponentText reason = new ChatComponentText(
                        "Packet Monitor reached the outgoing limit of " + limit + " packets per tick");
                if (panic) {
                    minecraft.getNetHandler().getNetworkManager().closeChannel(reason);
                } else {
                    minecraft.theWorld.sendQuittingDisconnectingPacket();
                    minecraft.loadWorld((WorldClient) null);
                    minecraft.displayGuiScreen(new GuiMainMenu());
                }
            });
        } catch (RuntimeException ignored) {
            disconnectQueued.set(false);
        }
    }
}

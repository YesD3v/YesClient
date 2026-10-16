package com.yesclient.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.network.Packet;

public class BungeePacketQueue {
     



    public static final Queue<Packet<?>> delayPacketQueue = new ConcurrentLinkedQueue<>();
}

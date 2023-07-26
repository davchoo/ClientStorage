package org.samo_lego.clientstorage.fabric_client.network;

import org.samo_lego.clientstorage.fabric_client.config.FabricConfig;

public class PacketLimiter {
    private static int packetCount = 0;
    private static long lastPacketTimestamp = System.currentTimeMillis();

    public static void needDelay() {
        if ((System.currentTimeMillis() - lastPacketTimestamp) >= FabricConfig.limiter.getDelay()) {
            packetCount = 0;
        } else if (packetCount++ >= FabricConfig.limiter.getThreshold()) {
            try {
                Thread.sleep(FabricConfig.limiter.getDelay());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            packetCount = 0;
        }
        lastPacketTimestamp = System.currentTimeMillis();
    }
}

package org.samo_lego.clientstorage.fabric_client.config;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.minecraft.network.FriendlyByteBuf;
import org.samo_lego.clientstorage.common.Config;
import org.samo_lego.clientstorage.fabric_client.network.PacketLimiter;

import java.util.Optional;

import static net.minecraft.server.network.ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE;

public class FabricConfig extends Config {

    public static PacketLimiter limiter = PacketLimiter.VANILLA;

    public double maxDist;

    public boolean informServerType;
    public boolean enabled;
    public boolean informSearch;

    public CustomLimiter customLimiter;

    public boolean enableCaching;

    private static Optional<Config> serverConfig = Optional.empty();

    public FabricConfig() {
        super(true);
        this.maxDist = Math.sqrt(MAX_INTERACTION_DISTANCE);

        this.informSearch = true;
        this.enabled = true;
        this.informServerType = true;
        this.enableCaching = true;
        this.customLimiter = new CustomLimiter();
    }


    @Override
    public void onLoad() {
        PacketLimiter.CUSTOM.setDelay(this.customLimiter.delay);
        PacketLimiter.CUSTOM.setThreshold(this.customLimiter.threshold);
    }

    @Override
    public void save() {
        this.customLimiter.delay = PacketLimiter.CUSTOM.getDelay();
        this.customLimiter.threshold = PacketLimiter.CUSTOM.getThreshold();

        super.save();
    }

    public void unpack(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        serverConfig = Optional.of(Config.GSON.fromJson(input.readUTF(), Config.class));
    }

    public boolean hasServerSettings() {
        return serverConfig.isPresent();
    }

    public void clearServerSettings() {
        serverConfig = Optional.empty();
    }

    @Override
    public boolean lookThroughBlocks() {
        return serverConfig.map(Config::lookThroughBlocks).orElseGet(super::lookThroughBlocks);
    }
}
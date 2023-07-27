package org.samo_lego.clientstorage.fabric_client.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.samo_lego.clientstorage.fabric_client.ClientStorageFabric;
import org.samo_lego.clientstorage.fabric_client.casts.ICSPlayer;
import org.samo_lego.clientstorage.fabric_client.casts.IRemoteStack;
import org.samo_lego.clientstorage.fabric_client.inventory.RemoteInventory;
import org.samo_lego.clientstorage.fabric_client.mixin.accessor.ACompoundContainer;
import org.samo_lego.clientstorage.fabric_client.mixin.accessor.AMultiPlayerGamemode;
import org.samo_lego.clientstorage.fabric_client.network.PacketLimiter;
import org.samo_lego.clientstorage.fabric_client.render.ESPRender;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainer;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainerBlock;
import org.samo_lego.clientstorage.fabric_client.util.ContainerUtil;
import org.samo_lego.clientstorage.fabric_client.util.PlayerLookUtil;
import org.samo_lego.clientstorage.fabric_client.util.StorageCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static org.samo_lego.clientstorage.fabric_client.ClientStorageFabric.config;

/**
 * The heart of the mod.
 */
public class ContainerDiscovery {

    private static final Queue<InteractableContainer> INTERACTION_Q = new ConcurrentLinkedQueue<>();
    private static final Queue<Consumer<AbstractContainerMenu>> PENDING_ACTIONS_Q = new ConcurrentLinkedQueue<>();

    private static InteractableContainer expectedInventory = null;
    public static BlockHitResult lastCraftingHit = null;
    public static int craftingPickupSlotId = -1;

    private static long lastInteractionTimestamp = 0;
    private static CompletableFuture<?> watchdog = CompletableFuture.completedFuture(null);

    public static boolean fakePacketsActive() {
        return !watchdog.isDone() && (!INTERACTION_Q.isEmpty() || expectedInventory != null);
    }

    public static void resetFakePackets() {
        INTERACTION_Q.clear();
        PENDING_ACTIONS_Q.clear();
        craftingPickupSlotId = -1;
        expectedInventory = null;
    }

    public static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (fakePacketsActive()) {
            return InteractionResult.FAIL;
        }
        ((ICSPlayer) player).cs_setLastInteractedContainer(null);

        if (world.isClientSide() && !player.isShiftKeyDown() && config.enabled) {
            BlockPos craftingPos = hitResult.getBlockPos();
            BlockState blockState = world.getBlockState(craftingPos);

            if (blockState.getBlock() == Blocks.CRAFTING_TABLE) {
                lastCraftingHit = hitResult;

                ContainerDiscovery.resetInventoryCache();

                if (config.enableBlocks) {
                    List<LevelChunk> chunks2check = ContainerDiscovery.getChunksAround(player.blockPosition(), world);
                    chunks2check.forEach(levelChunk -> {
                        // Check for blockentity containers
                        levelChunk.getBlockEntities().forEach((position, blockEntity) -> {
                            // Check if within reach
                            if (blockEntity instanceof InteractableContainer && player.getEyePosition().distanceTo(Vec3.atCenterOf(position)) < config.maxDist) {
                                // Check if container can be opened
                                // (avoid sending packets to those that client knows they can't be opened)
                                if (!ContainerUtil.shouldIncludeContainer(blockEntity, player)) {
                                    return;
                                }
                                scanContainer(ContainerUtil.getContainer(blockEntity));
                            }
                        });
                    });
                }

                // Check for other containers (e.g. chest minecarts, etc.)
                if (config.enableEntities) {
                    final var boundingBox = player.getBoundingBox().inflate(config.maxDist);
                    world.getEntities((Entity) null, boundingBox, InteractableContainer.CONTAINER_ENTITY_SELECTOR).forEach(entity -> scanContainer((InteractableContainer) entity));
                }

                if (!INTERACTION_Q.isEmpty()) {
                    CompletableFuture.runAsync(ContainerDiscovery::startSendPackets);
                    return InteractionResult.FAIL;  // We'll open the crafting table later
                }

                RemoteInventory.getInstance().sort();
            } else {
                ESPRender.removeBlockPos(craftingPos);

                final BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(craftingPos);
                assert !ContainerDiscovery.fakePacketsActive();
                if (blockEntity instanceof InteractableContainer) {
                    ((ICSPlayer) player).cs_setLastInteractedContainer(ContainerUtil.getContainer(blockEntity));
                } else {
                    ((ICSPlayer) player).cs_setLastInteractedContainer(null);
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static void scanContainer(InteractableContainer container) {
        final boolean singleplayer = Minecraft.getInstance().isLocalServer();
        if (singleplayer && container.isEmpty()) {
            ContainerDiscovery.copyServerContent(container);
        }

        if (!singleplayer && (container.isEmpty() || !config.enableCaching)) {
            addPendingInteraction(container, container::cs_storeContents);
        } else if (!container.isEmpty()) {
            for (int i = 0; i < container.getContainerSize(); ++i) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    ContainerDiscovery.addRemoteItem(container, i, stack);
                } else {
                    StorageCache.FREE_SPACE_CONTAINERS.compute(container, (key, value) -> value == null ? 1 : value + 1);
                }
            }
            StorageCache.CACHED_INVENTORIES.add(container);
        } else {
            StorageCache.FREE_SPACE_CONTAINERS.put(container, container.getContainerSize());
        }
    }

    /**
     * Copies the content of the server container to the client block entity container
     *
     * @param container block entity to copy to.
     */
    private static void copyServerContent(InteractableContainer container) {
        // Double chests need extra work
        if (container instanceof ACompoundContainer cnt) {
            copyServerContent((InteractableContainer) cnt.getContainer1());
            copyServerContent((InteractableContainer) cnt.getContainer2());
        }

        // We "cheat" here and copy the server side inventory to client if in singleplayer
        // Reason being that it's "cheaper" and also that
        // client was behaving differently than when playing on server
        ServerLevel level = Minecraft.getInstance()
                .getSingleplayerServer()
                .getLevel(Minecraft.getInstance().level.dimension());

        // Get server block entity

        final BlockPos pos = BlockPos.containing(container.cs_position());
        InteractableContainer serverContainer = (InteractableContainer) level.getChunkAt(pos).getBlockEntity(pos);
        if (serverContainer == null) {
            serverContainer = ContainerUtil.getContainer(level, pos);
        }

        if (serverContainer != null) {
            if (container.getContainerSize() != serverContainer.getContainerSize())
                ClientStorageFabric.tryLog(String.format("Server and client container sizes don't match! Client: %s, server: %s",
                        container.cs_info(),
                        serverContainer.cs_info()), ChatFormatting.RED);
            if (!serverContainer.isEmpty() && (serverContainer.cs_isEntity() || ((BaseContainerBlockEntity) serverContainer).canOpen(Minecraft.getInstance().player))) {
                ContainerUtil.copyContent(serverContainer, container, true);
            }
        }
    }

    private static void resetInventoryCache() {
        INTERACTION_Q.clear();
        PENDING_ACTIONS_Q.clear();
        craftingPickupSlotId = -1;
        expectedInventory = null;
        RemoteInventory.getInstance().reset();
        StorageCache.FREE_SPACE_CONTAINERS.clear();
    }

    /**
     * Gets the chunks around block cs_position.
     *
     * @param pos   block cs_position
     * @param world world
     * @return list of chunks
     */
    public static List<LevelChunk> getChunksAround(BlockPos pos, Level world) {
        final List<LevelChunk> chunks = new ArrayList<>();

        // Get chunks to check
        int radius = (int) config.maxDist;
        int minChunkX = (pos.getX() - radius) >> 4;
        int maxChunkX = (pos.getX() + radius) >> 4;
        int minChunkZ = (pos.getZ() - radius) >> 4;
        int maxChunkZ = (pos.getZ() + radius) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks.add(world.getChunk(x, z));
            }
        }
        return chunks;
    }

    public static void startSendPackets() {
        if (config.informSearch) {
            ClientStorageFabric.displayMessage("gameplay.clientstorage.performing_search");
        }
        ClientStorageFabric.tryLog("Starting to send following packets :: " + INTERACTION_Q, ChatFormatting.GREEN);
        sendNextPacket();
        if (watchdog == null || watchdog.isDone()) {
            watchdog = CompletableFuture.runAsync(ContainerDiscovery::startWatchdog);
        }
    }

    private static void startWatchdog() {
        while (!INTERACTION_Q.isEmpty() || expectedInventory != null) {
            long timeSinceLastPacket = System.currentTimeMillis() - lastInteractionTimestamp;
            if (timeSinceLastPacket > 1500) {
                ClientStorageFabric.tryLog("No response, skipping to next interaction", ChatFormatting.RED);
                sendNextPacket();
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send a packets from interaction queue.
     */
    public static void sendNextPacket() {
        lastInteractionTimestamp = System.currentTimeMillis();
        expectedInventory = null;

        var player = Minecraft.getInstance().player;
        if (player.containerMenu != player.inventoryMenu) {
            // Close whatever menu the player is in
            player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
            player.containerMenu = player.inventoryMenu;
        }

        while (!INTERACTION_Q.isEmpty()) {
            try {
                InteractableContainer container = INTERACTION_Q.poll();
                if (!config.lookThroughBlocks()) {
                    // Check if the container is behind a block
                    var hitResult = PlayerLookUtil.raycastTo(container.cs_position());
                    boolean behindBlock;
                    if (container instanceof InteractableContainerBlock blockContainer) {
                        behindBlock = !hitResult.getBlockPos().equals(blockContainer.cs_blockPos());
                        // TODO handle compound containers better
                    } else {
                        // TODO raycast for entities?
                        behindBlock = hitResult.getBlockPos().getCenter().distanceTo(container.cs_position()) > 1;
                    }
                    if (behindBlock) {
                        ClientStorageFabric.tryLog("Container is blocked :: " + container.cs_info(), ChatFormatting.DARK_RED);
                        continue;
                    }
                }

                ClientStorageFabric.tryLog("Sending packets :: " + container.cs_info(), ChatFormatting.AQUA);
                expectedInventory = container;
                container.cs_sendInteractionPacket();
                return;
            } catch (Exception e) {
                ClientStorageFabric.tryLog("Error while sending packets", ChatFormatting.RED);
                ClientStorageFabric.tryLog(e.getMessage(), ChatFormatting.RED);
                e.printStackTrace();
            }
        }
        ClientStorageFabric.tryLog("Finished sending packets", ChatFormatting.GREEN);
        reopenCraftingTable();
    }

    public static void reopenCraftingTable() {
        PacketLimiter.needDelay();
        Minecraft client = Minecraft.getInstance();
        var gm = (AMultiPlayerGamemode) client.gameMode;
        gm.cs_startPrediction(client.level, id ->
                new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, lastCraftingHit, id));
    }

    public static void addRemoteItem(InteractableContainer source, int slotId, ItemStack stack) {
        ClientStorageFabric.tryLog(String.format("Adding %s (origin: %s, slot #%d)", stack, source.cs_info(), slotId), ChatFormatting.DARK_GRAY);
        RemoteInventory.getInstance().addStack(IRemoteStack.fromStack(stack, source, slotId).copy());
    }

    public static void addPendingInteraction(InteractableContainer container, Consumer<AbstractContainerMenu> pendingAction) {
        INTERACTION_Q.add(container);
        PENDING_ACTIONS_Q.add(pendingAction);
    }

    public static void onInventoryInitialize(AbstractContainerMenu containerMenu) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (expectedInventory == null) {
            if (containerMenu.getType() == MenuType.CRAFTING) {
                if (craftingPickupSlotId != -1) {
                    mc.gameMode.handleInventoryMouseClick(containerMenu.containerId, craftingPickupSlotId, 0, ClickType.PICKUP, player);
                    craftingPickupSlotId = -1;
                }
                resetFakePackets();
                RemoteInventory.getInstance().sort();
            } else if (containerMenu != player.inventoryMenu && fakePacketsActive()) {
                ClientStorageFabric.tryLog("Received unexpected inventory initialize", ChatFormatting.RED);
            }
            return;
        }

        var stacks = containerMenu.getItems();
        if (expectedInventory.getContainerSize() + 36 != stacks.size()) {
            ClientStorageFabric.tryLog(String.format("Container size mismatch, expected %d [%s] but got %d.%n",
                            expectedInventory.getContainerSize(), expectedInventory.cs_info(), stacks.size() - 36),
                    ChatFormatting.RED);
            return;
        }

        ClientStorageFabric.tryLog(String.format("Received inventory contents for %s with: %s",
                expectedInventory.cs_info(),
                stacks.stream().filter(s -> !s.isEmpty()).toList()), ChatFormatting.YELLOW);

        Consumer<AbstractContainerMenu> pendingAction = PENDING_ACTIONS_Q.poll();
        if (pendingAction != null) {
            pendingAction.accept(containerMenu);
        }
        expectedInventory = null;
        sendNextPacket();
    }
}

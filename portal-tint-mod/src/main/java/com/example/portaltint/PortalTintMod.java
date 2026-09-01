package com.example.portaltint;

import com.example.portaltint.network.PortalTintFullSyncPayload;
import com.example.portaltint.network.PortalTintUpdatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.item.DyeItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PortalTintMod implements ModInitializer {

	public static final String MOD_ID = "portaltint";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Nivel de permiso requerido (2 = OP estándar de un servidor vanilla). */
	private static final int REQUIRED_PERMISSION_LEVEL = 2;

	@Override
	public void onInitialize() {
		// Registro de los tipos de paquete (deben coincidir en cliente y servidor)
		PayloadTypeRegistry.playS2C().register(PortalTintUpdatePayload.ID, PortalTintUpdatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PortalTintFullSyncPayload.ID, PortalTintFullSyncPayload.CODEC);

		// Enviar el estado completo del portal al jugador cuando se conecta
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			sendFullSync(player);
		});

		// Evento principal: clic derecho sobre un bloque con un tinte en la mano
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) {
				return ActionResult.PASS;
			}
			if (hand != Hand.MAIN_HAND) {
				return ActionResult.PASS;
			}

			BlockPos clickedPos = hitResult.getBlockPos();
			if (world.getBlockState(clickedPos).getBlock() != Blocks.NETHER_PORTAL) {
				return ActionResult.PASS;
			}

			if (!(player.getStackInHand(hand).getItem() instanceof DyeItem dyeItem)) {
				return ActionResult.PASS;
			}

			if (!player.hasPermissionLevel(REQUIRED_PERMISSION_LEVEL)) {
				player.sendMessage(Text.translatable("portaltint.no_permission"), true);
				return ActionResult.FAIL;
			}

			int color = dyeItem.getColor().getEntityColor();
			List<BlockPos> portalBlocks = findConnectedPortalBlocks(world, clickedPos);

			PortalTintState state = PortalTintState.get(world);
			for (BlockPos pos : portalBlocks) {
				state.setColor(pos, color);
			}

			broadcastUpdate((ServerWorld) world, portalBlocks, color);

			if (!player.getAbilities().creativeMode) {
				player.getStackInHand(hand).decrement(1);
			}

			return ActionResult.SUCCESS;
		});
	}

	/**
	 * BFS por todos los bloques NETHER_PORTAL conectados ortogonalmente a partir de
	 * la posición inicial. Solo lee el mundo, no modifica nada.
	 */
	private static List<BlockPos> findConnectedPortalBlocks(net.minecraft.world.World world, BlockPos start) {
		Set<BlockPos> visited = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		visited.add(start);

		while (!queue.isEmpty()) {
			BlockPos current = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.offset(direction);
				if (!visited.contains(neighbor) && world.getBlockState(neighbor).getBlock() == Blocks.NETHER_PORTAL) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}

		return new ArrayList<>(visited);
	}

	private static void broadcastUpdate(ServerWorld world, List<BlockPos> positions, int color) {
		PortalTintUpdatePayload payload = new PortalTintUpdatePayload(positions, color);
		for (ServerPlayerEntity player : world.getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void sendFullSync(ServerPlayerEntity player) {
		PortalTintState state = PortalTintState.get(player.getWorld());
		List<BlockPos> positions = new ArrayList<>(state.getColors().keySet());
		List<Integer> colors = new ArrayList<>();
		for (BlockPos pos : positions) {
			colors.add(state.getColor(pos));
		}
		ServerPlayNetworking.send(player, new PortalTintFullSyncPayload(positions, colors));
	}
}

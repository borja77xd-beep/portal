package com.example.portaltint;

import com.example.portaltint.network.PortalTintFullSyncPayload;
import com.example.portaltint.network.PortalTintUpdatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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

	/** Distancia máxima de alcance para detectar el portal, en bloques. */
	private static final double MAX_REACH = 6.0;

	/** Paso del rastreo manual de rayo, en bloques (más pequeño = más preciso). */
	private static final double STEP = 0.1;

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

		// Evento principal: usar (clic derecho) un tinte, sin depender de a qué
		// bloque "apunta" el motor, ya que nether_portal no tiene forma de colisión
		// y por tanto nunca sería seleccionado como bloque clickeado por el juego.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClient) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}
			if (hand != Hand.MAIN_HAND) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}

			if (!(player.getStackInHand(hand).getItem() instanceof DyeItem dyeItem)) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}

			BlockPos portalPos = findLookedAtPortal(world, player);
			if (portalPos == null) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}

			if (!player.hasPermissionLevel(REQUIRED_PERMISSION_LEVEL)) {
				player.sendMessage(Text.translatable("portaltint.no_permission"), true);
				return TypedActionResult.fail(player.getStackInHand(hand));
			}

			int color = dyeItem.getColor().getEntityColor();
			List<BlockPos> portalBlocks = findConnectedPortalBlocks(world, portalPos);

			PortalTintState state = PortalTintState.get(world);
			for (BlockPos pos : portalBlocks) {
				state.setColor(pos, color);
			}

			broadcastUpdate((ServerWorld) world, portalBlocks, color);

			if (!player.getAbilities().creativeMode) {
				player.getStackInHand(hand).decrement(1);
			}

			return TypedActionResult.success(player.getStackInHand(hand));
		});
	}

	/**
	 * Recorre manualmente la línea de mirada del jugador, paso a paso, buscando
	 * el primer bloque NETHER_PORTAL. Necesario porque ese bloque tiene una forma
	 * de colisión/contorno vacía y el juego no lo selecciona con el raycast normal.
	 */
	private static BlockPos findLookedAtPortal(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player) {
		Vec3d start = player.getCameraPosVec(1.0f);
		Vec3d direction = player.getRotationVec(1.0f);

		BlockPos lastPos = null;
		double distance = 0;
		while (distance <= MAX_REACH) {
			Vec3d point = start.add(direction.multiply(distance));
			BlockPos pos = BlockPos.ofFloored(point.x, point.y, point.z);

			if (!pos.equals(lastPos)) {
				if (world.getBlockState(pos).getBlock() == Blocks.NETHER_PORTAL) {
					return pos;
				}
				lastPos = pos;
			}

			distance += STEP;
		}
		return null;
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

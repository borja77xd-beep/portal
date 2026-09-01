package com.example.portaltint.client;

import com.example.portaltint.network.PortalTintFullSyncPayload;
import com.example.portaltint.network.PortalTintUpdatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.render.block.BlockRenderView;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Solo existe en el cliente. Guarda el color conocido de cada portal y le dice
 * al renderer qué color usar para el tintado (tintindex 0) del bloque.
 * No contiene ninguna lógica de comportamiento: es exclusivamente visual.
 */
public class PortalTintModClient implements ClientModInitializer {

	/** Cache local, solo para renderizado. Se repuebla en cada full sync. */
	private static final Map<BlockPos, Integer> COLOR_CACHE = new ConcurrentHashMap<>();

	private static final int DEFAULT_COLOR = 0xFFFFFF; // sin tinte = color original

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(PortalTintUpdatePayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				for (BlockPos pos : payload.positions()) {
					COLOR_CACHE.put(pos, payload.color());
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(PortalTintFullSyncPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				COLOR_CACHE.clear();
				for (int i = 0; i < payload.positions().size(); i++) {
					COLOR_CACHE.put(payload.positions().get(i), payload.colors().get(i));
				}
			});
		});

		BlockColorProvider colorProvider = (state, world, pos, tintIndex) -> {
			if (pos == null) {
				return DEFAULT_COLOR;
			}
			return COLOR_CACHE.getOrDefault(pos, DEFAULT_COLOR);
		};

		net.fabricmc.fabric.api.client.render.color.block.v1.BlockColorProviderRegistry
				.registerBlockColorProvider(colorProvider, Blocks.NETHER_PORTAL);
	}
}

package com.example.portaltint.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Enviado al jugador cuando se conecta o cambia de dimensión: le manda todo el
 * estado de tintado conocido para esa dimensión de una sola vez.
 */
public record PortalTintFullSyncPayload(List<BlockPos> positions, List<Integer> colors) implements CustomPayload {

	public static final CustomPayload.Id<PortalTintFullSyncPayload> ID =
			new CustomPayload.Id<>(Identifier.of("portaltint", "full_sync"));

	public static final PacketCodec<RegistryByteBuf, PortalTintFullSyncPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), PortalTintFullSyncPayload::positions,
			PacketCodecs.INTEGER.collect(PacketCodecs.toList()), PortalTintFullSyncPayload::colors,
			PortalTintFullSyncPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

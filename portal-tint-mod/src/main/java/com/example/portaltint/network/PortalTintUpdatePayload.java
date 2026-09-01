package com.example.portaltint.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Se envía del servidor al cliente cuando un portal (o parte de uno) cambia de color.
 * Contiene únicamente posiciones y un color: no transporta ninguna lógica de juego.
 */
public record PortalTintUpdatePayload(List<BlockPos> positions, int color) implements CustomPayload {

	public static final CustomPayload.Id<PortalTintUpdatePayload> ID =
			new CustomPayload.Id<>(Identifier.of("portaltint", "update"));

	public static final PacketCodec<RegistryByteBuf, PortalTintUpdatePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), PortalTintUpdatePayload::positions,
			PacketCodecs.INTEGER, PortalTintUpdatePayload::color,
			PortalTintUpdatePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

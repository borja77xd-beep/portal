package com.example.portaltint;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Guarda, para un mundo concreto, qué posiciones de portal tienen qué color.
 * Es puramente datos: no contiene ninguna lógica de comportamiento del juego.
 */
public class PortalTintState extends PersistentState {

	private static final String ID = "portaltint_colors";

	private final Map<BlockPos, Integer> colors = new HashMap<>();

	public static PortalTintState get(World world) {
		if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
			throw new IllegalStateException("PortalTintState solo existe en el servidor");
		}
		PersistentStateManager manager = serverWorld.getPersistentStateManager();
		return manager.getOrCreate(new Type<>(PortalTintState::new, PortalTintState::createFromNbt, null), ID);
	}

	public Map<BlockPos, Integer> getColors() {
		return colors;
	}

	public void setColor(BlockPos pos, int color) {
		colors.put(pos.toImmutable(), color);
		markDirty();
	}

	public Integer getColor(BlockPos pos) {
		return colors.get(pos);
	}

	private static PortalTintState createFromNbt(NbtCompound nbt, net.minecraft.registry.wrapper.RegistryWrapper.WrapperLookup lookup) {
		PortalTintState state = new PortalTintState();
		NbtList list = nbt.getList("portals", NbtCompound.COMPOUND_TYPE.get());
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i).get();
			BlockPos pos = new BlockPos(entry.getInt("x").orElse(0), entry.getInt("y").orElse(0), entry.getInt("z").orElse(0));
			int color = entry.getInt("color").orElse(0xFFFFFF);
			state.colors.put(pos, color);
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.wrapper.RegistryWrapper.WrapperLookup lookup) {
		NbtList list = new NbtList();
		for (Map.Entry<BlockPos, Integer> entry : colors.entrySet()) {
			NbtCompound entryNbt = new NbtCompound();
			entryNbt.putInt("x", entry.getKey().getX());
			entryNbt.putInt("y", entry.getKey().getY());
			entryNbt.putInt("z", entry.getKey().getZ());
			entryNbt.putInt("color", entry.getValue());
			list.add(entryNbt);
		}
		nbt.put("portals", list);
		return nbt;
	}
}

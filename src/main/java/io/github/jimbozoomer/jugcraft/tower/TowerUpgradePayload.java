package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Sent by the tower status screen's buttons. The server checks that the player is near the core (or at a console of its
 * tower) and may run it, then either pays the next step's blocks and starts building it
 * ({@link TowerCoreBlockEntity#tryUpgrade}) or, for LOAD FROM INVENTORY ({@code load}), puts in everything in the
 * player's inventory the tower still needs ({@link TowerCoreBlockEntity#depositFrom}).
 */
public record TowerUpgradePayload(BlockPos pos, BlockPos accessPos, boolean load) implements CustomPacketPayload {
	public TowerUpgradePayload(BlockPos pos) { this(pos, pos, false); }
	public TowerUpgradePayload(BlockPos pos, BlockPos accessPos) { this(pos, accessPos, false); }
	public static final Type<TowerUpgradePayload> TYPE = new Type<>(Jugcraft.id("drone_tower_upgrade"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TowerUpgradePayload> CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, TowerUpgradePayload::pos,
					BlockPos.STREAM_CODEC, TowerUpgradePayload::accessPos,
					net.minecraft.network.codec.ByteBufCodecs.BOOL, TowerUpgradePayload::load, TowerUpgradePayload::new);

	@Override
	public Type<TowerUpgradePayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> handle(context.player(), payload));
	}

	public static boolean mayAccessTower(ServerPlayer player, TowerCoreBlockEntity core, BlockPos accessPos) {
		if (core.getLevel() != player.level() || core.isRemoved() || !player.level().isLoaded(core.getBlockPos())
				|| player.level().getBlockEntity(core.getBlockPos()) != core) return false;
		if (player.distanceToSqr(Vec3.atCenterOf(core.getBlockPos())) <= TowerCoreBlockEntity.REACH_SQR) return true;
		return ConsoleDeskBlock.mayAccessTerminal(player, core.terminalPos(), accessPos)
				&& core.terminal() != null && core.getBlockPos().equals(core.terminal().towerPos());
	}

	public static void handle(ServerPlayer player, TowerUpgradePayload payload) {
		if (player.level().isLoaded(payload.pos()) && player.level().getBlockEntity(payload.pos()) instanceof TowerCoreBlockEntity core
				&& mayAccessTower(player, core, payload.accessPos())) {
			if (payload.load()) {
				TowerCoreBlock.deposit(core, player, () -> core.depositFrom(player));
			} else {
				core.tryUpgrade(player);
			}
		}
	}
}

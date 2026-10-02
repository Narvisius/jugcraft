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
 * Sent by the tower status screen's UPGRADE DRONE TOWER button. The server checks that the player is near the
 * core and may run it, then pays the modules and starts the next tier ({@link TowerCoreBlockEntity#tryUpgrade}).
 */
public record TowerUpgradePayload(BlockPos pos) implements CustomPacketPayload {
	public static final Type<TowerUpgradePayload> TYPE = new Type<>(Jugcraft.id("drone_tower_upgrade"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TowerUpgradePayload> CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, TowerUpgradePayload::pos, TowerUpgradePayload::new);

	@Override
	public Type<TowerUpgradePayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos pos = payload.pos();
			if (player.level().isLoaded(pos) && player.distanceToSqr(Vec3.atCenterOf(pos)) <= TowerCoreBlockEntity.REACH_SQR
					&& player.level().getBlockEntity(pos) instanceof TowerCoreBlockEntity core) {
				core.tryUpgrade(player);
			}
		});
	}
}

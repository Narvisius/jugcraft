package io.github.jimbozoomer.jugcraft.prospecting;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Geo-Resonance Prospector: right-click to survey the 3x3 chunks around you. The server runs the
 * survey ({@link OreSurvey}) and sends the vague readings to the client, which shows them on the
 * prospector's screen. A short cooldown keeps players from surveying every tick.
 */
public class ProspectorItem extends Item {
	public static final int COOLDOWN_TICKS = 60;

	public ProspectorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, new SurveyPayload(OreSurvey.survey(server, player.blockPosition(), level.getRandom())));
			player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}
}

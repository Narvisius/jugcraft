package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * What a Harvest Feast Table gives its diners. A feast's score is how many different foods are on the table plus how
 * many players have eaten at it in the last two minutes. Each serving eaten gives every recent diner within
 * {@value #REACH} blocks the feast's tier (the eater included):
 * <ul>
 * <li>{@value #GOOD_MEAL} or more, a good meal: Regeneration I for {@value #REGENERATION_TICKS} ticks;</li>
 * <li>{@value #FEAST} or more, a feast: and Absorption I for {@value #ABSORPTION_TICKS};</li>
 * <li>{@value #HARVEST_FEAST} or more, a harvest feast: and Haste I and Luck for {@value #LONG_TICKS};</li>
 * <li>{@value #GRAND_FEAST} or more, a grand feast: and Health Boost I for {@value #LONG_TICKS}, earning Harvest Home.</li>
 * </ul>
 * A solo player with a varied table reaches a harvest feast; a grand feast needs friends (or a very varied table).
 */
public final class Feasts {
	public static final int GOOD_MEAL = 3;
	public static final int FEAST = 5;
	public static final int HARVEST_FEAST = 8;
	public static final int GRAND_FEAST = 11;
	public static final int REGENERATION_TICKS = 200;
	public static final int ABSORPTION_TICKS = 2400;
	public static final int LONG_TICKS = 6000;
	public static final int REACH = 16;

	private Feasts() {
	}

	/** A feast's tier for {@code score}: 0 (an ordinary meal) to 4 (a grand feast). */
	public static int tier(int score) {
		return score >= GRAND_FEAST ? 4 : score >= HARVEST_FEAST ? 3 : score >= FEAST ? 2 : score >= GOOD_MEAL ? 1 : 0;
	}

	/** Gives {@code player} the effects of {@code tier}. */
	public static void bless(ServerPlayer player, int tier) {
		if (tier >= 1) {
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS));
		}
		if (tier >= 2) {
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS));
		}
		if (tier >= 3) {
			player.addEffect(new MobEffectInstance(MobEffects.HASTE, LONG_TICKS));
			player.addEffect(new MobEffectInstance(MobEffects.LUCK, LONG_TICKS));
		}
		if (tier >= 4) {
			player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_TICKS));
			TrickOrTreat.award(player, "harvest_home");
		}
	}

	/**
	 * {@code eater} has just eaten at the table at {@code pos}: works out the feast and blesses every recent diner within
	 * reach; returns the score.
	 */
	public static int ate(ServerLevel level, BlockPos pos, ServerPlayer eater) {
		List<FeastTableBlockEntity> table = FeastTableBlockEntity.table(level, pos);
		if (table.isEmpty()) {
			return 0;
		}
		long now = level.getGameTime();
		table.get(0).ate(eater.getUUID(), now);
		Set<UUID> diners = FeastTableBlockEntity.diners(table, now);
		int variety = FeastTableBlockEntity.variety(table);
		int score = variety + diners.size();
		int tier = tier(score);
		for (UUID id : diners) {
			Player diner = level.getPlayerByUUID(id);
			if (diner instanceof ServerPlayer player && player.isAlive() && player.blockPosition().closerThan(pos, REACH)) {
				bless(player, tier);
			}
		}
		eater.sendOverlayMessage(Component.translatable("message.jugcraft.feast_table.tier." + tier, variety, diners.size()));
		return score;
	}
}

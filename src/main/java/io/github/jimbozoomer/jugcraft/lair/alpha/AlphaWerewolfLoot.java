package io.github.jimbozoomer.jugcraft.lair.alpha;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The Alpha Werewolf's loot, per participant (everyone who hurt him or his pack, or rang the bell that flushed him, and is
 * still near his ring when he falls): each rolls {@code jugcraft:entities/alpha_werewolf} for themselves (the Alpha Pelt,
 * a chance of one of his trophies from {@code jugcraft:bosses/werewolf_alpha}, the Packfinder Charm and the Alpha's
 * Hood), a player's first kill always brings a trophy (whoever has not yet earned Hunter's Moon), the
 * {@value AlphaWerewolfEntity#EXPERIENCE} experience is shared out, and everyone earns Hunter's Moon. He is no Witching
 * Season boss: no seasonal extras. It goes straight into their inventory, the rest at their feet, so nobody else can pick
 * it up.
 */
public final class AlphaWerewolfLoot {
	public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/alpha_werewolf"));
	public static final ResourceKey<LootTable> TROPHIES = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("bosses/werewolf_alpha"));
	public static final String ADVANCEMENT = "hunters_moon";

	private AlphaWerewolfLoot() {
	}

	/** Gives each participant their loot; returns what each got. */
	public static Map<ServerPlayer, List<ItemStack>> reward(ServerLevel level, AlphaWerewolfEntity alpha, List<ServerPlayer> participants) {
		Map<ServerPlayer, List<ItemStack>> given = new LinkedHashMap<>();
		if (participants.isEmpty()) {
			return given;
		}
		int share = Math.max(1, AlphaWerewolfEntity.EXPERIENCE / participants.size());
		for (ServerPlayer player : participants) {
			List<ItemStack> loot = roll(level, alpha, player);
			for (ItemStack stack : loot) {
				player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
			}
			player.giveExperiencePoints(share);
			TrickOrTreat.award(player, ADVANCEMENT);
			player.sendSystemMessage(Component.translatable("message.jugcraft.alpha_werewolf.loot"));
			given.put(player, loot);
		}
		return given;
	}

	/** One participant's roll. */
	public static List<ItemStack> roll(ServerLevel level, AlphaWerewolfEntity alpha, ServerPlayer player) {
		List<ItemStack> loot = new ArrayList<>(table(level, TABLE, alpha, player));
		if (firstKill(player) && loot.stream().noneMatch(AlphaWerewolfLoot::trophy)) {
			loot.addAll(table(level, TROPHIES, alpha, player));
		}
		return loot;
	}

	/** Whether {@code stack} is one of his trophies, Moonfang or the Howler. */
	public static boolean trophy(ItemStack stack) {
		return stack.is(JugcraftAlpha.moonfang()) || stack.is(JugcraftAlpha.howler());
	}

	private static List<ItemStack> table(ServerLevel level, ResourceKey<LootTable> key, AlphaWerewolfEntity alpha, ServerPlayer player) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, alpha.position())
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		return List.copyOf(table.getRandomItems(params));
	}

	/** Whether this would be {@code player}'s first kill: they have not yet earned Hunter's Moon. */
	public static boolean firstKill(ServerPlayer player) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(ADVANCEMENT));
		return advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone();
	}
}

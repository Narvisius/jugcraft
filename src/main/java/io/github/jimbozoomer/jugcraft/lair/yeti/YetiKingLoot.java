package io.github.jimbozoomer.jugcraft.lair.yeti;

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
 * The Yeti King's loot, per participant (everyone who hurt him or his whelps and is still near his lake when he falls):
 * each rolls {@code jugcraft:entities/yeti_king} for themselves (Yeti Fur, a chance of one of his trophies from
 * {@code jugcraft:bosses/yeti_king}, the Yeti Mitten and the Yeti King's Crown), a player's first kill always brings a trophy
 * (whoever has not yet earned Abominable), the {@value YetiKingEntity#EXPERIENCE} experience is shared out, and everyone
 * earns Abominable. He is no Witching Season boss: no seasonal extras. It goes straight into their inventory, the rest at
 * their feet, so nobody else can pick it up.
 */
public final class YetiKingLoot {
	public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/yeti_king"));
	public static final ResourceKey<LootTable> TROPHIES = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("bosses/yeti_king"));
	public static final String ADVANCEMENT = "abominable";

	private YetiKingLoot() {
	}

	/** Gives each participant their loot; returns what each got. */
	public static Map<ServerPlayer, List<ItemStack>> reward(ServerLevel level, YetiKingEntity king, List<ServerPlayer> participants) {
		Map<ServerPlayer, List<ItemStack>> given = new LinkedHashMap<>();
		if (participants.isEmpty()) {
			return given;
		}
		int share = Math.max(1, YetiKingEntity.EXPERIENCE / participants.size());
		for (ServerPlayer player : participants) {
			List<ItemStack> loot = roll(level, king, player);
			for (ItemStack stack : loot) {
				player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
			}
			player.giveExperiencePoints(share);
			TrickOrTreat.award(player, ADVANCEMENT);
			player.sendSystemMessage(Component.translatable("message.jugcraft.yeti_king.loot"));
			given.put(player, loot);
		}
		return given;
	}

	/** One participant's roll. */
	public static List<ItemStack> roll(ServerLevel level, YetiKingEntity king, ServerPlayer player) {
		List<ItemStack> loot = new ArrayList<>(table(level, TABLE, king, player));
		if (firstKill(player) && loot.stream().noneMatch(YetiKingLoot::trophy)) {
			loot.addAll(table(level, TROPHIES, king, player));
		}
		return loot;
	}

	/** Whether {@code stack} is one of his trophies, the Glacier Maul or the Rimeclaw. */
	public static boolean trophy(ItemStack stack) {
		return stack.is(JugcraftYeti.glacierMaul()) || stack.is(JugcraftYeti.rimeclaw());
	}

	private static List<ItemStack> table(ServerLevel level, ResourceKey<LootTable> key, YetiKingEntity king, ServerPlayer player) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, king.position())
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		return List.copyOf(table.getRandomItems(params));
	}

	/** Whether this would be {@code player}'s first kill: they have not yet earned Abominable. */
	public static boolean firstKill(ServerPlayer player) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(ADVANCEMENT));
		return advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone();
	}
}

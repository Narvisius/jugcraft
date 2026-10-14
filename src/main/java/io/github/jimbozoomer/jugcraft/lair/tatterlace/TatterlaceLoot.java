package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CostumedMobs;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineLoot;
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
import net.minecraft.world.phys.Vec3;

/**
 * Madame Tatterlace's loot, for each player paid for her fall (lair/LairRewards, which calls {@link #pay}): everyone
 * who qualified in her fight (lair/Encounter: hurt her or her brood, or whose tamed creature did, or healed, warded or
 * cut another player free of her Binding Thread enough) and is alive within 64 blocks of her doily when she falls, and
 * anyone who died in the fight or left the game during it and is still away, paid once they are back. Each rolls
 * {@code jugcraft:entities/tatterlace} for themselves when paid (Gossamer Silk, and a chance of the Needle Rapier, the
 * Golden Thimble and Tatterlace's Headdress), a player's first kill always brings the Needle Rapier (whoever has not
 * yet earned Unravelled), the {@value TatterlaceEntity#EXPERIENCE} experience is shared out among everyone paid, now or
 * later, and everyone earns Unravelled. During the Halloween event (unless {@code lairs.event_loot} is off, as for every
 * lair boss) each also gets a roll of seasonal candy and a {@value #EVENT_HEADDRESS_CHANCE} chance more of the
 * headdress (judged when the share is paid). It goes straight into their inventory, the rest at their feet, so nobody
 * else can pick it up.
 */
public final class TatterlaceLoot {
	public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/tatterlace"));
	/** The name her rewards are kept under when they wait for a player (lair/LairRewards). */
	public static final String BOSS = "tatterlace";
	public static final String ADVANCEMENT = "unravelled";
	public static final float EVENT_HEADDRESS_CHANCE = 0.1F;

	private TatterlaceLoot() {
	}

	/** Gives each participant their loot; returns what each got. */
	public static Map<ServerPlayer, List<ItemStack>> reward(ServerLevel level, TatterlaceEntity boss, List<ServerPlayer> participants) {
		Map<ServerPlayer, List<ItemStack>> given = new LinkedHashMap<>();
		if (participants.isEmpty()) {
			return given;
		}
		int share = Math.max(1, TatterlaceEntity.EXPERIENCE / participants.size());
		for (ServerPlayer player : participants) {
			given.put(player, pay(level, boss.position(), player, share));
		}
		return given;
	}

	/**
	 * Pays one player their loot from a fall at {@code origin}: rolled now, into their inventory (the rest at their feet),
	 * their {@code experience} share and the advancement. Rewards paid later (lair/LairRewards) come this way too.
	 */
	public static List<ItemStack> pay(ServerLevel level, Vec3 origin, ServerPlayer player, int experience) {
		List<ItemStack> loot = roll(level, origin, player);
		for (ItemStack stack : loot) {
			player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
		}
		player.giveExperiencePoints(experience);
		TrickOrTreat.award(player, ADVANCEMENT);
		player.sendSystemMessage(Component.translatable("message.jugcraft.tatterlace.loot"));
		return loot;
	}

	/** One participant's roll. */
	public static List<ItemStack> roll(ServerLevel level, TatterlaceEntity boss, ServerPlayer player) {
		return roll(level, boss.position(), player);
	}

	/** One participant's roll, for a fall at {@code origin}. */
	public static List<ItemStack> roll(ServerLevel level, Vec3 origin, ServerPlayer player) {
		List<ItemStack> loot = new ArrayList<>(table(level, TABLE, origin, player));
		if (firstKill(player) && loot.stream().noneMatch(stack -> stack.is(JugcraftTatterlace.needleRapier()))) {
			loot.add(new ItemStack(JugcraftTatterlace.needleRapier()));
		}
		if (VesperineLoot.eventBonus()) {
			loot.addAll(table(level, CostumedMobs.CANDY, origin, player));
			if (level.getRandom().nextFloat() < EVENT_HEADDRESS_CHANCE
					&& loot.stream().noneMatch(stack -> stack.is(JugcraftTatterlace.TATTERLACE_HEADDRESS))) {
				loot.add(new ItemStack(JugcraftTatterlace.TATTERLACE_HEADDRESS));
			}
		}
		return loot;
	}

	private static List<ItemStack> table(ServerLevel level, ResourceKey<LootTable> key, Vec3 origin, ServerPlayer player) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		return List.copyOf(table.getRandomItems(params));
	}

	/** Whether this would be {@code player}'s first kill: they have not yet earned Unravelled. */
	public static boolean firstKill(ServerPlayer player) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(ADVANCEMENT));
		return advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone();
	}
}

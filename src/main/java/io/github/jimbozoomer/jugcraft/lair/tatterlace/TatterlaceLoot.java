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

/**
 * Madame Tatterlace's loot, per participant (everyone who hurt her or her brood and is still near her doily when she
 * falls): each rolls {@code jugcraft:entities/tatterlace} for themselves (Gossamer Silk, and a chance of the Needle
 * Rapier, the Golden Thimble and Tatterlace's Headdress), a player's first kill always brings the Needle Rapier (whoever
 * has not yet earned Unravelled), the {@value TatterlaceEntity#EXPERIENCE} experience is shared out, and everyone earns
 * Unravelled. During the Halloween event (unless {@code lairs.event_loot} is off, as for every lair boss) each also gets a
 * roll of seasonal candy and a {@value #EVENT_HEADDRESS_CHANCE} chance more of the headdress. It goes straight into their
 * inventory, the rest at their feet, so nobody else can pick it up.
 */
public final class TatterlaceLoot {
	public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/tatterlace"));
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
			List<ItemStack> loot = roll(level, boss, player);
			for (ItemStack stack : loot) {
				player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
			}
			player.giveExperiencePoints(share);
			TrickOrTreat.award(player, ADVANCEMENT);
			player.sendSystemMessage(Component.translatable("message.jugcraft.tatterlace.loot"));
			given.put(player, loot);
		}
		return given;
	}

	/** One participant's roll. */
	public static List<ItemStack> roll(ServerLevel level, TatterlaceEntity boss, ServerPlayer player) {
		List<ItemStack> loot = new ArrayList<>(table(level, TABLE, boss, player));
		if (firstKill(player) && loot.stream().noneMatch(stack -> stack.is(JugcraftTatterlace.needleRapier()))) {
			loot.add(new ItemStack(JugcraftTatterlace.needleRapier()));
		}
		if (VesperineLoot.eventBonus()) {
			loot.addAll(table(level, CostumedMobs.CANDY, boss, player));
			if (level.getRandom().nextFloat() < EVENT_HEADDRESS_CHANCE
					&& loot.stream().noneMatch(stack -> stack.is(JugcraftTatterlace.TATTERLACE_HEADDRESS))) {
				loot.add(new ItemStack(JugcraftTatterlace.TATTERLACE_HEADDRESS));
			}
		}
		return loot;
	}

	private static List<ItemStack> table(ServerLevel level, ResourceKey<LootTable> key, TatterlaceEntity boss, ServerPlayer player) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, boss.position())
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		return List.copyOf(table.getRandomItems(params));
	}

	/** Whether this would be {@code player}'s first kill: they have not yet earned Unravelled. */
	public static boolean firstKill(ServerPlayer player) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(ADVANCEMENT));
		return advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone();
	}
}

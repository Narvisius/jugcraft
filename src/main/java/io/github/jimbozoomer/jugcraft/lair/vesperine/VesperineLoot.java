package io.github.jimbozoomer.jugcraft.lair.vesperine;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CostumedMobs;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
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
 * Vesperine's loot, per participant (everyone who hurt her or her skulls and is still in her arena when she falls):
 * each rolls {@code jugcraft:entities/vesperine} for themselves (Reaper's Shade, and a chance of the Vesper Scythe, her
 * two skull trophies and the Reaper's Hood), a player's first kill always brings the Vesper Scythe (whoever has not yet
 * earned The Last Harvest), the {@value VesperineEntity#EXPERIENCE} experience is shared out, and everyone earns The
 * Last Harvest. During the Halloween event (unless {@code lairs.event_loot} is off) each also gets a roll of seasonal
 * candy and a {@value #EVENT_HOOD_CHANCE} chance more of the hood. It goes straight into their inventory, the rest at
 * their feet, so nobody else can pick it up.
 */
public final class VesperineLoot {
	public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE,
			Jugcraft.id("entities/vesperine"));
	public static final String ADVANCEMENT = "the_last_harvest";
	public static final float EVENT_HOOD_CHANCE = 0.1F;

	private VesperineLoot() {
	}

	/** Gives each participant their loot; returns what each got. */
	public static Map<ServerPlayer, List<ItemStack>> reward(ServerLevel level, VesperineEntity boss, List<ServerPlayer> participants) {
		Map<ServerPlayer, List<ItemStack>> given = new LinkedHashMap<>();
		if (participants.isEmpty()) {
			return given;
		}
		int share = Math.max(1, VesperineEntity.EXPERIENCE / participants.size());
		for (ServerPlayer player : participants) {
			List<ItemStack> loot = roll(level, boss, player);
			for (ItemStack stack : loot) {
				player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
			}
			player.giveExperiencePoints(share);
			TrickOrTreat.award(player, ADVANCEMENT);
			player.sendSystemMessage(Component.translatable("message.jugcraft.vesperine.loot"));
			given.put(player, loot);
		}
		return given;
	}

	/** One participant's roll. */
	public static List<ItemStack> roll(ServerLevel level, VesperineEntity boss, ServerPlayer player) {
		List<ItemStack> loot = new ArrayList<>(table(level, TABLE, boss, player));
		if (firstKill(player) && loot.stream().noneMatch(stack -> stack.is(JugcraftVesperine.vesperScythe()))) {
			loot.add(new ItemStack(JugcraftVesperine.vesperScythe()));
		}
		if (eventBonus()) {
			loot.addAll(table(level, CostumedMobs.CANDY, boss, player));
			if (level.getRandom().nextFloat() < EVENT_HOOD_CHANCE && loot.stream().noneMatch(stack -> stack.is(JugcraftVesperine.REAPER_HOOD))) {
				loot.add(new ItemStack(JugcraftVesperine.REAPER_HOOD));
			}
		}
		return loot;
	}

	private static List<ItemStack> table(ServerLevel level, ResourceKey<LootTable> key, VesperineEntity boss, ServerPlayer player) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, boss.position())
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		return List.copyOf(table.getRandomItems(params));
	}

	/** Whether this would be {@code player}'s first kill: they have not yet earned The Last Harvest. */
	public static boolean firstKill(ServerPlayer player) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(ADVANCEMENT));
		return advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** The Halloween event's bonus: while the event runs, unless {@code lairs.event_loot} is off. */
	public static boolean eventBonus() {
		return HalloweenSeason.active() && !"off".equalsIgnoreCase(JugcraftConfig.textOption("lairs.event_loot").trim());
	}
}

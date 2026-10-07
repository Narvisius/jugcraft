package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Costumed mobs: while the Halloween event runs ({@link HalloweenSeason}), each zombie, husk, skeleton, stray
 * and zombie villager rolls once, the first time it enters the world, and {@link #CHANCE} of them dress up in a
 * costume hat, an outfit (a vampire cape, mummy wraps, a skeleton suit, a werewolf mask, cat ears and tail or bat wings)
 * or a carved pumpkin (never over a helmet they already wear). The costume drops like any mob equipment, now and then; killed by a player (and with mob loot on), a costumed mob also drops one roll of
 * candy (loot table {@code jugcraft:entities/costumed_mob_candy}, rolled as a gift to the player). When the event ends no new
 * mob dresses up; those already dressed keep their costume. With agriculture switched off, no mob dresses up
 * and none drops candy.
 */
public final class CostumedMobs {
	public static final float CHANCE = 0.15F;
	/** Entity tags: rolled once (dressed or not), and dressed (what the candy table checks). */
	public static final String ROLLED = "jugcraft.costume_rolled";
	public static final String COSTUMED = "jugcraft.costumed";
	public static final List<String> MOBS = List.of("zombie", "husk", "skeleton", "stray", "zombie_villager");
	public static final List<String> COSTUMES = List.of("jugcraft:witch_hat", "jugcraft:ghost_sheet", "jugcraft:scarecrow_hat",
			"minecraft:carved_pumpkin", "jugcraft:vampire_cape", "jugcraft:mummy_wraps", "jugcraft:skeleton_suit", "jugcraft:werewolf_mask",
			"jugcraft:cat_ears_and_tail", "jugcraft:bat_wings");
	public static final ResourceKey<LootTable> CANDY = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/costumed_mob_candy"));

	private CostumedMobs() {
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && wears(mob)) {
				dress(mob, level.getRandom());
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.entityTags().contains(COSTUMED) && source.getEntity() instanceof ServerPlayer killer
					&& entity.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.MOB_DROPS)
					&& JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
				dropCandy(level, entity, killer);
			}
		});
	}

	/** Whether this kind of mob dresses up for Halloween. */
	public static boolean wears(Mob mob) {
		Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
		return type.getNamespace().equals("minecraft") && MOBS.contains(type.getPath());
	}

	/**
	 * Rolls a mob's costume once, while the event runs: returns whether it dressed up. A mob that already
	 * rolled, or one out of season, is left alone (out of season it may still roll in a later event).
	 */
	public static boolean dress(Mob mob, RandomSource random) {
		if (!HalloweenSeason.active() || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE) || mob.entityTags().contains(ROLLED)) {
			return false;
		}
		mob.addTag(ROLLED);
		if (random.nextFloat() >= CHANCE || !mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			return false;
		}
		Identifier costume = Identifier.parse(COSTUMES.get(random.nextInt(COSTUMES.size())));
		mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.getValue(costume)));
		mob.addTag(COSTUMED);
		return true;
	}

	/** Rolls the candy table once for a costumed mob a player killed and drops it where the mob fell. */
	public static List<ItemStack> dropCandy(ServerLevel level, LivingEntity mob, ServerPlayer killer) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(CANDY);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, mob.position())
				.withParameter(LootContextParams.THIS_ENTITY, killer).create(LootContextParamSets.GIFT);
		List<ItemStack> candy = List.copyOf(table.getRandomItems(params));
		candy.forEach(stack -> mob.spawnAtLocation(level, stack.copy()));
		return candy;
	}

	/** Puts a costume on a mob whatever the season or the dice (tests). */
	public static void dressNow(Mob mob, String costume) {
		mob.addTag(ROLLED);
		mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(costume))));
		mob.addTag(COSTUMED);
	}
}

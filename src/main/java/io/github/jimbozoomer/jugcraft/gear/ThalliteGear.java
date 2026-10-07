package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Thallite's two traits (docs/features/thallite.md). Neither adds damage, defense or speed.
 * <ul>
 * <li><b>Regrowth</b> (every item in {@code #jugcraft:thallite_gear}): every {@link #REGROWTH_SECONDS} seconds, each
 * such item worn or held gets back one use while its holder stands on living soil ({@code #jugcraft:living_ground}),
 * up to {@link #REGROWTH_CAP_PERCENT}% of full. With {@link #EARTHBOUND_FOR_STONE} or more Earthbound pieces worn, it
 * works on any natural ground ({@code #jugcraft:earthen_ground}). It adds uses, never metal.</li>
 * <li><b>Rooted</b> (Earthbound armor, {@code #jugcraft:earthbound_armor}): each piece worn adds {@link #ROOTED_PER_PIECE}
 * knockback resistance while its wearer stands on natural ground, as a temporary attribute modifier refreshed every
 * {@link #ROOTED_TICKS} ticks. In the air or water, nothing.</li>
 * </ul>
 * Everything runs on the server, for players only: one footing check per player twice a second and at most six slots
 * every five seconds. Keep the numbers in sync with tools/gear.py; tools/check_mod_data.py checks them.
 */
public final class ThalliteGear {
	/** Gear with Regrowth (data/jugcraft/tags/item/thallite_gear.json). */
	public static final TagKey<Item> GEAR = TagKey.create(Registries.ITEM, Jugcraft.id("thallite_gear"));
	/** Armor with Rooted (data/jugcraft/tags/item/earthbound_armor.json). */
	public static final TagKey<Item> EARTHBOUND = TagKey.create(Registries.ITEM, Jugcraft.id("earthbound_armor"));
	/** Living soil, where Regrowth works (data/jugcraft/tags/block/living_ground.json). */
	public static final TagKey<Block> LIVING_GROUND = TagKey.create(Registries.BLOCK, Jugcraft.id("living_ground"));
	/** Natural ground, where Rooted holds (data/jugcraft/tags/block/earthen_ground.json). */
	public static final TagKey<Block> EARTHEN_GROUND = TagKey.create(Registries.BLOCK, Jugcraft.id("earthen_ground"));
	public static final int REGROWTH_SECONDS = 5;
	public static final int REGROWTH_CAP_PERCENT = 75;
	public static final int EARTHBOUND_FOR_STONE = 2;
	public static final double ROOTED_PER_PIECE = 0.075;
	public static final int ROOTED_TICKS = 10;
	/** Rooted's knockback resistance modifier. */
	public static final Identifier ROOTED = Jugcraft.id("rooted");
	private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET);
	/** Worn or held: both hands and the four armor slots. */
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
			EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private ThalliteGear() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ThalliteGear::tick);
	}

	private static void tick(MinecraftServer server) {
		int now = server.getTickCount();
		boolean root = now % ROOTED_TICKS == 0;
		boolean regrow = now % (REGROWTH_SECONDS * 20) == 0;
		if (!root && !regrow) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			BlockState ground = footing(player);
			if (root) {
				root(player, ground);
			}
			if (regrow && regrow(player, ground) > 0) {
				ServerLevel level = player.level();
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.8, player.getZ(), 2, 0.3, 0.4,
						0.3, 0.0);
			}
		}
	}

	/** The block an entity stands on, or null in the air or in water. */
	public static BlockState footing(LivingEntity entity) {
		if (!entity.onGround() || entity.isInWater()) {
			return null;
		}
		return entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 0.2, entity.getZ()));
	}

	/** How many Earthbound pieces an entity wears. */
	public static int earthbound(LivingEntity entity) {
		int pieces = 0;
		for (EquipmentSlot slot : ARMOR) {
			if (entity.getItemBySlot(slot).is(EARTHBOUND)) {
				pieces++;
			}
		}
		return pieces;
	}

	/** Whether Regrowth works for this entity on this ground (null: in the air or water). */
	public static boolean regrows(LivingEntity entity, BlockState ground) {
		if (ground == null) {
			return false;
		}
		return ground.is(LIVING_GROUND) || (ground.is(EARTHEN_GROUND) && earthbound(entity) >= EARTHBOUND_FOR_STONE);
	}

	/** One round of Regrowth: each thallite item worn or held gets back a use, if the ground allows. Returns how many did. */
	public static int regrow(LivingEntity entity, BlockState ground) {
		if (!regrows(entity, ground)) {
			return 0;
		}
		int mended = 0;
		for (EquipmentSlot slot : SLOTS) {
			if (mend(entity.getItemBySlot(slot))) {
				mended++;
			}
		}
		return mended;
	}

	/** Gives back one use to a damaged piece of thallite gear below the cap; returns whether it did. */
	public static boolean mend(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(GEAR) || !stack.isDamageableItem()) {
			return false;
		}
		if (stack.getDamageValue() <= capDamage(stack.getMaxDamage())) {
			return false;
		}
		stack.setDamageValue(stack.getDamageValue() - 1);
		return true;
	}

	/** The wear Regrowth stops at: a piece with this much damage has REGROWTH_CAP_PERCENT of its uses. */
	public static int capDamage(int maxDamage) {
		return maxDamage - maxDamage * REGROWTH_CAP_PERCENT / 100;
	}

	/** Rooted's knockback resistance for this entity on this ground (null: in the air or water). */
	public static double rooted(LivingEntity entity, BlockState ground) {
		if (ground == null || !ground.is(EARTHEN_GROUND)) {
			return 0.0;
		}
		return earthbound(entity) * ROOTED_PER_PIECE;
	}

	/** Sets Rooted's modifier to what this entity's Earthbound pieces and ground give, or removes it. */
	public static void root(LivingEntity entity, BlockState ground) {
		AttributeInstance resistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (resistance == null) {
			return;
		}
		double amount = rooted(entity, ground);
		resistance.removeModifier(ROOTED);
		if (amount > 0) {
			resistance.addTransientModifier(new AttributeModifier(ROOTED, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}

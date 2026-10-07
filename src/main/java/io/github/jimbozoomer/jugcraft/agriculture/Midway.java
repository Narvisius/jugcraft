package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The fall fair midway (fall addition 26): striking the High Striker ({@link HighStrikerBlock}) and the prizes its bell
 * and Ring Toss ({@link RingTossBlock}) give.
 *
 * <p>A player strikes the High Striker by hitting its base (left click) with a Carnival Mallet. The server checks they
 * may use blocks there and are within {@value #REACH} blocks, and that the puck is at rest. The blow's strength is the
 * swing's charge ({@link Player#getAttackStrengthScale}) times a roll between {@value #FULL_LOW} and {@value #FULL_HIGH},
 * plus {@value #CRIT_BONUS} for a critical swing (falling, as a critical hit is): {@value #RING_AT} or more rings the
 * bell; less climbs that share of the way. The swing's charge is spent, as an attack spends it.
 *
 * <p>A prize is one roll of the loot table {@code jugcraft:gameplay/midway_prize}, given to the winner (dropped at their
 * feet if their inventory is full). It earns Step Right Up, and the Jumbo Pumpkin Plush Jackpot.
 */
public final class Midway {
	public static final float FULL_LOW = 0.7F;
	public static final float FULL_HIGH = 1.0F;
	public static final float CRIT_BONUS = 0.15F;
	public static final float RING_AT = 0.95F;
	public static final double REACH = 6.0;
	public static final String MALLET = "carnival_mallet";
	public static final String JACKPOT = "jumbo_pumpkin_plush";
	public static final ResourceKey<LootTable> PRIZES = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("gameplay/midway_prize"));
	/** The plush prizes and their footprints facing north, in pixels (tools/midway.py PLUSHES). */
	public static final List<Plush> PLUSHES = List.of(
			new Plush("pumpkin_plush", 3, 3, 13, 13, 8),
			new Plush("ghost_plush", 3, 4, 13, 12, 10),
			new Plush("bat_plush", 1, 5, 15, 11, 10),
			new Plush("black_cat_plush", 4, 3, 12, 12, 12),
			new Plush("squirrel_plush", 5, 4, 11, 13, 12),
			new Plush("werewolf_plush", 4, 3, 12, 11, 13),
			new Plush("jumbo_pumpkin_plush", 1, 1, 15, 15, 14),
			// The harvest plushes (Halloween decorations batch 16).
			new Plush("owl_plush", 4, 5, 12, 12, 14),
			new Plush("hedgehog_plush", 4, 2, 12, 13, 7),
			new Plush("acorn_plush", 4, 4, 12, 12, 13),
			new Plush("corn_plush", 4, 4, 12, 12, 13),
			new Plush("maple_leaf_plush", 1, 5, 15, 11, 15));

	/** A plush: its ID and its footprint (x0, z0 to x1, z1, and height) facing north, in pixels. */
	public record Plush(String id, int x0, int z0, int x1, int z1, int height) {
	}

	private Midway() {
	}

	static void register() {
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> attack(player, level, hand, pos));
	}

	public static Item mallet() {
		return JugcraftAgriculture.item(MALLET);
	}

	/**
	 * A left click on a block: on a High Striker's base with a Carnival Mallet in hand, a strike (on the server; on the
	 * client it only stops the click from starting to break the base). Anything else passes.
	 */
	public static InteractionResult attack(Player player, Level level, InteractionHand hand, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof HighStrikerBlock) || state.getValue(HighStrikerBlock.PART) != 0
				|| !player.getItemInHand(hand).is(mallet()) || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server && player instanceof ServerPlayer striker) {
			strike(server, pos, striker);
		}
		player.resetAttackStrengthTicker();
		return InteractionResult.SUCCESS;
	}

	/** {@code player} swings at the High Striker whose base is at {@code base}; returns the level the puck goes to (0 for none). */
	public static int strike(ServerLevel level, BlockPos base, ServerPlayer player) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE) || !level.mayInteract(player, base)
				|| player.distanceToSqr(Vec3.atCenterOf(base)) > REACH * REACH) {
			return 0;
		}
		float charge = player.getAttackStrengthScale(0.5F);
		boolean critical = charge > 0.9F && player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater()
				&& !player.isPassenger();
		return strike(level, base, player, levelFor(charge, critical, player.getRandom().nextFloat()));
	}

	/** Sends the puck at {@code base} up to {@code puck} for {@code player}; returns the level, or 0 if it was busy. */
	public static int strike(ServerLevel level, BlockPos base, @Nullable ServerPlayer player, int puck) {
		if (!(level.getBlockEntity(base) instanceof HighStrikerBlockEntity striker) || !striker.launch(level, puck, player)) {
			return 0;
		}
		level.playSound(null, base, sound("block.anvil.land", SoundEvents.WOOD_HIT), SoundSource.BLOCKS, 0.5F, 1.6F);
		level.sendParticles(ParticleTypes.CRIT, base.getX() + 0.5, base.getY() + 0.3, base.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.2);
		return puck;
	}

	/**
	 * How high a blow sends the puck: a swing charged {@code charge} (0 to 1), critical or not, with a {@code roll}
	 * (0 to 1). {@value #RING_AT} of full strength rings the bell ({@link HighStrikerBlock#RUNG}); less climbs that share
	 * of the lamps, at least one.
	 */
	public static int levelFor(float charge, boolean critical, float roll) {
		float strength = charge * Mth.lerp(roll, FULL_LOW, FULL_HIGH) + (critical ? CRIT_BONUS : 0.0F);
		if (strength >= RING_AT) {
			return HighStrikerBlock.RUNG;
		}
		return Math.clamp((int) (strength / RING_AT * HighStrikerBlock.RUNG), 1, HighStrikerBlock.LEVELS);
	}

	/** A lamp lights as the puck passes, a note higher each one. */
	public static void climb(ServerLevel level, BlockPos base, int puck) {
		float pitch = 0.6F + 1.4F * puck / HighStrikerBlock.RUNG;
		level.playSound(null, base.above(Math.min(HighStrikerBlock.PARTS - 1, (puck + 1) / 2)), sound("block.note_block.bit", SoundEvents.WOOL_HIT),
				SoundSource.BLOCKS, 0.6F, pitch);
	}

	/** The puck hits the bell: it rings, sparks fly, and the striker (if still about) wins a prize and Ring the Bell. */
	public static void ring(ServerLevel level, BlockPos base, @Nullable ServerPlayer striker) {
		BlockPos bell = base.above(HighStrikerBlock.PARTS - 1);
		level.playSound(null, bell, sound("block.bell.use", SoundEvents.BELL_BLOCK), SoundSource.BLOCKS, 2.0F, 1.0F);
		level.sendParticles(ParticleTypes.FIREWORK, bell.getX() + 0.5, bell.getY() + 1.0, bell.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.08);
		if (striker != null) {
			TrickOrTreat.award(striker, "ring_the_bell");
			prize(level, striker, Vec3.atCenterOf(bell));
		}
	}

	/** Rolls the prize table for {@code winner} and gives them what comes; returns it. */
	public static List<ItemStack> prize(ServerLevel level, ServerPlayer winner, Vec3 at) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(PRIZES);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at)
				.withParameter(LootContextParams.THIS_ENTITY, winner).create(LootContextParamSets.GIFT);
		List<ItemStack> prizes = List.copyOf(table.getRandomItems(params));
		for (ItemStack prize : prizes) {
			ItemStack given = prize.copy();
			if (!winner.getInventory().add(given)) {
				winner.spawnAtLocation(level, given);
			}
			if (prize.is(JugcraftAgriculture.item(JACKPOT))) {
				TrickOrTreat.award(winner, "jackpot");
			}
		}
		if (!prizes.isEmpty()) {
			TrickOrTreat.award(winner, "step_right_up");
			level.playSound(null, winner.getX(), winner.getY(), winner.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
		}
		return prizes;
	}

	/** The vanilla sound {@code id}, or {@code fallback} if there is none by that name. */
	static SoundEvent sound(String id, SoundEvent fallback) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(id)).orElse(fallback);
	}
}

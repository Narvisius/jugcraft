package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Harvest Crown's crowning, which opens the Crowfield (docs/features/crowfield.md): the crown used on a scarecrow
 * (the lower half's {@code scarecrow}) in the Overworld at night, the scarecrow wearing a head (any it can wear), its head
 * under the open sky, among the harvest: at least {@value #HARVEST_MIN} blocks of {@code #jugcraft:crowfield_harvest}
 * (pumpkins, carved or lit or not, the heirlooms, the squashes and gourds, giant pumpkins) within {@value #HARVEST_REACH}
 * blocks of it across and {@value #HARVEST_RISE} up or down. The crown settles on the scarecrow's head and the crows
 * burst up crying; the crown is used up, the scarecrow and the crops are left as they were, Grey Mist (a
 * {@link MistGateEntity}) rises where the crowner stands, and they come to in the farmyard. For
 * {@code lairs.gate_seconds} the mist is the gate: anyone who uses it follows, while there is room. Nobody is taken in by
 * standing near it.
 *
 * <p>When a step is missing (outside the Overworld, by day, off-season, the scarecrow bare-headed or under a roof, too
 * little harvest round it, a mist already risen within {@value #MIST_RANGE} blocks) the crown only says which and
 * nothing is used up; when every instance of the field is taken, the same. The Pumpkin King is a Witching Season boss:
 * any night of the year, unless the server keeps the rituals to the Halloween event ({@code lairs.off_season=off}).
 */
public final class HarvestCrownRite {
	public static final int COOLDOWN = 40;
	/** How near another open mist into the Crowfield must not be (blocks). */
	public static final int MIST_RANGE = 3;
	/** The harvest round the scarecrow: how far across, how far up or down, and how much (tools/lairs.py CROWN). */
	public static final int HARVEST_REACH = 6;
	public static final int HARVEST_RISE = 2;
	public static final int HARVEST_MIN = 8;
	public static final TagKey<Block> HARVEST = TagKey.create(Registries.BLOCK, Jugcraft.id("crowfield_harvest"));

	private HarvestCrownRite() {
	}

	/** What the crowning found missing here, in the order it is checked, or null when it is answered. */
	public enum Missing {
		NOT_SCARECROW, DISABLED, NOT_OVERWORLD, NOT_NIGHT, OFF_SEASON, BARE, ROOFED, NO_HARVEST, MISTING, FULL
	}

	/** The result of a crowning: what was missing (or null), and the instance opened. */
	public record Crowning(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** Uses {@code crown} on the scarecrow whose lower half is at {@code scarecrow}. */
	public static Crowning crown(ServerLevel level, ServerPlayer player, BlockPos scarecrow, ItemStack crown) {
		return crown(level, player, scarecrow, crown, !level.isBrightOutside(), Lairs.offSeason() || HalloweenSeason.active());
	}

	/**
	 * Uses {@code crown} on the scarecrow whose lower half is at {@code scarecrow}, {@code night} and whether the ritual is
	 * in season ({@code inSeason}: the server allows it all year, or the Halloween event is on) as given (game tests set
	 * them). Opens an instance of the field and takes the player in, or says what is missing and uses nothing.
	 */
	public static Crowning crown(ServerLevel level, ServerPlayer player, BlockPos scarecrow, ItemStack crown, boolean night,
			boolean inSeason) {
		BlockPos feet = HowlingTotemRite.feet(player);
		Missing missing = check(level, scarecrow, feet, night, inSeason);
		if (missing != null) {
			player.sendOverlayMessage(message(missing, harvest(level, scarecrow)));
			level.playSound(null, scarecrow.above(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.6F, 0.5F);
			return new Crowning(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.CROWFIELD);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL, 0));
			return new Crowning(Missing.FULL, null);
		}
		crown.consume(1, player);
		// The crown settles on the scarecrow's head and the crows burst up out of the field, crying.
		BlockPos head = scarecrow.above();
		level.playSound(null, head, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
		for (int i = 0; i < 3; i++) {
			level.playSound(null, head, SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.45F + i * 0.05F);
		}
		level.playSound(null, head, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.0F, 1.6F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, head.getX() + 0.5, head.getY() + 1.0, head.getZ() + 0.5, 30, 1.5, 0.8, 1.5, 0.08);
		level.sendParticles(ParticleTypes.WAX_OFF, head.getX() + 0.5, head.getY() + 0.9, head.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), feet.getY() + 0.2, player.getZ(), 30, 0.9, 0.2, 0.9, 0.01);
		MistGateEntity.open(level, feet.below(), instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.crown.opened"));
		follow(player, instance);
		return new Crowning(null, instance);
	}

	/**
	 * Checks a crowning of the scarecrow whose lower half is at {@code scarecrow}, by someone with their feet in
	 * {@code feet}, without changing anything.
	 */
	public static @Nullable Missing check(ServerLevel level, BlockPos scarecrow, BlockPos feet, boolean night, boolean inSeason) {
		BlockState state = level.getBlockState(scarecrow);
		if (!(state.getBlock() instanceof ScarecrowBlock)) {
			return Missing.NOT_SCARECROW;
		}
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!night) {
			return Missing.NOT_NIGHT;
		}
		if (!inSeason) {
			return Missing.OFF_SEASON;
		}
		if (ScarecrowBlock.head(level, scarecrow, state).isEmpty()) {
			return Missing.BARE;
		}
		if (HowlingTotemRite.roofed(level, scarecrow.above())) {
			return Missing.ROOFED;
		}
		if (harvest(level, scarecrow) < HARVEST_MIN) {
			return Missing.NO_HARVEST;
		}
		if (mist(level, feet) != null) {
			return Missing.MISTING;
		}
		return null;
	}

	/** How many blocks of the harvest lie round the scarecrow whose lower half is at {@code scarecrow}. */
	public static int harvest(Level level, BlockPos scarecrow) {
		int found = 0;
		for (BlockPos pos : BlockPos.betweenClosed(scarecrow.offset(-HARVEST_REACH, -HARVEST_RISE, -HARVEST_REACH),
				scarecrow.offset(HARVEST_REACH, HARVEST_RISE, HARVEST_REACH))) {
			if (level.getBlockState(pos).is(HARVEST)) {
				found++;
			}
		}
		return found;
	}

	public static Component message(Missing missing, int harvest) {
		return switch (missing) {
			case NOT_SCARECROW -> Component.translatable("message.jugcraft.lair.crown.not_scarecrow");
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.crown.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.crown.not_night");
			case OFF_SEASON -> Component.translatable("message.jugcraft.lair.crown.off_season");
			case BARE -> Component.translatable("message.jugcraft.lair.crown.bare");
			case ROOFED -> Component.translatable("message.jugcraft.lair.crown.roofed");
			case NO_HARVEST -> Component.translatable("message.jugcraft.lair.crown.no_harvest", harvest, HARVEST_MIN);
			case MISTING -> Component.translatable("message.jugcraft.lair.crown.misting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.crowfield"));
		};
	}

	/** An open mist into the Crowfield within {@value #MIST_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity mist(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - MIST_RANGE, at.getY() - MIST_RANGE, at.getZ() - MIST_RANGE, at.getX() + MIST_RANGE + 1,
				at.getY() + MIST_RANGE + 1, at.getZ() + MIST_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.CROWFIELD)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} into the Crowfield through the mist: the crows' cry, and they come to in the farmyard. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.5F);
		from.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
		return true;
	}
}

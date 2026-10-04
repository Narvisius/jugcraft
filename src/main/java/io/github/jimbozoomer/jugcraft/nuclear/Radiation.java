package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The shared radiation system (docs/features/nuclear.md): every half second, each player's dose rate is worked out
 * from the radioactive blocks, dropped items and carried items around them, through any shielding in between and
 * the hazmat suit they wear; the dose adds up, slowly wears off, and brings on radiation sickness.
 * <p>Units: mSv and mSv per game hour (1,000 ticks). Rate falls off as 1 / (1 + distance²); shielding blocks
 * between source and player cut it (lead and shielding concrete to a twentieth each, water to half, other solid
 * blocks to 60%).
 */
public final class Radiation {
	public static final int INTERVAL = 10;
	public static final double TICKS_PER_HOUR = 1000;
	public static final double RANGE = 48;
	/** Dose that wears off each interval (half the dose is gone in about 23 minutes). */
	public static final double RECOVERY = 0.0005;
	public static final double WEAKNESS = 500, NAUSEA = 1_000, SICKNESS = 2_000, SEVERE = 4_000, LETHAL = 8_000;
	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, io.github.jimbozoomer.jugcraft.Jugcraft.id("radiation"));

	private Radiation() {
	}

	public static RadiationState state(Player player) {
		RadiationState state = ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).getAttached(JugcraftNuclear.RADIATION);
		return state == null ? RadiationState.NONE : state;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator() || player.isCreative()) {
				if (state(player).rate() != 0) {
					((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).setAttached(JugcraftNuclear.RADIATION, new RadiationState(state(player).dose(), 0));
				}
				continue;
			}
			update(player);
		}
	}

	public static void update(ServerPlayer player) {
		double rate = rateAt(player);
		RadiationState old = state(player);
		double dose = old.dose() * (1 - RECOVERY) + rate * INTERVAL / TICKS_PER_HOUR;
		if (dose < 0.01) {
			dose = 0;
		}
		// Sync only when the readout would change.
		if (Math.abs(dose - old.dose()) > 0.05 || Math.abs(rate - old.rate()) > 0.05 || (rate == 0) != (old.rate() == 0)) {
			((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).setAttached(JugcraftNuclear.RADIATION, new RadiationState(dose, rate));
		}
		sickness(player, dose);
	}

	/** The dose rate (mSv/h) at the player, after shielding and their suit. */
	public static double rateAt(Player player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return 0;
		}
		Vec3 eye = player.getEyePosition();
		double nuclear = 0, hawking = 0;
		for (RadiationSources.Source source : RadiationSources.of(level)) {
			Vec3 at = Vec3.atCenterOf(source.pos());
			double d2 = at.distanceToSqr(eye);
			if (d2 > RANGE * RANGE) {
				continue;
			}
			double r = source.strength() / (1 + d2) * shielding(level, at, eye, source.pos());
			if (source.kind() == RadiationKind.HAWKING) {
				hawking += r;
			} else {
				nuclear += r;
			}
		}
		AABB box = player.getBoundingBox().inflate(32);
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
			double s = strength(item.getItem());
			if (s > 0) {
				Vec3 at = item.position().add(0, 0.25, 0);
				nuclear += s / (1 + at.distanceToSqr(eye)) * shielding(level, at, eye, null);
			}
		}
		for (Player other : level.getEntitiesOfClass(Player.class, box)) {
			double s = carried(other);
			if (s <= 0) {
				continue;
			}
			if (other == player) {
				nuclear += s / (1 + 0.25);        // carried at arm's length, half a block away
			} else {
				Vec3 at = other.position().add(0, 1, 0);
				nuclear += s / (1 + at.distanceToSqr(eye)) * shielding(level, at, eye, null);
			}
		}
		int pieces = hazmatPieces(player);
		nuclear *= 1 - RadiationKind.NUCLEAR.hazmatStops * pieces / 4.0;
		hawking *= 1 - RadiationKind.HAWKING.hazmatStops * pieces / 4.0;
		return nuclear + hawking;
	}

	/** Radioactivity of a stack (mSv/h one block away). */
	public static double strength(ItemStack stack) {
		Float per = stack.get(JugcraftNuclear.RADIOACTIVITY);
		return per == null ? 0 : per * stack.getCount();
	}

	/** Radioactivity of everything a player carries. */
	public static double carried(Player player) {
		double total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			total += strength(player.getInventory().getItem(i));
		}
		return total;
	}

	public static int hazmatPieces(Player player) {
		int n = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (player.getItemBySlot(slot).getItem() instanceof HazmatItem) {
				n++;
			}
		}
		return n;
	}

	/** How much of the radiation gets through the blocks between a source and the eye (1 = nothing in the way). */
	static double shielding(ServerLevel level, Vec3 from, Vec3 to, BlockPos skip) {
		double factor = 1;
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		int steps = (int) Math.ceil(length * 2);
		Set<BlockPos> seen = new HashSet<>();
		BlockPos eyeCell = BlockPos.containing(to);
		for (int i = 1; i < steps; i++) {
			BlockPos cell = BlockPos.containing(from.add(delta.scale(i / (double) steps)));
			if (cell.equals(skip) || cell.equals(eyeCell) || !seen.add(cell)) {
				continue;
			}
			BlockState state = level.getBlockState(cell);
			if (state.is(JugcraftNuclear.SHIELDING)) {
				factor *= 0.05;
			} else if (state.getFluidState().is(FluidTags.WATER)) {
				factor *= 0.5;
			} else if (!state.getCollisionShape(level, cell).isEmpty()) {
				factor *= 0.6;
			}
			if (factor < 1e-6) {
				return 0;
			}
		}
		return factor;
	}

	/** Radiation sickness: weakness, then nausea and hunger, then damage that grows with the dose. */
	static void sickness(ServerPlayer player, double dose) {
		if (dose >= WEAKNESS) {
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, dose >= NAUSEA ? 1 : 0, true, false, true));
		}
		if (dose >= NAUSEA) {
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false, true));
		}
		if (dose >= SICKNESS) {
			ServerLevel level = player.level();
			float damage = dose >= LETHAL ? 4.0F : dose >= SEVERE ? 1.0F : 0.5F;
			if (dose >= SEVERE || level.getGameTime() % 40 == 0) {
				player.hurtServer(level, new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE)), damage);
			}
		}
	}

	/** For tests and commands: set a player's dose. */
	public static void setDose(Player player, double dose) {
		((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).setAttached(JugcraftNuclear.RADIATION, new RadiationState(dose, state(player).rate()));
	}
}

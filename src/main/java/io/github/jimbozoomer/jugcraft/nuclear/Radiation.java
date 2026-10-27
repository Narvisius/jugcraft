package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The shared radiation system (docs/features/nuclear.md): every half second, each player's dose rate is worked out
 * from what is radioactive around them (reactors, uranium and thorium blocks and ore, spent fuel, radioactive items
 * dropped, carried, in containers or in item frames), through any shielding in between and the hazmat suit they
 * wear; the dose adds up, slowly wears off, and brings on radiation sickness. Creative players get the rate (a Geiger
 * counter reads it) but take no dose.
 * <p>Units: mSv and mSv per game hour (1,000 ticks). Rate falls off as 1 / (1 + distance²) and a little with the
 * air it crosses; blocks between source and player cut it by what they are made of (Shielding: lead and shielding
 * concrete let 0.1% through a block, stone 10%...). How far a source reaches grows with its strength
 * (RadiationField): a big pile reads much further off than one block.
 */
public final class Radiation {
	public static final int INTERVAL = 10;
	public static final double TICKS_PER_HOUR = 1000;
	public static final double RANGE = 48;
	/** How far round a player radioactive blocks, containers and item frames are looked for. */
	public static final int BLOCK_RANGE = 16;
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
			if (player.isSpectator()) {
				if (state(player).rate() != 0) {
					((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).setAttached(JugcraftNuclear.RADIATION, new RadiationState(state(player).dose(), 0));
				}
				continue;
			}
			update(player);
		}
	}

	public static void update(ServerPlayer player) {
		update(player, player.isCreative());
	}

	/** One update; {@code creative}: the counter still reads the rate, but no dose is taken (tests' mock players are always creative). */
	public static void update(ServerPlayer player, boolean creative) {
		double rate = rateAt(player);
		RadiationState old = state(player);
		double dose = creative ? old.dose() : old.dose() * (1 - RECOVERY) + rate * INTERVAL / TICKS_PER_HOUR;
		if (dose < 0.01) {
			dose = 0;
		}
		// Sync when the readout would change: the dose by a twentieth of a millisievert, the rate by 2% of itself (the
		// counter reads microsieverts an hour, so small rates matter).
		if (Math.abs(dose - old.dose()) > 0.05 || Math.abs(rate - old.rate()) > Math.max(1e-6, 0.02 * Math.max(rate, old.rate()))
				|| (rate == 0) != (old.rate() == 0)) {
			((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).setAttached(JugcraftNuclear.RADIATION, new RadiationState(dose, rate));
		}
		if (!creative) {
			sickness(player, dose);
		}
	}

	/**
	 * The dose rate (mSv/h) at the player, after shielding and their suit. Near the player (RadiationField.NEAR,
	 * rounded out to whole chunk sections) each radioactive block and container counts by itself; further off the
	 * level's section sums and container lists do (RadiationField), as far as each can still be measured.
	 */
	public static double rateAt(Player player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return 0;
		}
		double[] r = rates(level, player.getEyePosition(), player);
		int pieces = hazmatPieces(player);
		return r[0] * (1 - RadiationKind.NUCLEAR.hazmatStops * pieces / 4.0) + r[1] * (1 - RadiationKind.HAWKING.hazmatStops * pieces / 4.0);
	}

	/** The dose rate (mSv/h) at a point, as an unprotected person standing there would get it (surveys, tests). */
	public static double rateAt(ServerLevel level, Vec3 eye) {
		double[] r = rates(level, eye, null);
		return r[0] + r[1];
	}

	/** Nuclear and Hawking dose rates at a point; {@code self} (if any) carries what is in their pack at arm's length. */
	private static double[] rates(ServerLevel level, Vec3 eye, Player self) {
		Shielding.States states = RadiationField.states(level);
		double nuclear = 0, hawking = 0;
		for (RadiationSources.Source source : RadiationSources.of(level)) {
			Vec3 at = Vec3.atCenterOf(source.pos());
			double d2 = at.distanceToSqr(eye);
			double reach = RadiationField.reach(source.strength());
			if (d2 > reach * reach) {
				continue;
			}
			BlockPos own = source.pos();
			double r = source.strength() / (1 + d2) * RadiationField.air(Math.sqrt(d2))
					* Shielding.through(states, at, eye, (cell, state) -> cell.equals(own));
			if (source.kind() == RadiationKind.HAWKING) {
				hawking += r;
			} else {
				nuclear += r;
			}
		}
		// The near box: whole chunk sections round the eye.
		BlockPos c = BlockPos.containing(eye);
		int sx0 = (c.getX() - RadiationField.NEAR) >> 4, sx1 = (c.getX() + RadiationField.NEAR) >> 4;
		int sy0 = (c.getY() - RadiationField.NEAR) >> 4, sy1 = (c.getY() + RadiationField.NEAR) >> 4;
		int sz0 = (c.getZ() - RadiationField.NEAR) >> 4, sz1 = (c.getZ() + RadiationField.NEAR) >> 4;
		// Radioactive blocks near by: uranium and thorium ore and blocks, depleted uranium, spent fuel racks and casks.
		// Each is shielded by what lies between, other radioactive blocks of a pile included.
		for (RadioactiveMaterials.Found found : RadioactiveMaterials.blocksIn(level, eye, sx0 << 4, sy0 << 4, sz0 << 4,
				(sx1 << 4) + 15, (sy1 << 4) + 15, (sz1 << 4) + 15, 4096)) {
			Vec3 at = Vec3.atCenterOf(found.pos());
			double d2 = at.distanceToSqr(eye);
			BlockPos own = found.pos();
			nuclear += found.strength() / (1 + d2) * RadiationField.air(Math.sqrt(d2)) * Shielding.through(states, at, eye, (cell, state) -> cell.equals(own));
		}
		// Piles further off, as one source a section, seen from the player's side of them.
		for (RadiationField.SectionSum sum : RadiationField.sections(level)) {
			int x = SectionPos.x(sum.key()), y = SectionPos.y(sum.key()), z = SectionPos.z(sum.key());
			if (x >= sx0 && x <= sx1 && y >= sy0 && y <= sy1 && z >= sz0 && z <= sz1) {
				continue;
			}
			Vec3 v = eye.subtract(sum.centre());
			double d = v.length();
			if (d > RadiationField.FAR) {
				continue;
			}
			double s = sum.seenFrom(v.x / d, v.y / d, v.z / d);
			double open = s / (1 + d * d) * RadiationField.air(d);
			if (open < RadiationField.FLOOR) {
				continue;
			}
			// The pile's own blocks are already allowed for (seenFrom), so the line passes through them freely.
			nuclear += open * Shielding.through(states, sum.centre(), eye, (cell, state) -> RadioactiveMaterials.block(state) > 0);
		}
		// What is stored in chests, barrels and other containers (shielded ones let little out): near ones as they are
		// now, far ones from the last look round (RadiationField).
		for (int cx = sx0; cx <= sx1; cx++) {
			for (int cz = sz0; cz <= sz1; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (net.minecraft.world.level.block.entity.BlockEntity entity : chunk.getBlockEntities().values()) {
					double s = RadiationField.held(entity);
					if (s > 0) {
						nuclear += stored(states, entity.getBlockPos(), s, eye);
					}
				}
			}
		}
		for (RadiationField.Stored stored : RadiationField.stored(level)) {
			int cx = stored.pos().getX() >> 4, cz = stored.pos().getZ() >> 4;
			if (cx >= sx0 && cx <= sx1 && cz >= sz0 && cz <= sz1) {
				continue;
			}
			nuclear += stored(states, stored.pos(), stored.strength(), eye);
		}
		double reachEntities = 64;
		AABB box = new AABB(eye, eye).inflate(reachEntities);
		for (net.minecraft.world.entity.decoration.ItemFrame frame : level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class, box)) {
			double s = strength(frame.getItem());
			if (s > 0) {
				nuclear += free(states, frame.position(), s, eye);
			}
		}
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
			double s = strength(item.getItem());
			if (s > 0) {
				nuclear += free(states, item.position().add(0, 0.25, 0), s, eye);
			}
		}
		for (Player other : level.getEntitiesOfClass(Player.class, box)) {
			double s = carried(other);
			if (s <= 0) {
				continue;
			}
			if (other == self) {
				nuclear += s / (1 + 0.25);        // carried at arm's length, half a block away
			} else {
				nuclear += free(states, other.position().add(0, 1, 0), s, eye);
			}
		}
		return new double[] {nuclear, hawking};
	}

	/** A container's contents, from the middle of its block (its own block is not counted as shielding). */
	private static double stored(Shielding.States states, BlockPos pos, double s, Vec3 eye) {
		Vec3 at = Vec3.atCenterOf(pos);
		double d2 = at.distanceToSqr(eye);
		double reach = RadiationField.reach(s);
		if (d2 > reach * reach) {
			return 0;
		}
		return s / (1 + d2) * RadiationField.air(Math.sqrt(d2)) * Shielding.through(states, at, eye, (cell, state) -> cell.equals(pos));
	}

	/** Something radioactive in the open (a dropped item, an item frame, another player's pack). */
	private static double free(Shielding.States states, Vec3 at, double s, Vec3 eye) {
		double d2 = at.distanceToSqr(eye);
		double reach = RadiationField.reach(s);
		if (d2 > reach * reach) {
			return 0;
		}
		return s / (1 + d2) * RadiationField.air(Math.sqrt(d2)) * Shielding.through(states, at, eye, null);
	}

	/**
	 * Radioactivity of a stack (mSv/h one block away): its own, or the material's (RadioactiveMaterials); a stack
	 * holding others (a shulker box, a bundle, a lead pig) counts what is inside it, cut by its shielding.
	 */
	public static double strength(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		Float per = stack.get(JugcraftNuclear.RADIOACTIVITY);
		double own = (per == null ? RadioactiveMaterials.item(stack.getItem()) : per) * stack.getCount();
		double inside = 0;
		net.minecraft.world.item.component.ItemContainerContents contents = stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
		if (contents != null) {
			for (net.minecraft.world.item.ItemStackTemplate held : contents.nonEmptyItems()) {
				inside += strength(held.create());
			}
		}
		net.minecraft.world.item.component.BundleContents bundle = stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
		if (bundle != null) {
			for (net.minecraft.world.item.ItemStackTemplate held : bundle.items()) {
				inside += strength(held.create());
			}
		}
		if (inside > 0 && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
				&& blockItem.getBlock() instanceof ShieldedContainer.Block shielded) {
			inside *= shielded.leakage();
		}
		if (stack.getItem() == io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialContainment.ITEM) {
			inside *= io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialContainment.LEAKAGE;
		}
		return own + inside;
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

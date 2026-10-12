package io.github.jimbozoomer.jugcraft.client.guns;

import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Laser Sight (slice 9E, docs/features/guns.md): for each player near the viewer holding a gun with a Laser Sight
 * fitted, a red dot ({@link GunLaserParticle}) each tick where their look first meets a block or a creature within the
 * gun's range. That is where an unstrayed shot lands, since every shot leaves the eye along the look (guns/GunShots), so
 * from the hip it shows where the shots will go; others see it too. It changes nothing in the world and is drawn on the
 * client only: one ray and one particle a tick for each such player within {@link #SEEN_WITHIN} blocks. Slice 10G: with a
 * gun in each hand, both point along the look, so one dot shows if either has a Laser Sight (the main hand's, if both).
 */
public final class GunLaser {
	/** How near the viewer a player must be for their laser to be drawn, in blocks. */
	static final double SEEN_WITHIN = 64.0;
	/** The dot's size: its quad's half width, in blocks, for each block from the viewer (the dot is an eighth of it). */
	static final double SIZE_PER_BLOCK = 0.018;
	/** The smallest half width, close up. */
	static final double MIN_SIZE = 0.03;
	/** How far before what it meets the dot is drawn, so that it is not lost in it, in blocks. */
	static final double LIFT = 0.04;
	/** Dots drawn so far (for the client game tests). */
	private static long dots;
	/** Where the viewer's own laser last drew its dot (for the client game tests); null before any. */
	private static @Nullable Vec3 lastOwn;

	private GunLaser() {
	}

	/** Draws this tick's dots (GunsClient's client tick). */
	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		Player viewer = client.player;
		if (level == null || viewer == null || client.isPaused()) {
			return;
		}
		for (AbstractClientPlayer player : level.players()) {
			if (player.isSpectator() || player.isInvisible() || player.distanceToSqr(viewer) > SEEN_WITHIN * SEEN_WITHIN) {
				continue;
			}
			ItemStack stack = lasered(player);
			if (stack == null) {
				continue;
			}
			Vec3 at = point(level, player, GunItem.spec(stack).range());
			if (at == null) {
				continue;
			}
			double size = Math.max(MIN_SIZE, SIZE_PER_BLOCK * at.distanceTo(viewer.getEyePosition()));
			level.addParticle(JugcraftGuns.LASER_DOT, at.x, at.y, at.z, size, 0.0, 0.0);
			dots++;
			if (player == viewer) {
				lastOwn = at;
			}
		}
	}

	/**
	 * The gun whose Laser Sight draws this player's dot: the main hand's, else (slice 10G) with a gun in each hand the
	 * other's; null if neither has one.
	 */
	static @Nullable ItemStack lasered(Player player) {
		for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), GunItem.dual(player) ? player.getOffhandItem() : ItemStack.EMPTY}) {
			if (stack.getItem() instanceof GunItem && GunItem.attachments(stack).contains(JugcraftGuns.LASER_SIGHT)) {
				return stack;
			}
		}
		return null;
	}

	/**
	 * Where the player's look first meets a block or a living creature within the range, drawn back {@link #LIFT}
	 * toward the eye; null if it meets nothing.
	 */
	static @Nullable Vec3 point(Level level, Player player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(range));
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		boolean hitBlock = block.getType() != HitResult.Type.MISS;
		Vec3 reach = hitBlock ? block.getLocation() : end;
		Vec3 nearest = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, reach).inflate(1.0),
				creature -> creature != player && creature.isAlive() && !creature.isSpectator())) {
			var clip = creature.getBoundingBox().clip(eye, reach);
			if (clip.isPresent() && clip.get().distanceToSqr(eye) < best) {
				best = clip.get().distanceToSqr(eye);
				nearest = clip.get();
			}
		}
		Vec3 hit = nearest != null ? nearest : hitBlock ? reach : null;
		return hit == null ? null : hit.subtract(look.scale(LIFT));
	}

	/** Dots drawn so far. */
	public static long dots() {
		return dots;
	}

	/** Where the viewer's own laser last drew its dot, or null before any. */
	public static @Nullable Vec3 lastOwn() {
		return lastOwn;
	}
}

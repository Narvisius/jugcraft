package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Plasma wounds (Justin approved the plasma wounds sheet, take 2, on 6 November 2026): where a plasma bolt or the plasma
 * baton hits any living thing, a chunk of it is burned out: from a bolt, a tunnel right through it along the shot that
 * you can see through (remade 6 November 2026: through clothes, armour, a head, any mob, whatever the angle), from the
 * baton, a gash a few pixels deep along the rod; glowing hot and cooling to char. The server only works out where and which way, in the hit one's own frame (the feet, facing +z), and
 * sends that to the players who can see it; the hole itself is drawn on each client (client/PlasmaWoundsClient), followed
 * through every part of the model in its way. The bolt still stops at the first thing it hits.
 *
 * <p>Since r196 (the power armour sheets, takes 5 to 8; Justin, 9 November 2026) the cut follows the weapon: a swing's
 * wound is the fan its edge swept ({@link Swing}), on what was hit and on every foe in its path ({@link #sweep},
 * {@link #thrust}); the client burns away what lies in it, and what's cut right off falls and burns out
 * (client/PlasmaWoundsDebris). And a cut can kill outright ({@link #strike}): the server judges that on its own measure
 * of the creature ({@link Vitals}).
 */
public final class PlasmaWounds {
	/** A bolt's or a thrust's tunnel, a gash a set depth in from where it struck, and a swing's fan ({@link Swing}). */
	public static final byte THROUGH = 0, GASH = 2, FAN = 3;
	/** A bolt's tunnel's radius (model pixels), and a blade thrust's. */
	public static final float BOLT_RADIUS = 2.0F, THRUST_RADIUS = 1.5F;
	/** The baton's gash (px): a trench burned along the rod where it struck, its half-width, depth and length. */
	public static final float GASH_HALF = 1.0F, GASH_DEPTH = 2.5F, GASH_LENGTH = 7.0F;
	/** A swing that lands burns in at least this deep (px), however short the edge fell (as the baton's gash always did). */
	public static final float BITE = GASH_DEPTH;
	/**
	 * A wound's flags: the blow killed it (the creature comes apart along the cut); it's one that can't be killed outright
	 * (a player, a boss: a swing only gashes its trunk and head, a bite deep).
	 */
	public static final byte FATAL = 1, GUARDED = 2;
	/** A creature with at least this much health is as good as a boss: it can't be killed outright. */
	public static final float GUARDED_HEALTH = 200F;

	/** A wound on entity {@code entity}: where (blocks from its feet, its yaw taken out: +z its front), which way the
	 * plasma went (the same frame), through or a gash, its radius (a gash's half-width) and a gash's depth (px), a seed
	 * for its ragged edge, and a gash's line along the surface (the same frame) and length (px). A swing's fan
	 * ({@link #FAN}): where its pivot was, the way of its middle, the way the edge swept across that, the edge's
	 * half-thickness (px), how far out it burned (depth) and from how far out (inner; blocks), and the sweep's half angle
	 * (length; radians). And its flags ({@link #FATAL}, {@link #GUARDED}). */
	public record Payload(int entity, float hx, float hy, float hz, float dx, float dy, float dz, byte kind, float radius, float depth, int seed,
			float ux, float uy, float uz, float length, float inner, byte flags) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Jugcraft.id("plasma_wound"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeVarInt(p.entity);
			buf.writeFloat(p.hx);
			buf.writeFloat(p.hy);
			buf.writeFloat(p.hz);
			buf.writeFloat(p.dx);
			buf.writeFloat(p.dy);
			buf.writeFloat(p.dz);
			buf.writeByte(p.kind);
			buf.writeFloat(p.radius);
			buf.writeFloat(p.depth);
			buf.writeInt(p.seed);
			buf.writeFloat(p.ux);
			buf.writeFloat(p.uy);
			buf.writeFloat(p.uz);
			buf.writeFloat(p.length);
			buf.writeFloat(p.inner);
			buf.writeByte(p.flags);
		}, buf -> new Payload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
				buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
				buf.readFloat(), buf.readByte()));

		/** This wound with {@code flags} set as well. */
		public Payload with(int more) {
			return new Payload(entity, hx, hy, hz, dx, dy, dz, kind, radius, depth, seed, ux, uy, uz, length, inner, (byte) (flags | more));
		}

		public boolean fatal() {
			return (flags & FATAL) != 0;
		}

		public boolean guarded() {
			return (flags & GUARDED) != 0;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private PlasmaWounds() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
		// whatever plasma kills drops its meat cooked (Justin, 8 November 2026)
		net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			net.minecraft.world.damagesource.DamageSource source = context.getOptional(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE);
			if (source != null && context.getOptional(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY) instanceof LivingEntity
					&& plasma(source)) {
				drops.replaceAll(stack -> cook(context.getLevel(), stack));
			}
		});
	}

	/** Whether {@code source} is plasma: a bolt, or a blow from someone holding a plasma baton (a player's, a gate guard's). */
	public static boolean plasma(net.minecraft.world.damagesource.DamageSource source) {
		if (source.getDirectEntity() instanceof PlasmaBolt) {
			return true;
		}
		return source.getEntity() instanceof LivingEntity by && source.getDirectEntity() == by
				&& (by.getMainHandItem().getItem() instanceof PlasmaBatonItem || by instanceof TowerCrew crew && crew.batoned()
						|| by instanceof PlasmaBlade.Wearer w && PlasmaBlade.out(w.act()));
	}

	/**
	 * A drop as plasma leaves it: food that cooks (its smoker recipe, or a furnace's when that makes food too) cooked, the
	 * same count; anything else as it was. From the recipes, so every mod's meat cooks as its own furnace would do it.
	 */
	public static net.minecraft.world.item.ItemStack cook(ServerLevel level, net.minecraft.world.item.ItemStack stack) {
		if (stack.isEmpty() || !stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
			return stack;
		}
		var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack.copyWithCount(1));
		var recipes = level.recipeAccess();
		net.minecraft.world.item.ItemStack cooked = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMOKING, input, level)
				.map(r -> r.value().assemble(input)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
		if (cooked.isEmpty()) {
			cooked = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level)
					.map(r -> r.value().assemble(input)).filter(r -> r.has(net.minecraft.core.component.DataComponents.FOOD))
					.orElse(net.minecraft.world.item.ItemStack.EMPTY);
		}
		return cooked.isEmpty() ? stack : cooked.copyWithCount(stack.getCount() * cooked.getCount());
	}

	/**
	 * Where a blow from {@code attacker} lands on {@code target} and which way it goes (Justin, 8 November 2026: the
	 * baton's wound "always in the same spot", low on a big mob): along the attacker's own aim, where it meets the target;
	 * if their aim misses the box (a sweep), at their eye's height on it (within it), towards it.
	 */
	public static Vec3[] aim(LivingEntity attacker, LivingEntity target) {
		Vec3 eye = attacker.getEyePosition();
		Vec3 look = attacker.getViewVector(1.0F);
		AABB box = target.getBoundingBox();
		Optional<Vec3> hit = box.inflate(target.getPickRadius() + 0.05).clip(eye, eye.add(look.scale(16)));
		if (hit.isPresent()) {
			return new Vec3[] {hit.get(), look};
		}
		double h = box.getYsize();
		Vec3 at = new Vec3(box.getCenter().x, Mth.clamp(eye.y - 0.25, box.minY + h * 0.2, box.maxY - h * 0.1), box.getCenter().z);
		return new Vec3[] {at, at.subtract(eye)};
	}

	/** Where a line along {@code dir} through {@code at} first meets {@code box} (from the side it comes from), if it does. */
	public static Optional<Vec3> entry(AABB box, Vec3 at, Vec3 dir) {
		return box.clip(at.subtract(dir.scale(8)), at.add(dir.scale(8)));
	}

	/** The wound {@code burn} sends (null when there's no way to it): where it went in, in the target's own frame. */
	public static @Nullable Payload wound(LivingEntity target, Vec3 at, Vec3 dir, boolean baton, RandomSource random) {
		return wound(target, at, dir, baton, baton ? GASH_HALF : BOLT_RADIUS, random);
	}

	/** A tunnel right through {@code target} along {@code dir} through {@code at} (world), {@code radius} px (a thrust's). */
	public static @Nullable Payload tunnel(LivingEntity target, Vec3 at, Vec3 dir, float radius, RandomSource random) {
		return wound(target, at, dir, false, radius, random);
	}

	private static @Nullable Payload wound(net.minecraft.world.entity.Entity target, Vec3 at, Vec3 dir, boolean baton, float radius, RandomSource random) {
		if (dir.lengthSqr() < 1e-6) {
			return null;
		}
		dir = dir.normalize();
		AABB box = target.getBoundingBox();
		Vec3 hit = entry(box, at, dir).orElse(at);
		// a bolt goes right through whatever it hits, however thick (Justin, 6 November 2026: "there should be just a hole in
		// its head"); the client follows it through the model
		byte kind = baton ? GASH : THROUGH;
		float depth = baton ? GASH_DEPTH : 0;
		// the gash's line: the rod across the blow (level, square to it), tilted 15-45 degrees one way or the other
		Vec3 across = dir.cross(new Vec3(0, 1, 0));
		across = across.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : across.normalize();
		Vec3 upish = across.cross(dir).normalize();
		float tilt = (15 + random.nextFloat() * 30) * Mth.DEG_TO_RAD * (random.nextBoolean() ? 1 : -1);
		Vec3 u = across.scale(Mth.cos(tilt)).add(upish.scale(Mth.sin(tilt)));
		// into its own frame: from its feet, its body's yaw taken out (its front +z)
		float yaw = yaw(target);
		float c = Mth.cos(yaw), s = Mth.sin(yaw);
		Vec3 o = hit.subtract(target.position());
		float hx = (float) (o.x * c + o.z * s), hz = (float) (-o.x * s + o.z * c);
		float dx = (float) (dir.x * c + dir.z * s), dz = (float) (-dir.x * s + dir.z * c);
		float ux = (float) (u.x * c + u.z * s), uz = (float) (-u.x * s + u.z * c);
		return new Payload(target.getId(), hx, (float) o.y, hz, dx, (float) dir.y, dz, kind, radius, depth, random.nextInt(),
				ux, (float) u.y, uz, baton ? GASH_LENGTH : 0, 0, (byte) 0);
	}

	/**
	 * Burns a wound into {@code target}: aimed at {@code at} along {@code dir} (world), a bolt's ({@code baton} false: a
	 * tunnel right through) or the baton's (a gash: a trench burned along the rod, which lies across the blow, tilted a
	 * little either way); tells the players who can see it. Only the mark: {@link #strike} is the blow with what it does.
	 */
	public static void burn(ServerLevel level, LivingEntity target, Vec3 at, Vec3 dir, boolean baton) {
		send(target, wound(target, at, dir, baton, level.getRandom()));
	}

	private static void send(net.minecraft.world.entity.Entity target, @Nullable Payload p) {
		if (p == null) {
			return;
		}
		for (ServerPlayer player : PlayerLookup.tracking(target)) {
			ServerPlayNetworking.send(player, p);
		}
		if (target instanceof ServerPlayer self) {
			ServerPlayNetworking.send(self, p);
		}
	}

	/** The way an entity's own frame faces (radians): a living one's body, anything else's own turn. */
	static float yaw(net.minecraft.world.entity.Entity e) {
		return (e instanceof LivingEntity l ? l.yBodyRot : e.getYRot()) * Mth.DEG_TO_RAD;
	}

	// ------------------------------------------------------------------ the parts of a creature (10 November 2026)
	// Justin: "I shot this murmur ... and like the head. I guess it's two different entity parts ... it's not showing the
	// shot that went through their head". Some creatures are more than one entity: a body that is the creature, and parts
	// of it that only take blows for it (the Murmur's neck and head, the serpents' and worms' segments: menagerie's
	// RecoveredCreaturePart; the ender dragon's). A plasma hit on such a part burns its wound into that part, which its
	// own renderer draws cut as a creature's model is (client/PlasmaWoundsMesh: the parts' model path); the dragon's parts
	// are drawn with the dragon, so a hit on one burns the dragon itself.

	/** The living creature {@code e} is a part of (it takes blows for it), or null if it's none's. */
	public static @Nullable LivingEntity partOf(net.minecraft.world.entity.Entity e) {
		if (e instanceof LivingEntity) {
			return null;
		}
		if (e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart p) {
			return p.parentMob;
		}
		if (e instanceof io.github.jimbozoomer.jugcraft.menagerie.RecoveredCreaturePart p) {
			return p.parent();
		}
		return null;
	}

	/** Whether {@code e} is a part of a creature that its own renderer draws (so its wound goes on it, not the creature). */
	static boolean drawnApart(net.minecraft.world.entity.Entity e) {
		return partOf(e) != null && !(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart);
	}

	/**
	 * A plasma bolt (or a thrust: {@code radius}) has hit {@code hit}, a part of a creature, at {@code at} along {@code dir}
	 * (its damage dealt by the caller, with {@code source}): its tunnel, on the part if its own renderer draws it, else on
	 * the creature. Nothing if {@code hit} is no creature's part. Returns the wound sent (null: none).
	 */
	public static @Nullable Payload hitPart(ServerLevel level, net.minecraft.world.entity.Entity hit, net.minecraft.world.damagesource.DamageSource source,
			Vec3 at, Vec3 dir, float radius) {
		LivingEntity whole = partOf(hit);
		if (whole == null) {
			return null;
		}
		if (drawnApart(hit)) {
			Payload p = wound(hit, at, dir, false, radius, level.getRandom());
			send(hit, p);
			return p;
		}
		Payload p = judge(level, whole, source, wound(whole, at, dir, false, radius, level.getRandom()));
		send(whole, p);
		return p;
	}

	/**
	 * The parts drawn apart (a Murmur's head) of {@code target} and of {@code by}'s foes within {@code box} that {@code meets}
	 * says the weapon went through: each burned with {@code mark}; a foe that hasn't taken the blow yet ({@code hurt}: those
	 * that have) takes {@code damage} through its part.
	 */
	private static void parts(ServerLevel level, LivingEntity by, LivingEntity target, AABB box, java.util.function.Predicate<AABB> meets,
			java.util.function.Function<net.minecraft.world.entity.Entity, Payload> mark, java.util.Set<LivingEntity> hurt,
			net.minecraft.world.damagesource.DamageSource source, float damage) {
		for (net.minecraft.world.entity.Entity part : level.getEntities((net.minecraft.world.entity.Entity) null, box, e -> {
			LivingEntity w = drawnApart(e) ? partOf(e) : null;
			return w != null && (w == target || foe(by, w));
		})) {
			if (!meets.test(part.getBoundingBox())) {
				continue;
			}
			LivingEntity whole = partOf(part);
			if (whole != target && hurt.add(whole)) {
				part.hurtServer(level, source, damage);
				PlasmaBaton.hitEffects(level, whole, net.minecraft.sounds.SoundSource.HOSTILE);
			}
			send(part, mark.apply(part));
		}
	}

	// ------------------------------------------------------------------ the cut follows the weapon (r196)

	/**
	 * A swing of a plasma edge, where it really went (world; Justin, 9 November 2026: "i want the damage to the body to be
	 * reflected in the path of the sword or projectile. If the sword swings across a certain place, everything it hits I
	 * want to be affected"): the shoulder it turns about, the way of its middle (unit), the way the edge travels across
	 * that (unit, square to it), from how far out to how far out the edge burns (blocks), the sweep's half angle (radians)
	 * and the edge's half-thickness (model px). Whatever lies in that fan is burned away.
	 */
	public record Swing(Vec3 pivot, Vec3 dir, Vec3 sweep, float inner, float outer, float half, float radius) {
		/** The point of it {@code rho} blocks out, {@code phi} radians round from its middle. */
		public Vec3 at(double rho, double phi) {
			return pivot.add(dir.scale(rho * Math.cos(phi))).add(sweep.scale(rho * Math.sin(phi)));
		}

		/** Whether the edge passes through {@code box}. */
		public boolean meets(AABB box) {
			AABB grown = box.inflate(radius / 16.0);
			for (int a = -8; a <= 8; a++) {
				double phi = half * a / 8.0;
				if (grown.clip(at(inner, phi), at(outer, phi)).isPresent() || grown.contains(at(inner, phi))) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * {@code by}'s swing at {@code target}: about their right shoulder, its middle through {@code aim} (null: the
	 * target's middle line at about the shoulder's own height, within it), the edge coming down across it tilted
	 * {@code tilt} degrees towards their left (below 0: their right), burning from {@code inner} to {@code outer} blocks
	 * out (at their size 1), over {@code half} degrees either side of the middle.
	 */
	public static Swing swing(LivingEntity by, LivingEntity target, @Nullable Vec3 aim, float tilt, float inner, float outer, float half, float radius) {
		Vec3 to = new Vec3(target.getX() - by.getX(), 0, target.getZ() - by.getZ());
		Vec3 front = to.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, by.getYRot()) : to.normalize();
		Vec3 right = new Vec3(-front.z, 0, front.x);
		Vec3 pivot = by.position().add(0, by.getBbHeight() * 0.72, 0).add(right.scale(by.getBbWidth() * 0.5));
		AABB box = target.getBoundingBox();
		double h = box.getYsize();
		Vec3 at = aim != null ? aim : new Vec3(box.getCenter().x, Mth.clamp(pivot.y - 0.1, box.minY + h * 0.25, box.maxY - h * 0.2), box.getCenter().z);
		Vec3 dir = at.subtract(pivot);
		dir = dir.lengthSqr() < 1e-6 ? front : dir.normalize();
		float t = tilt * Mth.DEG_TO_RAD;
		Vec3 down = new Vec3(0, -Mth.cos(t), 0).add(right.scale(-Mth.sin(t)));
		Vec3 sweep = down.subtract(dir.scale(down.dot(dir)));
		sweep = sweep.lengthSqr() < 1e-6 ? right.scale(-1) : sweep.normalize();
		float size = by.getScale();
		return new Swing(pivot, dir, sweep, inner * size, outer * size, half * Mth.DEG_TO_RAD, radius);
	}

	/** A swing's wound on {@code target}, in its own frame: the fan itself (the client burns away what lies in it). */
	public static Payload fan(net.minecraft.world.entity.Entity target, Swing s, RandomSource random) {
		float yaw = yaw(target);
		float c = Mth.cos(yaw), sn = Mth.sin(yaw);
		Vec3 o = s.pivot().subtract(target.position());
		Vec3 d = s.dir(), u = s.sweep();
		return new Payload(target.getId(), (float) (o.x * c + o.z * sn), (float) o.y, (float) (-o.x * sn + o.z * c),
				(float) (d.x * c + d.z * sn), (float) d.y, (float) (-d.x * sn + d.z * c), FAN, s.radius(), s.outer(), random.nextInt(),
				(float) (u.x * c + u.z * sn), (float) u.y, (float) (-u.x * sn + u.z * c), s.half(), s.inner(), (byte) 0);
	}

	/** Whether {@code target} can't be killed outright (Justin, 9 November 2026: "Ordinary creatures only"): a player, a boss. */
	public static boolean guarded(LivingEntity target) {
		return target instanceof net.minecraft.world.entity.player.Player
				|| target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
				|| target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
				|| target instanceof net.minecraft.world.entity.monster.warden.Warden
				|| target.getType().builtInRegistryHolder().is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags.BOSSES)
				|| target.getMaxHealth() >= GUARDED_HEALTH;
	}

	/**
	 * What a plasma blow that has landed does to {@code target} beyond its damage (the caller has dealt that, with
	 * {@code source}): its wound {@code p}, burned along the weapon's own path; and (Justin, 9 November 2026: "if it does
	 * something like cut a torso in half or a head is decimated that the creature simply dies") an ordinary creature whose
	 * trunk that cuts in two, whose head it takes off or more than half burns away, dies at once. A blow that kills, either
	 * way, is marked so: the creature comes apart along the cut. Tells the players who can see it. Returns whether it
	 * is dead of it.
	 */
	public static boolean strike(ServerLevel level, LivingEntity target, net.minecraft.world.damagesource.DamageSource source, @Nullable Payload p) {
		Payload judged = judge(level, target, source, p);
		send(target, judged);
		return judged != null && judged.fatal();
	}

	/** {@link #strike} without the telling: the kill if it is one, and the wound as it's sent (marked fatal, or guarded). */
	public static @Nullable Payload judge(ServerLevel level, LivingEntity target, net.minecraft.world.damagesource.DamageSource source, @Nullable Payload p) {
		if (p == null) {
			return null;
		}
		int flags = 0;
		if (guarded(target)) {
			flags |= GUARDED;
		} else if (target.isAlive() && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand) && !target.isSleeping()) {
			Vitals vitals = VITALS.get(target);
			if (vitals == null || !vitals.fits(target)) {
				vitals = Vitals.of(target);
				VITALS.put(target, vitals);
			}
			if (vitals.burn(p, target.getScale() / 16F) && vitals.fatal()) {
				target.setInvulnerableTime(0);
				target.hurtServer(level, source, Float.MAX_VALUE);
			}
		}
		if (target.isDeadOrDying()) {
			flags |= FATAL;
			VITALS.remove(target);
		}
		return p.with(flags);
	}

	/** Whether a swing or thrust of {@code by}'s at something else cuts {@code other} too when it lies in the way: their foes only. */
	public static boolean foe(LivingEntity by, LivingEntity other) {
		if (other == by || !other.isAlive() || other instanceof TowerCrew || other instanceof io.github.jimbozoomer.jugcraft.tower.squad.Legionary
				|| other instanceof net.minecraft.world.entity.decoration.ArmorStand) {
			return false;
		}
		return other instanceof net.minecraft.world.entity.monster.Enemy || other instanceof net.minecraft.world.entity.Mob m && m.getTarget() == by;
	}

	/**
	 * {@code by}'s swing {@code s} has landed on {@code target} (its damage dealt by the caller, with {@code source}): the
	 * fan's wound on it, and on every foe of theirs the edge passed through as well, each taking {@code damage} too.
	 */
	public static void sweep(ServerLevel level, LivingEntity by, LivingEntity target, Swing s, net.minecraft.world.damagesource.DamageSource source,
			float damage) {
		strike(level, target, source, fan(target, s, level.getRandom()));
		double reach = s.outer() + 0.5;
		java.util.Set<LivingEntity> hurt = new java.util.HashSet<>();
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(s.pivot(), s.pivot()).inflate(reach),
				e -> e != target && foe(by, e))) {
			if (s.meets(other.getBoundingBox())) {
				hurt.add(other);
				other.hurtServer(level, source, damage);
				PlasmaBaton.hitEffects(level, other, net.minecraft.sounds.SoundSource.HOSTILE);
				strike(level, other, source, fan(other, s, level.getRandom()));
			}
		}
		// and the parts of creatures the edge went through (a Murmur's head: its own entity)
		parts(level, by, target, new AABB(s.pivot(), s.pivot()).inflate(reach), s::meets, e -> fan(e, s, level.getRandom()), hurt, source, damage);
	}

	/**
	 * {@code by}'s thrust has landed on {@code target} (its damage dealt by the caller): a tunnel right through it along
	 * the blade, from their shoulder at {@code aim} (null: as {@link #swing} aims); and through every foe of theirs the
	 * blade ({@code reach} blocks from the shoulder) went into as well, each taking {@code damage} too.
	 */
	public static void thrust(ServerLevel level, LivingEntity by, LivingEntity target, @Nullable Vec3 aim, float reach, float radius,
			net.minecraft.world.damagesource.DamageSource source, float damage) {
		Swing s = swing(by, target, aim, 0, 0, reach, 1, radius);
		Vec3 tip = s.pivot().add(s.dir().scale(s.outer()));
		strike(level, target, source, tunnel(target, s.pivot(), s.dir(), radius, level.getRandom()));
		java.util.Set<LivingEntity> hurt = new java.util.HashSet<>();
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(s.pivot(), tip).inflate(0.5), e -> e != target && foe(by, e))) {
			if (other.getBoundingBox().inflate(radius / 16.0).clip(s.pivot(), tip).isPresent()) {
				hurt.add(other);
				other.hurtServer(level, source, damage);
				PlasmaBaton.hitEffects(level, other, net.minecraft.sounds.SoundSource.HOSTILE);
				strike(level, other, source, tunnel(other, s.pivot(), s.dir(), radius, level.getRandom()));
			}
		}
		parts(level, by, target, new AABB(s.pivot(), tip).inflate(0.5), b -> b.inflate(radius / 16.0).clip(s.pivot(), tip).isPresent(),
				e -> wound(e, s.pivot(), s.dir(), false, radius, level.getRandom()), hurt, source, damage);
	}

	private static final java.util.Map<LivingEntity, Vitals> VITALS = new java.util.WeakHashMap<>();

	/**
	 * What the server keeps of a creature to judge a kill by (it has no models: those are the client's): its game box in
	 * small cells, those of its trunk (a column up its middle; a squat creature's, a beam along it) and of its head (a
	 * cube about its eyes: on top, or at a squat creature's front), and which of them plasma has burned away. The same
	 * tunnels and fans the client cuts are burned through it.
	 */
	static final class Vitals {
		private static final byte CORE = 1, HEAD = 2, BURNED = 4;
		final float w, h, eye, cell;
		final int nx, ny, nz;
		final byte[] cells;
		int core, head;

		private Vitals(float w, float h, float eye) {
			this.w = w;
			this.h = h;
			this.eye = eye;
			float m = Math.min(w, h);
			cell = Mth.clamp(m / 12F, 1F / 32F, 0.25F);
			nx = Math.max(1, Mth.ceil(w / cell));
			nz = nx;
			ny = Math.max(1, Mth.ceil(h / cell));
			cells = new byte[nx * ny * nz];
			boolean squat = h <= 1.6F * w;
			float hs = Mth.clamp(2.4F * (h - eye), 0.3F * m, m);
			float hy1 = Math.min(h, eye + hs / 2), hy0 = Math.max(0, hy1 - hs);
			float hz0 = squat ? w / 2 - hs : -hs / 2, hz1 = hz0 + hs;
			float cy0 = squat ? 0.4F * h : Math.min(0.3F * h, Math.max(0, hy0 - 3 * cell)), cy1 = squat ? Math.max(0.95F * h, cy0 + cell) : hy0;
			float cz0 = squat ? -0.45F * w : -0.2F * w, cz1 = squat ? hz0 : 0.2F * w, cx = 0.35F * w;
			for (int i = 0; i < nx; i++) {
				float x = x(i);
				for (int j = 0; j < ny; j++) {
					float y = y(j);
					for (int k = 0; k < nz; k++) {
						float z = z(k);
						byte v = 0;
						if (Math.abs(x) <= hs / 2 && y >= hy0 && y <= hy1 && z >= hz0 && z <= hz1) {
							v = HEAD;
							head++;
						} else if (Math.abs(x) <= cx && y >= cy0 && y <= cy1 && z >= cz0 && z <= cz1) {
							v = CORE;
							core++;
						}
						cells[index(i, j, k)] = v;
					}
				}
			}
		}

		static Vitals of(LivingEntity e) {
			return new Vitals(e.getBbWidth(), e.getBbHeight(), e.getEyeHeight());
		}

		boolean fits(LivingEntity e) {
			return Math.abs(e.getBbWidth() - w) < 1e-3F && Math.abs(e.getBbHeight() - h) < 1e-3F;
		}

		private int index(int i, int j, int k) {
			return (i * ny + j) * nz + k;
		}

		private float x(int i) {
			return (i + 0.5F) * cell - nx * cell / 2;
		}

		private float y(int j) {
			return (j + 0.5F) * cell;
		}

		private float z(int k) {
			return (k + 0.5F) * cell - nz * cell / 2;
		}

		/**
		 * Burns wound {@code p} through the cells ({@code px}: blocks to one of the creature's model pixels); whether it
		 * took any of the trunk or head.
		 */
		boolean burn(Payload p, float px) {
			// as the client sizes a wound on a little creature (PlasmaWoundsClient.sizeFor)
			float size = Mth.clamp(0.21F * Math.max(1F, Math.min(w, h) / px) / BOLT_RADIUS, 0.15F, 1F);
			float r = p.radius() * px * size + cell * 0.25F;
			Vec3 o = new Vec3(p.hx(), p.hy(), p.hz());
			Vec3 d = new Vec3(p.dx(), p.dy(), p.dz()).normalize();
			Vec3 u = new Vec3(p.ux(), p.uy(), p.uz());
			u = u.subtract(d.scale(u.dot(d)));
			u = u.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : u.normalize();
			Vec3 n = d.cross(u);
			double outer = p.depth();
			if (p.kind() == FAN) {
				// a swing that landed bites at least so deep from where its fan first meets the flesh
				double first = Double.MAX_VALUE;
				for (int c = 0; c < cells.length; c++) {
					if (cells[c] == CORE || cells[c] == HEAD) {
						Vec3 q = at(c).subtract(o);
						double a = q.dot(d), b = q.dot(u);
						if (Math.abs(q.dot(n)) <= r && Math.abs(Math.atan2(b, a)) <= p.length()) {
							first = Math.min(first, Math.sqrt(a * a + b * b));
						}
					}
				}
				if (first == Double.MAX_VALUE) {
					return false;
				}
				outer = Math.max(outer, first + BITE * px * size);
			}
			boolean any = false;
			for (int c = 0; c < cells.length; c++) {
				if (cells[c] != CORE && cells[c] != HEAD) {
					continue;
				}
				Vec3 q = at(c).subtract(o);
				double t = q.dot(d);
				boolean in;
				if (p.kind() == FAN) {
					double b = q.dot(u), rho = Math.sqrt(t * t + b * b), phi = Math.atan2(b, t), off;
					if (Math.abs(phi) <= p.length()) {
						off = Math.max(0, Math.max(p.inner() - rho, rho - outer));
					} else {
						double e = Math.copySign(p.length(), phi);
						double along = Mth.clamp(t * Math.cos(e) + b * Math.sin(e), p.inner(), outer);
						off = Math.hypot(t - along * Math.cos(e), b - along * Math.sin(e));
					}
					in = Math.hypot(off, q.dot(n)) <= r;
				} else {
					Vec3 off = q.subtract(d.scale(t));
					if (p.kind() == GASH) {
						if (t < -2.5 * px || t > p.depth() * px * size) {
							continue;
						}
						off = off.subtract(u.scale(Mth.clamp(off.dot(u), -p.length() / 2 * px * size, p.length() / 2 * px * size)));
					}
					in = off.lengthSqr() <= r * r;
				}
				if (in) {
					cells[c] |= BURNED;
					any = true;
				}
			}
			return any;
		}

		private Vec3 at(int c) {
			return new Vec3(x(c / (ny * nz)), y(c / nz % ny), z(c % nz));
		}

		/**
		 * Whether what's burned is the death of it: more than half its head gone, its trunk in two (the lesser piece at
		 * least 15 parts in 100 of it), or its head (what's left of it) off its trunk.
		 */
		boolean fatal() {
			int headLeft = 0;
			for (byte b : cells) {
				if (b == HEAD) {
					headLeft++;
				}
			}
			if (head > 0 && headLeft * 2 < head) {
				return true;
			}
			if (core == 0) {
				return false;
			}
			int[] piece = new int[cells.length];
			int best = 0, bestSize = 0, second = 0, pieces = 0;
			for (int c = 0; c < cells.length; c++) {
				if (cells[c] == CORE && piece[c] == 0) {
					int size = fill(c, ++pieces, piece, false);
					if (size > bestSize) {
						second = bestSize;
						bestSize = size;
						best = pieces;
					} else if (size > second) {
						second = size;
					}
				}
			}
			if (bestSize == 0 || second >= 0.15F * core) {
				return true;
			}
			if (headLeft == 0) {
				return false;
			}
			// the head still on: reached from the trunk's main piece through what's left
			int[] reached = new int[cells.length];
			int from = -1;
			for (int c = 0; c < cells.length && from < 0; c++) {
				if (piece[c] == best) {
					from = c;
				}
			}
			fill(from, 1, reached, true);
			int on = 0;
			for (int c = 0; c < cells.length; c++) {
				if (cells[c] == HEAD && reached[c] == 1) {
					on++;
				}
			}
			return on * 2 < headLeft;
		}

		/** Marks the piece of unburned trunk (and, {@code withHead}, head) that cell {@code from} is in; how many cells. */
		private int fill(int from, int mark, int[] piece, boolean withHead) {
			int[] stack = new int[cells.length];
			int top = 0, size = 0;
			stack[top++] = from;
			piece[from] = mark;
			while (top > 0) {
				int c = stack[--top];
				size++;
				int i = c / (ny * nz), j = c / nz % ny, k = c % nz;
				for (int s = 0; s < 6; s++) {
					int a = i + (s == 0 ? 1 : s == 1 ? -1 : 0), b = j + (s == 2 ? 1 : s == 3 ? -1 : 0), e = k + (s == 4 ? 1 : s == 5 ? -1 : 0);
					if (a < 0 || b < 0 || e < 0 || a >= nx || b >= ny || e >= nz) {
						continue;
					}
					int o = index(a, b, e);
					if (piece[o] == 0 && (cells[o] == CORE || withHead && cells[o] == HEAD)) {
						piece[o] = mark;
						stack[top++] = o;
					}
				}
			}
			return size;
		}
	}
}

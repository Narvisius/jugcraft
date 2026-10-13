package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * Two-handed swings (Arms III, batch 46, docs/features/arms-iii.md), after studying how Fiery Combat (a Bedrock add-on)
 * swings its greatswords; none of its code, animations or numbers is used.
 *
 * <p>A click with a two-handed arm ({@link JugcraftArms#TWO_HANDED}) does not hit at once, as vanilla's does: the client
 * (client/arms/TwoHandedInput) asks for a swing instead ({@link TwoHandedSwingPayload}). The server starts it, or
 * queues it behind a swing that is about to end, or refuses it (no two-handed arm in the main hand, something that
 * blocks in the off hand, using an item, busy with a weapon art, dead or spectating). A swing:
 * <ul>
 * <li>swings the arm for everyone, starts the attack charge again (as a click does), stops sprinting and slows its
 * wielder by {@link JugcraftArms#TWO_HANDED_SLOW} until it ends;</li>
 * <li>lands its blow {@code strike} ticks after the click, when the kind's animation lands it, on every foe in the
 * kind's arc within the arm's reach and in sight, nearest first, up to its {@code targets};</li>
 * <li>strikes each through vanilla's own thrust attack (Player.stabAttack: enchantments, knockback, wear, the item's
 * hit hooks) for the arm's attack damage and its trait bonus, at the charge the click had;</li>
 * <li>is the finishing blow, {@link JugcraftArms#FINISHER} times as strong, when it is the last attack of the kind's
 * combo (clicks within {@link JugcraftArms#COMBO_WINDOW} ticks carry the combo on); a maul's finishing blow also
 * shakes the ground ({@link JugcraftArms.Trait#QUAKE}), and a labrys's whirls right round ({@link JugcraftArms.Trait#WHIRL}).</li>
 * </ul>
 * Switching away from the arm, dying or leaving cancels the swing. Foes that other code protects from the player
 * (AttackEntityCallback, such as the town's townsfolk), allies, tamed pets of the wielder, the wielder's mount and
 * marker armor stands are never struck. Vanilla's instant hit with a two-handed arm is refused on the server, so a
 * client cannot skip the swing. The work each tick is over the swings in the air only.
 */
public final class TwoHanded {
	/** The movement modifier a swing in the air puts on its wielder. */
	public static final Identifier SLOW = Jugcraft.id("two_handed_swing");
	private static final Map<UUID, Swing> SWINGS = new HashMap<>();
	private static final Map<UUID, Combo> COMBOS = new HashMap<>();
	private static long now;
	/** True while a blow asks AttackEntityCallback whether a foe may be struck, so the refusal below lets it through. */
	private static boolean striking;

	private TwoHanded() {
	}

	/** A swing in the air: its wielder and arm, when it began and ends, its step of the combo and the charge at the click. */
	private static final class Swing {
		final ServerPlayer player;
		final ItemStack stack;
		final JugcraftArms.Heavy heavy;
		final long start;
		final long end;
		final int step;
		final int charge;
		boolean struck;
		boolean queued;

		Swing(ServerPlayer player, ItemStack stack, JugcraftArms.Heavy heavy, long start, long end, int step, int charge) {
			this.player = player;
			this.stack = stack;
			this.heavy = heavy;
			this.start = start;
			this.end = end;
			this.step = step;
			this.charge = charge;
		}
	}

	/** Where a player is in their combo: the last step swung and when. */
	private static final class Combo {
		int step = -1;
		long last = Long.MIN_VALUE / 2;
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TwoHandedSwingPayload.TYPE, TwoHandedSwingPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TwoHandedSwingPayload.TYPE, (payload, context) -> request(context.player()));
		ServerTickEvents.END_SERVER_TICK.register(TwoHanded::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer()));
		// Vanilla's instant hit with a two-handed arm is refused on the server, so no client can skip the swing.
		// So is any hit while busy with a weapon art (WeaponArts), whose own hits pass through here as a blow's do.
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> !level.isClientSide() && !striking
				&& entity instanceof LivingEntity && (heavy(player.getMainHandItem()) != null || WeaponArts.busy(player))
						? InteractionResult.FAIL : InteractionResult.PASS);
	}

	/** How the arm in this stack swings two-handed, or null if it is not a two-handed arm. */
	public static JugcraftArms.Heavy heavy(ItemStack stack) {
		return stack.getItem() instanceof ArmItem arm ? JugcraftArms.TWO_HANDED.get(arm.kind()) : null;
	}

	/** Whether the off hand holds something that blocks (a shield, a parrying arm), leaving no hand for the grip. */
	public static boolean offHandBusy(Player player) {
		return player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
	}

	/** Whether this player has a two-handed swing in the air. */
	public static boolean swinging(Player player) {
		return SWINGS.containsKey(player.getUUID());
	}

	/** A click: starts a swing, queues it behind one within QUEUE_TICKS of its end, or refuses it. */
	public static boolean request(ServerPlayer player) {
		Swing current = SWINGS.get(player.getUUID());
		if (current != null) {
			if (!current.queued && current.end - now <= JugcraftArms.QUEUE_TICKS) {
				current.queued = true;
				return true;
			}
			return false;
		}
		return start(player);
	}

	/** Starts a swing now, if the player may: see the class comment. */
	public static boolean start(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		JugcraftArms.Heavy heavy = heavy(stack);
		if (heavy == null || !player.isAlive() || player.isSpectator() || player.isUsingItem() || swinging(player)
				|| WeaponArts.busy(player)) {
			return false;
		}
		if (offHandBusy(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.two_handed.off_hand"));
			return false;
		}
		Combo combo = COMBOS.computeIfAbsent(player.getUUID(), id -> new Combo());
		combo.step = now - combo.last > JugcraftArms.COMBO_WINDOW ? 0 : (combo.step + 1) % heavy.combo();
		combo.last = now;
		int charge = ((AttackStrengthAccessor) player).jugcraft$attackStrengthTicker();
		player.resetAttackStrengthTicker();
		SwingAnimation animation = stack.getOrDefault(DataComponents.ATTACK_ANIMATION, SwingAnimation.DEFAULT);
		player.swing(InteractionHand.MAIN_HAND, animation, false);
		player.setSprinting(false);
		slow(player, true);
		long end = now + Math.max(heavy.strike() + 1, animation.duration());
		SWINGS.put(player.getUUID(), new Swing(player, stack, heavy, now, end, combo.step, charge));
		return true;
	}

	private static void tick(MinecraftServer server) {
		now = server.getTickCount();
		if (SWINGS.isEmpty()) {
			return;
		}
		List<ServerPlayer> next = new ArrayList<>();
		for (Iterator<Swing> it = SWINGS.values().iterator(); it.hasNext();) {
			Swing swing = it.next();
			ServerPlayer player = swing.player;
			boolean held = !player.isRemoved() && player.isAlive() && !player.isSpectator() && player.getMainHandItem() == swing.stack;
			if (held && !swing.struck && now - swing.start >= swing.heavy.strike()) {
				swing.struck = true;
				strike(player, swing.stack, swing.heavy, swing.step == swing.heavy.combo() - 1, swing.charge);
			}
			if (!held || now >= swing.end) {
				it.remove();
				slow(player, false);
				if (held && swing.queued) {
					next.add(player);
				}
			}
		}
		next.forEach(TwoHanded::start);
	}

	/**
	 * Lands a blow: every foe in the arc ({@link #foes}), each struck through Player.stabAttack at the given charge (the
	 * attack-strength ticker at the click), then the ticker put back. Returns the foes struck.
	 */
	public static List<LivingEntity> strike(ServerPlayer player, ItemStack stack, JugcraftArms.Heavy heavy, boolean finisher, int charge) {
		ServerLevel level = (ServerLevel) player.level();
		JugcraftArms.Trait trait = stack.getItem() instanceof ArmItem arm ? arm.trait() : null;
		boolean whirl = finisher && trait == JugcraftArms.Trait.WHIRL;
		// A labrys's finishing blow whirls right round.
		List<LivingEntity> foes = foes(player, stack, whirl
				? new JugcraftArms.Heavy(heavy.strike(), JugcraftArms.WHIRL_ARC, JugcraftArms.WHIRL_TARGETS, heavy.combo()) : heavy);
		DamageSource source = level.damageSources().playerAttack(player);
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		AttackStrengthAccessor strength = (AttackStrengthAccessor) player;
		int ticker = strength.jugcraft$attackStrengthTicker();
		strength.jugcraft$setAttackStrengthTicker(charge);
		List<LivingEntity> struck = new ArrayList<>();
		try {
			for (LivingEntity foe : foes) {
				float blow = base + stack.getItem().getAttackDamageBonus(foe, base, source);
				if (finisher) {
					blow *= JugcraftArms.FINISHER;
				}
				if (player.stabAttack(EquipmentSlot.MAINHAND, foe, blow, true, true, trait == JugcraftArms.Trait.HOOK)) {
					struck.add(foe);
				}
			}
		} finally {
			strength.jugcraft$setAttackStrengthTicker(ticker);
		}
		Vec3 look = player.getViewVector(1.0F);
		if (!struck.isEmpty()) {
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 1.5, player.getY(0.5), player.getZ() + look.z * 1.5,
					1, 0.0, 0.0, 0.0, 0.0);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
					1.0F, finisher ? 0.7F : 0.9F);
		}
		if (whirl) {
			for (int i = 0; i < 6; i++) {
				double angle = i * Math.PI / 3.0;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(angle) * 1.6, player.getY(0.5),
						player.getZ() + Math.sin(angle) * 1.6, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
		if (finisher && trait == JugcraftArms.Trait.QUAKE) {
			quake(level, player, base * JugcraftArms.FINISHER, new HashSet<>(struck), source);
		}
		return struck;
	}

	/**
	 * The foes a blow from this player reaches: living, not allied, not the wielder's mount or a marker stand, within the
	 * arm's reach (its attack range component), inside the arc across the wielder's view and no more than 2.5 blocks
	 * below or 1.5 above their eyes, in sight, and not protected from them (AttackEntityCallback); nearest first, at
	 * most the kind's targets.
	 */
	public static List<LivingEntity> foes(ServerPlayer player, ItemStack stack, JugcraftArms.Heavy heavy) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		double half = Math.toRadians(heavy.arc() / 2.0);
		List<LivingEntity> found = new ArrayList<>();
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0))) {
			if (!target(player, foe) || !player.isWithinAttackRange(stack, foe.getBoundingBox(), 0.0)) {
				continue;
			}
			Vec3 to = foe.getBoundingBox().getCenter().subtract(eye);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			double lookFlat = Math.sqrt(look.x * look.x + look.z * look.z);
			if (flat > 1.0E-4 && lookFlat > 1.0E-4) {
				double cos = (to.x * look.x + to.z * look.z) / (flat * lookFlat);
				double slack = Math.atan2(foe.getBbWidth() / 2.0, flat);
				if (Math.acos(Math.max(-1.0, Math.min(1.0, cos))) > half + slack) {
					continue;
				}
			}
			if (foe.getBoundingBox().maxY < eye.y - 2.5 || foe.getBoundingBox().minY > eye.y + 1.5 || !player.hasLineOfSight(foe)
					|| !allowed(player, level, foe)) {
				continue;
			}
			found.add(foe);
		}
		found.sort(Comparator.comparingDouble(player::distanceToSqr));
		return found.size() > heavy.targets() ? new ArrayList<>(found.subList(0, heavy.targets())) : found;
	}

	/** Whether a blow from this player may strike this entity at all (as vanilla's sweep, and sparing tamed pets). */
	static boolean target(ServerPlayer player, LivingEntity foe) {
		return foe != player && foe.isAlive() && !foe.isSpectator() && !player.isAlliedTo(foe) && !foe.isAlliedTo(player)
				&& !(foe instanceof ArmorStand stand && stand.isMarker()) && foe.getRootVehicle() != player.getRootVehicle();
	}

	/**
	 * Whether other code (AttackEntityCallback: the town's protection, other mods) lets this player strike this foe.
	 * The Concordance asks it too, for a spell's or a device's harm (Authority).
	 */
	public static boolean allowed(ServerPlayer player, ServerLevel level, LivingEntity foe) {
		striking = true;
		try {
			return AttackEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, foe, null) == InteractionResult.PASS;
		} finally {
			striking = false;
		}
	}

	/**
	 * A maul's finishing blow shakes the ground: every foe within QUAKE_RADIUS and a block of the wielder's footing is
	 * slowed, and those the cleave missed take QUAKE_SHARE of the blow and are thrown back.
	 */
	private static void quake(ServerLevel level, ServerPlayer player, float blow, Set<LivingEntity> struck, DamageSource source) {
		double radius = JugcraftArms.QUAKE_RADIUS;
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 1.0, radius))) {
			if (!target(player, foe) || Math.abs(foe.getY() - player.getY()) > 1.0 || foe.distanceToSqr(player) > radius * radius
					|| !allowed(player, level, foe)) {
				continue;
			}
			if (!struck.contains(foe) && foe.hurtServer(level, source, blow * JugcraftArms.QUAKE_SHARE)) {
				foe.knockback(0.5, player.getX() - foe.getX(), player.getZ() - foe.getZ(), source, blow);
			}
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, JugcraftArms.QUAKE_TICKS, JugcraftArms.QUAKE_AMPLIFIER), player);
		}
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.1, player.getZ(), 24, radius * 0.4, 0.05, radius * 0.4, 0.02);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.6F);
	}

	private static void slow(ServerPlayer player, boolean on) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		speed.removeModifier(SLOW);
		if (on) {
			speed.addTransientModifier(new AttributeModifier(SLOW, -JugcraftArms.TWO_HANDED_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void forget(ServerPlayer player) {
		if (SWINGS.remove(player.getUUID()) != null) {
			slow(player, false);
		}
		COMBOS.remove(player.getUUID());
	}
}

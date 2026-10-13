package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.rocketry.CombatRocket;
import io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeEntity;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeItem;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeLauncherItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The server's half of the guns: it alone fires, spends and loads rounds and deals damage. The client only asks
 * ({@link GunShotPayload}, {@link GunReloadPayload}) and plays the animations and sounds at once for its own player.
 * <ul>
 * <li>A shot needs a gun in the main hand, a loaded round (a creative player spends none) and the trigger's interval:
 * each player has a little credit that refills at one shot per interval, up to {@link #BURST}, so a late packet does
 * not cost a shot and a fast client gains none.</li>
 * <li>A magazine reload takes the gun's reload time and then loads what the inventory holds; a shell-at-a-time reload
 * loads one round after each shell's time, and a shot cuts it short. Switching away from the gun stops a reload.</li>
 * <li>Each bullet goes from the eye along the look, strayed by the gun's spread (less when aiming), to the first block
 * or creature in range. Creatures the shooter may not strike (allies, their own mount, marker stands, or anything
 * other code refuses through AttackEntityCallback, such as the town's protection) are passed through. A shotgun's
 * pellets on one creature land as one hit.</li>
 * <li>A gun with a bayonet fitted stabs (slice 7, {@link #stab}): a melee blow within the player's reach, at most once
 * every {@link #STAB_TICKS}.</li>
 * <li>Slice 8C, the heavy weapons ({@link JugcraftGuns#SHOTS}). A grenade gun lobs a Grenade from the eye along the
 * look, as the grenade launcher does; it bursts where it lands and breaks no block ({@link #lob}). A flame gun's burst
 * singes and sets alight every creature in a short cone ahead that the shooter may strike and the eye can see; it sets
 * no block alight ({@link #burn}). A gun whose barrels spin up fires only once its trigger has been held, the client
 * saying so each tick ({@link #spin}), for {@link JugcraftGuns#SPIN_UP} ticks unbroken; the server counts them.</li>
 * <li>An item of some ammunition loads more than one round ({@link JugcraftGuns#PER_ITEM}: a blaze powder is four of
 * the Stoker's bursts). A reload takes whole items; what of the last one does not fit is lost.</li>
 * <li>Slice 8D, the energy weapons. A reload draws each round's {@link JugcraftGuns#CHARGE} in JE from the Energy Cells
 * in the inventory, pooled, in inventory order, and leaves the cells ({@link #stocked}). A beam passes through every
 * creature in its line to the first block ({@link #beam}); an arc leaps to the creature nearest the aim within the
 * spread, then on from creature to creature ({@link #arc}). Neither breaks or lights a block. Every client that sees the
 * shooter is told where the shot went ({@link GunTracePayload}), to draw it.</li>
 * <li>Slice 9G: a grenade gun loads any grenade, one kind at a time ({@link #reloadAmmo}): the one in the other hand,
 * else the kind it holds while the inventory has one, else the first grenade in the inventory. A reload of another
 * kind first puts what the magazine held back in the inventory ({@link #load}). It lobs the kind it holds.</li>
 * <li>Slice 10A: a rocket gun fires a High-Explosive Rocket from the eye along the look, at the gun's rocket speed; it
 * flies straight and bursts where it hits or at the end of the gun's range, as the rocket launcher's rockets burst,
 * hurting living things only ({@link #rocket}).</li>
 * <li>Slice 10G, two guns at once: while the player holds a one-handed gun in each hand ({@link GunItem#dual}), the
 * other hand's gun fires and reloads too, each gun at its own rate (a trigger credit for each hand), from the hip and
 * strayed {@link JugcraftGuns#DUAL_SPREAD} times as far. One gun reloads at a time: while a magazine is out neither
 * fires, and a shot of either cuts a shell-at-a-time reload short.</li>
 * <li>Slice 11A, the Nether guns ({@link JugcraftGuns#INCENDIARY}): each creature their bullets hurt is set alight for
 * {@link #BURN_SECONDS}, as a burst of flame sets it alight. No block is set alight.</li>
 * </ul>
 */
public final class GunShots {
	/** Shots a player may bank (see the class comment). */
	static final double BURST = 2.0;
	/** How far a bullet's box test reaches past a creature's hitbox (blocks). */
	private static final double HIT_MARGIN = 0.15;
	/** Ticks between two bayonet stabs (slice 7). */
	public static final int STAB_TICKS = 12;
	/**
	 * Ticks without word from the client after which a gun's barrels have stopped spinning (slice 8C): the client says
	 * so every tick the trigger is held, so a gap this long means it was let go (a late packet or two do not count).
	 */
	static final int SPIN_GAP = 4;
	/** Ticks of a spin-up the server forgives, for packets that come unevenly. */
	static final int SPIN_SLACK = 2;
	/**
	 * Seconds a burst of flame sets a creature alight for (slice 8C; a fire aspect sword's first level is 4), and a Nether
	 * gun's bullet (slice 11A).
	 */
	public static final int BURN_SECONDS = 4;
	/** How hard a stab pushes its foe back (a sword's knockback is 0.4, and more for a sprinting strike). */
	private static final float STAB_KNOCKBACK = 0.4F;

	private static final Map<UUID, Trigger> TRIGGERS = new HashMap<>();
	/** The other hand's trigger credit (slice 10G, two guns at once). */
	private static final Map<UUID, Trigger> OFF_TRIGGERS = new HashMap<>();
	private static final Map<UUID, Reload> RELOADS = new LinkedHashMap<>();
	/** Each player's last stab (game time). */
	private static final Map<UUID, Long> STABS = new HashMap<>();
	/** Each player's spinning barrels (slice 8C). */
	private static final Map<UUID, Spin> SPINS = new HashMap<>();

	private GunShots() {
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(GunShotPayload.TYPE, GunShotPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GunReloadPayload.TYPE, GunReloadPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GunStabPayload.TYPE, GunStabPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GunSpinPayload.TYPE, GunSpinPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(GunActionPayload.TYPE, GunActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(GunTracePayload.TYPE, GunTracePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GunShotPayload.TYPE, (payload, context) -> fire(context.player(), payload.hand()));
		ServerPlayNetworking.registerGlobalReceiver(GunReloadPayload.TYPE, (payload, context) -> reload(context.player(), payload.hand()));
		ServerPlayNetworking.registerGlobalReceiver(GunStabPayload.TYPE, (payload, context) -> stab(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(GunSpinPayload.TYPE, (payload, context) -> spin(context.player()));
		ServerTickEvents.END_SERVER_TICK.register(GunShots::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer()));
	}

	/** Whether this player is reloading. */
	public static boolean reloading(ServerPlayer player) {
		return RELOADS.containsKey(player.getUUID());
	}

	/** Pulls the trigger of the gun in the player's main hand; whether it fired. */
	public static boolean fire(ServerPlayer player) {
		return fire(player, InteractionHand.MAIN_HAND);
	}

	/**
	 * Pulls the trigger of the gun in this hand; whether it fired. The other hand's gun fires only while the player holds a
	 * one-handed gun in each hand (slice 10G, {@link GunItem#dual}).
	 */
	public static boolean fire(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof GunItem gun) || !player.isAlive() || player.isSpectator()
				|| hand == InteractionHand.OFF_HAND && !GunItem.dual(player)) {
			return false;
		}
		GunSpec spec = GunItem.spec(stack);
		ServerLevel level = (ServerLevel) player.level();
		Reload loading = RELOADS.get(player.getUUID());
		if (loading != null) {
			if (!loading.spec.byShell()) {
				return false; // a magazine is out (slice 10G: of either gun; a reload takes both hands)
			}
			RELOADS.remove(player.getUUID());
			announce(player, GunActionPayload.STOP, 0, loading.hand);
		}
		boolean free = player.hasInfiniteMaterials();
		int loaded = GunItem.loaded(stack);
		if (loaded <= 0 && !free) {
			return false;
		}
		int spinUp = JugcraftGuns.spinUp(gun);
		if (spinUp > 0 && !spunUp(player, spinUp, level.getGameTime())) {
			return false; // the barrels are not up to speed
		}
		if (!spend(player, hand, spec, level.getGameTime())) {
			return false;
		}
		boolean aiming = GunItem.aiming(player, stack);
		// Slice 10G: two guns at once are fired from the hip, each straying further than one gun alone.
		float spread = aiming ? spec.aimSpread() : spec.hipSpread() * (GunItem.dual(player) ? JugcraftGuns.DUAL_SPREAD : 1.0F);
		switch (JugcraftGuns.shot(gun)) {
			case JugcraftGuns.GRENADE -> lob(level, player, stack, spec, spread);
			case JugcraftGuns.FLAME -> burn(level, player, spec, spread);
			case JugcraftGuns.BEAM -> beam(level, player, spec, spread, hand);
			case JugcraftGuns.ARC -> arc(level, player, spec, spread, hand);
			case JugcraftGuns.ROCKET -> rocket(level, player, gun, spec, spread);
			default -> shoot(level, player, gun, spec, spread);
		}
		if (!free) {
			GunItem.setLoaded(stack, loaded - 1);
		}
		player.awardStat(Stats.ITEM_USED.get(gun));
		player.gameEvent(GameEvent.PROJECTILE_SHOOT);
		Vec3 eye = player.getEyePosition();
		// The shooter's client plays its own shot; everyone else hears it here.
		level.playSound(player, eye.x, eye.y, eye.z, JugcraftGuns.sound("guns." + gun.name() + ".fire"), SoundSource.PLAYERS,
				GunItem.volume(stack), 0.95F + player.getRandom().nextFloat() * 0.1F);
		announce(player, aiming ? GunActionPayload.AIM_SHOOT : GunActionPayload.SHOOT, 0, hand);
		return true;
	}

	/**
	 * Stabs with the bayonet on the gun in the player's main hand (slice 7); whether it struck. A stab needs a bayonet
	 * fitted, no reload under way and {@link #STAB_TICKS} since the last; it reaches as far as the player's own reach,
	 * stops at a block, and strikes the nearest creature along the look that the player may strike, for the bayonet's
	 * damage as a melee blow, with a sword's push.
	 */
	public static boolean stab(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		GunAttachment bayonet = GunItem.bayonet(stack);
		if (bayonet == null || !player.isAlive() || player.isSpectator() || RELOADS.containsKey(player.getUUID())) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		Long last = STABS.get(player.getUUID());
		if (last != null && now >= last && now - last < STAB_TICKS) {
			return false;
		}
		STABS.put(player.getUUID(), now);
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)));
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		LivingEntity foe = null;
		double nearest = Double.MAX_VALUE;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0),
				candidate -> target(player, candidate))) {
			var clip = candidate.getBoundingBox().inflate(HIT_MARGIN).clip(eye, end);
			if (clip.isPresent() && clip.get().distanceToSqr(eye) < nearest) {
				nearest = clip.get().distanceToSqr(eye);
				foe = candidate;
			}
		}
		// The stabber's client plays its own stab; everyone else sees and hears it here.
		announce(player, GunActionPayload.STAB, 0);
		level.playSound(player, eye.x, eye.y, eye.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
		if (foe == null || !allowed(player, level, foe)) {
			return false;
		}
		DamageSource source = player.damageSources().playerAttack(player);
		if (foe.hurtServer(level, source, bayonet.stab())) {
			foe.knockback(STAB_KNOCKBACK, player.getX() - foe.getX(), player.getZ() - foe.getZ(), source, bayonet.stab());
		}
		level.playSound(null, foe.getX(), foe.getY(), foe.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	/**
	 * The player is holding the trigger of the gun in their main hand (slice 8C, {@link GunSpinPayload}): if its barrels
	 * spin up, they turn on. A new spin, after a gap of {@link #SPIN_GAP} ticks or more, starts the count again and is
	 * shown to the players who see this one.
	 */
	public static void spin(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof GunItem gun) || JugcraftGuns.spinUp(gun) <= 0 || !player.isAlive() || player.isSpectator()) {
			return;
		}
		long now = ((ServerLevel) player.level()).getGameTime();
		Spin spin = SPINS.get(player.getUUID());
		if (spin == null || now - spin.last > SPIN_GAP || now < spin.last) {
			spin = new Spin(now);
			SPINS.put(player.getUUID(), spin);
			announce(player, GunActionPayload.SPIN, 0);
		}
		spin.last = now;
	}

	/** Whether this player's barrels have spun for this many ticks unbroken, up to now. */
	public static boolean spunUp(ServerPlayer player, int ticks, long now) {
		Spin spin = SPINS.get(player.getUUID());
		return spin != null && now - spin.last <= SPIN_GAP && now - spin.since >= ticks - SPIN_SLACK;
	}

	/** Starts reloading the gun in the player's main hand; whether it started. */
	public static boolean reload(ServerPlayer player) {
		return reload(player, InteractionHand.MAIN_HAND);
	}

	/**
	 * Starts reloading the gun in this hand; whether it started. One gun reloads at a time, and the other hand's only while
	 * the player holds a one-handed gun in each hand (slice 10G, {@link GunItem#dual}).
	 */
	public static boolean reload(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof GunItem gun) || !player.isAlive() || player.isSpectator()
				|| RELOADS.containsKey(player.getUUID()) || hand == InteractionHand.OFF_HAND && !GunItem.dual(player)) {
			return false;
		}
		GunSpec spec = GunItem.spec(stack);
		Item ammo = reloadAmmo(player, stack, spec);
		int room = room(stack, spec, ammo, GunItem.loaded(stack));
		if (room <= 0) {
			return false;
		}
		int rounds = player.hasInfiniteMaterials() ? room : Math.min(room, stocked(player.getInventory(), gun, spec, ammo));
		if (rounds <= 0) {
			player.sendOverlayMessage(JugcraftGuns.charge(gun) > 0 ? Component.translatable("message.jugcraft.guns.no_charge")
					: JugcraftGuns.takesGrenades(spec) ? Component.translatable("message.jugcraft.guns.no_grenades")
					: Component.translatable("message.jugcraft.guns.no_ammo", Component.translatable(ammo.getDescriptionId())));
			return false;
		}
		RELOADS.put(player.getUUID(), new Reload(stack, hand, spec, ((ServerLevel) player.level()).getGameTime(), rounds, ammo));
		announce(player, GunActionPayload.RELOAD, rounds, hand);
		return true;
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Reload>> it = RELOADS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Reload> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Reload reload = entry.getValue();
			// It stops when the gun leaves its hand, or (slice 10G) the other hand's gun when there are no longer two.
			if (player == null || !player.isAlive() || player.getItemInHand(reload.hand) != reload.stack
					|| reload.hand == InteractionHand.OFF_HAND && !GunItem.dual(player)) {
				it.remove();
				if (player != null) {
					announce(player, GunActionPayload.STOP, 0, reload.hand);
				}
				continue;
			}
			long elapsed = ((ServerLevel) player.level()).getGameTime() - reload.start;
			GunSpec spec = reload.spec;
			if (spec.byShell()) {
				// One shell lands at the end of each shell's time; the gun closes after the last.
				while (reload.done < reload.rounds && elapsed >= spec.shellStart() + (long) spec.shellEach() * (reload.done + 1)) {
					if (load(player, reload.stack, spec, 1, reload.ammo) == 0) {
						reload.rounds = reload.done; // ran out (or the gun is full)
						break;
					}
					reload.done++;
				}
				if (elapsed >= spec.reloadTicks(reload.rounds)) {
					it.remove();
				}
			} else if (elapsed >= spec.reload()) {
				load(player, reload.stack, spec, reload.rounds, reload.ammo);
				it.remove();
			}
		}
	}

	/**
	 * Moves up to this many rounds of this ammunition from the inventory into the gun (any it has room for, free in
	 * creative). Ammunition an item of which loads several rounds is taken whole: the last item's rounds that do not fit
	 * are lost. An energy weapon's rounds are its cells' charge, drawn round by round (slice 8D). A grenade gun loading
	 * another kind of grenade than it holds first empties its magazine into the inventory (slice 9G; what does not fit
	 * drops at the player's feet, and in creative it is simply emptied).
	 */
	private static int load(ServerPlayer player, ItemStack stack, GunSpec spec, int wanted, Item ammo) {
		if (ammo != JugcraftGuns.loadedAmmo(stack, spec)) {
			int held = GunItem.loaded(stack);
			if (held > 0 && !player.hasInfiniteMaterials()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(JugcraftGuns.loadedAmmo(stack, spec), held), Prediction.SERVER_ONLY);
			}
			GunItem.setLoaded(stack, 0);
			JugcraftGuns.setLoadedAmmo(stack, spec, ammo);
		}
		int room = spec.capacity() - GunItem.loaded(stack);
		int rounds = Math.min(wanted, room);
		if (rounds <= 0) {
			return 0;
		}
		if (!player.hasInfiniteMaterials()) {
			int charge = JugcraftGuns.charge((GunItem) stack.getItem());
			if (charge > 0) {
				rounds = (int) Math.min(rounds, charged(player.getInventory(), ammo) / charge);
				draw(player.getInventory(), ammo, (long) rounds * charge);
			} else {
				int per = JugcraftGuns.perItem(spec);
				rounds = Math.min(rounds, take(player.getInventory(), ammo, (rounds + per - 1) / per) * per);
			}
		}
		GunItem.setLoaded(stack, GunItem.loaded(stack) + rounds);
		return rounds;
	}

	/**
	 * The ammunition a reload of this gun loads. A grenade gun takes any grenade (slice 9G): the one in the other hand;
	 * else the kind its magazine holds, while the inventory has one; else the first grenade in the inventory; and with
	 * none at all, the kind it holds. Any other gun takes its own ammunition. The client chooses the same, for its reload
	 * and its counter.
	 */
	public static Item reloadAmmo(Player player, ItemStack stack, GunSpec spec) {
		Item held = JugcraftGuns.loadedAmmo(stack, spec);
		if (!JugcraftGuns.takesGrenades(spec)) {
			return held;
		}
		if (player.getOffhandItem().getItem() instanceof GrenadeItem other) {
			return other;
		}
		Inventory inventory = player.getInventory();
		if (count(inventory, held) > 0) {
			return held;
		}
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).getItem() instanceof GrenadeItem grenade) {
				return grenade;
			}
		}
		return held;
	}

	/**
	 * Rounds a reload of this ammunition has room for, with this many loaded: all the magazine holds if it is another
	 * kind than the gun holds (slice 9G), which the reload puts back in the inventory first.
	 */
	public static int room(ItemStack stack, GunSpec spec, Item ammo, int loaded) {
		return spec.capacity() - (ammo == JugcraftGuns.loadedAmmo(stack, spec) ? loaded : 0);
	}

	/**
	 * Rounds the inventory holds for this gun: its ammunition's items, each loading {@link JugcraftGuns#perItem} rounds;
	 * or for an energy weapon (slice 8D), its Energy Cells' charge, pooled, in whole rounds of its
	 * {@link JugcraftGuns#CHARGE}. The client counts the same for its reload and its counter.
	 */
	public static int stocked(Inventory inventory, GunItem gun, GunSpec spec) {
		return stocked(inventory, gun, spec, JugcraftGuns.ammo(spec));
	}

	/** The same, of this ammunition (slice 9G: the grenade a grenade gun's reload loads, {@link #reloadAmmo}). */
	public static int stocked(Inventory inventory, GunItem gun, GunSpec spec, Item ammo) {
		int charge = JugcraftGuns.charge(gun);
		if (charge > 0) {
			return (int) Math.min(Integer.MAX_VALUE, charged(inventory, ammo) / charge);
		}
		return count(inventory, ammo) * JugcraftGuns.perItem(spec);
	}

	/** The JE the inventory's stacks of this chargeable item hold together (a cell stacks alone). */
	public static long charged(Inventory inventory, Item item) {
		long total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				total += Chargeable.energy(stack);
			}
		}
		return total;
	}

	/** Draws this much JE from the inventory's stacks of this chargeable item, emptying each in turn. */
	private static void draw(Inventory inventory, Item item, long wanted) {
		long left = wanted;
		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				long taken = Math.min(left, Chargeable.energy(stack));
				Chargeable.setEnergy(stack, Chargeable.energy(stack) - taken);
				left -= taken;
			}
		}
		if (left < wanted) {
			inventory.setChanged();
		}
	}

	/** How many of this item the inventory holds. */
	public static int count(Inventory inventory, Item item) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static int take(Inventory inventory, Item item, int wanted) {
		int taken = 0;
		for (int i = 0; i < inventory.getContainerSize() && taken < wanted; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				int n = Math.min(wanted - taken, stack.getCount());
				stack.shrink(n);
				taken += n;
			}
		}
		if (taken > 0) {
			inventory.setChanged();
		}
		return taken;
	}

	/** Spends a shot of the player's trigger credit for this hand, if they have one (see the class comment). */
	private static boolean spend(ServerPlayer player, InteractionHand hand, GunSpec spec, long now) {
		Map<UUID, Trigger> triggers = hand == InteractionHand.OFF_HAND ? OFF_TRIGGERS : TRIGGERS;
		Trigger trigger = triggers.computeIfAbsent(player.getUUID(), id -> new Trigger(now));
		trigger.credit = Math.min(BURST, trigger.credit + Math.max(0, now - trigger.last) / (double) spec.interval());
		trigger.last = now;
		if (trigger.credit < 1.0) {
			return false;
		}
		trigger.credit -= 1.0;
		return true;
	}

	/** Fires every pellet of one shot and deals what hit. */
	private static void shoot(ServerLevel level, ServerPlayer player, GunItem gun, GunSpec spec, float spread) {
		bullets(level, player, player.getEyePosition(), player.getLookAngle(), spec, spread, spec.damage(), JugcraftGuns.ignites(gun),
				candidate -> target(player, candidate), candidate -> allowed(player, level, candidate));
	}

	/**
	 * Fires every pellet of one shot from {@code eye} along {@code look}, strayed by {@code spread}, each to the first
	 * block or creature in the gun's range that {@code hittable} lets it strike (others are passed through), and deals
	 * {@code damage} a pellet to each creature it hits that {@code allowed} lets the shooter strike; a shotgun's pellets
	 * on one creature land as one hit. A player's shots and, slice 10F, a raider gunner's ({@link MobGuns}) fire this way.
	 * When {@code ignites} (slice 11A, a Nether gun), each creature the hit hurts is set alight for {@link #BURN_SECONDS}.
	 */
	static void bullets(ServerLevel level, LivingEntity shooter, Vec3 eye, Vec3 look, GunSpec spec, float spread, float damage,
			boolean ignites, Predicate<LivingEntity> hittable, Predicate<LivingEntity> allowed) {
		RandomSource random = shooter.getRandom();
		Map<LivingEntity, Float> hits = new LinkedHashMap<>();
		for (int i = 0; i < spec.pellets(); i++) {
			Vec3 direction = stray(look, spread, random);
			Vec3 end = eye.add(direction.scale(spec.range()));
			BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
			if (block.getType() != HitResult.Type.MISS) {
				end = block.getLocation();
			}
			LivingEntity target = null;
			double nearest = Double.MAX_VALUE;
			for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0), hittable)) {
				var clip = candidate.getBoundingBox().inflate(HIT_MARGIN).clip(eye, end);
				if (clip.isPresent() && clip.get().distanceToSqr(eye) < nearest) {
					nearest = clip.get().distanceToSqr(eye);
					target = candidate;
				}
			}
			if (target != null && allowed.test(target)) {
				hits.merge(target, damage, Float::sum);
				Vec3 at = eye.add(direction.scale(Math.sqrt(nearest)));
				level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.1);
			} else if (block.getType() != HitResult.Type.MISS) {
				Vec3 at = block.getLocation();
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(block.getBlockPos())), at.x, at.y,
						at.z, 4, 0.05, 0.05, 0.05, 0.1);
				level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.01);
			}
		}
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(JugcraftGuns.BULLET), shooter, shooter);
		// Guns fire faster than the half second a creature is shielded after a hit; the bullet damage type is tagged
		// minecraft:bypasses_cooldown, so each shot counts.
		hits.forEach((target, dealt) -> {
			if (target.hurtServer(level, source, dealt) && ignites) {
				target.igniteForSeconds(BURN_SECONDS);
			}
		});
		smoke(level, eye, look);
	}

	/** A shot's puff of smoke at the muzzle. */
	private static void smoke(ServerLevel level, ServerPlayer player) {
		smoke(level, player.getEyePosition(), player.getLookAngle());
	}

	private static void smoke(ServerLevel level, Vec3 eye, Vec3 look) {
		Vec3 muzzle = eye.add(look.scale(0.9)).add(0.0, -0.15, 0.0);
		level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.01);
	}

	/**
	 * Lobs one of the gun's grenades (slice 8C): a grenade from the eye along the look, as fast as the grenade launcher
	 * throws one and strayed by about the gun's spread in degrees. It goes off where it lands as its kind does
	 * ({@link GrenadeEntity}; slice 9G: the kind the magazine holds), and breaks no block.
	 */
	private static void lob(ServerLevel level, ServerPlayer player, ItemStack stack, GunSpec spec, float spread) {
		Projectile.spawnProjectileFromRotation(GrenadeEntity::new, level, new ItemStack(JugcraftGuns.loadedAmmo(stack, spec)), player,
				0.0F, GrenadeLauncherItem.LAUNCH_SPEED, spread);
		smoke(level, player);
	}

	/**
	 * Fires one of the gun's rockets (slice 10A): a High-Explosive Rocket from the eye along the look, at the gun's rocket
	 * speed ({@link JugcraftGuns#rocketSpeed}) and strayed by about the gun's spread in degrees. It flies straight, untouched
	 * by gravity, and bursts where it hits anything or, its fuse set to the gun's range, at the end of it
	 * ({@link CombatRocket}: it hurts living things only and breaks no block).
	 */
	private static void rocket(ServerLevel level, ServerPlayer player, GunItem gun, GunSpec spec, float spread) {
		float speed = JugcraftGuns.rocketSpeed(gun);
		CombatRocket rocket = Projectile.spawnProjectileFromRotation(CombatRocket::new, level, new ItemStack(JugcraftRocketry.HE_ROCKET),
				player, 0.0F, speed, spread);
		rocket.fuse(Mth.ceil(spec.range() / speed));
		smoke(level, player);
	}

	/**
	 * A burst of flame (slice 8C): every creature the shooter may strike, within the gun's range, inside the jet (the
	 * part of it nearest the jet's middle no more than the spread off the look) and with no block between it and the
	 * eye, takes the burst's damage as fire ({@link JugcraftGuns#FLAME_DAMAGE}: what fire spares, it spares) and burns
	 * for {@link #BURN_SECONDS}. No block is set alight.
	 */
	private static void burn(ServerLevel level, ServerPlayer player, GunSpec spec, float spread) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double reach = spec.range();
		double cone = Math.cos(Math.toRadians(spread));
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(JugcraftGuns.FLAME_DAMAGE), player, player);
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(reach),
				candidate -> target(player, candidate))) {
			Vec3 at = nearest(foe.getBoundingBox().inflate(HIT_MARGIN), eye, look, reach);
			Vec3 to = at.subtract(eye);
			double distance = to.length();
			if (distance > reach || distance > 1.0E-6 && to.dot(look) < cone * distance) {
				continue;
			}
			BlockHitResult block = level.clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (block.getType() != HitResult.Type.MISS || !allowed(player, level, foe)) {
				continue;
			}
			if (foe.hurtServer(level, source, spec.damage())) {
				foe.igniteForSeconds(BURN_SECONDS);
			}
		}
	}

	/**
	 * A beam (slice 8D): from the eye along the look, strayed by the spread, to the first block within range. Every creature
	 * in its line that the shooter may strike takes the shot's damage ({@link JugcraftGuns#ZAP_DAMAGE}); it passes through
	 * them all. No block is touched.
	 */
	private static void beam(ServerLevel level, ServerPlayer player, GunSpec spec, float spread, InteractionHand hand) {
		Vec3 eye = player.getEyePosition();
		Vec3 direction = stray(player.getLookAngle(), spread, player.getRandom());
		Vec3 end = eye.add(direction.scale(spec.range()));
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		DamageSource source = zap(level, player);
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0),
				candidate -> target(player, candidate))) {
			if (foe.getBoundingBox().inflate(HIT_MARGIN).clip(eye, end).isPresent() && allowed(player, level, foe)) {
				foe.hurtServer(level, source, spec.damage());
			}
		}
		trace(player, GunTracePayload.BEAM, List.of(end), hand);
	}

	/**
	 * An arc (slice 8D): it leaps to the creature nearest the aim that the shooter may strike, within the gun's range and
	 * at most the spread off the look, with no block between it and the eye (the nearer of two equally near the aim).
	 * From each creature it strikes it leaps on to the nearest other within {@link JugcraftGuns#ARC_REACH} blocks with no
	 * block between them, at most {@link JugcraftGuns#ARC_HOPS} times, each taking {@link JugcraftGuns#ARC_SHARE} of the
	 * damage before it. Finding none, it strikes the first block along the look within range, harmlessly.
	 */
	private static void arc(ServerLevel level, ServerPlayer player, GunSpec spec, float spread, InteractionHand hand) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double reach = spec.range();
		// The creatures in the cone, nearest the aim first (to a thousandth of a degree; then the nearer); the first that
		// other code lets the shooter strike is the mark (asked in turn, so no other is asked about).
		List<Map.Entry<LivingEntity, double[]>> cone = new ArrayList<>();
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(reach),
				candidate -> target(player, candidate))) {
			Vec3 at = nearest(foe.getBoundingBox().inflate(HIT_MARGIN), eye, look, reach);
			Vec3 to = at.subtract(eye);
			double distance = to.length();
			double angle = distance < 1.0E-6 ? 0.0 : Math.toDegrees(Math.acos(Mth.clamp(to.dot(look) / distance, -1.0, 1.0)));
			if (distance <= reach && angle <= spread && clear(level, player, eye, at)) {
				cone.add(Map.entry(foe, new double[] {Math.round(angle * 1000.0), distance}));
			}
		}
		cone.sort((a, b) -> a.getValue()[0] != b.getValue()[0] ? Double.compare(a.getValue()[0], b.getValue()[0])
				: Double.compare(a.getValue()[1], b.getValue()[1]));
		LivingEntity mark = cone.stream().map(Map.Entry::getKey).filter(foe -> allowed(player, level, foe)).findFirst().orElse(null);
		List<Vec3> points = new ArrayList<>();
		if (mark == null) {
			Vec3 end = eye.add(look.scale(reach));
			BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			points.add(block.getType() != HitResult.Type.MISS ? block.getLocation() : end);
			trace(player, GunTracePayload.ARC, points, hand);
			return;
		}
		DamageSource source = zap(level, player);
		Set<LivingEntity> struck = new LinkedHashSet<>();
		float damage = spec.damage();
		for (LivingEntity foe = mark; foe != null; foe = struck.size() > JugcraftGuns.ARC_HOPS ? null : leap(level, player, foe, struck)) {
			struck.add(foe);
			points.add(foe.getBoundingBox().getCenter());
			foe.hurtServer(level, source, damage);
			damage *= JugcraftGuns.ARC_SHARE;
		}
		trace(player, GunTracePayload.ARC, points, hand);
	}

	/**
	 * The creature an arc leaps to from this one: the nearest other within reach, centre to centre, with no block between
	 * them, that the shooter may strike (asked nearest first).
	 */
	private static @Nullable LivingEntity leap(ServerLevel level, ServerPlayer player, LivingEntity from, Set<LivingEntity> struck) {
		Vec3 centre = from.getBoundingBox().getCenter();
		double reach = JugcraftGuns.ARC_REACH * JugcraftGuns.ARC_REACH;
		List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
				from.getBoundingBox().inflate(JugcraftGuns.ARC_REACH), candidate -> target(player, candidate) && !struck.contains(candidate)
						&& candidate.getBoundingBox().getCenter().distanceToSqr(centre) <= reach));
		near.sort((a, b) -> Double.compare(a.getBoundingBox().getCenter().distanceToSqr(centre), b.getBoundingBox().getCenter().distanceToSqr(centre)));
		for (LivingEntity foe : near) {
			if (clear(level, player, centre, foe.getBoundingBox().getCenter()) && allowed(player, level, foe)) {
				return foe;
			}
		}
		return null;
	}

	/** Whether no block stands between these two points. */
	private static boolean clear(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
	}

	/** The energy weapons' damage, dealt by this player (slice 8D). */
	private static DamageSource zap(ServerLevel level, ServerPlayer player) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(JugcraftGuns.ZAP_DAMAGE), player,
				player);
	}

	/**
	 * Tells the shooter's client and every client that sees them where an energy weapon's shot went (slice 8D), and from
	 * which hand's gun (slice 10G).
	 */
	private static void trace(ServerPlayer player, int kind, List<Vec3> points, InteractionHand hand) {
		GunTracePayload payload = new GunTracePayload(player.getId(), kind, List.copyOf(points), hand == InteractionHand.OFF_HAND);
		Set<ServerPlayer> watchers = new LinkedHashSet<>(PlayerLookup.tracking(player));
		watchers.add(player);
		for (ServerPlayer watcher : watchers) {
			if (ServerPlayNetworking.canSend(watcher, GunTracePayload.TYPE)) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
	}

	/** The point of this box nearest the jet's middle: the box's point nearest to where the jet passes its centre. */
	static Vec3 nearest(AABB box, Vec3 eye, Vec3 look, double reach) {
		double along = Mth.clamp(box.getCenter().subtract(eye).dot(look), 0.0, reach);
		Vec3 onJet = eye.add(look.scale(along));
		return new Vec3(Mth.clamp(onJet.x, box.minX, box.maxX), Mth.clamp(onJet.y, box.minY, box.maxY),
				Mth.clamp(onJet.z, box.minZ, box.maxZ));
	}

	/** A direction at most {@code degrees} off {@code look}, evenly over the cone's face. */
	static Vec3 stray(Vec3 look, float degrees, RandomSource random) {
		if (degrees <= 0) {
			return look;
		}
		double angle = Math.toRadians(degrees) * Math.sqrt(random.nextDouble());
		double around = random.nextDouble() * Math.PI * 2;
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1.0E-6) {
			right = new Vec3(1, 0, 0);
		}
		right = right.normalize();
		Vec3 up = right.cross(look).normalize();
		Vec3 off = right.scale(Mth.cos((float) around)).add(up.scale(Mth.sin((float) around)));
		return look.scale(Math.cos(angle)).add(off.scale(Math.sin(angle))).normalize();
	}

	/** Creatures a bullet may hit (as weapons/TwoHanded's foes). */
	static boolean target(ServerPlayer player, LivingEntity foe) {
		return foe != player && foe.isAlive() && !foe.isSpectator() && !player.isAlliedTo(foe) && !foe.isAlliedTo(player)
				&& !(foe instanceof ArmorStand stand && stand.isMarker()) && foe.getRootVehicle() != player.getRootVehicle();
	}

	/** Whether other code (AttackEntityCallback: the town's protection, other mods) lets this player strike this foe. */
	static boolean allowed(ServerPlayer player, ServerLevel level, LivingEntity foe) {
		return AttackEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, foe, null) == InteractionResult.PASS;
	}

	/** Tells the clients that see this player (not the player's own, which has played it already). */
	private static void announce(ServerPlayer player, int action, int rounds) {
		announce(player, action, rounds, InteractionHand.MAIN_HAND);
	}

	/** The same, of the gun in this hand (slice 10G). */
	private static void announce(ServerPlayer player, int action, int rounds, InteractionHand hand) {
		GunActionPayload payload = new GunActionPayload(player.getId(), action, rounds, hand);
		for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
			if (watcher != player && ServerPlayNetworking.canSend(watcher, GunActionPayload.TYPE)) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
	}

	private static void forget(ServerPlayer player) {
		TRIGGERS.remove(player.getUUID());
		OFF_TRIGGERS.remove(player.getUUID());
		RELOADS.remove(player.getUUID());
		STABS.remove(player.getUUID());
		SPINS.remove(player.getUUID());
	}

	private static final class Trigger {
		double credit = BURST;
		long last;

		Trigger(long now) {
			last = now;
		}
	}

	/** A run of spinning (slice 8C): when it began and when the client last said the trigger was held. */
	private static final class Spin {
		final long since;
		long last;

		Spin(long now) {
			since = now;
			last = now;
		}
	}

	private static final class Reload {
		final ItemStack stack;
		/** The hand the gun is in (slice 10G: the other hand's, with a gun in each). */
		final InteractionHand hand;
		final GunSpec spec;
		final long start;
		/** What it loads (slice 9G: a grenade gun's kind of grenade). */
		final Item ammo;
		int rounds;
		int done;

		Reload(ItemStack stack, InteractionHand hand, GunSpec spec, long start, int rounds, Item ammo) {
			this.stack = stack;
			this.hand = hand;
			this.spec = spec;
			this.start = start;
			this.rounds = rounds;
			this.ammo = ammo;
		}
	}
}

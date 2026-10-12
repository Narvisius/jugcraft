package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoItemRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.guns.GunAnimations;
import io.github.jimbozoomer.jugcraft.client.guns.CellRackRenderer;
import io.github.jimbozoomer.jugcraft.client.guns.GunCasingParticle;
import io.github.jimbozoomer.jugcraft.client.guns.GunEffects;
import io.github.jimbozoomer.jugcraft.client.guns.GunLaser;
import io.github.jimbozoomer.jugcraft.client.guns.GunLaserParticle;
import io.github.jimbozoomer.jugcraft.client.guns.GunRenderer;
import io.github.jimbozoomer.jugcraft.client.guns.GunScope;
import io.github.jimbozoomer.jugcraft.client.guns.GunView;
import io.github.jimbozoomer.jugcraft.guns.GunActionPayload;
import io.github.jimbozoomer.jugcraft.guns.GunHooks;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunReloadPayload;
import io.github.jimbozoomer.jugcraft.guns.GunShotPayload;
import io.github.jimbozoomer.jugcraft.guns.GunSpinPayload;
import io.github.jimbozoomer.jugcraft.guns.GunStabPayload;
import io.github.jimbozoomer.jugcraft.guns.GunTracePayload;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The client's half of the guns (the server's is guns/GunShots): input, what the player sees and hears at once, and the
 * ammunition counter.
 * <ul>
 * <li>With a gun in the main hand the attack button is the trigger (it never mines or punches): a press fires a
 * single-shot gun, holding fires an automatic at its rate. Right click (held) aims. G reloads and H inspects (not R
 * and I, which Iris, in the pack, keeps for its shaders).</li>
 * <li>Each shot asks the server and at once plays the shot, its sound and a little kick of the view here; the server
 * may still refuse (it decides). The counter allows for shots the server has not answered yet.</li>
 * <li>Switching to a gun plays its draw, and it cannot fire until the draw is done.</li>
 * <li>Other players' guns are animated from the server's {@link GunActionPayload}.</li>
 * <li>Every shot, the player's own and others', shows its muzzle flash and black powder's smoke ({@link GunEffects}).</li>
 * <li>V stabs with a fitted bayonet (slice 7): the thrust and its swish at once, the blow from the server.</li>
 * <li>Aiming through a fitted scope shows the view through it, or a reflex sight's dot ({@link GunScope}).</li>
 * <li>Slice 8C: holding the trigger of a rotary gun spins its barrels up, telling the server each tick
 * ({@link GunSpinPayload}); it fires once they have spun for its spin-up, and they run down when it is let go.</li>
 * <li>Slice 8D: an energy weapon's shots are drawn where the server says they went ({@link GunTracePayload}); its
 * counter gives the shots its Energy Cells' charge holds.</li>
 * <li>Slice 10G, two guns at once: with a one-handed gun in each hand ({@link GunItem#dual}), right click is the other
 * gun's trigger (GunItem hands it the click, {@link GunHooks#offTrigger}): a press fires it, holding fires an automatic
 * at its rate. Each gun keeps its own rate, draw and counter, the other's beyond its slot on its side of the hotbar. G
 * reloads the main hand's gun, or the other's when the main one has no room or nothing to load; one gun reloads at a
 * time; H inspects both.</li>
 * </ul>
 */
public final class GunsClient {
	/** Ticks a drawn gun takes before it fires. */
	static final int DRAW_TICKS = 10;
	/** Ticks without a shot after which unanswered shots are written off. */
	private static final int IN_FLIGHT_TICKS = 10;
	/** The off-hand slot vanilla draws beside the hotbar, in GUI pixels (slice 10G: the other gun's counter is beyond it). */
	private static final int OFF_SLOT = 29;
	private static KeyMapping reloadKey;
	private static KeyMapping inspectKey;
	private static KeyMapping stabKey;
	private static long nextStab;
	/** When the reload under way ends (one gun reloads at a time), whether it loads a shell at a time, and whose it is. */
	private static long reloadingUntil;
	private static boolean reloadByShell;
	private static InteractionHand reloadingHand = InteractionHand.MAIN_HAND;
	/** Each hand's gun (slice 10G: with a one-handed gun in each, both fire). */
	private static final Held MAIN = new Held(InteractionHand.MAIN_HAND);
	private static final Held OFF = new Held(InteractionHand.OFF_HAND);
	private static boolean triggerHeld;
	/**
	 * Slice 10G: whether right click was down at the last tick's end (vanilla hands a held click on every few ticks, which
	 * pulls nothing), and whether this hold pulled the other gun's trigger (an automatic then fires while it lasts).
	 */
	private static boolean useHeld;
	private static boolean offPulling;
	/** When the trigger of the rotary gun in hand was pulled, and the tick it was last held (slice 8C); -1 when let go. */
	private static long spinSince = -1;
	private static long spunAt = -1;
	/** Each gun's GeoRenderProvider, made once. */
	private static final Map<GunItem, GeoRenderProvider> PROVIDERS = new HashMap<>();

	private GunsClient() {
	}

	public static void register() {
		GunHooks.controllers = GunAnimations::register;
		GunHooks.renderer = gun -> PROVIDERS.computeIfAbsent(gun, key -> new GeoRenderProvider() {
			private @Nullable GunRenderer renderer;

			@Override
			public GeoItemRenderer<?> getGeoItemRenderer() {
				if (renderer == null) {
					renderer = new GunRenderer(key);
				}
				return renderer;
			}
		});
		GunHooks.offTrigger = GunsClient::offTrigger;
		reloadKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.reload", InputConstants.KEY_G, PartyClient.CATEGORY));
		inspectKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.inspect", InputConstants.KEY_H, PartyClient.CATEGORY));
		stabKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.stab", InputConstants.KEY_V, PartyClient.CATEGORY));
		ClientPreAttackCallback.EVENT.register(GunsClient::attack);
		ClientTickEvents.END_CLIENT_TICK.register(GunsClient::tick);
		ClientPlayNetworking.registerGlobalReceiver(GunActionPayload.TYPE, (payload, context) -> receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(GunTracePayload.TYPE, (payload, context) -> GunEffects.trace(payload));
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Jugcraft.id("gun_ammo"), GunsClient::hud);
		GunScope.register();
		JugcraftGuns.CASINGS.values().forEach(casing -> ParticleProviderRegistry.getInstance().register(casing, GunCasingParticle::provider));
		ParticleProviderRegistry.getInstance().register(JugcraftGuns.LASER_DOT, GunLaserParticle::provider);
		ClientTickEvents.END_CLIENT_TICK.register(GunLaser::tick);
		// Slice 10E: the cells standing in a Cell Rack.
		BlockEntityRendererRegistry.register(JugcraftGuns.CELL_RACK_ENTITY, CellRackRenderer::new);
	}

	/** The reload key (for the client game tests). */
	public static KeyMapping reloadKey() {
		return reloadKey;
	}

	/** The inspect key (for the client game tests). */
	public static KeyMapping inspectKey() {
		return inspectKey;
	}

	/** The bayonet stab key (for the client game tests). */
	public static KeyMapping stabKey() {
		return stabKey;
	}

	/** The attack button, with a gun in the main hand, is the trigger. */
	private static boolean attack(Minecraft client, LocalPlayer player, int clicks) {
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof GunItem gun) || player.isSpectator()) {
			return false;
		}
		boolean fresh = clicks > 0 && !triggerHeld;
		triggerHeld = true;
		int spinUp = JugcraftGuns.spinUp(gun);
		if (spinUp > 0 && client.level != null && !spin(client.level.getGameTime(), player, spinUp)
				&& !(fresh && predicted(MAIN, stack) <= 0)) {
			return true; // the barrels are still spinning up (an empty gun still clicks at once)
		}
		if (fresh || gun.spec().auto()) {
			pull(client, player, MAIN, stack, gun, fresh);
		}
		return true;
	}

	/**
	 * Slice 10G: right click reached the gun in the other hand, with a one-handed gun in each (GunItem.use, as vanilla
	 * hands the click on). A fresh press pulls its trigger; vanilla hands a held click on again every few ticks, which
	 * pulls nothing, but an automatic fires at its rate for as long as the button is held ({@link #tick}).
	 */
	private static void offTrigger() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || player.isSpectator() || !GunItem.dual(player)) {
			return;
		}
		ItemStack stack = player.getOffhandItem();
		boolean fresh = !useHeld;
		useHeld = true;
		offPulling = true;
		if (fresh && stack.getItem() instanceof GunItem gun) {
			pull(client, player, OFF, stack, gun, true);
		}
	}

	/**
	 * The rotary gun's trigger is held this tick: its barrels turn (here at once, and the server is told once a tick);
	 * whether they have spun for its spin-up.
	 */
	private static boolean spin(long now, LocalPlayer player, int spinUp) {
		if (spunAt == now) {
			return now - spinSince >= spinUp;
		}
		if (spinSince < 0 || now - spunAt > 1 || now < spunAt) {
			spinSince = now;
		}
		spunAt = now;
		ClientPlayNetworking.send(GunSpinPayload.INSTANCE);
		GunEffects.spinning(player);
		return now - spinSince >= spinUp;
	}

	private static void pull(Minecraft client, LocalPlayer player, Held held, ItemStack stack, GunItem gun, boolean fresh) {
		if (client.level == null) {
			return;
		}
		GunSpec spec = GunItem.spec(stack);
		long now = client.level.getGameTime();
		if (now < held.nextShot || now < held.readyAt || now < reloadingUntil && !reloadByShell) {
			return;
		}
		if (predicted(held, stack) <= 0 && !player.hasInfiniteMaterials()) {
			if (fresh) {
				local(player, "dry_fire");
				held.nextShot = now + spec.interval();
				// An empty gun reloads itself on a fresh pull, when there is something to load.
				reload(client, player, held.hand);
			}
			return;
		}
		ClientPlayNetworking.send(GunShotPayload.of(held.hand));
		held.nextShot = now + spec.interval();
		held.lastShot = now;
		if (!player.hasInfiniteMaterials()) {
			held.inFlight++; // creative spends nothing, so the server never settles it
		}
		if (now < reloadingUntil && reloadingHand != held.hand) {
			// A shot cuts a shell-at-a-time reload short, the other gun's too (slice 10G): that gun stops loading.
			GunAnimations.trigger(player, reloadingHand, "idle");
		}
		reloadingUntil = 0;
		boolean aiming = GunItem.aiming(player, stack);
		GunAnimations.trigger(player, held.hand, aiming ? "aim_shoot" : "shoot");
		GunEffects.shot(player, held.hand);
		player.level().playLocalSound(player.getX(), player.getEyeY(), player.getZ(), JugcraftGuns.sound("guns." + gun.name() + ".fire"),
				SoundSource.PLAYERS, GunItem.volume(stack), 0.95F + player.getRandom().nextFloat() * 0.1F, false);
		// A little kick of the view: more for a heavier shot, less when aimed or with a brake, stock or grip.
		float kick = (0.5F + 0.125F * spec.damage() * spec.pellets()) * (aiming ? 0.6F : 1.0F) * GunItem.kick(stack);
		player.setXRot(player.getXRot() - kick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		GunView.tick(player);
		if (player == null || client.level == null) {
			return;
		}
		long now = client.level.getGameTime();
		if (!client.options.keyAttack.isDown()) {
			triggerHeld = false;
			spinSince = -1;
		}
		useHeld = client.options.keyUse.isDown();
		if (!useHeld) {
			offPulling = false;
		}
		draw(player, MAIN, now);
		draw(player, OFF, now);
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof GunItem)) {
			while (reloadKey.consumeClick()) {
				// nothing to reload
			}
			while (inspectKey.consumeClick()) {
				// nothing to inspect
			}
			while (stabKey.consumeClick()) {
				// nothing to stab with
			}
			return;
		}
		for (Held held : new Held[] {MAIN, OFF}) {
			if (now - held.lastShot > IN_FLIGHT_TICKS) {
				held.inFlight = 0;
			}
		}
		boolean dual = GunItem.dual(player);
		while (reloadKey.consumeClick()) {
			// Slice 10G: with a gun in each hand, the other's when the main one has no room or nothing to load.
			reload(client, player, dual && !canReload(player, MAIN) && canReload(player, OFF) ? InteractionHand.OFF_HAND
					: InteractionHand.MAIN_HAND);
		}
		while (inspectKey.consumeClick()) {
			if (now >= reloadingUntil && now >= MAIN.readyAt) {
				GunAnimations.trigger(player, InteractionHand.MAIN_HAND, "inspect");
				if (dual && now >= OFF.readyAt) {
					GunAnimations.trigger(player, InteractionHand.OFF_HAND, "inspect");
				}
			}
		}
		while (stabKey.consumeClick()) {
			stab(client, player, now);
		}
		// Slice 10G: an automatic in the other hand fires at its rate while right click, which pulled it, is held.
		ItemStack off = player.getOffhandItem();
		if (offPulling && dual && off.getItem() instanceof GunItem gun && GunItem.spec(off).auto()) {
			pull(client, player, OFF, off, gun, false);
		}
	}

	/** Switching to a gun in this hand plays its draw, and it cannot fire until the draw is done. */
	private static void draw(LocalPlayer player, Held held, long now) {
		ItemStack stack = player.getItemInHand(held.hand);
		long id = stack.getItem() instanceof GunItem ? GeoItem.getId(stack) : Long.MIN_VALUE;
		if (id == held.id) {
			return;
		}
		held.id = id;
		held.inFlight = 0;
		held.lastSynced = -1;
		if (held == MAIN) {
			spinSince = -1;
		}
		if (reloadingHand == held.hand) {
			reloadingUntil = 0; // the gun that was reloading has left the hand
		}
		if (stack.getItem() instanceof GunItem) {
			GunAnimations.trigger(player, held.hand, "draw");
			held.readyAt = now + DRAW_TICKS;
		}
	}

	/** Stabs with the gun's bayonet, if it has one: asks the server, and plays the thrust and its swish here at once. */
	private static void stab(Minecraft client, LocalPlayer player, long now) {
		if (GunItem.bayonet(player.getMainHandItem()) == null || now < nextStab || now < MAIN.readyAt || now < reloadingUntil) {
			return;
		}
		ClientPlayNetworking.send(GunStabPayload.INSTANCE);
		nextStab = now + GunShots.STAB_TICKS;
		GunEffects.stabbed(player);
		player.level().playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
				0.8F, 1.3F, false);
	}

	/** Whether the gun in this hand has room for a reload and something to load (slice 10G: which gun G reloads). */
	private static boolean canReload(LocalPlayer player, Held held) {
		ItemStack stack = player.getItemInHand(held.hand);
		if (!(stack.getItem() instanceof GunItem gun)) {
			return false;
		}
		GunSpec spec = GunItem.spec(stack);
		Item ammo = GunShots.reloadAmmo(player, stack, spec);
		return GunShots.room(stack, spec, ammo, predicted(held, stack)) > 0
				&& (player.hasInfiniteMaterials() || GunShots.stocked(player.getInventory(), gun, spec, ammo) > 0);
	}

	/**
	 * Asks for a reload of the gun in this hand (the other hand's only with a gun in each, slice 10G), if there is room and
	 * something to load, and plays it here.
	 */
	private static void reload(Minecraft client, LocalPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof GunItem gun) || client.level == null
				|| hand == InteractionHand.OFF_HAND && !GunItem.dual(player)) {
			return;
		}
		long now = client.level.getGameTime();
		GunSpec spec = GunItem.spec(stack);
		// Slice 9G: the ammunition the server's reload will choose (a grenade gun's kind of grenade).
		Item ammo = GunShots.reloadAmmo(player, stack, spec);
		int room = GunShots.room(stack, spec, ammo, predicted(held(hand), stack));
		int rounds = player.hasInfiniteMaterials() ? room : Math.min(room, GunShots.stocked(player.getInventory(), gun, spec, ammo));
		if (now < reloadingUntil || room <= 0) {
			return;
		}
		ClientPlayNetworking.send(GunReloadPayload.of(hand));
		if (rounds > 0) {
			GunAnimations.trigger(player, hand, GunAnimations.reloadName(spec, rounds));
			reloadingUntil = now + spec.reloadTicks(rounds);
			reloadByShell = spec.byShell();
			reloadingHand = hand;
		}
	}

	private static Held held(InteractionHand hand) {
		return hand == InteractionHand.OFF_HAND ? OFF : MAIN;
	}

	/** Rounds loaded in this hand's gun, less shots sent that the server has not answered yet. */
	private static int predicted(Held held, ItemStack stack) {
		int synced = GunItem.loaded(stack);
		if (held.lastSynced >= 0 && synced < held.lastSynced) {
			held.inFlight = Math.max(0, held.inFlight - (held.lastSynced - synced));
		} else if (synced > held.lastSynced) {
			held.inFlight = 0;
		}
		held.lastSynced = synced;
		return synced - held.inFlight;
	}

	private static void local(LocalPlayer player, String event) {
		player.level().playLocalSound(player.getX(), player.getEyeY(), player.getZ(), JugcraftGuns.sound("guns." + event),
				SoundSource.PLAYERS, 0.8F, 1.0F, false);
	}

	/** Another player fired or began to reload: play it on their gun (slice 10G: the one in the hand the server names). */
	private static void receive(GunActionPayload payload) {
		Minecraft client = Minecraft.getInstance();
		Entity entity = client.level == null ? null : client.level.getEntity(payload.entity());
		InteractionHand hand = payload.hand();
		if (!(entity instanceof LivingEntity holder) || !(holder.getItemInHand(hand).getItem() instanceof GunItem gun)) {
			return;
		}
		switch (payload.action()) {
			case GunActionPayload.SHOOT -> {
				GunAnimations.trigger(holder, hand, "shoot");
				GunEffects.shot(holder, hand);
			}
			case GunActionPayload.AIM_SHOOT -> {
				GunAnimations.trigger(holder, hand, "aim_shoot");
				GunEffects.shot(holder, hand);
			}
			case GunActionPayload.RELOAD -> GunAnimations.trigger(holder, hand, GunAnimations.reloadName(gun.spec(), payload.rounds()));
			case GunActionPayload.STAB -> GunEffects.stabbed(holder);
			case GunActionPayload.SPIN -> GunEffects.spun(holder, JugcraftGuns.spinUp(gun));
			default -> GunAnimations.trigger(holder, hand, "idle");
		}
	}

	/**
	 * The counter above the hotbar's right end: loaded / capacity, and the rounds to hand (or "Reloading"); for an energy
	 * weapon, the shots its Energy Cells' charge holds; for a grenade gun, the grenades of the kind its reload would load
	 * (slice 9G). Slice 10G: with a gun in each hand, each gun's counter is on its own side of the hotbar, the other
	 * hand's beyond its slot.
	 */
	private static void hud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.gui.hud.isHidden() || client.level == null
				|| !(player.getMainHandItem().getItem() instanceof GunItem)) {
			return;
		}
		int middle = graphics.guiWidth() / 2;
		int bottom = graphics.guiHeight() - 22;
		if (!GunItem.dual(player)) {
			counter(graphics, client, player, MAIN, middle + 91 + 8, bottom, false);
			return;
		}
		// Vanilla draws the off-hand slot on the off arm's side: the other gun's counter goes beyond it, the main one's on
		// the main arm's side.
		if (player.getMainArm() == HumanoidArm.RIGHT) {
			counter(graphics, client, player, MAIN, middle + 91 + 8, bottom, false);
			counter(graphics, client, player, OFF, middle - 91 - OFF_SLOT - 8, bottom, true);
		} else {
			counter(graphics, client, player, MAIN, middle - 91 - 8, bottom, true);
			counter(graphics, client, player, OFF, middle + 91 + OFF_SLOT + 8, bottom, false);
		}
	}

	/** One gun's counter, from x, or ending at x. */
	private static void counter(GuiGraphicsExtractor graphics, Minecraft client, LocalPlayer player, Held held, int x, int bottom,
			boolean endingAt) {
		ItemStack stack = player.getItemInHand(held.hand);
		if (!(stack.getItem() instanceof GunItem gun) || client.level == null) {
			return;
		}
		GunSpec spec = GunItem.spec(stack);
		// The rounds loaded less the shots the server has not answered yet (each answer settles one).
		int loaded = Math.max(0, predicted(held, stack));
		Component count = Component.translatable("hud.jugcraft.guns.ammo", loaded, spec.capacity());
		Item toLoad = GunShots.reloadAmmo(player, stack, spec);
		Component below = client.level.getGameTime() < reloadingUntil && reloadingHand == held.hand
				? Component.translatable("hud.jugcraft.guns.reloading")
				: JugcraftGuns.charge(gun) > 0 ? Component.translatable("hud.jugcraft.guns.in_cells", GunShots.stocked(player.getInventory(), gun, spec))
				: Component.literal(GunShots.count(player.getInventory(), toLoad) + " ").append(Component.translatable(toLoad.getDescriptionId()));
		graphics.text(client.font, count, endingAt ? x - client.font.width(count) : x, bottom, loaded == 0 ? 0xFFFF5555 : 0xFFFFFFFF, true);
		graphics.text(client.font, below, endingAt ? x - client.font.width(below) : x, bottom + 10, 0xFFC8C8C8, true);
	}

	/**
	 * What this client knows of the gun in one hand: when it may next fire, when its draw is done, the shots sent that the
	 * server has not answered, and which gun it is (its animation id). Slice 10G: with a gun in each hand, both fire.
	 */
	private static final class Held {
		final InteractionHand hand;
		long nextShot;
		long readyAt;
		long lastShot;
		int inFlight;
		int lastSynced = -1;
		long id = Long.MIN_VALUE;

		Held(InteractionHand hand) {
			this.hand = hand;
		}
	}
}

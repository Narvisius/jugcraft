package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The NT-7 in the player's own hands (P6, take 3 of rifle A, Justin, 9 November 2026: "a good first person view using
 * it", "the holographic sight ... accurately lined up with where shots land"; his picks: it works like the guns, his own
 * arms hold it).
 * <ul>
 * <li>The attack button is the trigger: held, it fires each time the rifle is ready (the server decides,
 * {@link PlasmaRifleItem#trigger}); it never mines or punches. Each shot kicks the rifle back a little.</li>
 * <li>Right click held aims down the sight (the item is used: {@link PlasmaRifleItem}), eased in and out over
 * {@value #EASE_TICKS} ticks: the rifle slides from the hip until the sight's rear window is on the middle of the screen,
 * the view narrows to {@value #ZOOM} of itself (as the guns' reflex sight does), and near full aim the holographic
 * reticle is drawn on the middle of the screen in place of the crosshair ({@link #hud}): where the bolt lands.</li>
 * <li>First person ({@link #firstPerson}, from PlasmaRifleHandsMixin, for the rifle in either hand): placed from the
 * rifle's own numbers (tools/plasma_rifle_nt7.py: the item's centre is the grip's top, its pixels the rifle's), low on the
 * right at the hip, the player's own arms on it (their skin and sleeves, wide or slim): the right hand on the grip, the
 * left on the fore grip, as the guns draw them, smaller as the aim comes in so the sight stays clear. A left-handed
 * player holds it on the left. In the other hand it's drawn at that side's hip, without arms.</li>
 * </ul>
 * Placing: in the view (the eye at the origin looking down -z, blocks) a point r of the rifle (its pixels: x its left, y
 * up, z to the muzzle) is at T + (-r.x, r.y, -r.z) / 16, T eased from {@link #HIP} to {@link #AIMED}. Aimed, the rear
 * window's middle (the reticle's centre) is on the view's axis, {@value #EYE_BACK} pixels ahead of the eye.
 */
public final class PlasmaRifleClient {
	public static final int EASE_TICKS = 4;
	/** How far the view narrows at full aim (as the guns' reflex sight). */
	public static final float ZOOM = 0.85F;
	/** How far into the aim the reticle replaces the crosshair. */
	public static final float RETICLE_AT = 0.9F;
	/** The sight (the rifle's pixels): the rear window's middle, the front window, and the eye's distance behind it, aimed. */
	public static final float SIGHT_Y = 4.21F, SIGHT_Z = -0.5F, FRONT_Z = 0.88F, EYE_BACK = 2.8F;
	/** The rifle's origin (the grip's top) in the view, blocks: at the hip, and aimed. */
	public static final Vector3f HIP = new Vector3f(9.5F / 16.0F, -5.5F / 16.0F, -12.0F / 16.0F);
	public static final Vector3f AIMED = new Vector3f(0.0F, -SIGHT_Y / 16.0F, -(EYE_BACK - SIGHT_Z) / 16.0F);
	/** The hands (the rifle's pixels) and the way each arm runs to its shoulder from there: right on the grip, left on the fore grip. */
	private static final Vector3f RIGHT_HAND = new Vector3f(0.0F, -1.2F, 0.1F), RIGHT_SHOULDER = new Vector3f(-4.5F, -14.0F, -10.0F);
	private static final Vector3f LEFT_HAND = new Vector3f(0.0F, -1.7F, 6.3F), LEFT_SHOULDER = new Vector3f(10.0F, -14.0F, 0.0F);
	/** The arms' size at the hip and at full aim (as the guns', slice 9F). */
	private static final float ARM_HIP = 0.8F, ARM_AIMED = 0.5F;
	/** A shot's kick: back (pixels) and up (degrees) at its peak, and what's left of it each tick. */
	private static final float KICK_BACK = 1.2F, KICK_UP = 4.0F, KICK_DECAY = 0.55F;
	/** The reticle's ring is this far out on its texture (its radius, a fraction of the side); its size on the screen is
	 * 0.6 of the front window's half-width, as the hand's view (70 degrees, unzoomed) shows it from the eye. */
	private static final float RETICLE_RING = 0.30F, RETICLE_OF_WINDOW = 0.6F, WINDOW_HALF = 0.46F;
	private static final Identifier RETICLE = Jugcraft.id("textures/gui/plasma_reticle.png");

	private static float previous, current;
	private static float kickPrevious, kick;
	private static long nextShot;
	private static ModelPart[] arms, slimArms;
	/** Frames drawn in first person, and frames the reticle was drawn (for the client game tests). */
	private static long frames, reticles;

	private PlasmaRifleClient() {
	}

	public static void register() {
		PlasmaCharge.register();
		ClientPreAttackCallback.EVENT.register(PlasmaRifleClient::attack);
		ClientTickEvents.END_CLIENT_TICK.register(PlasmaRifleClient::tick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Jugcraft.id("plasma_reticle"), PlasmaRifleClient::hud);
	}

	/** The attack button, with the rifle in the main hand, is its trigger. */
	private static boolean attack(Minecraft client, LocalPlayer player, int clicks) {
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof PlasmaRifleItem) || player.isSpectator()) {
			return false;
		}
		long now = client.level == null ? 0 : client.level.getGameTime();
		if (!player.getCooldowns().isOnCooldown(stack) && now >= nextShot) {
			ClientPlayNetworking.send(PlasmaRifleItem.TriggerPayload.INSTANCE);
			nextShot = now + PlasmaRifle.COOLDOWN;
			kick = 1.0F;
		}
		return true;
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		previous = current;
		boolean aiming = player != null && PlasmaRifleItem.aiming(player);
		current = Mth.clamp(current + (aiming ? 1.0F : -1.0F) / EASE_TICKS, 0.0F, 1.0F);
		kickPrevious = kick;
		kick *= KICK_DECAY;
		if (kick < 0.01F) {
			kick = 0.0F;
		}
	}

	/** How far into the aim, eased. */
	public static float aim(float partialTick) {
		float t = Mth.lerp(partialTick, previous, current);
		return t * t * (3.0F - 2.0F * t);
	}

	/** The view's field of view modifier narrowed while aiming down the rifle's sight (GunView.fov hands it here). */
	public static float fov(float modifier) {
		return modifier * Mth.lerp(aim(1.0F), 1.0F, ZOOM);
	}

	/** Whether the reticle is drawn now (the crosshair is then left out: GunScope). */
	public static boolean reticle(float partialTick) {
		Minecraft client = Minecraft.getInstance();
		return client.player != null && client.options.getCameraType().isFirstPerson() && client.player.getMainHandItem().getItem() instanceof PlasmaRifleItem
				&& aim(partialTick) >= RETICLE_AT;
	}

	/** The reticle on the middle of the screen, the size the front window shows it at (the hand's view is 70 degrees, unzoomed). */
	private static void hud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (!reticle(delta.getGameTimeDeltaPartialTick(false))) {
			return;
		}
		float tan = RETICLE_OF_WINDOW * WINDOW_HALF / (FRONT_Z - (SIGHT_Z - EYE_BACK));
		float ring = tan / (float) Math.tan(Math.toRadians(35.0)) * graphics.guiHeight() / 2.0F;
		int side = Math.max(8, Math.round(ring / RETICLE_RING));
		graphics.blit(RenderPipelines.GUI_TEXTURED, RETICLE, (graphics.guiWidth() - side) / 2, (graphics.guiHeight() - side) / 2, 0.0F, 0.0F,
				side, side, side, side);
		reticles++;
	}

	/**
	 * Draws the rifle in first person for this hand, with the player's arms for the main hand; returns false to leave it to
	 * vanilla (never, now: kept for a hand this can't draw).
	 */
	public static boolean firstPerson(AvatarRenderState player, ItemStackRenderState item, float partialTick, InteractionHand hand, float equip,
			PoseStack pose, SubmitNodeCollector collector, int light) {
		boolean main = hand == InteractionHand.MAIN_HAND;
		boolean leftArm = (player.mainArm == HumanoidArm.LEFT) == main;
		float side = leftArm ? -1.0F : 1.0F;
		float aimed = main ? aim(partialTick) : 0.0F;
		float k = main ? Mth.lerp(partialTick, kickPrevious, kick) : 0.0F;
		frames++;
		pose.pushPose();
		Vector3f t = new Vector3f(HIP).lerp(AIMED, aimed);
		pose.translate(t.x * side, t.y - 0.6F * equip, t.z + KICK_BACK * k / 16.0F);
		pose.rotate(new Quaternionf().rotationX((float) Math.toRadians(KICK_UP * k)));
		// the rifle's frame: the item's model space (the grip's top at its centre) turned so the barrel runs ahead
		pose.rotate(new Quaternionf().rotationY((float) Math.toRadians(90.0)));
		item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
		if (main) {
			float size = Mth.lerp(aimed, ARM_HIP, ARM_AIMED);
			boolean slim = player.skin != null && player.skin.model() == PlayerModelType.SLIM;
			Identifier skin = player.skin == null ? null : player.skin.body().texturePath();
			if (skin != null) {
				arm(pose, collector, light, skin, slim, leftArm ? mirror(RIGHT_HAND) : RIGHT_HAND, leftArm ? mirror(RIGHT_SHOULDER) : RIGHT_SHOULDER,
						!leftArm, size);
				arm(pose, collector, light, skin, slim, leftArm ? mirror(LEFT_HAND) : LEFT_HAND, leftArm ? mirror(LEFT_SHOULDER) : LEFT_SHOULDER,
						leftArm, size);
			}
		}
		pose.popPose();
		return true;
	}

	private static Vector3f mirror(Vector3f v) {
		return new Vector3f(-v.x, v.y, v.z);
	}

	/**
	 * One arm in the rifle's frame (as posed: the item's model space, blocks): its fist at {@code hand}, running toward
	 * {@code shoulder} (the rifle's pixels), drawn as the guns draw theirs (GunArmsLayer): the player model's arm, twelve
	 * pixels, the fist two past the hand.
	 */
	private static void arm(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean slim, Vector3f hand, Vector3f shoulder,
			boolean rightPart, float size) {
		ModelPart part = parts(slim)[rightPart ? 0 : 1];
		pose.pushPose();
		// the rifle's pixels to the item's model space: (x, y, z) -> (z, y, -x) / 16
		pose.translate(hand.z / 16.0F, hand.y / 16.0F, -hand.x / 16.0F);
		Vector3f d = new Vector3f(shoulder).sub(hand);
		pose.rotate(new Quaternionf().rotationTo(0.0F, -1.0F, 0.0F, d.z, d.y, -d.x));
		pose.scale(size, size, size);
		float centre = slim ? 0.5F : 1.0F;
		pose.translate((rightPart ? centre : -centre) / 16.0F, -8.0F / 16.0F, 0.0F);
		collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(skin), (p, buffer) -> {
			PoseStack local = new PoseStack();
			local.last().set(p);
			part.render(local, buffer, light, OverlayTexture.NO_OVERLAY);
		});
		pose.popPose();
	}

	/** The player model's right and left arm (with their sleeves), at the origin. */
	private static ModelPart[] parts(boolean slim) {
		if (slim ? slimArms == null : arms == null) {
			ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
			ModelPart[] baked = {root.getChild("right_arm"), root.getChild("left_arm")};
			for (ModelPart part : baked) {
				part.x = part.y = part.z = 0.0F;
				part.xRot = part.yRot = part.zRot = 0.0F;
			}
			if (slim) {
				slimArms = baked;
			} else {
				arms = baked;
			}
		}
		return slim ? slimArms : arms;
	}

	/** Frames drawn in first person so far (for the client game tests). */
	public static long frames() {
		return frames;
	}

	/** Frames the reticle was drawn so far (for the client game tests). */
	public static long reticles() {
		return reticles;
	}
}

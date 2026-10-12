package io.github.jimbozoomer.jugcraft.client.guns;

import com.geckolib.cache.model.GeoLocator;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import java.util.List;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a gun with GeckoLib: the model rebuilt from the owner's parts (assets/jugcraft/geckolib/models/item/&lt;gun&gt;),
 * the owner's atlas (textures/item/guns/&lt;gun&gt;) and animations. In the player's own first-person view it also
 * draws their arms where the animations put them ({@link GunArmsLayer}) and, while they aim, slides the gun so its
 * sight sits on the middle of the screen. The gun's fitted attachments show as its own parts, in place of the standard
 * parts they replace (tools/guns.py effective_bones()).
 * <p>
 * The model's coordinates are the owner's item-model pixels less (8, 0, 8), so moving it by half a block puts it where
 * the owner's item model was, and the owner's display transforms (the base model, models/item/&lt;gun&gt;) apply as
 * they were made.
 * <p>
 * Slice 6: just after a shot, a gun in someone's hand shows its muzzle flash ({@link GunFlashLayer}).
 * <p>
 * Slice 7: aiming slides a fitted scope's eyepiece onto the middle of the screen ("sight_&lt;scope&gt;", in place of
 * "sight"); when the view through a magnifying scope fills the screen ({@link GunScope}) the gun drops out of sight.
 * <p>
 * Slice 8B: a gun whose moving parts would reach the eye aimed at the hip's depth is held further out as it is aimed
 * ({@link GunLooks#EYE_RELIEF}).
 * <p>
 * Slice 8C: a rotary gun's barrels turn as its holder spins them ({@link GunEffects#barrelTurn}); a flame gun's jet of
 * flame is a prop its shot moves. A gun without sights (the Thresher) is not slid over when aimed.
 * <p>
 * Slice 9F, the aiming polish: as a gun is slid onto its sights, its holder's arms are drawn smaller
 * ({@link GunArmsLayer#AIMED_SIZE}), and past halfway a fitted stock is left out ({@link #SHOULDERED}): held where the
 * hip view holds them, the grip hand and the Light and Weighted Stocks came up under the crosshair.
 * <p>
 * Slice 10G, two guns at once: a gun drawn in a left hand has its holder's arms drawn as their mirror image
 * ({@link View#left}); the other hand's gun has its own flash and only its grip hand's arm, and is never slid onto its
 * sights or thrust ({@link View#offHand}).
 */
public final class GunRenderer extends GeoItemRenderer<GunItem> {
	/** The entity holding the gun (for where its sounds play). */
	public static final DataTicket<Integer> OWNER = DataTicket.create("jugcraft_gun_owner", Integer.class);
	/** The player's own first-person view: what the arms and aiming need. */
	public static final DataTicket<View> VIEW = DataTicket.create("jugcraft_gun_view", View.class);
	/** The gun's fitted attachments. */
	public static final DataTicket<Fitted> FITTED = DataTicket.create("jugcraft_gun_attachments", Fitted.class);
	/** A muzzle flash to draw: the shot was moments ago and nothing fitted hides it. */
	public static final DataTicket<Flash> FLASH = DataTicket.create("jugcraft_gun_flash", Flash.class);
	/** How far a rotary gun's barrels have turned (radians, slice 8C). */
	public static final DataTicket<Float> BARREL_TURN = DataTicket.create("jugcraft_gun_barrel_turn", Float.class);
	/** Whether the gun is drawn in a left hand (slice 10G: its casings fly out to that side). */
	public static final DataTicket<Boolean> LEFT = DataTicket.create("jugcraft_gun_left", Boolean.class);
	/** How far the gun drops, out of sight, while the view through its scope fills the screen (blocks). */
	private static final float PUT_AWAY = 1.5F;
	/** How far a bayonet stab drives the gun forward, and down, on screen at full thrust (blocks). */
	private static final float THRUST_REACH = 0.35F;
	private static final float THRUST_DROP = 0.06F;
	/** A slot's attachment bones, and a second set where the slot is on two bones (the Warden Pistol's spare magazine). */
	private static final List<String> SETS = List.of("", "_2");
	/**
	 * How far into aiming a fitted stock drops out of the player's own view (slice 9F). Aimed, a stock is set against the
	 * shoulder, under and behind the eye; drawn where the hip view holds it, the Light and Weighted Stocks rose under the
	 * crosshair as a block (the Garrison Rifle's Light Stock to the crosshair itself).
	 */
	static final float SHOULDERED = 0.5F;
	/** Frames a fitted stock was left out while aiming (for the client game tests). */
	private static long stowed;
	/** How much further out this gun is held at full aim than at the hip (blocks; {@link GunLooks#EYE_RELIEF}). */
	private final float eyeRelief;

	public GunRenderer(GunItem gun) {
		super(new DefaultedItemGeoModel<GunItem>(Jugcraft.id(gun.name())).withAltTexture(Jugcraft.id("guns/" + gun.name())));
		eyeRelief = GunLooks.EYE_RELIEF.getOrDefault(gun.name(), 0.0F) / 16.0F;
		withRenderLayer(new GunArmsLayer(this));
		withRenderLayer(new GunFlashLayer(this));
	}

	@Override
	public void addRenderData(GunItem gun, RenderData data, GeoRenderState state, float partialTick) {
		List<String> fitted = GunItem.attachments(data.itemStack());
		state.addGeckolibData(FITTED, new Fitted(fitted));
		ItemDisplayContext context = data.renderPerspective();
		boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		state.addGeckolibData(LEFT, left);
		Entity owner = data.itemOwner() instanceof Entity entity ? entity : null;
		// Slice 10G: the gun in the hand that is not the holder's main one is the other hand's.
		InteractionHand hand = owner instanceof LivingEntity holder && left != (holder.getMainArm() == HumanoidArm.LEFT)
				? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		if (owner != null) {
			state.addGeckolibData(OWNER, owner.getId());
			int spinUp = JugcraftGuns.spinUp(gun);
			if (spinUp > 0) {
				state.addGeckolibData(BARREL_TURN, GunEffects.barrelTurn(owner.getId(), owner.level().getGameTime(), partialTick, spinUp));
			}
			float age = GunEffects.flashAge(owner.getId(), hand, owner.level().getGameTime(), partialTick);
			if (age >= 0.0F && inHand(context) && fitted.stream().noneMatch(GunLooks.HIDE_FLASH::contains)) {
				// From the muzzle, or from the front of a barrel attachment that lengthens it.
				String locator = fitted.stream().filter(name -> JugcraftGuns.ATTACHMENTS.get(name).slot().equals("barrel")).findFirst()
						.map(name -> "muzzle_" + name).orElse("muzzle");
				state.addGeckolibData(FLASH, new Flash(age, GunEffects.lastShot(owner.getId(), hand),
						GunLooks.FLASH_SIZES.getOrDefault(gun.spec().ammo(), 6.0F), GunLooks.FLASH_TINTS.getOrDefault(gun.spec().ammo(), 0xFFFFFF),
						locator));
			}
		}
		Minecraft client = Minecraft.getInstance();
		if (context.firstPerson() && owner instanceof AbstractClientPlayer player && player == client.player) {
			boolean slim = player.getSkin().model() == PlayerModelType.SLIM;
			String optic = GunItem.inSlot(data.itemStack(), "optic");
			// Slice 10G: only the main hand's gun is aimed and thrust; a scope's view puts both away.
			boolean offHand = hand == InteractionHand.OFF_HAND;
			state.addGeckolibData(VIEW, new View(player.getSkin().body().texturePath(), slim, offHand ? 0.0F : GunView.aim(partialTick),
					offHand ? 0.0F : GunEffects.thrust(player.getId(), player.level().getGameTime(), partialTick),
					optic == null ? "sight" : "sight_" + optic, GunScope.viewing(partialTick), left, offHand));
		}
	}

	/**
	 * The locator aiming puts on the middle of the screen: the fitted scope's eyepiece, else the gun's own sight; none on a
	 * gun without sights (the Thresher, the Seam Cutter), which stays at the hip when aimed.
	 */
	static Optional<GeoLocator> sight(RenderPassInfo<GeoRenderState> info, View view) {
		return info.model().getLocator(view.sight()).or(() -> info.model().getLocator("sight"));
	}

	/** Frames a fitted stock was left out while aiming, so far (for the client game tests). */
	public static long stowed() {
		return stowed;
	}

	/** Whether the gun is drawn in someone's hand (not in a slot, on the ground or in a frame). */
	private static boolean inHand(ItemDisplayContext context) {
		return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
	}

	/**
	 * The props the animations move on bones of their own (tools/guns.py PROPS); slice 8C's jet of flame, and the
	 * Fowler's second ball (slice 10C), one rammed down each barrel.
	 */
	private static final List<String> PROPS = List.of("shell", "ball", "ball2", "ram", "flash", "flame");

	/**
	 * A prop (a shell, a ball, a ramrod, a priming flash) rests out of place: an animation's offsets bring it where it
	 * belongs, and an animation that leaves it alone (an inspect, the idle) would show it at its rest. So it shows only
	 * while an animation moves it.
	 * <p>
	 * Each attachment's bone ("att_&lt;id&gt;") shows only while it is fitted, and a slot's standard part
	 * ("std_&lt;slot&gt;") only while nothing fitted replaces it. A rotary gun's barrels turn about their middle, which
	 * runs along the bore (slice 8C). In the player's own view, a fitted stock is left out once they are halfway into
	 * aiming down the gun's sights ({@link #SHOULDERED}; slice 9F).
	 */
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> info, BoneSnapshots snapshots) {
		for (String prop : PROPS) {
			snapshots.ifPresent(prop, bone -> bone.skipRender(!bone.hasTranslation()));
		}
		Float turn = info.getGeckolibData(BARREL_TURN);
		if (turn != null) {
			snapshots.ifPresent("barrels", bone -> bone.setRotZ(turn));
		}
		Fitted fitted = info.getGeckolibData(FITTED);
		List<String> on = fitted == null ? List.of() : fitted.attachments();
		View view = info.getGeckolibData(VIEW);
		boolean shouldered = view != null && view.aim() >= SHOULDERED && sight(info, view).isPresent();
		if (shouldered && on.stream().anyMatch(name -> JugcraftGuns.ATTACHMENTS.get(name).slot().equals("stock"))) {
			stowed++;
		}
		for (String set : SETS) {
			for (String name : JugcraftGuns.ATTACHMENTS.keySet()) {
				boolean stowedStock = shouldered && JugcraftGuns.ATTACHMENTS.get(name).slot().equals("stock");
				snapshots.ifPresent("att_" + name + set, bone -> bone.skipRender(!on.contains(name) || stowedStock));
			}
			for (String slot : JugcraftGuns.SLOTS) {
				boolean replaced = on.stream().map(JugcraftGuns.ATTACHMENTS::get)
						.anyMatch(attachment -> attachment.slot().equals(slot) && attachment.replaces());
				snapshots.ifPresent("std_" + slot + set, bone -> bone.skipRender(replaced));
			}
		}
	}

	@Override
	public void adjustRenderPose(RenderPassInfo<GeoRenderState> info) {
		PoseStack poseStack = info.poseStack();
		View view = info.getGeckolibData(VIEW);
		if (view != null && view.aim() > 0) {
			GeoLocator sight = sight(info, view).orElse(null);
			if (sight != null) {
				// Where the sight is on screen now, then the move that puts it on the crosshair (in view space).
				Matrix4f pose = new Matrix4f(poseStack.last().pose()).translate(0.5F, 0.0F, 0.5F);
				Vector3f at = pose.transformPosition(new Vector3f(sight.offsetX() / 16.0F, sight.offsetY() / 16.0F, sight.offsetZ() / 16.0F));
				poseStack.last().pose().translateLocal(-at.x() * view.aim(), -at.y() * view.aim(), 0.0F);
			}
			// Held further out, straight ahead (in view space), so its sight stays on the crosshair.
			poseStack.last().pose().translateLocal(0.0F, 0.0F, -eyeRelief * view.aim());
		}
		if (view != null && view.thrust() > 0) {
			// A bayonet stab drives the gun forward and a little down, and back (in view space).
			poseStack.last().pose().translateLocal(0.0F, -THRUST_DROP * view.thrust(), -THRUST_REACH * view.thrust());
		}
		if (view != null && view.putAway()) {
			// The view through the scope fills the screen; the gun would only stand in it.
			poseStack.last().pose().translateLocal(0.0F, -PUT_AWAY, 0.0F);
		}
		poseStack.translate(0.5F, 0.0F, 0.5F);
	}

	/**
	 * @param skin    the player's skin texture
	 * @param slim    whether their arms are three pixels wide
	 * @param aim     how far into aiming down the sights (0 to 1)
	 * @param thrust  how far into a bayonet stab's thrust (0 to 1; {@link GunEffects#thrust})
	 * @param sight   the locator aiming puts on the middle of the screen: "sight", or a fitted scope's "sight_&lt;scope&gt;"
	 * @param putAway whether the view through a scope fills the screen, so the gun is out of sight ({@link GunScope})
	 * @param left    whether the gun is drawn in the left hand, so the arms are drawn as their mirror image (slice 10G)
	 * @param offHand whether it is the other hand's gun, beside the main one (slice 10G): only its grip hand's arm is drawn
	 */
	public record View(net.minecraft.resources.Identifier skin, boolean slim, float aim, float thrust, String sight,
			boolean putAway, boolean left, boolean offHand) {
	}

	/** @param attachments the attachments fitted to the gun drawn ({@link GunItem#attachments}) */
	public record Fitted(List<String> attachments) {
	}

	/**
	 * @param age     ticks since the shot
	 * @param shot    the shot's game time (picks the frame and its turn)
	 * @param size    across, in the model's pixels ({@link GunLooks#FLASH_SIZES})
	 * @param tint    the colour multiplied into the owner's frames, RGB ({@link GunLooks#FLASH_TINTS}; white for none)
	 * @param locator where it comes out: "muzzle", or "muzzle_&lt;attachment&gt;" for a barrel attachment
	 */
	public record Flash(float age, long shot, float size, int tint, String locator) {
	}
}

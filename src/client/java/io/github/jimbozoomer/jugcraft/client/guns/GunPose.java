package io.github.jimbozoomer.jugcraft.client.guns;

import io.github.jimbozoomer.jugcraft.guns.GunItem;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * How a player holds a gun, seen from outside (slice 6): the gun arm raised along the look, so the gun points where
 * they look (the owner's third-person transforms are made for a raised arm: hanging, the gun pointed at the ground),
 * and for a gun held in both hands the other arm brought across to the fore-end, as vanilla holds a loaded crossbow.
 * One-handed guns ({@link GunLooks.Look#twoHanded}) leave the other arm be, but for a second one-handed gun in the other
 * hand (slice 10G, {@link GunItem#dual}): then that arm is raised along the look too. A gun whose transform tilts it up off
 * the arm ({@link GunLooks#TILT}: the Gattaler's, made for an arm hanging at the hip) has both arms hang that much lower.
 * <p>
 * The arms motion hooks call this: {@link #extract} as a player's render state is filled (ArmsRenderStateMixin) and
 * {@link #apply} after vanilla poses the model (ArmsHumanoidModelMixin); armor posed from the same state follows. The
 * pose is one of eight shared values and the tilt the gun's own, so nothing is allocated per frame (a bayonet's thrust,
 * slice 7, only while it lasts). Players, and (slice 10F) mobs holding a gun, such as the raider gunners; not while
 * swimming, gliding or asleep.
 */
public final class GunPose {
	/** The hold on a player's render state: null when they hold no gun. */
	public static final RenderStateDataKey<Hold> HOLD = RenderStateDataKey.create(() -> "jugcraft:gun_hold");
	/** How far into a bayonet stab's thrust they are (slice 7): null at rest. */
	public static final RenderStateDataKey<Float> THRUST = RenderStateDataKey.create(() -> "jugcraft:gun_thrust");
	/** How far the held gun's transform tilts it up off the arm, in degrees ({@link GunLooks#TILT}): null for none. */
	public static final RenderStateDataKey<Float> TILT = RenderStateDataKey.create(() -> "jugcraft:gun_tilt");
	/** How far the arms drive forward at full thrust (model pixels). */
	private static final float THRUST_REACH = 4.0F;
	/** Vanilla raises a crouching player's arms this much with the crouch; the raised arms keep it. */
	private static final float CROUCH = 0.4F;
	/** Frames posed so far, and of those with a gun in each hand (slice 10G; for the client game tests). */
	private static long posed;
	private static long posedDual;

	private GunPose() {
	}

	/** The gun's hold for this frame, its tilt and any bayonet thrust, kept on the player's render state. */
	public static void extract(LivingEntity entity, ArmedEntityRenderState state, float partialTick) {
		Hold hold = null;
		Float tilt = null;
		if ((entity instanceof Player || entity instanceof Mob) && entity.getMainHandItem().getItem() instanceof GunItem gun
				&& !entity.isVisuallySwimming()
				&& !entity.isFallFlying() && !entity.isSleeping()) {
			// The other hand busy with something of its own (eating, a shield) keeps to it.
			boolean otherBusy = entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.OFF_HAND;
			boolean both = GunLooks.of(gun.name()).twoHanded() && !otherBusy;
			// Slice 10G: a one-handed gun in each hand raises both arms, each with its gun.
			boolean dual = GunItem.dual(entity) && !otherBusy;
			hold = Hold.of(both, entity.getMainArm() == HumanoidArm.LEFT, dual);
			tilt = GunLooks.tilt(gun.name());
		}
		if (state.getData(HOLD) != hold) {
			state.setData(HOLD, hold);
		}
		if (state.getData(TILT) != tilt) {
			state.setData(TILT, tilt);
		}
		float thrust = hold == null ? 0.0F : GunEffects.thrust(entity.getId(), entity.level().getGameTime(), partialTick);
		if (thrust > 0.0F || state.getData(THRUST) != null) {
			state.setData(THRUST, thrust > 0.0F ? thrust : null);
		}
	}

	/** Raises the arms for the hold, over vanilla's pose. */
	public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
		Hold hold = state.getData(HOLD);
		if (hold == null) {
			return;
		}
		posed++;
		ModelPart main = hold.leftHanded() ? model.leftArm : model.rightArm;
		ModelPart other = hold.leftHanded() ? model.rightArm : model.leftArm;
		float side = hold.leftHanded() ? -1.0F : 1.0F;
		float crouch = state.isCrouching ? CROUCH : 0.0F;
		ModelPart head = model.head;
		// A gun tilted up off the arm is held that much lower, so it still points along the look.
		Float tilt = state.getData(TILT);
		float lower = tilt == null ? 0.0F : (float) Math.toRadians(tilt);
		// Along the look, a little in toward the body.
		main.xRot = (float) (-Math.PI / 2.0) + lower + head.xRot + 0.1F + crouch;
		main.yRot = head.yRot - 0.3F * side;
		main.zRot = 0.0F;
		if (hold.twoHanded()) {
			// Across to the fore-end.
			other.xRot = -1.5F + lower + head.xRot + crouch;
			other.yRot = head.yRot + 0.6F * side;
			other.zRot = 0.0F;
		} else if (hold.dual()) {
			posedDual++;
			// Slice 10G: the other gun's arm along the look as well, a little in toward the body.
			other.xRot = (float) (-Math.PI / 2.0) + lower + head.xRot + 0.1F + crouch;
			other.yRot = head.yRot + 0.3F * side;
			other.zRot = 0.0F;
		}
		Float thrust = state.getData(THRUST);
		if (thrust != null) {
			// A bayonet stab: the arms drive the gun forward and back.
			main.z -= THRUST_REACH * thrust;
			if (hold.twoHanded()) {
				other.z -= THRUST_REACH * thrust;
			}
		}
	}

	/** Frames a player has been posed holding a gun, so far (for the client game tests). */
	public static long posed() {
		return posed;
	}

	/** Frames posed holding a gun in each hand, both arms raised, so far (slice 10G; for the client game tests). */
	public static long posedDual() {
		return posedDual;
	}

	/**
	 * @param twoHanded    both arms come up
	 * @param leftHanded   the gun is in the left hand
	 * @param dual         a one-handed gun in each hand: both arms come up, each with its own (slice 10G)
	 */
	public record Hold(boolean twoHanded, boolean leftHanded, boolean dual) {
		private static final Hold[] ALL = new Hold[8];

		static {
			for (int i = 0; i < ALL.length; i++) {
				ALL[i] = new Hold((i & 1) != 0, (i & 2) != 0, (i & 4) != 0);
			}
		}

		static Hold of(boolean twoHanded, boolean leftHanded, boolean dual) {
			return ALL[(twoHanded ? 1 : 0) + (leftHanded ? 2 : 0) + (dual ? 4 : 0)];
		}
	}
}

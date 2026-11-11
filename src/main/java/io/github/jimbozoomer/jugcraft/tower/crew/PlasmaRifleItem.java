package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The NT-7 plasma rifle in a player's hands (the creative menu only, {@link PlasmaRifle}; take 3 of rifle A, Justin, 9
 * November 2026). It works as the guns do (his pick): the attack button is the trigger (client/PlasmaRifleClient sends
 * {@link TriggerPayload}; the server decides, {@link #trigger}), and right click held aims down its holographic sight
 * (using the item: the view narrows, you walk at {@value #AIM_SPEED} of your pace, the reticle is drawn on the middle of
 * the screen). A shot every {@value PlasmaRifle#COOLDOWN} ticks, as before.
 * <p>
 * "Accurately lined up with where shots land" (Justin): a shot goes exactly where the middle of the screen is, the
 * crosshair from the hip and the reticle's dot when aimed. The server finds the first block or creature on the line
 * from the eye along the look ({@link #aimPoint}, as far as a bolt flies), and the bolt flies straight from the muzzle
 * to that point with no spread ({@link #muzzle}: just ahead of the eye, under the sight when aimed, at the gun's side at
 * the hip; from the eye itself when something is nearer than that). Bolts don't fall, so it lands on the dot at any range.
 */
public class PlasmaRifleItem extends Item {
	/** Walking speed while aiming (as a gun's; vanilla's using an item is 0.2). */
	public static final float AIM_SPEED = 0.6F;
	/** How far a player's shot reaches: as far as a bolt flies. */
	public static final double REACH = PlasmaRifle.SPEED * PlasmaRifle.LIFE;
	/** The bolt starts this far ahead of the eye; at the hip this far to the gun's side and below the eye; aimed, below. */
	static final double AHEAD = 0.8, HIP_SIDE = 0.2, HIP_DOWN = 0.14, AIMED_DOWN = 0.08;

	/** Client to server: the player pulled the trigger of the rifle in their main hand. */
	public record TriggerPayload() implements CustomPacketPayload {
		public static final TriggerPayload INSTANCE = new TriggerPayload();
		public static final Type<TriggerPayload> TYPE = new Type<>(Jugcraft.id("plasma_trigger"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TriggerPayload> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public PlasmaRifleItem(Properties properties) {
		super(properties);
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TriggerPayload.TYPE, TriggerPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TriggerPayload.TYPE, (payload, context) -> trigger(context.player()));
	}

	/** Aims down the sight while held (the main hand only: the trigger is the attack button). */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.NONE;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	/** Whether this player is aiming down the rifle's sight. */
	public static boolean aiming(LivingEntity entity) {
		ItemStack stack = entity.getMainHandItem();
		return stack.getItem() instanceof PlasmaRifleItem && entity.isUsingItem() && entity.getUseItem() == stack;
	}

	/**
	 * The trigger, pulled by this player: fires if the rifle is in their main hand and ready (not on its cooldown) and
	 * they're alive and playing; returns the bolt, or null.
	 */
	public static @Nullable PlasmaBolt trigger(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof PlasmaRifleItem) || !player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(stack)) {
			return null;
		}
		ServerLevel level = player.level();
		Vec3 aim = aimPoint(player, REACH);
		PlasmaBolt bolt = PlasmaRifle.fire(level, player, muzzle(player, aiming(player), aim), aim, null, 0.0F);
		player.getCooldowns().addCooldown(stack, PlasmaRifle.COOLDOWN);
		return bolt;
	}

	/**
	 * Where a shot from this player goes: the first block (its collision shape) or creature (that a bolt could hit) on the
	 * line from their eye along their look, within {@code reach}; the line's end if nothing.
	 */
	public static Vec3 aimPoint(Player player, double reach) {
		Level level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(reach));
		HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
				e -> !e.isSpectator() && e.isPickable() && !e.isPassengerOfSameVehicle(player), eye.distanceToSqr(end));
		return hit != null ? hit.getLocation() : end;
	}

	/**
	 * Where this player's bolt starts: {@value #AHEAD} ahead of the eye, below it (under the sight when aimed; lower and
	 * to the gun's side at the hip); from the eye itself if the aimed point is nearer than that.
	 */
	public static Vec3 muzzle(Player player, boolean aimed, Vec3 aim) {
		Vec3 eye = player.getEyePosition();
		if (aim.distanceTo(eye) < AHEAD + 0.4) {
			return eye;
		}
		Vec3 look = player.getLookAngle();
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		Vec3 up = right.cross(look).normalize();
		double side = player.getMainArm() == HumanoidArm.LEFT ? -HIP_SIDE : HIP_SIDE;
		Vec3 from = eye.add(look.scale(AHEAD));
		return aimed ? from.subtract(up.scale(AIMED_DOWN)) : from.add(right.scale(side)).subtract(up.scale(HIP_DOWN));
	}
}

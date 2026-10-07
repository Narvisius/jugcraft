package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

/**
 * The hex brews of the Bubbling Cauldron (fall addition 21, {@link BubblingCauldronBlock}): the effects their draughts
 * give and what drinking each does.
 * <ul>
 * <li>The Shrinking Draught: Shrunk for {@value #SHRINKING_SECONDS} seconds, half size (attribute scale ×(1 −
 * {@value #SHRUNK_SCALE})). A shrunk player without room to grow back when it ends stays small another
 * {@value #ROOM_EXTEND_TICKS} ticks, as often as needed, so nobody grows into a wall.</li>
 * <li>The Giant's Draught: Giant for {@value #GIANT_SECONDS} seconds, scale ×(1 + {@value #GIANT_SCALE}), stepping
 * {@value #GIANT_STEP} blocks higher and reaching {@value #GIANT_REACH} further. Only drunk with room to grow.</li>
 * <li>Flying Ointment, rubbed on: Slow Falling for {@value #FLYING_SECONDS} seconds.</li>
 * </ul>
 * Each cancels the other's size. Effects are the server's; the size reaches every client as an attribute.
 */
public final class Hexes {
	public static final double SHRUNK_SCALE = 0.5;
	public static final double GIANT_SCALE = 0.6;
	public static final double GIANT_STEP = 0.5;
	public static final double GIANT_REACH = 1.0;
	public static final int SHRINKING_SECONDS = 180;
	public static final int GIANT_SECONDS = 180;
	public static final int FLYING_SECONDS = 30;
	public static final int ROOM_EXTEND_TICKS = 100;
	/** A player's width and height at normal size. */
	private static final double WIDTH = 0.6;
	private static final double HEIGHT = 1.8;

	public static Holder<MobEffect> SHRUNK;
	public static Holder<MobEffect> GIANT;

	private Hexes() {
	}

	/** A hex's effect, with no more than its attribute changes. */
	private static final class HexEffect extends MobEffect {
		HexEffect(MobEffectCategory category, int colour) {
			super(category, colour);
		}
	}

	static void register() {
		SHRUNK = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("shrunk"),
				new HexEffect(MobEffectCategory.NEUTRAL, 0x6FBF4A).addAttributeModifier(Attributes.SCALE, Jugcraft.id("shrunk"), -SHRUNK_SCALE,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		GIANT = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("giant"),
				new HexEffect(MobEffectCategory.NEUTRAL, 0xE07A1A).addAttributeModifier(Attributes.SCALE, Jugcraft.id("giant"), GIANT_SCALE,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
						.addAttributeModifier(Attributes.STEP_HEIGHT, Jugcraft.id("giant_step"), GIANT_STEP, AttributeModifier.Operation.ADD_VALUE)
						.addAttributeModifier(Attributes.BLOCK_INTERACTION_RANGE, Jugcraft.id("giant_reach"), GIANT_REACH,
								AttributeModifier.Operation.ADD_VALUE)
						.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, Jugcraft.id("giant_arm"), GIANT_REACH,
								AttributeModifier.Operation.ADD_VALUE));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				keepSmallWithoutRoom(player);
			}
		});
	}

	/** The id of the draught a glass bottle draws from the hex brew {@code hex}. */
	public static String draughtId(BubblingCauldronBlock.Brew hex) {
		return switch (hex) {
			case SHRINKING -> "shrinking_draught";
			case GIANT -> "giants_draught";
			default -> "flying_ointment";
		};
	}

	/** The draught a glass bottle draws from the hex brew {@code hex}. */
	public static Item draught(BubblingCauldronBlock.Brew hex) {
		return JugcraftAgriculture.item(draughtId(hex));
	}

	/** Whether {@code player}, standing where they are, would fit at {@code scale} times their normal size. */
	public static boolean roomFor(Player player, double scale) {
		double half = WIDTH * scale / 2.0;
		AABB box = new AABB(player.getX() - half, player.getY(), player.getZ() - half, player.getX() + half, player.getY() + HEIGHT * scale,
				player.getZ() + half);
		return player.level().noCollision(player, box);
	}

	/** Whether the giant's draught may be drunk here. */
	public static boolean roomToGrow(Player player) {
		return roomFor(player, 1.0 + GIANT_SCALE);
	}

	/** What drinking (or rubbing on) the draught of {@code hex} does to {@code player}. */
	public static void drink(ServerPlayer player, BubblingCauldronBlock.Brew hex) {
		switch (hex) {
			case SHRINKING -> {
				player.removeEffect(GIANT);
				player.addEffect(new MobEffectInstance(SHRUNK, SHRINKING_SECONDS * 20));
				TrickOrTreat.award(player, "drink_me");
			}
			case GIANT -> {
				player.removeEffect(SHRUNK);
				player.addEffect(new MobEffectInstance(GIANT, GIANT_SECONDS * 20));
				TrickOrTreat.award(player, "fee_fi_fo_fum");
			}
			default -> player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FLYING_SECONDS * 20));
		}
	}

	/** A shrunk player about to grow back with no room to stays small a little longer. */
	public static void keepSmallWithoutRoom(ServerPlayer player) {
		MobEffectInstance shrunk = player.getEffect(SHRUNK);
		if (shrunk != null && !shrunk.isInfiniteDuration() && shrunk.getDuration() <= 2 && !roomFor(player, 1.0)) {
			player.addEffect(new MobEffectInstance(SHRUNK, ROOM_EXTEND_TICKS));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.hex.no_room"));
		}
	}
}

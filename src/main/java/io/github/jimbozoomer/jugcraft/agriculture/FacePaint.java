package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Face paint: a design painted on a player's face with a Face Paint Kit ({@link FacePaintKitItem}). It is kept on the
 * player (a Fabric data attachment, saved with them and sent to every client that can see them, each of which draws it on
 * their face) until it washes off: with the player's head under water (looked for every {@value #WASH_TICKS} ticks), or
 * at death. A painted face counts as a costume, for trick-or-treating and the costume contest ({@link #inCostume}).
 */
public final class FacePaint {
	public static final int WASH_TICKS = 20;

	/** The designs a kit can paint. */
	public enum Design implements StringRepresentable {
		SKULL, PUMPKIN, BLACK_CAT, VAMPIRE, WITCH, SCARECROW;

		public static final Codec<Design> CODEC = StringRepresentable.fromEnum(Design::values);
		public static final StreamCodec<ByteBuf, Design> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
				index -> values()[Math.floorMod(index, values().length)], Design::ordinal);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		public Component displayName() {
			return Component.translatable("face_paint.jugcraft." + getSerializedName());
		}

		/** The next design round the kit's dial. */
		public Design next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** The design painted on a player's face, if any. */
	public static AttachmentType<Design> PAINT;

	private FacePaint() {
	}

	static void register() {
		PAINT = AttachmentRegistry.<Design>builder().persistent(Design.CODEC).syncWith(Design.STREAM_CODEC, AttachmentSyncPredicate.all())
				.buildAndRegister(Jugcraft.id("face_paint"));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % WASH_TICKS == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					washIfUnderWater(player);
				}
			}
		});
	}

	/** The design on {@code player}'s face, or null. */
	public static @Nullable Design design(Player player) {
		return player.getAttached(PAINT);
	}

	public static boolean painted(Player player) {
		return player.hasAttached(PAINT);
	}

	/** Paints {@code design} on {@code player}'s face (over any design already there). */
	public static void paint(Player player, Design design) {
		player.setAttached(PAINT, design);
	}

	/** Washes {@code player}'s face clean; returns whether there was paint to wash off. */
	public static boolean wash(Player player) {
		if (!painted(player)) {
			return false;
		}
		player.removeAttached(PAINT);
		return true;
	}

	/** Whether water stands where {@code player}'s eyes are. */
	public static boolean headUnderWater(Player player) {
		return player.level().getFluidState(BlockPos.containing(player.getEyePosition())).is(FluidTags.WATER);
	}

	/** Washes the paint off a player whose head is under water; returns whether it did. */
	public static boolean washIfUnderWater(ServerPlayer player) {
		if (!painted(player) || !headUnderWater(player) || !wash(player)) {
			return false;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.face_paint.washed"));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.4F, 1.4F);
		return true;
	}

	/** Whether {@code player} is in costume: a costume on their head, or a painted face. */
	public static boolean inCostume(Player player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(TrickOrTreat.COSTUMES) || painted(player);
	}
}

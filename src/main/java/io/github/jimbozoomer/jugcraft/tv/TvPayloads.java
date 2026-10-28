package io.github.jimbozoomer.jugcraft.tv;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The flat screen TV's packets. */
public final class TvPayloads {
	private TvPayloads() {
	}

	/**
	 * Server to client: open the SCREEN window for the screen whose controller is at {@code pos}; {@code control} if
	 * this player may change what it plays, {@code owner} the name of the player whose party may.
	 */
	public record Open(BlockPos pos, boolean control, String owner) implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(Jugcraft.id("tv_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Open::pos,
				ByteBufCodecs.BOOL, Open::control,
				ByteBufCodecs.stringUtf8(64), Open::owner,
				Open::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: open the window of the screen the TV block at {@code pos} is part of (right-clicked from afar). */
	public record Ask(BlockPos pos) implements CustomPacketPayload {
		public static final Type<Ask> TYPE = new Type<>(Jugcraft.id("tv_ask"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Ask> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Ask::pos, Ask::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Client to server: a control on the screen whose controller is at {@code pos} ({@link #LINK} with the link as
	 * {@code text}, {@link #SEEK} with where to as {@code value} in ms, {@link #VOLUME} with the volume as {@code value},
	 * {@link #LOOP} and {@link #PLAYLIST} on with {@code value} 1, off with 0).
	 * The server checks the player may (their party's screen, close enough) and the link is a web address.
	 */
	public record Control(BlockPos pos, int action, String text, long value) implements CustomPacketPayload {
		public static final int LINK = 0, PLAY = 1, PAUSE = 2, SEEK = 3, STOP = 4, VOLUME = 5, LOOP = 6, PLAYLIST = 7;
		public static final Type<Control> TYPE = new Type<>(Jugcraft.id("tv_control"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Control::pos,
				ByteBufCodecs.VAR_INT, Control::action,
				ByteBufCodecs.stringUtf8(TvBlockEntity.MAX_URL), Control::text,
				ByteBufCodecs.VAR_LONG, Control::value,
				Control::new);

		public Control(BlockPos pos, int action) {
			this(pos, action, "", 0);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}

package io.github.jimbozoomer.jugcraft.blueprint;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Blueprint networking. The server sends every blueprint in its library to each player (so holograms and the
 * Blueprint Table work for imported blueprints too); the table sends print and import requests; the stake
 * screen asks for progress and sends its buttons. The server checks reach and permissions every time.
 */
public final class BlueprintNetwork {
	/** Pasted text is sent in parts this long (serverbound packets are limited to 32 KB). */
	public static final int CHUNK = 8000;
	private static final double REACH_SQR = 10 * 10;
	private static final Map<UUID, BlueprintUpload> UPLOADS = new HashMap<>();

	private BlueprintNetwork() {
	}

	/** One blueprint for the client's library; {@code reset} clears the client's library first. */
	public record SyncPayload(boolean reset, String id, String name, String source, String json) implements CustomPacketPayload {
		public static final Type<SyncPayload> TYPE = new Type<>(Jugcraft.id("blueprint_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBoolean(p.reset);
			buf.writeUtf(p.id);
			buf.writeUtf(p.name);
			buf.writeUtf(p.source);
			buf.writeUtf(p.json, Blueprint.MAX_CHARS);
		}, buf -> new SyncPayload(buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(Blueprint.MAX_CHARS)));

		@Override
		public Type<SyncPayload> type() {
			return TYPE;
		}
	}

	/** Part {@code part} (0-based) of {@code parts} of a pasted blueprint. */
	public record ImportPayload(int part, int parts, String text) implements CustomPacketPayload {
		public static final Type<ImportPayload> TYPE = new Type<>(Jugcraft.id("blueprint_import"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ImportPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeVarInt(p.part);
			buf.writeVarInt(p.parts);
			buf.writeUtf(p.text, CHUNK);
		}, buf -> new ImportPayload(buf.readVarInt(), buf.readVarInt(), buf.readUtf(CHUNK)));

		@Override
		public Type<ImportPayload> type() {
			return TYPE;
		}
	}

	/** The outcome of an import, shown on the table's IMPORT page. */
	public record ImportResultPayload(boolean ok, String message, String id) implements CustomPacketPayload {
		public static final Type<ImportResultPayload> TYPE = new Type<>(Jugcraft.id("blueprint_import_result"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ImportResultPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBoolean(p.ok);
			buf.writeUtf(p.message);
			buf.writeUtf(p.id);
		}, buf -> new ImportResultPayload(buf.readBoolean(), buf.readUtf(), buf.readUtf()));

		@Override
		public Type<ImportResultPayload> type() {
			return TYPE;
		}
	}

	/** PRINT at a Blueprint Table. */
	public record PrintPayload(BlockPos table, String id) implements CustomPacketPayload {
		public static final Type<PrintPayload> TYPE = new Type<>(Jugcraft.id("blueprint_print"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PrintPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBlockPos(p.table);
			buf.writeUtf(p.id);
		}, buf -> new PrintPayload(buf.readBlockPos(), buf.readUtf()));

		@Override
		public Type<PrintPayload> type() {
			return TYPE;
		}
	}

	/** The stake screen: 0 = send progress, 1 = switch Personal/Party, 2 = rotate, 3 = remove. */
	public record StakeActionPayload(BlockPos pos, int action) implements CustomPacketPayload {
		public static final int INFO = 0;
		public static final int MODE = 1;
		public static final int ROTATE = 2;
		public static final int REMOVE = 3;
		public static final Type<StakeActionPayload> TYPE = new Type<>(Jugcraft.id("stake_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StakeActionPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBlockPos(p.pos);
			buf.writeVarInt(p.action);
		}, buf -> new StakeActionPayload(buf.readBlockPos(), buf.readVarInt()));

		@Override
		public Type<StakeActionPayload> type() {
			return TYPE;
		}
	}

	/** Progress for the stake screen: blocks in place, what is still needed (item ids and counts), blocks in the way. */
	public record StakeInfoPayload(BlockPos pos, String name, int done, int total, String mode, List<String> items, List<Integer> counts,
			int wrong, List<String> handOnly, boolean canChange, boolean open) implements CustomPacketPayload {
		public static final Type<StakeInfoPayload> TYPE = new Type<>(Jugcraft.id("stake_info"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StakeInfoPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBlockPos(p.pos);
			buf.writeUtf(p.name);
			buf.writeVarInt(p.done);
			buf.writeVarInt(p.total);
			buf.writeUtf(p.mode);
			writeStrings(buf, p.items);
			buf.writeVarInt(p.counts.size());
			p.counts.forEach(buf::writeVarInt);
			buf.writeVarInt(p.wrong);
			writeStrings(buf, p.handOnly);
			buf.writeBoolean(p.canChange);
			buf.writeBoolean(p.open);
		}, buf -> new StakeInfoPayload(buf.readBlockPos(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(),
				readStrings(buf), readInts(buf), buf.readVarInt(), readStrings(buf), buf.readBoolean(), buf.readBoolean()));

		@Override
		public Type<StakeInfoPayload> type() {
			return TYPE;
		}
	}

	private static void writeStrings(FriendlyByteBuf buf, List<String> list) {
		buf.writeVarInt(list.size());
		list.forEach(buf::writeUtf);
	}

	private static List<String> readStrings(FriendlyByteBuf buf) {
		int n = Math.min(256, buf.readVarInt());
		List<String> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			out.add(buf.readUtf());
		}
		return out;
	}

	private static List<Integer> readInts(FriendlyByteBuf buf) {
		int n = Math.min(256, buf.readVarInt());
		List<Integer> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			out.add(buf.readVarInt());
		}
		return out;
	}

	static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ImportResultPayload.TYPE, ImportResultPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StakeInfoPayload.TYPE, StakeInfoPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ImportPayload.TYPE, ImportPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PrintPayload.TYPE, PrintPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StakeActionPayload.TYPE, StakeActionPayload.CODEC);

		ServerLifecycleEvents.SERVER_STARTED.register(BlueprintLibrary::load);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendAll(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> UPLOADS.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> UPLOADS.clear());

		ServerPlayNetworking.registerGlobalReceiver(PrintPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Blueprint blueprint = BlueprintLibrary.get(payload.id());
			if (blueprint != null && near(player, payload.table()) && player.level().getBlockState(payload.table()).is(JugcraftBlueprints.TABLE)) {
				var stack = BlueprintItem.stack(blueprint);
				if (!player.getInventory().add(stack)) {
					net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
				}
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.blueprint.printed", blueprint.name));
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(ImportPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (payload.parts() < 1 || payload.parts() > Blueprint.MAX_CHARS / CHUNK + 1 || payload.part() < 0 || payload.part() >= payload.parts()) {
				return;
			}
			BlueprintUpload text = payload.part() == 0 ? new BlueprintUpload(payload.parts()) : UPLOADS.get(player.getUUID());
			if (text == null) {
				return;
			}
			if (!text.append(payload.part(), payload.parts(), payload.text())) {
				UPLOADS.remove(player.getUUID());
				return;
			}
			UPLOADS.put(player.getUUID(), text);
			if (!text.complete()) {
				return;
			}
			UPLOADS.remove(player.getUUID());
			try {
				Blueprint blueprint = BlueprintLibrary.importText(context.server(), text.text());
				for (ServerPlayer other : context.server().getPlayerList().getPlayers()) {
					send(other, blueprint, false);
				}
				ServerPlayNetworking.send(player, new ImportResultPayload(true, "Imported \"" + blueprint.name + "\": " + blueprint.sizeX + " x "
						+ blueprint.sizeY + " x " + blueprint.sizeZ + ", " + blueprint.size() + " blocks. It is now in the LIBRARY.", blueprint.id));
				Jugcraft.LOGGER.info("{} imported blueprint {} ({} blocks)", player.getName().getString(), blueprint.id, blueprint.size());
			} catch (Blueprint.Invalid | IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
				ServerPlayNetworking.send(player, new ImportResultPayload(false, e.getMessage(), ""));
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(StakeActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!near(player, payload.pos()) || !(player.level().getBlockEntity(payload.pos()) instanceof SurveyStakeBlockEntity stake)) {
				return;
			}
			switch (payload.action()) {
				case StakeActionPayload.MODE -> {
					if (stake.mayChange(player)) {
						stake.toggleMode(player);
					}
				}
				case StakeActionPayload.ROTATE -> {
					if (stake.mayChange(player)) {
						stake.rotate();
					}
				}
				case StakeActionPayload.REMOVE -> {
					if (stake.mayChange(player)) {
						stake.removeAndReturn(player);
						return;
					}
				}
				default -> {
				}
			}
			ServerPlayNetworking.send(player, stake.info(player, false));
		});
	}

	private static boolean near(ServerPlayer player, BlockPos pos) {
		return player.level().isLoaded(pos) && player.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH_SQR;
	}

	static void sendAll(ServerPlayer player) {
		List<Blueprint> all = new ArrayList<>(BlueprintLibrary.all());
		ServerPlayNetworking.send(player, new SyncPayload(true, "", "", "", ""));
		for (Blueprint blueprint : all) {
			send(player, blueprint, false);
		}
	}

	private static void send(ServerPlayer player, Blueprint blueprint, boolean reset) {
		ServerPlayNetworking.send(player, new SyncPayload(reset, blueprint.id, blueprint.name, blueprint.source, blueprint.json));
	}

	/** Used by the client receiver: adds a synced blueprint to the client's library. */
	public static void receive(SyncPayload payload) {
		if (payload.reset()) {
			Blueprint.clear(true);
		}
		if (payload.id().isEmpty()) {
			return;
		}
		try {
			Blueprint.put(Blueprint.parse(payload.id(), payload.json(), payload.source()), true);
		} catch (Blueprint.Invalid e) {
			Jugcraft.LOGGER.warn("Server blueprint {} does not load on this client: {}", payload.id(), e.getMessage());
		}
	}

}

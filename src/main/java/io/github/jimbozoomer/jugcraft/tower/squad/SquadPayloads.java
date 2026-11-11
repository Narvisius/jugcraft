package io.github.jimbozoomer.jugcraft.tower.squad;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * What the drop squads say between the client and the server: a mark from the designator's sight and what became of it,
 * the player's account (for the sight's support pick), and the manager's desk's requisitions (its page, a purchase).
 */
public final class SquadPayloads {
	private SquadPayloads() {
	}

	/** A mark (client to server): the spot looked at through the sight and the squad picked (3, 5 or 7). */
	public record Mark(Vec3 at, int size) implements CustomPacketPayload {
		public static final Type<Mark> TYPE = new Type<>(Jugcraft.id("squad_mark"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Mark> CODEC = StreamCodec.composite(Vec3.STREAM_CODEC, Mark::at,
				ByteBufCodecs.VAR_INT, Mark::size, Mark::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** What became of a mark (server to client: SquadDrop's results), where and for which squad. */
	public record Marked(int result, Vec3 at, int size) implements CustomPacketPayload {
		public static final Type<Marked> TYPE = new Type<>(Jugcraft.id("squad_marked"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Marked> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Marked::result,
				Vec3.STREAM_CODEC, Marked::at, ByteBufCodecs.VAR_INT, Marked::size, Marked::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** The player's account (server to client): the charges for each squad, and whether a squad of theirs is out. */
	public record Account(List<Integer> charges, boolean inField) implements CustomPacketPayload {
		public static final Type<Account> TYPE = new Type<>(Jugcraft.id("squad_account"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Account> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), Account::charges, ByteBufCodecs.BOOL, Account::inField, Account::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * The manager's desk's requisitions page (server to client): the desk, what the tower has of each payment (its store
	 * and the logistics network its terminal reaches), the player's account, their price for each size now (iron, copper,
	 * gold and diamonds, size after size: twelve), whether they may requisition here, whether a squad of theirs is out, and
	 * whether to open the page (else it's a refresh of an open one).
	 */
	public record Page(BlockPos desk, List<Integer> have, List<Integer> charges, List<Integer> prices, boolean allowed, boolean inField,
			boolean open) implements CustomPacketPayload {
		public static final Type<Page> TYPE = new Type<>(Jugcraft.id("squad_requisitions"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Page::desk,
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), Page::have, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), Page::charges,
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), Page::prices, ByteBufCodecs.BOOL, Page::allowed, ByteBufCodecs.BOOL, Page::inField,
				ByteBufCodecs.BOOL, Page::open, Page::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** A requisition (client to server): at the desk, a squad of {@link JugcraftSquads#SIZES}[index]. */
	public record Buy(BlockPos desk, int index) implements CustomPacketPayload {
		public static final Type<Buy> TYPE = new Type<>(Jugcraft.id("squad_buy"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Buy> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Buy::desk,
				ByteBufCodecs.VAR_INT, Buy::index, Buy::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * The leader's orders panel (server to client): the leader (his entity id), his rank and name (rank;name), the squad's
	 * size, its roster near (rank;name;health percent each, the leader first), its order and patrol radius, whether it's in
	 * a fight, a note on the last order (a key of message.jugcraft.squad.order, or none), the game tick their day in the
	 * field is up (the ship called home then: {@link Legionary#FIELD_TICKS}; -1 unknown), and whether to open the panel
	 * (else it's a refresh of an open one).
	 */
	public record Orders(int leader, String title, int size, List<String> roster, int order, int radius, boolean inCombat, String note, long recallAt,
			boolean open) implements CustomPacketPayload {
		public static final Type<Orders> TYPE = new Type<>(Jugcraft.id("squad_orders"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Orders> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Orders::leader,
				ByteBufCodecs.stringUtf8(96), Orders::title, ByteBufCodecs.VAR_INT, Orders::size, ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(16)),
				Orders::roster, ByteBufCodecs.VAR_INT, Orders::order, ByteBufCodecs.VAR_INT, Orders::radius, ByteBufCodecs.BOOL, Orders::inCombat,
				ByteBufCodecs.stringUtf8(48), Orders::note, ByteBufCodecs.VAR_LONG, Orders::recallAt, ByteBufCodecs.BOOL, Orders::open, Orders::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** An order (client to server): to the leader (his entity id), which ({@link Legionary#FOLLOW} ..), and the patrol's
	 * radius. */
	public record Order(int leader, int order, int radius) implements CustomPacketPayload {
		public static final Type<Order> TYPE = new Type<>(Jugcraft.id("squad_order"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Order> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Order::leader,
				ByteBufCodecs.VAR_INT, Order::order, ByteBufCodecs.VAR_INT, Order::radius, Order::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Order.TYPE, Order.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Orders.TYPE, Orders.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Order.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.level().getEntity(payload.leader()) instanceof Legionary leader) {
				int result = SquadOrders.order(player, leader, payload.order(), payload.radius());
				if (result != SquadOrders.NOT_YOURS && result != SquadOrders.TOO_FAR) {
					SquadOrders.page(player, leader, false, SquadOrders.NOTES[result]);
				}
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(Mark.TYPE, Mark.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Buy.TYPE, Buy.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Marked.TYPE, Marked.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Account.TYPE, Account.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Page.TYPE, Page.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Mark.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Vec3 at = payload.at();
			if (!Double.isFinite(at.x) || !Double.isFinite(at.y) || !Double.isFinite(at.z)) {
				return;
			}
			int result = SquadDrop.mark(player, at, payload.size());
			if (result == SquadDrop.OK) {
				player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), JugcraftSquads.MARK, SoundSource.PLAYERS, 0.8F, 1.0F);
			}
			ServerPlayNetworking.send(player, new Marked(result, at, payload.size()));
			sendAccount(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(Buy.TYPE, (payload, context) -> Requisitions.buy(context.player(), payload.desk(), payload.index()));
	}

	/** Tells {@code player} their account (their sight's support pick shows it). */
	public static void sendAccount(ServerPlayer player) {
		SquadAccounts accounts = SquadAccounts.get(player.level());
		ServerPlayNetworking.send(player, new Account(list(accounts.charges(player.getUUID())), accounts.inField(player.level().getServer(), player.getUUID())));
	}

	static List<Integer> list(int[] values) {
		List<Integer> out = new ArrayList<>(values.length);
		for (int v : values) {
			out.add(v);
		}
		return out;
	}
}

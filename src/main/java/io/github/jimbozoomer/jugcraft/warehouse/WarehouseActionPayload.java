package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A button on a warehouse screen (part W2; client to server): place an order (the terminal's REQUEST or the kiosk's),
 * cancel one or send it to pipes, switch the Goods Out panel's page, the Output Port's mode and streams, the pickup
 * window's search. pos is the terminal or the Goods Out panel the screen belongs to; the server checks reach.
 */
public record WarehouseActionPayload(BlockPos pos, int action, ItemStack item, long amount, int arg, long extra, String text) implements CustomPacketPayload {
	public static final int ORDER = 0, CANCEL = 1, TO_PIPES = 2, PAGE = 3, PORT_MODE = 4, STREAM_ADD = 5, STREAM_SET = 6, STREAM_REMOVE = 7, SEARCH = 8;
	/** The BUILDING page (part W3): ADD A FLOOR (arg 1: build with the party's items), cancel it, switch what it is built with. */
	public static final int ADD_FLOOR = 9, CANCEL_FLOOR = 10, FLOOR_MODE = 11;
	/** Part W4: the Liquids Out flanges' mode; a liquid order ready at the fill point sent to pipes instead. */
	public static final int LIQUID_MODE = 12, LIQUID_TO_PIPES = 13, LSTREAM_SET = 14, LSTREAM_REMOVE = 15;
	/**
	 * Part W5, the CONTROL and LAYOUT pages: a job group up or down (arg the group, amount -1 / +1), consolidation's and
	 * cycle counts' modes (arg), PERSONAL / PARTY (arg 1), PAUSE ALL DRONES, a drone's button (arg its id, amount the
	 * action: Terminal droneAction), a circuit's breaker (arg), RUSH an order (arg its id).
	 */
	public static final int PRIORITY = 16, CONSOL_MODE = 17, COUNT_MODE = 18, ACCESS = 19, PAUSE_ALL = 20, DRONE = 21, CIRCUIT = 22, RUSH = 23;
	public static final double REACH_SQR = 10 * 10;
	public static final Type<WarehouseActionPayload> TYPE = new Type<>(Jugcraft.id("warehouse_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WarehouseActionPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, WarehouseActionPayload::pos, ByteBufCodecs.VAR_INT, WarehouseActionPayload::action,
			ItemStack.OPTIONAL_STREAM_CODEC, WarehouseActionPayload::item, ByteBufCodecs.VAR_LONG, WarehouseActionPayload::amount,
			ByteBufCodecs.VAR_INT, WarehouseActionPayload::arg, ByteBufCodecs.VAR_LONG, WarehouseActionPayload::extra,
			ByteBufCodecs.stringUtf8(64), WarehouseActionPayload::text, WarehouseActionPayload::new);

	public static WarehouseActionPayload of(BlockPos pos, int action, int arg) {
		return new WarehouseActionPayload(pos, action, ItemStack.EMPTY, 0, arg, 0, "");
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos pos = payload.pos();
			if (!player.level().isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR) {
				return;
			}
			apply(player, payload);
		});
	}

	/** Carries out a screen's action for a player (also used by tests). */
	public static boolean apply(ServerPlayer player, WarehouseActionPayload p) {
		var level = player.level();
		TerminalBlockEntity t = level.getBlockEntity(p.pos()) instanceof TerminalBlockEntity term ? term
				: level.getBlockEntity(p.pos()) instanceof GoodsOutBlockEntity goods ? goods.terminal()
				: level.getBlockEntity(p.pos()) instanceof LiquidsOutBlockEntity liquids ? liquids.terminal() : null;
		if (t != null && !t.primary()) {
			// Another terminal joined to the same warehouse runs it: the order goes to that one.
			TerminalBlockEntity runs = Warehouses.terminalOf(level, t.getBlockPos());
			t = runs != null ? runs : t;
		}
		if (t == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.no_terminal"));
			return false;
		}
		long now = level.getGameTime();
		// Part W5: a PERSONAL warehouse is its owner's alone; a PARTY one their party's too.
		if (!t.mayUse(player)) {
			t.deny(player);
			return false;
		}
		switch (p.action()) {
			case ORDER -> {
				if (t.openOrders(player.getUUID()) >= TerminalBlockEntity.MAX_OPEN) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.too_many", TerminalBlockEntity.MAX_OPEN));
					return false;
				}
				String from = level.getBlockEntity(p.pos()) instanceof GoodsOutBlockEntity ? "KIOSK" : "TERMINAL";
				Order o = t.placeOrder(player.getUUID(), player.getName().getString(), p.item(), Math.max(1, p.amount()), p.arg(), from, now);
				if (o == null) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.not_stocked"));
					return false;
				}
				if (Drums.isLiquid(o.item)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.liquid_ordered", o.id, Drums.buckets(o.amount), Drums.name(o.item)));
				} else {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.ordered", o.id, o.item.getHoverName(), String.format(java.util.Locale.ROOT, "%,d", o.amount)));
				}
				return true;
			}
			case CANCEL -> {
				if (t.cancel(p.arg(), player)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.cancelled", p.arg()));
					return true;
				}
				return false;
			}
			case TO_PIPES -> {
				return t.toPipes(p.arg(), player);
			}
			case PAGE -> {
				if (level.getBlockEntity(p.pos()) instanceof GoodsOutBlockEntity goods) {
					goods.open(player, p.arg());
					return true;
				}
				return false;
			}
			case PORT_MODE -> t.setPortMode(p.arg());
			case LIQUID_MODE -> t.setLiquidMode(p.arg());
			case LSTREAM_SET -> t.setLiquidStream(p.arg(), (int) p.amount(), p.extra());
			case LSTREAM_REMOVE -> t.removeLiquidStream(p.arg());
			case LIQUID_TO_PIPES -> {
				return t.liquidToPipes(p.arg(), player);
			}
			case STREAM_ADD -> {
				return t.addStream(p.item(), (int) p.amount(), p.extra(), player);
			}
			case STREAM_SET -> t.setStream(p.arg(), (int) p.amount(), p.extra());
			case STREAM_REMOVE -> t.removeStream(p.arg());
			// The building's pages belong to the terminal used (the one in the pre-made building), not the one running it.
			case ADD_FLOOR -> {
				return WarehouseBuilding.addFloor(player.level(), level.getBlockEntity(p.pos()) instanceof TerminalBlockEntity own ? own : t, player,
						p.arg() == 1);
			}
			case CANCEL_FLOOR -> {
				return WarehouseBuilding.cancel(player.level(), level.getBlockEntity(p.pos()) instanceof TerminalBlockEntity own ? own : t, player);
			}
			case FLOOR_MODE -> {
				return WarehouseBuilding.setMode(player.level(), level.getBlockEntity(p.pos()) instanceof TerminalBlockEntity own ? own : t, player,
						p.arg() == 1);
			}
			case PRIORITY -> t.movePriority(p.arg(), p.amount() < 0 ? -1 : 1);
			case CONSOL_MODE -> t.setConsolidateMode(p.arg());
			case COUNT_MODE -> t.setCountMode(p.arg());
			case ACCESS -> {
				return t.setUseMode(player, p.arg() == 1);
			}
			case PAUSE_ALL -> t.setFleetPaused(!t.fleetPaused());
			case DRONE -> {
				return t.droneAction(p.arg(), (int) p.amount());
			}
			case CIRCUIT -> t.toggleCircuit(p.arg());
			case RUSH -> {
				return t.rush(p.arg(), player);
			}
			case SEARCH -> {
				if (player.containerMenu instanceof GoodsOutMenu menu) {
					menu.setSearch(p.text());
					menu.broadcastChanges();
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}
}

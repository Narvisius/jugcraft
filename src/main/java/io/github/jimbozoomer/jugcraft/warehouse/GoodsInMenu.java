package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The GOODS IN screen (client/warehouse/GoodsInScreen.java): the Intake Case Packer's 54-slot drop-off bay over the
 * player's inventory, ordinary slots (click, shift-click), with INSERT ALL, INSERT MATCHING (only what the warehouse
 * already stores, or the bay holds) and TAKE ALL BACK. Opened from the Goods In panel outside or the intake itself.
 */
public class GoodsInMenu extends AbstractContainerMenu {
	/** The screen's size and where its slots are (the mockup's layout, GUI pixels). */
	public static final int WIDTH = 352, HEIGHT = 280, BAY_X = 13, BAY_Y = 40, INV_X = 13, INV_Y = 197, HOTBAR_Y = 255;
	public static final int INSERT_ALL = 0, INSERT_MATCHING = 1, TAKE_ALL = 2;
	private final Container bay;
	public final BlockPos intake;
	public final @Nullable BlockPos panel;
	private final Player player;
	private int ticks;

	/** What the client needs to open it: the intake, and the panel it was opened from (or the intake again). */
	public record Opening(BlockPos intake, BlockPos panel) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::intake, BlockPos.STREAM_CODEC, Opening::panel, Opening::new);
	}

	public GoodsInMenu(int id, Inventory inventory, Opening opening) {
		this(id, inventory, new SimpleContainer(IntakeBlockEntity.BAY), opening.intake(), opening.panel());
	}

	public GoodsInMenu(int id, Inventory inventory, Container bay, BlockPos intake, @Nullable BlockPos panel) {
		super(JugcraftWarehouse.GOODS_IN_MENU, id);
		this.bay = bay;
		this.intake = intake;
		this.panel = panel;
		this.player = inventory.player;
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 9; c++) {
				addSlot(new Slot(bay, r * 9 + c, BAY_X + c * 18, BAY_Y + r * 18));
			}
		}
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				addSlot(new Slot(inventory, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
			}
		}
		for (int c = 0; c < 9; c++) {
			addSlot(new Slot(inventory, c, INV_X + c * 18, HOTBAR_Y));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		BlockPos at = panel != null ? panel : intake;
		return !player.isSpectator() && player.level().isLoaded(intake)
				&& player.level().getBlockEntity(intake) instanceof IntakeBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(at)) <= 10 * 10;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int bayEnd = IntakeBlockEntity.BAY;
		if (index < bayEnd) {
			if (!moveItemStackTo(stack, bayEnd, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, bayEnd, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer) || !stillValid(player)) {
			return false;
		}
		int bayEnd = IntakeBlockEntity.BAY;
		if (id == TAKE_ALL) {
			for (int i = 0; i < bayEnd; i++) {
				ItemStack s = slots.get(i).getItem();
				if (!s.isEmpty()) {
					moveItemStackTo(s, bayEnd, slots.size(), true);
					slots.get(i).setChanged();
				}
			}
		} else if (id == INSERT_ALL || id == INSERT_MATCHING) {
			TerminalBlockEntity terminal = Warehouses.terminalOf(player.level(), intake);
			// The main inventory only (not the hotbar), as other mods' insert-all buttons do.
			for (int i = bayEnd; i < bayEnd + 27; i++) {
				ItemStack s = slots.get(i).getItem();
				if (s.isEmpty() || id == INSERT_MATCHING && !matches(s, terminal)) {
					continue;
				}
				moveItemStackTo(s, 0, bayEnd, false);
				slots.get(i).setChanged();
			}
		}
		broadcastChanges();
		return true;
	}

	private boolean matches(ItemStack s, @Nullable TerminalBlockEntity terminal) {
		if (terminal != null && terminal.stock().containsKey(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(s))) {
			return true;
		}
		for (int i = 0; i < bay.getContainerSize(); i++) {
			if (ItemStack.isSameItemSameComponents(bay.getItem(i), s)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (player instanceof ServerPlayer server && ticks++ % 10 == 0) {
			ServerPlayNetworking.send(server, status(server));
		}
	}

	private GoodsInPayload status(ServerPlayer server) {
		long now = server.level().getGameTime();
		ItemStack packing = ItemStack.EMPTY;
		float progress = 0;
		int conveyor = 0;
		if (server.level().getBlockEntity(intake) instanceof IntakeBlockEntity in) {
			if (in.packing() != null) {
				packing = in.packing().item().copyWithCount(Math.min(99, Math.max(1, in.packing().count())));
				progress = Math.min(1, (now - in.packStart()) / (float) IntakeBlockEntity.PACK_TICKS);
			}
			conveyor = in.conveyor().size();
		}
		List<GoodsInPayload.Inlet> inlets = new ArrayList<>();
		if (panel != null && server.level().getBlockEntity(panel) instanceof GoodsInBlockEntity goods) {
			for (int i = 0; i < GoodsInBlockEntity.INLETS; i++) {
				inlets.add(new GoodsInPayload.Inlet(piped(server, goods, i), goods.last(i), goods.rate(i), goods.backed(i)));
			}
		}
		TerminalBlockEntity terminal = Warehouses.terminalOf(server.level(), intake);
		int building = 0, used = 0, total = 0;
		List<TerminalPayload.Logged> log = new ArrayList<>();
		if (terminal != null) {
			for (Network.Station st : terminal.network().stations) {
				if (server.level().getBlockEntity(st.origin()) instanceof StationBlockEntity s
						&& (s.state() == StationBlockEntity.BUILDING || s.state() == StationBlockEntity.WRAPPING) && s.build() != null && !s.build().isEmpty()) {
					building++;
				}
			}
			used = terminal.placesUsed();
			total = terminal.placesTotal();
			for (TerminalBlockEntity.PutAway p : terminal.log()) {
				log.add(new TerminalPayload.Logged(p.time(), p.item(), p.count(), p.place()));
			}
		}
		return new GoodsInPayload(packing, progress, conveyor, building, used, total, inlets, log, terminal != null, server.level().getOverworldClockTime());
	}

	/** Whether a pipe is joined to an inlet's face. */
	private static boolean piped(ServerPlayer server, GoodsInBlockEntity goods, int inlet) {
		if (!(goods.getBlockState().getBlock() instanceof PieceBlock piece)) {
			return false;
		}
		net.minecraft.core.Direction facing = goods.getBlockState().getValue(PieceBlock.FACING);
		int part = piece.piece.part(inlet % 2 == 0 ? 0 : 2, inlet / 2, 0);
		BlockPos at = goods.getBlockPos().offset(piece.piece.offset(facing, part)).relative(facing.getOpposite());
		return server.level().getBlockState(at).getBlock() instanceof io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock;
	}
}

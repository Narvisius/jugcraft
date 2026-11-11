package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
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

/**
 * The LIQUIDS IN screen (part W4; client/warehouse/LiquidsInScreen.java): the Liquids In panel's TIP BUCKETS slots
 * over the player's inventory (TIP ALL, TAKE BACK), the inlets and what the Drum Filling Station behind it is doing.
 */
public class LiquidsInMenu extends AbstractContainerMenu {
	public static final int WIDTH = 352, HEIGHT = 256, TIP_X = 13, TIP_Y = 150, INV_X = 13, INV_Y = 172, HOTBAR_Y = 230;
	public static final int TIP_ALL = 0, TAKE_BACK = 1;
	private final Container funnel;
	public final BlockPos panel, filler;
	private final Player player;
	private int ticks;

	public record Opening(BlockPos panel, BlockPos filler) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::panel, BlockPos.STREAM_CODEC, Opening::filler, Opening::new);
	}

	public LiquidsInMenu(int id, Inventory inventory, Opening opening) {
		this(id, inventory, new SimpleContainer(LiquidsInBlockEntity.SLOTS), opening.panel(), opening.filler());
	}

	public LiquidsInMenu(int id, Inventory inventory, Container funnel, BlockPos panel, BlockPos filler) {
		super(JugcraftWarehouse.LIQUIDS_IN_MENU, id);
		this.funnel = funnel;
		this.panel = panel;
		this.filler = filler;
		this.player = inventory.player;
		for (int c = 0; c < LiquidsInBlockEntity.SLOTS; c++) {
			addSlot(new Slot(funnel, c, TIP_X + c * 18, TIP_Y));
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
		return !player.isSpectator() && player.level().isLoaded(panel) && player.level().getBlockEntity(panel) instanceof LiquidsInBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(panel)) <= 10 * 10;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int end = LiquidsInBlockEntity.SLOTS;
		if (index < end) {
			if (!moveItemStackTo(stack, end, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, end, false)) {
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
		int end = LiquidsInBlockEntity.SLOTS;
		if (id == TAKE_BACK) {
			for (int i = 0; i < end; i++) {
				ItemStack s = slots.get(i).getItem();
				if (!s.isEmpty()) {
					moveItemStackTo(s, end, slots.size(), true);
					slots.get(i).setChanged();
				}
			}
		} else if (id == TIP_ALL && player.level().getBlockEntity(panel) instanceof LiquidsInBlockEntity in) {
			in.tipAll(player);
		}
		broadcastChanges();
		return true;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (player instanceof ServerPlayer server && ticks++ % 10 == 0) {
			ServerPlayNetworking.send(server, status(server));
		}
	}

	private LiquidsPayload status(ServerPlayer server) {
		List<LiquidsPayload.Inlet> inlets = new ArrayList<>();
		boolean linked = false;
		if (server.level().getBlockEntity(panel) instanceof LiquidsInBlockEntity in) {
			linked = in.linked();
			for (int i = 0; i < LiquidsInBlockEntity.INLETS; i++) {
				inlets.add(new LiquidsPayload.Inlet(in.piped(i), in.last(i), in.rate(i), in.waits(i)));
			}
		}
		LiquidsPayload.Station station = server.level().getBlockEntity(filler) instanceof FillerBlockEntity f ? station(f, server.level().getGameTime())
				: LiquidsPayload.Station.NONE;
		return new LiquidsPayload(true, linked, inlets, station, List.of(), FluidVariant.blank(), 0, 0, 0, 0, 0, List.of(), List.of());
	}

	/** The filling station as the screens show it. */
	static LiquidsPayload.Station station(FillerBlockEntity f, long now) {
		List<Integer> drums = new ArrayList<>();
		for (Drums.Kind k : Drums.Kind.values()) {
			drums.add(f.drums(k));
		}
		int drum = 0;
		long filled = 0;
		FillerMotion.Plan plan = f.plan();
		if (plan != null && plan.type() == FillerMotion.FILL) {
			drum = plan.spot() + 1;
			filled = (long) (plan.amount() * FillerMotion.at(plan, now - plan.start()).filled());
		}
		String note = "";
		Drums.Kind kind = f.liquid().isBlank() ? null : Drums.kindFor(f.liquid());
		if (kind != null && f.drums(kind) == 0 && plan == null) {
			note = "NO EMPTY " + kind.name().replace("RED", "FLAMMABLES").replace("POLY", "HDPE") + " DRUMS";
		} else if (f.pallets() == 0 && f.state() == FillerBlockEntity.EMPTY && !f.liquid().isBlank()) {
			note = "NO EMPTY PALLETS";
		} else if (f.state() == FillerBlockEntity.READY) {
			note = "PALLET READY FOR A DRONE";
		}
		return new LiquidsPayload.Station(f.state(), f.liquid(), f.tank(), drum, filled, drums, f.pallets(), f.freePlaces(), note);
	}
}

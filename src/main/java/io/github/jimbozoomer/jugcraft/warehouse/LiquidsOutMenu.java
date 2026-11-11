package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The LIQUIDS OUT screen (part W4; client/warehouse/LiquidsOutScreen.java): no slots. The player's liquid orders, the
 * fill point (FILL MY BUCKETS fills every empty container they carry from what is ready for them), the OUT flange's mode
 * and the liquid streams. Orders are placed at the terminal (STOCK, the LIQUIDS filter).
 */
public class LiquidsOutMenu extends AbstractContainerMenu {
	public static final int FILL_ALL = 0;
	public final BlockPos panel, terminal;
	private final Player player;
	private int ticks;

	public record Opening(BlockPos panel, BlockPos terminal) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::panel, BlockPos.STREAM_CODEC, Opening::terminal, Opening::new);
	}

	public LiquidsOutMenu(int id, Inventory inventory, Opening opening) {
		super(JugcraftWarehouse.LIQUIDS_OUT_MENU, id);
		this.panel = opening.panel();
		this.terminal = opening.terminal();
		this.player = inventory.player;
	}

	@Override
	public boolean stillValid(Player player) {
		return !player.isSpectator() && player.level().isLoaded(panel) && player.level().getBlockEntity(panel) instanceof LiquidsOutBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(panel)) <= 10 * 10;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer) || !stillValid(player)) {
			return false;
		}
		if (id == FILL_ALL && player.level().getBlockEntity(panel) instanceof LiquidsOutBlockEntity out && out.fillAll(player) <= 0) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.warehouse.nothing_to_fill"));
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
		long now = server.level().getGameTime();
		LiquidsOutBlockEntity out = server.level().getBlockEntity(panel) instanceof LiquidsOutBlockEntity o ? o : null;
		TerminalBlockEntity t = out == null ? null : out.terminal();
		EmptierBlockEntity e = out == null ? null : out.emptier();
		List<TerminalPayload.OrderLine> orders = new ArrayList<>();
		List<LiquidsPayload.LiquidStream> streams = new ArrayList<>();
		List<LiquidsPayload.Stocked> stocked = new ArrayList<>();
		int mode = 0;
		if (t != null) {
			for (Order o : t.orders()) {
				if (Drums.isLiquid(o.item) && o.stream == 0 && player.getUUID().equals(o.owner) && orders.size() < 24) {
					orders.add(t.line(o, now));
				}
			}
			for (int k = 0; k < t.liquidStreams().size(); k++) {
				Order.Stream st = t.liquidStreams().get(k);
				streams.add(new LiquidsPayload.LiquidStream(k, st.item.copy(), st.rate, st.reserve, st.held));
			}
			for (TerminalBlockEntity.Stock st : t.stock().values()) {
				if (Drums.isLiquid(st.item) && stocked.size() < 64) {
					stocked.add(new LiquidsPayload.Stocked(st.item.copy(), st.count));
				}
			}
			mode = t.liquidMode();
		}
		long mine = 0;
		if (e != null) {
			for (EmptierBlockEntity.Share s : e.shares()) {
				if (s.dest == Order.WINDOW && (s.owner == null || s.owner.equals(player.getUUID()))) {
					mine += Drums.mb(s.amount);
				}
			}
		}
		return new LiquidsPayload(false, out != null && out.linked(), List.of(), LiquidsPayload.Station.NONE, orders,
				e == null ? FluidVariant.blank() : e.liquid(), e == null ? 0 : e.level(), EmptierBlockEntity.TANK, mine, mode,
				out == null ? 0 : out.flow(), streams, stocked);
	}
}

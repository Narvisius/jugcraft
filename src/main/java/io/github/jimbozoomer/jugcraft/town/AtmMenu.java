package io.github.jimbozoomer.jugcraft.town;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Jug Teller's screen: the player's Jugs, and sending Jugs to another player. When it opens, the server lists the
 * players online (not the user; at most {@link #MAX_NAMES}) and remembers who each is; the client shows the names. The
 * amount and the chosen name live on the server and come down as menu data; every change is a menu button:
 * {@code 0..63} chooses a name, {@link #ADD} {@code +0..3} adds 1, 10, 100 or 1,000, {@link #SUBTRACT} {@code +0..3}
 * takes them away, {@link #CLEAR} empties it, {@link #ALL} puts in everything, {@link #SEND} sends. Sending is checked
 * on the server: the ATM still there and in reach ({@link TownShops#atmRange}), the recipient still online, the amount
 * between 1 and the sender's Jugs, the recipient's balance under the maximum, and one send per {@link #MIN_TICKS} ticks.
 */
public class AtmMenu extends AbstractContainerMenu {
	public static final int MAX_NAMES = 64;
	public static final int ADD = 100;
	public static final int SUBTRACT = 104;
	public static final int CLEAR = 108;
	public static final int ALL = 109;
	public static final int SEND = 110;
	public static final int MIN_TICKS = 10;
	private static final int[] STEPS = {1, 10, 100, 1000};
	private static final Map<UUID, Long> LAST_SEND = new HashMap<>();

	public record Opening(BlockPos atm, List<String> names) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::atm, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Opening::names, Opening::new);
	}

	public final Opening opening;
	private final ContainerData data;
	private final List<UUID> recipients;
	private int amount;
	private int chosen = -1;

	/** Client side. */
	public AtmMenu(int containerId, Inventory inventory, Opening opening) {
		this(containerId, inventory, opening, new SimpleContainerData(5), List.of());
	}

	private AtmMenu(int containerId, Inventory inventory, Opening opening, ContainerData data, List<UUID> recipients) {
		super(JugcraftTown.ATM_MENU, containerId);
		this.opening = opening;
		this.recipients = recipients;
		this.data = data;
		addDataSlots(data);
	}

	/** Opens the ATM at {@code pos} for a player (server side). */
	public static void open(ServerPlayer player, BlockPos pos) {
		MinecraftServer server = player.level().getServer();
		List<ServerPlayer> others = new ArrayList<>(server.getPlayerList().getPlayers());
		others.removeIf(other -> other.getUUID().equals(player.getUUID()));
		others.sort(Comparator.comparing(other -> other.getName().getString()));
		if (others.size() > MAX_NAMES) {
			others = others.subList(0, MAX_NAMES);
		}
		List<String> names = others.stream().map(other -> other.getName().getString()).toList();
		List<UUID> ids = others.stream().map(ServerPlayer::getUUID).toList();
		Opening opening = new Opening(pos.immutable(), names);
		player.openMenu(new ExtendedMenuProvider<Opening>() {
			@Override
			public Opening getScreenOpeningData(ServerPlayer p) {
				return opening;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("block.jugcraft.atm");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				AtmMenu[] menu = new AtmMenu[1];
				ContainerData data = new ContainerData() {
					@Override
					public int get(int index) {
						long balance = Jugs.balance(server, p.getUUID());
						return switch (index) {
							case 0 -> (int) (balance & 0xFFFF);
							case 1 -> (int) ((balance >>> 16) & 0xFFFF);
							case 2 -> menu[0].amount & 0xFFFF;
							case 3 -> (menu[0].amount >>> 16) & 0xFFFF;
							default -> menu[0].chosen + 1;
						};
					}

					@Override
					public void set(int index, int value) {
					}

					@Override
					public int getCount() {
						return 5;
					}
				};
				menu[0] = new AtmMenu(containerId, inventory, opening, data, ids);
				return menu[0];
			}
		});
	}

	public long balance() {
		return (data.get(0) & 0xFFFFL) | ((data.get(1) & 0xFFFFL) << 16);
	}

	public int amount() {
		return (data.get(2) & 0xFFFF) | ((data.get(3) & 0xFFFF) << 16);
	}

	/** The chosen name's index, or -1. */
	public int chosen() {
		return data.get(4) - 1;
	}

	@Override
	public boolean stillValid(Player player) {
		if (player.level().isClientSide()) {
			return true;
		}
		double range = TownShops.get().atmRange;
		return player.level().getBlockState(opening.atm()).getBlock() instanceof AtmBlock
				&& player.distanceToSqr(opening.atm().getX() + 0.5, opening.atm().getY() + 0.5, opening.atm().getZ() + 0.5) <= range * range;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer server) || !stillValid(player)) {
			return false;
		}
		long balance = Jugs.balance(server.level().getServer(), server.getUUID());
		int cap = (int) Math.min(Integer.MAX_VALUE, balance);
		if (id >= 0 && id < MAX_NAMES) {
			if (id >= recipients.size()) {
				return false;
			}
			chosen = id;
		} else if (id >= ADD && id < ADD + 4) {
			amount = (int) Math.min(cap, (long) amount + STEPS[id - ADD]);
		} else if (id >= SUBTRACT && id < SUBTRACT + 4) {
			amount = Math.max(0, amount - STEPS[id - SUBTRACT]);
		} else if (id == CLEAR) {
			amount = 0;
		} else if (id == ALL) {
			amount = cap;
		} else if (id == SEND) {
			return send(server);
		} else {
			return false;
		}
		broadcastChanges();
		return true;
	}

	private boolean send(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		long now = player.level().getGameTime();
		Long last = LAST_SEND.get(player.getUUID());
		if (last != null && now - last < MIN_TICKS && now >= last) {
			return false;
		}
		LAST_SEND.put(player.getUUID(), now);
		if (chosen < 0 || chosen >= recipients.size() || amount <= 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.atm.choose"));
			return false;
		}
		ServerPlayer recipient = server.getPlayerList().getPlayer(recipients.get(chosen));
		if (recipient == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.atm.offline", opening.names().get(chosen)));
			return false;
		}
		int sent = amount;
		if (!Jugs.transfer(server, player.getUUID(), recipient.getUUID(), sent)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.atm.refused"));
			return false;
		}
		amount = 0;
		broadcastChanges();
		player.sendSystemMessage(Component.translatable("message.jugcraft.atm.sent", sent, recipient.getDisplayName()));
		recipient.sendSystemMessage(Component.translatable("message.jugcraft.atm.received", sent, player.getDisplayName()));
		player.level().playSound(null, opening.atm(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
		return true;
	}

	public static void clear() {
		LAST_SEND.clear();
	}

	/** Sends Jugs as the ATM would, for tests: false if refused. */
	public static boolean sendForTest(ServerPlayer from, @Nullable ServerPlayer to, int amount) {
		return to != null && Jugs.transfer(from.level().getServer(), from.getUUID(), to.getUUID(), amount);
	}
}

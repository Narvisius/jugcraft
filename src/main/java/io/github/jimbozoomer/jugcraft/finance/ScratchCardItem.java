package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A scratch card ({@link ShowGames.ScratchCard}), bought at a lottery kiosk. Using one scratches it: the server draws
 * the prize, the central bank pays it at once, and the card's screen lets the player rub off its nine panels (three of
 * one amount win that amount). The panels are only a show of the prize already drawn.
 */
public class ScratchCardItem extends Item {
	private static final Random RANDOM = new Random();
	private final ShowGames.ScratchCard card;

	public ScratchCardItem(Properties properties, ShowGames.ScratchCard card) {
		super(properties);
		this.card = card;
	}

	public ShowGames.ScratchCard card() {
		return card;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer server) {
			MinecraftServer mc = server.level().getServer();
			ItemStack ticket=player.getItemInHand(hand);
            if(!ScratchTickets.get(mc).redeem(ticket,card.ordinal())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This is a blank or already redeemed card. Buy a paid ticket at a lottery kiosk."));
                return InteractionResult.FAIL;
            }
            Ledger ledger = Ledger.get(mc);
			FinanceBook book = ledger.book();
			book.account(server.getUUID(), server.getName().getString());
			long prize = card.draw(RANDOM);
			long paid = 0;
			if (prize > 0) {
				String text = prize >= card.price * 40 ? server.getName().getString() + " won " + FinanceBook.amount(prize)
						+ " Jugs on a " + card.title + "!" : null;
				paid = book.bankPays(server.getUUID(), prize, FinanceService.day(mc), text);
				ledger.setDirty();
				FinanceService.announce(mc, ledger);
			}
			ItemStack stack = player.getItemInHand(hand);
			stack.consume(1, player);
			ServerPlayNetworking.send(server, new ScratchPayload(card.ordinal(), paid, panels(card, prize)));
		}
		return InteractionResult.SUCCESS;
	}

	/** Nine panels: three of the prize when there is one; never three of anything otherwise. */
	static List<Long> panels(ShowGames.ScratchCard card, long prize) {
		List<Long> amounts = new ArrayList<>();
		for (int i = 0; i < card.prizes.length; i += 2) {
			amounts.add(card.prizes[i]);
		}
		List<Long> out = new ArrayList<>();
		if (prize > 0) {
			out.add(prize);
			out.add(prize);
			out.add(prize);
		}
		List<Long> others = new ArrayList<>(amounts);
		others.remove(prize);
		Collections.shuffle(others, RANDOM);
		int i = 0;
		while (out.size() < 9) {
			long value = others.get(i % others.size());
			if (Collections.frequency(out, value) < 2) {
				out.add(value);
			}
			i++;
			if (i > 200) {
				out.add(0L);
			}
		}
		Collections.shuffle(out, RANDOM);
		return out;
	}
}

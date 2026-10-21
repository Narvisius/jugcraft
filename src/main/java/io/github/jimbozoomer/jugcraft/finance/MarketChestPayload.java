package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the pricing panel of the Market Chest a player has open. Display only; the server checks every
 * change the player asks for.
 *
 * @param pos       the chest's packed position
 * @param company   "Name (TICKER)", or empty when it sells for nobody yet
 * @param corpId    the company it sells for (0: none)
 * @param choices   companies the player could switch it to
 * @param paused    selling is paused
 * @param rows      each kind of item it deals in
 * @param treasury  the company's treasury
 */
public record MarketChestPayload(long pos, String company, long corpId, List<Choice> choices, boolean paused, List<Row> rows,
		long treasury, String notice, boolean noticeOk) implements CustomPacketPayload {
	public static final int MAX_ROWS = 40;
	public static final int MAX_TEXT = 200;
	public static final Type<MarketChestPayload> TYPE = new Type<>(Jugcraft.id("market_chest_panel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MarketChestPayload> CODEC = StreamCodec.ofMember(
			MarketChestPayload::write, MarketChestPayload::read);

	public record Choice(long id, String label) {
	}

	/** One kind of item: its price (0: not for sale), how many are listed and how many still sit in the chest. */
	public record Row(ItemStack item, String key, long price, int listed, int inChest) {
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarLong(pos);
		buf.writeUtf(company, MAX_TEXT);
		buf.writeVarLong(corpId);
		buf.writeVarInt(Math.min(choices.size(), 32));
		for (int i = 0; i < Math.min(choices.size(), 32); i++) {
			buf.writeVarLong(choices.get(i).id());
			buf.writeUtf(choices.get(i).label(), MAX_TEXT);
		}
		buf.writeBoolean(paused);
		buf.writeVarInt(Math.min(rows.size(), MAX_ROWS));
		for (int i = 0; i < Math.min(rows.size(), MAX_ROWS); i++) {
			Row row = rows.get(i);
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, row.item());
			buf.writeUtf(row.key(), MAX_TEXT);
			buf.writeVarLong(row.price());
			buf.writeVarInt(row.listed());
			buf.writeVarInt(row.inChest());
		}
		buf.writeVarLong(treasury);
		buf.writeUtf(notice, MAX_TEXT);
		buf.writeBoolean(noticeOk);
	}

	private static MarketChestPayload read(RegistryFriendlyByteBuf buf) {
		long pos = buf.readVarLong();
		String company = buf.readUtf(MAX_TEXT);
		long corpId = buf.readVarLong();
		int choiceCount = Math.min(32, buf.readVarInt());
		List<Choice> choices = new ArrayList<>(choiceCount);
		for (int i = 0; i < choiceCount; i++) {
			choices.add(new Choice(buf.readVarLong(), buf.readUtf(MAX_TEXT)));
		}
		boolean paused = buf.readBoolean();
		int rowCount = Math.min(MAX_ROWS, buf.readVarInt());
		List<Row> rows = new ArrayList<>(rowCount);
		for (int i = 0; i < rowCount; i++) {
			ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			rows.add(new Row(item, buf.readUtf(MAX_TEXT), buf.readVarLong(), buf.readVarInt(), buf.readVarInt()));
		}
		long treasury = buf.readVarLong();
		String notice = buf.readUtf(MAX_TEXT);
		boolean ok = buf.readBoolean();
		return new MarketChestPayload(pos, company, corpId, choices, paused, rows, treasury, notice, ok);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

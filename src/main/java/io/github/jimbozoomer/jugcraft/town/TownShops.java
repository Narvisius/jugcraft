package io.github.jimbozoomer.jugcraft.town;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The town's shops and the economy's numbers, from {@code data/jugcraft/town/shops.json} (tools/town_shops.py), read
 * from the mod jar on both sides: the client lists a shop's offers from it, the server prices every trade from it.
 * Prices are whole Jugs for the whole count.
 */
public final class TownShops {
	/** One offer: the item, how many, the price in Jugs, and the themes it is on sale in (null: always). */
	public record Offer(String item, int count, int price, @Nullable List<String> themes) {
		public boolean onSale(String theme) {
			return themes == null || themes.contains(theme);
		}

		public Item itemType() {
			Identifier id = Identifier.tryParse(item);
			Item found = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
			return found == null ? Items.AIR : found;
		}

		public ItemStack stack() {
			Item type = itemType();
			return type == Items.AIR ? ItemStack.EMPTY : new ItemStack(type, count);
		}
	}

	public record Shop(String id, String name, List<Offer> sells, List<Offer> buys) {
		/** The offers for sale in this theme, in order. */
		public List<Offer> sellsIn(String theme) {
			return sells.stream().filter(offer -> offer.onSale(theme)).toList();
		}
	}

	private static @Nullable TownShops instance;

	public final Map<String, Shop> shops;
	public final int welcomeJugs;
	public final long maxBalance;
	public final double tradeRange;
	public final double atmRange;

	private TownShops(JsonObject root) {
		Map<String, Shop> shops = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("shops").entrySet()) {
			JsonObject o = e.getValue().getAsJsonObject();
			shops.put(e.getKey(), new Shop(e.getKey(), o.get("name").getAsString(), offers(o.getAsJsonArray("sells")),
					offers(o.getAsJsonArray("buys"))));
		}
		this.shops = shops;
		JsonObject economy = root.getAsJsonObject("economy");
		welcomeJugs = economy.get("welcome").getAsInt();
		maxBalance = economy.get("max_balance").getAsLong();
		tradeRange = economy.get("trade_range").getAsDouble();
		atmRange = economy.get("atm_range").getAsDouble();
	}

	public static synchronized TownShops get() {
		if (instance == null) {
			String path = "/data/jugcraft/town/shops.json";
			try (InputStream raw = TownShops.class.getResourceAsStream(path)) {
				if (raw == null) {
					throw new IllegalStateException("missing " + path);
				}
				instance = new TownShops(JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the town's shops", e);
				throw new IllegalStateException("Town shops unreadable", e);
			}
		}
		return instance;
	}

	public @Nullable Shop shop(@Nullable String id) {
		return id == null ? null : shops.get(id);
	}

	private static List<Offer> offers(JsonArray array) {
		List<Offer> out = new ArrayList<>();
		for (JsonElement e : array) {
			JsonObject o = e.getAsJsonObject();
			List<String> themes = null;
			if (o.has("themes")) {
				themes = new ArrayList<>();
				for (JsonElement t : o.getAsJsonArray("themes")) {
					themes.add(t.getAsString());
				}
				themes = List.copyOf(themes);
			}
			out.add(new Offer(o.get("item").getAsString(), o.get("count").getAsInt(), o.get("price").getAsInt(), themes));
		}
		return List.copyOf(out);
	}
}

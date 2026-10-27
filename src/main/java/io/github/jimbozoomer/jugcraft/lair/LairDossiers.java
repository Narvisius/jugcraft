package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.AlchemyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Axis;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Product;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Property;
import io.github.jimbozoomer.jugcraft.concordance.rime.Frostward;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload.EdgeKind;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceLoot;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantLoot;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineLoot;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingLoot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The encyclopedia's Expeditions page (Concordance Chunk A, A-04; the Glacier Hall and the Cinder Kiln since Chunk B,
 * B-02): a dossier for each lair the player has found, and nothing for one they have not (A-D9). A lair is found by
 * carrying its token (its rite is then shown: what to make, where and when, and whether it waits for the Halloween
 * season); its keeper by meeting them (being in the lair, being owed its reward) or beating them, when the dossier adds
 * how support earns a share of the reward (what counts in that lair: the keeper's own creatures, healing, a Stillroot
 * Poultice on a friend while the Concordance brews it (Chunk C, C-02), wards and any objective), the trophy a first win
 * brings (one, or one of a pair at random), what the keeper gives (the acquisition text JEI shows, "with the default
 * data": {@code jei.jugcraft.acquire.<item>}, tools/acquisition.py), the cheaper token and the pieces the loot makes, how
 * to leave, and, in a fight, the player's own part in it. The Glacier Hall's rite also names what readies a player for
 * his frost (Chunk B, B-08: Winter Brine and the Yeti Mitten), and the Cinder Kiln's what readies a player for his slag
 * and flame (Chunk C, C-02: the Kiln Draught and the Salamander Charm).
 * <p>
 * Discovery is read, not saved: the inventory, {@link Lairs#VISIT}, {@link LairRewards#OWED} and the first-win
 * advancements. So a player who went in, left, and used up their token (a Cursed Spindle, a Frost Horn, a Kiln Seal)
 * reads as not having found the lair until they carry one again or win; recorded as a known limit. It only reads.
 * <p>
 * With every keeper found and beaten the page draws each recipe it names as a grid: twelve for the four keepers, which
 * is {@link PageWriter#MAX_GRIDS}; tools/check_mod_data.py (check_encyclopedia) refuses a table that would draw more.
 */
public final class LairDossiers {
	public static final String PAGE = "expedition";
	/** The status the Kiln Draught carries: the Cinder Kiln's preparation fact names it only while Ember brews this (C-P9). */
	public static final String FIRE_RESISTANCE = "minecraft:fire_resistance";

	/** How far a player has come with one lair. */
	public enum Discovery {
		UNKNOWN, RITE, MET, DEFEATED
	}

	/**
	 * A lair's keeper, with the tokens that open the lair, what the keeper gives and the rite recipes the loot opens. The
	 * first recipe of {@link #rite} makes a token (tools/check_mod_data.py holds it so; {@link #canOpen} reads it).
	 */
	public enum Boss {
		VESPERINE(Lair.HOLLOW_ACRE, VesperineLoot.BOSS, VesperineLoot.ADVANCEMENT, List.of("vesper_scythe"), "message.jugcraft.lair.rite.off_season",
				List.of("death_knell", "mourning_wreath"), List.of("death_knell", "mourning_wreath", "gothic_headstone_from_stonecutting"),
				List.of(Items.LILY_OF_THE_VALLEY, Items.CANDLE), List.of("reaper_shade", "vesper_scythe", "reaper_hood", "dirge_skull", "requiem_skull"),
				List.of("mourning_wreath_from_shade"), List.of("reaper_hood")),
		TATTERLACE(Lair.SPINDLE_LOFT, TatterlaceLoot.BOSS, TatterlaceLoot.ADVANCEMENT, List.of("needle_rapier"), "message.jugcraft.lair.spindle.off_season",
				List.of("cursed_spindle"), List.of("cursed_spindle", "spinning_wheel"), List.of(),
				List.of("gossamer_silk", "needle_rapier", "golden_thimble", "tatterlace_headdress"), List.of("cursed_spindle_from_silk"), List.of()),
		YETI_KING(Lair.GLACIER_HALL, YetiKingLoot.BOSS, YetiKingLoot.ADVANCEMENT, List.of("glacier_maul", "rimeclaw"), null,
				List.of("frost_horn"), List.of("frost_horn"), List.of(Items.SNOW_BLOCK),
				List.of("yeti_fur", "glacier_maul", "rimeclaw", "yeti_mitten", "yeti_king_crown"), List.of("frost_horn_from_fur"), List.of()),
		CINDER_TYRANT(Lair.CINDER_KILN, CinderTyrantLoot.BOSS, CinderTyrantLoot.ADVANCEMENT, List.of("cinderbrand", "magmaw"), null,
				List.of("kiln_seal"), List.of("kiln_seal"), List.of(),
				List.of("tyrant_scale", "cinderbrand", "magmaw", "salamander_charm", "tyrant_crest"), List.of("kiln_seal_from_scales"), List.of());

		public final Lair lair;
		/** The keeper's id in {@link LairRewards} ("vesperine"), and its entity's. */
		public final String boss;
		public final String advancement;
		/** What a player's first win always brings: one trophy, or one of a pair at random (the keeper's trophy table). */
		public final List<String> trophies;
		/** The words a rite refused out of the Halloween season gives; null for a rite that works all year. */
		public final @Nullable String offSeason;
		/** The items whose carrying shows the lair's rite. */
		public final List<String> tokens;
		/** The recipes the rite needs (each a way to it). */
		public final List<String> rite;
		/** Town sales that help with the rite: each shown only where a town stands, for an offer on sale today. */
		public final List<Item> sales;
		/** What the keeper's table gives, each with its acquisition text ({@code jei.jugcraft.acquire.<item>}, tools/acquisition.py). */
		public final List<String> loot;
		/** Cheaper recipes for a token that the loot opens: optional, after a win. */
		public final List<String> afterWin;
		/** Recipes for a piece made of the loot, which has no other recipe: optional, after a win. */
		public final List<String> madeFrom;

		Boss(Lair lair, String boss, String advancement, List<String> trophies, @Nullable String offSeason, List<String> tokens,
				List<String> rite, List<Item> sales, List<String> loot, List<String> afterWin, List<String> madeFrom) {
			this.lair = lair;
			this.boss = boss;
			this.advancement = advancement;
			this.trophies = trophies;
			this.offSeason = offSeason;
			this.tokens = tokens;
			this.rite = rite;
			this.sales = sales;
			this.loot = loot;
			this.afterWin = afterWin;
			this.madeFrom = madeFrom;
		}
	}

	private LairDossiers() {
	}

	/** Whether {@code player} has beaten {@code boss}: its first-win advancement is theirs. */
	public static boolean beaten(ServerPlayer player, Boss boss) {
		AdvancementHolder win = player.level().getServer().getAdvancements().get(Jugcraft.id(boss.advancement));
		return win != null && player.getAdvancements().getOrStartProgress(win).isDone();
	}

	/** Whether {@code player} has beaten any lair's keeper (the First Light route's lair step). */
	public static boolean anyBeaten(ServerPlayer player) {
		for (Boss boss : Boss.values()) {
			if (beaten(player, boss)) {
				return true;
			}
		}
		return false;
	}

	/** Whether this server has the recipe of any lair's token (each rite's first recipe): a lair can be opened in survival. */
	public static boolean canOpen(PageWriter out) {
		for (Boss boss : Boss.values()) {
			if (out.present(Jugcraft.id(boss.rite.getFirst()))) {
				return true;
			}
		}
		return false;
	}

	/** How far {@code player} has come with {@code boss}'s lair (the furthest that holds). */
	public static Discovery discovery(ServerPlayer player, Boss boss) {
		if (beaten(player, boss)) {
			return Discovery.DEFEATED;
		}
		LairVisit visit = player.getAttached(Lairs.VISIT);
		LairRewards.Owed owed = player.getAttached(LairRewards.OWED);
		if (visit != null && visit.lair().equals(boss.lair.id) || owed != null && owed.due().stream().anyMatch(due -> due.boss().equals(boss.boss))) {
			return Discovery.MET;
		}
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			Item carried = inventory.getItem(slot).getItem();
			for (String token : boss.tokens) {
				if (carried == item(token)) {
					return Discovery.RITE;
				}
			}
		}
		return Discovery.UNKNOWN;
	}

	public static void write(ServerPlayer player, EncyclopediaRequestPayload request, PageWriter out) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return;
		}
		int unknown = 0;
		for (Boss boss : Boss.values()) {
			Discovery discovery = discovery(player, boss);
			if (discovery == Discovery.UNKNOWN) {
				unknown++;
				continue;
			}
			out.heading(name(boss.lair));
			rite(out, boss);
			if (discovery.ordinal() >= Discovery.MET.ordinal()) {
				keeper(player, out, boss, discovery);
			}
		}
		if (unknown > 0 && unknown < Boss.values().length) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.unknown_count", unknown));
		}
	}

	/** How the lair opens: the rite in words with its numbers, the recipes it needs, the sales that help, when it works, and the party it holds. */
	private static void rite(PageWriter out, Boss boss) {
		Component rite = switch (boss) {
			case VESPERINE -> Component.translatable("encyclopedia.jugcraft.dossier.hollow_acre.rite", LastRites.WREATH_RANGE, LastRites.CANDLES,
					LastRites.CANDLE_RANGE, LastRites.GRAVE_RANGE);
			case TATTERLACE -> Component.translatable("encyclopedia.jugcraft.dossier.spindle_loft.rite");
			case YETI_KING -> Component.translatable("encyclopedia.jugcraft.dossier.glacier_hall.rite");
			case CINDER_TYRANT -> Component.translatable("encyclopedia.jugcraft.dossier.cinder_kiln.rite");
		};
		out.fact(rite, null, items(boss.tokens));
		for (String recipe : boss.rite) {
			Item made = result(recipe);
			out.recipe(EdgeKind.REQUIRED, Jugcraft.id(recipe), made == null ? Component.translatable("encyclopedia.jugcraft.route.make", recipe)
					: Component.translatable(recipe.endsWith("_headstone_from_stonecutting") ? "encyclopedia.jugcraft.dossier.grave"
							: "encyclopedia.jugcraft.route.make", made.getDefaultInstance().getHoverName()));
		}
		for (Item sold : boss.sales) {
			out.sales(sold);
		}
		// A Witching Season rite waits for the Halloween event unless lairs.off_season is on; the others keep no season.
		out.fact(boss.offSeason == null ? Component.translatable("encyclopedia.jugcraft.dossier.all_year")
				: Lairs.offSeason() ? Component.translatable("encyclopedia.jugcraft.dossier.any_night")
				: HalloweenSeason.active() ? Component.translatable("encyclopedia.jugcraft.dossier.in_season")
				: Component.translatable(boss.offSeason));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier.party", Lairs.partySize(), Lairs.gateTicks() / 20));
		prepare(out, boss);
	}

	/**
	 * What readies a player for the lair's hazard, as text and icons and never a recipe grid (B-P29: with four keepers the
	 * page's grids are spoken for, {@link PageWriter#MAX_GRIDS}). A brew is named only while the Concordance is on and the
	 * data's property brews its status (its longest time the data's), with a codex link once the Alembic Arts are
	 * observed; the keeper's own lasting answer from his loot beside it, or alone. The Glacier Hall (Chunk B, B-08):
	 * Winter Brine's Frostward and the Yeti Mitten. The Cinder Kiln (Chunk C, C-02, C-P9): the Kiln Draught's Fire
	 * Resistance, which his slag and flame respect, and the Salamander Charm. It only reads.
	 */
	private static void prepare(PageWriter out, Boss boss) {
		boolean concordance = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		if (boss == Boss.YETI_KING) {
			Property rime = concordance ? Alchemy.catalog().property(Axis.RIME.id) : null;
			if (!brews(rime, Frostward.STATUS)) {
				out.fact(Component.translatable("encyclopedia.jugcraft.dossier.glacier_hall.prepare.mitten"), null, items(List.of("yeti_mitten")));
				return;
			}
			List<Item> icons = new ArrayList<>(List.of(Items.SNOWBALL));
			icons.addAll(items(List.of("crucible", "draught", "yeti_mitten")));
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.glacier_hall.prepare", rime.maxTicks() / 20), null, icons);
			out.codex("alembic/winter_brine", "jugcraft:alembic_arts", ResearchState.OBSERVED);
		} else if (boss == Boss.CINDER_TYRANT) {
			Property ember = concordance ? Alchemy.catalog().property(Axis.EMBER.id) : null;
			if (!brews(ember, FIRE_RESISTANCE)) {
				out.fact(Component.translatable("encyclopedia.jugcraft.dossier.cinder_kiln.prepare.charm"), null, items(List.of("salamander_charm")));
				return;
			}
			List<Item> icons = new ArrayList<>(List.of(Items.MAGMA_CREAM, Items.SUGAR));
			icons.addAll(items(List.of("crucible", "draught", "salamander_charm")));
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.cinder_kiln.prepare", ember.maxTicks() / 20), null, icons);
			out.codex("alembic/kiln_draught", "jugcraft:alembic_arts", ResearchState.OBSERVED);
		}
	}

	/** Whether {@code property} (null: the Concordance off, or no such property in the data) brews {@code status}. */
	public static boolean brews(@Nullable Property property, String status) {
		return property != null && status.equals(property.status());
	}

	/** The keeper, once met: how support earns a share, the first win and the loot, the recipes it opens, leaving, and the fight. */
	private static void keeper(ServerPlayer player, PageWriter out, Boss boss, Discovery discovery) {
		out.heading(Component.translatable("entity.jugcraft." + boss.boss));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier." + boss.lair.id + ".credit", number(Encounter.HEALING),
				number(Encounter.WARDING), (int) Encounter.REACH));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier.credit.wards"));
		// Concordance Chunk C (C-02): a Stillroot Poultice on a friend is healing too, named while the Concordance brews it.
		if (poultices()) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.credit.poultice"), null, items(List.of("salve")));
		}
		List<Item> trophies = items(boss.trophies);
		if (discovery == Discovery.DEFEATED) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.won",
					Component.translatable("advancements.jugcraft." + boss.advancement + ".title")));
		} else if (trophies.size() == 1) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.first_win", trophies.getFirst().getDefaultInstance().getHoverName()),
					null, trophies);
		} else if (trophies.size() > 1) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.first_win.pair", trophies.get(0).getDefaultInstance().getHoverName(),
					trophies.get(1).getDefaultInstance().getHoverName()), null, trophies);
		}
		for (String loot : boss.loot) {
			Item item = item(loot);
			if (item != null) {
				out.fact(Component.translatable("jei.jugcraft.acquire." + loot), null, List.of(item));
			}
		}
		for (String recipe : boss.afterWin) {
			Item made = result(recipe);
			out.recipe(EdgeKind.OPTIONAL_BOSS, Jugcraft.id(recipe), Component.translatable("encyclopedia.jugcraft.dossier.after_win",
					made == null ? Component.literal(recipe) : made.getDefaultInstance().getHoverName()));
		}
		for (String recipe : boss.madeFrom) {
			Item made = result(recipe);
			out.recipe(EdgeKind.OPTIONAL_BOSS, Jugcraft.id(recipe), Component.translatable("encyclopedia.jugcraft.dossier.made_from",
					made == null ? Component.literal(recipe) : made.getDefaultInstance().getHoverName()));
		}
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier." + boss.lair.id + ".leave"));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier.grave_goods"));
		LairRewards.Owed owed = player.getAttached(LairRewards.OWED);
		if (owed != null && owed.due().stream().anyMatch(due -> due.boss().equals(boss.boss))) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.owed"));
		}
		LairVisit visit = player.getAttached(Lairs.VISIT);
		if (visit != null && visit.lair().equals(boss.lair.id)) {
			share(player, out);
		}
	}

	/**
	 * Whether a Stillroot Poultice can be brewed here and earn its credit (Concordance Chunk C, C-02): the Concordance is
	 * on, the data's Verdance brews {@link Encounter#POULTICE_STATUS}, the status whose heals its helper is credited for,
	 * and a salve carrying that status alone is the Stillroot Poultice (Alchemy.product's own match: a data pack that drops
	 * the product, or adds a second salve of that status so the match is ambiguous, earns nobody credit). It only reads.
	 */
	private static boolean poultices() {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return false;
		}
		AlchemyCatalog catalog = Alchemy.catalog();
		Property verdance = catalog.property(Axis.VERDANCE.id);
		Product salve = catalog.product(Product.Form.SALVE, List.of(Encounter.POULTICE_STATUS), false);
		return brews(verdance, Encounter.POULTICE_STATUS) && salve != null && Alchemy.STILLROOT_POULTICE.equals(salve.id());
	}

	/** The player's part in the fight round them so far, and whether it is enough for a share. */
	private static void share(ServerPlayer player, PageWriter out) {
		for (Encounter fight : Encounter.around(player)) {
			Encounter.Share share = fight.share(player);
			if (share == null) {
				out.fact(Component.translatable("encyclopedia.jugcraft.dossier.share.none"));
				continue;
			}
			out.fact(Component.translatable(share.qualifies() ? "encyclopedia.jugcraft.dossier.share.qualifies" : "encyclopedia.jugcraft.dossier.share.not_yet"),
					Component.translatable("encyclopedia.jugcraft.dossier.share", number(share.damage()), number(share.healing()), number(share.warding()),
							share.deeds()), List.of());
		}
	}

	/** The items of these ids that this game has (one a feature leaves out is skipped). */
	private static List<Item> items(List<String> ids) {
		List<Item> out = new ArrayList<>();
		for (String id : ids) {
			Item item = item(id);
			if (item != null) {
				out.add(item);
			}
		}
		return out;
	}

	/** The item a rite recipe makes, from its id ({@code gothic_headstone_from_stonecutting} makes a gothic_headstone). */
	private static @Nullable Item result(String recipe) {
		int from = recipe.indexOf("_from_");
		return item(from < 0 ? recipe : recipe.substring(0, from));
	}

	private static @Nullable Item item(String path) {
		Item item = BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
		return item == Items.AIR ? null : item;
	}

	/** A lair's name. */
	public static Component name(Lair lair) {
		return Component.translatable("lair.jugcraft." + lair.id);
	}

	/** Health to one decimal place, whole numbers without one. */
	private static String number(float value) {
		float rounded = Math.round(value * 10.0F) / 10.0F;
		return rounded == Math.rint(rounded) ? String.valueOf((int) rounded) : String.valueOf(rounded);
	}
}

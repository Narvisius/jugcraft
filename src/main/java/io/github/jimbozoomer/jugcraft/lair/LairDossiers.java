package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload.EdgeKind;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceLoot;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineLoot;
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
 * The encyclopedia's Expeditions page (Concordance Chunk A, A-04): a dossier for each lair the player has found, and
 * nothing for one they have not (A-D9). A lair is found by carrying its token (its rite is then shown: what to make,
 * where and when); its keeper by meeting her (being in her lair, being owed her reward) or beating her, when the
 * dossier adds how support earns a share of her reward (what counts in her lair: her own creatures, healing, wards and
 * her objective), what she gives (the acquisition text JEI shows, "with the default data":
 * {@code jei.jugcraft.acquire.<item>}, tools/acquisition.py), the cheaper token and the pieces her loot makes, how to
 * leave, and, in a fight, the player's own part in it.
 * <p>
 * Discovery is read, not saved: the inventory, {@link Lairs#VISIT}, {@link LairRewards#OWED} and the first-win
 * advancements. So a player who went in, left, and used up their token (a Cursed Spindle) reads as not having found the
 * lair until they carry one again or win; recorded as a known limit. It only reads.
 */
public final class LairDossiers {
	public static final String PAGE = "expedition";

	/** How far a player has come with one lair. */
	public enum Discovery {
		UNKNOWN, RITE, MET, DEFEATED
	}

	/** A lair's keeper, with the tokens that open it, what she gives and the rite recipes her loot opens. */
	public enum Boss {
		VESPERINE(Lair.HOLLOW_ACRE, VesperineLoot.BOSS, VesperineLoot.ADVANCEMENT, "vesper_scythe",
				List.of("death_knell", "mourning_wreath"), List.of("death_knell", "mourning_wreath", "gothic_headstone_from_stonecutting"),
				List.of("reaper_shade", "vesper_scythe", "reaper_hood", "dirge_skull", "requiem_skull"),
				List.of("mourning_wreath_from_shade"), List.of("reaper_hood")),
		TATTERLACE(Lair.SPINDLE_LOFT, TatterlaceLoot.BOSS, TatterlaceLoot.ADVANCEMENT, "needle_rapier",
				List.of("cursed_spindle"), List.of("cursed_spindle", "spinning_wheel"),
				List.of("gossamer_silk", "needle_rapier", "golden_thimble", "tatterlace_headdress"),
				List.of("cursed_spindle_from_silk"), List.of());

		public final Lair lair;
		/** Her id in {@link LairRewards} ("vesperine"), and her entity's. */
		public final String boss;
		public final String advancement;
		/** The trophy a player's first win always brings. */
		public final String trophy;
		/** The items whose carrying shows her lair's rite. */
		public final List<String> tokens;
		/** The recipes her rite needs (each a way to it). */
		public final List<String> rite;
		/** What her table gives, each with its acquisition text ({@code jei.jugcraft.acquire.<item>}, tools/acquisition.py). */
		public final List<String> loot;
		/** Cheaper recipes for a token that her loot opens: optional, after a win. */
		public final List<String> afterWin;
		/** Recipes for a piece made of her loot, which has no other recipe: optional, after a win. */
		public final List<String> madeFrom;

		Boss(Lair lair, String boss, String advancement, String trophy, List<String> tokens, List<String> rite, List<String> loot,
				List<String> afterWin, List<String> madeFrom) {
			this.lair = lair;
			this.boss = boss;
			this.advancement = advancement;
			this.trophy = trophy;
			this.tokens = tokens;
			this.rite = rite;
			this.loot = loot;
			this.afterWin = afterWin;
			this.madeFrom = madeFrom;
		}
	}

	private LairDossiers() {
	}

	/** How far {@code player} has come with {@code boss}'s lair (the furthest that holds). */
	public static Discovery discovery(ServerPlayer player, Boss boss) {
		AdvancementHolder win = player.level().getServer().getAdvancements().get(Jugcraft.id(boss.advancement));
		if (win != null && player.getAdvancements().getOrStartProgress(win).isDone()) {
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

	/** How the lair opens: the rite in words with its numbers, the recipes it needs, the season, and the party it holds. */
	private static void rite(PageWriter out, Boss boss) {
		Component rite = switch (boss) {
			case VESPERINE -> Component.translatable("encyclopedia.jugcraft.dossier.hollow_acre.rite", LastRites.WREATH_RANGE, LastRites.CANDLES,
					LastRites.CANDLE_RANGE, LastRites.GRAVE_RANGE);
			case TATTERLACE -> Component.translatable("encyclopedia.jugcraft.dossier.spindle_loft.rite");
		};
		out.fact(rite, null, tokens(boss));
		for (String recipe : boss.rite) {
			Item made = result(recipe);
			out.recipe(EdgeKind.REQUIRED, Jugcraft.id(recipe), made == null ? Component.translatable("encyclopedia.jugcraft.route.make", recipe)
					: Component.translatable(recipe.endsWith("_headstone_from_stonecutting") ? "encyclopedia.jugcraft.dossier.grave"
							: "encyclopedia.jugcraft.route.make", made.getDefaultInstance().getHoverName()));
		}
		if (boss == Boss.VESPERINE) {
			out.sales(Items.LILY_OF_THE_VALLEY);
			out.sales(Items.CANDLE);
		}
		out.fact(Lairs.offSeason() ? Component.translatable("encyclopedia.jugcraft.dossier.any_night")
				: HalloweenSeason.active() ? Component.translatable("encyclopedia.jugcraft.dossier.in_season")
				: Component.translatable(boss == Boss.VESPERINE ? "message.jugcraft.lair.rite.off_season" : "message.jugcraft.lair.spindle.off_season"));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier.party", Lairs.partySize(), Lairs.gateTicks() / 20));
	}

	/** The keeper, once met: how support earns a share, her first win and loot, the recipes it opens, leaving, and the fight. */
	private static void keeper(ServerPlayer player, PageWriter out, Boss boss, Discovery discovery) {
		out.heading(Component.translatable("entity.jugcraft." + boss.boss));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier." + boss.lair.id + ".credit", number(Encounter.HEALING),
				number(Encounter.WARDING), (int) Encounter.REACH));
		out.fact(Component.translatable("encyclopedia.jugcraft.dossier.credit.wards"));
		Item trophy = item(boss.trophy);
		if (discovery == Discovery.DEFEATED) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.won",
					Component.translatable("advancements.jugcraft." + boss.advancement + ".title")));
		} else if (trophy != null) {
			out.fact(Component.translatable("encyclopedia.jugcraft.dossier.first_win", trophy.getDefaultInstance().getHoverName()), null, List.of(trophy));
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

	private static List<Item> tokens(Boss boss) {
		List<Item> out = new ArrayList<>();
		for (String token : boss.tokens) {
			Item item = item(token);
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

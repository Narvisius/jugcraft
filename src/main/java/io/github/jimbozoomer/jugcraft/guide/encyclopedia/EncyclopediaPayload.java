package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * The encyclopedia the server built for one player (Concordance Chunk A, A-04), sent when they ask: the request it
 * answers, notices (such as the Concordance being switched off) and its pages, each a list of rows. A row is a heading,
 * a step of a route (with where it stands for this player), one way to a step (an edge: required, recommended, by trade,
 * or after a boss), a fact, or a link; it says what it means in words, may carry exact figures, shows items as icons,
 * may show a recipe as its crafting grid, and may lead somewhere (the codex, the Spell Composer, the journal, another
 * page). The server writes all of it: every recipe shown is one this server has, read from the server's own recipe, and
 * nothing a player has not met is named.
 */
public record EncyclopediaPayload(int sequence, List<Component> notices, List<Page> pages) implements CustomPacketPayload {
	public static final int MAX_PAGES = 6;
	public static final int MAX_NOTICES = 4;
	public static final Type<EncyclopediaPayload> TYPE = new Type<>(Jugcraft.id("encyclopedia"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EncyclopediaPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, EncyclopediaPayload::sequence,
			ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NOTICES)), EncyclopediaPayload::notices,
			Page.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PAGES)), EncyclopediaPayload::pages,
			EncyclopediaPayload::new);

	public EncyclopediaPayload {
		notices = List.copyOf(notices);
		pages = List.copyOf(pages);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** What a row is. */
	public enum Kind {
		HEADING, STEP, EDGE, FACT, LINK;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** Where a step of a route stands for this player. */
	public enum StepState {
		/** Not a step. */
		NONE,
		DONE,
		/** The step to take next. */
		NEXT,
		/** Can be done now, beside the next step. */
		OPEN,
		/** Waits for a step before it. */
		LATER,
		/** Needed, but this server has no way to it (a recipe removed by a data pack, say). */
		BLOCKED;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** How much a step, or a way to it (an edge), matters. */
	public enum EdgeKind {
		NONE,
		/** Needed: the step, or one of the ways to it. */
		REQUIRED,
		/** Not needed, but makes the step easier. */
		RECOMMENDED,
		/** Bought, sold or shared between players instead of made. */
		TRADE,
		/** Opens only after a boss has been beaten; never the only way. */
		OPTIONAL_BOSS;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** One page: its id (for the views' tabs and for links), its title and its rows. */
	public record Page(String id, Component title, List<Row> rows) {
		public static final int MAX_ID = 32;
		public static final int MAX_ROWS = 96;
		public static final StreamCodec<RegistryFriendlyByteBuf, Page> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(MAX_ID), Page::id,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Page::title,
				Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)), Page::rows,
				Page::new);

		public Page {
			rows = List.copyOf(rows);
		}
	}

	/**
	 * One row: its kind, a step's state and how much it (or an edge) matters (as ordinals: one this game does not know
	 * reads as a plain fact, or as none), its text, the exact figures behind it if any, items to show (at most
	 * {@value #MAX_ICONS}), a recipe's grid and where it leads, if anywhere.
	 */
	public record Row(int kind, int state, int edge, Component text, Optional<Component> exact, List<Item> icons, Optional<Grid> grid,
			Optional<Link> link) {
		public static final int MAX_ICONS = 9;
		public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Row::kind,
				ByteBufCodecs.VAR_INT, Row::state,
				ByteBufCodecs.VAR_INT, Row::edge,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::text,
				ByteBufCodecs.optional(ComponentSerialization.TRUSTED_STREAM_CODEC), Row::exact,
				ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(MAX_ICONS)), Row::icons,
				ByteBufCodecs.optional(Grid.STREAM_CODEC), Row::grid,
				ByteBufCodecs.optional(Link.STREAM_CODEC), Row::link,
				Row::new);

		public Row {
			icons = List.copyOf(icons);
		}

		public Kind kindOf() {
			Kind[] kinds = Kind.values();
			return kind >= 0 && kind < kinds.length ? kinds[kind] : Kind.FACT;
		}

		public StepState stateOf() {
			StepState[] states = StepState.values();
			return state >= 0 && state < states.length ? states[state] : StepState.NONE;
		}

		public EdgeKind edgeOf() {
			EdgeKind[] edges = EdgeKind.values();
			return edge >= 0 && edge < edges.length ? edges[edge] : EdgeKind.NONE;
		}
	}

	/**
	 * A recipe as the server has it, to draw as a crafting grid: its id, its station ({@link #SHAPED}, {@link #SHAPELESS}
	 * or {@link #STONECUTTER}), the grid's width (shaped), each slot's items (an empty slot has none; a slot that takes
	 * any of several shows up to {@value #MAX_ALTERNATIVES} of them in turn), and what it makes and how many.
	 */
	public record Grid(Identifier recipe, int station, int width, List<List<Item>> slots, Item result, int count) {
		public static final int SHAPED = 0;
		public static final int SHAPELESS = 1;
		public static final int STONECUTTER = 2;
		public static final int MAX_SLOTS = 9;
		public static final int MAX_ALTERNATIVES = 4;
		public static final StreamCodec<RegistryFriendlyByteBuf, Grid> STREAM_CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC, Grid::recipe,
				ByteBufCodecs.VAR_INT, Grid::station,
				ByteBufCodecs.VAR_INT, Grid::width,
				ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(MAX_ALTERNATIVES)).apply(ByteBufCodecs.list(MAX_SLOTS)), Grid::slots,
				ByteBufCodecs.registry(Registries.ITEM), Grid::result,
				ByteBufCodecs.VAR_INT, Grid::count,
				Grid::new);

		public Grid {
			slots = slots.stream().map(List::copyOf).toList();
		}
	}

	/**
	 * Where a row leads: a codex entry (its id within the Arcane Concordance codex, offered only once the player's
	 * research has opened it), the Spell Composer, the Concordance Journal, or another page of the encyclopedia (its id).
	 */
	public record Link(int kind, String target) {
		public static final int CODEX = 0;
		public static final int COMPOSER = 1;
		public static final int JOURNAL = 2;
		public static final int PAGE = 3;
		public static final int MAX_TARGET = 128;
		public static final StreamCodec<RegistryFriendlyByteBuf, Link> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Link::kind,
				ByteBufCodecs.stringUtf8(MAX_TARGET), Link::target,
				Link::new);

		public static Link codex(String entry) {
			return new Link(CODEX, entry);
		}

		public static Link composer() {
			return new Link(COMPOSER, "");
		}

		public static Link journal() {
			return new Link(JOURNAL, "");
		}

		public static Link page(String id) {
			return new Link(PAGE, id);
		}
	}
}

package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;

/**
 * The Drone Tower's two guide books: the <b>Field Manual</b> (crafted from a book and a tier 1 drone) walks
 * through building a tower in survival; the <b>Creative Quick Start</b> is handed once to every player who joins
 * a world in creative mode, and covers the creative shortcuts (Creative Energy Cell, Creative Supply Crate, where
 * the modules go). Right-click to read: client/GuideBookScreen shows each page's heading, a screenshot from the game
 * and a paragraph (pages and text from tools/guide_books.py).
 */
public final class GuideBooks {
	/** Page counts; keep in step with tools/guide_books.py (check_mod_data.py checks the lang has them). */
	public static final int MANUAL_PAGES = 10;
	public static final int CREATIVE_PAGES = 8;
	/** Tag on a player who has had the creative book, so it is only given once. */
	public static final String GIVEN_TAG = "jugcraft.creative_guide";

	/** Opens a book's screen on the client (set by the client entrypoint); does nothing on a dedicated server. */
	public static java.util.function.Consumer<String> openScreen = book -> {
	};

	public static Item MANUAL;
	public static Item CREATIVE_GUIDE;

	private GuideBooks() {
	}

	public static void register() {
		MANUAL = book("drone_tower_manual", "Drone Tower Field Manual", MANUAL_PAGES);
		CREATIVE_GUIDE = book("creative_tower_guide", "Creative Quick Start", CREATIVE_PAGES);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(MANUAL);
			output.accept(CREATIVE_GUIDE);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> giveCreativeGuide(handler.player));
	}

	/** Gives the Creative Quick Start to a creative player who has not had it yet. */
	public static boolean giveCreativeGuide(ServerPlayer player) {
		return giveCreativeGuide(player, player.gameMode());
	}

	/** As {@link #giveCreativeGuide(ServerPlayer)}, for a player in game mode {@code mode} (tests pass it). */
	public static boolean giveCreativeGuide(ServerPlayer player, GameType mode) {
		if (mode != GameType.CREATIVE || player.entityTags().contains(GIVEN_TAG)) {
			return false;
		}
		player.addTag(GIVEN_TAG);
		ItemStack book = new ItemStack(CREATIVE_GUIDE);
		if (!player.getInventory().add(book)) {
			player.spawnAtLocation((net.minecraft.server.level.ServerLevel) player.level(), book);
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.guide.given"));
		return true;
	}

	private static Item book(String id, String title, int pages) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, new Book(id, new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON)));
	}

	/** A guide book: right-click to read it. */
	public static class Book extends Item {
		private final String id;

		Book(String id, Properties properties) {
			super(properties);
			this.id = id;
		}

		public String bookId() {
			return id;
		}

		@Override
		public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
				net.minecraft.world.InteractionHand hand) {
			if (level.isClientSide()) {
				openScreen.accept(id);
			}
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
	}
}

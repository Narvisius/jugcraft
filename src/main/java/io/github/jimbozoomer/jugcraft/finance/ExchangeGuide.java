package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * The Exchange Guide: a book that explains only the Exchange (bank, markets, land and shares). Right-click to read it
 * (client/ExchangeGuideScreen, pages from tools/exchange_guide.py). Every player gets one the first time they join; more
 * come from /jugcraft exchange guide.
 */
public final class ExchangeGuide {
	/** Tag on a player who has had the guide, so it is only given once. */
	public static final String GIVEN_TAG = "jugcraft.exchange_guide";

	/** Opens the guide on the client (set by the client entrypoint); does nothing on a dedicated server. */
	public static Runnable openScreen = () -> {
	};

	public static Item GUIDE;

	private ExchangeGuide() {
	}

	static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id("exchange_guide"));
		GUIDE = Registry.register(BuiltInRegistries.ITEM, key,
				new Book(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(GUIDE));
	}

	/** Gives the guide to a player who has not had it yet. */
	static void giveOnce(ServerPlayer player) {
		if (player.entityTags().contains(GIVEN_TAG)) {
			return;
		}
		player.addTag(GIVEN_TAG);
		give(player);
		player.sendSystemMessage(Component.translatable("message.jugcraft.exchange_guide.given"));
	}

	/** Puts a guide in the player's inventory (or at their feet when it is full). */
	static void give(ServerPlayer player) {
		ItemStack book = new ItemStack(GUIDE);
		if (!player.getInventory().add(book)) {
			player.spawnAtLocation((ServerLevel) player.level(), book);
		}
	}

	/** The guide book: right-click to read it. */
	public static class Book extends Item {
		Book(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (level.isClientSide()) {
				openScreen.run();
			}
			return InteractionResult.SUCCESS;
		}
	}
}

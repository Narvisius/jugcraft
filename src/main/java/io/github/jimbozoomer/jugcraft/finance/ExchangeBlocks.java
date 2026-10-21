package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The Exchange's blocks: the Market Chest, its block entity and its menu. */
public final class ExchangeBlocks {
	public static Block MARKET_CHEST;
	public static BlockEntityType<MarketChestBlockEntity> MARKET_CHEST_ENTITY;
	public static ExtendedMenuType<MarketChestMenu, BlockPos> MARKET_CHEST_MENU;
	public static final java.util.Map<PokerTableBlock.Design, Block> POKER_TABLES = new java.util.EnumMap<>(PokerTableBlock.Design.class);
	public static BlockEntityType<PokerTableBlockEntity> POKER_TABLE_ENTITY;
	public static final java.util.Map<CasinoBlock.Game, Block> CASINO = new java.util.EnumMap<>(CasinoBlock.Game.class);
	public static BlockEntityType<CasinoBlockEntity> CASINO_ENTITY;
	public static final java.util.Map<ShowGames.ScratchCard, Item> SCRATCH_CARDS = new java.util.EnumMap<>(ShowGames.ScratchCard.class);

	private ExchangeBlocks() {
	}

	/**
	 * A company's controller buys a Market Chest from the Exchange: the admins' price goes to the reserve and the chest
	 * goes into their inventory (or at their feet when it is full).
	 */
	static FinanceBook.Result buyChest(ServerPlayer player, FinanceBook book, long corpId, long day) {
		Equities equities = Equities.get(player.level().getServer());
		Equities.Corporation corp = equities.corp(corpId);
		if (corp == null || !player.getUUID().equals(equities.controller(corp))) {
			return FinanceBook.Result.fail("Only the holder of most of a company's shares buys its Market Chests.");
		}
		long price = ExchangeSettings.value(book, ExchangeSettings.CHEST_PRICE);
		if (price > 0) {
			FinanceBook.Result paid = book.payBank(player.getUUID(), price, day, player.getName().getString()
					+ " bought a Market Chest for " + corp.ticker + " for " + book.format(price) + ".");
			if (!paid.ok()) {
				return paid;
			}
		}
		net.minecraft.world.item.ItemStack chest = new net.minecraft.world.item.ItemStack(MARKET_CHEST);
		if (!player.getInventory().add(chest)) {
			player.spawnAtLocation((net.minecraft.server.level.ServerLevel) player.level(), chest);
		}
		return new FinanceBook.Result(true, "A Market Chest is in your inventory: place it and open it to set prices.");
	}

	static void register() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("market_chest"));
		// Wood-hard and explosion-proof, so a creeper cannot open the shop.
		MARKET_CHEST = Registry.register(BuiltInRegistries.BLOCK, blockKey, new MarketChestBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD).strength(2.5F, 1200.0F).sound(SoundType.WOOD).noOcclusion().setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("market_chest"));
		Item item = Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(MARKET_CHEST, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		MARKET_CHEST_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("market_chest"),
				FabricBlockEntityTypeBuilder.create(MarketChestBlockEntity::new, MARKET_CHEST).build());
		MARKET_CHEST_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("market_chest"),
				new ExtendedMenuType<>((containerId, inventory, pos) -> new MarketChestMenu(containerId, inventory, pos),
						BlockPos.STREAM_CODEC.cast()));
		// The three poker tables: wood-hard, and each part is a block of the same table.
		java.util.List<Item> tableItems = new java.util.ArrayList<>();
		for (PokerTableBlock.Design design : PokerTableBlock.Design.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(design.id));
			Block table = Registry.register(BuiltInRegistries.BLOCK, key, new PokerTableBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_GREEN).strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion().setId(key), design));
			POKER_TABLES.put(design, table);
			ResourceKey<Item> tableItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(design.id));
			tableItems.add(Registry.register(BuiltInRegistries.ITEM, tableItemKey,
					new BlockItem(table, new Item.Properties().setId(tableItemKey).useBlockDescriptionPrefix())));
		}
		POKER_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("poker_table"),
				FabricBlockEntityTypeBuilder.create(PokerTableBlockEntity::new, POKER_TABLES.values().toArray(Block[]::new)).build());
		// The casino games: tables and slot machines, each one block and item; the slot machines glow a little.
		for (CasinoBlock.Game game : CasinoBlock.Game.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(game.id));
			int light = game.light();
			Block block = Registry.register(BuiltInRegistries.BLOCK, key, new CasinoBlock(BlockBehaviour.Properties.of()
					.mapColor(game.machine() ? MapColor.METAL : MapColor.COLOR_GREEN).strength(2.5F, 6.0F)
					.sound(game.machine() ? SoundType.METAL : SoundType.WOOD).lightLevel(state -> light).noOcclusion().setId(key), game));
			CASINO.put(game, block);
			ResourceKey<Item> gameItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(game.id));
			tableItems.add(Registry.register(BuiltInRegistries.ITEM, gameItemKey,
					new BlockItem(block, new Item.Properties().setId(gameItemKey).useBlockDescriptionPrefix())));
		}
		// Scratch cards: bought at a lottery kiosk, scratched by using them.
		for (ShowGames.ScratchCard card : ShowGames.ScratchCard.values()) {
			ResourceKey<Item> cardKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(card.id()));
			Item cardItem = Registry.register(BuiltInRegistries.ITEM, cardKey, new ScratchCardItem(new Item.Properties()
					.setId(cardKey).stacksTo(16), card));
			SCRATCH_CARDS.put(card, cardItem);
			tableItems.add(cardItem);
		}
		CASINO_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("casino_game"),
				FabricBlockEntityTypeBuilder.create(CasinoBlockEntity::new, CASINO.values().toArray(Block[]::new)).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(item);
			tableItems.forEach(output::accept);
		});

		// Only a casino game's owner (or an operator) may take it down: it is their house.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(state.getBlock() instanceof CasinoBlock casinoBlock) || !(player instanceof ServerPlayer server)) {
				return true;
			}
			if (!(level.getBlockEntity(casinoBlock.masterPos(pos, state)) instanceof CasinoBlockEntity casino)
					|| casino.owner() == null || casino.isOwner(server) || FinanceService.isAdmin(server)) {
				return true;
			}
			server.sendOverlayMessage(Component.literal("Only " + casino.ownerName() + " can take down this game."));
			return false;
		});

		// Only the company's controller (or an operator) may break its chest.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(blockEntity instanceof MarketChestBlockEntity chest) || !(player instanceof ServerPlayer server)
					|| chest.corp() <= 0 || chest.mayUse(server)) {
				return true;
			}
			server.sendOverlayMessage(Component.literal("Only the company's controller can take down this Market Chest."));
			return false;
		});
	}
}

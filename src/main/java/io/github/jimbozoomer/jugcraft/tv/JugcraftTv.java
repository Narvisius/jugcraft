package io.github.jimbozoomer.jugcraft.tv;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The flat screen TVs (docs/features/flat-screen-tv.md): their block entity, the SCREEN window's packets, and what
 * the server does with its controls. The server only keeps the link and the clock; playing is the players' games'
 * (client/tv/TvPlayer.java).
 */
public final class JugcraftTv {
	public static BlockEntityType<TvBlockEntity> TV_ENTITY;
	/** The surround speaker (TvSpeakerBlock) and its link; the Sync Key and what it's set to (SyncKeyItem). */
	public static Block SPEAKER;
	public static BlockEntityType<TvSpeakerBlockEntity> SPEAKER_ENTITY;
	public static Item SYNC_KEY;
	public static DataComponentType<GlobalPos> SYNC_SCREEN, SYNC_SPEAKER;

	private JugcraftTv() {
	}

	/** Called by PlantInterior.register with the TV block it registered. */
	public static void register(Block tv) {
		TV_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("flat_tv"),
				FabricBlockEntityTypeBuilder.create(TvBlockEntity::new, tv).build());
		PayloadTypeRegistry.clientboundPlay().register(TvPayloads.Open.TYPE, TvPayloads.Open.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TvPayloads.Control.TYPE, TvPayloads.Control.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TvPayloads.Control.TYPE, (payload, context) -> control(context.player(), payload));
		PayloadTypeRegistry.serverboundPlay().register(TvPayloads.Ask.TYPE, TvPayloads.Ask.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TvPayloads.Ask.TYPE, (payload, context) -> ask(context.player(), payload.pos()));
		registerSpeakers();
	}

	/** The surround speakers and the Sync Key (Justin, 27 October 2026). */
	private static void registerSpeakers() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("tv_speaker"));
		SPEAKER = Registry.register(BuiltInRegistries.BLOCK, key, new TvSpeakerBlock(BlockBehaviour.Properties.of().setId(key)
				.strength(1.0F).noOcclusion().sound(SoundType.METAL)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("tv_speaker"));
		Item speakerItem = Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(SPEAKER, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		SPEAKER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("tv_speaker"),
				FabricBlockEntityTypeBuilder.create(TvSpeakerBlockEntity::new, SPEAKER).build());
		SYNC_SCREEN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("sync_screen"),
				DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
		SYNC_SPEAKER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("sync_speaker"),
				DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
		ResourceKey<Item> syncKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("sync_key"));
		SYNC_KEY = Registry.register(BuiltInRegistries.ITEM, syncKey, new SyncKeyItem(new Item.Properties().setId(syncKey).stacksTo(1)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(speakerItem));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(SYNC_KEY));
	}

	/**
	 * A right-click on a screen from further than the game's reach (client/tv/TvFarUse): its window, if the block is a
	 * TV and the player is within {@link FlatTvBlock#FAR_REACH} of it (public for the tests). Returns whether it opened.
	 */
	public static boolean ask(ServerPlayer player, BlockPos pos) {
		if (!(player.level() instanceof ServerLevel level) || !level.isLoaded(pos) || player.isSpectator()) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof FlatTvBlock)) {
			return false;
		}
		if (FlatTvBlock.screenDistance(pos, state.getValue(FlatTvBlock.FACING), 1, 1, player.getEyePosition()) > FlatTvBlock.FAR_REACH + 2) {
			return false;
		}
		FlatTvBlock.openFor(level, player, pos, state);
		return true;
	}

	/** Sends {@code player} the SCREEN window for the screen whose controller is {@code tv}. */
	static void open(ServerPlayer player, TvBlockEntity tv) {
		if (ServerPlayNetworking.canSend(player, TvPayloads.Open.TYPE)) {
			ServerPlayNetworking.send(player, new TvPayloads.Open(tv.getBlockPos(), tv.mayControl(player), ownerName(player, tv.owner())));
		}
	}

	private static String ownerName(ServerPlayer player, UUID owner) {
		if (owner == null) {
			return "";
		}
		ServerPlayer online = player.level().getServer().getPlayerList().getPlayer(owner);
		if (online != null) {
			return online.getName().getString();
		}
		String name = player.level().getServer().services().nameToIdCache().get(owner).map(NameAndId::name).orElse("");
		return name.length() > 64 ? name.substring(0, 64) : name;
	}

	/** A link the TVs will play: an http(s) web address with a host, no spaces or control characters. */
	public static boolean validLink(String s) {
		if (s.isEmpty() || s.length() > TvBlockEntity.MAX_URL) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch <= ' ' || ch == 0x7F) {
				return false;
			}
		}
		try {
			URI u = new URI(s);
			String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
			return (scheme.equals("https") || scheme.equals("http")) && u.getHost() != null && !u.getHost().isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	/** A control from the SCREEN window: checked (their party's screen, close enough, a web address), then done (public for the tests). */
	public static void control(ServerPlayer player, TvPayloads.Control c) {
		BlockPos pos = c.pos();
		if (!(player.level().getBlockEntity(pos) instanceof TvBlockEntity tv) || !tv.isController()) {
			return;
		}
		if (tv.screenDistance(player.getEyePosition()) > FlatTvBlock.FAR_REACH + FlatTvBlock.WINDOW_SLACK || !tv.mayControl(player)) {
			return;
		}
		long now = System.currentTimeMillis();
		switch (c.action()) {
			case TvPayloads.Control.LINK -> {
				if (validLink(c.text())) {
					tv.setLink(c.text());
				}
			}
			case TvPayloads.Control.PLAY -> tv.play(now);
			case TvPayloads.Control.PAUSE -> tv.pause(now);
			case TvPayloads.Control.SEEK -> tv.seek(now, c.value());
			case TvPayloads.Control.STOP -> tv.setLink("");
			case TvPayloads.Control.VOLUME -> tv.setVolume((int) Math.max(0, Math.min(100, c.value())));
			case TvPayloads.Control.LOOP -> tv.setLoop(c.value() != 0);
			case TvPayloads.Control.PLAYLIST -> tv.setPlaylist(c.value() != 0);
			default -> {
			}
		}
	}
}

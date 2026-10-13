package io.github.jimbozoomer.jugcraft.concordance.courier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.logistics.RequestState;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Logistics on the server (roadmap step 18, docs/features/arcane-concordance-logistics.md): the Courier Post, the
 * Porter Key's binding to it, filing and reading requests, and the {@code logistics} command. The ledger is
 * {@link CourierLedger}; the couriers are Clockwork Porters bound to a post
 * ({@link io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity}).
 */
public final class Couriers {
	/** How far round its post a courier looks for the items asked for. */
	public static final int SOURCE_RADIUS = 8;
	/** How many history lines the post shows (it keeps {@value Logistics#HISTORY}). */
	public static final int SHOWN_HISTORY = 8;

	public static Block COURIER_POST;
	public static BlockEntityType<CourierPostBlockEntity> POST_ENTITY;
	/** A Porter Key's Courier Post (set by using the key on a post). */
	public static DataComponentType<GlobalPos> KEY_POST;

	private Couriers() {
	}

	public static void register() {
		KEY_POST = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("courier_post"),
				DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("courier_post"));
		COURIER_POST = Registry.register(BuiltInRegistries.BLOCK, key, new CourierPostBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD).strength(2.5F, 6.0F).sound(SoundType.WOOD).setId(key)));
		POST_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("courier_post"),
				FabricBlockEntityTypeBuilder.create(CourierPostBlockEntity::new, COURIER_POST).build());
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("courier_post"));
		Item post = Registry.register(BuiltInRegistries.ITEM, itemKey,
				new SkyItem(COURIER_POST, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(post));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
	}

	public static Place place(ServerLevel level, BlockPos pos) {
		return new Place(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
	}

	public static BlockPos pos(Place place) {
		return new BlockPos(place.x(), place.y(), place.z());
	}

	/** Files a request at {@code post} for {@code count} of exactly {@code sample}, if {@code player} may. */
	public static void request(ServerPlayer player, ServerLevel level, CourierPostBlockEntity post, ItemStack sample, int count) {
		if (!Workers.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		if (!Workers.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.unknown"));
			return;
		}
		if (!post.mayUse(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.courier.not_yours"));
			return;
		}
		if (sample.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.courier.no_sample"));
			return;
		}
		Place at = place(level, post.getBlockPos());
		Logistics.Filed filed = CourierLedger.of(level.getServer()).file(player.getUUID(), at, at, sample, count, level.registryAccess(),
				level.getGameTime());
		if (filed.outcome() == Logistics.Outcome.DONE) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.courier.filed", count, sample.getHoverName(), filed.id()));
		} else {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.courier.refused." + filed.outcome().name().toLowerCase(java.util.Locale.ROOT),
					Logistics.MAX_OPEN_PER_PLAYER, Logistics.MAX_WANTED));
		}
	}

	/** The post's summary and its recent history. */
	public static void describe(ServerPlayer player, ServerLevel level, CourierPostBlockEntity post) {
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place at = place(level, post.getBlockPos());
		int open = 0;
		int moving = 0;
		for (Request request : ledger.ledger().requests()) {
			if (request.ticket().post().equals(at)) {
				open++;
				if (request.progress().carried() > 0) {
					moving++;
				}
			}
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.courier.header", open, moving, post.freeSlots(),
				CourierPostBlockEntity.SLOTS));
		List<Event> history = ledger.ledger().history(at);
		for (Event event : history.subList(Math.max(0, history.size() - SHOWN_HISTORY), history.size())) {
			player.sendSystemMessage(event(event));
		}
	}

	/** One history line in words. */
	public static Component event(Event event) {
		Component note = event.note().matches("[a-z_]+") ? Component.translatable("compose.jugcraft.courier.note." + event.note())
				: Component.literal(event.note());
		return Component.translatable("message.jugcraft.concordance.courier.event." + event.kind(), event.request(), event.amount(), note);
	}

	/** A request in words: its id, what and how many, its state and progress. */
	public static Component line(CourierLedger ledger, Request request) {
		ItemVariant item = ledger.item(request.id());
		Component name = item == null ? Component.literal("?") : item.toStack().getHoverName();
		RequestState state = request.state();
		String note = request.progress().note();
		Component why = note.isEmpty() ? Component.empty()
				: Component.translatable("message.jugcraft.concordance.courier.because", Component.translatable("compose.jugcraft.courier.note." + note));
		return Component.translatable("message.jugcraft.concordance.courier.line", request.id(), request.ticket().wanted(), name,
				Component.translatable("compose.jugcraft.courier.state." + state.id), request.progress().delivered(),
				request.progress().carried(), why);
	}

	/** A player's open requests. */
	public static void list(ServerPlayer player, Consumer<Component> out) {
		CourierLedger ledger = CourierLedger.of(player.level().getServer());
		boolean any = false;
		for (Request request : ledger.ledger().requests()) {
			if (request.ticket().requester().equals(player.getUUID())) {
				out.accept(line(ledger, request));
				any = true;
			}
		}
		if (!any) {
			out.accept(Component.translatable("message.jugcraft.concordance.courier.none"));
		}
	}

	/** The nearest Courier Post within {@value #SOURCE_RADIUS} blocks that {@code player} may use, or null. */
	static CourierPostBlockEntity nearestPost(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		CourierPostBlockEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-SOURCE_RADIUS, -SOURCE_RADIUS, -SOURCE_RADIUS),
				at.offset(SOURCE_RADIUS, SOURCE_RADIUS, SOURCE_RADIUS))) {
			if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CourierPostBlockEntity post && post.mayUse(player.getUUID())) {
				double distance = pos.distSqr(at);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = post;
				}
			}
		}
		return best;
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("logistics")
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					list(player, line -> context.getSource().sendSuccess(() -> line, false));
					return 1;
				})
				.then(Commands.literal("request").then(Commands.argument("count", IntegerArgumentType.integer(1, Logistics.MAX_WANTED))
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							if (!RateGate.allow(player, "logistics", 10)) {
								context.getSource().sendFailure(Component.translatable("message.jugcraft.concordance.courier.too_fast"));
								return 0;
							}
							CourierPostBlockEntity post = nearestPost(player);
							if (post == null) {
								context.getSource().sendFailure(Component.translatable("message.jugcraft.concordance.courier.no_post", SOURCE_RADIUS));
								return 0;
							}
							request(player, (ServerLevel) player.level(), post, player.getMainHandItem(), IntegerArgumentType.getInteger(context, "count"));
							return 1;
						})))
				.then(Commands.literal("cancel").then(Commands.argument("id", LongArgumentType.longArg(1L)).executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					if (!RateGate.allow(player, "logistics", 10)) {
						context.getSource().sendFailure(Component.translatable("message.jugcraft.concordance.courier.too_fast"));
						return 0;
					}
					long id = LongArgumentType.getLong(context, "id");
					CourierLedger ledger = CourierLedger.of(player.level().getServer());
					Logistics.Outcome outcome = ledger.cancel(id, player.getUUID(), player.level().getGameTime());
					report(context.getSource(), outcome, ledger.ledger().get(id) == null ? "cancelled" : "returning", id);
					return outcome == Logistics.Outcome.DONE ? 1 : 0;
				})))
				.then(Commands.literal("recover").then(Commands.argument("id", LongArgumentType.longArg(1L)).executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					if (!RateGate.allow(player, "logistics", 10)) {
						context.getSource().sendFailure(Component.translatable("message.jugcraft.concordance.courier.too_fast"));
						return 0;
					}
					long id = LongArgumentType.getLong(context, "id");
					Logistics.Outcome outcome = CourierLedger.of(player.level().getServer()).recover(id, player, player.level().getGameTime());
					report(context.getSource(), outcome, "recovered", id);
					return outcome == Logistics.Outcome.DONE ? 1 : 0;
				}))))));
	}

	private static void report(CommandSourceStack source, Logistics.Outcome outcome, String done, long id) {
		if (outcome == Logistics.Outcome.DONE) {
			source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.courier." + done, id), false);
		} else {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.courier.refused." + outcome.name().toLowerCase(java.util.Locale.ROOT),
					Logistics.MAX_OPEN_PER_PLAYER, Logistics.MAX_WANTED));
		}
	}
}

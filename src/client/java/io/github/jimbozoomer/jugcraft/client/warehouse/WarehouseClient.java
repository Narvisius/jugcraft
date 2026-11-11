package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.DronePayload;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsInPayload;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsOutPayload;
import io.github.jimbozoomer.jugcraft.warehouse.JugcraftWarehouse;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * The warehouse's client side: its renderers and screens, the drones' legs as they come (DronePayload), the terminals'
 * pages (TerminalPayload, for their monitors, the drones' screens and the terminal's screen), the GOODS IN screen's
 * status; and the loaded terminals, whose fleets are drawn every frame (DroneRenderer).
 */
public final class WarehouseClient {
	private static final Set<TerminalBlockEntity> TERMINALS = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<BlockPos, TerminalPayload> SNAPSHOTS = new HashMap<>();
	private static final Map<BlockPos, io.github.jimbozoomer.jugcraft.warehouse.BuildingPayload> BUILDINGS = new HashMap<>();
	/** Part W5: each terminal's CONTROL page and floor map, as they last came. */
	private static final Map<BlockPos, io.github.jimbozoomer.jugcraft.warehouse.ControlPayload> CONTROLS = new HashMap<>();
	private static final Map<BlockPos, io.github.jimbozoomer.jugcraft.warehouse.MapPayload> MAPS = new HashMap<>();
	private static @Nullable GoodsInPayload goodsIn;
	private static @Nullable GoodsOutPayload goodsOut;
	private static io.github.jimbozoomer.jugcraft.warehouse.@Nullable LiquidsPayload liquids;

	private WarehouseClient() {
	}

	public static void register() {
		BlockEntityRendererRegistry.register(JugcraftWarehouse.INTAKE_ENTITY, IntakeRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.STATION_ENTITY, StationRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.TERMINAL_ENTITY, TerminalRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.GOODS_IN_ENTITY, GoodsInRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.DOCK_ENTITY, DockRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.RACK_ENTITY, RackRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.READY_STORAGE_ENTITY, ReadyStorageRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.GOODS_OUT_ENTITY, GoodsOutRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.LIFT_ENTITY, LiftRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.AISLE_SIGN_ENTITY, AisleSignRenderer::new);
		// Part W4: liquids in drums.
		BlockEntityRendererRegistry.register(JugcraftWarehouse.FILLER_ENTITY, FillerRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.EMPTIER_ENTITY, EmptierRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.LIQUIDS_IN_ENTITY, LiquidsInRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.LIQUIDS_OUT_ENTITY, LiquidsOutRenderer::new);
		MenuScreens.register(JugcraftWarehouse.LIQUIDS_IN_MENU, LiquidsInScreen::new);
		MenuScreens.register(JugcraftWarehouse.LIQUIDS_OUT_MENU, LiquidsOutScreen::new);
		MenuScreens.register(JugcraftWarehouse.GOODS_IN_MENU, GoodsInScreen::new);
		MenuScreens.register(JugcraftWarehouse.TERMINAL_MENU, TerminalScreen::new);
		MenuScreens.register(JugcraftWarehouse.GOODS_OUT_MENU, GoodsOutScreen::new);
		// Part W5: the switchboard, the status board, the signal box.
		BlockEntityRendererRegistry.register(JugcraftWarehouse.SWITCHBOARD_ENTITY, SwitchboardRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.STATUS_BOARD_ENTITY, StatusBoardRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftWarehouse.SIGNAL_BOX_ENTITY, SignalBoxRenderer::new);
		MenuScreens.register(JugcraftWarehouse.SWITCHBOARD_MENU, SwitchboardScreen::new);
		MenuScreens.register(JugcraftWarehouse.SIGNAL_BOX_MENU, SignalBoxScreen::new);
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof TerminalBlockEntity terminal) {
				TERMINALS.add(terminal);
			}
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof TerminalBlockEntity terminal) {
				TERMINALS.remove(terminal);
				SNAPSHOTS.remove(terminal.getBlockPos());
				BUILDINGS.remove(terminal.getBlockPos());
				CONTROLS.remove(terminal.getBlockPos());
				MAPS.remove(terminal.getBlockPos());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(DronePayload.TYPE, (payload, context) -> {
			var level = Minecraft.getInstance().level;
			if (level != null && level.getBlockEntity(payload.terminal()) instanceof TerminalBlockEntity terminal) {
				terminal.receive(payload.drone());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TerminalPayload.TYPE, (payload, context) -> {
			TerminalPayload old = SNAPSHOTS.get(payload.terminal());
			// The light pages (no stock list) don't wipe a full one an open screen is showing.
			if (payload.items().size() == payload.types() || old == null || old.items().size() != old.types()) {
				SNAPSHOTS.put(payload.terminal(), payload);
			} else {
				SNAPSHOTS.put(payload.terminal(), new TerminalPayload(payload.terminal(), payload.primary(), old.items(), payload.fleet(),
						payload.log(), payload.placesUsed(), payload.placesTotal(), payload.casesWaiting(), payload.itemsIn(), payload.truncated(),
						payload.stored(), payload.types(), payload.orders(), payload.out()));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.warehouse.BuildingPayload.TYPE,
				(payload, context) -> BUILDINGS.put(payload.terminal(), payload));
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.warehouse.ControlPayload.TYPE,
				(payload, context) -> CONTROLS.put(payload.terminal(), payload));
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.warehouse.MapPayload.TYPE, (payload, context) -> MAPS.put(payload.terminal(), payload));
		ClientPlayNetworking.registerGlobalReceiver(GoodsInPayload.TYPE, (payload, context) -> goodsIn = payload);
		ClientPlayNetworking.registerGlobalReceiver(GoodsOutPayload.TYPE, (payload, context) -> goodsOut = payload);
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.warehouse.LiquidsPayload.TYPE, (payload, context) -> liquids = payload);
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> DroneRenderer.render(context.levelState().cameraRenderState.pos, context.poseStack(),
				context.submitNodeCollector(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
	}

	static Set<TerminalBlockEntity> terminals() {
		return TERMINALS;
	}

	static @Nullable TerminalPayload snapshot(BlockPos terminal) {
		return SNAPSHOTS.get(terminal);
	}

	static io.github.jimbozoomer.jugcraft.warehouse.@Nullable BuildingPayload building(BlockPos terminal) {
		return BUILDINGS.get(terminal);
	}

	static io.github.jimbozoomer.jugcraft.warehouse.@Nullable ControlPayload control(@Nullable BlockPos terminal) {
		return terminal == null ? null : CONTROLS.get(terminal);
	}

	static io.github.jimbozoomer.jugcraft.warehouse.@Nullable MapPayload map(@Nullable BlockPos terminal) {
		return terminal == null ? null : MAPS.get(terminal);
	}

	static @Nullable GoodsInPayload goodsIn() {
		return goodsIn;
	}

	static @Nullable GoodsOutPayload goodsOut() {
		return goodsOut;
	}

	static io.github.jimbozoomer.jugcraft.warehouse.@Nullable LiquidsPayload liquids() {
		return liquids;
	}
}

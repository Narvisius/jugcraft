package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nuclear.SiteData;
import io.github.jimbozoomer.jugcraft.nuclear.SiteRegistry;
import io.github.jimbozoomer.jugcraft.nuclear.Sites;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;

/**
 * The client's half of the compiled sites (towers and pump houses build): their block model, and their anchors putting
 * them in the client's SiteRegistry as they arrive (the chunks they cover are built again, so blocks meshed before
 * their anchor arrived get their look).
 */
public final class SiteClient {
	private SiteClient() {
	}

	public static void register() {
		SiteData.CLIENT = true;
		SiteRegistry.CLIENT_SIDE = true;
		SiteModel.register();
		// The pump houses' controllers draw their sites' live parts (the tower's draws its own: CoolingTowerRenderer).
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.WATER_WORKS_ENTITY, WaterWorksRenderer::new);
		// The transformer yard's and the switchyard's controllers draw theirs (transformer yard and switchyard build).
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.YARD_ENTITY, YardRenderer::new);
		// The reactor building's controller draws its building's (22 October 2026).
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.REACTOR_BUILDING_ENTITY, ReactorBuildingRenderer::new);
		// An anchor showing another site now (the reactor building at power or refuelling): swapped, its chunks built again.
		Sites.clientRefresh = entity -> {
			if (entity.getLevel() instanceof ClientLevel level) {
				SiteRegistry.Placement was = Sites.unload(entity, level);
				SiteRegistry.Placement now = Sites.load(entity, level);
				if (was != null) rebuild(level, was.bounds());
				if (now != null) rebuild(level, now.bounds());
			}
		};
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
			SiteRegistry.Placement p = Sites.load(entity, level);
			if (p != null) {
				rebuild(level, p.bounds());
			}
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
			SiteRegistry.Placement p = Sites.unload(entity, level);
			if (p != null && Minecraft.getInstance().level == level) {
				rebuild(level, p.bounds());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SiteRegistry.clearClient());
	}

	private static void rebuild(ClientLevel level, AABB b) {
		level.setSectionRangeDirty((int) Math.floor(b.minX) >> 4, (int) Math.floor(b.minY) >> 4, (int) Math.floor(b.minZ) >> 4,
				(int) Math.floor(b.maxX) >> 4, (int) Math.floor(b.maxY) >> 4, (int) Math.floor(b.maxZ) >> 4);
	}
}

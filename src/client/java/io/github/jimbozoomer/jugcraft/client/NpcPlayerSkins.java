package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * A custom NPC's player skin on the client: the profile the server resolved and synced ({@link Townsfolk#profile()}),
 * through the game's own skin cache (as {@link OwnerStatueSkins} does for statues), so textures download off the
 * render thread and are shared with player heads and players. Until it has loaded the NPC wears the town's default.
 * At most {@link #MAX} NPCs' lookups are kept; render thread only.
 */
final class NpcPlayerSkins {
	static final int MAX = 64;

	private record Entry(ResolvableProfile profile, Supplier<PlayerSkinRenderCache.RenderInfo> lookup) {
	}

	/** By entity id, least recently drawn first. */
	private static final Map<Integer, Entry> LOOKUPS = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, NpcPlayerSkins.Entry> eldest) {
			return size() > MAX;
		}
	};

	private NpcPlayerSkins() {
	}

	static void extract(Townsfolk npc, TownsfolkRenderer.State state) {
		ResolvableProfile profile = npc.profile();
		Entry entry = LOOKUPS.get(npc.getId());
		if (entry == null || !entry.profile().equals(profile)) {
			entry = new Entry(profile, Minecraft.getInstance().playerSkinRenderCache().createLookup(profile));
			LOOKUPS.put(npc.getId(), entry);
		}
		PlayerSkinRenderCache.RenderInfo info = entry.lookup().get();
		if (info == null) {
			return;
		}
		var skin = info.playerSkin();
		state.playerSkin = skin.body().texturePath();
		state.slim = skin.model() == PlayerModelType.SLIM;
	}
}


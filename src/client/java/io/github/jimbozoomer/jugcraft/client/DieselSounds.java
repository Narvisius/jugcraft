package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * The emergency diesel generators' running sound, on the client: a loop of the V16 at its rated speed whose pitch and
 * loudness follow the engine (it fades in as the engine fires and catches, rises as it runs up, and runs down with it
 * when stopped). The air start and the stop are one-shot sounds the server plays (EquipmentPlacement). The engine's
 * speed is worked out here the same way the renderer turns its parts (client/NuclearRenderer.diesel), so the sound
 * keeps going when the generator is out of sight.
 */
public final class DieselSounds {
	static final double RANGE = 44;
	private static final Map<BlockPos, Engine> ENGINES = new HashMap<>();
	private static int scan;

	private DieselSounds() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(DieselSounds::tick);
	}

	/** How many generators are making a sound now (game tests). */
	public static int playing() {
		return (int) ENGINES.values().stream().filter(e -> e.sound != null && !e.sound.isStopped()).count();
	}

	private static final class Engine {
		final BlockPos anchor;
		final Vec3 middle;
		float speed;
		boolean firing;
		boolean gone;
		EngineSound sound;

		Engine(BlockPos anchor, Vec3 middle) {
			this.anchor = anchor;
			this.middle = middle;
		}
	}

	private static void tick(Minecraft client) {
		Level level = client.level;
		if (level == null || client.player == null) {
			ENGINES.values().forEach(e -> e.gone = true);
			ENGINES.clear();
			return;
		}
		Vec3 ear = client.player.getEyePosition();
		// Look for generators near the player now and then (not every tick).
		if (scan-- <= 0) {
			scan = 10;
			BlockPos at = client.player.blockPosition();
			int cx = at.getX() >> 4, cz = at.getZ() >> 4, r = (int) Math.ceil(RANGE / 16) + 1;
			for (int x = cx - r; x <= cx + r; x++) {
				for (int z = cz - r; z <= cz + r; z++) {
					if (!(level.getChunk(x, z) instanceof LevelChunk chunk)) {
						continue;
					}
					for (BlockEntity entity : chunk.getBlockEntities().values()) {
						if (entity instanceof EquipmentBlockEntity e && e.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR
								&& !ENGINES.containsKey(e.getBlockPos())) {
							// Over the middle of the engine (in the open: a sound inside the machine's frames would be
							// muffled by them, client/SoundOcclusion).
							BlockPos middle = e.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(3, 5, 6), e.facing()));
							Vec3 m = Vec3.atCenterOf(middle);
							if (m.distanceToSqr(ear) < RANGE * RANGE) {
								ENGINES.put(e.getBlockPos().immutable(), new Engine(e.getBlockPos().immutable(), m));
							}
						}
					}
				}
			}
		}
		ENGINES.values().removeIf(engine -> {
			if (!(level.getBlockEntity(engine.anchor) instanceof EquipmentBlockEntity e) || engine.middle.distanceToSqr(ear) > RANGE * RANGE * 1.2) {
				engine.gone = true;
				return true;
			}
			// The same run-up as the renderer: cranking on air for a second and a half, then up to speed in two and a
			// half; stopped, it coasts down over four.
			long since = level.getGameTime() - e.runningSince;
			if (e.running) {
				engine.speed = since < 30 ? 0.12F : Math.min(1F, 0.12F + (since - 30) / 50F * 0.88F);
			} else {
				engine.speed = Math.max(0, engine.speed - 0.0125F);
			}
			engine.firing = e.running && since >= 28;
			boolean heard = engine.firing || !e.running;  // not while it only cranks on air (the start sound has that)
			if (engine.speed > 0.01F && heard && (engine.sound == null || engine.sound.isStopped())) {
				engine.sound = new EngineSound(engine);
				client.getSoundManager().play(engine.sound);
			}
			return false;
		});
	}

	/** One generator's running loop: pitch with the engine's speed, loud while it fires, fading as it runs down. */
	private static final class EngineSound extends AbstractTickableSoundInstance {
		private final Engine engine;

		EngineSound(Engine engine) {
			super(JugcraftNuclear.EDG_RUN, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
			this.engine = engine;
			this.looping = true;
			this.delay = 0;
			this.attenuation = Attenuation.LINEAR;
			this.x = engine.middle.x;
			this.y = engine.middle.y;
			this.z = engine.middle.z;
			this.volume = 0.01F;
			this.pitch = 0.5F;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			if (engine.gone || engine.speed <= 0.01F) {
				volume -= 0.05F;
				if (volume <= 0 || engine.gone) {
					stop();
				}
				return;
			}
			// The loop is the engine at full speed; slower is lower (the game plays no lower than half pitch, so below
			// half speed it gets quieter instead).
			float target = engine.firing ? 0.35F + 0.65F * engine.speed : engine.speed * 1.3F;
			volume += (Math.min(1F, target) - volume) * 0.15F;
			pitch = Math.max(0.5F, Math.min(1F, 0.3F + 0.7F * engine.speed));
		}
	}
}

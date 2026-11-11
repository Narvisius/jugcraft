package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * A gate guard's lit plasma baton humming (tools/plasma_baton.py: a seamless loop stretched out of the ignition's held
 * part). It comes in as the ignition's sound ends ({@value #START} ticks into the draw) at the ignition's level and eases
 * down to a quiet idle over {@value #SETTLE} ticks, follows them, and stops the moment the baton goes out (the power-down
 * sound carries on from it). Client only: the server never hears it, and each client times it from when it saw the draw
 * begin ({@link #track}).
 */
public class BatonHum extends AbstractTickableSoundInstance {
	/** Ticks from the draw's start to the hum: the ignition plays from tick 7 and its held part ends about 15 ticks on. */
	static final int START = 22;
	/** Ticks the hum takes to settle from the ignition's level to the idle (-16 dB). */
	static final int SETTLE = 12;
	private static final float LOUD = 0.9F, IDLE = 0.16F;

	private final TowerCrew crew;
	private int age;

	BatonHum(TowerCrew crew) {
		super(PlasmaBaton.HUM, SoundSource.HOSTILE, RandomSource.create());
		this.crew = crew;
		this.looping = true;
		this.delay = 0;
		this.volume = LOUD;
		follow();
	}

	private void follow() {
		x = crew.getX();
		y = crew.getY() + 1.0;
		z = crew.getZ();
	}

	@Override
	public void tick() {
		if (crew.isRemoved() || !TowerCrew.lit(crew.act()) || crew.heldProp() != TowerCrew.PROP_BATON) {
			crew.clientHum = null;
			stop();
			return;
		}
		follow();
		float k = Mth.clamp(++age / (float) SETTLE, 0F, 1F);
		volume = IDLE + (LOUD - IDLE) * (1 - k * k * (3 - 2 * k));
	}

	/** Notes when a crew member's act changes (its tick count), and when their baton lit (the draw's start). */
	static void track(TowerCrew crew) {
		int act = crew.act();
		if (crew.clientAct == act) {
			return;
		}
		if (act == TowerCrew.ACT_DRAW) {
			crew.clientLitSince = crew.tickCount;
		} else if (TowerCrew.lit(act) && !TowerCrew.lit(crew.clientAct)) {
			crew.clientLitSince = crew.tickCount - START;      // already lit when first seen: hum at once
		}
		crew.clientAct = act;
		crew.clientActSince = crew.tickCount;
	}

	/** Each client tick: the crew's acts noted, and a hum started for each baton lit long enough that hasn't one. */
	static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.isPaused()) {
			return;
		}
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof TowerCrew crew)) {
				continue;
			}
			track(crew);
			if (crew.clientHum == null && TowerCrew.lit(crew.act()) && crew.heldProp() == TowerCrew.PROP_BATON
					&& crew.tickCount - crew.clientLitSince >= START && client.player != null && client.player.distanceToSqr(crew) < 24 * 24) {
				BatonHum hum = new BatonHum(crew);
				crew.clientHum = hum;
				client.getSoundManager().play(hum);
			}
		}
	}
}

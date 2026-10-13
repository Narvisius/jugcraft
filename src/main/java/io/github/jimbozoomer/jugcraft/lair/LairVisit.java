package io.github.jimbozoomer.jugcraft.lair;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * A player's visit to a lair, saved with the player while they are inside: which instance they are in, and exactly where
 * they stood when they went in (dimension, position and facing), where they go back to.
 */
public record LairVisit(String lair, int slot, UUID instance, String dimension, double x, double y, double z, float yRot, float xRot) {
	public static final Codec<LairVisit> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("lair").forGetter(LairVisit::lair),
			Codec.INT.fieldOf("slot").forGetter(LairVisit::slot),
			UUIDUtil.CODEC.fieldOf("instance").forGetter(LairVisit::instance),
			Codec.STRING.fieldOf("dimension").forGetter(LairVisit::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(LairVisit::x),
			Codec.DOUBLE.fieldOf("y").forGetter(LairVisit::y),
			Codec.DOUBLE.fieldOf("z").forGetter(LairVisit::z),
			Codec.FLOAT.fieldOf("y_rot").forGetter(LairVisit::yRot),
			Codec.FLOAT.fieldOf("x_rot").forGetter(LairVisit::xRot)).apply(i, LairVisit::new));
}

package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The plasma baton item's plasma: {@code {"type": "jugcraft:baton_plasma", "flat": false}} in its item definition
 * (assets/jugcraft/items/plasma_baton.json, tools/plasma_baton.py), drawn over its lit model wherever the item is drawn
 * ({@link BatonPlasma}); {@code flat} for a menu's slot. A guard's baton doesn't come this way: its plasma grows as it
 * lights, drawn by TowerCrewRenderer.
 */
public final class BatonPlasmaItem implements NoDataSpecialModelRenderer {
	/** Whose it is, for the flicker: every item's the same (nothing here says which stack it is). */
	private static final int SEED = 7;
	private final boolean flat;

	private BatonPlasmaItem(boolean flat) {
		this.flat = flat;
	}

	public static void register() {
		SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("baton_plasma"), Unbaked.MAP_CODEC);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
		// a slot's picture is kept once drawn: still there, moving anywhere else
		BatonPlasma.submit(pose, collector, 1F, flat, SEED, flat ? 0 : BatonPlasma.now());
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		// the box round the glowing air
		float x0 = (BatonPlasma.ROD_FROM - 1.3F + BatonPlasma.GX) / 16F, x1 = (BatonPlasma.TIP + 1.65F + BatonPlasma.GX) / 16F, r = 2.3F / 16F;
		for (int corner = 0; corner < 8; corner++) {
			output.accept(new Vector3f((corner & 1) == 0 ? x0 : x1, 0.5F + ((corner & 2) == 0 ? -r : r), 0.5F + ((corner & 4) == 0 ? -r : r)));
		}
	}

	record Unbaked(boolean flat) implements NoDataSpecialModelRenderer.Unbaked {
		static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.BOOL.optionalFieldOf("flat", false).forGetter(Unbaked::flat)).apply(i, Unbaked::new));

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
			return new BatonPlasmaItem(flat);
		}
	}
}

package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Assay;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Mixture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * The Alembic Crucible as it really is (roadmap step 27). GeckoLib draws the pot and plays its clip (empty, still or
 * simmering) as before; this renderer draws the liquid itself: standing at the mixture's volume (one pixel a part, of
 * {@value Mixture#MAX_PARTS}) and coloured by what the server sent, the spoon's reading: its strongest property, and
 * murky when it is fouled. The liquid's texture is pale so that the colour shows true. A client never learns more of
 * the mixture than a spoon would tell, and nothing here decides anything.
 */
public final class CrucibleRenderer extends GeoBlockRenderer<CrucibleBlockEntity, BlockEntityRenderState> {
	private static final DataTicket<Integer> COLOUR = DataTicket.create("jugcraft_crucible_colour", Integer.class);
	private static final DataTicket<Float> FILL = DataTicket.create("jugcraft_crucible_fill", Float.class);
	/** The model's liquid bone: pivot at its floor, six pixels tall. */
	private static final String LIQUID = "liquid";
	/** Plain water, as the liquid looked before step 27. */
	static final int WATER = 0xFF46A0A0;
	/** What fouling mixes into the colour. */
	static final int MURK = 0xFF5A4E34;

	public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
		super(context, JugcraftConcordance.CRUCIBLE_ENTITY);
	}

	/** The liquid's colour for what a spoon would find (each property its own hue; murky, half mud). */
	public static int colour(Assay.Look look) {
		int base = look.taste() == null ? WATER : switch (look.taste()) {
			case RADIANCE -> 0xFFFFD85A;
			case VERDANCE -> 0xFF6EC85A;
			case EMBER -> 0xFFF06E32;
			case RIME -> 0xFFB4E6FA;
			case TIDE -> 0xFF326EDC;
			case HOLLOW -> 0xFF8C5AAA;
		};
		return look.murky() ? mix(base, MURK) : base;
	}

	private static int mix(int a, int b) {
		int red = (ARGB.red(a) + ARGB.red(b)) / 2;
		int green = (ARGB.green(a) + ARGB.green(b)) / 2;
		int blue = (ARGB.blue(a) + ARGB.blue(b)) / 2;
		return ARGB.color(255, red, green, blue);
	}

	@Override
	public void addRenderData(CrucibleBlockEntity crucible, @Nullable Void ignored, BlockEntityRenderState state, float partialTick) {
		state.addGeckolibData(COLOUR, colour(crucible.look()));
		state.addGeckolibData(FILL, Math.clamp(crucible.parts() / (float) Mixture.MAX_PARTS, 0.0F, 1.0F));
	}

	/** The pot's own pass leaves the liquid out; it is drawn on its own, tinted (below). */
	@Override
	public void preRenderPass(RenderPassInfo<BlockEntityRenderState> info, SubmitNodeCollector renderTasks) {
		super.preRenderPass(info, renderTasks);
		info.model().getBone(LIQUID).filter(CuboidGeoBone.class::isInstance).ifPresent(bone -> info.addPerBoneRender(bone, this::renderLiquid));
	}

	/** The liquid stands at the mixture's volume: its height (from its floor) times the fill, after the clip's own bob. */
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots snapshots) {
		float fill = info.getOrDefaultGeckolibData(FILL, 0.0F);
		snapshots.get(LIQUID).ifPresent(liquid -> liquid.setScaleY(liquid.getScaleY() * fill).skipRender(true));
	}

	private void renderLiquid(RenderPassInfo<BlockEntityRenderState> info, GeoBone bone, SubmitNodeCollector renderTasks) {
		BlockEntityRenderState state = info.renderState();
		RenderType type = getRenderType(state, getTextureLocation(state));
		if (type == null || info.getOrDefaultGeckolibData(FILL, 0.0F) <= 0.0F) {
			return;
		}
		int colour = ARGB.multiply(info.renderColor(), info.getOrDefaultGeckolibData(COLOUR, WATER));
		int light = info.packedLight();
		int overlay = info.packedOverlay();
		renderTasks.submitCustomGeometry(info.poseStack(), type, (pose, buffer) -> {
			PoseStack poseStack = new PoseStack();
			poseStack.last().set(pose);
			bone.translateAwayFromPivotPoint(poseStack);
			for (GeoCube cube : ((CuboidGeoBone) bone).cubes) {
				poseStack.pushPose();
				cube.render(poseStack, buffer, light, overlay, colour);
				poseStack.popPose();
			}
		});
	}
}

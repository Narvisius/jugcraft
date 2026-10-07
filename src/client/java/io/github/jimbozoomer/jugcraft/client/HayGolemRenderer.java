package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HayGolem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Hay Golem ({@link HayGolemModel}) and the head it wears: the pumpkin, twelve pixels a side, sitting on its
 * collar and turning as its head turns. A hand-carved head is the plain pumpkin it was carved from with its carving drawn
 * over it, as {@link ScarecrowRenderer} draws a scarecrow's, glowing at full brightness when lit.
 */
public class HayGolemRenderer extends MobRenderer<HayGolem, HayGolemRenderer.State, HayGolemModel> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/entity/hay_golem.png");
	/** The head's size in blocks. */
	private static final float HEAD = 12.0F / 16.0F;
	/** A block item drawn FIXED is half a block. */
	private static final float FIXED = 0.5F;
	/** How far outside the pumpkin the carving sits, in pumpkin widths. */
	private static final float OUT = 0.004F;

	private final ItemModelResolver itemModels;

	public static class State extends LivingEntityRenderState {
		final ItemStackRenderState head = new ItemStackRenderState();
		boolean working;
		PumpkinCarving carving = PumpkinCarving.BLANK;
		@Nullable RenderType carvingType;
		boolean lit;
	}

	public HayGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new HayGolemModel(context.bakeLayer(HayGolemModel.LAYER)), 0.6F);
		itemModels = context.getItemModelResolver();
		addLayer(new HeadLayer(this));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HayGolem golem, State state, float partialTick) {
		super.extractRenderState(golem, state, partialTick);
		state.working = golem.working();
		state.carvingType = null;
		ItemStack head = golem.head();
		ItemStack pumpkin = head;
		state.lit = ScarecrowBlockEntity.lit(head);
		if (head.getItem() instanceof BlockItem item && item.getBlock() instanceof CarvedPumpkinBlock) {
			// A hand-carved pumpkin is modelled as the plain pumpkin with the carving drawn over it: do the same.
			Block plain = JugcraftAgriculture.plainPumpkin(item.getBlock());
			pumpkin = plain == null ? ItemStack.EMPTY : new ItemStack(plain);
			state.carving = head.getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK);
			if (!state.carving.isBlank()) {
				state.carvingType = CarvingTextures.get(state.carving, state.lit, ScarecrowBlockEntity.soul(head), CarvingTextures.Glow.of(head));
			}
		}
		itemModels.updateForTopItem(state.head, pumpkin, ItemDisplayContext.FIXED, golem.level(), null, golem.getId());
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	/** The pumpkin on the golem's collar, and its carving. */
	static class HeadLayer extends RenderLayer<State, HayGolemModel> {
		HeadLayer(RenderLayerParent<State, HayGolemModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
			RenderType type = state.carvingType;
			if (state.head.isEmpty() && type == null) {
				return;
			}
			pose.pushPose();
			getParentModel().body().translateAndRotate(pose);
			getParentModel().head().translateAndRotate(pose);
			// Model space has y down and the golem facing -z: turn about and flip y and z so the pumpkin stands the right
			// way up with its face to the front.
			pose.translate(0.0F, -HEAD / 2.0F, 0.0F);
			pose.rotateDegrees(Axis.YP, 180.0F);
			if (!state.head.isEmpty()) {
				pose.pushPose();
				float scale = HEAD / FIXED;
				pose.scale(scale, -scale, -scale);
				state.head.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
				pose.popPose();
			}
			if (type != null) {
				PumpkinCarving carving = state.carving;
				int glow = state.lit ? LightCoordsUtil.FULL_BRIGHT : light;
				float scale = HEAD * (1.0F + 2.0F * OUT);
				pose.scale(scale, -scale, -scale);
				pose.translate(-0.5F, -0.5F, -0.5F);
				collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
					for (int face = 0; face < PumpkinCarving.FACES; face++) {
						if (!carving.isBlank(face)) {
							CarvedPumpkinRenderer.side(buffer, matrix, PumpkinCarving.side(Direction.NORTH, face), face, glow);
						}
					}
				});
			}
			pose.popPose();
		}
	}
}

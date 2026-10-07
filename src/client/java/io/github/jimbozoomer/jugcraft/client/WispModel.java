package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A will-o'-wisp: a bright core inside a slowly turning, see-through halo, bobbing in the air. Drawn glowing and
 * see-through (texture: tools/night_textures.py).
 */
public class WispModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("will_o_wisp"), "main");

	private final ModelPart core;
	private final ModelPart halo;

	public WispModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucentEmissive);
		core = root.getChild("core");
		halo = root.getChild("halo");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
		root.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(0, 8).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float bob = Mth.sin(state.ageInTicks * 0.15F) * 1.5F;
		core.y = 20.0F + bob;
		halo.y = 20.0F + bob;
		halo.yRot = state.ageInTicks * 0.05F;
		halo.xRot = state.ageInTicks * 0.03F;
	}
}

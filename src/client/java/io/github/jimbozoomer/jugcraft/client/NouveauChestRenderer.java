package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauChest;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Vanilla lid motion and double-chest joining with supplied single/left/right textures. */
public final class NouveauChestRenderer extends ChestRenderer<NouveauChest.Chest> {
    private final ChestModel single=new ChestModel(ChestModel.createSingleBodyLayer().bakeRoot()),left=new ChestModel(ChestModel.createDoubleBodyLeftLayer().bakeRoot()),right=new ChestModel(ChestModel.createDoubleBodyRightLayer().bakeRoot());
    public NouveauChestRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override public void submit(ChestRenderState s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){pose.pushPose();pose.mulPose(modelTransformation(s.facing));var model=switch(s.type){case LEFT->left;case RIGHT->right;default->single;};String suffix=switch(s.type){case LEFT->"_left";case RIGHT->"_right";default->"";};float open=1-(float)Math.pow(1-s.open,3);collector.submitModel(model,open,pose,Jugcraft.id("textures/nouveau/entity/chest/archwood_chest"+suffix+".png"),s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}

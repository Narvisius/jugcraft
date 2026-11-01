package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildWroughtEquipment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Original horn/tusk geometry follows the wearer's native head pose. */
public final class WildArmorRenderer {
    private static Helm model;
    static void register(){net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("wilds_armor_models"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->model=null);}
    public static boolean draw(PoseStack pose,SubmitNodeCollector out,ItemStack stack,EquipmentSlot slot,int light,HumanoidRenderState state){if(!(stack.getItem() instanceof WildWroughtEquipment.Helmet)||slot!=EquipmentSlot.HEAD)return false;if(model==null)model=new Helm(RecoveredWildlifeModel.bake(RecoveredWildlifeModel.read("recovered/wilds_wrought_helmet")));var texture=Jugcraft.id("textures/newest/wilds/item/wrought_helmet.png");out.order(0).submitModel(model,state,pose,stack.hasFoil()?RenderTypes.armorCutoutNoCullGlint(texture):RenderTypes.entityCutout(texture),light,OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor);return true;}
    private static final class Helm extends HumanoidModel<HumanoidRenderState>{Helm(ModelPart root){super(root);}@Override public void setupAnim(HumanoidRenderState s){super.setupAnim(s);head.visible=hat.visible=body.visible=leftArm.visible=rightArm.visible=leftLeg.visible=rightLeg.visible=false;head.visible=true;}}
}

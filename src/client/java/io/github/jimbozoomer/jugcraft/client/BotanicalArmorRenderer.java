package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalArmor;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Original supplied rigs follow vanilla humanoid animation, including crouching and swimming. */
public final class BotanicalArmorRenderer {
    private static final Map<String,ArmorModel> MODELS=new HashMap<>();
    public static void register(){net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("botanical_armor_models"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->MODELS.clear());}
    public static boolean draw(PoseStack pose,SubmitNodeCollector collector,ItemStack stack,EquipmentSlot slot,int light,HumanoidRenderState state){
        if(!(stack.getItem() instanceof BotanicalArmor.Armor armor))return false;
        if(BotanicalArmor.invisible(stack))return true;
        var model=MODELS.computeIfAbsent(armor.family+"/"+slot,k->{
            String layer=slot==EquipmentSlot.LEGS?"inside":"outside";
            try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_jugtania_models/"+armor.family+"_"+layer+".json")).openAsReader()){
                return new ArmorModel(RecoveredWildlifeModel.bake(JsonParser.parseReader(reader).getAsJsonObject()),slot);
            }catch(java.io.IOException e){throw new IllegalStateException("Missing supplied botanical armor "+k,e);}
        });
        String holiday=armor.family.equals("manaweave")&&java.time.LocalDate.now().getMonthValue()==12?"_holiday":"";
        var texture=Jugcraft.id("textures/newest/jugtania/model/armor_"+armor.family+holiday+".png");
        collector.order(0).submitModel(model,state,pose,(stack.hasFoil()?RenderTypes.armorCutoutNoCullGlint(texture):RenderTypes.entityCutout(texture)),light,OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor);
        return true;
    }
    private static final class ArmorModel extends HumanoidModel<HumanoidRenderState> {
        private final EquipmentSlot slot;
        ArmorModel(ModelPart root,EquipmentSlot slot){super(root);this.slot=slot;}
        @Override public void setupAnim(HumanoidRenderState state){
            super.setupAnim(state);head.visible=hat.visible=body.visible=leftArm.visible=rightArm.visible=leftLeg.visible=rightLeg.visible=false;
            switch(slot){case HEAD-> {head.visible=true;hat.visible=true;}case CHEST->{body.visible=true;leftArm.visible=true;rightArm.visible=true;}case LEGS->{body.visible=true;leftLeg.visible=true;rightLeg.visible=true;}case FEET->{leftLeg.visible=true;rightLeg.visible=true;}default->{}}
        }
    }
}

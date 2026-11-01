package io.github.jimbozoomer.jugcraft.client;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalPixie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
public final class BotanicalPixieRenderer extends MobRenderer<BotanicalPixie,LivingEntityRenderState,BotanicalPixieRenderer.Model> {
    public BotanicalPixieRenderer(EntityRendererProvider.Context c){super(c,new Model(load()),.15F);}
    private static ModelPart load(){try(var r=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_jugtania_models/PixieModel.json")).openAsReader()){return RecoveredWildlifeModel.bake(JsonParser.parseReader(r).getAsJsonObject());}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
    @Override public Identifier getTextureLocation(LivingEntityRenderState s){return Jugcraft.id("textures/newest/jugtania/model/pixie"+(java.time.LocalDate.now().getMonthValue()==10?"_halloween":"")+".png");}
    public static final class Model extends EntityModel<LivingEntityRenderState>{
        private final ModelPart rig;Model(ModelPart root){super(root);rig=root;}
        @Override public void setupAnim(LivingEntityRenderState s){super.setupAnim(s);float angle=(float)(Math.cos(s.ageInTicks*1.7)*Math.PI);rig.getChild("rightWingT").yRot=-angle*.5F;rig.getChild("leftWingT").yRot=angle*.5F;rig.getChild("rightWingB").yRot=-angle*.25F;rig.getChild("leftWingB").yRot=angle*.25F;}
    }
}

package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneBoats;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

final class ArcaneBoatRenderer {
    static void register(){
        ArcaneBoats.BOATS.forEach((wood,type)->EntityRendererRegistry.register(type,c->new AbstractBoatRenderer(c,Jugcraft.id("textures/newest/forbidden/entity/boat/"+wood+".png")){
            private final EntityModel<BoatRenderState> model=new BoatModel(c.bakeLayer(ModelLayers.OAK_BOAT));
            @Override protected EntityModel<BoatRenderState> model(){return model;}
        }));
        ArcaneBoats.CHESTS.forEach((wood,type)->EntityRendererRegistry.register(type,c->new AbstractBoatRenderer(c,Jugcraft.id("textures/newest/forbidden/entity/chest_boat/"+wood+".png")){
            private final EntityModel<BoatRenderState> model=new BoatModel(c.bakeLayer(ModelLayers.OAK_CHEST_BOAT));
            @Override protected EntityModel<BoatRenderState> model(){return model;}
        }));
    }
}

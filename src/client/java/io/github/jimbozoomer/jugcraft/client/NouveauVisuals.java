package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.animatable.*;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.*;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Original geometry, texture atlases and clips, selected from saved gameplay state. */
public final class NouveauVisuals {
    static final DataTicket<String> MODEL=DataTicket.create("jugcraft_nouveau_model",String.class);
    static final DataTicket<String> TEXTURE=DataTicket.create("jugcraft_nouveau_texture",String.class);
    private NouveauVisuals(){}
    static final class Model<T extends GeoAnimatable> extends GeoModel<T> {
        final String group,fallback;final Function<T,String> animation;
        Model(String group,String fallback,Function<T,String> animation){this.group=group;this.fallback=fallback;this.animation=animation;}
        @Override public Identifier getModelResource(GeoRenderState s){String name=s.getGeckolibData(MODEL);return Jugcraft.id(""+group+"/nouveau/"+(name==null?fallback:name)+"");}
        @Override public Identifier getTextureResource(GeoRenderState s){String texture=s.getGeckolibData(TEXTURE);return Jugcraft.id("textures/nouveau/"+(texture==null?group+"/"+fallback:texture)+".png");}
        @Override public Identifier getAnimationResource(T item){return Jugcraft.id(""+group+"/nouveau/"+animation.apply(item)+"");}
    }
    public static String familiarModel(String species){return species.equals("bookwyrm")?"book_wyrm":species;}
    public static String creatureTexture(String species){return "entity/"+switch(species){case "drygmy"->"drygmy_brown";case "wixie"->"wixie_black";case "whirlisprig"->"whirlisprig_spring";case "starbuncle"->"starbuncle_yellow";case "book_wyrm"->"book_wyrm_purple";default->species;};}
    public static void register(){
        BlockEntityRenderers.register(NouveauChest.TYPE,NouveauChestRenderer::new);
        NouveauParticle.register();NouveauFillProperty.register();
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(List.of(new net.minecraft.client.color.block.BlockTintSource(){
            public int color(net.minecraft.world.level.block.state.BlockState state){return 0xFFFFFFFF;}
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter level,net.minecraft.core.BlockPos at){return level.getBlockEntity(at) instanceof NouveauWorkshop jar?0xFF000000|jar.potion().getColor():0xFFFFFFFF;}
        }),NouveauCatalog.block("potion_jar"));
        EntityRendererRegistry.register(NouveauBoat.TYPE,context->new AbstractBoatRenderer(context,Jugcraft.id("textures/nouveau/entity/boat/archwood.png")){
            private final net.minecraft.client.model.EntityModel<BoatRenderState> boat=new net.minecraft.client.model.object.boat.BoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.OAK_BOAT));
            @Override protected net.minecraft.client.model.EntityModel<BoatRenderState> model(){return boat;}
        });
        MenuScreens.register(NouveauWorkshops.MENU,NouveauWorkshopScreen::new);
        MenuScreens.register(NouveauFamiliars.MENU,NouveauFamiliarScreen::new);
        BlockEntityRenderers.register(NouveauWorkshops.ENTITY,NouveauWorkshopRenderer::new);
        BlockEntityRenderers.register(NouveauAnimatedDecor.TYPE,context->new GeoBlockRenderer<NouveauAnimatedDecor,BlockEntityRenderState>(context,new Model<>("block","mage_block",b->b.entry().geo())){
            public void captureDefaultRenderState(NouveauAnimatedDecor b,Void unused,BlockEntityRenderState s,float partial){s.addGeckolibData(MODEL,b.entry().geo());s.addGeckolibData(TEXTURE,"block/"+NouveauPresentation.texture(b.entry().geo()));super.captureDefaultRenderState(b,unused,s,partial);}
        });
        Map<String,GeoRenderProvider> workshops=new HashMap<>();
        NouveauWorkshopItem.renderer=e->workshops.computeIfAbsent(e.source(),key->new GeoRenderProvider(){
            final GeoItemRenderer<NouveauWorkshopItem> renderer=new GeoItemRenderer<>(new GeoModel<>(){
                @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("item/nouveau/"+e.geo()+"");}
                @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/nouveau/block/"+NouveauPresentation.texture(e.geo())+".png");}
                @Override public Identifier getAnimationResource(NouveauWorkshopItem item){return Jugcraft.id("item/nouveau/"+e.geo()+"");}
            });
            @Override public GeoItemRenderer<?> getGeoItemRenderer(){return renderer;}
        });
        Map<String,GeoRenderProvider> instruments=new HashMap<>();
        NouveauInstruments.renderer=name->{
            var entry=NouveauCatalog.ITEM_ENTRIES.get(name);if(entry==null||entry.geo().isEmpty())return null;
            return instruments.computeIfAbsent(name,key->new GeoRenderProvider(){
                final GeoItemRenderer<?> renderer=NouveauEquipmentRenderer.item(entry);
                @Override public GeoItemRenderer<?> getGeoItemRenderer(){return renderer;}
            });
        };
        for(var entry:NouveauHelpers.TYPES.entrySet())EntityRendererRegistry.register(entry.getValue(),context->new GeoEntityRenderer<NouveauHelper,EntityRenderState>(context,new Model<>("entity",entry.getKey(),h->h.species())){
            @Override public void captureDefaultRenderState(NouveauHelper e,Void unused,EntityRenderState s,float partial){s.addGeckolibData(MODEL,e.species());s.addGeckolibData(TEXTURE,e.texture());super.captureDefaultRenderState(e,unused,s,partial);}
        });
        for(var entry:NouveauEncounters.TYPES.entrySet())EntityRendererRegistry.register(entry.getValue(),context->new GeoEntityRenderer<NouveauBeast,EntityRenderState>(context,new Model<>("entity",entry.getKey(),e->e.species())){
            @Override public void captureDefaultRenderState(NouveauBeast e,Void unused,EntityRenderState s,float partial){s.addGeckolibData(MODEL,e.species());s.addGeckolibData(TEXTURE,creatureTexture(e.species()));super.captureDefaultRenderState(e,unused,s,partial);}
        });
        EntityRendererRegistry.register(NouveauFamiliars.TYPE,context->new GeoEntityRenderer<NouveauFamiliar,EntityRenderState>(context,new Model<>("entity","book_wyrm",e->familiarModel(e.species()))){
            @Override public void captureDefaultRenderState(NouveauFamiliar e,Void unused,EntityRenderState s,float partial){String model=familiarModel(e.species());s.addGeckolibData(MODEL,model);s.addGeckolibData(TEXTURE,creatureTexture(model));super.captureDefaultRenderState(e,unused,s,partial);}
        }.withScale(.6F));
        Map<String,GeoRenderProvider> armor=new HashMap<>();
        NouveauGear.armorRenderer=item->armor.computeIfAbsent(item.entry.source().split("_")[0],set->new GeoRenderProvider(){
            final GeoArmorRenderer<NouveauGear.Armor,HumanoidRenderState> renderer=NouveauEquipmentRenderer.armor(set);
            @Override public GeoArmorRenderer<?,?> getGeoArmorRenderer(ItemStack stack,EquipmentSlot slot){return renderer;}
        });
        BlockEntityRenderers.register(NouveauDecor.SIGN,StandingSignRenderer::new);
        BlockEntityRenderers.register(NouveauDecor.HANGING_SIGN,HangingSignRenderer::new);
        BlockEntityRenderers.register(NouveauWeaves.TYPE,NouveauFacadeRenderer::new);
        EntityRendererRegistry.register(NouveauAnimatedBlock.TYPE,NouveauAnimatedRenderer::new);
        EntityRendererRegistry.register(NouveauSpine.TYPE,context->new GeoEntityRenderer<NouveauSpine,EntityRenderState>(context,new Model<>("entity","chimera_spike",e->"chimera_spike")));
    }
}

package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public final class MenagerieClient {
    private static final DataTicket<String> TEXTURE=DataTicket.create("jugcraft_menagerie_skin",String.class);
    private static final DataTicket<String> MODEL=DataTicket.create("jugcraft_menagerie_model",String.class);
    private static final DataTicket<String> ARMOR=DataTicket.create("jugcraft_menagerie_armor",String.class);
    private static final DataTicket<Boolean> SADDLE=DataTicket.create("jugcraft_menagerie_saddle",Boolean.class),CHEST=DataTicket.create("jugcraft_menagerie_chest",Boolean.class);
    private MenagerieClient(){}
    public static void register(){
        net.minecraft.client.gui.screens.MenuScreens.register(HouseholdMenu.HEAT,HouseholdScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(HouseholdMenu.COLD,HouseholdScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(HouseholdMenu.CAPSID,HouseholdScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(CaveWorkshop.TYPE,CaveWorkshopScreen::new);
        for(var b:Household.BLOCKS.values())if(b instanceof HouseholdBlock furniture&&(furniture.kind.equals("sink")||furniture.name.endsWith("bath")))net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.constant(0xFF3F76E4)),b);
        net.minecraft.client.gui.screens.MenuScreens.register(CavernDeviceMenu.TYPE,CavernDeviceScreen::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(Household.FAN,HouseholdFanRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(CavernDevice.TYPE,CavernDeviceRenderer::new);
        Ecology.BOATS.forEach((wood,type)->EntityRendererRegistry.register(type,context->new net.minecraft.client.renderer.entity.AbstractBoatRenderer(context,Jugcraft.id("textures/menagerie/ecology/entity/boat/"+wood+".png")){
            private final net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> boat=new net.minecraft.client.model.object.boat.BoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.OAK_BOAT));
            @Override protected net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> model(){return boat;}
        }));
        Ecology.CHEST_BOATS.forEach((wood,type)->EntityRendererRegistry.register(type,context->new net.minecraft.client.renderer.entity.AbstractBoatRenderer(context,Jugcraft.id("textures/menagerie/ecology/entity/chest_boat/"+wood+".png")){
            private final net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> boat=new net.minecraft.client.model.object.boat.BoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.OAK_CHEST_BOAT));
            @Override protected net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> model(){return boat;}
        }));
        var wearableRenderers=new java.util.HashMap<String,com.geckolib.animatable.client.GeoRenderProvider>();
        GroveCreatures.renderer=item->wearableRenderers.computeIfAbsent(item.name,name->new com.geckolib.animatable.client.GeoRenderProvider(){
            final String model=item.name.equals("companion_acorn")?"companion_acorn":item.name.equals("sporeling_backpack")?"sporeling_backpack":"flower_crown";
            final com.geckolib.renderer.GeoArmorRenderer<GroveCreatures.Wearable,net.minecraft.client.renderer.entity.state.HumanoidRenderState> renderer=new com.geckolib.renderer.GeoArmorRenderer<>(new GeoModel<>(){
                public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("armor/menagerie/"+model);}
                public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id(model.equals("companion_acorn")?"textures/menagerie/companion/armor/acorn.png":"textures/menagerie/grove/armor/"+model+".png");}
                public Identifier getAnimationResource(GroveCreatures.Wearable a){return Jugcraft.id("armor/menagerie/"+model);}
            });
            @Override public com.geckolib.renderer.GeoArmorRenderer<?,?> getGeoArmorRenderer(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.EquipmentSlot slot){return renderer;}
        });
        if(FableCreatures.DART!=null)EntityRendererRegistry.register(FableCreatures.DART,context->new net.minecraft.client.renderer.entity.ArrowRenderer<FableCreatures.Dart,net.minecraft.client.renderer.entity.state.ArrowRenderState>(context){
            public net.minecraft.client.renderer.entity.state.ArrowRenderState createRenderState(){return new net.minecraft.client.renderer.entity.state.ArrowRenderState();}
            protected Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ArrowRenderState s){return Jugcraft.id("textures/menagerie/fable/entity/jinx_dart.png");}
        });
        if(FableCreatures.PURSE!=null)EntityRendererRegistry.register(FableCreatures.PURSE,context->new GeoEntityRenderer<FableCreatures.Purse,EntityRenderState>(context,new GeoModel<>(){
            public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/menagerie/fable/wisp_purse");}
            public Identifier getTextureResource(GeoRenderState s){String skin=s.getGeckolibData(TEXTURE);return Jugcraft.id("textures/menagerie/fable/entity/"+(skin==null?"wisp":skin)+".png");}
            public Identifier getAnimationResource(FableCreatures.Purse a){return Jugcraft.id("entity/menagerie/fable/wisp_purse");}
        }){
            @Override public void captureDefaultRenderState(FableCreatures.Purse purse,Void unused,EntityRenderState state,float partial){state.addGeckolibData(TEXTURE,purse.getItem().is(net.minecraft.world.item.Items.GLOWSTONE_DUST)?"nether_wisp":purse.getItem().is(net.minecraft.world.item.Items.ENDER_PEARL)?"ender_wisp":"wisp");super.captureDefaultRenderState(purse,unused,state,partial);}
        });
        OwnerArrow.TYPES.forEach((id,type)->EntityRendererRegistry.register(type,context->new net.minecraft.client.renderer.entity.ArrowRenderer<OwnerArrow,net.minecraft.client.renderer.entity.state.ArrowRenderState>(context){
            public net.minecraft.client.renderer.entity.state.ArrowRenderState createRenderState(){return new net.minecraft.client.renderer.entity.state.ArrowRenderState();}
            protected Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ArrowRenderState state){String group=id.startsWith("beast_")?"beast":"cavern";return Jugcraft.id("textures/menagerie/"+group+"/entity/"+id.substring(group.length()+1)+".png");}
        }));
        EntityRendererRegistry.register(CavernProjectile.TYPE,net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(CavernSubmarine.TYPE,context->new GeoEntityRenderer<CavernSubmarine,EntityRenderState>(context,new GeoModel<>(){
            public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/menagerie/cavern_submarine");}
            public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/menagerie/cavern/entity/submarine/submarine.png");}
            public Identifier getAnimationResource(CavernSubmarine a){return Jugcraft.id("entity/menagerie/cavern_submarine");}
        }));
        EntityRendererRegistry.register(CavernSubmarine.BOARD,context->new GeoEntityRenderer<CavernSubmarine,EntityRenderState>(context,new GeoModel<>(){
            public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/menagerie/beast_straddleboard");}
            public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/menagerie/beast/entity/straddleboard.png");}
            public Identifier getAnimationResource(CavernSubmarine a){return Jugcraft.id("entity/menagerie/beast_straddleboard");}
        }));
        NaturalCollections.PARTICLES.forEach(type->net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(type,LandscapeParticle::provider));
        NaturalCollections.BOATS.forEach((id,type)->EntityRendererRegistry.register(type,context->new net.minecraft.client.renderer.entity.AbstractBoatRenderer(context,Jugcraft.id(NaturalCollections.BOAT_TEXTURES.get(id))){
            private final net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> boat=new net.minecraft.client.model.object.boat.BoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.OAK_BOAT));
            @Override protected net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> model(){return boat;}
        }));
        NaturalCollections.CHEST_BOATS.forEach((id,type)->EntityRendererRegistry.register(type,context->new net.minecraft.client.renderer.entity.AbstractBoatRenderer(context,Jugcraft.id(NaturalCollections.BOAT_TEXTURES.get(id))){
            private final net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> boat=new net.minecraft.client.model.object.boat.BoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.OAK_CHEST_BOAT));
            @Override protected net.minecraft.client.model.EntityModel<net.minecraft.client.renderer.entity.state.BoatRenderState> model(){return boat;}
        }));
        MenagerieItems.openGuide=()->EncyclopediaClient.open(net.minecraft.client.Minecraft.getInstance(),EncyclopediaScreen.Back.GAME);
        Menagerie.TYPES.forEach((id,type)->{if(RecoveredWildlifeRenderer.supports(id))return;EntityRendererRegistry.register(type,context->new GeoEntityRenderer<MenagerieAnimal,EntityRenderState>(context,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){String model=s.getGeckolibData(MODEL);return Jugcraft.id("entity/menagerie/"+(model==null?Menagerie.SPECIES.get(id).model():model));}
            @Override public Identifier getTextureResource(GeoRenderState s){String t=s.getGeckolibData(TEXTURE);return Jugcraft.id("textures/"+(t==null?Menagerie.SPECIES.get(id).variants().getFirst().texture():t)+".png");}
            @Override public Identifier getAnimationResource(MenagerieAnimal a){return Jugcraft.id("entity/menagerie/"+a.species().model());}
        }){
            @Override public void captureDefaultRenderState(MenagerieAnimal a,Void unused,EntityRenderState s,float partial){s.addGeckolibData(TEXTURE,a.texture());s.addGeckolibData(MODEL,a.model());s.addGeckolibData(ARMOR,a.species().armorTextures().getOrDefault(a.armor(),""));s.addGeckolibData(SADDLE,a.saddled());s.addGeckolibData(CHEST,a.chested());super.captureDefaultRenderState(a,unused,s,partial);}
        }.withRenderLayer(renderer->new com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer<MenagerieAnimal,Void,EntityRenderState>(renderer,Jugcraft.id("textures/menagerie/mount/model/entity/armor/acencia_armor_iron.png")){
            @Override protected Identifier getTextureResource(EntityRenderState s){return Jugcraft.id("textures/"+s.getGeckolibData(ARMOR)+".png");}
            @Override protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(EntityRenderState s){String texture=s.getGeckolibData(ARMOR);return texture==null||texture.isEmpty()?null:net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(getTextureResource(s));}
            @Override public void preRender(com.geckolib.renderer.base.RenderPassInfo<EntityRenderState> pass,net.minecraft.client.renderer.SubmitNodeCollector collector){
                pass.addBoneUpdater((info,bones)->{
                    boolean saddle=Boolean.TRUE.equals(info.getGeckolibData(SADDLE));
                    boolean chest=Boolean.TRUE.equals(info.getGeckolibData(CHEST));
                    for(String name:new String[]{"saddle","bridle"})bones.get(name).ifPresent(b->b.skipRender(!saddle).skipChildrenRender(!saddle));
                    bones.get("chests").ifPresent(b->b.skipRender(!chest).skipChildrenRender(!chest));
                });
            }
        }).withRenderLayer(renderer->new com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer<MenagerieAnimal,Void,EntityRenderState>(renderer){
            @Override protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(EntityRenderState state){
                var texture=getTextureResource(state);
                // Most supplied creatures have no emissive map. Rendering a missing map
                // covers their valid base skin with Minecraft's missing-texture pattern.
                return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()?super.getRenderType(state):null;
            }
        }).withRenderLayer(renderer->new com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer<MenagerieAnimal,Void,EntityRenderState>(context,renderer){
            @Override protected java.util.List<RenderData> getRelevantBones(MenagerieAnimal animal,Void unused,EntityRenderState state,float partial){
                if(!animal.species().id().equals("companion_roly_poly")||!animal.chested()||animal.isBaby())return java.util.List.of();
                var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST);stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,Jugcraft.id("companion_roly_pack"));
                var itemState=new net.minecraft.client.renderer.item.ItemStackRenderState();itemModelResolver.updateForLiving(itemState,stack,net.minecraft.world.item.ItemDisplayContext.NONE,animal);
                return java.util.List.of(RenderData.item("body",net.minecraft.world.item.ItemDisplayContext.NONE,itemState));
            }
            @Override public void addRenderData(MenagerieAnimal animal,Void unused,EntityRenderState state,float partial){state.addGeckolibData(CONTENTS,getRelevantBones(animal,unused,state,partial));}
        }));});
        RecoveredWildlifeRenderer.register();
        RecoveredCreaturePartRenderer.register();
        EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.menagerie.RottenStorage.HUNTER_WOLF, context->new net.minecraft.client.renderer.entity.WolfRenderer(context){@Override protected void scale(net.minecraft.client.renderer.entity.state.WolfRenderState state,com.mojang.blaze3d.vertex.PoseStack pose){super.scale(state,pose);pose.scale(1.2F,1.2F,1.2F);}});
        EntityRendererRegistry.register(MenagerieSeasonal.PUMPKLING,context->new GeoEntityRenderer<MenagerieSeasonal.HerbalPumpkling,EntityRenderState>(context,new SeasonalModel<>("pumpkling")));
        EntityRendererRegistry.register(MenagerieSeasonal.GHOST,context->new GeoEntityRenderer<MenagerieSeasonal.VeilGhost,EntityRenderState>(context,new SeasonalModel<>("ghost")){
            @Override public void captureDefaultRenderState(MenagerieSeasonal.VeilGhost a,Void unused,EntityRenderState s,float partial){super.captureDefaultRenderState(a,unused,s,partial);s.isInvisible=!a.revealed();}
        });
    }
    private static final class SeasonalModel<T extends com.geckolib.animatable.GeoAnimatable> extends GeoModel<T>{
        private final String name;SeasonalModel(String name){this.name=name;}
        @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/menagerie/herbal_"+name);}
        @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/menagerie/herbal/entity/"+name+".png");}
        @Override public Identifier getAnimationResource(T a){return Jugcraft.id("entity/menagerie/herbal_"+name);}
    }
}


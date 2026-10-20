package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.*;
import com.geckolib.animatable.client.GeoRenderProvider;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** GeckoLib owns all posing; the server owns links, colors and movement. */
public final class JugNouveauClient {
    private static final DataTicket<Integer> COLOR=DataTicket.create("jugcraft_bookwyrm_color",Integer.class);
    private static final DataTicket<Integer> STAR_COLOR=DataTicket.create("jugcraft_starbuncle_color",Integer.class);
    private static final DataTicket<net.minecraft.client.renderer.item.ItemStackRenderState> CARGO=DataTicket.create("jugcraft_starbuncle_cargo",net.minecraft.client.renderer.item.ItemStackRenderState.class);
    private static final DataTicket<net.minecraft.client.renderer.item.ItemStackRenderState> HAT=DataTicket.create("jugcraft_starbuncle_hat",net.minecraft.client.renderer.item.ItemStackRenderState.class);
    private JugNouveauClient(){}
    public static void register(){
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(NouveauStorageTerminal.TabState.TYPE,(packet,context)->{if(context.client().player!=null&&context.client().player.containerMenu instanceof NouveauStorageTerminal.Menu menu&&menu.containerId==packet.menu())menu.visibleGroup=packet.label();});
        NouveauGear.openGuide=name->EncyclopediaClient.open(net.minecraft.client.Minecraft.getInstance(),EncyclopediaScreen.Back.GAME);
        NouveauVisuals.register();NouveauControlsClient.register();NouveauScryingClient.register();NouveauScrySightClient.register();NouveauBubbleClient.register();
        MenuScreens.register(NouveauRepositories.MENU,NouveauRepositoryScreen::new);MenuScreens.register(NouveauStorageTerminal.TYPE,NouveauStorageTerminalScreen::new);
        MenuScreens.register(NouveauPotionMenu.TYPE,NouveauPotionScreen::new);
        EntityRendererRegistry.register(NouveauPotionBolt.TYPE,net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(JugNouveau.STARBUNCLE,context->new GeoEntityRenderer<Starbuncle,EntityRenderState>(context,new NouveauVisuals.Model<>("entity","starbuncle",Starbuncle::renderStyle)){
            @Override public void addRenderData(Starbuncle star,@Nullable Void unused,EntityRenderState state,float partial){
                state.addGeckolibData(STAR_COLOR,star.color());state.addGeckolibData(NouveauVisuals.MODEL,star.renderStyle());
                var item=new net.minecraft.client.renderer.item.ItemStackRenderState();
                context.getItemModelResolver().updateForLiving(item,star.cargoDisplay(),net.minecraft.world.item.ItemDisplayContext.FIXED,star);
                state.addGeckolibData(CARGO,item);
                var hat=new net.minecraft.client.renderer.item.ItemStackRenderState();if(star.accessory()>0)context.getItemModelResolver().updateForLiving(hat,new net.minecraft.world.item.ItemStack(NouveauCatalog.item(star.accessory()==1?"starbuncle_shades":"wixie_hat")),net.minecraft.world.item.ItemDisplayContext.NONE,star);state.addGeckolibData(HAT,hat);
            }
            @Override public void preRenderPass(com.geckolib.renderer.base.RenderPassInfo<EntityRenderState> info,net.minecraft.client.renderer.SubmitNodeCollector tasks){
                super.preRenderPass(info,tasks);
                info.model().getBone("head").ifPresent(bone->info.addPerBoneRender(bone,(pass,part,collector)->{
                    var item=pass.getGeckolibData(HAT);if(item==null||item.isEmpty())return;var pose=pass.poseStack();pose.pushPose();pose.translate(0,-.25,.15625);item.submit(pose,collector,pass.packedLight(),pass.packedOverlay(),0);pose.popPose();
                }));
                info.model().getBone("item").ifPresent(bone->info.addPerBoneRender(bone,(pass,part,collector)->{
                    var item=pass.getGeckolibData(CARGO);if(item==null||item.isEmpty())return;
                    var pose=pass.poseStack();pose.pushPose();pose.translate(0,.16,0);pose.scale(.35F,.35F,.35F);
                    item.submit(pose,collector,pass.packedLight(),pass.packedOverlay(),0);pose.popPose();
                }));
            }
            @Override public Identifier getTextureLocation(EntityRenderState state){
                String model=state.getGeckolibData(NouveauVisuals.MODEL);if(model!=null)model=model.replaceFirst("_stack[234]$","");if(model!=null&&!model.equals("starbuncle"))return Jugcraft.id("textures/nouveau/entity/"+switch(model){case "goobuncle"->"starbuncle_goo";case "bootybuncle"->"starbuncle_mask";default->model;}+".png");
                Integer color=state.getGeckolibData(STAR_COLOR);return Jugcraft.id("textures/entity/starbuncle_"+Starbuncle.COLORS[color==null?4:Math.clamp(color,0,15)]+".png");
            }
        });
        EntityRendererRegistry.register(JugNouveau.BOOK_WYRM,context->new GeoEntityRenderer<BookWyrm,EntityRenderState>(context,JugNouveau.BOOK_WYRM){
            @Override public void addRenderData(BookWyrm wyrm,@Nullable Void unused,EntityRenderState state,float partial){state.addGeckolibData(COLOR,wyrm.color());}
            @Override public Identifier getTextureLocation(EntityRenderState state){
                Integer color=state.getGeckolibData(COLOR);return Jugcraft.id("textures/entity/book_wyrm_"+JugNouveau.COLORS[color==null?3:Math.clamp(color,0,5)]+".png");
            }
        });
        BlockEntityRenderers.register(JugNouveau.LECTERN_ENTITY,context->new GeoBlockRenderer<StorageLectern,BlockEntityRenderState>(context,JugNouveau.LECTERN_ENTITY));
        MenuScreens.register(JugNouveau.MENU,StorageLecternScreen::new);
        var provider=new GeoRenderProvider(){
            private @Nullable GeoItemRenderer<LecternItem> renderer;
            @Override public GeoItemRenderer<?> getGeoItemRenderer(){
                if(renderer==null)renderer=new GeoItemRenderer<>(new DefaultedItemGeoModel<LecternItem>(Jugcraft.id("bookwyrm_lectern")){
                    @Override public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState state){return Jugcraft.id("textures/block/bookwyrm_lectern.png");}
                });
                return renderer;
            }
        };
        LecternItem.renderer=()->provider;
    }
}

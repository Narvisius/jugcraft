package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauControls;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class NouveauControlsClient {
    private NouveauControlsClient(){}
    public static void register(){
        KeyMapping[] keys={
            new KeyMapping("key.jugcraft.nouveau_familiar",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY),
            new KeyMapping("key.jugcraft.nouveau_crown",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY),
            new KeyMapping("key.jugcraft.nouveau_spell_style",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY),
            new KeyMapping("key.jugcraft.nouveau_familiar_collection",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY)};
        for(KeyMapping key:keys)KeyMappingHelper.registerKeyMapping(key);
        ClientTickEvents.END_CLIENT_TICK.register(client->{for(int i=0;i<keys.length;i++)while(keys[i].consumeClick())if(client.player!=null&&client.gui.screen()==null)ClientPlayNetworking.send(new NouveauControls.Action(i));});
        MenuScreens.register(NouveauControls.MENU,StyleScreen::new);
    }
    private static final class StyleScreen extends AbstractContainerScreen<NouveauControls.StyleMenu> {
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
        StyleScreen(NouveauControls.StyleMenu menu,Inventory inventory,Component title){super(menu,inventory,title,310,220);}
        @Override protected void init(){super.init();for(int i=0;i<NouveauControls.PALETTES.length;i++){int id=i;addRenderableWidget(Button.builder(Component.literal(NouveauControls.PALETTES[i]),b->choose(id)).bounds(leftPos+14+i%4*71,topPos+44+i/4*21,68,18).build());}
            for(int i=0;i<NouveauControls.PARTICLES.length;i++){int id=32+i;addRenderableWidget(Button.builder(Component.literal(NouveauControls.PARTICLES[i].replace('_',' ')),b->choose(id)).bounds(leftPos+14+i%5*57,topPos+113+i/5*21,54,18).build());}
            for(int i=0;i<NouveauControls.SOUNDS.length;i++){int id=64+i;addRenderableWidget(Button.builder(Component.literal(new String[]{"Silent","Ember","Earth","Storm"}[i]),b->choose(id)).bounds(leftPos+14+i*71,topPos+181,68,18).build());}}
        private void choose(int button){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);}
        @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){int x=leftPos,y=topPos;g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);
            g.text(font,"Palette  ·  "+NouveauControls.PALETTES[Math.clamp(menu.selection(0),0,7)],x+14,y+29,0xffdbcda7,false);g.text(font,"Particle trail",x+14,y+98,0xffdbcda7,false);g.text(font,"Casting sound",x+14,y+166,0xffdbcda7,false);}
        @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){g.text(font,title,14,12,0xfff0e4c4,false);}
    }
}

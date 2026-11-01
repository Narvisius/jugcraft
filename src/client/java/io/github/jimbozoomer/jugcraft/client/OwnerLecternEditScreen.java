package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerLecternEditing;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Native book navigation, selection, narration and signing, bound to the actual lectern session. */
public final class OwnerLecternEditScreen extends BookEditScreen {
    private final UUID token;private boolean sent;
    private final List<Integer> styles=new ArrayList<>();
    private int page(){return ((io.github.jimbozoomer.jugcraft.mixin.client.OwnerLecternPageAccess)(Object)this).jugcraft$currentPage();}
    private int currentStyle(){return page()<styles.size()?styles.get(page()):0;}
    private void choose(boolean ink){while(styles.size()<=page()&&styles.size()<100)styles.add(0);if(page()>=styles.size())return;int v=currentStyle();styles.set(page(),ink?v/16*16+(v%16+1)%16:((v/16+1)%6)*16+v%16);}
    @Override protected void init(){super.init();
        int x=Math.min(width-96,width/2+100),y=Math.max(5,height/2-70);
        var quill=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Quill style"),b->choose(false)).bounds(x,y,92,20).build());
        quill.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal("Style this page when signed: normal, italic, bold, underline, strike or obfuscated.")));
        var ink=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Ink color"),b->choose(true)).bounds(x,y+23,92,20).build());
        ink.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal("Cycle this page's sixteen ink colors. Text remains editable until signing.")));
    }
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);int x=Math.min(width-96,width/2+100),y=Math.max(5,height/2-70);
        String[] quills={"default","italic","bold","underline","strikethrough","obfuscated"};int value=currentStyle();String color=net.minecraft.ChatFormatting.getByCode("0123456789abcdef".charAt(value%16)).name().toLowerCase(java.util.Locale.ROOT);
        g.text(font,net.minecraft.network.chat.Component.literal("Aa sample").setStyle(OwnerLecternEditing.style(value)),x+4,y+48,0xFFFFFFFF,false);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/newest/amends/gui/quill/"+quills[value/16]+".png"),x+4,y+65,0,0,24,72,24,72,48,144);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/newest/amends/gui/ink_well/"+color+".png"),x+34,y+96,0,0,32,32,32,32,32,32);
    }
    private OwnerLecternEditScreen(OwnerLecternEditing.Open opening){super(Minecraft.getInstance().player,new ItemStack(Items.WRITABLE_BOOK),InteractionHand.MAIN_HAND,opening.contents());token=opening.token();styles.addAll(opening.styles());}
    static void register(){ClientPlayNetworking.registerGlobalReceiver(OwnerLecternEditing.Open.TYPE,(p,c)->c.client().execute(()->c.client().gui.setScreen(new OwnerLecternEditScreen(p))));}
    public void saveLectern(List<String> pages,Optional<String> title){
        if(sent)return;sent=true;var copy=new ArrayList<>(pages);while(copy.size()>1&&copy.getLast().isEmpty())copy.removeLast();ClientPlayNetworking.send(new OwnerLecternEditing.Save(token,List.copyOf(copy),title,List.copyOf(styles)));
    }
}

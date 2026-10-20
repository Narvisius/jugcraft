package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauStorageTerminal;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A searchable cabinet beside a real crafting workspace. Catalog tiles are requests, never physical stacks. */
public final class NouveauStorageTerminalScreen extends AbstractContainerScreen<NouveauStorageTerminal.Menu> {
    private EditBox search;private TransmutationScreen.ActionButton previous,next,tab;
    private String pending="";private int quiet;
    public NouveauStorageTerminalScreen(NouveauStorageTerminal.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,344,240);}
    @Override protected void init(){super.init();
        search=addRenderableWidget(new EditBox(font,leftPos+18,topPos+28,162,18,Component.literal("Search connected storage")));search.setMaxLength(48);search.setHint(Component.literal("Find an item…"));search.setValue(pending);search.setResponder(text->{pending=text;quiet=4;});
        tab=button(3,210,136,116,"All inventories");tab.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Cycle groups of named containers. Rename a chest or repository to create a group.")));
        previous=button(0,18,126,76,"Previous");next=button(1,104,126,76,"Next");
        button(2,210,212,116,"Store inventory").setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Moves your inventory into permitted linked storage. Unaccepted items stay with you.")));
    }
    private TransmutationScreen.ActionButton button(int id,int x,int y,int w,String text){return addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+x,topPos+y,w,Component.literal(text),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}));}
    @Override protected void containerTick(){super.containerTick();if(quiet>0&&--quiet==0)ClientPlayNetworking.send(new NouveauStorageTerminal.Search(menu.containerId,pending));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){tab.setMessage(Component.literal(font.plainSubstrByWidth(menu.visibleGroup,106)));previous.active=menu.page()>0;next.active=menu.page()+1<menu.pages();extractBackground(g,mx,my,partial);super.extractRenderState(g,mx,my,partial);
        for(int i=0;i<NouveauStorageTerminal.Menu.GRID;i++){int count=menu.count(i);if(count<1)continue;var slot=menu.slots.get(i);String text=count>=10000?(count/1000)+"k":Integer.toString(count);g.text(font,text,leftPos+slot.x+17-font.width(text),topPos+slot.y+10,0xFFF2E8D5,true);}
        extractTooltip(g,mx,my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){int x=leftPos,y=topPos;g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);
        OwnerAlchemyArt.panel(g,x+12,y+44,174,80,OwnerAlchemyArt.VIOLET);OwnerAlchemyArt.panel(g,x+205,y+55,124,107,OwnerAlchemyArt.GOLD);OwnerAlchemyArt.panel(g,x+12,y+150,174,86,OwnerAlchemyArt.GOLD);
        for(var slot:menu.slots)OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);
        g.text(font,"→",x+278,y+94,OwnerAlchemyArt.GOLD,false);
        var lines=font.split(Component.literal("Click: stack\nRight-click: one\nShift: to inventory\nHeld item: deposit"),112);for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),x+214,y+162+i*9,OwnerAlchemyArt.SILVER,false);
        OwnerAlchemyArt.sigil(g,x+319,y+23,9,OwnerAlchemyArt.VIOLET,false,partial);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){g.text(font,"Book Wyrm Workshop",18,11,0xFFE9DFF5,false);g.text(font,"Crafting",218,42,OwnerAlchemyArt.GOLD,false);g.text(font,"Storage "+(menu.page()+1)+" / "+menu.pages(),211,31,OwnerAlchemyArt.SILVER,false);g.text(font,"Your inventory",18,146,OwnerAlchemyArt.SILVER,false);g.text(font,menu.connected()+" linked inventories",211,201,OwnerAlchemyArt.SILVER,false);}
}

package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.PersonalWorkshops;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PersonalWorkshopScreen extends AbstractContainerScreen<PersonalWorkshops.Menu> {
    private Button buy,publish,charge;
    public PersonalWorkshopScreen(PersonalWorkshops.Menu menu,Inventory inv,Component title){super(menu,inv,title,176,menu.kind.equals("personal_safe")?222:236);}
    private void action(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();titleLabelY=inventoryLabelY=10000;if(menu.kind.equals("personal_safe"))return;
        buy=addRenderableWidget(Button.builder(Component.literal("Buy"),b->action(0)).bounds(leftPos+82,topPos+45,84,19).tooltip(Tooltip.create(Component.literal("One exchange. Payment, stock and space are checked before anything moves. Charging fills your selected hotbar item."))).build());
        publish=addRenderableWidget(Button.builder(Component.empty(),b->action(1)).bounds(leftPos+8,topPos+213,78,18).tooltip(Tooltip.create(Component.literal("Publish to let visitors buy, including inside your claim. Stock remains private; hoppers cannot extract it."))).build());
        charge=addRenderableWidget(Button.builder(Component.empty(),b->action(2)).bounds(leftPos+90,topPos+213,78,18).tooltip(Tooltip.create(Component.literal("JE delivered for one full payment. Requires that much stored energy and free space in the customer's held item."))).build());
    }
    @Override protected void containerTick(){super.containerTick();if(buy==null)return;buy.active=menu.data.get(1)==1;buy.setMessage(Component.literal(menu.kind.equals("energy_vendor")?"Buy charge":"Trade"));publish.visible=menu.data.get(0)==1;publish.setMessage(Component.literal(menu.data.get(1)==1?"Published":"Private"));charge.visible=menu.data.get(0)==1&&menu.kind.equals("energy_vendor");charge.setMessage(Component.literal(PersonalWorkshops.CHARGES[Math.clamp(menu.data.get(2),0,2)]+" JE"));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        boolean safe=menu.kind.equals("personal_safe");String texture=safe?"personal_safe":menu.kind+(menu.data.get(0)==1?"_owner":"_customer");
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/"+texture+".png"),leftPos,topPos,0,0,176,safe?222:72,256,256);
        if(!safe){g.fill(leftPos+1,topPos+72,leftPos+175,topPos+235,0xffc6c6c6);
            g.fill(leftPos+73,topPos+8,leftPos+169,topPos+42,0xffc6c6c6);g.text(font,"Pay",leftPos+13,topPos+23,0xff303638,false);g.text(font,"Get",leftPos+13,topPos+57,0xff303638,false);
            if(menu.kind.equals("energy_vendor")){g.text(font,PersonalWorkshops.CHARGES[Math.clamp(menu.data.get(2),0,2)]+" JE",leftPos+79,topPos+12,0xff303638,false);g.text(font,(menu.data.get(3)*100)+" stored",leftPos+79,topPos+25,0xff303638,false);}
            else{g.text(font,"Stock / payments",leftPos+79,topPos+12,0xff303638,false);g.text(font,"Trades: "+menu.data.get(5),leftPos+79,topPos+25,0xff303638,false);}
            String[] status={"Ready","Private / paused","Not enough payment","Payments full","Out of stock","Inventory full","Needs power / held item"};
            g.text(font,status[Math.clamp(menu.data.get(4),0,6)],leftPos+8,topPos+114,0xff303638,false);
            // Native stock/payment rows and player inventory extend the original trade panel.
            for(var slot:menu.slots)if(slot.container!=menu.templates){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff373737);g.fill(x,y,x+16,y+16,0xff8b8b8b);}
            if(menu.data.get(0)==1&&mx>=leftPos+49&&mx<leftPos+67&&my>=topPos+18&&my<topPos+70)g.text(font,"Cursor stack sets sample",leftPos+8,topPos+203,0xff303638,false);
        }else g.text(font,"Personal Safe",leftPos+8,topPos+6,0xff303638,false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}

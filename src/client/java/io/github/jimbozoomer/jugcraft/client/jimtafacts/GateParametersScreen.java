package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGateParameters;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/** Button-only parameter editor; server chooses the gate, current row and allowed modifier capacity. */
public final class GateParametersScreen extends AbstractContainerScreen<IndustrialGateParameters.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    private final Button[] fields=new Button[IndustrialGateParameters.FIELDS];
    public GateParametersScreen(IndustrialGateParameters.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,320,246);}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){
        super.init();titleLabelY=inventoryLabelY=10000;
        addRenderableWidget(Button.builder(Component.literal("< Row"),b->send(0)).bounds(leftPos+12,topPos+25,65,18).build());
        addRenderableWidget(Button.builder(Component.literal("Row >"),b->send(1)).bounds(leftPos+243,topPos+25,65,18).build());
        for(int i=0;i<fields.length;i++) {
            int field=i,group=i<12?i/4:(i-12)/3,column=i<12?i%4:(i-12)%3;
            int[] xs=i<12?new int[]{14,77,166,223}:new int[]{14,96,207};
            int[] widths=i<12?new int[]{57,83,51,83}:new int[]{76,105,99};
            fields[i]=addRenderableWidget(Button.builder(Component.empty(),b->send(10+field)).bounds(leftPos+xs[column],topPos+(i<12?64:154)+group*22,widths[column],18).build());
            fields[i].setTooltip(Tooltip.create(Component.literal(i<12?"Extra sensor condition. Every enabled condition in this row must hold. Threshold cycles in 5% steps.":"Additional output channel driven by this row's combined condition.")));
        }
        addRenderableWidget(Button.builder(Component.literal("Edit primary rules"),b->send(2)).bounds(leftPos+84,topPos+222,152,18).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        for(int i=0;i<fields.length;i++) {
            int value=menu.value(i);boolean condition=i<12;
            int group=condition?i/4:(i-12)/3,column=condition?i%4:(i-12)%3;
            fields[i].active=group<(condition?IndustrialGateParameters.conditions(menu.modifier()):IndustrialGateParameters.actions(menu.modifier()));
            String label=column==0?(value==1?"Enabled":"Disabled"):column==1?DyeColor.byId(value).getName():condition?(column==2?(value==1?">":"<"):value+"%"):value==1?"On":"Off";
            fields[i].setMessage(Component.literal(label));
        }
        extractBackground(g,x,y,delta);super.extractRenderState(g,x,y,delta);extractTooltip(g,x,y);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta){
        ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);
        ScreenKit.pane(g,theme,leftPos+8,topPos+47,304,85);
        ScreenKit.pane(g,theme,leftPos+8,topPos+137,304,83);
        String kind=menu.modifier()>0?IndustrialGateParameters.KINDS.get(Math.min(2,menu.modifier()-1)):"Gate";
        g.text(font,kind.toUpperCase()+"  /  GATE PARAMETERS",leftPos+12,topPos+10,theme.label(),false);
        g.text(font,"Row "+(menu.row()+1)+" of "+menu.rows(),leftPos+118,topPos+30,theme.text(),false);
        g.text(font,"Additional conditions  •  all must hold",leftPos+14,topPos+51,theme.text(),false);
        g.text(font,"Additional output channels",leftPos+14,topPos+141,theme.text(),false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}

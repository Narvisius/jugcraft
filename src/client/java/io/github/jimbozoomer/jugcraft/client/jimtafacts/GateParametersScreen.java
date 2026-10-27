package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGateParameters;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * Button-only parameter editor; server chooses the gate, current row and allowed modifier capacity. In the electric look
 * (docs/features/new-screen-styles.md), its keys lit while a condition or channel is enabled; laid out at 320 x 246 and
 * scaled down to fit a smaller window.
 */
public final class GateParametersScreen extends FittedContainerScreen<IndustrialGateParameters.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    private final ScreenKit.TextButton[] fields=new ScreenKit.TextButton[IndustrialGateParameters.FIELDS];
    public GateParametersScreen(IndustrialGateParameters.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,320,246);}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private ScreenKit.TextButton key(int x,int y,int w,Component label,int id){return addRenderableWidget(new ScreenKit.TextButton(leftPos+x,topPos+y,w,18,theme,label,b->send(id)));}
    @Override protected void init(){
        super.init();titleLabelY=inventoryLabelY=10000;
        key(12,25,65,Component.literal("< Row"),0);
        key(243,25,65,Component.literal("Row >"),1);
        for(int i=0;i<fields.length;i++) {
            int group=i<12?i/4:(i-12)/3,column=i<12?i%4:(i-12)%3;
            int[] xs=i<12?new int[]{14,77,166,223}:new int[]{14,96,207};
            int[] widths=i<12?new int[]{57,83,51,83}:new int[]{76,105,99};
            fields[i]=key(xs[column],(i<12?64:154)+group*22,widths[column],Component.empty(),10+i);
            fields[i].setTooltip(Tooltip.create(Component.literal(i<12?"Extra sensor condition. Every enabled condition in this row must hold. Threshold cycles in 5% steps.":"Additional output channel driven by this row's combined condition.")));
        }
        key(84,222,152,Component.literal("Edit primary rules"),2);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        for(int i=0;i<fields.length;i++) {
            int value=menu.value(i);boolean condition=i<12;
            int group=condition?i/4:(i-12)/3,column=condition?i%4:(i-12)%3;
            fields[i].active=group<(condition?IndustrialGateParameters.conditions(menu.modifier()):IndustrialGateParameters.actions(menu.modifier()));
            // The enable switch and an output that is on are lit.
            fields[i].setSelected(fields[i].active&&value==1&&(column==0||!condition&&column==2));
            String label=column==0?(value==1?"Enabled":"Disabled"):column==1?DyeColor.byId(value).getName():condition?(column==2?(value==1?">":"<"):value+"%"):value==1?"On":"Off";
            fields[i].setMessage(Component.literal(label));
        }
        super.extractRenderState(g,x,y,delta);
    }
    @Override protected void background(GuiGraphicsExtractor g,int x,int y,float delta){
        ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);
        ScreenKit.pane(g,theme,leftPos+8,topPos+47,304,85);
        ScreenKit.pane(g,theme,leftPos+8,topPos+137,304,83);
        String kind=menu.modifier()>0?IndustrialGateParameters.KINDS.get(Math.min(2,menu.modifier()-1)):"Gate";
        ScreenKit.title(g,font,theme,leftPos,topPos,Component.literal(kind.toUpperCase()+"  /  GATE PARAMETERS"),menu.modifier()>0?theme.ok():theme.dim(),imageWidth-8);
        String row="Row "+(menu.row()+1)+" of "+menu.rows();
        g.text(font,row,leftPos+160-font.width(row)/2,topPos+30,theme.label(),false);
        g.text(font,"Additional conditions  •  all must hold",leftPos+14,topPos+51,theme.progress(),false);
        g.text(font,"Additional output channels",leftPos+14,topPos+141,theme.progress(),false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}

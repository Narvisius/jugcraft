package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.MatterWorkshops;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native machine style (the electric look), explicit slot roles and operation status; no typed pattern names. */
public final class MatterWorkshopScreen extends AbstractContainerScreen<MatterWorkshops.Menu> implements ScreenKit.Fitted {
    private ScreenKit.TextButton mode;
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    public MatterWorkshopScreen(MatterWorkshops.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,256,210);}
    @Override protected void init(){super.init();titleLabelX=12;titleLabelY=6;inventoryLabelX=8;inventoryLabelY=114;mode=addRenderableWidget(new ScreenKit.TextButton(leftPos+86,topPos+104,152,17,theme,Component.literal("Fill containers"),button->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){mode.visible=menu.data.get(0)==5;mode.setMessage(Component.literal(menu.data.get(5)==0?"Fill containers →":"← Empty containers"));extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);ScreenKit.pane(g,theme,leftPos+11,topPos+22,234,85);ScreenKit.tray(g,theme,leftPos+5,topPos+122,166,81);
        for(int i=0;i<9;i++){int x=leftPos+18+i%3*18,y=topPos+37+i/3*18;g.fill(x,y,x+18,y+18,theme.slotDark());g.fill(x+1,y+1,x+17,y+17,theme.slotFace());}
        for(int row=0;row<4;row++)for(int col=0;col<9;col++){int x=leftPos+7+col*18,y=topPos+(row==3?183:125+row*18);g.fill(x,y,x+18,y+18,theme.slotDark());g.fill(x+1,y+1,x+17,y+17,theme.slotFace());}
    }
    @Override public ScreenRectangle panelArea(){return new ScreenRectangle(leftPos,topPos,imageWidth,imageHeight);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,12,6,theme.label());g.text(font,"Inventory",8,114,theme.label());
        int kind=menu.data.get(0);String help=switch(kind){case 0->"Top-left: optional scrap amplifier. Connect power and drain UU-Matter through pipes.";case 1->"Top-left: sample. Top-middle: blank crystal memory. Encoded memory appears in the center.";case 2->"Store encoded memories here. An adjacent replicator uses the first pattern when its own slot is empty.";case 4->"Top-left: food. Top-middle: empty can. Center: sealed food; right: returned bowl or bottle.";case 5->"Top-left: fluid container. Center: finished container. Connect fluid pipes and choose fill or empty below.";default->"Top-left: encoded memory. Supply UU-Matter and JE. Center: replicated item.";};
        int y=26;for(var line:font.split(Component.literal(help),151)){g.text(font,line,88,y,theme.text());y+=10;}
        String[] state={"Idle","Redstone pause","Output full","Needs JE","Working","Needs valid pattern","Needs UU-Matter"};
        g.text(font,kind>=4&&menu.data.get(4)==5?"Needs ingredients":kind==5&&menu.data.get(4)==6?"No fluid transfer":state[Math.clamp(menu.data.get(4),0,state.length-1)],88,88,menu.data.get(4)==4?theme.ok():theme.warn());
        g.text(font,(menu.data.get(2)*100)+" JE",181,126,theme.text());g.text(font,menu.data.get(3)+(kind==5?" mB":" mB UU"),181,138,theme.text());
        if(kind==0)g.text(font,(menu.data.get(1)/100)+"%",181,150,theme.text());
        int py=164;for(var line:font.split(Component.literal(kind>=4?"Redstone pauses work. 400 JE per operation.":"Redstone pauses work. Convenience outputs only."),65)){g.text(font,line,181,py,theme.dim());py+=9;}
    }
}

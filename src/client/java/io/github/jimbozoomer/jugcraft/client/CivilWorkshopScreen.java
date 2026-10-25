package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Status and slot roles stay visible; start/reset controls never require typed commands. */
public final class CivilWorkshopScreen extends AbstractContainerScreen<CivilWorkshops.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");private Button start,reset;
    public CivilWorkshopScreen(CivilWorkshops.Menu menu,Inventory inv,Component title){super(menu,inv,title,256,228);}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.literal("Start"),button->send(0)).bounds(leftPos+8,topPos+108,76,18).build());reset=addRenderableWidget(Button.builder(Component.literal("Reset scan"),button->send(1)).bounds(leftPos+91,topPos+108,80,18).build());}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){boolean on=menu.data.get(5)!=0;start.setMessage(Component.literal(on?"Pause":"Start"));reset.active=!on;extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);ScreenKit.pane(g,theme,leftPos+5,topPos+22,167,71);ScreenKit.tray(g,theme,leftPos+5,topPos+140,166,81);for(int i=0;i<27;i++)slot(g,7+i%9*18,35+i/9*18,i==0?theme.ok():i>=14?theme.warn():theme.slotDark());for(int row=0;row<4;row++)for(int col=0;col<9;col++)slot(g,7+col*18,row==3?201:143+row*18,theme.slotDark());}
    private void slot(GuiGraphicsExtractor g,int x,int y,int border){g.fill(leftPos+x,topPos+y,leftPos+x+18,topPos+y+18,border);g.fill(leftPos+x+1,topPos+y+1,leftPos+x+17,topPos+y+17,theme.slotFace());}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){g.text(font,title,8,7,theme.label());g.text(font,"Tool / plan     Materials → Outputs",8,24,theme.dim());g.text(font,"Inventory",8,132,theme.label());String[] status={"Paused","Redstone pause","Protected / owner away","Needs JE","Working","Needs plan / materials / area","Blocked / wrong tool","Needs fluid","Output full","Finished"};g.text(font,status[Math.clamp(menu.data.get(1),0,9)],8,96,menu.data.get(1)==4?theme.ok():theme.warn());String help=menu.data.get(0)==6?"Plan in slot 1; tool in slot 2 for levelling. Supply seeds, bone meal and piped water. 4,096 JE/change.":menu.data.get(0)==0?"Plan in first slot. Materials in white slots. Uses existing blueprints; only places safe blocks.":menu.data.get(0)==5?"Pipes supply water or lava. Each placed source consumes 1,000 mB.":"Tool in first slot. Materials in white slots. Amber slots collect mined blocks.";int y=26;for(var line:font.split(Component.literal(help),69)){g.text(font,line,180,y,theme.text());y+=10;}g.text(font,menu.data.get(2)*10+" JE",180,144,theme.text());g.text(font,menu.data.get(3)+" / "+menu.data.get(4),180,157,theme.dim());y=177;for(var line:font.split(Component.literal("Use Volume Marker on two corners, then machine."),69)){g.text(font,line,180,y,theme.dim());y+=10;}}
}

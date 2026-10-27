package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialHeat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Shared industrial panels; live tank/heat gauges and explicit piping directions without typed configuration. In the
 * electric look, with its key lit in superheated mode (docs/features/new-screen-styles.md).
 */
public final class IndustrialHeatScreen extends AbstractContainerScreen<IndustrialHeat.Menu> implements ScreenKit.Fitted {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    private ScreenKit.TextButton steamMode;
    public IndustrialHeatScreen(IndustrialHeat.Menu menu,Inventory inv,Component title){super(menu,inv,title,320,226);}
    @Override protected void init(){super.init();titleLabelX=12;titleLabelY=7;inventoryLabelX=8;inventoryLabelY=131;
        steamMode=addRenderableWidget(new ScreenKit.TextButton(leftPos+183,topPos+201,126,18,theme,Component.literal("Change steam mode"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}));}
    private String kind(){return IndustrialHeat.KINDS.get(Math.clamp(menu.data.get(0),0,IndustrialHeat.KINDS.size()-1));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){steamMode.visible=menu.data.get(0)==8;steamMode.active=menu.data.get(5)==0;steamMode.setMessage(Component.literal(menu.data.get(8)==1?"Mode: Superheated":"Mode: Steam"));steamMode.setSelected(menu.data.get(8)==1);extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);
        ScreenKit.pane(g,theme,leftPos+9,topPos+24,302,98);
        ScreenKit.tray(g,theme,leftPos+5,topPos+140,166,81);
        if((menu.data.get(0)==0||menu.data.get(0)>=10)){g.fill(leftPos+19,topPos+58,leftPos+37,topPos+76,theme.slotDark());g.fill(leftPos+20,topPos+59,leftPos+36,topPos+75,theme.slotFace());}
        gauge(g,67,47,menu.data.get(1),4096,0xFFD79742);
        gauge(g,67,69,menu.data.get(4),16000,0xFF588AA3);
        gauge(g,67,91,menu.data.get(5),16000,0xFF99B7C0);
        for(int row=0;row<4;row++)for(int col=0;col<9;col++){int x=leftPos+7+col*18,y=topPos+(row==3?200:142+row*18);g.fill(x,y,x+18,y+18,theme.slotDark());g.fill(x+1,y+1,x+17,y+17,theme.slotFace());}
    }
    @Override public ScreenRectangle panelArea(){return new ScreenRectangle(leftPos,topPos,imageWidth,imageHeight);}
    private void gauge(GuiGraphicsExtractor g,int x,int y,int value,int max,int color){int px=leftPos+x,py=topPos+y;g.fill(px,py,px+192,py+6,theme.slotDark());g.fill(px+1,py+1,px+1+(int)(190L*Math.clamp(value,0,max)/max),py+5,color);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,12,7,theme.label());g.text(font,"Inventory",8,131,theme.label());
        g.text(font,menu.data.get(7)==1?"Redstone paused":menu.data.get(6)==1?"Working":"Idle / awaiting connection",15,28,menu.data.get(6)==1?theme.ok():theme.warn());
        g.text(font,"Heat",15,45,theme.text());g.text(font,menu.data.get(1)+" / 4096",67,37,theme.text());
        g.text(font,"Input",15,82,theme.text());g.text(font,menu.data.get(4)+" / 16000 mB",67,59,theme.text());
        g.text(font,"Output",15,103,theme.text());g.text(font,menu.data.get(5)+" / 16000 mB",67,81,theme.text());
        g.text(font,(menu.data.get(2)*10)+" JE",184,130,theme.text());
        String help=switch(kind()){
            case "wind_kinetic_generator"->"Rotor in front, shaft behind. Clear the whole rotor area with sky overhead. Higher altitude increases output.";
            case "water_kinetic_generator"->"Iron, steel or carbon rotor in front; shaft behind. Needs a clear submerged rotor area and flowing water.";
            case "radioisotope_generator"->"RTG pellet: 2 JE/tick, 20,000 working seconds. Pauses at full charge; connect cables to side or back.";
            case "radioisotope_heat_generator"->"RTG pellet: 4 heat/tick, 20,000 working seconds. Pauses when full. Front faces heat receiver.";
            case "solid_heat_generator"->"Insert fuel. Point the hot front into a Stirling or boiler inlet. Stored fuel pauses when full.";
            case "electric_heat_generator"->"5 JE → 4 heat. Hot front faces the receiving machine's front.";
            case "fluid_heat_generator"->"Pipe generator fuel in. Hot front faces a Stirling or boiler. Stops when heat is full.";
            case "liquid_heat_exchanger"->"Hot coolant in; cooled coolant out below. Front supplies heat to Stirling or boiler.";
            case "steam_generator"->menu.data.get(8)==1?"12 heat + 1 water → 10 superheated steam. Feed two turbine stages. Stops at 95% output pressure.":"4 heat + 1 water → 10 steam. Front receives heat; bottom outputs. Drain before switching mode.";
            case "steam_kinetic_generator"->menu.data.get(9)==1?"10 superheated steam → 4 KE + 10 steam. Send steam below to a second turbine.":"10 steam → 2 KE + 1 water. Front drives shafts; water leaves below. Retains paid KE if disconnected.";
            case "stirling_generator"->"Front faces heat source. JE exits other faces. 2 heat → 1 JE.";
            case "stirling_kinetic_generator"->"Front receives heat; rear drives shafts. 2 heat → 1 KE.";
            default->"Front drives shafts. 80% electrical conversion; no power spent on a disconnected shaft.";
        };
        int y=146;for(var line:font.split(Component.literal(help),127)){g.text(font,line,183,y,theme.dim());y+=9;}
        g.text(font,"Fluid ports: bottom OUT · other faces IN",15,111,theme.dim());
    }
}

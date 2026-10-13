package local.peepo.client;

import local.peepo.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Inventory stays visible while the right panel switches between assignments and routine preferences. */
public final class CompanionScreen extends AbstractContainerScreen<CompanionMenu> {
    private final Button[] buttons=new Button[9],remove=new Button[6],moveUp=new Button[5],moveDown=new Button[5],settings=new Button[9];
    private final Button[] transport=new Button[8],filters=new Button[4];
    private Button filterBack;
    private static final String[] MODES={"Follow","Stay","Home","Work"};
    private int panel; // Jobs and routine. Transport controls live on each job row.
    private Button tab,supplyTab;
    private int supplyView;
    private final Button[] supplyNav=new Button[3],storageRemove=new Button[8],tools=new Button[4],links=new Button[40],routes=new Button[16],limits=new Button[2];
    private Button handLock;
    public CompanionScreen(CompanionMenu menu,Inventory inventory,Component title){super(menu,inventory,title,CompanionMenu.WIDTH,312);}
    private Button button(String text,int x,int y,int width,int height,int id){
        return addRenderableWidget(Button.builder(Component.literal(text),b->{if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}).bounds(leftPos+x,topPos+y,width,height).build());
    }
    private void tip(Button b,String text){b.setTooltip(Tooltip.create(Component.literal(text)));}
    @Override protected void init(){
        super.init();inventoryLabelX=80;inventoryLabelY=218;
        tab=addRenderableWidget(Button.builder(Component.literal("Routine >"),b->{panel=panel==1?0:1;updateButtons();}).bounds(leftPos+imageWidth-84,topPos+6,76,16).build());
        supplyTab=addRenderableWidget(Button.builder(Component.literal("Supplies"),b->{panel=panel==2?0:2;updateButtons();}).bounds(leftPos+imageWidth-166,topPos+6,78,16).build());
        initSupplies();
        for(int i=0;i<4;i++){filters[i]=button("F",CompanionMenu.RECIPE_X,CompanionMenu.assignmentY(i+1),16,16,60+i);tip(filters[i],"Filter");}
        filterBack=button("< Back",373,78,70,18,64);
        for(int i=0;i<8;i++)transport[i]=button("",254+(i%2)*80,CompanionMenu.assignmentY(1+i/2),78,16,50+i);
        for(int i=0;i<MODES.length;i++)buttons[i]=button(MODES[i],100+i*53,34,50,18,i);
        buttons[6]=button("-",100,56,22,18,6);buttons[7]=button("+",125,56,22,18,7);
        buttons[8]=button("Party",210,56,102,18,8);
        tip(buttons[8],"Only the owner can allow party members to give commands.");
        for(int i=0;i<6;i++){
            remove[i]=button("x",imageWidth-19,CompanionMenu.assignmentY(i)+2,11,12,20+i);
            tip(remove[i],i==0?"Clear home":i==5?"Clear assigned lunch source":i==6?"Clear Supply":i==7?"Clear Output":"Clear work "+i);
            if(i>0 && i<5){
                moveUp[i]=button("\u2191",imageWidth-43,CompanionMenu.assignmentY(i)+2,11,12,30+(i-1)*2);
                moveDown[i]=button("\u2193",imageWidth-31,CompanionMenu.assignmentY(i)+2,11,12,31+(i-1)*2);
                tip(moveUp[i],"Move up: higher work priority");tip(moveDown[i],"Move down: lower work priority");
            }
        }
        settings[8]=button("Social",100,181,212,18,48);
        tip(settings[8],"Wave to your owner and have short conversations while standing idle. Work, food, rest and commands take priority.");
        settings[0]=button("Schedule",100,78,212,18,40);
        settings[1]=button("Alerts",100,99,103,18,41);
        settings[2]=button("Food",207,99,105,18,42);
        settings[3]=button("Carry meals",100,159,212,18,43);
        settings[4]=button("-",263,120,22,16,44);settings[5]=button("+",290,120,22,16,45);
        settings[6]=button("-",263,139,22,16,46);settings[7]=button("+",290,139,22,16,47);
        tip(settings[0],"Auto: work whenever energy allows. Day/Night shifts follow Overworld time in every dimension. Sleep remains night-only in the Overworld; other dimensions use seated rest.");
        tip(settings[1],"A short nearby sound when a new problem starts. At most once every 20 seconds.");
        tip(settings[2],"Best meal favors stronger energy regeneration. Small meal favors cheaper, weaker meals. Only cargo slots are used.");
        tip(settings[3],"Keep 0-4 meals in the eight cargo slots. Refill only at your assigned lunch source.");
        tip(settings[4],"Lower the energy percentage at which work pauses.");tip(settings[5],"Raise the energy percentage at which work pauses.");
        tip(settings[6],"Lower the energy needed to resume. Always at least 10 points above the break threshold.");tip(settings[7],"Raise the energy needed to resume.");
        updateButtons();
    }
    private PeepoEntity npc(){return minecraft==null || minecraft.player==null?null:menu.companion(minecraft.player);}
    private void updateButtons(){
        boolean filtering=menu.filterRow()>=0;
        if(filtering)panel=menu.filterRow()>=4?2:0;
        menu.showRecipes=panel==0 || filtering;
        tab.visible=supplyTab.visible=!filtering;
        updateSupplies(filtering);filterBack.visible=filtering;
        for(int i=0;i<4;i++)filters[i].visible=panel==0 && !filtering && menu.value(25+i)>0 && menu.value(25+i)!=4;
        for(var b:buttons)if(b!=null)b.visible=!filtering;
        tab.setMessage(Component.literal(panel==1?"< Jobs":"Routine"));supplyTab.setMessage(Component.literal(panel==2?"< Jobs":"Supplies"));
        for(var setting:settings)setting.visible=panel==1;
        var npc=npc();
        for(int i=0;i<8;i++){
            int value=menu.value(31+i),mode=Math.min(2,value&3),reason=value>>2;
            var b=transport[i];b.visible=panel==0 && !filtering;b.active=npc!=null && npc.assignments.view().get(1+i/2)!=null && reason!=3;
            b.setMessage(Component.literal((i%2==0?"Supply: ":"Output: ")+new String[]{"Auto","On","Off"}[mode]).withColor(reason==2?0xFFCC66:reason==1?0xAAAAAA:0xFFFFFF));
            String state=switch(reason){case 1->"Disabled by you.";case 2->"External item automation detected; Peepo yields this direction.";case 3->"This station has no companion item transport for this direction.";case 4->"No available, loaded station.";case 5->"No complete allowed recipe is available from the current inputs and Supply.";default->"Peepo may handle this direction when there is work to do.";};
            tip(b,state+" Click to cycle Auto / On / Off. Auto recognizes connected transport and recent machine transfers. On overrides detection; Off stops this direction. Speed assistance is unchanged.");
        }
        for(int i=0;i<6;i++){remove[i].visible=panel==0 && !filtering;remove[i].active=npc!=null && npc.assignments.view().get(i)!=null;}
        for(int i=1;i<5;i++){
            moveUp[i].visible=moveDown[i].visible=panel==0 && !filtering;
            moveUp[i].active=remove[i].active && i>1 && remove[i-1].active;
            moveDown[i].active=remove[i].active && i<4 && remove[i+1].active;
        }
        settings[8].setMessage(Component.literal(menu.value(39)==1?"Social: On":"Social: Off"));
        settings[0].setMessage(Component.literal("Schedule: "+CompanionPreferences.SCHEDULES[Math.clamp(menu.value(18),0,2)]));
        settings[1].setMessage(Component.literal(menu.value(23)==1?"Alerts: On":"Alerts: Off"));
        settings[2].setMessage(Component.literal(CompanionPreferences.FOODS[Math.clamp(menu.value(21),0,1)]));
        settings[3].setMessage(Component.literal("Carry meals: "+menu.value(22)+" (have "+menu.value(24)+")"));
        settings[4].active=menu.value(19)>0;settings[5].active=menu.value(19)<Math.min(70,menu.value(20)-10);
        settings[6].active=menu.value(20)>menu.value(19)+10;settings[7].active=menu.value(20)<95;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        for(int i=0;i<MODES.length;i++)buttons[i].setMessage(Component.literal(MODES[i]).withColor(menu.value(0)==i?0x80FF80:0xFFFFFF));
        buttons[6].active=menu.value(3)>4;buttons[7].active=menu.value(3)<16;buttons[8].active=menu.value(5)==1;
        buttons[8].setMessage(Component.literal(menu.value(4)==1?"Party: Allowed":"Owner only"));
        updateButtons();
        extractBackground(g,mouseX,mouseY,delta);super.extractRenderState(g,mouseX,mouseY,delta);extractTooltip(g,mouseX,mouseY);
        supplyTooltip(g,mouseX,mouseY);
        int row=assignmentRow(mouseY-topPos);var npc=npc();
        if(panel==0 && menu.filterRow()<0 && npc!=null && mouseX>=leftPos+100 && mouseX<leftPos+(row>0 && row<5?253:imageWidth-20) && mouseY>=topPos+78 && row>=0 && row<CompanionAssignments.COUNT){
            var target=npc.assignments.view().get(row);
            String text=target==null?"Use the Companion Planner to select this companion, then right-click a "+(row==0?"bed.":row==5?"lunch crate or lunch cover.":row==6?"container for Supply (blue); click again to switch roles.":row==7?"container for Output (yellow); click again to switch roles.":"workstation or farmland."):
                target.name()+" at "+target.at().pos().toShortString()+" in "+target.at().dimension().identifier()+" - "+CompanionStatus.from(menu.value(CompanionMenu.assignmentData(row))).label;
            if(row>0 && row<5 && target!=null)text+=". Priority "+row+" (top is highest).";
            if(target!=null && target.garden())text+=" Up to 8 connected soil blocks. A hoe in Hand halves work time. Supply brings seeds; Output takes surplus harvests.";
            if(target!=null && target.shearing())text+=" Adult woolly sheep within 8 blocks. Give shears in Hand or a Tools supply; Output receives wool.";
            if(target!=null && target.milking())text+=" Adult cows within 8 blocks. Supply brings empty buckets; Output receives milk. Each cow rests one minute between milkings.";
            if(row==6 || row==7)text+=" Porter moves items from Supply to Output. Work uses these containers for machine recipes.";
            g.setTooltipForNextFrame(font,Component.literal(text),mouseX,mouseY);
        }
        if(panel==0 && menu.filterRow()<0)for(int i=0;i<4;i++){
            var slot=menu.getSlot(CompanionMenu.RECIPE_START+i);
            if(slot.isActive() && mouseX>=leftPos+slot.x && mouseX<leftPos+slot.x+16 && mouseY>=topPos+slot.y && mouseY<topPos+slot.y+16){
                var lines=new java.util.ArrayList<Component>();
                if(menu.value(25+i)==4){
                    lines.add(slot.hasItem()?Component.literal("Plant: ").append(slot.getItem().getHoverName()):Component.literal("Automatic planting"));
                    lines.add(Component.literal("Click with a seed or raw crop to choose this plot's crop."));
                    lines.add(Component.literal("Your item stays on the cursor. Right-click to restore Auto."));
                    lines.add(Component.literal("Supply brings matching seeds. Existing crops finish growing first."));
                    lines.add(Component.literal("Tomatoes need trellises; remove trellises for other crops."));
                    g.setTooltipForNextFrame(font,lines,java.util.Optional.empty(),mouseX,mouseY);continue;
                }
                if(slot.hasItem()){
                    lines.add(slot.getItem().getHoverName());
                    var lore=slot.getItem().get(net.minecraft.core.component.DataComponents.LORE);
                    if(lore!=null)lines.addAll(lore.lines());
                }else lines.add(Component.literal(menu.value(25+i)==2?"Automatic: any available raw pie or cake.":"Choose a recipe to enable supplies."));
                lines.add(Component.literal(menu.value(25+i)==2?"Click with a whole raw or baked pie/cake to choose one.":"Click with a finished item to set a ghost recipe."));
                lines.add(Component.literal("Your item stays on the cursor. Right-click to clear."));
                if(menu.value(25+i)==2)lines.add(Component.literal("Clear to restore Auto. Supply prepared raw pies/cakes and fuel."));
                else lines.add(Component.literal("Click the same output again to cycle matching recipes."));
                g.setTooltipForNextFrame(font,lines,java.util.Optional.empty(),mouseX,mouseY);
            }
        }

    }
    private void initSupplies(){
        String[] names={"Containers","Job links","Porter routes"};
        for(int i=0;i<3;i++){final int v=i;supplyNav[i]=addRenderableWidget(Button.builder(Component.literal(names[i]),b->{supplyView=v;updateButtons();}).bounds(leftPos+100+i*122,topPos+78,118,16).build());}
        for(int i=0;i<8;i++)storageRemove[i]=button("x",i<4?268:454,112+(i%4)*20,14,16,100+i);
        for(int i=0;i<4;i++){tools[i]=button("Tools",231,112+i*20,35,16,110+i);tip(tools[i],"Allow fetching a knife or hoe from this Supply. The old hand item stays in cargo.");}
        handLock=button("Lock Hand",100,194,182,16,114);tip(handLock,"Prevent automatic tool changes. Keep your torch or preferred tool equipped.");
        for(int row=0;row<4;row++)for(int side=0;side<2;side++)for(int c=0;c<5;c++){
            int i=row*10+side*5+c;links[i]=button(c==0?"All":Integer.toString(c),190+side*141+(c==0?0:35+(c-1)*24),113+row*23,c==0?33:22,16,200+i);
            tip(links[i],c==0?"Use all assigned containers, including newly added ones.":"Choose this container; then toggle additional numbers. Green = included. All restores shared access.");
        }
        for(int row=0;row<4;row++){
            int y=110+row*24;
            routes[row*4]=button("",132,y,105,18,300+row*4);routes[row*4+1]=button("",246,y,105,18,301+row*4);
            routes[row*4+2]=button("",355,y,65,18,302+row*4);routes[row*4+3]=button("Filter",424,y,44,18,303+row*4);
            tip(routes[row*4],"Cycle Supply 1-4. Changing an endpoint turns this route off until you enable it.");
            tip(routes[row*4+1],"Cycle Output 1-4. Routes run after ready workstation jobs.");
            tip(routes[row*4+3],"Filter, source reserve and destination stock target");
        }
        // Dynamic route IDs are selected from the open filter, never from a client target position.
        for(int i=0;i<2;i++){final int side=i;limits[i]=addRenderableWidget(Button.builder(Component.literal(""),b->{if(minecraft!=null && minecraft.gameMode!=null && menu.filterRow()>=4)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,320+(menu.filterRow()-4)*2+side);}).bounds(leftPos+105,topPos+110+i*23,125,18).build());}
    }
    private void updateSupplies(boolean filtering){
        boolean visible=panel==2 && !filtering;var npc=npc();
        for(int i=0;i<3;i++){supplyNav[i].visible=visible;supplyNav[i].active=i!=supplyView;}
        for(int i=0;i<8;i++){storageRemove[i].visible=visible && supplyView==0;storageRemove[i].active=npc!=null && npc.assignments.view().get(CompanionAssignments.containerSlot(i>=4,i%4))!=null;}
        for(int i=0;i<4;i++){tools[i].visible=visible && supplyView==0;tools[i].active=storageRemove[i].active;tools[i].setMessage(Component.literal("Tools").withColor((menu.value(41)&(1<<i))!=0?0x80FF80:0xFFFFFF));}
        handLock.visible=visible && supplyView==0;handLock.setMessage(Component.literal(menu.value(42)==1?"Hand: Locked":"Hand: Auto tools"));
        for(int i=0;i<40;i++){
            var b=links[i];b.visible=visible && supplyView==1;b.active=npc!=null && npc.assignments.view().get(1+i/10)!=null;
            int mask=menu.value(43+i/10*2+(i%10)/5),choice=i%5;boolean selected=choice==0?mask==0:mask==0 || (mask&(1<<(choice-1)))!=0;
            b.setMessage(Component.literal(choice==0?"All":Integer.toString(choice)).withColor(selected?0x80FF80:0xFFFFFF));
        }
        for(int i=0;i<16;i++){
            var b=routes[i];b.visible=visible && supplyView==2;int row=i/4,part=i%4;
            if(part<2){int index=menu.value(51+row*4+part);b.setMessage(Component.literal((part==0?"Supply ":"Output ")+(index+1)));
                var target=npc==null?null:npc.assignments.view().get(CompanionAssignments.containerSlot(part==1,index));
                tip(b,(target==null?"Not assigned":target.name()+" at "+target.at().pos().toShortString())+". Click to cycle; changing the endpoint disables this route.");}
            if(part==2)b.setMessage(Component.literal(menu.value(53+row*4)==1?"On":"Off").withColor(menu.value(53+row*4)==1?0x80FF80:0xFFFFFF));
        }
        for(int i=0;i<2;i++){limits[i].visible=menu.filterRow()>=4;if(limits[i].visible){int row=menu.filterRow()-4;limits[i].setMessage(Component.literal((i==0?"Leave: ":"Keep: ")+menu.value(i==0?54+row*4:67+row)));}}
    }
    private void supplyLabels(GuiGraphicsExtractor g){
        var npc=npc();
        if(supplyView==0){
            g.text(font,"Supply (blue)",101,100,CompanionAssignments.SUPPLY_COLOR,false);g.text(font,"Output (yellow)",287,100,0xFF886600,false);
            for(int i=0;i<8;i++){
                var target=npc==null?null:npc.assignments.view().get(CompanionAssignments.containerSlot(i>=4,i%4));
                String label=(i%4+1)+": "+(target==null?"Not assigned":target.name());
                g.text(font,fit(label,i<4?127:162),i<4?101:287,116+(i%4)*20,color(CompanionStatus.from(menu.value(71+i))),false);
            }
            g.text(font,"Planner: click to switch roles",288,198,0xFF404040,false);
        }else if(supplyView==1){
            g.text(font,"Job",101,100,0xFF404040,false);g.text(font,"Supplies",191,100,0xFF404040,false);g.text(font,"Outputs",332,100,0xFF404040,false);
            for(int i=0;i<4;i++){var t=npc==null?null:npc.assignments.view().get(i+1);g.text(font,fit((i+1)+" "+(t==null?"Unassigned":t.name()),85),101,117+i*23,0xFF404040,false);}
        }else {
            g.text(font,"Source",133,98,0xFF404040,false);g.text(font,"Destination",247,98,0xFF404040,false);
            for(int i=0;i<4;i++)g.text(font,Integer.toString(i+1),109,115+i*24,0xFF404040,false);
        }
    }
    private void supplyTooltip(GuiGraphicsExtractor g,int mx,int my){
        if(panel!=2 || supplyView!=0 || menu.filterRow()>=0 || npc()==null)return;
        int x=mx-leftPos,y=my-topPos;if(y<112 || y>=192)return;
        int index=(y-112)/20;boolean output=x>=287;if(x<(output?287:100) || x>=(output?453:230))return;
        var t=npc().assignments.view().get(CompanionAssignments.containerSlot(output,index));
        String text=t==null?"Select this companion with the Planner, then click a container. Click again to switch Supply / Output.":t.name()+" at "+t.at().pos().toShortString()+" in "+t.at().dimension().identifier()+" - "+CompanionStatus.from(menu.value(71+(output?4:0)+index)).label;
        g.setTooltipForNextFrame(font,Component.literal(text),mx,my);
    }
    private int assignmentRow(int y){
        for(int row=0;row<6;row++)if(y>=CompanionMenu.assignmentY(row) && y<CompanionMenu.assignmentY(row)+16)return row;
        return -1;
    }
    private int color(CompanionStatus status){return status.problem()?0xFF994433:status==CompanionStatus.WORKING?0xFF168030:status==CompanionStatus.UNSUPPORTED?0xFF775577:0xFF304030;}
    private String fit(String text,int width){return font.width(text)>width?font.plainSubstrByWidth(text,width-9)+"...":text;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF373737);g.fill(x+1,y+1,x+imageWidth-2,y+imageHeight-2,0xFFFFFFFF);g.fill(x+3,y+3,x+imageWidth-1,y+imageHeight-1,0xFF555555);g.fill(x+4,y+4,x+imageWidth-4,y+imageHeight-4,0xFFC6C6C6);
        for(var slot:menu.slots){if(!slot.isActive())continue;int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFFFFFFFF);g.fill(sx-1,sy-1,sx+16,sy+16,0xFF373737);g.fill(sx,sy,sx+16,sy+16,0xFF8B8B8B);}
        if(panel==0 && menu.filterRow()<0)for(int i=0;i<6;i++){
            int rowY=y+CompanionMenu.assignmentY(i);
            g.fill(x+99,rowY,x+(i==0 || i>=5?imageWidth-20:252),rowY+14,0xFFDADADA);
            if(i>=CompanionAssignments.SUPPLY)g.fill(x+99,rowY,x+101,rowY+14,i==CompanionAssignments.SUPPLY?CompanionAssignments.SUPPLY_COLOR:CompanionAssignments.OUTPUT_COLOR);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        g.text(font,fit(title.getString(),222),8,8,0xFF404040,false);
        g.text(font,"Energy "+menu.value(1)+"%   Health "+menu.value(2)+"%",8,24,0xFF404040,false);
        g.text(font,"Range: "+menu.value(3),151,61,0xFF404040,false);
        g.text(font,"Costume",8,42,0xFF404040,false);g.text(font,"Hand",66,42,0xFF404040,false);g.text(font,"Storage",16,82,0xFF404040,false);
        g.text(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF404040,false);
        var status=CompanionStatus.from(menu.value(11));
        g.text(font,fit(status.label,84),8,140,color(status),false);
        if(menu.filterRow()>=4){
            g.text(font,"Porter route "+(menu.filterRow()-3)+" filter",105,82,0xFF404040,false);
            g.text(font,"Leave: minimum source stock per item",105,179,0xFF404040,false);
            g.text(font,"Keep: destination target (0 = unlimited)",105,191,0xFF404040,false);
            g.text(font,"Ghost items; right-click clears. Empty = any.",105,203,0xFF404040,false);return;
        }
        if(menu.filterRow()>=0){
            var companion=npc();var target=companion==null?null:companion.assignments.view().get(menu.filterRow()+1);
            g.text(font,"Filter: "+(target==null?"Workstation":fit(target.name(),200)),105,82,0xFF404040,false);
            g.text(font,"Up to nine allowed outputs",105,174,0xFF404040,false);
            g.text(font,"Click an item into a slot. Your item stays yours.",105,187,0xFF404040,false);
            g.text(font,"Right-click to clear. Empty filter allows anything.",105,200,0xFF404040,false);return;
        }
        if(panel==2){supplyLabels(g);return;}
        if(panel==1){
            g.text(font,"Break at: "+menu.value(19)+"%",101,124,0xFF404040,false);
            g.text(font,"Resume at: "+menu.value(20)+"%",101,143,0xFF404040,false);return;
        }
        var npc=npc();
        for(int i=0;i<6;i++){
            var target=npc==null?null:npc.assignments.view().get(i);
            var rowStatus=CompanionStatus.from(menu.value(CompanionMenu.assignmentData(i)));
            String prefix=i==0?"Home":i==5?"Lunch":i==6?"Supply":i==7?"Output":"Work "+i;
            String label=prefix+": "+(target==null?"Not assigned":target.name()+" "+target.at().pos().toShortString());
            g.text(font,fit(label,i==0 || i>=5?imageWidth-125:148),101,CompanionMenu.assignmentY(i)+3,color(rowStatus),false);
        }

    }
}

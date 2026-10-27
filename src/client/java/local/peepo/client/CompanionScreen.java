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
    private Button stockMode,stockApply,stockOutputs;
    private final Button[] stockAdjust=new Button[4];
    private EditBox stockTarget,stockRestock;
    private int shownStockSlot=-1,shownStockRevision=-1,shownStockRow=-1;
    private Button sparklingCider,agedCider,honey,honeycomb,draught,salve;
    private boolean stockPanel(){
        var view=menu.stockView;var n=npc();
        return view!=null && view.entries().size()==9 && menu.filterRow()>=0 && menu.filterRow()<4 && view.row()==menu.filterRow() && n!=null
            && n.assignments.view().get(view.row()+1)!=null && n.assignments.view().get(view.row()+1).at().pos().equals(view.anchor());
    }
    private int stockSlot(){return Math.clamp(menu.value(79),0,8);}
    private int number(EditBox box){try{return Integer.parseInt(box.getValue());}catch(NumberFormatException ignored){return -1;}}
    private void sendStock(int action,int target,int restock){
        if(!stockPanel())return;var v=menu.stockView;
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new CompanionStockEditPayload(menu.containerId,v.row(),v.anchor(),v.revision(),stockSlot(),action,target,restock));
    }
    private void initStock(){
        stockMode=addRenderableWidget(Button.builder(Component.literal("Unlimited"),b->{var e=menu.stockView.entries().get(stockSlot());sendStock(0,e.target()==0?64:0,e.target()==0?32:0);}).bounds(leftPos+105,topPos+118,139,18).build());
        stockTarget=addRenderableWidget(new EditBox(font,leftPos+183,topPos+142,39,16,Component.literal("Production target")));
        stockRestock=addRenderableWidget(new EditBox(font,leftPos+183,topPos+166,39,16,Component.literal("Restock at")));
        for(var box:new EditBox[]{stockTarget,stockRestock}){box.setMaxLength(4);}
        for(int i=0;i<4;i++){final int index=i;stockAdjust[i]=addRenderableWidget(Button.builder(Component.literal(i%2==0?"-":"+"),b->{
            int target=Math.clamp(number(stockTarget)+(index<2?(index%2==0?-1:1):0),1,CompanionProductionStock.MAX_TARGET);
            int restock=Math.clamp(number(stockRestock)+(index>=2?(index%2==0?-1:1):0),0,target-1);
            stockTarget.setValue(Integer.toString(target));stockRestock.setValue(Integer.toString(restock));sendStock(1,target,restock);
        }).bounds(leftPos+(i%2==0?163:226),topPos+141+(i/2)*24,17,18).build());}
        stockApply=addRenderableWidget(Button.builder(Component.literal("Apply quantities"),b->sendStock(1,number(stockTarget),number(stockRestock))).bounds(leftPos+105,topPos+187,139,17).build());
        stockOutputs=addRenderableWidget(Button.builder(Component.literal("Use linked Outputs"),b->sendStock(2,0,0)).bounds(leftPos+330,topPos+207,136,17).build());
        tip(stockMode,"Per-product goal shared by helpers at this workstation. Unlimited preserves normal production.");
        tip(stockApply,"Count items, not stacks (1-4096). Restock at must be lower than Target. Already committed batches finish.");
        tip(stockOutputs,"Replace this workstation's shared counted-container list with this job's current linked Outputs. Configure links under Supplies.");
    }
    private void updateStock(){
        boolean visible=stockPanel();stockMode.visible=stockApply.visible=stockOutputs.visible=visible;
        stockTarget.visible=stockRestock.visible=visible;for(var b:stockAdjust)b.visible=visible;
        if(!visible){stockTarget.setFocused(false);stockRestock.setFocused(false);shownStockRow=-1;return;}
        var v=menu.stockView;int slot=stockSlot();var e=v.entries().get(slot);
        boolean item=e.target()>=0 && !menu.getSlot(CompanionMenu.FILTER_START+slot).getItem().isEmpty();
        if(shownStockRow!=v.row() || shownStockSlot!=slot || shownStockRevision!=v.revision()){
            stockTarget.setValue(Integer.toString(e.target()<=0?64:e.target()));stockRestock.setValue(Integer.toString(e.target()<=0?32:e.restock()));
            shownStockRow=v.row();shownStockSlot=slot;shownStockRevision=v.revision();
        }
        stockMode.active=item;stockMode.setMessage(Component.literal(e.target()<0?"Machine controls":e.target()==0?"Unlimited":"Maintain stock"));
        boolean edit=item && e.target()>0;stockTarget.setEditable(edit);stockRestock.setEditable(edit);
        for(var b:stockAdjust)b.active=edit;
        stockApply.active=edit && number(stockTarget)>0 && number(stockTarget)<=CompanionProductionStock.MAX_TARGET && number(stockRestock)>=0 && number(stockRestock)<number(stockTarget);
    }
    private void stockLabels(GuiGraphicsExtractor g){
        var v=menu.stockView;var item=menu.getSlot(CompanionMenu.FILTER_START+stockSlot()).getItem();var e=v.entries().get(stockSlot());
        g.text(font,item.isEmpty()?"Select a product":fit(item.getHoverName().getString(),139),105,104,0xFF404040,false);
        g.text(font,"Target",105,146,0xFF404040,false);g.text(font,"Restock at",105,170,0xFF404040,false);
        g.text(font,"Stored: "+(e.available()?e.stored():"?"),320,108,0xFF404040,false);
        g.text(font,"At station / on way:",320,122,0xFF404040,false);
        g.text(font,Integer.toString(e.underway()),320,135,0xFF404040,false);
        g.text(font,e.target()<0?"Use machine fluid target":e.available()?(e.target()==0?"Unlimited":e.stored()+e.underway()>=e.target()?"Target covered":"Restock when needed"):"Stock unavailable",320,152,e.available()?0xFF246535:0xFF994433,false);
        g.text(font,fit("Left-click selects; right-click clears.",210),258,172,0xFF404040,false);
        g.text(font,fit("Empty filter allows any available recipe.",210),258,185,0xFF404040,false);
        g.text(font,"Count stock in (shared):",105,208,0xFF404040,false);
        if(v.outputs().isEmpty())g.text(font,"Link Outputs, then use the button to adopt them.",105,222,0xFF555555,false);
        for(int i=0;i<v.outputs().size();i++)g.text(font,fit(v.outputs().get(i),i==0?220:355),105,222+i*9,0xFF555555,false);
    }
    private static final String[] MODES={"Follow","Stay","Home","Work"};
    private int panel; // Jobs and routine. Transport controls live on each job row.
    private Button tab,supplyTab;
    private int supplyView;
    private final Button[] supplyNav=new Button[3],storageRemove=new Button[8],tools=new Button[4],links=new Button[40],routes=new Button[16],limits=new Button[2];
    private Button handLock,renameCompanion,detailsBack,nameSave;
    private EditBox nameBox;
    private final Button[] renameJobs=new Button[4],zoneOptions=new Button[4],animalTasks=new Button[5],gardenTasks=new Button[2];
    private static final String[] ANIMAL_TASKS={"Shearing","Milking","Eggs","Breeding","Slaughter"};
    private Button populationOpen,populationBack,populationRefresh,populationType,populationAdd;
    private final Button[] populationAdjust=new Button[4];
    private net.minecraft.resources.Identifier animalType;
    private java.util.List<net.minecraft.resources.Identifier> animalTypes(){
        var n=npc();var t=n==null?null:n.assignments.view().get(detailRow);var list=new java.util.ArrayList<net.minecraft.resources.Identifier>();
        if(t!=null && t.zone()!=null)for(var p:t.zone().populations())list.add(p.type());
        if(menu.populationRow==detailRow && menu.populationAnchor.equals(detailAnchor))for(var id:menu.populationTypes)if(!list.contains(id))list.add(id);return list;
    }
    private void populationEdit(int action){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new PopulationEditPayload(menu.containerId,detailRow,detailAnchor,action,animalType==null?net.minecraft.resources.Identifier.withDefaultNamespace("pig"):animalType));}
    private void initPopulation(){
        populationOpen=addRenderableWidget(Button.builder(Component.literal("Population..."),b->{panel=4;populationEdit(0);updateButtons();}).bounds(leftPos+346,topPos+181,117,20).build());
        populationBack=addRenderableWidget(Button.builder(Component.literal("< Back"),b->{panel=5;updateButtons();}).bounds(leftPos+104,topPos+78,75,18).build());
        populationRefresh=addRenderableWidget(Button.builder(Component.literal("Refresh animals"),b->populationEdit(0)).bounds(leftPos+348,topPos+78,115,18).build());
        populationType=addRenderableWidget(Button.builder(Component.literal("Animal"),b->{var types=animalTypes();if(!types.isEmpty())animalType=types.get((types.indexOf(animalType)+1)%types.size());}).bounds(leftPos+104,topPos+102,250,20).build());
        populationAdd=addRenderableWidget(Button.builder(Component.literal("Add type"),b->{var t=npc().assignments.view().get(detailRow);populationEdit(t.zone().population(animalType)==null?1:2);}).bounds(leftPos+360,topPos+102,103,20).build());
        for(int i=0;i<4;i++){final int action=3+i;populationAdjust[i]=addRenderableWidget(Button.builder(Component.literal(i%2==0?"-":"+"),b->populationEdit(action)).bounds(leftPos+322+(i%2)*116,topPos+132+(i/2)*25,25,20).build());}
        tip(populationType,"Click to cycle types found inside this zone, including compatible modded animals. Add only the types you want managed.");
        tip(populationAdjust[0],"Total population to breed toward, including babies and pending pairs. Zero disables breeding for this type.");
        tip(populationAdjust[2],"Minimum adults to retain. Babies, named, owned, leashed and breeding animals are never slaughtered.");
    }
    private void updatePopulation(boolean filtering){
        var n=npc();var t=n==null?null:n.assignments.view().get(detailRow);boolean zone=t!=null && t.care() && t.zone()!=null && t.at().pos().equals(detailAnchor);
        populationOpen.visible=panel==5 && !filtering && zone;boolean show=panel==4 && !filtering;
        populationBack.visible=populationRefresh.visible=populationType.visible=populationAdd.visible=show;
        var types=animalTypes();if(!types.contains(animalType))animalType=types.isEmpty()?null:types.getFirst();
        var rule=zone && animalType!=null?t.zone().population(animalType):null;
        populationRefresh.active=zone;populationType.active=zone && !types.isEmpty();
        String name=animalType==null?"No animals found":net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(animalType)?net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(animalType).getDescription().getString():animalType.toString();
        populationType.setMessage(Component.literal(fit(name+" >",235)));if(animalType!=null)tip(populationType,animalType+". Click to cycle. Only added types are bred/slaughtered.");
        populationAdd.setMessage(Component.literal(rule==null?"Add type":"Remove type"));populationAdd.active=zone && animalType!=null && (rule!=null || t.zone().populations().size()<AnimalCareZone.MAX_SPECIES);
        for(int i=0;i<4;i++){populationAdjust[i].visible=show;int value=rule==null?0:i<2?rule.target():rule.keep();populationAdjust[i].active=zone && rule!=null && (i%2==0?value>0:value<AnimalCareZone.MAX_POPULATION);}
    }
    private int detailRow;
    private net.minecraft.core.BlockPos detailAnchor=net.minecraft.core.BlockPos.ZERO;
    private void chooseDetails(int row){
        detailRow=row;var npc=npc();if(npc==null)return;var target=npc.assignments.view().get(row);
        detailAnchor=row==0 || target==null?net.minecraft.core.BlockPos.ZERO:target.at().pos();
        nameBox.setValue(row==0?npc.hasCustomName()?npc.getCustomName().getString():"":target==null?"":npc.assignments.label(row));
    }
    private void sendDetails(int action){
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new CompanionEditPayload(menu.containerId,detailRow,detailAnchor,action,action==0?nameBox.getValue():""));
    }
    private void initDetails(){
        renameCompanion=renameButton(0,240,6);
        tip(renameCompanion,"Rename companion");
        detailsBack=addRenderableWidget(Button.builder(Component.literal("< Jobs"),b->{panel=0;updateButtons();}).bounds(leftPos+104,topPos+78,75,18).build());
        for(int i=0;i<4;i++){
            final int row=i+1;
            renameJobs[i]=renameButton(row,100,CompanionMenu.assignmentY(row));
            tip(renameJobs[i],"Rename job "+row);
            zoneOptions[i]=iconButton("Zone options",CompanionMenu.RECIPE_X,CompanionMenu.assignmentY(row),GEAR,b->openDetails(row,true));
            tip(zoneOptions[i],"Zone options");
        }
        nameBox=addRenderableWidget(new EditBox(font,leftPos+104,topPos+113,278,18,Component.literal("Name")));nameBox.setMaxLength(CompanionNames.LIMIT);
        nameSave=addRenderableWidget(Button.builder(Component.literal("Save"),b->sendDetails(0)).bounds(leftPos+388,topPos+112,75,20).build());
        for(int i=0;i<5;i++){final int bit=1<<i;animalTasks[i]=addRenderableWidget(Button.builder(Component.literal(ANIMAL_TASKS[i]),b->sendDetails(bit)).bounds(leftPos+104+(i%3)*121,topPos+158+(i/3)*23,117,20).build());}
        for(int i=0;i<2;i++){final int bit=i==0?CompanionAssignments.REPAIR:CompanionAssignments.FERTILIZE;
            gardenTasks[i]=addRenderableWidget(Button.builder(Component.empty(),b->sendDetails(bit)).bounds(leftPos+104+i*181,topPos+158,176,20).build());}
        tip(gardenTasks[0],"Repair only previously observed farmland, using a hoe. Never tills paths or arbitrary dirt.");
        tip(gardenTasks[1],"Use available bone meal, bat guano or superphosphate. Carry at most 4; one application per zone every 20 seconds and per crop every 60 seconds. Harvest and planting come first.");
        initPopulation();
    }
    private void openDetails(int row,boolean zone){
        chooseDetails(row);panel=zone?5:3;updateButtons();
        if(!zone){setFocused(nameBox);nameBox.setFocused(true);}
    }
    private String companionTitle(){return fit(npc()==null?title.getString():npc().getDisplayName().getString(),250);}
    private void updateDetails(boolean filtering){
        boolean naming=panel==3 && !filtering,zone=panel==5 && !filtering;var npc=npc();
        renameCompanion.visible=!filtering;renameCompanion.active=npc!=null;
        renameCompanion.setX(leftPos+14+font.width(companionTitle()));
        detailsBack.visible=naming || zone;
        var target=npc==null?null:npc.assignments.view().get(detailRow);
        boolean valid=npc!=null && (detailRow==0 || target!=null && target.at().pos().equals(detailAnchor));
        nameBox.visible=nameSave.visible=naming;nameSave.active=valid;nameBox.setEditable(valid);
        for(int i=0;i<4;i++){
            var job=npc==null?null:npc.assignments.view().get(i+1);
            renameJobs[i].visible=panel==0 && !filtering;renameJobs[i].active=job!=null;
            zoneOptions[i].visible=panel==0 && !filtering && job!=null && (job.care() || job.gardenZone()!=null && !job.orchard());
        }
        for(int i=0;i<5;i++){var b=animalTasks[i];b.visible=zone && target!=null && target.care();b.active=valid;b.setMessage(Component.literal(ANIMAL_TASKS[i]+": "+(target!=null && target.task(1<<i)?"On":"Off")));}
        for(int i=0;i<2;i++){var b=gardenTasks[i];b.visible=zone && target!=null && target.gardenZone()!=null && !target.orchard();b.active=valid;
            b.setMessage(Component.literal((i==0?"Repair farmland: ":"Fertilizing: ")+(npc!=null && npc.assignments.gardenMaintenance(detailRow,i==0?CompanionAssignments.REPAIR:CompanionAssignments.FERTILIZE)?i==0?"On":"Auto":"Off")));}
        if(!naming && getFocused()==nameBox)setFocused(null);
    }
    public CompanionScreen(CompanionMenu menu,Inventory inventory,Component title){super(menu,inventory,title,CompanionMenu.WIDTH,CompanionMenu.HEIGHT);}
    private Button button(String text,int x,int y,int width,int height,int id){
        return addRenderableWidget(Button.builder(Component.literal(text),b->{if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}).bounds(leftPos+x,topPos+y,width,height).build());
    }
    // Draw the pencil directly so it stays legible without relying on font glyph coverage.
    private static final String[] PENCIL={"......ee.",".....eee.","....ssse.","...yyys..","..yyys...",".yyys....","wyys.....","wws......","k........"};
    private static final String[] GEAR={"....sss....",".s.sssss.s.",".sssssssss.","..ss...ss..","sss.....sss","sss.....sss","sss.....sss","..ss...ss..",".sssssssss.",".s.sssss.s.","....sss...."};
    private static final String[] FUNNEL={"sssssssssss","sssssssssss",".sssssssss.","..sssssss..","...sssss...","....sss....","....sss....","....sss....","....ss....."};
    private Button renameButton(int row,int x,int y){
        return iconButton(row==0?"Rename companion":"Rename job "+row,x,y,PENCIL,b->openDetails(row,false));
    }
    private Button iconButton(String label,int x,int y,String[] pixels,Button.OnPress onPress){
        return addRenderableWidget(new Button(leftPos+x,topPos+y,16,16,Component.literal(label),onPress,n->n.get()){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
                extractDefaultSprite(g);
                int ox=getX()+(getWidth()-pixels[0].length())/2,oy=getY()+(getHeight()-pixels.length)/2;
                for(int py=0;py<pixels.length;py++)for(int px=0;px<pixels[py].length();px++){
                    char pixel=pixels[py].charAt(px);if(pixel=='.')continue;
                    int color=!active?0xFF888888:switch(pixel){case 'e'->0xFFE79099;case 's'->0xFFCACACA;case 'y'->0xFFFFCF53;case 'w'->0xFFF5D9A0;default->0xFF303030;};
                    g.fill(ox+px,oy+py,ox+px+1,oy+py+1,color);
                }
            }
        });
    }
    private void tip(Button b,String text){b.setTooltip(Tooltip.create(Component.literal(text)));}
    @Override protected void init(){
        super.init();inventoryLabelX=80;inventoryLabelY=218+CompanionMenu.STATUS_SPACE;
        tab=addRenderableWidget(Button.builder(Component.literal("Routine >"),b->{panel=panel==1?0:1;updateButtons();}).bounds(leftPos+imageWidth-84,topPos+6,76,16).build());
        supplyTab=addRenderableWidget(Button.builder(Component.literal("Supplies"),b->{panel=panel==2?0:2;updateButtons();}).bounds(leftPos+imageWidth-166,topPos+6,78,16).build());
        initSupplies();initDetails();initStock();
        for(int i=0;i<4;i++){
            final int id=60+i;
            filters[i]=iconButton("Filter",CompanionMenu.RECIPE_X,CompanionMenu.assignmentY(i+1),FUNNEL,b->{
                if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);
            });
            tip(filters[i],"Filter");
        }
        filterBack=button("< Back",373,78,70,18,64);
        draught=button("Draught",105,109,125,20,69);
        salve=button("Salve",105,135,125,20,70);
        tip(draught,"Supply glass bottles for the written formula. Empty / both defaults to Draught.");
        tip(salve,"Supply bowls for the written formula. Existing brews finish with containers already loaded.");
        honey=button("Honey",105,109,125,20,67);
        honeycomb=button("Honeycomb",105,135,125,20,68);
        tip(honey,"Collect honey bottles. Supply empty glass bottles and place a smoking campfire below the hive.");
        tip(honeycomb,"Collect honeycomb using equipped shears or a Tools supply chest. Campfire smoke is required.");
        sparklingCider=button("Sparkling",105,109,125,20,65);
        agedCider=button("Aged",105,135,125,20,66);
        tip(sparklingCider,"Collect after one Minecraft day (20 minutes). Overdue batches are collected as aged cider.");
        tip(agedCider,"Wait three Minecraft days (60 minutes) before collecting.");
        for(int i=0;i<8;i++)transport[i]=button("",254+(i%2)*70,CompanionMenu.assignmentY(1+i/2),68,16,50+i);
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
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){
        if(panel==3 && nameBox.isFocused() && event.input()!=256){if(event.input()==257){if(nameSave.active)sendDetails(0);return true;}nameBox.keyPressed(event);return true;}
        if(stockPanel() && (stockTarget.isFocused() || stockRestock.isFocused()) && event.input()!=256){
            if(event.input()==257){if(stockApply.active)sendStock(1,number(stockTarget),number(stockRestock));return true;}
            (stockTarget.isFocused()?stockTarget:stockRestock).keyPressed(event);return true;
        }
        return super.keyPressed(event);
    }
    private PeepoEntity npc(){return minecraft==null || minecraft.player==null?null:menu.companion(minecraft.player);}
    private void updateButtons(){
        boolean filtering=menu.filterRow()>=0;
        if(filtering)panel=menu.filterRow()>=4?2:0;
        menu.showRecipes=panel==0 || filtering;
        tab.visible=supplyTab.visible=!filtering;
        updateSupplies(filtering);updateDetails(filtering);updatePopulation(filtering);updateStock();filterBack.visible=filtering;
        boolean crucible=filtering && menu.filterRow()<4 && menu.value(25+menu.filterRow())==11;
        draught.visible=salve.visible=crucible;
        if(crucible){
            boolean d=false,s=false;
            for(int i=0;i<9;i++){var item=menu.getSlot(CompanionMenu.FILTER_START+i).getItem();d|=item.is(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.DRAUGHT);s|=item.is(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.SALVE);}
            boolean selected=s && !d;draught.setMessage(Component.literal(selected?"Draught":"Draught (selected)"));salve.setMessage(Component.literal(selected?"Salve (selected)":"Salve"));
        }
        boolean hive=filtering && menu.filterRow()<4 && menu.value(25+menu.filterRow())==9;
        honey.visible=honeycomb.visible=hive;
        if(hive){
            boolean bottle=false,comb=false;
            for(int i=0;i<9;i++){var item=menu.getSlot(CompanionMenu.FILTER_START+i).getItem();bottle|=item.is(net.minecraft.world.item.Items.HONEY_BOTTLE);comb|=item.is(net.minecraft.world.item.Items.HONEYCOMB);}
            boolean selected=comb && !bottle;
            honey.setMessage(Component.literal(selected?"Honey":"Honey (selected)"));
            honeycomb.setMessage(Component.literal(selected?"Honeycomb (selected)":"Honeycomb"));
        }
        boolean cider=filtering && menu.filterRow()<4 && menu.value(25+menu.filterRow())==6;
        sparklingCider.visible=agedCider.visible=cider;
        if(cider){
            boolean hasSparkling=false,hasAged=false;
            for(int i=0;i<9;i++){
                var item=menu.getSlot(CompanionMenu.FILTER_START+i).getItem();
                hasSparkling|=item.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item("sparkling_cider"));
                hasAged|=item.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item("aged_cider"));
            }
            boolean aged=hasAged && !hasSparkling;
            sparklingCider.setMessage(Component.literal(aged?"Sparkling":"Sparkling (selected)"));
            agedCider.setMessage(Component.literal(aged?"Aged (selected)":"Aged"));
        }
        for(int i=0;i<4;i++)filters[i].visible=panel==0 && !filtering && menu.value(25+i)>0 && menu.value(25+i)!=4;
        for(var b:buttons)if(b!=null)b.visible=!filtering;
        tab.setMessage(Component.literal(panel==1?"< Jobs":"Routine"));supplyTab.setMessage(Component.literal(panel==2?"< Jobs":"Supplies"));
        for(var setting:settings)setting.visible=panel==1;
        var npc=npc();
        for(int i=0;i<8;i++){
            int value=menu.value(31+i),mode=Math.min(2,value&3),reason=value>>2;
            var b=transport[i];b.visible=panel==0 && !filtering;b.active=npc!=null && npc.assignments.view().get(1+i/2)!=null && reason!=3;
            b.setMessage(Component.literal((i%2==0?"Supply: ":"Out: ")+new String[]{"Auto","On","Off"}[mode]).withColor(reason==2?0xFFCC66:reason==1?0xAAAAAA:0xFFFFFF));
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
        if(panel==0 && menu.filterRow()<0 && npc()!=null)for(int i=1;i<=4;i++){
            int sy=topPos+CompanionMenu.assignmentY(i)+17;
            if(mouseX>=leftPos+101 && mouseX<leftPos+imageWidth-12 && mouseY>=sy && mouseY<sy+10)
                g.setTooltipForNextFrame(font,Component.literal(menu.jobDetail(i,npc().assignments.view().get(i))),mouseX,mouseY);
        }
        int row=assignmentRow(mouseY-topPos);var npc=npc();
        if(panel==0 && menu.filterRow()<0 && npc!=null && mouseX>=leftPos+(row>0 && row<5?118:100) && mouseX<leftPos+(row>0 && row<5?253:imageWidth-20) && mouseY>=topPos+78 && row>=0 && row<CompanionAssignments.COUNT){
            var target=npc.assignments.view().get(row);
            String text=target==null?"Use the Companion Planner to select this companion, then right-click a "+(row==0?"bed.":row==5?"lunch crate or lunch cover.":row==6?"container for Supply (blue); click again to switch roles.":row==7?"container for Output (yellow); click again to switch roles.":"workstation or farmland."):
                npc.assignments.label(row)+" at "+target.at().pos().toShortString()+" in "+target.at().dimension().identifier()+" - "+CompanionStatus.from(menu.value(CompanionMenu.assignmentData(row))).label;
            if(row>0 && row<5 && target!=null)text+=". Priority "+row+" (top is highest).";
            if(target!=null && target.garden())text+=target.gardenZone()!=null?(target.orchard()?" Orchard fruit picking, with linked Output. Leave safe access below the canopy.":" Garden zone: farmland, planters, awakened Verdant Beds, cane, bamboo, cactus, berries, cocoa, pumpkins, melons and flooded rice. Supply brings planting items; Output takes surplus. Select a crop to establish new ground plantings. The gear opens optional farmland repair and limited fertilizing."):CompanionGarden.hanging(target)?" Up to 8 saved hanging spots. Supply brings ready-made bundles; Output takes dried herbs. Fresh bundles dry naturally.":CompanionGarden.mushrooms(target)?" Up to 8 connected mushroom soil blocks. Shears or a knife in Hand are required for picking. Supply brings mushrooms; Output takes surplus harvests.":" Up to 8 connected farmland or planter boxes. A hoe in Hand halves work time. Supply brings seeds; Output takes surplus harvests.";
            if(target!=null && target.block().equals(local.peepo.PeepoMod.id("sprinkler")))text+=" Supply water buckets and Fertilizer; Output receives empty buckets. Keeps up to four fertilizer stocked. Uses one work slot; pipe and hopper inputs still work.";
            if(target!=null && target.treeTap())text+=" Supply empty glass bottles; Output receives latex bottles. Leave safe ground in front of the tap. Bucket upgrades remain manual.";
            if(target!=null && target.water())text+=" Supply empty bottles, buckets or partly filled gourd canteens; Output receives filled containers. Leave a dry bank. Buckets remove source water: use an infinite-water pool for repeated filling.";
            if(target!=null && target.care())text+=" Tasks can be enabled independently using this job's Zone options button. Uses the outlined rectangular area; missing shears or buckets do not stop other tasks.";
            if(target!=null && target.shearing())text+=" Adult woolly sheep within 8 blocks. Give shears in Hand or a Tools supply; Output receives wool.";
            if(target!=null && target.milking())text+=" Adult cows within 8 blocks. Supply brings empty buckets; Output receives milk. Each cow rests one minute between milkings.";
            if(target!=null && target.eggs())text+=" Collects dropped eggs within 8 blocks. No tools or Supply needed; Output receives collected eggs. Chickens lay at their normal rate.";
            if(row==6 || row==7)text+=" Porter moves items from Supply to Output. Work uses these containers for machine recipes.";
            g.setTooltipForNextFrame(font,Component.literal(text),mouseX,mouseY);
        }
        if(panel==0 && menu.filterRow()<0)for(int i=0;i<4;i++){
            var slot=menu.getSlot(CompanionMenu.RECIPE_START+i);
            if(slot.isActive() && mouseX>=leftPos+slot.x && mouseX<leftPos+slot.x+16 && mouseY>=topPos+slot.y && mouseY<topPos+slot.y+16){
                var lines=new java.util.ArrayList<Component>();
                if(menu.value(25+i)==4){
                    lines.add(slot.hasItem()?Component.literal("Grow / hang: ").append(slot.getItem().getHoverName()):Component.literal("Automatic planting / hanging"));
                    lines.add(Component.literal("Click with a seed, mushroom or herb bundle to choose what to grow or hang."));
                    lines.add(Component.literal("Your item stays on the cursor. Right-click to restore Auto."));
                    lines.add(Component.literal("Supply brings matching planting items or bundles. Existing crops grow and bundles dry first."));
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
                tip(b,(target==null?"Not assigned":npc.assignments.label(CompanionAssignments.containerSlot(part==1,index))+" at "+target.at().pos().toShortString())+". Click to cycle; changing the endpoint disables this route.");}
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
                String label=(i%4+1)+": "+(target==null?"Not assigned":npc.assignments.label(CompanionAssignments.containerSlot(i>=4,i%4)));
                g.text(font,fit(label,i<4?127:162),i<4?101:287,116+(i%4)*20,color(CompanionStatus.from(menu.value(71+i))),false);
            }
            g.text(font,"Planner: click to switch roles",288,198,0xFF404040,false);
        }else if(supplyView==1){
            g.text(font,"Job",101,100,0xFF404040,false);g.text(font,"Supplies",191,100,0xFF404040,false);g.text(font,"Outputs",332,100,0xFF404040,false);
            for(int i=0;i<4;i++){var t=npc==null?null:npc.assignments.view().get(i+1);g.text(font,fit((i+1)+" "+(t==null?"Unassigned":npc.assignments.label(i+1)),85),101,117+i*23,0xFF404040,false);}
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
        String text=t==null?"Select this companion with the Planner, then click a container. Click again to switch Supply / Output.":npc().assignments.label(CompanionAssignments.containerSlot(output,index))+" at "+t.at().pos().toShortString()+" in "+t.at().dimension().identifier()+" - "+CompanionStatus.from(menu.value(71+(output?4:0)+index)).label;
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
        if(stockPanel()){
            var slot=menu.getSlot(CompanionMenu.FILTER_START+stockSlot());int sx=x+slot.x,sy=y+slot.y;
            g.fill(sx-2,sy-2,sx+18,sy-1,0xFF27863B);g.fill(sx-2,sy+17,sx+18,sy+18,0xFF27863B);
            g.fill(sx-2,sy-1,sx-1,sy+17,0xFF27863B);g.fill(sx+17,sy-1,sx+18,sy+17,0xFF27863B);
        }
        if(panel==0 && menu.filterRow()<0)for(int i=0;i<6;i++){
            int rowY=y+CompanionMenu.assignmentY(i);
            g.fill(x+99,rowY,x+(i==0 || i>=5?imageWidth-20:252),rowY+14,0xFFDADADA);
            if(i>0 && i<5)g.fill(x+99,rowY+16,x+imageWidth-10,rowY+27,0xFFD1D1D1);
            if(i>=CompanionAssignments.SUPPLY)g.fill(x+99,rowY,x+101,rowY+14,i==CompanionAssignments.SUPPLY?CompanionAssignments.SUPPLY_COLOR:CompanionAssignments.OUTPUT_COLOR);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        g.text(font,companionTitle(),8,8,0xFF404040,false);
        g.text(font,"Energy "+menu.value(1)+"%   Health "+menu.value(2)+"%",8,24,0xFF404040,false);
        g.text(font,"Range: "+menu.value(3),151,61,0xFF404040,false);
        g.text(font,"Costume",8,42,0xFF404040,false);g.text(font,"Hand",66,42,0xFF404040,false);g.text(font,"Storage",16,82,0xFF404040,false);
        g.text(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF404040,false);
        var status=CompanionStatus.from(menu.value(11));
        g.text(font,fit(status.label,84),8,140,color(status),false);
        g.text(font,"Paused while",8,156,0xFF555555,false);
        g.text(font,"editing",8,167,0xFF555555,false);
        if(menu.filterRow()>=4){
            g.text(font,"Porter route "+(menu.filterRow()-3)+" filter",105,82,0xFF404040,false);
            g.text(font,"Leave: minimum source stock per item",105,179,0xFF404040,false);
            g.text(font,"Keep: destination target (0 = unlimited)",105,191,0xFF404040,false);
            g.text(font,"Ghost items; right-click clears. Empty = any.",105,203,0xFF404040,false);return;
        }
        if(menu.filterRow()>=0){
            var companion=npc();var target=companion==null?null:companion.assignments.view().get(menu.filterRow()+1);
            g.text(font,"Filter: "+(target==null?"Workstation":fit(companion.assignments.label(menu.filterRow()+1),200)),105,82,0xFF404040,false);
            if(stockPanel()){stockLabels(g);return;}
            if(menu.value(25+menu.filterRow())==15){
                g.text(font,"Choose products or fluid output buckets.",105,174,0xFF404040,false);
                g.text(font,"Supply items/tools; fluids still use pipes.",105,187,0xFF404040,false);
                g.text(font,"Machine recipe locks still apply.",105,200,0xFF404040,false);return;
            }
            if(menu.value(25+menu.filterRow())==14){
                g.text(font,"Choose finished products. Empty allows any.",105,174,0xFF404040,false);
                g.text(font,"Supply ingredients, fuel and reusable tools.",105,187,0xFF404040,false);
                g.text(font,"Crucible furnace also needs ceramic parts.",105,200,0xFF404040,false);return;
            }
            if(menu.value(25+menu.filterRow())==12){
                g.text(font,"Choose compostable inputs for the Mulch Maw.",105,174,0xFF404040,false);
                g.text(font,"Keeps up to 16 items; pauses when beds are full.",105,187,0xFF404040,false);
                g.text(font,"Default: chaff. Empty allows any mulch.",105,200,0xFF404040,false);return;
            }
            if(menu.value(25+menu.filterRow())==11){
                g.text(font,"Set a written formula on the crucible first.",105,174,0xFF404040,false);
                g.text(font,"Supply exact reagents and bottles or bowls.",105,187,0xFF404040,false);
                g.text(font,"Water uses pipes. Heat follows the formula.",105,200,0xFF404040,false);return;
            }
            if(menu.value(25+menu.filterRow())==10){
                g.text(font,"Restocks remembered sale-slot items.",105,174,0xFF404040,false);
                g.text(font,"Stock each slot and set prices at the stand.",105,187,0xFF404040,false);
                g.text(font,"Empty filter allows all remembered goods.",105,200,0xFF404040,false);return;
            }
            if(menu.value(25+menu.filterRow())==9){
                g.text(font,"Choose honey bottles or honeycomb.",105,174,0xFF404040,false);
                g.text(font,"Empty / both: Honey. Campfire smoke required.",105,187,0xFF404040,false);
                g.text(font,"Supply bottles, or equip / supply shears.",105,200,0xFF404040,false);return;
            }
            if(menu.value(25+menu.filterRow())==6){
                g.text(font,"Choose when Peepo collects this barrel.",105,174,0xFF404040,false);
                g.text(font,"Empty / both: Sparkling. Bottles are reused.",105,187,0xFF404040,false);
                g.text(font,"If collected late, sparkling becomes aged.",105,200,0xFF404040,false);return;
            }
            g.text(font,"Up to nine allowed outputs",105,174,0xFF404040,false);
            g.text(font,"Click an item into a slot. Your item stays yours.",105,187,0xFF404040,false);
            g.text(font,"Right-click to clear. Empty filter allows anything.",105,200,0xFF404040,false);return;
        }
        if(panel==3){
            var companion=npc();var target=companion==null?null:companion.assignments.view().get(detailRow);
            g.text(font,detailRow==0?"Companion name":"Job name",104,101,0xFF404040,false);
            g.text(font,"Leave blank to restore the default name.",104,139,0xFF404040,false);

            return;
        }
        if(panel==5){
            var companion=npc();
            g.text(font,"Zone options",188,83,0xFF404040,false);
            g.text(font,companion==null?"":fit(companion.assignments.label(detailRow),350),104,107,0xFF404040,false);
            var target=companion==null?null:companion.assignments.view().get(detailRow);
            boolean garden=target!=null && target.gardenZone()!=null;
            g.text(font,garden?"Garden maintenance (optional)":"Livestock Management tasks",104,139,0xFF404040,false);
            if(garden)g.text(font,"Harvesting and replanting take priority.",104,190,0xFF404040,false);
            return;
        }
        if(panel==4){
            var n=npc();var t=n==null?null:n.assignments.view().get(detailRow);var rule=t==null || t.zone()==null || animalType==null?null:t.zone().population(animalType);
            g.text(font,"Population settings",188,83,0xFF404040,false);
            g.text(font,"Breed toward (total)",104,138,0xFF404040,false);g.text(font,rule==null?"--":Integer.toString(rule.target()),377,138,0xFF404040,false);
            g.text(font,"Adults to keep",104,163,0xFF404040,false);g.text(font,rule==null?"--":Integer.toString(rule.keep()),377,163,0xFF404040,false);
            g.text(font,rule==null?"Add this type to enable its population settings.":"Babies count toward the total; only surplus adults are culled.",104,190,0xFF404040,false);
            g.text(font,"Up to 8 types. Breeding / Slaughter switches are separate.",104,202,0xFF404040,false);return;
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
            String label=target==null?prefix+": Not assigned":(i>0 && i<5?"":prefix+": ")+npc.assignments.label(i)+" "+target.at().pos().toShortString();
            g.text(font,fit(label,i==0 || i>=5?imageWidth-125:130),i>0 && i<5?119:101,CompanionMenu.assignmentY(i)+3,color(rowStatus),false);
            if(i>0 && i<5)g.text(font,fit(menu.jobDetail(i,target),imageWidth-115),101,CompanionMenu.assignmentY(i)+18,color(rowStatus),false);
        }

    }
}

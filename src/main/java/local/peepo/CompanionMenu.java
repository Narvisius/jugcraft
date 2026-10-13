package local.peepo;

import net.minecraft.core.Registry;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.flag.FeatureFlags;

public final class CompanionMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT=79, RECIPE_START=46, FILTER_START=50;
    public static final int WIDTH=480, RECIPE_X=416;
    public static MenuType<CompanionMenu> TYPE;
    private final PeepoEntity npc;
    private final ContainerData data;
    private final Inventory playerInventory;
    private final net.minecraft.world.SimpleContainer recipeIcons=new net.minecraft.world.SimpleContainer(4);
    private final net.minecraft.world.SimpleContainer filterIcons=new net.minecraft.world.SimpleContainer(9);
    private int filterRow=-1;
    private CompanionAssignments.Target filterTarget;
    private final int[] recipeEnabled=new int[4];
    private final int[] transportDisplay=new int[8];
    private long nextRecipeRefresh,nextRecipeEdit;
    public boolean showRecipes=true;
    public static int assignmentY(int row){return row==0?78:row>=5?168+(row-5)*18:94+(row-1)*18;}
    public static int assignmentData(int row){return row<6?12+row:29+row-6;}
    public static void initialize(){TYPE=Registry.register(BuiltInRegistries.MENU,PeepoMod.id("companion_commands"),new MenuType<>(CompanionMenu::new,FeatureFlags.VANILLA_SET));}
    public CompanionMenu(int id,Inventory inventory){this(id,inventory,null);}
    public CompanionMenu(int id,Inventory inventory,PeepoEntity npc){
        super(TYPE,id);this.npc=npc;playerInventory=inventory;
        data=npc==null?new SimpleContainerData(DATA_COUNT):new ContainerData(){
            public int get(int i){
                var supplies=npc.assignments.supplies;
                if(i>=71)return npc.report.row(CompanionAssignments.containerSlot(i>=75,(i-71)%4));
                if(i>=67)return supplies.route(i-67).keep;
                if(i>=51){var r=supplies.route((i-51)/4);return switch((i-51)%4){case 0->r.source;case 1->r.destination;case 2->r.enabled?1:0;default->r.leave;};}
                if(i>=43)return supplies.mask(1+(i-43)/2,(i-43)%2==1);
                if(i==42)return supplies.handLocked()?1:0;
                if(i==41){int mask=0;for(int j=0;j<4;j++)if(supplies.tools(j))mask|=1<<j;return mask;}
                if(i==40)return filterRow;
                if(i==39)return npc.preferences.social?1:0;
                if(i>=31 && i<39)return transportDisplay[i-31];
                if(i>=29 && i<31)return npc.report.row(i-29+6);
                if(i>=25 && i<29)return recipeEnabled[i-25];
                if(i>=12 && i<18)return npc.report.row(i-12);
                return switch(i){case 0->npc.orders.mode();case 1->npc.getEnergy()*100/npc.getEnergyCapacity();case 2->Math.round(npc.getHealth()*100/npc.getMaxHealth());case 3->npc.orders.radius();case 4->npc.orders.party()?1:0;case 5->npc.orders.owner(inventory.player)?1:0;case 6->npc.orders.homeHere()?1:0;case 7->npc.orders.workHere()?1:0;case 8->npc.orders.targetAvailable()?1:0;case 9->npc.getId()&0xffff;case 10->(npc.getId()>>>16)&0xffff;
                    case 11->npc.report.overall();case 18->npc.preferences.schedule;case 19->npc.preferences.breakAt;case 20->npc.preferences.resumeAt;
                    case 21->npc.preferences.foodPolicy;case 22->npc.preferences.carryMeals;case 23->npc.preferences.alerts?1:0;case 24->npc.food.meals();default->0;};
            }
            public void set(int i,int value){}public int getCount(){return DATA_COUNT;}
        };
        addDataSlots(data);
        var contents=npc==null?new net.minecraft.world.SimpleContainer(10):npc.belongings;
        for(int row=0;row<2;row++)for(int col=0;col<4;col++)addSlot(new Slot(contents,col+row*4,16+col*18,96+row*18));
        addSlot(new Slot(contents,8,16,54){
            public boolean mayPlace(ItemStack stack){return stack.is(net.minecraft.world.item.Items.JACK_O_LANTERN);}
            public int getMaxStackSize(){return 1;}
        });
        addSlot(new Slot(contents,9,70,54){public int getMaxStackSize(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,80+col*18,230+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,80+col*18,288));
        for(int row=0;row<4;row++){
            final int at=row;
            addSlot(new Slot(recipeIcons,row,RECIPE_X,assignmentY(row+1)){
                public boolean mayPlace(ItemStack stack){return false;}
                public boolean mayPickup(Player player){return false;}
                public boolean isActive(){return showRecipes && value(25+at)==4 && filterRow()<0;}
            });
        }
        for(int i=0;i<9;i++)addSlot(new Slot(filterIcons,i,258+(i%3)*18,105+(i/3)*18){
            public boolean mayPlace(ItemStack stack){return false;}
            public boolean mayPickup(Player player){return false;}
            public boolean isActive(){return showRecipes && filterRow()>=0;}
        });
        refreshRecipes();
        if(npc!=null && stillValid(inventory.player))npc.openSettings(this);
    }
    /** The exact server menu must still be open; handles disconnects and replaced menus without player scans. */
    boolean editing(PeepoEntity companion){
        var player=playerInventory.player;
        return npc==companion && !player.isRemoved() && player.containerMenu==this && stillValid(player);
    }
    @Override public void removed(Player player){
        try{super.removed(player);}finally{if(npc!=null)npc.closeSettings(this);}
    }
    private CompanionAssignments.Target recipeTarget(int row){
        if(npc==null || row<0 || row>=4 || !stillValid(playerInventory.player))return null;
        var target=npc.assignments.get(row+1);
        if(target==null || !target.present(npc.level()) || target.at().pos().distToCenterSqr(npc.position())>64*64
            || !CompanionJobs.permitted(npc,target.at().pos())
            || io.github.jimbozoomer.jugcraft.town.TownProtection.denies(playerInventory.player,npc.level(),target.at().pos()))return null;
        return target;
    }
    private net.minecraft.world.level.block.entity.BlockEntity recipeStation(int row){
        var target=recipeTarget(row);if(target==null || target.livestock())return null;
        var be=npc.level().getBlockEntity(target.at().pos());
        return KitchenCompanionPort.of(be)!=null || be instanceof HearthOvenBlockEntity || be instanceof CookingPotBlockEntity pot && !pot.isLocked()
            || be instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity machine && !machine.isLocked() && (machine.companionPort.selectable() || machine.companionPort.locked())?be:null;
    }
    private void refreshRecipes(){
        if(npc==null)return;
        for(int row=0;row<4;row++){
            transportDisplay[row*2]=npc.assignments.transportDisplay(row+1,true);
            transportDisplay[row*2+1]=npc.assignments.transportDisplay(row+1,false);
            var station=recipeStation(row);
            var pot=station instanceof CookingPotBlockEntity p?p:null;
            var oven=station instanceof HearthOvenBlockEntity o?o:null;
            var machine=station instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity m?m:null;
            var target=recipeTarget(row);boolean garden=target!=null && target.garden();
            recipeEnabled[row]=garden?4:KitchenCompanionPort.of(station)!=null?5:machine!=null?3:oven!=null?2:pot!=null?1:0;
            var icon=ItemStack.EMPTY;
            if(garden){var seed=npc.assignments.gardenSeed(target);if(seed!=null)icon=new ItemStack(seed);}
            if(!ItemStack.matches(recipeIcons.getItem(row),icon))recipeIcons.setItem(row,icon);
        }
        if(filterRow>=4){for(int i=0;i<9;i++)filterIcons.setItem(i,npc.assignments.supplies.route(filterRow-4).icon(i));}
        else if(filterRow>=0){
            var target=recipeTarget(filterRow);var station=recipeStation(filterRow);
            var filter=station==null?null:CompanionFilters.get(station);
            if(target==null || !target.equals(filterTarget) || filter==null){filterRow=-1;filterTarget=null;}
            for(int i=0;i<9;i++)filterIcons.setItem(i,filterRow<0?ItemStack.EMPTY:filter.get(i));
        }
        nextRecipeRefresh=npc.level().getGameTime()+10;
    }
    @Override public void broadcastChanges(){
        if(npc!=null && npc.level().getGameTime()>=nextRecipeRefresh)refreshRecipes();
        super.broadcastChanges();
    }
    public int filterRow(){return npc==null?value(40):filterRow;}
    @Override public void clicked(int slot,int button,ContainerInput type,Player player){
        if(slot>=FILTER_START && slot<FILTER_START+9){
            if(npc==null || !stillValid(player) || type!=ContainerInput.PICKUP || button<0 || button>1 || filterRow<0)return;
            if(filterRow>=4){
                long time=npc.level().getGameTime();if(time<nextRecipeEdit)return;nextRecipeEdit=time+2;
                npc.assignments.supplies.route(filterRow-4).filter(slot-FILTER_START,button==1?ItemStack.EMPTY:getCarried());
                refreshRecipes();broadcastChanges();return;
            }
            var target=recipeTarget(filterRow);var station=recipeStation(filterRow);long now=npc.level().getGameTime();
            if(target==null || !target.equals(filterTarget) || station==null || now<nextRecipeEdit)return;nextRecipeEdit=now+2;
            if(!CompanionFilters.set(station,slot-FILTER_START,button==1?ItemStack.EMPTY:getCarried()))player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Choose an output made by this workstation."));
            refreshRecipes();broadcastChanges();return;
        }
        if(slot>=RECIPE_START && slot<RECIPE_START+4){
            // These are display copies, never inventory. Ignore drag, swap, clone, throw and shift-click.
            if(type!=ContainerInput.PICKUP || button<0 || button>1 || npc==null || !stillValid(player))return;
            int row=slot-RECIPE_START;var target=recipeTarget(row);long now=npc.level().getGameTime();
            if(target==null || now<nextRecipeEdit)return;nextRecipeEdit=now+2;
            var held=getCarried();
            if(target.garden()){
                if(!npc.assignments.selectGardenSeed(row+1,button==1?ItemStack.EMPTY:held))player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Choose a supported seed or raw crop for this garden."));
                refreshRecipes();broadcastChanges();return;
            }
            return;
        }
        if(stillValid(player)){
            var before=npc==null?null:npc.food.createSnapshot();
            try{super.clicked(slot,button,type,player);}finally{if(npc!=null)npc.garden.playerEditedCargo(before);}
        }
    }
    public PeepoEntity companion(Player player){if(npc!=null)return npc;var entity=player.level().getEntity((data.get(9)&0xffff)|((data.get(10)&0xffff)<<16));return entity instanceof PeepoEntity p?p:null;}
    public int value(int i){return data.get(i);}
    @Override public boolean stillValid(Player player){return npc==null || npc.isAlive() && !npc.isRemoved() && player.isAlive() && npc.level()==player.level() && npc.distanceToSqr(player)<=64 && !player.isSpectator() && npc.orders.allowed(player);}
    @Override public boolean clickMenuButton(Player player,int id){
        if(npc==null || !stillValid(player))return false;
        if(id>=60 && id<=64){
            if(id==64){filterRow=-1;filterTarget=null;}
            else {int row=id-60;var station=recipeStation(row);if(station==null || CompanionFilters.get(station)==null)return false;filterRow=row;filterTarget=recipeTarget(row);}
            refreshRecipes();broadcastChanges();return true;
        }
        if(id>=320 && id<328 && filterRow==4+(id-320)/2){
            var r=npc.assignments.supplies.route((id-320)/2);int old=id%2==0?r.leave:r.keep,next=0;
            for(int n:CompanionSupplies.LIMITS)if(n>old){next=n;break;}
            if(id%2==0)r.leave=next;else r.keep=next;broadcastChanges();return true;
        }
        if(filterRow>=0)return false;
        if(id>=100){
            long now=npc.level().getGameTime();if(now<nextRecipeEdit)return false;nextRecipeEdit=now+2;
            var supplies=npc.assignments.supplies;
            if(id<108)npc.assignments.clear(CompanionAssignments.containerSlot(id>=104,(id-100)%4));
            else if(id>=110 && id<114)supplies.toggleTools(id-110);
            else if(id==114)supplies.toggleHandLock();
            else if(id>=200 && id<240)supplies.link(1+(id-200)/10,(id-200)%10>=5,(id-200)%5);
            else if(id>=300 && id<316){
                int row=(id-300)/4;var r=supplies.route(row);
                switch((id-300)%4){
                    case 0->{r.source=(r.source+1)%4;r.enabled=false;}
                    case 1->{r.destination=(r.destination+1)%4;r.enabled=false;}
                    case 2->{if(!r.enabled && (supplies.endpoint(row,false)==null || supplies.endpoint(row,true)==null))return false;r.enabled=!r.enabled;}
                    case 3->{filterRow=4+row;filterTarget=null;}
                }
            }else return false;
            npc.readiness.clear();refreshRecipes();broadcastChanges();return true;
        }
        boolean changed=npc.orders.command(player,id);if(changed){refreshRecipes();broadcastChanges();}return changed;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        var before=npc==null?null:npc.food.createSnapshot();
        try{return moveCargo(player,index);}finally{if(npc!=null)npc.garden.playerEditedCargo(before);}
    }
    private ItemStack moveCargo(Player player,int index){
        if(!stillValid(player) || index<0 || index>=RECIPE_START)return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<10){if(!moveItemStackTo(stack,10,46,true))return ItemStack.EMPTY;}
        else {
            if(stack.is(net.minecraft.world.item.Items.JACK_O_LANTERN) && !slots.get(8).hasItem())moveItemStackTo(stack,8,9,false);
            if(!stack.isEmpty())moveItemStackTo(stack,0,8,false);
            if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}

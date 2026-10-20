package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Six-button surveying desk. Noise sampling is spread across ticks and never loads/generates chunks. */
public final class CaveWorkshop extends AbstractContainerMenu {
    public static final List<String> CAVES=List.of("magnetic_caves","primordial_caves","toxic_caves","abyssal_chasm","forlorn_hollows","candy_cavity");
    public static final List<String> NAMES=List.of("Magnetic Caves","Primordial Caves","Toxic Caves","Abyssal Chasm","Forlorn Hollows","Candy Cavity");
    public static MenuType<CaveWorkshop> TYPE;
    private static final Set<CaveWorkshop> SEARCHES=new LinkedHashSet<>();
    private final Player owner;
    private final BlockPos desk;
    private final ContainerData data;
    private BlockPos origin;
    private int ring,edge,index;
    private static final int RADIUS=16,STEP=64;
    public CaveWorkshop(int id,Inventory inv){this(id,inv,null,new SimpleContainerData(7));}
    private CaveWorkshop(int id,Inventory inv,BlockPos p,ContainerData data){super(TYPE,id);owner=inv.player;desk=p;this.data=data;addDataSlots(data);}
    static void register(){
        TYPE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("cave_workshop"),new MenuType<>(CaveWorkshop::new,FeatureFlags.VANILLA_SET));
        ServerTickEvents.END_SERVER_TICK.register(server->{int budget=64;for(var menu:List.copyOf(SEARCHES)){
            if(!(menu.owner instanceof ServerPlayer p)||p.level().getServer()!=server)continue;
            if(p.containerMenu!=menu||!menu.stillValid(p)){SEARCHES.remove(menu);continue;}
            for(int n=0;n<8&&budget>0&&menu.status()==1;n++,budget--)menu.sample(p);
            if(menu.status()!=1)SEARCHES.remove(menu);menu.broadcastChanges();if(budget==0)break;
        }});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->SEARCHES.removeIf(m->m.owner instanceof ServerPlayer p&&p.level().getServer()==s));
    }
    static void open(ServerPlayer p,BlockPos desk){p.openMenu(new SimpleMenuProvider((id,inv,who)->new CaveWorkshop(id,inv,desk==null?null:desk.immutable(),new SimpleContainerData(7)),Component.literal("Cave Survey")));}
    public int status(){return data.get(0);}
    public int selected(){return data.get(1);}
    public int targetX(){return (data.get(2)&65535)|(data.get(5)<<16);}
    public int targetZ(){return (data.get(3)&65535)|(data.get(6)<<16);}
    public int progress(){return data.get(4);}
    @Override public boolean stillValid(Player p){return p==owner&&(desk==null?p.getMainHandItem().is(Menagerie.ITEMS.get("cavern_cave_map"))||p.getOffhandItem().is(Menagerie.ITEMS.get("cavern_cave_map"))||p.getMainHandItem().is(Menagerie.ITEMS.get("cavern_underground_cabin_explorer_map"))||p.getOffhandItem().is(Menagerie.ITEMS.get("cavern_underground_cabin_explorer_map")):p.distanceToSqr(desk.getX()+.5,desk.getY()+.5,desk.getZ()+.5)<=64&&p.level().getBlockState(desk).is(Menagerie.BLOCKS.get("cavern_spelunkery_table"))&&Menagerie.mayChange(p.level(),p,desk));}
    @Override public boolean clickMenuButton(Player who,int choice){
        if(!(who instanceof ServerPlayer p)||!stillValid(p)||choice<0||choice>=CAVES.size()||status()==1||!RateGate.allow(p,"cave_survey",40))return false;
        if(!p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)){data.set(0,5);return false;}
        if(SEARCHES.size()>=16){data.set(0,6);return false;}
        data.set(0,1);data.set(1,choice);data.set(4,0);origin=new BlockPos(p.getBlockX(),-16,p.getBlockZ());ring=0;edge=0;index=0;SEARCHES.add(this);return true;
    }
    private void sample(ServerPlayer p){
        int dx=0,dz=0;
        if(ring>0){int width=ring*2;int step=edge%width;switch(edge/width){case 0->{dx=-ring+step;dz=-ring;}case 1->{dx=ring;dz=-ring+step;}case 2->{dx=ring-step;dz=ring;}default->{dx=-ring;dz=ring-step;}}}
        int x=origin.getX()+dx*STEP,z=origin.getZ()+dz*STEP;index++;data.set(4,index*100/1089);
        var biome=p.level().getUncachedNoiseBiome(Math.floorDiv(x,4),-4,Math.floorDiv(z,4));
        if(biome.is(Jugcraft.id("cavern_"+CAVES.get(selected())))){data.set(0,2);data.set(2,x&65535);data.set(5,x>>16);data.set(3,z&65535);data.set(6,z>>16);saveMap(p,x,z);return;}
        if(ring==0){ring=1;edge=0;}else if(++edge>=ring*8){ring++;edge=0;}
        if(ring>RADIUS)data.set(0,3);
    }
    private void saveMap(ServerPlayer p,int x,int z){
        ItemStack map=ItemStack.EMPTY;for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(Menagerie.ITEMS.get("cavern_cave_map"))){map=s;break;}}
        if(map.isEmpty()){
            // A survey is free to consult; a physical map requires paper and a tablet at the desk.
            if(desk==null)return;int paper=-1,tablet=-1;for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(Items.PAPER))paper=i;if(s.is(Menagerie.ITEMS.get("cavern_cave_tablet")))tablet=i;}
            if(paper<0||tablet<0||p.getInventory().getFreeSlot()<0)return;
            p.getInventory().getItem(paper).shrink(1);p.getInventory().getItem(tablet).shrink(1);map=new ItemStack(Menagerie.item("cavern_cave_map"));p.getInventory().add(map);
            // Inventory.add may merge/copy the input; retrieve the actual stored map before writing its data.
            for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Menagerie.item("cavern_cave_map"))){map=p.getInventory().getItem(i);break;}
        }
        var tag=map.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString("Cave",CAVES.get(selected()));tag.putInt("SurveyX",x);tag.putInt("SurveyZ",z);tag.putString("SurveyDimension",p.level().dimension().identifier().toString());map.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        map.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(NAMES.get(selected())),Component.literal("Survey: X "+x+", Y -16, Z "+z),Component.literal("Predicted biome in unexplored terrain; existing chunks may differ."))));p.getInventory().setChanged();
    }
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public void removed(Player p){SEARCHES.remove(this);super.removed(p);}
}

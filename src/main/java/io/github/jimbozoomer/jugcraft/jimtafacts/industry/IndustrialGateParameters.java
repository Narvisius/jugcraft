package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Source modifier capacities, adapted to native extra sensor predicates and output channels. */
public final class IndustrialGateParameters {
    public static final List<String> KINDS=List.of("lapis","quartz","diamond");
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static MenuType<Menu> TYPE;
    public static final int FIELDS=21; // three [enabled, channel, above, threshold] + three [enabled, channel, on]
    private IndustrialGateParameters() {}
    public static void register() {
        TYPE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_gate_parameters"),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
        for(int i=0;i<KINDS.size();i++) {
            int modifier=i+1;String name=KINDS.get(i);
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_gate_modifier_"+name));
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key)) {
                @Override public InteractionResult useOn(UseOnContext context) {
                    if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof IndustrialAutomation.GateEntity gate))return InteractionResult.PASS;
                    if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
                    if(player.isSpectator()||!Authority.mayChange(player.level(),player,context.getClickedPos()))return InteractionResult.FAIL;
                    if(gate.modifier()==0) {
                        gate.installModifier(modifier);
                        if(!player.isCreative())context.getItemInHand().shrink(1);
                    } else if(gate.modifier()!=modifier) {
                        player.sendOverlayMessage(Component.literal("Remove the installed modifier first: sneak-use empty hand after removing any expansion."));return InteractionResult.FAIL;
                    }
                    open(player,gate);return InteractionResult.SUCCESS;
                }
            }));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->ITEMS.values().forEach(out::accept));
    }
    public static void open(ServerPlayer player,IndustrialAutomation.GateEntity gate) {
        player.openMenu(new SimpleMenuProvider((id,inv,p)->new Menu(id,inv,gate),Component.literal("Gate Parameters")));
    }
    public static int conditions(int modifier) {return modifier==3?3:modifier==0?0:1;}
    public static int actions(int modifier) {return modifier==3?3:modifier==2?1:0;}
    public static final class Menu extends AbstractContainerMenu {
        private final IndustrialAutomation.GateEntity gate;
        private final ContainerData data;
        private int row;
        public Menu(int id,Inventory inv){this(id,inv,null);}
        public Menu(int id,Inventory inv,IndustrialAutomation.GateEntity gate) {
            super(TYPE,id);this.gate=gate;
            data=gate==null?new SimpleContainerData(24):new ContainerData(){
                public int get(int i){return i==0?row:i==1?gate.ruleRows():i==2?gate.modifier():gate.parameter(row,i-3);}
                public void set(int i,int v){}
                public int getCount(){return 24;}
            };addDataSlots(data);
        }
        public int row(){return data.get(0);}
        public int rows(){return data.get(1);}
        public int modifier(){return data.get(2);}
        public int value(int field){return data.get(3+field);}
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||gate!=null&&gate.modifier()>0&&!gate.isRemoved()
                &&p.level().getBlockEntity(gate.getBlockPos())==gate&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(gate.getBlockPos()))<=64
                &&p instanceof ServerPlayer sp&&Authority.mayChange(sp.level(),p,gate.getBlockPos());}
        @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
        @Override public boolean clickMenuButton(Player p,int id) {
            if(!(p instanceof ServerPlayer sp)||!stillValid(p)||!RateGate.allow(sp,"jimta_gate_parameters",2))return false;
            row=Math.clamp(row,0,gate.ruleRows()-1);
            if(id==0)row=Math.floorMod(row-1,gate.ruleRows());
            else if(id==1)row=(row+1)%gate.ruleRows();
            else if(id==2){p.openMenu(gate);return true;}
            else if(id>=10&&id<10+FIELDS) {
                int field=id-10;
                if(field<12&&field/4>=conditions(gate.modifier())||field>=12&&(field-12)/3>=actions(gate.modifier()))return false;
                int value=gate.parameter(row,field);
                int max=field<12?(field%4==1?15:field%4==3?100:1):((field-12)%3==1?15:1);
                int step=field<12&&field%4==3?5:1;
                gate.parameter(row,field,(value+step)%(max+step));
            } else return false;
            broadcastChanges();return true;
        }
    }
}

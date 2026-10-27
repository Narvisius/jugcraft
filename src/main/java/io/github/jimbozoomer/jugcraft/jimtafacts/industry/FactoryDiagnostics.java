package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Original diagnostic art with native transaction-aware JE load and bounded per-player inspection. */
public final class FactoryDiagnostics {
    public static Block TESTER;public static Item DEBUGGER;public static BlockEntityType<Node> TYPE;
    private FactoryDiagnostics(){}
    public static void register(){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_power_tester"));TESTER=Registry.register(BuiltInRegistries.BLOCK,key,new Tester(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key)));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(TESTER,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_power_tester"),FabricBlockEntityTypeBuilder.create(Node::new,TESTER).build());
        EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.getLevel()!=null&&n.getLevel().hasNeighborSignal(n.getBlockPos())?null:n.energy,TYPE);
        var debug=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_factory_debugger"));DEBUGGER=Registry.register(BuiltInRegistries.ITEM,debug,new Item(new Item.Properties().setId(debug).stacksTo(1)){
            @Override public InteractionResult useOn(UseOnContext c){
                if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
                if(p.isSpectator()||!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
                var node=p.level().getBlockEntity(c.getClickedPos());String report;
                if(node instanceof Node meter)report=meter.report();
                else if(node instanceof CivilWorkshops.Node workshop){if(!workshop.permitted(p))return InteractionResult.FAIL;report=workshop.kind()+": "+(workshop.running?"running":"paused")+", scan "+workshop.cursor+"/"+workshop.volume()+", "+workshop.energy.getAmount()+" JE";}
                else {var energy=EnergyStorage.SIDED.find(p.level(),c.getClickedPos(),c.getClickedFace());report=BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(c.getClickedPos()).getBlock())+(energy==null?" · no JE port on this face":" · "+energy.getAmount()+"/"+energy.getCapacity()+" JE");}
                p.sendOverlayMessage(Component.literal(report));return InteractionResult.SUCCESS;
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{o.accept(TESTER);o.accept(DEBUGGER);});
    }
    private static final class Tester extends Block implements EntityBlock,EnergyConnectable {
        Tester(Properties p){super(p);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,p,state,n)->((Node)n).tick():null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
            if(l instanceof ServerLevel server){if(!Authority.mayChange(server,p,pos))return InteractionResult.FAIL;if(l.getBlockEntity(pos) instanceof Node n){if(p.isShiftKeyDown()){n.total=0;n.setChanged();}p.sendOverlayMessage(Component.literal(n.report()+" · consumes input; redstone pauses; sneak resets total"));}}return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity {
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(1_000_000,1_000_000,0,this::committed){
            @Override public long insert(long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction){return level!=null&&level.hasNeighborSignal(worldPosition)?0:super.insert(amount,transaction);}
        };
        private long accounted,total,pending,lastTick;private int ticks;
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        private void committed(){long delta=Math.max(0,energy.getAmount()-accounted);accounted=energy.getAmount();total+=Math.min(delta,Long.MAX_VALUE-total);pending+=Math.min(delta,Long.MAX_VALUE-pending);setChanged();}
        public void tick(){lastTick=pending;pending=0;if(level!=null&&!level.hasNeighborSignal(worldPosition)){energy.setAmount(0);accounted=0;}if(++ticks%20==0)setChanged();}
        public long total(){return total;}
        public String report(){return "Load tester: "+lastTick+" JE last tick; "+total+" JE total; "+energy.getAmount()+" JE buffered";}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putLong("Energy",energy.getAmount());out.putLong("Total",total);out.putLong("Pending",pending);out.putLong("LastTick",lastTick);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);energy.setAmount(in.getLongOr("Energy",0));accounted=energy.getAmount();total=Math.max(0,in.getLongOr("Total",0));pending=Math.max(0,in.getLongOr("Pending",0));lastTick=Math.max(0,in.getLongOr("LastTick",0));}
    }
}

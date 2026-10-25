package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Bounded 5x5x5 shell around the existing component reactor. No chunk tickets or remote inventory access. */
public final class ReactorVessel {
    public static Block VESSEL,FLUID_PORT,ACCESS_HATCH,REDSTONE_PORT;
    public static BlockEntityType<Port> TYPE;
    private ReactorVessel(){}
    public static void register(){
        VESSEL=block("vessel",false);FLUID_PORT=block("fluid_port",true);
        ACCESS_HATCH=block("access_hatch",true);REDSTONE_PORT=block("redstone_port",true);
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_reactor_port"),FabricBlockEntityTypeBuilder.create(Port::new,FLUID_PORT,ACCESS_HATCH,REDSTONE_PORT).build());
        FluidStorage.SIDED.registerForBlockEntity((port,side)->{
            var core=port.core();if(core==null||!port.getBlockState().is(FLUID_PORT))return null;
            return side==Direction.DOWN?core.hotCoolant:core.coolant;
        },TYPE);
        ItemStorage.SIDED.registerForBlockEntity((port,side)->{
            var core=port.core();return core!=null&&port.getBlockState().is(ACCESS_HATCH)?ContainerStorage.of(core,side):null;
        },TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{out.accept(VESSEL);out.accept(FLUID_PORT);out.accept(ACCESS_HATCH);out.accept(REDSTONE_PORT);});
    }
    private static Block block(String name,boolean port){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_reactor_"+name));
        var properties=BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).strength(5F,20F);
        Block block=port?new PortBlock(properties):new Block(properties);Registry.register(BuiltInRegistries.BLOCK,key,block);
        var ik=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));return block;
    }
    public record Formation(boolean complete,boolean redstone){}
    public static boolean shell(BlockState state){return state.is(VESSEL)||state.is(FLUID_PORT)||state.is(ACCESS_HATCH)||state.is(REDSTONE_PORT);}
    public static Formation inspect(ServerLevel level,CompactReactor.Core core){
        var actor=Authority.answering(level,core.owner);
        if(actor==null||!Authority.mayChange(level,actor,core.getBlockPos()))return new Formation(false,false);
        boolean redstone=false;int chambers=0,ports=0;
        BlockPos center=core.getBlockPos();
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
            var at=center.offset(x,y,z);if(!level.hasChunkAt(at))return new Formation(false,false);
            var state=level.getBlockState(at);
            if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))==2){
                if(!shell(state)||!Authority.mayChange(level,actor,at)||!ClaimTransfers.allowed(level,center,at))return new Formation(false,false);
                if(state.is(FLUID_PORT))ports++;
                if(state.is(REDSTONE_PORT)&&level.hasNeighborSignal(at))redstone=true;
            }else if(x==0&&y==0&&z==0){
                if(level.getBlockEntity(at)!=core)return new Formation(false,false);
            }else if(Math.abs(x)+Math.abs(y)+Math.abs(z)==1){
                if(!state.is(CompactReactor.CHAMBER)||!Authority.mayChange(level,actor,at)||!ClaimTransfers.allowed(level,center,at))return new Formation(false,false);
                chambers++;
            }else if(!state.isAir())return new Formation(false,false);
        }
        return new Formation(chambers==6&&ports>0,redstone);
    }
    static final class PortBlock extends Block implements EntityBlock {
        PortBlock(Properties p){super(p);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Port(p,s);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(!state.is(FLUID_PORT))return InteractionResult.PASS;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            if(player.isSpectator()||!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
            var port=(Port)level.getBlockEntity(pos);var core=port==null?null:port.core();
            if(core==null||!Authority.mayChange(level,player,core.getBlockPos()))return InteractionResult.FAIL;
            FluidStorageUtil.interactWithFluidStorage(hit.getDirection()==Direction.DOWN?core.hotCoolant:core.coolant,player,hand);
            return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(level.isClientSide())return InteractionResult.SUCCESS;
            if(player.isSpectator()||!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
            var port=(Port)level.getBlockEntity(pos);var core=port==null?null:port.core();
            if(core==null){player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Requires a complete 5×5×5 vessel, six core chambers and an owned reactor."));return InteractionResult.SUCCESS;}
            if(!Authority.mayChange(level,player,core.getBlockPos()))return InteractionResult.FAIL;
            if(state.is(ACCESS_HATCH))player.openMenu(core);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Coolant "+core.coolant.millibuckets()+" / 16000 mB · hot coolant "+core.hotCoolant.millibuckets()+" / 16000 mB · bottom: hot output"));
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Port extends BlockEntity {
        public Port(BlockPos p,BlockState s){super(TYPE,p,s);}
        public CompactReactor.Core core(){
            if(!(level instanceof ServerLevel server))return null;
            CompactReactor.Core found=null;
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
                if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=2)continue;
                var at=worldPosition.offset(x,y,z);
                if(server.hasChunkAt(at)&&server.getBlockEntity(at) instanceof CompactReactor.Core candidate&&inspect(server,candidate).complete()){
                    if(found!=null)return null;found=candidate;
                }
            }return found;
        }
    }
}

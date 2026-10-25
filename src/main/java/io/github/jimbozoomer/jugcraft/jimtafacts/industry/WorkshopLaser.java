package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import java.util.UUID;

/** Original laser emitter, native JE delivery to workshop tables along a loaded, claim-safe 16-block ray. */
public final class WorkshopLaser {
    public static Block BLOCK;public static BlockEntityType<Node> TYPE;
    public static final EnumProperty<Direction> FACING=DirectionalBlock.FACING;
    private WorkshopLaser(){}
    public static void register(){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_workshop_laser"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Emitter(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion()));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(BLOCK,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,key.identifier(),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.input,TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->out.accept(BLOCK));
    }
    static final class Emitter extends Block implements EntityBlock,EnergyConnectable {
        Emitter(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){
            return switch(s.getValue(FACING)){
                case UP->net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,0,0,16,4,16),Block.box(5,4,5,11,13,11));
                case DOWN->net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,12,0,16,16,16),Block.box(5,3,5,11,12,11));
                case NORTH->net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,0,12,16,16,16),Block.box(5,5,3,11,11,12));
                case SOUTH->net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,0,0,16,16,4),Block.box(5,5,4,11,11,13));
                case EAST->net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,0,0,4,16,16),Block.box(4,5,5,13,11,11));
                case WEST->net.minecraft.world.phys.shapes.Shapes.or(Block.box(12,0,0,16,16,16),Block.box(3,5,5,12,11,11));
            };
        }
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,p,state,e)->((Node)e).step((ServerLevel)world):null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){if(placer!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=placer.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player p,BlockHitResult hit){
            if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof Node n&&Authority.mayChange(level,p,pos))p.sendOverlayMessage(Component.literal(n.energy.getAmount()+" / 40000 JE · aim at an assembly, integration, charging, programming or advanced crafting table within 16 blocks · redstone pauses"));return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity {
        public UUID owner;public int beamDistance;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(40000,256,128,this::setChanged);
        private final EnergyStorage input=new EnergyStorage(){
            public boolean supportsInsertion(){return true;}public long insert(long n,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return energy.insert(n,tx);}
            public boolean supportsExtraction(){return false;}public long extract(long n,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return 0;}
            public long getAmount(){return energy.getAmount();}public long getCapacity(){return energy.getCapacity();}public Object transferIdentity(){return energy;}
        };
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public void step(ServerLevel level){
            int distance=0;var actor=Authority.answering(level,owner);
            if(actor!=null&&energy.getAmount()>0&&!level.hasNeighborSignal(worldPosition)&&Authority.mayChange(level,actor,worldPosition)){
                Direction facing=getBlockState().getValue(FACING);
                for(int i=1;i<=16;i++){
                    var at=worldPosition.relative(facing,i);
                    if(!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at)||!ClaimTransfers.allowed(level,worldPosition,at))break;
                    var state=level.getBlockState(at);var block=state.getBlock();
                    if(block==JimtaIndustry.MACHINES.get("jimta_assembly_table")||block==JimtaIndustry.MACHINES.get("jimta_integration_table")
                            ||block==JimtaIndustry.MACHINES.get("jimta_advanced_crafting_table")||WorkshopTables.BLOCKS.containsValue(block)){
                        var receiver=EnergyStorage.SIDED.find(level,at,facing.getOpposite());
                        if(receiver!=null&&receiver.transferIdentity()!=energy)try(var tx=Transaction.openOuter()){
                            long moved=receiver.insert(Math.min(128,energy.getAmount()),tx);
                            if(moved>0&&energy.extract(moved,tx)==moved){tx.commit();distance=i;}
                        }
                        break;
                    }
                    if(!state.getCollisionShape(level,at).isEmpty())break;
                }
            }
            if(distance!=beamDistance){beamDistance=distance;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
        }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("render_only",true);tag.putInt("beam",beamDistance);return tag;}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putLong("energy",energy.getAmount());}
        @Override protected void loadAdditional(ValueInput in){if(in.getBooleanOr("render_only",false)){beamDistance=Math.clamp(in.getIntOr("beam",0),0,16);return;}super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);energy.setAmount(in.getLongOr("energy",0));beamDistance=0;}
    }
}

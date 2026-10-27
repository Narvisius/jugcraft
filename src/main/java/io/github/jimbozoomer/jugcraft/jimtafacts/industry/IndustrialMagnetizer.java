package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.UUID;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Paid, owner-authorized fence elevator; never teleports riders or forces chunks. */
public final class IndustrialMagnetizer {
    public static final net.minecraft.tags.TagKey<Item> BOOTS=net.minecraft.tags.TagKey.create(Registries.ITEM,Jugcraft.id("magnetizer_boots"));
    private static final java.util.Map<ServerPlayer,Long> LAST_LIFT=new java.util.WeakHashMap<>();
    public static Block BLOCK;
    public static BlockEntityType<Node> TYPE;
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    private IndustrialMagnetizer() {}
    public static void register() {
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_magnetizer"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Housing(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key)));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());
        Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(BLOCK,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,key.identifier(),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        EnergyStorage.SIDED.registerForBlockEntity((node,side)->side==node.getBlockState().getValue(FACING)?null:node.energy,TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o->o.accept(BLOCK));
    }
    private static final class Housing extends Block implements EntityBlock,EnergyConnectable {
        Housing(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack){if(entity instanceof ServerPlayer player&&level.getBlockEntity(pos) instanceof Node n){n.owner=player.getUUID();n.setChanged();}}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,pos,state,node)->((Node)node).tick():null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof Node n&&n.permitted(p))p.sendOverlayMessage(Component.literal("Magnetizer: "+n.energy.getAmount()+" JE · front iron fence, 20 blocks up/down · metal boots lift; sneak descends · redstone pauses"));return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BlockEntity {
        public UUID owner;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(16000,128,0,this::setChanged);
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public boolean permitted(ServerPlayer player){return owner!=null&&player.level()==level&&player.isAlive()&&!player.isSpectator()&&(owner.equals(player.getUUID())||JugcraftParties.sameParty(owner,player.getUUID()))&&Authority.mayChange(level,player,worldPosition);}
        private boolean fence(ServerLevel level,ServerPlayer player,BlockPos pos){return level.hasChunkAt(pos)&&level.getBlockState(pos).is(IndustrialConstruction.IRON_FENCE)&&ClaimTransfers.allowed(level,worldPosition,pos)&&Authority.mayChange(level,player,pos);}
        public void tick(){
            if(!(level instanceof ServerLevel server)||server.hasNeighborSignal(worldPosition)||energy.getAmount()<4)return;
            var actor=Authority.present(server,owner);if(actor==null||!permitted(actor))return;
            var base=worldPosition.relative(getBlockState().getValue(FACING));
            if(!fence(server,actor,base))return;
            int bottom=base.getY(),top=bottom;
            for(int i=1;i<=20&&fence(server,actor,base.above(i));i++)top=base.getY()+i;
            for(int i=1;i<=20&&fence(server,actor,base.below(i));i++)bottom=base.getY()-i;
            var bounds=new AABB(base.getX()-.7,bottom,base.getZ()-.7,base.getX()+1.7,top+1,base.getZ()+1.7);
            int moved=0;
            var riders=new java.util.ArrayList<ServerPlayer>();server.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),bounds,p->p.isAlive()&&!p.isSpectator(),riders,8);
            for(var rider:riders){
                if(moved>=8||energy.getAmount()<4)break;
                if(LAST_LIFT.getOrDefault(rider,Long.MIN_VALUE)==server.getGameTime()||!ClaimTransfers.allowed(server,worldPosition,rider.blockPosition())||!permitted(rider)||rider.isPassenger()||rider.getAbilities().flying||!Authority.mayChange(server,rider,rider.blockPosition()))continue;
                var boots=rider.getItemBySlot(EquipmentSlot.FEET);
                if(!boots.is(BOOTS))continue;
                double speed=rider.isShiftKeyDown()?-.12:.22;
                if(!server.noCollision(rider,rider.getBoundingBox().move(0,speed,0)))continue;
                var motion=rider.getDeltaMovement();rider.setDeltaMovement(motion.x,speed,motion.z);
                rider.resetFallDistance();rider.connection.send(new ClientboundSetEntityMotionPacket(rider));
                energy.setAmount(energy.getAmount()-4);LAST_LIFT.put(rider,server.getGameTime());moved++;
            }
        }
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);out.putLong("Energy",energy.getAmount());}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);energy.setAmount(in.getLongOr("Energy",0));}
    }
}

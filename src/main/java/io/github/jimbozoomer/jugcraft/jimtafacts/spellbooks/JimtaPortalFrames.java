package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.shapes.*;

/** Original two-block frames: paid-once binding, saved reciprocal identities, dye and explicit unlinking. */
public final class JimtaPortalFrames {
    public static Frame BLOCK;public static Item ITEM;public static BlockEntityType<Node> NODE;
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    private record Pending(GlobalPos pos,UUID identity){}
    private static final Map<ServerPlayer,Pending> FIRST=new WeakHashMap<>();
    public static void register(){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_spellbooks_portal_frame"));BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Frame(BlockBehaviour.Properties.of().setId(key).strength(10,6).noOcclusion().sound(SoundType.COPPER_GRATE).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());ITEM=Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(BLOCK,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        NODE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimtafacts_portal_frame"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(ITEM));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->FIRST.clear());
    }
    public static Node at(Level level,BlockPos pos){var state=level.getBlockState(pos);if(!state.is(BLOCK))return null;if(state.getValue(HALF)==DoubleBlockHalf.UPPER)pos=pos.below();return level.getBlockEntity(pos) instanceof Node node?node:null;}
    private static Node aimed(ServerPlayer p){var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(48)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));return hit.getType()==HitResult.Type.BLOCK?at(p.level(),hit.getBlockPos()):null;}
    public static boolean handles(ServerPlayer p){if(FIRST.containsKey(p)&&JimtaRecasts.isPending(p,"portal"))return true;FIRST.remove(p);return !JimtaRecasts.isPending(p,"portal")&&aimed(p)!=null;}
    public static boolean cast(ServerPlayer p){
        if(p.level().dimension().equals(JimtaPocketRooms.DIMENSION))return false;
        var node=aimed(p);if(node==null||!node.permitted(p)||node.link!=null)return false;
        if(!JimtaRecasts.isPending(p,"portal")){FIRST.put(p,new Pending(GlobalPos.of(p.level().dimension(),node.getBlockPos()),node.identity));JimtaRecasts.begin(p,"portal",2,2400);p.sendOverlayMessage(Component.literal("First frame selected. Cast Portal at another frame within two minutes."));return true;}
        var pending=FIRST.get(p);if(pending==null)return false;var firstLevel=p.level().getServer().getLevel(pending.pos.dimension());
        if(firstLevel==null||!firstLevel.hasChunkAt(pending.pos.pos()))return false;var first=at(firstLevel,pending.pos.pos());
        if(first==null||first==node||!first.identity.equals(pending.identity)||first.link!=null||!first.permitted(p))return false;
        first.link=GlobalPos.of(p.level().dimension(),node.getBlockPos());first.partner=node.identity;node.link=pending.pos;node.partner=first.identity;first.changed();node.changed();JimtaRecasts.consume(p,"portal");FIRST.remove(p);return true;
    }
    public static final class Node extends BlockEntity {
        private UUID owner,identity=UUID.randomUUID(),partner;private GlobalPos link;private int colour=-1;private boolean visibleConnected;
        Node(BlockPos pos,BlockState state){super(NODE,pos,state);}
        public int colour(){return colour;}public boolean connected(){return visibleConnected;}
        boolean permitted(ServerPlayer p){return owner!=null&&(owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID()))&&level instanceof ServerLevel l&&Authority.mayChange(l,p,worldPosition)&&Authority.mayChange(l,p,worldPosition.above());}
        private Node partner(){if(!(level instanceof ServerLevel l)||link==null||partner==null)return null;var target=l.getServer().getLevel(link.dimension());if(target==null||!target.hasChunkAt(link.pos()))return null;var node=at(target,link.pos());return node!=null&&node.identity.equals(partner)&&identity.equals(node.partner)&&GlobalPos.of(l.dimension(),worldPosition).equals(node.link)?node:null;}
        public void unlink(){var other=partner();link=null;partner=null;changed();if(other!=null){other.link=null;other.partner=null;other.changed();}}
        private void changed(){visibleConnected=link!=null;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
        private void tick(){
            if(!(level instanceof ServerLevel l)||getBlockState().getValue(HALF)!=DoubleBlockHalf.LOWER||l.getGameTime()%2!=0||!JimtaSpellbooks.enabled())return;
            var node=partner();boolean connected=node!=null;if(visibleConnected!=connected){visibleConnected=connected;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
            if(node==null||!(node.level instanceof ServerLevel target))return;
            var facing=node.getBlockState().getValue(FACING);Vec3 center=Vec3.atBottomCenterOf(node.worldPosition),exit=center.add(facing.getStepX()*.8,0,facing.getStepZ()*.8);
            for(var p:l.getEntitiesOfClass(ServerPlayer.class,new AABB(worldPosition).expandTowards(0,1,0),p->p.isAlive()&&!p.isPassenger()).stream().limit(16).toList()){
                if(JimtaWardEntity.travelCooling(p)||!Authority.mayChange(l,p,worldPosition)||!JimtaWardSpells.safe(p,target,exit))continue;
                JimtaWardEntity.travelCooldown(p);if(p.teleportTo(target,exit.x,exit.y,exit.z,Set.of(),facing.toYRot(),p.getXRot(),true)){p.resetFallDistance();JimtaSpellbookCurios.afterTeleport(p);}
            }
        }
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("identity",UUIDUtil.CODEC,identity);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(partner!=null)out.store("partner",UUIDUtil.CODEC,partner);if(link!=null)out.store("link",GlobalPos.CODEC,link);out.putInt("colour",colour);}
        @Override protected void loadAdditional(ValueInput in){if(in.getBooleanOr("visual_only",false)){visibleConnected=in.getBooleanOr("connected",false);colour=in.getIntOr("colour",-1);return;}super.loadAdditional(in);identity=in.read("identity",UUIDUtil.CODEC).orElseGet(UUID::randomUUID);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);partner=in.read("partner",UUIDUtil.CODEC).orElse(null);link=in.read("link",GlobalPos.CODEC).orElse(null);colour=in.getIntOr("colour",-1);visibleConnected=link!=null;}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries){var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,registries);out.putBoolean("visual_only",true);out.putBoolean("connected",visibleConnected);out.putInt("colour",colour);return out.buildResult();}
        @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    }
    public static class Frame extends Block implements EntityBlock {
        Frame(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,HALF);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var at=c.getClickedPos();if(at.getY()>=c.getLevel().getMaxY()||!c.getLevel().getBlockState(at.above()).canBeReplaced(c)||!c.getLevel().getWorldBorder().isWithinBounds(at.above())||c.getPlayer() instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,at.above()))return null;return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){super.setPlacedBy(l,p,s,actor,stack);if(l.isClientSide())return;l.setBlock(p.above(),s.setValue(HALF,DoubleBlockHalf.UPPER),Block.UPDATE_ALL);if(l.getBlockEntity(p) instanceof Node n&&actor!=null){n.owner=actor.getUUID();n.changed();}}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos pos,Direction dir,BlockPos other,BlockState neighbor,RandomSource random){if(dir==(s.getValue(HALF)==DoubleBlockHalf.LOWER?Direction.UP:Direction.DOWN)&&(!neighbor.is(this)||neighbor.getValue(HALF)==s.getValue(HALF)))return Blocks.AIR.defaultBlockState();return super.updateShape(s,l,ticks,pos,dir,other,neighbor,random);}
        @Override public BlockState playerWillDestroy(Level l,BlockPos pos,BlockState s,Player p){var node=at(l,pos);if(node!=null)node.unlink();if(s.getValue(HALF)==DoubleBlockHalf.UPPER){if(!p.isCreative()&&l instanceof ServerLevel server&&l.getBlockState(pos.below()).is(this))Block.dropResources(l.getBlockState(pos.below()),server,pos.below(),l.getBlockEntity(pos.below()),p,p.getMainHandItem());l.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}return super.playerWillDestroy(l,pos,s,p);}
        @Override protected RenderShape getRenderShape(BlockState s){return s.getValue(HALF)==DoubleBlockHalf.UPPER?RenderShape.INVISIBLE:RenderShape.MODEL;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide()&&t==NODE?(world,pos,state,be)->((Node)be).tick():null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player actor,BlockHitResult hit){if(actor instanceof ServerPlayer p&&p.isShiftKeyDown()){var node=at(l,pos);if(node!=null&&node.permitted(p)){node.unlink();p.sendOverlayMessage(Component.literal("Portal frame disconnected."));return InteractionResult.SUCCESS;}}return InteractionResult.PASS;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player actor,InteractionHand hand,BlockHitResult hit){if(stack.getItem() instanceof DyeItem dye){if(actor instanceof ServerPlayer p){var n=at(l,pos);if(n!=null&&n.permitted(p)&&n.link!=null&&n.colour!=dye.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextureDiffuseColor()){n.colour=dye.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextureDiffuseColor();n.changed();stack.consume(1,p);}}return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return slab(s.getValue(FACING));}
        @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){Direction f=s.getValue(FACING);boolean x=f.getAxis()==Direction.Axis.X;double min=(f==Direction.NORTH||f==Direction.WEST)?13:0,max=min+3;var hole=x?Block.box(min,0,1,max,16,15):Block.box(1,0,min,15,16,max);var sides=Shapes.join(slab(f),hole,BooleanOp.ONLY_FIRST);if(s.getValue(HALF)==DoubleBlockHalf.UPPER)sides=Shapes.or(sides,x?Block.box(min,15,0,max,16,16):Block.box(0,15,min,16,16,max));return sides;}
        private static VoxelShape slab(Direction f){return switch(f){case NORTH->Block.box(0,0,13,16,16,16);case SOUTH->Block.box(0,0,0,16,16,3);case WEST->Block.box(13,0,0,16,16,16);default->Block.box(0,0,0,3,16,16);};}
    }
}

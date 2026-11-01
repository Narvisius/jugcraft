package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Original 5 essence / 5 tick transfer, with capacity-safe accounting and bounded loaded-block discovery. */
public final class ArcaneInjector extends BlockEntity {
    private UUID owner;
    public long activated;
    private int cursor;
    private BlockPos forge,bestForge;
    private final BlockPos[] jars=new BlockPos[4],bestJars=new BlockPos[4];
    public ArcaneInjector(BlockPos pos,BlockState state){super(ForbiddenArcane.INJECTOR,pos,state);}
    private static boolean closer(BlockPos origin,BlockPos candidate,BlockPos old){return old==null||origin.distSqr(candidate)<origin.distSqr(old);}
    public static void tick(Level raw,BlockPos pos,BlockState state,ArcaneInjector q){
        if(!(raw instanceof ServerLevel l)||!state.getValue(InjectorBlock.ENABLED))return;var actor=Authority.present(l,q.owner);if(actor==null||!Authority.mayChange(l,actor,pos))return;
        // A complete 17-cube search spans 39 ticks instead of loading/scanning it in one tick.
        for(int n=0;n<128;n++){
            int index=q.cursor++;var at=pos.offset(index%17-8,index/289-8,index/17%17-8);
            if(l.hasChunkAt(at)&&Authority.mayChange(l,actor,at)&&ClaimTransfers.allowed(l,pos,at)){
                var be=l.getBlockEntity(at);
                if(be instanceof ArcaneWorkshop w&&w.forge()&&pos.distSqr(at)<=16&&w.acceptsSource(pos)&&closer(pos,at,q.bestForge))q.bestForge=at;
                else if(be instanceof ArcaneJar jar&&jar.storedAmount()>0&&pos.distSqr(at)<=64&&closer(pos,at,q.bestJars[jar.storedKind()]))q.bestJars[jar.storedKind()]=at;
            }
            if(q.cursor>=4913){q.cursor=0;q.forge=q.bestForge;q.bestForge=null;System.arraycopy(q.bestJars,0,q.jars,0,4);java.util.Arrays.fill(q.bestJars,null);break;}
        }
        if(l.getGameTime()%5!=0||q.forge==null||!l.hasChunkAt(q.forge)||!(l.getBlockEntity(q.forge) instanceof ArcaneWorkshop w)||!w.forge()||!w.acceptsSource(pos))return;
        for(int type=0;type<4;type++){
            var at=q.jars[type];if(at==null||!l.hasChunkAt(at)||!(l.getBlockEntity(at) instanceof ArcaneJar jar)||jar.storedKind()!=type||jar.storedAmount()==0||!Authority.mayChange(l,actor,at)||!ClaimTransfers.allowed(l,at,q.forge)||!w.acceptsSource(at))continue;
            int accepted=Math.min(5,Math.min(jar.storedAmount(),ArcaneWorkshop.capacity(w.tier(),type)-w.essence(type)));if(accepted<=0)continue;
            // These two changes are synchronous and have no external callbacks between debit and credit.
            jar.take(accepted);w.addEssence(type,accepted);
            if(l.getGameTime()%10==0){var from=net.minecraft.world.phys.Vec3.atCenterOf(at);var to=net.minecraft.world.phys.Vec3.atCenterOf(q.forge);for(int i=0;i<=12;i++){double t=i/12D;var point=from.lerp(to,t).add(0,Math.sin(t*Math.PI)*.75,0);l.sendParticles(type==1?ParticleTypes.SOUL:ParticleTypes.ENCHANT,point.x,point.y,point.z,1,0,0,0,0);}}
            break;
        }
    }
    private void toggle(ServerPlayer p){owner=p.getUUID();activated=p.level().getGameTime();p.level().setBlockAndUpdate(worldPosition,getBlockState().cycle(InjectorBlock.ENABLED));setChanged();p.level().sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putLong("activated",activated);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);activated=in.getLongOr("activated",0);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public static final class InjectorBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
        public static final BooleanProperty ENABLED=BlockStateProperties.ENABLED,WATERLOGGED=BlockStateProperties.WATERLOGGED;
        public InjectorBlock(Properties p){super(p.noOcclusion());registerDefaultState(defaultBlockState().setValue(ENABLED,false).setValue(WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ENABLED,WATERLOGGED);}
        @Override protected RenderShape getRenderShape(BlockState s){return s.getValue(ENABLED)?RenderShape.INVISIBLE:RenderShape.MODEL;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArcaneInjector(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,ForbiddenArcane.INJECTOR,ArcaneInjector::tick);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));}
        @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos q,BlockState n,RandomSource r){if(s.getValue(WATERLOGGED))t.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return s;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){super.setPlacedBy(l,p,s,e,stack);if(e instanceof ServerPlayer actor&&l.getBlockEntity(p) instanceof ArcaneInjector q){q.owner=actor.getUUID();q.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(player instanceof ServerPlayer actor){if(!Authority.mayChange(actor.level(),actor,p))return InteractionResult.FAIL;if(l.getBlockEntity(p) instanceof ArcaneInjector q)q.toggle(actor);}return InteractionResult.SUCCESS;}
    }
}

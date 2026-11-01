package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.block.BonemealSource;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

/** Supplied small devices use native placement, persistence and the shared claim boundary. */
public final class BotanicalOddities {
    private BotanicalOddities(){}
    public static final class PetalBlock extends FlowerBlock implements BonemealableBlock {
        private final String name;
        PetalBlock(String name,Properties p){super(net.minecraft.world.effect.MobEffects.REGENERATION,5,p);this.name=name;}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,1.6,16);}
        @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,BonemealSource source){return l.isEmptyBlock(p.above());}
        @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){return true;}
        @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){
            var tall=Jugtania.block(name.replace("_buried_petals","_double_flower"));
            if(l.isEmptyBlock(p.above())&&tall instanceof DoublePlantBlock)DoublePlantBlock.placeAt(l,tall.defaultBlockState(),p,3);
        }
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
            if(r.nextInt(4)==0){int index=Jugtania.COLORS.indexOf(name.replace("_buried_petals",""));int color=net.minecraft.world.item.DyeColor.byId(Math.max(0,index)).getTextureDiffuseColor();l.addParticle(new DustParticleOptions(color,.4F),p.getX()+r.nextDouble(),p.getY()+.13,p.getZ()+r.nextDouble(),0,.01,0);}
        }
    }
    public static final class FelPumpkin extends HorizontalDirectionalBlock {
        FelPumpkin(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public void setPlacedBy(Level raw,BlockPos at,BlockState state,LivingEntity placer,ItemStack stack){
            super.setPlacedBy(raw,at,state,placer,stack);
            if(!(raw instanceof ServerLevel l)||!(placer instanceof ServerPlayer p))return;
            for(int y=0;y<3;y++){var pos=at.below(y);if(!l.hasChunkAt(pos)||!Authority.mayChange(l,p,pos)||(y>0&&!l.getBlockState(pos).is(Blocks.IRON_BARS)))return;}
            var blaze=net.minecraft.world.entity.EntityTypes.BLAZE.create(l,EntitySpawnReason.TRIGGERED);if(blaze==null)return;
            blaze.setPos(at.getX()+.5,at.getY()-1.95,at.getZ()+.5);blaze.setPersistenceRequired();blaze.addTag("jugcraft_fel_blaze");
            // Only after the entity is admitted do we consume the three verified ritual blocks.
            if(!l.addFreshEntity(blaze))return;
            for(int y=0;y<3;y++){var pos=at.below(y);l.levelEvent(2001,pos,Block.getId(l.getBlockState(pos)));l.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}
            l.sendParticles(ParticleTypes.FLAME,blaze.getX(),blaze.getY()+1,blaze.getZ(),60,.5,1,.5,.03);
        }
    }
    public static final class Starfield extends BotanicalBlock {
        Starfield(Properties p){super("starfield",p);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,4,16);}
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
            long time=Math.floorMod(l.getDefaultClockTime(),24000);if(time<13000||time>23000)return;
            for(int i=0;i<2;i++){
                double x=p.getX()+.5+(r.nextDouble()-.5)*128,z=p.getZ()+.5+(r.nextDouble()-.5)*128;
                l.addParticle(ParticleTypes.END_ROD,x,Math.min(l.getMaxY()-2,p.getY()+96),z,0,-.005,0);
            }
        }
    }
    public static final class Launcher extends BotanicalBlock {
        Launcher(Properties p){super("light_launcher",p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED,false).setValue(BlockStateProperties.WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.POWERED,BlockStateProperties.WATERLOGGED);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(net.minecraft.world.level.material.Fluids.WATER));}
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos at,Direction direction,BlockPos neighbor,BlockState n,RandomSource r){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(at,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,at,direction,neighbor,n,r);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,4,16);}
    }
    static void launch(BotanicalWorkshop host,ServerLevel l,ServerPlayer actor){
        boolean powered=l.hasNeighborSignal(host.getBlockPos()),old=host.getBlockState().getValue(BlockStateProperties.POWERED);
        if(powered==old)return;l.setBlockAndUpdate(host.getBlockPos(),host.getBlockState().setValue(BlockStateProperties.POWERED,powered));if(!powered)return;
        var relays=new ArrayList<BotanicalWorkshop>();for(var direction:Direction.values()){var at=host.getBlockPos().relative(direction);if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof BotanicalWorkshop relay&&BotanicalLuminizer.TYPES.contains(relay.kind())&&relay.luminizer.next(actor)!=null)relays.add(relay);}
        if(relays.isEmpty())return;int count=0;
        for(var rider:l.getEntities((Entity)null,new AABB(host.getBlockPos()).expandTowards(0,1,0),e->(e instanceof LivingEntity||e instanceof ItemEntity)&&e.isAlive()&&!e.isSpectator()&&!e.isPassenger()&&!e.isVehicle())){
            if(++count>16)break;
            // Players authorize their own route; a device never carries them using its owner's permissions.
            var permission=rider instanceof ServerPlayer p?p:actor;
            if(rider instanceof net.minecraft.world.entity.TamableAnimal pet&&pet.getOwnerReference()!=null&&!pet.getOwnerReference().matches(actor))continue;
            if(!Authority.mayChange(l,permission,rider.blockPosition()))continue;
            var relay=relays.get(l.getRandom().nextInt(relays.size()));relay.luminizer.ride(rider,permission);
        }
    }
}

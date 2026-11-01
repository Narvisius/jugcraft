package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

/** Source horn/drum harvesting, bounded and permission checked at each affected block/animal. */
final class BotanicalHorns {
    private static final String[] TAGS={"wild","canopy","covering"};
    private BotanicalHorns(){}
    static void harvest(ServerLevel level,ServerPlayer actor,BlockPos origin,int type){
        int horizontal=12-type*3,vertical=3+type*4,limit=32+type*16;
        var tag=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_horn_of_the_"+TAGS[type]+"_breakable"));var found=new ArrayList<BlockPos>();
        // Bounded source-sized volume, no forced loads; excludes all inventory/functional flower block entities.
        for(var pos:BlockPos.betweenClosed(origin.offset(-horizontal,-vertical,-horizontal),origin.offset(horizontal,vertical,horizontal))){
            if(!level.hasChunkAt(pos)||BotanicalFields.mufflers(level,Vec3.atCenterOf(pos))>0)continue;var state=level.getBlockState(pos);
            boolean allowed=state.is(tag)||type==0&&state.getBlock() instanceof VegetationBlock;
            if(!allowed||state.hasBlockEntity()||type==1&&state.hasProperty(BlockStateProperties.PERSISTENT)&&state.getValue(BlockStateProperties.PERSISTENT)||!Authority.mayChange(level,actor,pos)||!ClaimTransfers.allowed(level,origin,pos))continue;found.add(pos.immutable());
        }
        // Random ordering prevents repeated use stripping only one edge of the source-sized area.
        for(int i=found.size()-1;i>0;i--){int j=level.getRandom().nextInt(i+1);var swap=found.get(i);found.set(i,found.get(j));found.set(j,swap);}
        for(int i=0;i<Math.min(limit,found.size());i++)level.destroyBlock(found.get(i),true,actor);
    }
    static void drum(BotanicalWorkshop drum,ServerPlayer actor){
        var level=actor.level();var origin=drum.getBlockPos();
        if(drum.kind().equals("drum_gathering"))gather(level,actor,origin);else harvest(level,actor,origin,drum.kind().equals("drum_canopy")?1:0);
        level.playSound(null,origin,BotanicalSounds.get("drum"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
    }
    private static void gather(ServerLevel level,ServerPlayer actor,BlockPos origin){
        int sheared=0,seen=0;int[] budget={64};var shears=new ItemStack(Jugtania.item("manasteel_shears"));
        for(var mob:level.getEntitiesOfClass(Mob.class,new AABB(origin).inflate(10))){
            if(++seen>128)break;if(!mob.isAlive()||mob.isBaby()||!Authority.mayChange(level,actor,mob.blockPosition())||!Authority.mayStrike(actor,mob)||!ClaimTransfers.allowed(level,origin,mob.blockPosition())||BotanicalFields.mufflers(level,mob.position())>0)continue;
            if(mob instanceof Chicken chicken&&!chicken.isChickenJockey()){if(chicken.eggTime>600)chicken.eggTime=Math.max(600,chicken.eggTime/2);else if(chicken.eggTime>1&&chicken.eggTime<200)chicken.eggTime=1;}
            if(mob.getType().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jugtania_drum/milkable"))))convert(level,actor,origin,mob,Items.BUCKET,Items.MILK_BUCKET,budget);
            if(mob instanceof MushroomCow cow){
                var access=(io.github.jimbozoomer.jugcraft.mixin.BotanicalMooshroomAccess)cow;
                var effect=access.jugcraft$stew();
                if(effect!=null&&budget[0]>0){var bowls=level.getEntitiesOfClass(ItemEntity.class,cow.getBoundingBox(),d->d.isAlive()&&d.getItem().is(Items.BOWL)&&Authority.mayChange(level,actor,d.blockPosition()));
                    if(!bowls.isEmpty()){var bowl=bowls.get(0);var stew=new ItemStack(Items.SUSPICIOUS_STEW);stew.set(net.minecraft.core.component.DataComponents.SUSPICIOUS_STEW_EFFECTS,effect);if(level.addFreshEntity(new ItemEntity(level,cow.getX(),cow.getY()+1,cow.getZ(),stew))){bowl.getItem().shrink(1);if(bowl.getItem().isEmpty())bowl.discard();access.jugcraft$stew(null);budget[0]--;}}
                }convert(level,actor,origin,cow,Items.BOWL,Items.MUSHROOM_STEW,budget);
            }
            // Mooshrooms are milked, never converted into ordinary cows by automatic shearing.
            if(sheared<4&&mob instanceof Shearable target&&!mob.getType().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jugtania_drum/no_shearing")))&&target.readyForShearing()){target.shear(level,net.minecraft.sounds.SoundSource.BLOCKS,shears);sheared++;}
        }
    }
    private static void convert(ServerLevel level,ServerPlayer actor,BlockPos origin,Mob mob,Item input,Item output,int[] budget){
        for(var drop:level.getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox(),d->d.isAlive()&&d.getItem().is(input))){
            if(budget[0]<=0)break;if(!Authority.mayChange(level,actor,drop.blockPosition())||!ClaimTransfers.allowed(level,origin,drop.blockPosition()))continue;
            while(!drop.getItem().isEmpty()&&budget[0]-->0){if(!level.addFreshEntity(new ItemEntity(level,mob.getX(),mob.getY()+1,mob.getZ(),new ItemStack(output))))break;drop.getItem().shrink(1);}if(drop.getItem().isEmpty())drop.discard();
        }
    }
    static final class Drum extends BotanicalBlock implements SimpleWaterloggedBlock {
        Drum(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,net.minecraft.world.level.block.state.BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.WATERLOGGED);}
        @Override public net.minecraft.world.level.block.state.BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);}
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.world.level.block.state.BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected net.minecraft.world.level.block.state.BlockState updateShape(net.minecraft.world.level.block.state.BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighbor,net.minecraft.world.level.block.state.BlockState other,net.minecraft.util.RandomSource random){if(state.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));return super.updateShape(state,level,ticks,pos,side,neighbor,other,random);}
    }
    static final class Horn extends Item {
        private final int type;Horn(int type,Properties p){super(p.stacksTo(1));this.type=type;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.TOOT_HORN;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){if(entity instanceof ServerPlayer actor&&remaining%5==0&&remaining<72000){harvest(actor.level(),actor,actor.blockPosition(),type);if(remaining%20==0)actor.level().playSound(null,actor.blockPosition(),BotanicalSounds.get("horn_doot"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}}
    }
}

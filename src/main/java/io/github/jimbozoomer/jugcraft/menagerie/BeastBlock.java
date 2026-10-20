package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyPatternMenu;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;

/** Native incubators, nectar feeders and working props using the supplied state/model set. */
class BeastBlock extends HouseholdBlock {
    static final class WithEntity extends BeastBlock implements EntityBlock { WithEntity(Properties p,JsonObject d){super(p,d);} }
    BeastBlock(Properties p,JsonObject d){super(p,d);}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos p,BlockState s){return CavernControl.BEAST.contains(name)?new CavernControl(p,s):name.equals("capsid")?new BeastCapsid(p,s):null;}
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==CavernControl.TYPE?(level,p,state,be)->CavernControl.tick((ServerLevel)level,p,state,(CavernControl)be):!l.isClientSide()&&type==BeastCapsid.TYPE?(level,p,state,be)->BeastCapsid.tick((ServerLevel)level,p,state,(BeastCapsid)be):null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){super.setPlacedBy(l,p,s,who,stack);if(who instanceof Player player&&l.getBlockEntity(p) instanceof CavernControl control)control.owner(player);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        if(l.getBlockEntity(p) instanceof CavernControl control&&player instanceof ServerPlayer sp){if(!control.permitted(sp))return InteractionResult.FAIL;if(control.loadChain(player,stack))return InteractionResult.SUCCESS;}
        if(name.equals("hummingbird_feeder")){
            int content=Integer.parseInt(value(s,"contents"));boolean water=stack.is(Items.WATER_BUCKET),sugar=stack.is(Items.SUGAR);
            int next=water?(content==2?3:1):sugar?(content==1?3:2):content;
            if(next==content)return InteractionResult.TRY_WITH_EMPTY_HAND;
            if(!l.isClientSide()){if(water)player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.BUCKET)));else stack.consume(1,player);l.setBlock(p,set(s,"contents",Integer.toString(next)),UPDATE_ALL);}return InteractionResult.SUCCESS;
        }
        if(name.equals("leafcutter_ant_chamber")&&stack.is(net.minecraft.tags.ItemTags.LEAVES)){
            if(!l.isClientSide()){String field=stateDefinition.getProperty("fungus")!=null?"fungus":"level";var prop=stateDefinition.getProperty(field);if(prop!=null){int n=Integer.parseInt(value(s,field));if(prop.getValue(Integer.toString(n+1)).isPresent()){l.setBlock(p,set(s,field,Integer.toString(n+1)),UPDATE_ALL);stack.consume(1,player);}}}return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack,s,l,p,player,hand,hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        if(name.equals("transmutation_table")){
            if(player instanceof ServerPlayer sp)sp.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyPatternMenu(id,inv,()->sp.level()==l&&sp.distanceToSqr(Vec3.atCenterOf(p))<=64&&l.getBlockState(p).is(this)&&Menagerie.mayChange(l,sp,p),ItemStack.EMPTY),getName()));return InteractionResult.SUCCESS;
        }
        if(name.equals("leafcutter_ant_chamber")){
            String field=stateDefinition.getProperty("fungus")!=null?"fungus":"level";var prop=stateDefinition.getProperty(field);
            if(prop!=null&&Integer.parseInt(value(s,field))>0){if(!l.isClientSide()){popResource(l,p.above(),new ItemStack(Menagerie.item("beast_gongylidia")));l.setBlock(p,set(s,field,"0"),UPDATE_ALL);}return InteractionResult.SUCCESS;}
        }
        return super.useWithoutItem(s,l,p,player,hit);
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        String species=switch(name){case "crocodile_egg"->"crocodile";case "caiman_egg"->"caiman";case "platypus_egg"->"platypus";case "terrapin_egg"->"terrapin";case "triops_eggs"->"triops";default->"";};
        if(!species.isEmpty()){
            if(random.nextInt(12)!=0||l.getEntitiesOfClass(MenagerieAnimal.class,new AABB(p).inflate(12)).size()>=12)return;
            if(stateDefinition.getProperty("hatch")!=null){int stage=Integer.parseInt(value(s,"hatch"));if(stage<2){l.setBlock(p,set(s,"hatch",Integer.toString(stage+1)),UPDATE_ALL);return;}}
            var mob=Menagerie.TYPES.get("beast_"+species).create(l,EntitySpawnReason.BREEDING);if(mob==null)return;mob.setBaby(true);mob.snapTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);
            if(!l.noCollision(mob)||!l.addFreshEntity(mob))return;
            int eggs=stateDefinition.getProperty("eggs")==null?1:Integer.parseInt(value(s,"eggs"));if(eggs<=1)l.removeBlock(p,false);else l.setBlock(p,set(set(s,"eggs",Integer.toString(eggs-1)),"hatch","0"),UPDATE_ALL);return;
        }
        if(name.equals("hummingbird_feeder")&&value(s,"contents").equals("3"))for(var bird:l.getEntitiesOfClass(MenagerieAnimal.class,new AABB(p).inflate(10),a->a.species().source().equals("hummingbird")&&!a.sitting()).stream().limit(4).toList()){
            bird.getNavigation().moveTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,1);if(bird.distanceToSqr(Vec3.atCenterOf(p))<4){bird.heal(2);l.setBlock(p,set(s,"contents","0"),UPDATE_ALL);break;}
        }
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block neighbor,net.minecraft.world.level.redstone.Orientation orientation,boolean moved){
        super.neighborChanged(s,l,p,neighbor,orientation,moved);if(l.isClientSide()||!name.equals("gustmaker")&&!name.equals("void_worm_beak")&&!name.equals("sculk_boomer"))return;
        boolean on=l.hasNeighborSignal(p);if(on==flag(s,"powered"))return;l.setBlock(p,set(s,"powered",Boolean.toString(on)),UPDATE_ALL);if(on)l.scheduleTick(p,this,1);
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(!flag(s,"powered"))return;
        var direction=facing(s);var center=Vec3.atCenterOf(p.relative(direction));
        if(name.equals("gustmaker")){
            for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center,center.add(direction.getStepX()*4,direction.getStepY()*4,direction.getStepZ()*4)).inflate(.6)).stream().limit(16).toList())e.push(direction.getStepX()*.12,direction.getStepY()*.12,direction.getStepZ()*.12);
            l.sendParticles(ParticleTypes.CLOUD,center.x,center.y,center.z,2,.1,.1,.1,.01);l.scheduleTick(p,this,10);
        }
        // Powered props never damage unconsenting players or erase player construction.
    }
}

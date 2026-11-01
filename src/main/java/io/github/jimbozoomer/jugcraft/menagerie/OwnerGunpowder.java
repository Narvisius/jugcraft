package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.entity.item.PrimedTnt;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Gunpowder is a consumable fire fuse, never an electrical redstone wire. */
public final class OwnerGunpowder {
    private OwnerGunpowder(){}
    public static boolean fuse(BlockState s){return s.getBlock() instanceof OwnerSupplementBlock b&&b.function.equals("fuse");}
    public static boolean supported(LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||level.getBlockState(pos.below()).is(Blocks.HOPPER);}
    static boolean fire(BlockState s){return s.is(net.minecraft.tags.BlockTags.FIRE)||s.is(Blocks.LAVA)||s.is(Blocks.MAGMA_BLOCK)||s.getBlock() instanceof TorchBlock&&!(s.getBlock() instanceof RedstoneTorchBlock)||s.getBlock() instanceof WallTorchBlock&&!(s.getBlock() instanceof RedstoneWallTorchBlock)||s.is(OwnerSupplementaries.BLOCKS.get("blaze_rod"))||s.hasProperty(BlockStateProperties.LIT)&&s.getValue(BlockStateProperties.LIT)&&(s.getBlock() instanceof CampfireBlock||s.getBlock() instanceof AbstractCandleBlock);}
    static boolean target(BlockState s){return fuse(s)||s.is(Blocks.TNT)||s.getBlock() instanceof CampfireBlock||s.getBlock() instanceof AbstractCandleBlock||s.getBlock() instanceof OwnerSupplementBlock b&&(b.function.equals("cannon")||b.function.startsWith("sconce"))||fire(s);}
    public static BlockState connections(BlockState s,LevelReader l,BlockPos p){
        for(var dir:Direction.Plane.HORIZONTAL){var q=p.relative(dir);String side="none";if(l.hasChunkAt(q)){
            var n=l.getBlockState(q);if(target(n))side="side";
            else if(n.isFaceSturdy(l,q,dir.getOpposite())&&!l.getBlockState(p.above()).isRedstoneConductor(l,p.above())&&target(l.getBlockState(q.above())))side="up";
            else if(!n.isRedstoneConductor(l,q)&&target(l.getBlockState(q.below())))side="side";
        }s=HouseholdBlock.set(s,dir.getName(),side);}return s;
    }
    public static InteractionResult place(UseOnContext use){
        var who=use.getPlayer();if(who==null||who.isSpectator())return InteractionResult.PASS;var level=use.getLevel();var c=new BlockPlaceContext(use);var p=c.getClickedPos();var block=OwnerSupplementaries.BLOCKS.get("gunpowder");
        if(block==null||!c.canPlace()||!level.hasChunkAt(p)||!level.isInWorldBounds(p)||!level.getWorldBorder().isWithinBounds(p)||!supported(level,p)||!level.getFluidState(p).isEmpty()||!Menagerie.mayChange(level,who,p))return InteractionResult.FAIL;
        if(!level.isClientSide()&&level.setBlockAndUpdate(p,connections(block.defaultBlockState(),level,p))){if(level.getBlockEntity(p) instanceof OwnerSupplementData d){d.owner=who.getUUID();d.changed();}use.getItemInHand().consume(1,who);level.playSound(null,p,SoundEvents.SAND_PLACE,SoundSource.BLOCKS,.5F,1.4F);}return InteractionResult.SUCCESS;
    }
    public static boolean light(ServerLevel level,BlockPos p,ServerPlayer actor){
        if(actor==null||!level.hasChunkAt(p)||!Authority.mayChange(level,actor,p)||!(level.getBlockEntity(p) instanceof OwnerSupplementData data)||!data.block().function.equals("fuse")||!HouseholdBlock.value(data.getBlockState(),"burning").equals("0"))return false;
        data.fuseActor=actor.getUUID();data.pulseUntil=level.getGameTime()+2;level.setBlockAndUpdate(p,HouseholdBlock.set(data.getBlockState(),"burning","1"));data.changed();level.playSound(null,p,SoundEvents.TNT_PRIMED,SoundSource.BLOCKS,.35F,1.9F);return true;
    }
    public static InteractionResult use(OwnerSupplementData data,Player who,ItemStack held,InteractionHand hand){
        if(!(data.getLevel() instanceof ServerLevel level)||!(who instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
        if((held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE))&&light(level,data.getBlockPos(),actor)){if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,who,hand.asEquipmentSlot());else held.consume(1,who);return InteractionResult.SUCCESS;}
        return InteractionResult.PASS;
    }
    public static void tick(ServerLevel level,OwnerSupplementData data){
        var p=data.getBlockPos();int age=Integer.parseInt(HouseholdBlock.value(data.getBlockState(),"burning"));
        if(age==0){if(level.getGameTime()%10!=Math.floorMod(p.asLong(),10))return;var actor=Authority.present(level,data.owner);if(actor==null)return;for(var dir:Direction.values()){var q=p.relative(dir);if(level.hasChunkAt(q)&&fire(level.getBlockState(q))){light(level,p,actor);break;}}return;}
        if(level.getGameTime()<data.pulseUntil)return;var actor=Authority.present(level,data.fuseActor);
        if(actor==null||!Authority.mayChange(level,actor,p)){level.setBlockAndUpdate(p,HouseholdBlock.set(data.getBlockState(),"burning","0"));data.fuseActor=null;data.changed();return;}
        level.sendParticles(ParticleTypes.SMALL_FLAME,p.getX()+.5,p.getY()+.08,p.getZ()+.5,2,.2,.02,.2,.005);
        if(age==2){var s=connections(data.getBlockState(),level,p);for(var dir:Direction.Plane.HORIZONTAL){String side=HouseholdBlock.value(s,dir.getName());if(side.equals("none"))continue;var q=p.relative(dir);if(side.equals("up"))q=q.above();else if(!target(level.getBlockState(q))&&!level.getBlockState(q).isRedstoneConductor(level,q))q=q.below();ignite(level,q,actor);}}
        if(age>=8){for(var dir:Direction.values())ignite(level,p.relative(dir),actor);level.removeBlock(p,false);level.sendParticles(ParticleTypes.POOF,p.getX()+.5,p.getY()+.1,p.getZ()+.5,4,.15,.1,.15,.02);level.playSound(null,p,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.3F,1.8F);}
        else{level.setBlockAndUpdate(p,HouseholdBlock.set(data.getBlockState(),"burning",Integer.toString(age+1)));data.pulseUntil=level.getGameTime()+2;data.setChanged();}
    }
    public static void contact(Level world,BlockPos pos,Entity entity){
        if(!(world instanceof ServerLevel level)||!(level.getBlockEntity(pos) instanceof OwnerSupplementData data))return;
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile&&projectile.isOnFire()&&projectile.getOwner() instanceof ServerPlayer actor)light(level,pos,actor);
        else if(entity instanceof ServerPlayer actor&&actor.getDeltaMovement().y>.12&&Authority.mayChange(level,actor,pos)&&!HouseholdBlock.value(data.getBlockState(),"burning").equals("0")){
            level.setBlockAndUpdate(pos,HouseholdBlock.set(data.getBlockState(),"burning","0"));data.fuseActor=null;data.changed();level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.4F,1.3F);
        }
    }
    static void ignite(ServerLevel level,BlockPos pos,ServerPlayer actor){
        if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!Authority.mayChange(level,actor,pos))return;
        var s=level.getBlockState(pos);if(fuse(s)){light(level,pos,actor);return;}
        if(level.getBlockEntity(pos) instanceof OwnerSupplementData d&&d.block().function.equals("cannon")){OwnerCannon.ignite(d,actor);return;}
        if(s.is(Blocks.TNT)){var tnt=new PrimedTnt(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,actor);tnt.addTag("JugcraftHouseholdCannon");if(level.addFreshEntity(tnt)){if(!level.removeBlock(pos,false)){tnt.discard();return;}level.playSound(null,pos,SoundEvents.TNT_PRIMED,SoundSource.BLOCKS,1,1);}return;}
        if((s.getBlock() instanceof CampfireBlock||s.getBlock() instanceof AbstractCandleBlock||s.getBlock() instanceof OwnerSupplementBlock b&&b.function.startsWith("sconce"))&&!HouseholdBlock.flag(s,"waterlogged")&&s.hasProperty(BlockStateProperties.LIT)&&!s.getValue(BlockStateProperties.LIT))level.setBlockAndUpdate(pos,s.setValue(BlockStateProperties.LIT,true));
    }
}

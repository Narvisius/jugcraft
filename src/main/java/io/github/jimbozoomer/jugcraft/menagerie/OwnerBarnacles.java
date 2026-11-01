package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.level.block.BonemealSource;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/** Original six-face underwater growth, with permission checks at every destination. */
final class OwnerBarnacles extends MultifaceBlock implements BonemealableBlock {
    static final TagKey<Block> BLACKLIST=TagKey.create(Registries.BLOCK,Jugcraft.id("supps_barnacles_blacklist"));
    OwnerBarnacles(Properties properties){super(properties);}
    @Override public boolean isValidStateForPlacement(BlockGetter level,BlockState state,BlockPos pos,Direction face){
        var support=pos.relative(face);var block=level.getBlockState(support);
        return block.isSolid()&&!block.is(BLACKLIST)&&super.isValidStateForPlacement(level,state,pos,face);
    }
    private MultifaceSpreader spreader(ServerPlayer actor){return new MultifaceSpreader(new MultifaceSpreader.DefaultSpreaderConfig(this){
        @Override public boolean canSpreadInto(BlockGetter level,BlockPos origin,MultifaceSpreader.SpreadPos target){
            var p=target.pos();
            if(level instanceof ServerLevel server&&(!server.hasChunkAt(p)||!server.hasChunkAt(p.relative(target.face()))||!Authority.mayChange(server,actor,p)))return false;
            return level.getFluidState(p).getType()==Fluids.WATER&&super.canSpreadInto(level,origin,target);
        }
    });}
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,BonemealSource source){
        if(!state.getValue(WATERLOGGED))return false;
        // No owner is available to native dispenser/mob callbacks: only allow
        // their growth where an unprivileged actor may actually change terrain.
        var actor=level instanceof ServerLevel server?Authority.depositDrillActor(server,null):null;
        var spread=spreader(actor);for(var face:Direction.values())if(spread.canSpreadInAnyDirection(state,level,pos,face))return true;return false;
    }
    @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){return true;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source){spreader(Authority.depositDrillActor(level,null)).spreadFromRandomFaceTowardRandomDirection(state,level,pos,random);}
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!held.is(Items.BONE_MEAL))return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
        if(!(level instanceof ServerLevel server)||!(player instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
        if(!state.getValue(WATERLOGGED))return InteractionResult.FAIL;
        if(spreader(actor).spreadFromRandomFaceTowardRandomDirection(state,server,pos,server.getRandom()).isPresent()){
            held.consume(1,player);server.levelEvent(1505,pos,15);return InteractionResult.SUCCESS;
        }return InteractionResult.FAIL;
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(state.getValue(WATERLOGGED)&&random.nextInt(100)==0){var faces=availableFaces(state).toArray(Direction[]::new);if(faces.length==0)return;var d=faces[random.nextInt(faces.length)];level.addParticle(ParticleTypes.BUBBLE,pos.getX()+.5+d.getStepX()*.44,pos.getY()+.5+d.getStepY()*.44,pos.getZ()+.5+d.getStepZ()*.44,0,.1,0);}
    }
}

package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;

/** Long-lived, single-effect area incense, extinguished by water and bound to its operator. */
final class BotanicalIncense {
    private final BotanicalWorkshop plate;private int remaining;
    BotanicalIncense(BotanicalWorkshop plate){this.plate=plate;}
    static boolean allowed(String id){return BotanicalBrewing.canInfuseEquipment(id);}
    static boolean accepts(ItemStack stack){return stack.is(Jugtania.item("incense_stick"))&&allowed(stack.getOrDefault(BotanicalBrewing.BREW,""));}
    boolean burning(){return remaining>0;}
    boolean extractable(){return !burning();}
    InteractionResult use(ServerPlayer actor,InteractionHand hand){
        var held=actor.getItemInHand(hand);var stored=plate.getItem(0);
        if(burning()){actor.sendOverlayMessage(Component.literal("Incense: "+remaining/20+" seconds remaining"));return InteractionResult.SUCCESS;}
        if(held.isEmpty()&&!stored.isEmpty()){actor.getInventory().placeItemBackInInventory(stored.copy(),net.minecraft.util.Prediction.SERVER_ONLY);plate.setItem(0,ItemStack.EMPTY);return InteractionResult.SUCCESS;}
        if(accepts(held)&&stored.isEmpty()){plate.setItem(0,held.copyWithCount(1));if(!actor.getAbilities().instabuild)held.shrink(1);return InteractionResult.SUCCESS;}
        if(held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE)){
            if(!accepts(stored)||plate.getBlockState().getValue(BlockStateProperties.WATERLOGGED))return InteractionResult.FAIL;
            remaining=BotanicalBrewing.BREWS.get(stored.getOrDefault(BotanicalBrewing.BREW,"")).effects().get(0).getDuration()*60;
            if(!actor.getAbilities().instabuild){if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,actor,hand.asEquipmentSlot());else held.shrink(1);}
            actor.level().playSound(null,plate.getBlockPos(),BotanicalSounds.get("incense_plate_ignite"),net.minecraft.sounds.SoundSource.BLOCKS,.5F,1.75F);plate.changed();return InteractionResult.SUCCESS;
        }actor.sendOverlayMessage(Component.literal("Insert infused incense, then light it with flint and steel or a fire charge."));return InteractionResult.SUCCESS;
    }
    void tick(ServerLevel level,ServerPlayer actor){
        if(remaining<=0)return;var pos=plate.getBlockPos();var stack=plate.getItem(0);
        if(!accepts(stack)||plate.getBlockState().getValue(BlockStateProperties.WATERLOGGED)||--remaining==0){remaining=0;plate.setItem(0,ItemStack.EMPTY);plate.changed();return;}
        plate.setChanged();if(level.getGameTime()%20!=0)return;
        var effect=BotanicalBrewing.BREWS.get(stack.getOrDefault(BotanicalBrewing.BREW,"")).effects().get(0);
        for(var player:level.players()){
            if(player.isSpectator()||Math.abs(player.getX()-pos.getX()-.5)>32||Math.abs(player.getY()-pos.getY()-.5)>32||Math.abs(player.getZ()-pos.getZ()-.5)>32||!Authority.mayChange(level,actor,player.blockPosition())||!ClaimTransfers.allowed(level,pos,player.blockPosition()))continue;
            var old=player.getEffect(effect.getEffect());int duration=effect.getEffect().equals(MobEffects.NIGHT_VISION)?285:80;
            if(old==null||old.getAmplifier()<=effect.getAmplifier()&&old.getDuration()<duration-20)player.addEffect(new MobEffectInstance(effect.getEffect(),duration,effect.getAmplifier(),true,true));
        }
        level.sendParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.4,pos.getZ()+.5,2,.08,.08,.08,.005);plate.changed();
    }
    void save(ValueOutput out){out.putInt("incense_remaining",remaining);}
    void load(ValueInput in){remaining=Math.clamp(in.getIntOr("incense_remaining",0),0,576000);}
    static final class Stick extends Item {
        Stick(Properties p){super(p);}
        @Override public Component getName(ItemStack stack){String id=stack.getOrDefault(BotanicalBrewing.BREW,"");return id.isEmpty()?super.getName(stack):Component.literal("Incense of "+id.replace('_',' '));}
    }
    static final class Plate extends BotanicalBlock implements SimpleWaterloggedBlock {
        Plate(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,false).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.WATERLOGGED,BlockStateProperties.HORIZONTAL_FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==Fluids.WATER);}
        @Override protected FluidState getFluidState(BlockState state){return state.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state);}
        @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,net.minecraft.util.RandomSource random){if(state.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));return super.updateShape(state,level,ticks,pos,direction,neighbor,neighborState,random);}
        @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.incense.burning()?2:b.getItem(0).isEmpty()?0:1:0;}
    }
}

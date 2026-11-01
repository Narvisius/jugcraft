package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.time.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Six real display slots, petting hops, retained names and the supplied birthday celebration. */
public final class BotanicalPotato {
    private final BotanicalWorkshop host;private Component name=Component.empty();private long jumpAt=Long.MIN_VALUE,lastDoIt=Long.MIN_VALUE;private int birthday;
    BotanicalPotato(BotanicalWorkshop host){this.host=host;}
    public Component name(){return name;}
    public float jump(float partial){if(host.getLevel()==null||jumpAt==Long.MIN_VALUE)return 0;double elapsed=host.getLevel().getGameTime()-jumpAt+partial;return elapsed<0||elapsed>20?0:20-(float)elapsed;}
    private void hop(){if(jump(0)>0)return;jumpAt=host.getLevel().getGameTime();host.changed();}
    InteractionResult use(ServerPlayer p,InteractionHand hand,Direction face){
        if(!host.permitted(p))return InteractionResult.FAIL;var held=p.getItemInHand(hand);
        if(held.is(Items.NAME_TAG)&&held.has(DataComponents.CUSTOM_NAME)){name=held.get(DataComponents.CUSTOM_NAME);if(!p.hasInfiniteMaterials())held.shrink(1);hop();host.changed();return InteractionResult.SUCCESS;}
        int slot=slot(face);var previous=host.getItem(slot).copy();
        if(held.isEmpty()){if(!previous.isEmpty()){host.setItem(slot,ItemStack.EMPTY);p.setItemInHand(hand,previous);}}
        else {host.setItem(slot,held.copyWithCount(1));if(!p.hasInfiniteMaterials())held.shrink(1);if(!previous.isEmpty())p.getInventory().placeItemBackInInventory(previous,net.minecraft.util.Prediction.SERVER_ONLY);}
        hop();long now=p.level().getGameTime();if(name.getString().trim().toLowerCase(java.util.Locale.ROOT).endsWith("shia labeouf")&&(lastDoIt==Long.MIN_VALUE||now-lastDoIt>=40)){lastDoIt=now;p.level().playSound(null,host.getBlockPos(),BotanicalSounds.get("doit"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
        p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,host.getBlockPos().getX()+.5,host.getBlockPos().getY()+.7,host.getBlockPos().getZ()+.5,2,.15,.1,.15,0);return InteractionResult.SUCCESS;
    }
    int slot(Direction worldFace){
        if(worldFace.getAxis()==Direction.Axis.Y)return worldFace.ordinal();
        var facing=host.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        // Rendered local SOUTH is the front at zero yaw; map world faces back to that frame.
        Direction local=worldFace;
        if(facing==Direction.NORTH)local=local.getOpposite();
        else if(facing==Direction.EAST)local=local.getClockWise();
        else if(facing==Direction.WEST)local=local.getCounterClockWise();
        return local.ordinal();
    }
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.getRandom().nextInt(100)==0)hop();var today=LocalDate.now();if(today.getMonthValue()!=7||today.getDayOfMonth()!=19){birthday=0;return;}
        var at=host.getBlockPos().relative(host.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));if(!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at)){birthday=0;return;}
        var cake=level.getBlockState(at);var guests=level.getEntitiesOfClass(Player.class,new AABB(host.getBlockPos()).inflate(8));
        if(!(cake.getBlock() instanceof CandleCakeBlock)||!cake.getValue(BlockStateProperties.LIT)||guests.isEmpty()){birthday=0;return;}
        birthday++;int index=java.util.List.of(100,170,240,310,380).indexOf(birthday);
        if(index>=0){Component message=Component.literal("<").append(name.getString().isEmpty()?Component.literal("Tiny Potato"):name).append("> ").append(Component.translatable("botania.tater_birthday."+index,today.getYear()-2014));for(var guest:guests)guest.sendSystemMessage(message);hop();level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,at.getX()+.5,at.getY()+1,at.getZ()+.5,15,.5,.5,.5,.01);}
        if(birthday>=380){level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());level.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,at.getX()+.5,at.getY()+1.5,at.getZ()+.5,60,.7,.8,.7,.1);level.playSound(null,at,net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,net.minecraft.sounds.SoundSource.BLOCKS,1,1);birthday=0;}host.setChanged();
    }
    void save(ValueOutput out){out.store("potato_name",ComponentSerialization.CODEC,name);out.putLong("potato_jump",jumpAt);out.putInt("potato_birthday",birthday);out.putLong("potato_doit",lastDoIt);}
    void load(ValueInput in){name=in.read("potato_name",ComponentSerialization.CODEC).orElse(Component.empty());jumpAt=in.getLongOr("potato_jump",Long.MIN_VALUE);birthday=Math.clamp(in.getIntOr("potato_birthday",0),0,379);lastDoIt=in.getLongOr("potato_doit",Long.MIN_VALUE);}
    public static final class Block extends BotanicalBlock implements SimpleWaterloggedBlock {
        Block(Properties p){super("tiny_potato",p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.HORIZONTAL_FACING,BlockStateProperties.WATERLOGGED);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);}
        @Override public void setPlacedBy(Level level,BlockPos at,BlockState state,LivingEntity entity,ItemStack stack){super.setPlacedBy(level,at,state,entity,stack);if(level.getBlockEntity(at) instanceof BotanicalWorkshop w){w.potato.name=stack.getOrDefault(DataComponents.CUSTOM_NAME,Component.empty());w.changed();}}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){return box(6,0,6,10,6,10);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){return player instanceof ServerPlayer p&&level.getBlockEntity(at) instanceof BotanicalWorkshop w?w.potato.use(p,hand,hit.getDirection()):InteractionResult.SUCCESS;}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos at,Player player,BlockHitResult hit){return player instanceof ServerPlayer p&&level.getBlockEntity(at) instanceof BotanicalWorkshop w?w.potato.use(p,InteractionHand.MAIN_HAND,hit.getDirection()):InteractionResult.SUCCESS;}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos at,Direction side){if(!(l.getBlockEntity(at) instanceof BotanicalWorkshop w))return 0;int count=0;for(int i=0;i<6;i++)if(!w.getItem(i).isEmpty())count++;return count==0?0:1+14*count/6;}
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos at,Direction side,BlockPos other,BlockState state,net.minecraft.util.RandomSource r){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(at,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,at,side,other,state,r);}
    }
}

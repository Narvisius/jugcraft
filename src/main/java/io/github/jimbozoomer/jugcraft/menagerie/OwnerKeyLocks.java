package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;

/** Named reusable keys. Key access never bypasses town or player claims. */
public final class OwnerKeyLocks {
    public static BlockEntityType<Data> TYPE;
    private OwnerKeyLocks() {}
    public static void register(){
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("supps_key_lock"),FabricBlockEntityTypeBuilder.create(Data::new,
            OwnerSupplementaries.BLOCKS.get("lock_block"),OwnerSupplementaries.BLOCKS.get("netherite_door"),OwnerSupplementaries.BLOCKS.get("netherite_trapdoor")).build());
        UseBlockCallback.EVENT.register((who,level,hand,hit)->{
            var pos=base(level,hit.getBlockPos());var state=level.getBlockState(pos);var node=level.getBlockEntity(pos);
            if(!(node instanceof Data)&&!(node instanceof OwnerSupplementData d&&d.block().function.equals("safe")))return InteractionResult.PASS;
            if(who.isSpectator()||who.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>64||!Menagerie.mayChange(level,who,pos))return InteractionResult.FAIL;
            boolean safe=node instanceof OwnerSupplementData;
            if(safe&&key(who.getItemInHand(hand))==null)return InteractionResult.PASS;
            if(state.getBlock() instanceof Door&&!Menagerie.mayChange(level,who,pos.above()))return InteractionResult.FAIL;
            // Let the normal bucket action reach a water/lava-loggable trapdoor.
            // Check possession before returning PASS, including on the predictive client.
            if(state.getBlock() instanceof Trap && who.getItemInHand(hand).getItem() instanceof BucketItem)
                return hasKey(who,password(node))?InteractionResult.PASS:InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            String password=password(node),held=key(who.getItemInHand(hand));
            if(held!=null&&(password==null||who.isShiftKeyDown())){
                if(node instanceof OwnerSupplementData d&&d.owner!=null&&!d.owner.equals(who.getUUID())&&!who.isCreative())return InteractionResult.FAIL;
                if(password==null){set(node,held);message(who,"Lock assigned to this key. Rename keys in an anvil to make matching copies.");}
                else if(who.isCreative()||password.equals(held)){set(node,null);message(who,"Key lock cleared.");}
                else message(who,"This key does not match the lock.");
                sound(level,pos,true);return InteractionResult.SUCCESS;
            }
            if(!hasKey(who,password)){message(who,"Locked: carry the matching named key.");return InteractionResult.FAIL;}
            if(safe){who.openMenu((OwnerSupplementData)node);return InteractionResult.SUCCESS;}
            if(state.getBlock() instanceof Door door)door.setOpen(who,level,state,pos,!state.getValue(DoorBlock.OPEN));
            else if(state.getBlock() instanceof Trap){var next=state.cycle(TrapDoorBlock.OPEN);level.setBlockAndUpdate(pos,next);sound(level,pos,next.getValue(TrapDoorBlock.OPEN));}
            else if(state.getBlock() instanceof Lock&&!state.getValue(BlockStateProperties.POWERED)){
                level.setBlockAndUpdate(pos,state.setValue(BlockStateProperties.POWERED,true));level.scheduleTick(pos,state.getBlock(),20);sound(level,pos,true);
            }
            return InteractionResult.SUCCESS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level,who,pos,state,node)->{
            var p=base(level,pos);var lock=level.getBlockEntity(p);
            return !(lock instanceof Data)||hasKey(who,password(lock));
        });
    }
    static BlockPos base(Level level,BlockPos pos){var s=level.getBlockState(pos);return s.getBlock() instanceof Door&&s.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?pos.below():pos;}
    static String key(ItemStack stack){if(!stack.is(OwnerSupplementaries.ITEMS.get("key")))return null;var name=stack.get(DataComponents.CUSTOM_NAME);String value=name==null?"Key":name.getString();return value.length()>256?value.substring(0,256):value;}
    static String password(BlockEntity node){return node instanceof Data d?d.password:node instanceof OwnerSupplementData d?d.keyPassword:null;}
    static void set(BlockEntity node,String value){if(node instanceof Data d)d.password=value;else if(node instanceof OwnerSupplementData d)d.keyPassword=value;node.setChanged();}
    public static boolean hasKey(Player who,String password){if(password==null||who.isCreative())return true;for(int i=0;i<who.getInventory().getContainerSize();i++)if(password.equals(key(who.getInventory().getItem(i))))return true;return password.equals(key(who.getOffhandItem()));}
    static void sound(Level level,BlockPos pos,boolean open){level.playSound(null,pos,open?SoundEvents.IRON_TRAPDOOR_OPEN:SoundEvents.IRON_TRAPDOOR_CLOSE,SoundSource.BLOCKS,.7F,1.2F);}
    static void message(Player who,String text){who.sendOverlayMessage(Component.literal(text));}
    public static final class Data extends BlockEntity {
        String password;
        Data(BlockPos pos,BlockState state){super(TYPE,pos,state);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(password!=null)out.putString("KeyPassword",password);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);password=in.getString("KeyPassword").orElse(null);if(password!=null&&password.length()>256)password=password.substring(0,256);}
        // No client update packet: the key name is not presentation state.
    }
    public static final class Door extends DoorBlock implements EntityBlock {
        Door(Properties props){super(BlockSetType.IRON,props.sound(SoundType.NETHERITE_BLOCK));}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return s.getValue(HALF)==DoubleBlockHalf.LOWER?new Data(pos,s):null;}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(OPEN,false).setValue(POWERED,false);}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block changed,net.minecraft.world.level.redstone.Orientation o,boolean moving){}
    }
    public static final class Trap extends TrapDoorBlock implements EntityBlock {
        static final BooleanProperty LAVA=BooleanProperty.create("lavalogged");
        Trap(Properties props){super(BlockSetType.IRON,props.sound(SoundType.NETHERITE_BLOCK).lightLevel(s->s.getValue(LAVA)?15:0));registerDefaultState(defaultBlockState().setValue(LAVA,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(LAVA);}
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(LAVA)?net.minecraft.world.level.material.Fluids.LAVA.getSource(false):super.getFluidState(s);}
        @Override public boolean canPlaceLiquid(net.minecraft.world.entity.LivingEntity who,BlockGetter l,BlockPos p,BlockState s,net.minecraft.world.level.material.Fluid fluid){return !s.getValue(LAVA)&&!s.getValue(WATERLOGGED)&&(fluid==net.minecraft.world.level.material.Fluids.LAVA||fluid==net.minecraft.world.level.material.Fluids.WATER);}
        @Override public boolean placeLiquid(LevelAccessor l,BlockPos p,BlockState s,net.minecraft.world.level.material.FluidState fluid){if(!canPlaceLiquid(null,l,p,s,fluid.getType()))return false;var prop=fluid.getType()==net.minecraft.world.level.material.Fluids.LAVA?LAVA:WATERLOGGED;if(!l.isClientSide()){l.setBlock(p,s.setValue(prop,true),3);l.scheduleTick(p,fluid.getType(),fluid.getType().getTickDelay(l));}return true;}
        @Override public ItemStack pickupBlock(net.minecraft.world.entity.LivingEntity who,LevelAccessor l,BlockPos p,BlockState s){if(s.getValue(LAVA)){l.setBlock(p,s.setValue(LAVA,false),3);return new ItemStack(Items.LAVA_BUCKET);}return super.pickupBlock(who,l,p,s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos q,BlockState n,RandomSource random){if(s.getValue(LAVA))ticks.scheduleTick(p,net.minecraft.world.level.material.Fluids.LAVA,net.minecraft.world.level.material.Fluids.LAVA.getTickDelay(l));return super.updateShape(s,l,ticks,p,dir,q,n,random);}

        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return new Data(pos,s);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(OPEN,false).setValue(POWERED,false).setValue(LAVA,c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.LAVA);}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block changed,net.minecraft.world.level.redstone.Orientation o,boolean moving){if(s.getValue(WATERLOGGED))l.scheduleTick(p,net.minecraft.world.level.material.Fluids.WATER,5);}
    }
    public static final class Lock extends Block implements EntityBlock {
        Lock(Properties props){super(props);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.POWERED);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Data(pos,state);}
        @Override protected boolean isSignalSource(BlockState s){return true;}
        @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction dir){return s.getValue(BlockStateProperties.POWERED)?15:0;}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(s.getValue(BlockStateProperties.POWERED)){l.setBlockAndUpdate(p,s.setValue(BlockStateProperties.POWERED,false));sound(l,p,false);}}
    }
}

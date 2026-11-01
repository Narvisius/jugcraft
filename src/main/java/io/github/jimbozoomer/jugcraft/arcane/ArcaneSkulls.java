package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.*;

/** Source fire-resistant skull stages. Placement preserves remaining protection time. */
public final class ArcaneSkulls {
    public static final List<String> NAMES=List.of("obsidian","cracked_obsidian","fragmented_obsidian","fading_obsidian","aurealic_obsidian","eternal_obsidian");
    public static DataComponentType<Integer> TICKS;
    private ArcaneSkulls(){}
    public static void register(){
        TICKS=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("arcane_skull_ticks"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,260)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->{if(!source.is(DamageTypeTags.IS_FIRE))return true;var stack=entity.getItemBySlot(EquipmentSlot.HEAD);if(!(stack.getItem() instanceof SkullItem skull))return true;return skull.kind.equals("aurealic_obsidian")&&(!(entity instanceof ServerPlayer p)||p.getAttachedOrCreate(ArcaneEssences.AUREAL)<=0);});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){if(!p.isOnFire())continue;var stack=p.getItemBySlot(EquipmentSlot.HEAD);if(!(stack.getItem() instanceof SkullItem skull))continue;int stage=NAMES.indexOf(skull.kind);if(stage==4){p.setAttached(ArcaneEssences.AUREAL,Math.max(0,p.getAttachedOrCreate(ArcaneEssences.AUREAL)-1));continue;}if(stage<0||stage>=4)continue;int ticks=stack.getOrDefault(TICKS,(8+p.getRandom().nextInt(6))*20)-1;if(ticks>0)stack.set(TICKS,ticks);else{Item replacement=stage==3?Items.SKELETON_SKULL:ForbiddenArcane.item(NAMES.get(stage+1)+"_skull");var next=stack.transmuteCopy(replacement,1);next.remove(TICKS);p.setItemSlot(EquipmentSlot.HEAD,next);p.level().playSound(null,p.blockPosition(),ForbiddenArcane.sound("block.obsidian_skull.crack"),p.getSoundSource(),.9F,.9F+p.getRandom().nextFloat()*.15F);}}});
    }
    public static final class SkullItem extends StandingAndWallBlockItem {
        public final String kind;
        public SkullItem(String kind,Block floor,Block wall,Properties p){super(floor,wall,Direction.DOWN,p.stacksTo(1).fireResistant().component(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.HEAD).setDamageOnHurt(false).build()));this.kind=kind;}
    }
    public static final class Skull extends BaseEntityBlock {
        public final String kind;public final boolean wall;
        public static final IntegerProperty ROTATION=BlockStateProperties.ROTATION_16;
        public static final EnumProperty<net.minecraft.core.Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
        public Skull(String kind,boolean wall,Properties p){super(p);this.kind=kind;this.wall=wall;registerDefaultState(defaultBlockState().setValue(ROTATION,0).setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(ROTATION,FACING);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Head(p,s);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(!wall)return Block.box(4,0,4,12,8,12);return switch(s.getValue(FACING)){case SOUTH->Block.box(4,4,0,12,12,8);case EAST->Block.box(0,4,4,8,12,12);case WEST->Block.box(8,4,4,16,12,12);default->Block.box(4,4,8,12,12,16);};}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){if(!wall)return defaultBlockState().setValue(ROTATION,net.minecraft.world.level.block.state.properties.RotationSegment.convertToSegment(c.getRotation()));for(var d:c.getNearestLookingDirections())if(d.getAxis().isHorizontal()){var s=defaultBlockState().setValue(FACING,d.getOpposite());if(s.canSurvive(c.getLevel(),c.getClickedPos()))return s;}return null;}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){if(!wall)return true;var f=s.getValue(FACING);var q=p.relative(f.getOpposite());return l.getBlockState(q).isFaceSturdy(l,q,f);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction f,BlockPos q,BlockState n,RandomSource r){return s.canSurvive(l,p)?s:Blocks.AIR.defaultBlockState();}
        @Override protected BlockState rotate(BlockState s,Rotation r){return wall?s.setValue(FACING,r.rotate(s.getValue(FACING))):s.setValue(ROTATION,r.rotate(s.getValue(ROTATION),16));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return wall?rotate(s,m.getRotation(s.getValue(FACING))):s.setValue(ROTATION,m.mirror(s.getValue(ROTATION),16));}
        @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean data){var stack=new ItemStack(ForbiddenArcane.item(kind+"_skull"));if(l.getBlockEntity(p) instanceof Head head&&head.ticks>=0)stack.set(TICKS,head.ticks);return stack;}
    }
    public static final class Head extends BlockEntity {
        private int ticks=-1;
        public Head(BlockPos p,BlockState s){super(ForbiddenArcane.SKULL,p,s);}
        @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);o.putInt("remaining",ticks);}
        @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);ticks=Math.clamp(i.getIntOr("remaining",-1),-1,260);}
        @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);if(ticks>=0)b.set(TICKS,ticks);}
        @Override protected void applyImplicitComponents(DataComponentGetter c){super.applyImplicitComponents(c);ticks=c.getOrDefault(TICKS,-1);}
    }
}

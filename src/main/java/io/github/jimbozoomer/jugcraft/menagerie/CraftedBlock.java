package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Supplied connected meshes, cushions, stack counts and hammer variants. */
public class CraftedBlock extends HouseholdBlock implements SimpleWaterloggedBlock {
    static class Seat extends CraftedBlock implements Sittable { Seat(Properties p, JsonObject d) { super(p,d); } }
    static class Storage extends CraftedBlock implements EntityBlock { Storage(Properties p, JsonObject d) { super(p,d); } }
    CraftedBlock(Properties p, JsonObject d) {
        super(p, d);
        var state = defaultBlockState();
        state = set(state, "shape", "single"); state = set(state, "trim", "normal");
        state = set(state, "color", "white"); state = set(state, "color", "none");
        state = set(state, "half", "lower"); state = set(state, "type", "1");
        registerDefaultState(state);
    }
    private boolean joins(BlockState a, BlockState b) { return a.is(b.getBlock()) && value(a,"color").equals(value(b,"color")); }
    private BlockState connect(BlockState s, LevelReader l, BlockPos pos) {
        if (name.endsWith("_table") && !name.endsWith("_side_table")) {
            boolean n=joins(s,l.getBlockState(pos.north())), e=joins(s,l.getBlockState(pos.east())),
                w=joins(s,l.getBlockState(pos.west())), south=joins(s,l.getBlockState(pos.south()));
            String shape = "single";
            if(n&&e&&south&&w)shape="center";
            else if(n&&south&&e)shape="west_center"; else if(n&&south&&w)shape="east_center";
            else if(e&&w&&n)shape="south_center"; else if(e&&w&&south)shape="north_center";
            else if(e&&south)shape="north_west_corner"; else if(w&&south)shape="north_east_corner";
            else if(e&&n)shape="south_west_corner"; else if(w&&n)shape="south_east_corner";
            else if(n&&south)shape="east_west_center"; else if(e&&w)shape="north_south_center";
            else if(n)shape="north_side"; else if(south)shape="south_side"; else if(e)shape="east_side"; else if(w)shape="west_side";
            return set(s,"shape",shape);
        }
        var f=facing(s); var left=l.getBlockState(pos.relative(f.getCounterClockWise())); var right=l.getBlockState(pos.relative(f.getClockWise()));
        boolean a=joins(s,left)&&facing(left)==f,b=joins(s,right)&&facing(right)==f;
        s=set(s,"shape",a?(b?"middle":"right"):(b?"left":"single"));
        // Corner benches/couches use the same clockwise convention as vanilla stair connections.
        var front=l.getBlockState(pos.relative(f)); var back=l.getBlockState(pos.relative(f.getOpposite()));
        if(joins(s,front)&&facing(front).getAxis()!=f.getAxis()) s=set(s,"shape",facing(front)==f.getCounterClockWise()?"outer_left":"outer_right");
        else if(joins(s,back)&&facing(back).getAxis()!=f.getAxis()) s=set(s,"shape",facing(back)==f.getCounterClockWise()?"inner_left":"inner_right");
        return s;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext c) {
        var s=super.getStateForPlacement(c); if(s==null)return null;
        if(s.hasProperty(BlockStateProperties.WATERLOGGED))s=s.setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));
        if(getStateDefinition().getProperty("half")!=null && getStateDefinition().getProperty("half").getValue("upper").isPresent()) {
            BlockPos upper=c.getClickedPos().above();
            if(!c.getLevel().getBlockState(upper).canBeReplaced(c)||c.getPlayer()!=null&&!Menagerie.mayChange(c.getLevel(),c.getPlayer(),upper))return null;
            s=set(s,"half","lower");
        } else s=set(s,"half",c.getClickedFace()==Direction.DOWN||c.getClickedFace()!=Direction.UP&&c.getClickLocation().y-c.getClickedPos().getY()>.5?"top":"bottom");
        s=set(s,"seed",Integer.toString(c.getLevel().getRandom().nextInt(256)+1));
        return connect(s,c.getLevel(),c.getClickedPos());
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){
        super.setPlacedBy(l,p,s,who,stack);
        if(!l.isClientSide()&&value(s,"half").equals("lower"))l.setBlock(p.above(),set(s,"half","upper"),UPDATE_ALL);
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos n,BlockState other,RandomSource random){
        if(flag(s,"waterlogged"))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));
        String half=value(s,"half");
        if((half.equals("lower")&&dir==Direction.UP||half.equals("upper")&&dir==Direction.DOWN)&&!other.is(this))return Blocks.AIR.defaultBlockState();
        return connect(s,l,p);
    }
    @Override protected FluidState getFluidState(BlockState s){return flag(s,"waterlogged")?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        String item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String color=stack.has(net.minecraft.core.component.DataComponents.DYE)?stack.get(net.minecraft.core.component.DataComponents.DYE).getSerializedName():item.startsWith("crafted_")&&item.endsWith("_cushion")?item.substring(8,item.length()-8):"";
        if(!color.isEmpty()&&getStateDefinition().getProperty("color")!=null&&!value(s,"color").equals(color)){
            var next=set(s,"color",color);if(next!=s){if(!l.isClientSide()){l.setBlockAndUpdate(p,next);stack.consume(1,player);}return InteractionResult.SUCCESS;}
        }
        for(String property:new String[]{"pieces","jars","books"})if(getStateDefinition().getProperty(property)!=null&&stack.is(asItem())){
            var next=set(s,property,Integer.toString(Integer.parseInt(value(s,property))+1));
            if(next!=s){if(!l.isClientSide()){l.setBlockAndUpdate(p,next);stack.consume(1,player);}return InteractionResult.SUCCESS;}
        }
        if(getStateDefinition().getProperty("counter")!=null && stack.getItem() instanceof BlockItem blockItem){
            var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            var next=id.getNamespace().equals("minecraft")?set(s,"counter",id.getPath()):s;
            if(next!=s){if(!l.isClientSide()){l.setBlockAndUpdate(p,next);stack.consume(1,player);}return InteractionResult.SUCCESS;}
        }
        return super.useItemOn(stack,s,l,p,player,hand,hit);
    }
    public void customize(Level l,BlockPos p,BlockState s,boolean rotate,Player player){
        if(value(s,"half").equals("upper")||value(s,"half").equals("lower")){
            BlockPos base=value(s,"half").equals("upper")?p.below():p;
            var lower=l.getBlockState(base);var upper=l.getBlockState(base.above());
            if(!lower.is(this)||!upper.is(this)||!Menagerie.mayChange(l,player,base)||!Menagerie.mayChange(l,player,base.above()))return;
            l.setBlock(base,rotateClockwise(lower),UPDATE_CLIENTS);l.setBlock(base.above(),rotateClockwise(upper),UPDATE_ALL);return;
        }
        if(rotate){l.setBlockAndUpdate(p,rotateClockwise(s));return;}
        for(String name:new String[]{"type","trim","seed"}){
            var prop=getStateDefinition().getProperty(name);if(prop==null)continue;
            var values=prop.getPossibleValues().stream().map(Object::toString).toList();
            l.setBlockAndUpdate(p,set(s,name,values.get((values.indexOf(value(s,name))+1)%values.size())));return;
        }
        l.setBlockAndUpdate(p,rotateClockwise(s));
    }
}

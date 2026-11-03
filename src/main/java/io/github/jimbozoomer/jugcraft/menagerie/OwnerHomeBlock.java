package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.tower.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import java.util.Set;

/** Original Home state families with bounded, claim-checked native interactions. */
public class OwnerHomeBlock extends HouseholdBlock implements EntityBlock, OptionalWaterloggedBlock {
    public final String function, rig;
    static OwnerHomeBlock make(Properties p, JsonObject d) {
        return Set.of("chair","sofa","couch","bench","zaisu","saddle_block","stump_chair","telescope").contains(d.get("function").getAsString())?new Seat(p,d):new OwnerHomeBlock(p,d);
    }
    private static final class Seat extends OwnerHomeBlock implements Sittable { Seat(Properties p,JsonObject d){super(p,d);} }
    OwnerHomeBlock(Properties p, JsonObject d) {
        super(p,d);function=d.get("function").getAsString();rig=d.has("rig")?d.get("rig").getAsString():"";
        registerDefaultState(set(set(set(set(set(defaultBlockState(),"contains","none"),"part",function.equals("bathtub")?"front":"bottom"),"half","upper"),"vertical_connection","single"),"horizontal_connection","single"));
    }
    public static String stateValue(BlockState s,String key) { return value(s,key); }
    public boolean storage(){return Set.of("storage_counter","drawer","desk").contains(function);}
    private boolean liquid(){return Set.of("sink_counter","sink","bathtub","fountain","dye_vat").contains(function);}
    private boolean same(BlockState s){return s.getBlock() instanceof OwnerHomeBlock b&&b.function.equals(function);}
    private int part(BlockState s){return switch(value(s,"part")){case "middle"->1;case "top"->2;default->0;};}
    public BlockPos root(BlockPos pos,BlockState s){return function.equals("grandfather_clock")?pos.below(part(s)):function.equals("chandelier")&&value(s,"half").equals("lower")?pos.above():function.equals("bathtub")&&value(s,"part").equals("back")?pos.relative(facing(s).getOpposite()):pos;}
    private boolean editable(Level l,Player player,BlockPos pos,BlockState s){
        BlockPos root=root(pos,s);if(!Menagerie.mayChange(l,player,root))return false;
        if(function.equals("grandfather_clock"))return Menagerie.mayChange(l,player,root.above())&&Menagerie.mayChange(l,player,root.above(2));
        if(function.equals("chandelier"))return Menagerie.mayChange(l,player,root.below());
        return !function.equals("bathtub")||Menagerie.mayChange(l,player,root.relative(facing(s)));
    }
    private BlockState joined(BlockState s,BlockGetter level,BlockPos p) {
        for(Direction dir:Direction.Plane.HORIZONTAL)s=set(s,dir.getName(),Boolean.toString(same(level.getBlockState(p.relative(dir)))));
        for(String corner:new String[]{"north_east","north_west","south_east","south_west"}){
            String[] axes=corner.split("_");Direction a=Direction.byName(axes[0]),b=Direction.byName(axes[1]);
            s=set(s,corner,Boolean.toString(same(level.getBlockState(p.relative(a).relative(b)))));
        }
        Direction f=facing(s);if(!f.getAxis().isHorizontal())return s;
        BlockState left=level.getBlockState(p.relative(f.getClockWise())),right=level.getBlockState(p.relative(f.getCounterClockWise()));
        boolean a=same(left)&&facing(left)==f,b=same(right)&&facing(right)==f;
        String horizontal=a?(b?"middle":"right"):b?"left":"single";
        s=set(s,"horizontal_connection",horizontal);s=set(s,"advanced_horizontal_connection",horizontal);
        boolean up=same(level.getBlockState(p.above())),down=same(level.getBlockState(p.below()));
        String vertical=up?(down?"middle":"tail"):down?"head":"single";
        s=set(s,"vertical_connection",vertical);s=set(s,"vertical_with_extra_connection",vertical);
        if(function.equals("counter")){
            String shape="straight";BlockState front=level.getBlockState(p.relative(f)),back=level.getBlockState(p.relative(f.getOpposite()));
            if(same(front)&&facing(front).getAxis()!=f.getAxis())shape=facing(front)==f.getCounterClockWise()?"outer_left":"outer_right";
            else if(same(back)&&facing(back).getAxis()!=f.getAxis())shape=facing(back)==f.getCounterClockWise()?"inner_left":"inner_right";
            s=set(s,"shape",shape);
        }
        return s;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var level=c.getLevel();var p=c.getClickedPos();int height=function.equals("grandfather_clock")?3:1;
        for(int i=0;i<height;i++)if(!level.getBlockState(p.above(i)).canBeReplaced(c)||p.getY()+i>=level.getMaxY()||c.getPlayer()!=null&&!Menagerie.mayChange(level,c.getPlayer(),p.above(i)))return null;
        var state=set(defaultBlockState(),"facing",c.getHorizontalDirection().getOpposite().getName());
        state=set(state,"rotation",Integer.toString(Math.floorMod((int)Math.floor((c.getRotation()+180)*16/360+.5),16)));
        state=set(state,"face",c.getClickedFace()==Direction.UP?"floor":c.getClickedFace()==Direction.DOWN?"ceiling":"wall");
        state=set(state,"hanging",Boolean.toString(c.getClickedFace()==Direction.DOWN));
        state=set(state,"omni_rotation",Integer.toString(Math.floorMod((int)Math.floor(c.getRotation()/90+.5),4)));
        state=set(state,"waterlogged",Boolean.toString(level.getFluidState(p).getType()==Fluids.WATER));
        if(function.equals("lamp")){
            int count=1;for(int i=1;i<=3&&same(level.getBlockState(p.below(i)));i++)count++;
            for(int i=1;i<=3&&same(level.getBlockState(p.above(i)));i++)count++;
            if(count>3)return null;
            if(same(level.getBlockState(p.below()))){var below=level.getBlockState(p.below());state=set(set(state,"facing",facing(below).getName()),"lit",value(below,"lit"));}
        }
        if(function.equals("bathtub")){BlockPos other=p.relative(facing(state));if(!level.getBlockState(other).canBeReplaced(c)||c.getPlayer()!=null&&!Menagerie.mayChange(level,c.getPlayer(),other))return null;}
        if(function.equals("chandelier")&&(!level.getBlockState(p.below()).canBeReplaced(c)||c.getPlayer()!=null&&!Menagerie.mayChange(level,c.getPlayer(),p.below())))return null;
        return joined(state,level,p);
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack item){
        if(l.isClientSide())return;
        if(function.equals("grandfather_clock"))for(int i=1;i<3;i++)l.setBlock(p.above(i),set(set(s,"part",i==1?"middle":"top"),"waterlogged",Boolean.toString(l.getFluidState(p.above(i)).getType()==Fluids.WATER)),UPDATE_ALL);
        if(function.equals("bathtub"))l.setBlock(p.relative(facing(s)),set(s,"part","back"),UPDATE_ALL);
        if(function.equals("chandelier"))l.setBlock(p.below(),set(s,"half","lower"),UPDATE_ALL);
        if(l.getBlockEntity(p) instanceof OwnerHome.Data data){data.color=DyedItemColor.getOrDefault(item,0xFFFFFFFF);data.owner=placer==null?null:placer.getUUID();data.changed();}
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader level,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos q,BlockState other,RandomSource random){
        if(flag(s,"waterlogged"))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        // Delay pair validation until all placement cells exist. No chunk loads.
        if(function.equals("grandfather_clock")||function.equals("bathtub")||function.equals("chandelier"))ticks.scheduleTick(p,this,1);
        return joined(s,level,p);
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        BlockPos root=root(p,s);boolean valid=true;
        if(function.equals("grandfather_clock"))for(int i=0;i<3;i++)valid&=l.getBlockState(root.above(i)).is(this)&&part(l.getBlockState(root.above(i)))==i;
        if(function.equals("bathtub"))valid=l.getBlockState(root).is(this)&&l.getBlockState(root.relative(facing(s))).is(this);
        if(function.equals("chandelier"))valid=l.getBlockState(root).is(this)&&l.getBlockState(root.below()).is(this);
        if(!valid)l.destroyBlock(p,true);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return storage()?new OwnerHomeStorage(p,s):new OwnerHome.Data(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        if(l.isClientSide()||type!=OwnerHome.DATA)return null;
        if(function.equals("grandfather_clock")&&value(s,"part").equals("top"))return (level,pos,state,be)->{
            var data=(OwnerHome.Data)be;long time=level.getDefaultClockTime();
            if(level.getGameTime()%20==0)level.playSound(null,pos,OwnerHome.sound("grandfather_clock_tick"),SoundSource.BLOCKS,.15F,1);
            if(data.lastClockTime>=0&&time>data.lastClockTime&&time-data.lastClockTime<100&&Math.floorDiv(data.lastClockTime+6000,24000)!=Math.floorDiv(time+6000,24000))level.playSound(null,pos,OwnerHome.sound("grandfather_clock_midnight"),SoundSource.BLOCKS,.5F,1);
            data.lastClockTime=time;
        };
        if(function.equals("telescope"))return (level,pos,state,be)->{
            if(level.getGameTime()%2!=0)return;var data=(OwnerHome.Data)be;
            for(var seat:level.getEntitiesOfClass(SeatEntity.class,new AABB(pos))){
                if(seat.getFirstPassenger() instanceof Player player){
                    float yaw=player.getYRot(),pitch=player.getXRot();if(Math.abs(yaw-data.yaw)>.1F||Math.abs(pitch-data.pitch)>.1F){data.yaw=yaw;data.pitch=pitch;data.changed();}break;
                }
            }
        };
        if(function.equals("fountain_spout")||function.equals("sink")||function.equals("sink_counter")||function.equals("bathtub"))return (level,pos,state,be)->{
            if(level.getGameTime()%20==0)OwnerHomeLiquids.pump((ServerLevel)level,pos,state,(OwnerHome.Data)be);
        };
        return null;
    }
    @Override protected FluidState getFluidState(BlockState s){return flag(s,"waterlogged")?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(function.equals("grandfather_clock")){int i=part(s);return i==2?Block.box(1,0,1,15,12,15):Block.box(2,0,2,14,16,14);}
        if(function.equals("chair")&&!rig.isEmpty())return chairShape(s);
        return super.getShape(s,l,p,c);
    }
    private VoxelShape chairShape(BlockState s){
        double angle=Math.toRadians(stateDefinition.getProperty("rotation")==null?facing(s).toYRot():Integer.parseInt(value(s,"rotation"))*22.5);
        double sin=Math.sin(angle),cos=Math.cos(angle),shift=flag(s,"tucked")?-.625:0;
        VoxelShape result=Shapes.empty();
        for(var box:java.util.List.of(new AABB(.125,0,.125,.875,.625,.875),new AABB(.125,.625,.75,.875,1.625,.95))){
            double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
            for(double x:new double[]{box.minX,box.maxX})for(double z:new double[]{box.minZ,box.maxZ}){
                double localX=x-.5,localZ=z-.5+shift,rotatedX=localX*cos-localZ*sin+.5,rotatedZ=localX*sin+localZ*cos+.5;
                minX=Math.min(minX,rotatedX);maxX=Math.max(maxX,rotatedX);minZ=Math.min(minZ,rotatedZ);maxZ=Math.max(maxZ,rotatedZ);
            }
            result=Shapes.or(result,Shapes.create(minX,box.minY,minZ,maxX,box.maxY,maxZ));
        }
        return result;
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return function.equals("falling_liquid")||function.equals("tent")&&flag(s,"open")?Shapes.empty():getShape(s,l,p,c);}
    @Override public double seatHeight(){return function.equals("telescope")?.2:function.equals("zaisu")?.15:.625;}
    @Override public boolean publicTownInteraction(){return !storage();}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){
        if(!editable(l,who,p,s)&&!(publicTownInteraction()&&io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(who,l,p)))return InteractionResult.FAIL;
        if(l.isClientSide())return InteractionResult.SUCCESS;
        if(storage()){if(l.getBlockEntity(p) instanceof OwnerHomeStorage storage)who.openMenu(storage);return InteractionResult.SUCCESS;}
        if(liquid()&&who.isShiftKeyDown()&&stateDefinition.getProperty("triggered")!=null){
            BlockPos base=root(p,s);l.setBlockAndUpdate(base,set(l.getBlockState(base),"triggered",Boolean.toString(!flag(s,"triggered"))));
            if(l.getBlockEntity(base) instanceof OwnerHome.Data data){data.owner=who.getUUID();data.changed();}
            who.sendOverlayMessage(Component.literal(flag(s,"triggered")?"Tap off":"Tap on — draws from a nearby filled basin or cauldron"));return InteractionResult.SUCCESS;
        }
        if(function.endsWith("clock")){
            long time=Math.floorMod(l.getDefaultClockTime()+6000,24000);int hour=(int)(time/1000),minute=(int)((time%1000)*60/1000);
            who.sendOverlayMessage(Component.literal(String.format(java.util.Locale.ROOT,"%02d:%02d %s",hour%12==0?12:hour%12,minute,hour>=12?"PM":"AM")));return InteractionResult.SUCCESS;
        }
        if(function.equals("chair")&&(who.isShiftKeyDown()||flag(s,"tucked"))){l.setBlockAndUpdate(p,set(s,"tucked",Boolean.toString(!flag(s,"tucked"))));return InteractionResult.SUCCESS;}
        if(this instanceof Sittable){
            if(function.equals("telescope")&&l.getBlockEntity(p) instanceof OwnerHome.Data data){data.yaw=who.getYRot();data.pitch=who.getXRot();data.changed();}
            float yaw=stateDefinition.getProperty("rotation")==null?facing(s).toYRot():Integer.parseInt(value(s,"rotation"))*22.5F;
            SeatEntity.sit(l,p,who,yaw,seatHeight());return InteractionResult.SUCCESS;
        }
        if(stateDefinition.getProperty("lit")!=null||function.equals("tent")){
            if(function.equals("lamp")){
                var members=new java.util.ArrayList<BlockPos>();members.add(p);
                for(Direction dir:java.util.List.of(Direction.UP,Direction.DOWN))for(int i=1;i<=3;i++){BlockPos q=p.relative(dir,i);if(!same(l.getBlockState(q)))break;if(!Menagerie.mayChange(l,who,q)&&!io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(who,l,q))return InteractionResult.FAIL;members.add(q);}
                for(BlockPos q:members)l.setBlockAndUpdate(q,set(l.getBlockState(q),"lit",Boolean.toString(!flag(s,"lit"))));
                l.playSound(null,p,OwnerHome.sound("lamp_toggle"),SoundSource.BLOCKS,.3F,flag(s,"lit")?.8F:1);
                return InteractionResult.SUCCESS;
            }
            String property=function.equals("tent")?"open":"lit";l.setBlockAndUpdate(p,set(s,property,Boolean.toString(!flag(s,property))));
            if(function.equals("chandelier")){
                BlockPos other=value(s,"half").equals("lower")?p.above():p.below();if(l.getBlockState(other).is(this))l.setBlockAndUpdate(other,set(l.getBlockState(other),property,Boolean.toString(!flag(s,property))));
            }
            l.playSound(null,p,SoundEvents.LEVER_CLICK,SoundSource.BLOCKS,.5F,flag(s,property)?.6F:.8F);return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit){
        if(!editable(l,who,p,s))return InteractionResult.FAIL;
        if(l.getBlockEntity(p) instanceof OwnerHome.Data data){
            if(stack.is(OwnerHome.item("paint_brush"))){
                if(!l.isClientSide()){
                    if(who.isShiftKeyDown())stack.set(DataComponents.DYED_COLOR,new DyedItemColor(data.color));
                    else {data.color=DyedItemColor.getOrDefault(stack,0xFFFFFFFF);data.changed();}
                }return InteractionResult.SUCCESS;
            }
            if(stack.getItem() instanceof DyeItem dye){
                if(!l.isClientSide()){int rgb=dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextureDiffuseColor();data.color=data.color==0xFFFFFFFF?rgb:blend(data.color,rgb);data.changed();stack.consume(1,who);}return InteractionResult.SUCCESS;
            }
            if(Set.of("chair","sofa","couch").contains(function)&&stack.is(OwnerHome.item("cushion"))||Set.of("chair","sofa","couch").contains(function)&&(stack.is(OwnerHome.item("hay_cushion"))||stack.is(OwnerHome.item("trader_cushion")))){
                if(!l.isClientSide()){var old=data.cushion;data.cushion=stack.copyWithCount(1);stack.consume(1,who);if(!old.isEmpty())who.getInventory().placeItemBackInInventory(old,net.minecraft.util.Prediction.SERVER_ONLY);data.changed();}return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.SHEARS)&&!data.cushion.isEmpty()){
                if(!l.isClientSide()){who.getInventory().placeItemBackInInventory(data.cushion,net.minecraft.util.Prediction.SERVER_ONLY);data.cushion=ItemStack.EMPTY;data.changed();}return InteractionResult.SUCCESS;
            }
        }
        if(liquid()){InteractionResult result=OwnerHomeLiquids.use(this,stack,s,l,p,who,hand);if(result!=InteractionResult.PASS)return result;}
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    private static int blend(int a,int b){return 0xFF000000|(((a>>16&255)+(b>>16&255))/2<<16)|(((a>>8&255)+(b>>8&255))/2<<8)|((a&255)+(b&255))/2;}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return storage();}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction direction){return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(l.getBlockEntity(p));}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide()&&player.preventsBlockDrops()&&(function.equals("grandfather_clock")||function.equals("bathtub")||function.equals("chandelier"))){
            BlockPos base=root(p,s);int count=function.equals("grandfather_clock")?3:2;
            for(int i=0;i<count;i++){BlockPos q=function.equals("grandfather_clock")?base.above(i):function.equals("chandelier")?base.below(i):base.relative(facing(s),i);if(!q.equals(p)&&l.getBlockState(q).is(this))l.setBlock(q,Blocks.AIR.defaultBlockState(),UPDATE_ALL|UPDATE_SUPPRESS_DROPS);}
        }return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var drops=super.getDrops(s,params);
        if(params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof OwnerHome.Data data&&data.color!=-1)
            for(var stack:drops)if(stack.is(asItem()))stack.set(DataComponents.DYED_COLOR,new DyedItemColor(data.color));
        return drops;
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){
        String rotation=value(s,"rotation");return rotation.isEmpty()?super.rotate(s,r):set(s,"rotation",Integer.toString(r.rotate(Integer.parseInt(rotation),16)));
    }
    @Override protected BlockState mirror(BlockState s,Mirror m){
        String rotation=value(s,"rotation");return rotation.isEmpty()?super.mirror(s,m):set(s,"rotation",Integer.toString(m.mirror(Integer.parseInt(rotation),16)));
    }
}

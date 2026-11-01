package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Supplied botanical sensors, weather doll, spark switch and life-producing devices. */
public final class BotanicalDevices {
    private static void weather(ServerLevel level,int clear,int rain,boolean raining,boolean thunder){var data=level.getWeatherData();data.setClearWeatherTime(clear);data.setRainTime(rain);data.setThunderTime(rain);data.setRaining(raining);data.setThundering(thunder);}

    static final Set<String> TYPES=Set.of("animated_torch","forest_eye","ender_eye_block","teru_teru_bozu","spawner_claw","cocoon","spark_changer");
    private final BotanicalWorkshop host;
    private int mode,turnTicks,oldSide,signal,age,emeralds,chorus;
    private boolean rare,redstone,hatchAquatic;
    private String hatchType="";
    BotanicalDevices(BotanicalWorkshop host){this.host=host;}
    private static final Direction[] SIDES={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    private int side(){return Arrays.asList(SIDES).indexOf(host.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));}
    public float angle(float partial){if(!host.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING))return 0;int side=side();if(turnTicks==0)return side*90;int delta=Math.floorMod(side-oldSide,4);return (oldSide+delta*(1-Math.max(0,turnTicks-partial)/4F))*90;}
    public boolean rotating(){return turnTicks>0;}
    public int age(){return age;}
    void rotate(ServerLevel level,int next){if(turnTicks>0)return;oldSide=side();turnTicks=4;level.setBlockAndUpdate(host.getBlockPos(),host.getBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,SIDES[Math.floorMod(next,4)]));host.changed();level.updateNeighborsAt(host.getBlockPos(),host.getBlockState().getBlock());}
    void burst(ServerPlayer player){rotate(player.level(),switch(mode){case 1->side()+1;case 2->player.level().getRandom().nextInt(4);default->side()+2;});}
    void wand(ServerPlayer actor){if(host.kind().equals("animated_torch")){mode=(mode+1)%3;host.changed();}actor.sendOverlayMessage(Component.literal(status()));}
    String status(){return switch(host.kind()){
        case "animated_torch"->"Animated Torch: "+List.of("toggle","rotate","random").get(mode)+" · sneak-empty-hand rotates manually";
        case "forest_eye"->"Eye of the Ancients: comparator "+signal+" (animals nearby minus one)";
        case "cocoon"->"Cocoon: "+Math.min(120,age/20)+" / 120 seconds · emeralds "+emeralds+"/20 · chorus "+chorus+"/20"+(rare?" · rare hatch guaranteed":"");
        case "teru_teru_bozu"->"Sunflower clears rain; Blue Orchid has a one-in-ten chance to start rain.";
        case "spark_changer"->"Spark Tinkerer: insert an augment. A redstone edge swaps it with an adjacent pool spark.";
        case "spawner_claw"->"Life Imbuer: place above a spawner. Keeps it active for six mana per tick.";
        default->"Ender Overseer: emits redstone when a player looks at it within 64 blocks. Pumpkins conceal the gaze.";};}
    InteractionResult use(ServerPlayer actor,InteractionHand hand){
        var held=actor.getItemInHand(hand);String kind=host.kind();
        if(kind.equals("spark_changer")){
            if(!held.isEmpty()&&!augment(held))return InteractionResult.PASS;
            var previous=host.getItem(0).copy();host.setItem(0,held.isEmpty()?ItemStack.EMPTY:held.copyWithCount(1));if(!held.isEmpty()&&!actor.getAbilities().instabuild)held.shrink(1);if(!previous.isEmpty())actor.getInventory().placeItemBackInInventory(previous,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;
        }
        if(kind.equals("cocoon")&&feed(held,actor.getAbilities().instabuild))return InteractionResult.SUCCESS;
        if(kind.equals("teru_teru_bozu")&&weather(actor.level(),held,actor.getAbilities().instabuild))return InteractionResult.SUCCESS;
        if(held.isEmpty()){if(kind.equals("animated_torch")&&actor.isShiftKeyDown())rotate(actor.level(),side()+1);else actor.sendOverlayMessage(Component.literal(status()));return InteractionResult.SUCCESS;}
        return InteractionResult.PASS;
    }
    static boolean augment(ItemStack stack){return !stack.isEmpty()&&List.of("dominant","recessive","dispersive","isolated").stream().anyMatch(a->stack.is(Jugtania.item("spark_upgrade_"+a)));}
    private boolean feed(ItemStack stack,boolean creative){
        if(!hatchType.isEmpty())return false;
        if(stack.is(Items.EMERALD)&&emeralds<20)emeralds++;
        else if(stack.is(Items.CHORUS_FRUIT)&&chorus<20)chorus++;
        else if(stack.is(Jugtania.item("life_essence"))&&!rare){rare=true;age=Math.max(age,1200);}
        else return false;
        if(!creative)stack.shrink(1);host.changed();return true;
    }
    private boolean weather(ServerLevel level,ItemStack stack,boolean creative){
        if(!level.dimension().equals(Level.OVERWORLD))return false;
        if(stack.is(Items.SUNFLOWER)&&level.isRaining())weather(level,12000+level.getRandom().nextInt(168000),0,false,false);
        else if(stack.is(Items.BLUE_ORCHID)&&!level.isRaining()){if(level.getRandom().nextInt(10)==0)weather(level,0,12000+level.getRandom().nextInt(12000),true,false);}
        else return false;
        if(!creative)stack.shrink(1);return true;
    }
    void tick(ServerLevel level,ServerPlayer actor){
        String kind=host.kind();var pos=host.getBlockPos();
        if(kind.equals("animated_torch")){if(turnTicks>0){turnTicks--;host.setChanged();if(turnTicks==0){host.changed();level.updateNeighborsAt(pos,host.getBlockState().getBlock());}}return;}
        if(kind.equals("spark_changer")){
            boolean powered=level.hasNeighborSignal(pos);if(powered&&!redstone){
                var list=new ArrayList<BotanicalWorkshop>();for(var dir:Direction.Plane.HORIZONTAL){var p=pos.relative(dir);if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof BotanicalWorkshop other&&other.kind().endsWith("pool")&&other.sparks.installed()&&other.permitted(actor)&&ClaimTransfers.allowed(level,pos,p))list.add(other);}
                Collections.shuffle(list,new Random(level.getRandom().nextLong()));
                for(var other:list){var replacement=other.sparks.swap(host.getItem(0));if(replacement!=null){host.setItem(0,replacement);break;}}
            }if(redstone!=powered){redstone=powered;host.setChanged();}return;
        }
        if(kind.equals("forest_eye")&&level.getGameTime()%10==0){int next=Math.clamp(level.getEntitiesOfClass(Animal.class,new AABB(pos).inflate(6),a->a.isAlive()).size()-1,0,15);if(signal!=next){signal=next;host.changed();}return;}
        if(kind.equals("ender_eye_block")&&level.getGameTime()%2==0){
            boolean watching=false;for(var player:level.players()){
                if(!player.isAlive()||player.isSpectator()||player.distanceToSqr(Vec3.atCenterOf(pos))>4096||player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN))continue;
                Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(64));
                // Only raycast loaded paths; a watcher cannot cause chunk generation.
                boolean loaded=true;for(int i=0;i<=64;i+=4)if(!level.hasChunkAt(BlockPos.containing(start.lerp(end,i/64D)))){loaded=false;break;}if(!loaded)continue;
                var hit=level.clip(new ClipContext(start,end,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,player));if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos)){watching=true;break;}
            }
            if(host.getBlockState().getValue(BlockStateProperties.POWERED)!=watching)level.setBlockAndUpdate(pos,host.getBlockState().setValue(BlockStateProperties.POWERED,watching));return;
        }
        if(kind.equals("teru_teru_bozu")){
            if(level.dimension().equals(Level.OVERWORLD)&&level.isRaining()&&level.getRandom().nextInt(9600)==0)weather(level,12000+level.getRandom().nextInt(168000),0,false,false);
            int next=level.isRaining()?15:0;if(signal!=next){signal=next;host.changed();}
        }
        if(kind.equals("cocoon")){if(age<2400){age++;host.setChanged();}if(age>=2400&&level.getGameTime()%20==0)hatch(level,actor);}
        if((kind.equals("cocoon")||kind.equals("teru_teru_bozu"))&&level.getGameTime()%10==0){int examined=0;for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos))){if(++examined>32)break;if(!ClaimTransfers.allowed(level,pos,drop.blockPosition())||!Authority.mayChange(level,actor,drop.blockPosition()))continue;var stack=drop.getItem();if(kind.equals("cocoon")?feed(stack,false):weather(level,stack,false)){if(stack.isEmpty())drop.discard();else drop.setItem(stack);break;}}}
    }
    private void hatch(ServerLevel level,ServerPlayer actor){
        var pos=host.getBlockPos();var water=new ArrayList<BlockPos>();for(var d:Direction.values()){var p=d==Direction.UP?pos:pos.relative(d);if(level.hasChunkAt(p)&&level.getFluidState(p).isSource()&&level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER))water.add(p);}
        if(hatchType.isEmpty()){
            EntityType<?> type;
            if(level.getRandom().nextFloat()<chorus/20F)type=net.minecraft.world.entity.EntityTypes.SHULKER;
            else if(level.getRandom().nextFloat()<emeralds/20F)type=net.minecraft.world.entity.EntityTypes.VILLAGER;
            else{hatchAquatic=!water.isEmpty();String tag="jugtania_cocoon/"+(rare||level.getRandom().nextFloat()<.075F?"rare":"common")+(water.isEmpty()?"":"_aquatic");var choices=BuiltInRegistries.ENTITY_TYPE.get(TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id(tag)));type=choices.flatMap(c->c.getRandomElement(level.getRandom())).map(Holder::value).orElse(null);}
            if(type==null)return;hatchType=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();host.setChanged();
        }
        var type=BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.tryParse(hatchType));if(type==null||!(type.create(level,EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob))return;
        if(hatchAquatic&&water.isEmpty())return;
        var at=hatchAquatic?water.get(level.getRandom().nextInt(water.size())):pos;mob.snapTo(at.getX()+.5,at.getY()+.5,at.getZ()+.5,level.getRandom().nextFloat()*360,0);
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(at),EntitySpawnReason.MOB_SUMMONED,null);if(mob instanceof AgeableMob young)young.setAge(-24000);
        var box=mob.getBoundingBox();for(var p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!Authority.mayChange(level,actor,p)||!ClaimTransfers.allowed(level,pos,p))return;if(!p.equals(pos)&&!level.getBlockState(p).getCollisionShape(level,p).isEmpty())return;}
        if(!level.getEntities(mob,box,e->e.isAlive()&&!(e instanceof ItemEntity)).isEmpty())return;
        mob.setPersistenceRequired();
        if(level.getRandom().nextInt(100)==0){mob.setCustomName(Component.literal("Jeanne d' ").append(mob.getName()).append(" [SSR]").withStyle(net.minecraft.ChatFormatting.GOLD));mob.setCustomNameVisible(true);}
        if(!level.addFreshEntity(mob))return;
        level.setBlockAndUpdate(pos,host.getBlockState().getValue(BlockStateProperties.WATERLOGGED)?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());level.levelEvent(2004,pos,0);
    }
    /** Called from the spawner's ordinary nearby-player check; all other spawning rules stay native. */
    public static boolean sustain(ServerLevel level,BlockPos spawner){var pos=spawner.above();if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof BotanicalWorkshop b)||!b.kind().equals("spawner_claw"))return false;var actor=b.actor();return actor!=null&&b.permitted(actor)&&Authority.mayChange(level,actor,spawner)&&ClaimTransfers.allowed(level,pos,spawner)&&b.draw(6);}
    void save(ValueOutput out){out.putInt("device_mode",mode);out.putInt("device_turn",turnTicks);out.putInt("device_old_side",oldSide);out.putInt("device_signal",signal);out.putInt("cocoon_age",age);out.putInt("cocoon_emeralds",emeralds);out.putInt("cocoon_chorus",chorus);out.putBoolean("cocoon_rare",rare);out.putString("cocoon_hatch",hatchType);out.putBoolean("cocoon_aquatic",hatchAquatic);out.putBoolean("device_redstone",redstone);}
    void load(ValueInput in){mode=Math.clamp(in.getIntOr("device_mode",0),0,2);turnTicks=Math.clamp(in.getIntOr("device_turn",0),0,4);oldSide=Math.clamp(in.getIntOr("device_old_side",0),0,3);signal=Math.clamp(in.getIntOr("device_signal",0),0,15);age=Math.clamp(in.getIntOr("cocoon_age",0),0,2400);emeralds=Math.clamp(in.getIntOr("cocoon_emeralds",0),0,20);chorus=Math.clamp(in.getIntOr("cocoon_chorus",0),0,20);rare=in.getBooleanOr("cocoon_rare",false);hatchType=in.getStringOr("cocoon_hatch","");hatchAquatic=in.getBooleanOr("cocoon_aquatic",false);redstone=in.getBooleanOr("device_redstone",false);}
    static final class Device extends BotanicalBlock implements SimpleWaterloggedBlock {
        Device(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED,false).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.POWERED,BlockStateProperties.HORIZONTAL_FACING,BlockStateProperties.WATERLOGGED);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==Fluids.WATER);}
        @Override protected FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState ns,net.minecraft.util.RandomSource random){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,p,d,n,ns,random);}
        @Override protected boolean isSignalSource(BlockState s){return kind.equals("animated_torch")||kind.equals("ender_eye_block");}
        @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){if(kind.equals("ender_eye_block"))return s.getValue(BlockStateProperties.POWERED)?15:0;return kind.equals("animated_torch")&&s.getValue(BlockStateProperties.HORIZONTAL_FACING)==d&&l.getBlockEntity(p) instanceof BotanicalWorkshop b&&!b.devices.rotating()?15:0;}
        @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof BotanicalWorkshop b?(kind.equals("forest_eye")||kind.equals("teru_teru_bozu")?b.devices.signal:super.getAnalogOutputSignal(s,l,p,d)):0;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(kind){case "animated_torch"->Block.box(0,0,0,16,4,16);case "forest_eye"->Block.box(4,4,4,12,12,12);case "cocoon"->Block.box(3,0,3,13,14,13);case "teru_teru_bozu"->Block.box(4,.16,4,12,15.84,12);default->super.getShape(s,l,p,c);};}
        @Override protected RenderShape getRenderShape(BlockState s){return Set.of("animated_torch","cocoon","teru_teru_bozu").contains(kind)?RenderShape.INVISIBLE:super.getRenderShape(s);}
    }
}

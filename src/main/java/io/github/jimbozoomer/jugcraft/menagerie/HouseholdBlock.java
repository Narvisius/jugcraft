package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenCabinetBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

/** State and collision adapters retain the supplied connected furniture variants. */
public class HouseholdBlock extends Block {
    private static final ThreadLocal<JsonObject> BUILDING=new ThreadLocal<>();
    public final String name,kind;
    private final Map<BlockState,VoxelShape> shapes=new IdentityHashMap<>();
    private final JsonObject definition;
    static HouseholdBlock create(Properties p,JsonObject d){
        BUILDING.set(d);
        try {
            String kind=d.get("kind").getAsString(), name=d.get("source").getAsString();
            if(kind.equals("cavern"))return CavernControl.CAVE.contains(name)||CavernDevice.KINDS.contains(name)
                    ?new CavernBlock.WithEntity(p,d):new CavernBlock(p,d);
            if(kind.equals("beast"))return CavernControl.BEAST.contains(name)||name.equals("capsid")
                    ?new BeastBlock.WithEntity(p,d):new BeastBlock(p,d);
            if(kind.equals("seat"))return new Seat(p,d);
            return name.equals("door_mat")||name.endsWith("ceiling_fan")||kind.equals("storage")||HouseholdCooking.KINDS.contains(kind)
                    ?new WithEntity(p,d):new HouseholdBlock(p,d);
        }finally{BUILDING.remove();}
    }
    private static final class WithEntity extends HouseholdBlock implements EntityBlock { WithEntity(Properties p,JsonObject d){super(p,d);} }
    private static final class Seat extends HouseholdBlock implements Sittable { Seat(Properties p,JsonObject d){super(p,d);} }
    protected HouseholdBlock(Properties p,JsonObject d){
        super(p);definition=d;name=d.get("source").getAsString();kind=d.get("kind").getAsString();
        BlockState state=stateDefinition.any();
        for(var prop:stateDefinition.getProperties()){
            for(String choice:List.of("false","0","north","bottom","default","unconnected","left","wall"))if(prop.getValue(choice).isPresent()){state=set(state,prop.getName(),choice);break;}
        }
        registerDefaultState(state);
        var supplied=d.getAsJsonObject("shapes");
        for(var s:stateDefinition.getPossibleStates()){
            VoxelShape shape=Shapes.empty();
            for(var entry:supplied.entrySet())if(matches(s,entry.getKey())){
                for(var box:entry.getValue().getAsJsonArray()){
                    var a=box.getAsJsonArray();shape=Shapes.or(shape,Block.box(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble(),a.get(3).getAsDouble(),a.get(4).getAsDouble(),a.get(5).getAsDouble()));
                }break;
            }
            if(d.has("parts"))for(var part:d.getAsJsonArray("parts")){
                var piece=part.getAsJsonObject();if(!condition(s,piece.getAsJsonObject("when")))continue;
                for(var box:piece.getAsJsonArray("boxes")){var a=box.getAsJsonArray();shape=Shapes.or(shape,Block.box(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble(),a.get(3).getAsDouble(),a.get(4).getAsDouble(),a.get(5).getAsDouble()));}
            }
            shapes.put(s,(shape.isEmpty()?Block.box(0,0,0,16,name.contains("fence")||name.contains("hedge")?16:2,16):shape).optimize());
        }
    }
    private static boolean matches(BlockState s,String key){for(String pair:key.split(",")){var parts=pair.split("=");if(parts.length==2&&!value(s,parts[0]).equals(parts[1]))return false;}return true;}
    private static boolean condition(BlockState s,JsonObject condition){
        for(var entry:condition.entrySet()){
            if(entry.getKey().equals("OR")){boolean any=false;for(var v:entry.getValue().getAsJsonArray())any|=condition(s,v.getAsJsonObject());if(!any)return false;}
            else if(entry.getKey().equals("AND")){for(var v:entry.getValue().getAsJsonArray())if(!condition(s,v.getAsJsonObject()))return false;}
            else if(!Arrays.asList(entry.getValue().getAsString().split("\\|" )).contains(value(s,entry.getKey())))return false;
        }return true;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        for(var entry:BUILDING.get().getAsJsonObject("properties").entrySet()){
            String name=entry.getKey();List<String> choices=new ArrayList<>();entry.getValue().getAsJsonArray().forEach(v->choices.add(v.getAsString()));
            if(!choices.isEmpty()&&choices.stream().allMatch(v->v.equals("false")||v.equals("true")))b.add(BooleanProperty.create(name));
            else if(name.equals("facing"))b.add(choices.contains("up")?BlockStateProperties.FACING:HorizontalDirectionalBlock.FACING);
            else b.add(new Choice(name,choices));
        }
    }
    private static final class Choice extends Property<String>{
        private final List<String> choices;
        Choice(String name,List<String> values){super(name,String.class);choices=List.copyOf(values);}
        @Override public List<String> getPossibleValues(){return choices;}
        @Override public String getName(String value){return value;}
        @Override public Optional<String> getValue(String value){return choices.contains(value)?Optional.of(value):Optional.empty();}
        @Override public int getInternalIndex(String value){return choices.indexOf(value);}
        @Override public boolean equals(Object o){return this==o||o instanceof Choice p&&super.equals(o)&&choices.equals(p.choices);}
        @Override public int generateHashCode(){return 31*super.generateHashCode()+choices.hashCode();}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    static BlockState set(BlockState state,String name,String value){Property prop=state.getBlock().getStateDefinition().getProperty(name);if(prop==null)return state;var v=prop.getValue(value);return v.isPresent()?state.setValue(prop,(Comparable)v.get()):state;}
    @SuppressWarnings({"rawtypes","unchecked"})
    static String value(BlockState state,String name){
        // Light functions are evaluated while Block is still building its state
        // definition. The state's properties already exist at that point.
        Property p=state.getProperties().stream().filter(v->v.getName().equals(name)).findFirst().orElse(null);
        return p==null?"":p.getName(state.getValue(p));
    }
    public static boolean flag(BlockState s,String name){return value(s,name).equals("true");}
    public static Direction facing(BlockState s){return s.hasProperty(HorizontalDirectionalBlock.FACING)?s.getValue(HorizontalDirectionalBlock.FACING):s.hasProperty(BlockStateProperties.FACING)?s.getValue(BlockStateProperties.FACING):Direction.NORTH;}
    public BlockState rotateClockwise(BlockState s){var f=facing(s);return f.getAxis().isVertical()?s:set(s,"facing",f.getClockWise().getSerializedName());}
    public boolean rotatePlaced(Level l,BlockPos p,BlockState state,Player player){
        if(!paired())return l.setBlock(p,rotateClockwise(state),UPDATE_ALL);
        BlockPos base=value(state,"type").equals("head")?p.relative(facing(state).getOpposite()):p;
        var old=l.getBlockState(base);var oldHead=base.relative(facing(old));var next=rotateClockwise(old);var newHead=base.relative(facing(next));
        if(!l.hasChunkAt(newHead)||!Menagerie.mayChange(l,player,base)||!Menagerie.mayChange(l,player,oldHead)||!Menagerie.mayChange(l,player,newHead)||!l.getBlockState(oldHead).is(this)||!l.getBlockState(newHead).canBeReplaced())return false;
        // Suppress intermediate neighbour updates while both halves move, preserving their water/open state.
        l.setBlock(oldHead,Blocks.AIR.defaultBlockState(),UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE);
        l.setBlock(base,set(next,"type","bottom"),UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE);
        l.setBlock(newHead,set(next,"type","head"),UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE);
        for(var at:List.of(base,oldHead,newHead))l.updateNeighborsAt(at,this);
        return true;
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return set(s,"facing",r.rotate(facing(s)).getSerializedName());}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(facing(s)));}
    public double seatHeight(){return name.endsWith("bath")?.25:name.endsWith("stool")?.65:.5;}
    private boolean paired(){var type=stateDefinition.getProperty("type");return type!=null&&type.getValue("head").isPresent()&&type.getValue("bottom").isPresent();}
    private boolean joins(BlockState other){return other.getBlock() instanceof HouseholdBlock b&&family().equals(b.family());}
    private String family(){return name.replaceFirst("^(pale_oak|dark_oak|oak|spruce|birch|jungle|acacia|mangrove|cherry|bamboo|crimson|warped|light_blue|light_gray|white|orange|magenta|yellow|lime|pink|gray|cyan|purple|blue|brown|green|red|black)_","");}
    private BlockState connections(BlockState s,LevelReader l,BlockPos p){
        for(Direction dir:Direction.Plane.HORIZONTAL)s=set(s,dir.getSerializedName(),Boolean.toString(joins(l.getBlockState(p.relative(dir)))));
        var f=facing(s);if(f.getAxis().isVertical())return s;var leftState=l.getBlockState(p.relative(f.getCounterClockWise()));var rightState=l.getBlockState(p.relative(f.getClockWise()));boolean left=joins(leftState)&&facing(leftState)==f,right=joins(rightState)&&facing(rightState)==f;
        s=set(set(s,"left",Boolean.toString(left)),"right",Boolean.toString(right));
        if(stateDefinition.getProperty("shape")!=null){
            String shape=left?(right?"middle":"right"):(right?"left":"default");
            var dirs=new ArrayList<String>();for(Direction d:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST))if(joins(l.getBlockState(p.relative(d))))dirs.add(d.getSerializedName());
            var prop=stateDefinition.getProperty("shape");String joined=String.join("_",dirs);
            if(!joined.isEmpty()&&prop.getValue(joined).isPresent())shape=joined;
            s=set(s,"shape",shape);
        }
        return s;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext c){
        BlockState s=set(defaultBlockState(),"facing",c.getHorizontalDirection().getOpposite().getSerializedName());
        if(s.hasProperty(BlockStateProperties.FACING))s=s.setValue(BlockStateProperties.FACING,c.getClickedFace().getOpposite());
        if(stateDefinition.getProperty("face")!=null)s=set(s,"face",c.getClickedFace()==Direction.UP?"floor":c.getClickedFace()==Direction.DOWN?"ceiling":"wall");
        s=set(s,"hinge",c.getClickLocation().x-c.getClickedPos().getX()>.5?"right":"left");
        if(paired()&&value(s,"type").equals("bottom")){
            var next=c.getClickedPos().relative(facing(s));if(!c.getLevel().getBlockState(next).canBeReplaced(c)||c.getPlayer()!=null&&!Menagerie.mayChange(c.getLevel(),c.getPlayer(),next))return null;
        }
        return connections(s,c.getLevel(),c.getClickedPos());
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){
        if(!l.isClientSide()&&paired()&&value(s,"type").equals("bottom"))l.setBlock(p.relative(facing(s)),set(s,"type","head"),UPDATE_ALL);
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos n,BlockState other,RandomSource random){
        String part=value(s,"type");
        if(paired()&&(part.equals("head")||part.equals("bottom"))){
            Direction partner=part.equals("bottom")?facing(s):facing(s).getOpposite();
            if(dir==partner&&!other.is(this))return Blocks.AIR.defaultBlockState();
        }
        return connections(s,l,p);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shapes.get(s);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return name.endsWith("fence_gate")&&flag(s,"open")?Shapes.empty():getShape(s,l,p,c);}
    public @Nullable BlockEntity newBlockEntity(BlockPos p,BlockState s){
        if(name.equals("door_mat"))return new io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlockEntity(p,s);
        if(name.endsWith("ceiling_fan"))return new HouseholdFan(p,s);
        if(kind.equals("storage"))return new KitchenCabinetBlockEntity(p,s);
        if(HouseholdCooking.KINDS.contains(kind))return new HouseholdAppliance(p,s);
        return null;
    }
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        if(l.isClientSide())return null;
        return type==Household.APPLIANCE?(level,pos,state,entity)->AbstractFurnaceBlockEntity.serverTick((ServerLevel)level,pos,state,(HouseholdAppliance)entity):null;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){Containers.updateNeighboursAfterDestroy(s,l,p);}
    @Override protected boolean triggerEvent(BlockState s,Level l,BlockPos p,int event,int value){
        var entity=l.getBlockEntity(p);return entity!=null&&entity.triggerEvent(event,value);
    }
    @Override protected @Nullable MenuProvider getMenuProvider(BlockState s,Level l,BlockPos p){
        return l.getBlockEntity(p) instanceof MenuProvider provider?provider:null;
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(l.getBlockEntity(p) instanceof KitchenCabinetBlockEntity cabinet)cabinet.recheckOpen();
        if(kind.equals("bell"))l.setBlock(p,set(s,"powered","false"),UPDATE_ALL);
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(kind.equals("spile")&&flag(s,"has_bucket")&&l.getBlockState(p.relative(facing(s).getOpposite())).is(Ecology.block("sappy_maple_log"))){
            int full=Integer.parseInt(value(s,"fullness"));if(full<3)l.setBlock(p,set(s,"fullness",Integer.toString(full+1)),UPDATE_CLIENTS);
        }
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        if(name.equals("door_mat")&&stack.is(Items.NAME_TAG)){
            var text=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);if(text!=null&&l.getBlockEntity(p) instanceof io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlockEntity mat&&!l.isClientSide())mat.engrave(text.getString());return InteractionResult.SUCCESS;
        }
        if(kind.equals("spile")&&stack.is(Items.BUCKET)&&!flag(s,"has_bucket")){
            if(!l.isClientSide()){stack.consume(1,player);l.setBlock(p,set(set(s,"has_bucket","true"),"fullness","0"),UPDATE_ALL);}return InteractionResult.SUCCESS;
        }
        if(kind.equals("cauldron")&&stack.is(Items.GLASS_BOTTLE)){
            var below=l.getBlockState(p.below());boolean heated=below.is(Blocks.MAGMA_BLOCK)||below.is(Blocks.LAVA)||below.is(Blocks.CAMPFIRE)&&below.getValue(BlockStateProperties.LIT);
            if(!heated){if(!l.isClientSide())player.sendOverlayMessage(Component.literal("Heat the sap cauldron over a lit campfire to bottle syrup."));return InteractionResult.SUCCESS;}
            if(!l.isClientSide()){
                player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Menagerie.item("ecology_maple_syrup_bottle"))));
                int remaining=Integer.parseInt(value(s,"level"))-1;l.setBlock(p,remaining==0?Blocks.CAULDRON.defaultBlockState():set(s,"level",Integer.toString(remaining)),UPDATE_ALL);
            }return InteractionResult.SUCCESS;
        }
        if(name.equals("pot")&&stack.is(net.minecraft.tags.ItemTags.AXES)){
            if(!l.isClientSide()){l.setBlock(p,set(s,"chisel",Integer.toString((Integer.parseInt(value(s,"chisel"))+1)%6)),UPDATE_ALL);stack.hurtAndBreak(1,player,hand);}return InteractionResult.SUCCESS;
        }
        if(name.equals("maple_pancakes")&&stack.is(Menagerie.ITEMS.get("ecology_maple_pancake"))){
            int count=Integer.parseInt(value(s,"servings"));if(count<11){if(!l.isClientSide()){stack.consume(1,player);l.setBlock(p,set(s,"servings",Integer.toString(count+1)),UPDATE_ALL);}return InteractionResult.SUCCESS;}
        }
        if(kind.equals("cutting")&&l instanceof ServerLevel server){
            var tool=player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            var recipe=CuttingRecipe.find(server.getServer(),stack,tool);
            if(recipe.isPresent()){
                stack.consume(1,player);for(var result:recipe.get().cut())if(!player.getInventory().add(result))Block.popResource(l,p.above(),result);
                tool.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);return InteractionResult.SUCCESS;
            }
            player.sendOverlayMessage(Component.literal("Hold the food and a knife, one in each hand, to slice."));return InteractionResult.SUCCESS;
        }
        if(kind.equals("sink")||name.endsWith("bath")){
            BlockPos basin=value(s,"type").equals("head")?p.relative(facing(s).getOpposite()):p;BlockState current=l.getBlockState(basin);
            if(!Menagerie.mayChange(l,player,basin)||name.endsWith("bath")&&!Menagerie.mayChange(l,player,basin.relative(facing(current))))return InteractionResult.FAIL;
            int water=stateDefinition.getProperty("water_level")==null?0:Integer.parseInt(value(current,"water_level"));
            if(stack.is(Items.WATER_BUCKET)&&water<3){
                if(!l.isClientSide()){player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.BUCKET)));water(l,basin,current,3);}return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.BUCKET)&&water==3){
                if(!l.isClientSide()){player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.WATER_BUCKET)));water(l,basin,current,0);}return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.GLASS_BOTTLE)&&(water>0||l.getFluidState(basin.below()).is(net.minecraft.world.level.material.Fluids.WATER))){
                if(!l.isClientSide()){player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER)));if(water>0)water(l,basin,current,water-1);}return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    private void water(Level l,BlockPos p,BlockState s,int amount){
        l.setBlock(p,set(s,"water_level",Integer.toString(amount)),UPDATE_ALL);
        if(name.endsWith("bath")&&value(s,"type").equals("bottom")){var other=p.relative(facing(s));var partner=l.getBlockState(other);if(partner.is(this))l.setBlock(other,set(partner,"water_level",Integer.toString(amount)),UPDATE_ALL);}
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        if(l.isClientSide()){if(name.equals("computer"))MenagerieItems.openGuide.run();return InteractionResult.SUCCESS;}
        if(kind.equals("spile")&&flag(s,"has_bucket")){
            boolean full=value(s,"fullness").equals("3");Block.popResource(l,p,new ItemStack(full?Menagerie.item("ecology_maple_sap_bucket"):Items.BUCKET));l.setBlock(p,set(set(s,"has_bucket","false"),"fullness","0"),UPDATE_ALL);return InteractionResult.SUCCESS;
        }
        if(kind.equals("meal")&&player.canEat(false)){
            player.getFoodData().eat(3,.4F);
            if(name.equals("maple_pancakes")){int remaining=Integer.parseInt(value(s,"servings"));if(remaining==0)l.removeBlock(p,false);else l.setBlock(p,set(s,"servings",Integer.toString(remaining-1)),UPDATE_ALL);}
            else {int bites=Integer.parseInt(value(s,"bites"));if(bites>=3)l.removeBlock(p,false);else l.setBlock(p,set(s,"bites",Integer.toString(bites+1)),UPDATE_ALL);}
            return InteractionResult.SUCCESS;
        }
        if(l.getBlockEntity(p) instanceof MenuProvider menu){player.openMenu(menu);return InteractionResult.SUCCESS;}
        if(kind.equals("workbench")){HouseholdConstruction.open((ServerPlayer)player,p);return InteractionResult.SUCCESS;}
        if(kind.equals("seat")){
            if(player.isShiftKeyDown()&&stateDefinition.getProperty("tucked")!=null)l.setBlock(p,set(s,"tucked",Boolean.toString(!flag(s,"tucked"))),UPDATE_ALL);
            else SeatEntity.sit(l,p,player,facing(s),seatHeight());
            return InteractionResult.SUCCESS;
        }
        if(kind.equals("display")||kind.equals("switch")||name.endsWith("fence_gate")){toggle(l,p,s);return InteractionResult.SUCCESS;}
        if(kind.equals("bell")){l.playSound(null,p,SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.BLOCKS,1,1);l.setBlock(p,set(s,"powered","true"),UPDATE_ALL);l.scheduleTick(p,this,20);return InteractionResult.SUCCESS;}
        if(kind.equals("cutting"))player.sendOverlayMessage(Component.literal("Hold food and a knife in opposite hands to slice; combine ingredients at a crafting table."));
        return InteractionResult.PASS;
    }
    void toggle(Level l,BlockPos p,BlockState s){
        String field=stateDefinition.getProperty("enabled")!=null?"enabled":stateDefinition.getProperty("powered")!=null?"powered":stateDefinition.getProperty("lit")!=null?"lit":"open";
        boolean on=!flag(s,field);var next=set(s,field,Boolean.toString(on));
        if(kind.equals("switch")||kind.equals("display"))next=set(next,"lit",Boolean.toString(on));
        if(name.endsWith("fence_gate"))next=set(next,"open",Boolean.toString(on));
        l.setBlock(p,next,UPDATE_ALL);
        l.playSound(null,p,SoundEvents.LEVER_CLICK,SoundSource.BLOCKS,.4F,flag(s,field)?.5F:.6F);
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block neighbor,net.minecraft.world.level.redstone.Orientation orientation,boolean moving){
        if(!l.isClientSide()&&!name.endsWith("lightswitch")&&(kind.equals("switch")||kind.equals("display")||name.endsWith("fence_gate"))){
            boolean powered=l.hasNeighborSignal(p);
            if(powered!=flag(s,"powered")){
                var next=set(s,"powered",Boolean.toString(powered));next=set(next,"lit",Boolean.toString(powered));next=set(next,"enabled",Boolean.toString(powered));
                if(name.endsWith("fence_gate"))next=set(next,"open",Boolean.toString(powered));l.setBlock(p,next,UPDATE_ALL);
            }
        }
    }
    @Override protected boolean isSignalSource(BlockState s){return kind.equals("bell")||name.endsWith("lightswitch");}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return flag(s,"powered")||flag(s,"enabled")?15:0;}
    @Override public void fallOn(Level l,BlockState s,BlockPos p,Entity e,double distance){if(!kind.equals("trampoline")||e.isSuppressingBounce())super.fallOn(l,s,p,e,distance);}
}

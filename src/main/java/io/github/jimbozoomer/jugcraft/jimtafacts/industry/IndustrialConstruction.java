package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry;
import io.github.jimbozoomer.jugcraft.chemistry.FoamSprayerItem;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Original construction art with curing, painting and source scaffold support limits. */
public final class IndustrialConstruction {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<DyeColor, Block> WALLS = new EnumMap<>(DyeColor.class);
    public static Block FOAM, REINFORCED_FOAM, REINFORCED_STONE, ALLOY_GLASS, ALLOY_DOOR, IRON_FENCE;
    public static Item SPRAYER, PAINTER, POWDER;
    private IndustrialConstruction() {}
    public static void register() {
        for (var color : DyeColor.values()) WALLS.put(color, block("cf_wall_" + color.getName(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3,30), Block::new));
        REINFORCED_STONE = block("reinforced_stone", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(40,1200), Block::new);
        FOAM = block("construction_foam", BlockBehaviour.Properties.ofFullCopy(Blocks.WOOL.white()).noCollision().noOcclusion().strength(.01F), p -> new Foam(p, false));
        REINFORCED_FOAM = block("reinforced_foam", BlockBehaviour.Properties.ofFullCopy(FOAM), p -> new Foam(p, true));
        for (String material : List.of("wood", "iron")) for (boolean reinforced : List.of(false, true)) {
            int strength = material.equals("wood") ? reinforced ? 5 : 2 : reinforced ? 12 : 5;
            String name = material + "_scaffold" + (reinforced ? "_reinforced" : "");
            block(name, BlockBehaviour.Properties.ofFullCopy(material.equals("wood") ? Blocks.OAK_PLANKS : Blocks.IRON_BLOCK).noOcclusion().strength(.8F),
                    p -> new Scaffold(p, strength, material, reinforced));
        }
        ALLOY_GLASS = block("alloy_glass", BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(3,100).noOcclusion(), p -> new TransparentBlock(p) {});
        ALLOY_DOOR = block("alloy_door", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(40,1200), p -> new DoorBlock(BlockSetType.IRON, p) {});
        IRON_FENCE = block("iron_fence", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(3,30), p -> new IronBarsBlock(p) {});
        SPRAYER = item("cf_sprayer", p -> new Sprayer(p.stacksTo(1)));
        PAINTER = item("painter", p -> new Painter(p.stacksTo(1)));
        POWDER = item("cf_powder", Item::new);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> BLOCKS.values().forEach(out::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> { out.accept(SPRAYER); out.accept(PAINTER); out.accept(POWDER); });
    }
    private static Block block(String name, BlockBehaviour.Properties properties, Function<BlockBehaviour.Properties, Block> factory) {
        String id = "jimta_" + name; var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key))); BLOCKS.put(name, block);
        var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id)); var p = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, itemKey, name.equals("alloy_door") ? new DoubleHighBlockItem(block,p) : new BlockItem(block,p)); return block;
    }
    private static Item item(String name, Function<Item.Properties, Item> factory) {
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name)); return Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key)));
    }
    public static boolean cure(ServerLevel level, BlockPos pos, boolean reinforced) {
        var next = (reinforced ? REINFORCED_STONE : WALLS.get(DyeColor.LIGHT_GRAY)).defaultBlockState();
        // Soft foam may surround a player; never turn it solid through an entity.
        if (!level.getEntities((net.minecraft.world.entity.Entity)null, new AABB(pos), entity -> !entity.isSpectator()).isEmpty()) return false;
        return level.setBlockAndUpdate(pos, next);
    }
    public static final class Foam extends Block {
        final boolean reinforced;
        Foam(Properties p, boolean reinforced) { super(p); this.reinforced = reinforced; }
        @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) { level.scheduleTick(pos,this,500); }
        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(1000) < level.getMaxLocalRawBrightness(pos) * 6 && cure(level,pos,reinforced)) return;
            level.scheduleTick(pos,this,500);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(Items.SAND)) return InteractionResult.PASS;
            if (!Authority.mayChange(level,player,pos)) return InteractionResult.FAIL;
            if (level instanceof ServerLevel server && cure(server,pos,reinforced)) { stack.consume(1,player); return InteractionResult.SUCCESS; }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
    }
    public static final class Scaffold extends Block {
        public static final IntegerProperty SUPPORT = IntegerProperty.create("support",0,12);
        final int strength; final String material; final boolean reinforced;
        Scaffold(Properties p,int strength,String material,boolean reinforced) {
            super(p); this.strength=strength;this.material=material;this.reinforced=reinforced;
            registerDefaultState(stateDefinition.any().setValue(SUPPORT,strength));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(SUPPORT); }
        @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return Block.box(1,0,1,15,16,15); }
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved) { l.scheduleTick(p,this,1); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource random) { ticks.scheduleTick(p,this,1);return s; }
        public int support(LevelReader level,BlockPos pos) {
            if (!level.hasChunkAt(pos.below())) return -1;
            var below=level.getBlockState(pos.below());
            if (!(below.getBlock() instanceof Scaffold) && below.isFaceSturdy(level,pos.below(),Direction.UP)) return strength;
            int best=below.getBlock() instanceof Scaffold ? below.getValue(SUPPORT) : -1;
            for (var direction:Direction.Plane.HORIZONTAL) {
                var adjacent=pos.relative(direction);if(!level.hasChunkAt(adjacent))continue;
                var state=level.getBlockState(adjacent);if(state.getBlock() instanceof Scaffold)best=Math.max(best,state.getValue(SUPPORT)-1);
            }
            return Math.min(strength,best);
        }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random) {
            int support=support(l,p);
            if(support<0)l.destroyBlock(p,true);else if(s.getValue(SUPPORT)!=support)l.setBlockAndUpdate(p,s.setValue(SUPPORT,support));
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
            int cost=material.equals("wood")?2:1;
            if(reinforced || stack.getCount()<cost || !(material.equals("wood")?stack.is(Items.STICK):stack.is(IRON_FENCE.asItem())))return InteractionResult.PASS;
            if(!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(!level.isClientSide()){level.setBlockAndUpdate(pos,BLOCKS.get(material+"_scaffold_reinforced").defaultBlockState());stack.consume(cost,player);}return InteractionResult.SUCCESS;
        }
    }
    public static final class Sprayer extends Item {
        Sprayer(Properties properties) { super(properties); }
        @Override public InteractionResult useOn(UseOnContext context) {
            var player=context.getPlayer();if(player==null||player.isSpectator())return InteractionResult.FAIL;
            if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            var at=context.getClickedPos();var state=level.getBlockState(at);int available=FoamSprayerItem.foamLeft(player);
            if(available<=0||!Authority.mayChange(level,player,at))return InteractionResult.FAIL;
            if(state.getBlock() instanceof Scaffold scaffold){
                level.setBlockAndUpdate(at,(scaffold.material.equals("iron")?REINFORCED_FOAM:FOAM).defaultBlockState());FoamSprayerItem.useFoam(player,1);return InteractionResult.SUCCESS;
            }
            var start=state.canBeReplaced()?at:at.relative(context.getClickedFace());
            var fill=FoamSprayerItem.fill(level,player,start,Math.min(ConstructionChemistry.SPRAY_BLOCKS,available));
            if(fill.isEmpty())return InteractionResult.FAIL;
            for(var pos:fill)level.setBlockAndUpdate(pos,FOAM.defaultBlockState());FoamSprayerItem.useFoam(player,fill.size());return InteractionResult.SUCCESS;
        }
    }
    public static final class Painter extends Item {
        Painter(Properties p){super(p);}
        private static int paint(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("jimtafacts_paint",0);}
        public static DyeColor color(ItemStack stack){return DyeColor.byId(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("jimtafacts_paint_color",0));}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var dye=player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            if(!(dye.getItem() instanceof DyeItem item)||player.isSpectator())return InteractionResult.PASS;
            if(!level.isClientSide()){
                var stack=player.getItemInHand(hand);var color=item.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE);
                CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->{tag.putInt("jimtafacts_paint",32);tag.putInt("jimtafacts_paint_color",color.getId());});
                stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of((float)color.getId()+1),List.of(),List.of(),List.of()));dye.consume(1,player);
            }return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext context){
            var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();var stack=context.getItemInHand();
            if(player==null||player.isSpectator()||paint(stack)<=0||!WALLS.containsValue(level.getBlockState(pos).getBlock()))return InteractionResult.PASS;
            if(!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
            var target=WALLS.get(color(stack));if(level.getBlockState(pos).is(target))return InteractionResult.PASS;
            if(!level.isClientSide()){
                level.setBlockAndUpdate(pos,target.defaultBlockState());int left=paint(stack)-1;CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putInt("jimtafacts_paint",left));
                if(left==0)stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            }return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            lines.accept(Component.literal("Paint: "+paint(stack)+" / 32 · "+color(stack).getName()));lines.accept(Component.literal("Use with dye in the other hand, then paint hardened CF walls."));
        }
    }
}

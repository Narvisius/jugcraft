package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.HitResult;

/** Source seven-stage water gel, with bounded loaded-only spreading and persistent claim authority. */
public final class FactoryWaterGel {
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,6);
    public static Block BLOCK;
    public static Item SEED,GEL;
    public static BlockEntityType<Node> TYPE;
    private FactoryWaterGel(){}
    public static void register(){
        var blockKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_water_gel"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,blockKey,new GelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK).strength(.6F).setId(blockKey)));
        var seedKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_water_gel_seed"));
        SEED=Registry.register(BuiltInRegistries.ITEM,seedKey,new Seed(new Item.Properties().setId(seedKey).stacksTo(16)));
        var gelKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_gelled_water"));
        GEL=Registry.register(BuiltInRegistries.ITEM,gelKey,new Item(new Item.Properties().setId(gelKey)));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_water_gel"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(o->{o.accept(SEED);o.accept(GEL);});
    }
    private static boolean source(ServerLevel level,BlockPos p){
        return level.hasChunkAt(p)&&level.getBlockState(p).is(Blocks.WATER)&&level.getFluidState(p).isSource();
    }
    public static boolean start(ServerLevel level,Player player,BlockPos pos){
        if(!player.isAlive()||player.isSpectator()||!source(level,pos)||!Authority.mayChange(level,player,pos))return false;
        return place(level,pos,0,player.getUUID(),pos);
    }
    private static boolean place(ServerLevel level,BlockPos pos,int stage,UUID owner,BlockPos origin){
        if(!level.setBlock(pos,BLOCK.defaultBlockState().setValue(STAGE,stage),Block.UPDATE_ALL))return false;
        if(level.getBlockEntity(pos) instanceof Node n){n.owner=owner;n.origin=origin.immutable();n.setChanged();}
        level.scheduleTick(pos,BLOCK,(stage<4?200:400)+level.getRandom().nextInt(150));return true;
    }
    private static final class Seed extends Item {
        Seed(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player.isSpectator()||!player.isAlive())return InteractionResult.FAIL;
            var ray=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(7)),ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
            if(ray.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;
            if(level instanceof ServerLevel server){
                if(!start(server,player,ray.getBlockPos()))return InteractionResult.FAIL;
                player.getItemInHand(hand).consume(1,player);
                level.playSound(null,ray.getBlockPos(),net.minecraft.sounds.SoundEvents.SLIME_BLOCK_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.6F,.8F);
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> lines,TooltipFlag f){
            lines.accept(Component.literal("Use on source water. Gel spreads locally, then sets when separated from water."));
            lines.accept(Component.literal("Break gel to recover portable water. Spread pauses while its owner is offline."));
        }
    }
    private static final class GelBlock extends Block implements EntityBlock {
        GelBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(STAGE,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(STAGE);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
            if(l.getBlockEntity(p) instanceof Node n)n.step(l);
        }
    }
    public static final class Node extends BlockEntity {
        public UUID owner;
        public BlockPos origin;
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);origin=p;}
        public void step(ServerLevel server){
            if(server.getBlockEntity(worldPosition)!=this||!getBlockState().is(BLOCK))return;
            int stage=getBlockState().getValue(STAGE);if(stage>=6)return;
            var player=Authority.present(server,owner);
            if(player==null||!player.isAlive()||!Authority.mayChange(server,player,worldPosition)){
                server.scheduleTick(worldPosition,BLOCK,200);return;
            }
            if(stage<4){
                var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();var selected=new ArrayList<BlockPos>();
                seen.add(worldPosition);queue.add(worldPosition);
                // Four finite stages, at most three new sources per stage. No chunk loads and no 10,000-cell source scan.
                int visited=0;
                while(!queue.isEmpty()&&visited++<64&&selected.size()<3){
                    var p=queue.removeFirst();
                    for(Direction direction:Direction.values()){
                        var next=p.relative(direction);
                        if(seen.size()>=128||!seen.add(next)||Math.abs((long)next.getX()-origin.getX())>12||Math.abs((long)next.getY()-origin.getY())>12||Math.abs((long)next.getZ()-origin.getZ())>12||!server.hasChunkAt(next)||!Authority.mayChange(server,player,next))continue;
                        var state=server.getBlockState(next);
                        if(state.is(Blocks.WATER)){
                            if(source(server,next))selected.add(next);else queue.addLast(next);
                        }else if(state.is(BLOCK)&&server.getBlockEntity(next) instanceof Node gel&&Objects.equals(owner,gel.owner)&&origin.equals(gel.origin))queue.addLast(next);
                        if(selected.size()==3)break;
                    }
                }
                for(var p:selected)if(source(server,p)&&Authority.mayChange(server,player,p))place(server,p,stage+1,owner,origin);
            }else{
                // Do not assume an unloaded neighbor is dry.
                for(Direction d:Direction.values()){
                    var p=worldPosition.relative(d);
                    if(!server.hasChunkAt(p)||server.getBlockState(p).is(Blocks.WATER)){server.scheduleTick(worldPosition,BLOCK,600);return;}
                }
            }
            server.setBlock(worldPosition,getBlockState().setValue(STAGE,stage+1),Block.UPDATE_ALL);
            if(stage+1<6)server.scheduleTick(worldPosition,BLOCK,stage+1<4?200:400);
        }
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.store("origin",BlockPos.CODEC,origin);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);origin=in.read("origin",BlockPos.CODEC).orElse(worldPosition);}
    }
}

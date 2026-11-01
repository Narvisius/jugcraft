package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Decorative camouflage never changes the underlying block's identity or drops. */
public final class BotanicalPlatforms {
    public static final Set<String> TYPES=Set.of("abstruse_platform","spectral_platform","infrangible_platform");
    private final BotanicalWorkshop host;private BlockState disguise;
    BotanicalPlatforms(BotanicalWorkshop host){this.host=host;}
    public BlockState disguise(){return disguise;}
    private boolean editable(ServerPlayer actor){return host.permitted(actor)&&(!host.kind().equals("infrangible_platform")||actor.hasInfiniteMaterials());}
    private static boolean valid(BlockState state){return state!=null&&!state.isAir()&&!state.hasBlockEntity()&&!(state.getBlock() instanceof BotanicalBlock)&&state.getRenderShape()==RenderShape.MODEL&&state.getFluidState().isEmpty();}
    InteractionResult use(ServerPlayer p,InteractionHand hand,BlockHitResult hit){
        if(!editable(p))return InteractionResult.FAIL;var held=p.getItemInHand(hand);
        if(held.is(Jugtania.item("phantom_ink"))){if(disguise!=Blocks.BARRIER.defaultBlockState()){disguise=Blocks.BARRIER.defaultBlockState();if(!p.hasInfiniteMaterials())held.shrink(1);host.changed();}return InteractionResult.SUCCESS;}
        if(!(held.getItem() instanceof BlockItem block))return InteractionResult.PASS;
        var context=new BlockPlaceContext(p,hand,held,hit){@Override public BlockPos getClickedPos(){return host.getBlockPos();}};
        var state=block.getBlock().getStateForPlacement(context);if(!valid(state))return InteractionResult.FAIL;disguise=state;host.changed();return InteractionResult.SUCCESS;
    }
    void wand(ServerPlayer actor){
        if(!editable(actor))return;boolean clear=disguise==null||actor.isShiftKeyDown();var replacement=clear?null:disguise;
        var queue=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();var selected=new ArrayList<BotanicalWorkshop>();queue.add(host.getBlockPos());
        // A bounded preflight avoids both recursive overflow and half-painted protected groups.
        while(!queue.isEmpty()){
            var at=queue.removeFirst();if(!visited.add(at))continue;if(!actor.level().hasChunkAt(at))continue;
            if(!(actor.level().getBlockEntity(at) instanceof BotanicalWorkshop other)||!other.kind().equals(host.kind()))continue;
            if(other!=host&&(clear?other.platforms.disguise==null:other.platforms.disguise!=null))continue;
            if(!other.platforms.editable(actor)||!ClaimTransfers.allowed(actor.level(),host.getBlockPos(),at))return;
            if(selected.size()==512){actor.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Platform group exceeds 512 blocks; split it before copying camouflage."));return;}
            selected.add(other);for(var face:Direction.values())queue.add(at.relative(face));
        }
        for(var other:selected){other.platforms.disguise=replacement;other.changed();}
    }
    void save(ValueOutput out){if(disguise!=null)out.store("platform_disguise",BlockState.CODEC,disguise);}
    void load(ValueInput in){disguise=in.read("platform_disguise",BlockState.CODEC).filter(s->s.is(Blocks.BARRIER)||valid(s)).orElse(null);}
    public static final class Platform extends BotanicalBlock {
        Platform(String name,Properties p){super(name,name.equals("infrangible_platform")?p.strength(-1,3600000):p);}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
            if(stack.is(Jugtania.item("twig_wand")))return InteractionResult.PASS;
            return player instanceof ServerPlayer p&&level.getBlockEntity(at) instanceof BotanicalWorkshop w?w.platforms.use(p,hand,hit):InteractionResult.SUCCESS;
        }
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){return level.getBlockEntity(at) instanceof BotanicalWorkshop w&&w.platforms.disguise!=null&&!w.platforms.disguise.is(Blocks.BARRIER)?w.platforms.disguise.getShape(level,at,context):Shapes.block();}
        @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){
            if(kind.equals("spectral_platform"))return Shapes.empty();
            if(kind.equals("abstruse_platform")&&context instanceof EntityCollisionContext ec){Entity entity=ec.getEntity();if(entity!=null&&(entity.getY()<=at.getY()+.9||context.isDescending()))return Shapes.empty();}
            return Shapes.block();
        }
        @Override protected VoxelShape getOcclusionShape(BlockState state){return Shapes.empty();}
    }
}

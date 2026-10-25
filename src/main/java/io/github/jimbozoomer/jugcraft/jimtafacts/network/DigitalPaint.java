package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Supplied paintballs, shimmer and three splotch textures. Paint never mines or overwrites a solid block. */
public final class DigitalPaint {
    public static final List<String> ITEMS = new ArrayList<>();
    public static Block PAINT;
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final EnumProperty<Direction> FACE = BlockStateProperties.FACING;
    public static final BooleanProperty LUMEN = BooleanProperty.create("lumen");
    public static final IntegerProperty PATTERN = IntegerProperty.create("pattern", 0, 2);
    private DigitalPaint() {}
    public static void register() {
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_paint"));
        PAINT = Registry.register(BuiltInRegistries.BLOCK, key, new Splotch(BlockBehaviour.Properties.of().setId(key).strength(0).noOcclusion().noCollision().lightLevel(s -> s.getValue(LUMEN) ? 15 : 0)));
        DigitalStorage.BLOCKS.put("paint", PAINT);
        var paintKey = ResourceKey.create(Registries.ITEM, key.identifier()); Registry.register(BuiltInRegistries.ITEM, paintKey, new BlockItem(PAINT, new Item.Properties().setId(paintKey).useBlockDescriptionPrefix()));
        for (var color : DyeColor.values()) for (boolean lumen : new boolean[]{false, true}) {
            String name = color.getName() + (lumen ? "_lumen_paint_ball" : "_paint_ball"); ITEMS.add(name);
            var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, itemKey, new Ball(new Item.Properties().setId(itemKey), color, lumen)));
        }
    }
    public static final class Ball extends Item {
        public final DyeColor color; public final boolean lumen;
        Ball(Properties p, DyeColor color, boolean lumen) { super(p); this.color = color; this.lumen = lumen; }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (!(c.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            if (!paint(p, c.getClickedPos(), c.getClickedFace(), color, lumen)) return InteractionResult.FAIL;
            if (!p.isCreative()) c.getItemInHand().shrink(1); return InteractionResult.SUCCESS;
        }
    }
    public static boolean paint(ServerPlayer p, BlockPos support, Direction outward, DyeColor color, boolean lumen) {
        var level = p.level(); var target = support.relative(outward);
        if (!p.isAlive() || p.isSpectator() || !level.isLoaded(target) || !level.isLoaded(support) || !Authority.mayChange(level, p, support) || !Authority.mayChange(level, p, target)) return false;
        var old = level.getBlockState(target); if (!old.isAir() && !old.is(PAINT)) return false;
        if (!Block.canSupportCenter(level, support, outward)) return false;
        return level.setBlockAndUpdate(target, PAINT.defaultBlockState().setValue(FACE, outward.getOpposite()).setValue(COLOR, color).setValue(LUMEN, lumen).setValue(PATTERN, level.getRandom().nextInt(3)));
    }
    public static boolean fire(ServerPlayer p, Ball ball) {
        Vec3 from = p.getEyePosition(), direction = p.getLookAngle(), to = from;
        for (int i = 1; i <= 64; i++) { var next = from.add(direction.scale(i)); if (!p.level().isLoaded(BlockPos.containing(next))) break; to = next; }
        var hit = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.BLOCK) return paint(p, hit.getBlockPos(), hit.getDirection(), ball.color, ball.lumen);
        return false;
    }
    public static final class Splotch extends Block {
        Splotch(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACE, Direction.NORTH).setValue(COLOR, DyeColor.WHITE).setValue(LUMEN, false).setValue(PATTERN, 0)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACE, COLOR, LUMEN, PATTERN); }
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) { return defaultBlockState().setValue(FACE, c.getClickedFace().getOpposite()); }
        @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos pos) { var face = s.getValue(FACE); return Block.canSupportCenter(l, pos.relative(face), face.getOpposite()); }
        @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess ticks, BlockPos pos, Direction d, BlockPos at, BlockState neighbor, RandomSource random) { return s.canSurvive(l, pos) ? s : Blocks.AIR.defaultBlockState(); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos at, CollisionContext c) { return switch (s.getValue(FACE)) {
            case NORTH -> Block.box(5, 5, 0, 11, 11, .2); case SOUTH -> Block.box(5, 5, 15.8, 11, 11, 16); case DOWN -> Block.box(5, 0, 5, 11, .2, 11); case UP -> Block.box(5, 15.8, 5, 11, 16, 11); case WEST -> Block.box(0, 5, 5, .2, 11, 11); case EAST -> Block.box(15.8, 5, 5, 16, 11, 11); }; }
    }
}

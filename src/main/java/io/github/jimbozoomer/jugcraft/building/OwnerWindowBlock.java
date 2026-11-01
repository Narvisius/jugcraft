package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Original window meshes with per-position states, bounded groups and claim checks. */
final class OwnerWindowBlock extends Block {
    // Block builds its state definition inside super(). This scoped construction
    // context supplies the authored schema; it never stores world/gameplay state.
    private static final ThreadLocal<Map<String, Choice>> CONSTRUCTION = new ThreadLocal<>();
    private static final BooleanProperty MANUAL = BooleanProperty.create("manual");
    private final Map<String, Choice> choices;
    private final String kind;
    private final OwnerAuthoredShapes shapes;
    static OwnerWindowBlock create(Properties properties, JsonObject descriptor) {
        Map<String, Choice> schema = new LinkedHashMap<>();
        descriptor.getAsJsonObject("properties").entrySet().forEach(entry -> {
            List<String> values = new ArrayList<>();
            entry.getValue().getAsJsonArray().forEach(value -> values.add(value.getAsString()));
            schema.put(entry.getKey(), new Choice(entry.getKey(), values));
        });
        if (CONSTRUCTION.get() != null) throw new IllegalStateException("Nested window construction");
        CONSTRUCTION.set(Map.copyOf(schema));
        try { return new OwnerWindowBlock(properties, descriptor); }
        finally { CONSTRUCTION.remove(); }
    }
    private OwnerWindowBlock(Properties properties, JsonObject descriptor) {
        super(properties);
        choices = CONSTRUCTION.get(); kind = descriptor.get("kind").getAsString();
        BlockState state = defaultBlockState().setValue(MANUAL, false);
        for (var entry : Map.of("facing", "north", "part", "base", "windowstate", "closed",
                "blindsstate", "closed", "tied", "none", "open", "false", "hinge", "left").entrySet())
            state = set(state, entry.getKey(), entry.getValue());
        registerDefaultState(state);
        shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Choice property : Objects.requireNonNull(CONSTRUCTION.get()).values()) builder.add(property);
        builder.add(MANUAL);
    }
    private String value(BlockState state, String name) {
        Choice property = choices.get(name); return property == null ? "" : state.getValue(property);
    }
    private BlockState set(BlockState state, String name, String value) {
        Choice property = choices.get(name);
        return property != null && property.getPossibleValues().contains(value) ? state.setValue(property, value) : state;
    }
    private BlockState cycle(BlockState state, String name, boolean backwards) {
        Choice property = choices.get(name); if (property == null) return state;
        var values = property.getPossibleValues();
        return state.setValue(property, values.get(Math.floorMod(values.indexOf(state.getValue(property)) + (backwards ? -1 : 1), values.size())));
    }
    private Direction facing(BlockState state) {
        return switch (value(state, "facing")) { case "east" -> Direction.EAST; case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST; default -> Direction.NORTH; };
    }
    private boolean aligned(BlockGetter level, BlockPos pos, BlockState state) {
        var other = level.getBlockState(pos);
        return other.is(this) && value(state, "facing").equals(value(other, "facing"));
    }
    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        if (!choices.containsKey("part") || state.getValue(MANUAL)) return state;
        boolean up = aligned(level, pos.above(), state), down = aligned(level, pos.below(), state);
        if (kind.equals("window_parapet"))
            return set(state, "part", level.getBlockState(pos.above()).is(BlockTags.FLOWER_POTS) ? "flower" : "empty");
        if (kind.equals("window_blinds")) return set(state, "part", up ? "bottom" : "top");
        if (kind.equals("window_openable")) return set(state, "part", up ? (down ? "middle" : "below") : (down ? "above" : "base"));
        String vertical = up ? (down ? "middle" : "bottom") : (down ? "top" : "base");
        if (!kind.equals("window_connected")) return set(state, "part", vertical);
        Direction left = facing(state).getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        boolean l = aligned(level, pos.relative(left), state), r = aligned(level, pos.relative(left.getOpposite()), state);
        if (!l && !r) return set(state, "part", vertical);
        String row = up ? (down ? "mid" : "bot") : (down ? "top" : "single");
        return set(state, "part", row + (l ? (r ? "_m" : "_l") : "_r"));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Direction direction = context.getHorizontalDirection().getOpposite();
        Choice facing = choices.get("facing");
        if (facing != null && facing.getPossibleValues().size() == 2)
            direction = direction.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        state = set(state, "facing", direction.getName());
        double x = context.getClickLocation().x - context.getClickedPos().getX();
        double z = context.getClickLocation().z - context.getClickedPos().getZ();
        boolean right = switch (direction) { case NORTH -> x > .5; case SOUTH -> x < .5;
            case EAST -> z > .5; default -> z < .5; };
        state = set(state, "hinge", right ? "right" : "left");
        return connected(state, context.getLevel(), context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return connected(state, level, pos);
    }
    private boolean mayUse(ServerLevel level, Player player, BlockPos pos) {
        return Authority.mayChange(level, player, pos) || TownProtection.allowsPublicUse(player, level, pos);
    }
    private List<BlockPos> column(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> members = new ArrayList<>(); members.add(pos.immutable());
        if (!kind.equals("window_openable") && !kind.equals("window_blinds")) return members;
        for (Direction direction : List.of(Direction.UP, Direction.DOWN)) {
            BlockPos next = pos.relative(direction);
            while (level.hasChunkAt(next) && aligned(level, next, state)) {
                if (members.size() == 64) return List.of(); // Reject oversized groups, never update only half.
                members.add(next); next = next.relative(direction);
            }
        }
        return members;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        boolean key = stack.is(OwnerBuildingCollection.ITEMS.get("windowworks_key"));
        boolean hammer = stack.is(OwnerBuildingCollection.ITEMS.get("windowworks_hammer"));
        if (!key && !hammer) return InteractionResult.PASS;
        if (hammer && choices.containsKey("part") && !kind.equals("window_blinds")) {
            if (level instanceof ServerLevel server) {
                if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos, player.isShiftKeyDown() ? connected(state.setValue(MANUAL, false), level, pos)
                    : cycle(state, "part", false).setValue(MANUAL, true));
            }
            return InteractionResult.SUCCESS;
        }
        if (key && choices.containsKey("windowstate")) return operate(state, level, pos, player, hit, true);
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return operate(state, level, pos, player, hit, false);
    }
    private InteractionResult operate(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, boolean key) {
        String property, target;
        if (choices.containsKey("windowstate")) {
            property = "windowstate"; String current = value(state, property);
            if (key) target = current.equals("locked") ? "closed" : "locked";
            else if (current.equals("locked")) return InteractionResult.FAIL;
            else if (current.startsWith("open")) target = "closed";
            else {
                double offset = facing(state).getAxis() == Direction.Axis.Z ? hit.getLocation().x - pos.getX() : hit.getLocation().z - pos.getZ();
                target = offset < .5 ? "open_left" : "open_right";
            }
        } else if (choices.containsKey("blindsstate")) {
            property = "blindsstate"; target = switch (value(state, property)) { case "closed" -> "open"; case "open" -> "raised"; default -> "closed"; };
        } else if (choices.containsKey("open")) { property = "open"; target = value(state, property).equals("true") ? "false" : "true"; }
        else if (choices.containsKey("tied")) { property = "tied"; target = value(cycle(state, property, player.isShiftKeyDown()), property); }
        else return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            List<BlockPos> members = column(level, pos, state);
            if (members.isEmpty()) return InteractionResult.FAIL;
            for (BlockPos member : members) {
                if (key ? !Authority.mayChange(server, player, member) : !mayUse(server, player, member)) return InteractionResult.FAIL;
                if (!key && value(level.getBlockState(member), "windowstate").equals("locked")) return InteractionResult.FAIL;
            }
            for (BlockPos member : members) level.setBlock(member, set(level.getBlockState(member), property, target), Block.UPDATE_CLIENTS);
            for (BlockPos member : members) level.updateNeighborsAt(member, this);
            level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, .5F, 1);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = shapes == null ? Shapes.block() : shapes.get(state);
        // Raised lower blinds keep a selectable string so they can be lowered again.
        return shape.isEmpty() ? Block.box(7, 0, 7, 9, 16, 9) : shape;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (value(state, "windowstate").startsWith("open") || value(state, "blindsstate").equals("raised")
                || kind.equals("window_curtain")) return Shapes.empty();
        return shapes == null ? Shapes.block() : shapes.get(state);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        if (!choices.containsKey("facing")) return state;
        Direction next = rotation.rotate(facing(state));
        if (choices.get("facing").getPossibleValues().size() == 2) next = next.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        return set(state, "facing", next.getName());
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        BlockState result = rotate(state, mirror.getRotation(facing(state)));
        String hinge = value(result, "hinge");
        if (!hinge.isEmpty()) result = set(result, "hinge", hinge.equals("left") ? "right" : "left");
        String opened = value(result, "windowstate");
        if (opened.startsWith("open")) result = set(result, "windowstate", opened.equals("open_left") ? "open_right" : "open_left");
        return result;
    }
    private static final class Choice extends Property<String> {
        private final List<String> values;
        Choice(String name, List<String> values) { super(name, String.class); this.values = List.copyOf(values); }
        @Override public List<String> getPossibleValues() { return values; }
        @Override public String getName(String value) { return value; }
        @Override public Optional<String> getValue(String value) { return values.contains(value) ? Optional.of(value) : Optional.empty(); }
        @Override public int getInternalIndex(String value) { return values.indexOf(value); }
        @Override public boolean equals(Object other) { return this == other || other instanceof Choice choice && super.equals(other) && values.equals(choice.values); }
        @Override public int generateHashCode() { return 31 * super.generateHashCode() + values.hashCode(); }
    }
}

package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
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

/** Supplied October 29 architecture on native placement, redstone and shape rules. */
public final class OwnerBuildingCollection {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private OwnerBuildingCollection() { }
    public static void register() {
        OwnerTwigs.registerSounds();
        JsonObject catalog;
        Map<String, List<Block>> carvingFamilies = new LinkedHashMap<>();
        try (var in = OwnerBuildingCollection.class.getResourceAsStream("/jugcraft/newest_building.json")) {
            catalog = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException("Owner building catalog", e); }
        for (var entry : catalog.getAsJsonObject("blocks").entrySet()) {
            String id = entry.getKey(); var d = entry.getValue().getAsJsonObject();
            String kind = d.get("kind").getAsString(); boolean wood = d.get("wood").getAsBoolean();
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
            var properties = BlockBehaviour.Properties.ofFullCopy(wood ? Blocks.OAK_PLANKS : Blocks.STONE).setId(key);
            if (d.get("collection").getAsString().equals("factoryworks"))
                properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key);
            if (d.has("base")) {
                var base = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(d.get("base").getAsString()));
                if (base == null || base == Blocks.AIR) throw new IllegalStateException("Unknown owner building material " + d.get("base"));
                properties = BlockBehaviour.Properties.ofFullCopy(base).setId(key);
            }
            if (kind.equals("holiday_leaves")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).setId(key);
            if (kind.equals("holiday_grass") || kind.equals("holiday_plant")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).setId(key);
            if (kind.equals("holiday_soil")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).setId(key);
            if (kind.equals("holiday_web")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB).setId(key);
            if (kind.equals("holiday_snow")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW).setId(key);
            if (d.has("soft")) properties.strength(.2F);
            if (kind.equals("twig_flowers") || kind.equals("twig_lichen")) properties.noCollision().instabreak();
            if (kind.equals("twig_lantern")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).setId(key);
            if (kind.equals("twig_pot")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.DECORATED_POT).setId(key);
            if (kind.equals("twig_copper")) properties = BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).setId(key).randomTicks();
            if (d.has("light")) { int brightness = d.get("light").getAsInt(); properties.lightLevel(state -> brightness); }
            if (d.has("lamp")) properties.lightLevel(state -> io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock.flag(state, "lit") ? 15 : 0);
            if (!kind.equals("block") && !kind.equals("pillar")) properties.noOcclusion();
            if (kind.startsWith("light_") && !kind.equals("light_chain")) {
                int brightness = d.get("soul").getAsBoolean() ? 10 : 15;
                properties.lightLevel(state -> !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT) ? brightness : 0);
            }
            Block block = switch (kind) {
                case "slab" -> new SlabBlock(properties);
                case "stairs" -> new StairBlock((wood ? Blocks.OAK_PLANKS : Blocks.STONE).defaultBlockState(), properties);
                case "wall" -> new WallBlock(properties);
                case "pillar" -> new RotatedPillarBlock(properties);
                case "powered_pillar" -> new OwnerChisel.PoweredPillar(properties);
                case "powered_block" -> new PoweredBlock(properties);
                case "transparent" -> new TransparentBlock(properties);
                case "ice" -> new IceBlock(properties);
                case "ice_pillar" -> new OwnerChisel.IcePillar(properties);
                case "door" -> new DoorBlock(d.get("iron").getAsBoolean() ? BlockSetType.IRON : BlockSetType.OAK, properties);
                case "sliding_door" -> new SlidingDoor(properties);
                case "stable_door" -> new StableDoor(properties);
                case "garage" -> new OwnerGarageDoor(properties);
                case "window_pane" -> new IronBarsBlock(properties);
                case "authored_steps" -> new OwnerStairBlocks.Steps(properties, d);
                case "authored_platform" -> new OwnerStairBlocks.Platform(properties, d);
                case "authored_railing" -> new OwnerStairBlocks.Railing(properties, d);
                case "authored_balcony" -> new OwnerStairBlocks.Balcony(properties, d);
                case "light_short" -> new OwnerLightBlocks.Lamp(properties, d);
                case "light_wall" -> new OwnerLightBlocks.Wall(properties, d);
                case "light_column" -> new OwnerLightBlocks.Column(properties, d);
                case "light_mount" -> new OwnerLightBlocks.Mounted(properties, d);
                case "light_dyeable" -> new OwnerLightBlocks.Dyeable(properties, d);
                case "light_lantern" -> new OwnerLightBlocks.Lantern(properties, d);
                case "light_chain" -> new ChainBlock(properties);
                case "light_slab" -> new SlabBlock(properties);
                case "joined_furniture" -> furniture(properties, d);
                case "home_decor" -> home(properties.noOcclusion(), d);
                case "twig_amethyst" -> new AmethystBlock(properties);
                case "twig_copper" -> new OwnerTwigs.Copper(properties, id);
                case "twig_flowers" -> new MultifaceBlock(properties);
                case "twig_lichen" -> new GlowLichenBlock(properties);
                case "twig_potted" -> new FlowerPotBlock(BLOCKS.get("twigworks_azalea_flowers"), properties);
                case "twig_pot" -> new OwnerSiltPot(properties);
                case "twig_lantern" -> new OwnerLightBlocks.Lantern(properties, d);
                case "twig_decor" -> twigs(properties, d);
                case "holiday_decor" -> seasonal(properties, d);
                case "holiday_pumpkin" -> new CarvedPumpkinBlock(properties);
                case "holiday_leaves" -> new TintedParticleLeavesBlock(.01F, properties);
                case "holiday_soil" -> new GrassBlock(properties);
                case "holiday_grass" -> new OwnerHolidayPlants.Grass(properties, id);
                case "holiday_plant" -> new OwnerHolidayPlants.Tall(properties);
                case "holiday_snow" -> new OwnerHolidayPlants.Snow(properties);
                case "holiday_web" -> new WebBlock(properties);
                case "window_blinds", "window_curtain", "window_shutter", "window_openable", "window_parapet",
                     "window_connected", "window_column", "window_rod" -> OwnerWindowBlock.create(properties, d);
                case "bridge_deck" -> new OwnerBridgeBlocks.Deck(properties, d);
                case "bridge_rope" -> new OwnerBridgeBlocks.Rope(properties, d);
                case "bridge_stairs" -> new OwnerBridgeBlocks.Stairs(properties, d);
                case "bridge_support" -> new OwnerBridgeBlocks.Support(properties, d);
                case "bridge_rail" -> new OwnerBridgeBlocks.Rail(properties, d);
                case "bridge_light" -> new OwnerBridgeBlocks.Light(properties, d);
                case "path" -> new LowPath(properties);
                case "facing_path" -> new FacingPath(properties);
                case "engraved" -> new Engraved(properties);
                case "flattened" -> new Flattened(properties);
                case "factory_facing" -> new OwnerFactoryBlocks.Facing(properties);
                case "powered_factory_fan" -> new OwnerFactoryBlocks.PoweredFan(properties);
                case "large_factory_fan" -> new OwnerFactoryBlocks.LargeFan(properties);
                case "fence" -> new OwnerFenceBlocks.Fence(properties, false, false);
                case "thick_fence" -> new OwnerFenceBlocks.Fence(properties, true, false);
                case "wired_fence" -> new OwnerFenceBlocks.Fence(properties, false, true);
                case "fence_gate" -> new FenceGateBlock(WoodType.OAK, properties);
                case "stacked_gate" -> new OwnerFenceBlocks.Gate(properties);
                case "stacked_fence" -> d.get("middle").getAsBoolean()
                    ? new OwnerFenceBlocks.ThreePartFence(properties) : new OwnerFenceBlocks.Stacked(properties);
                case "block" -> new Block(properties);
                default -> throw new IllegalStateException("Unimplemented building kind: " + kind);
            };
            Registry.register(BuiltInRegistries.BLOCK, key, block); BLOCKS.put(id, block);
            if (kind.equals("joined_furniture") && d.get("function").getAsString().equals("storage"))
                ((net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType)io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.KITCHEN_CABINET_ENTITY).addValidBlock(block);
            if (d.has("base")) carvingFamilies.computeIfAbsent(d.get("base").getAsString(), ignored -> new ArrayList<>()).add(block);
            var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
            if (d.has("no_item")) continue;
            var ip = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
            var item = id.equals("twigworks_bronzed_seashell") ? new OwnerTwigs.ShellItem(block, ip)
                : id.equals("twigworks_pebble") ? new OwnerTwigs.PebbleItem(block, ip) : new BlockItem(block, ip);
            Registry.register(BuiltInRegistries.ITEM, itemKey, item); ITEMS.put(id, item);
        }
        for (var entry : catalog.getAsJsonObject("items").entrySet()) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(entry.getKey()));
            var descriptor = entry.getValue().getAsJsonObject();
            var itemProperties = new Item.Properties().setId(key);
            if (descriptor.has("kind") && descriptor.get("kind").getAsString().equals("twig_novelty"))
                itemProperties.stacksTo(1).rarity(Rarity.EPIC).fireResistant();
            if (descriptor.has("kind") && descriptor.get("kind").getAsString().equals("candy_cane"))
                itemProperties.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(.2F).build());
            if (descriptor.has("kind") && descriptor.get("kind").getAsString().equals("pliers")) itemProperties.durability(256);
            var item = descriptor.has("kind") && descriptor.get("kind").getAsString().equals("chisel")
                ? new OwnerChisel(itemProperties) : new Item(itemProperties);
            Registry.register(BuiltInRegistries.ITEM, key, item); ITEMS.put(entry.getKey(), item);
        }
        OwnerChisel.finishFamilies(carvingFamilies);
        OwnerTwigs.finishRegistration();
        io.github.jimbozoomer.jugcraft.menagerie.OwnerHome.finishRegistration();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Jugcraft.id("newest_building"), FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.jugcraft.newest_building"))
            .icon(() -> new ItemStack(ITEMS.get("stoneworks_andesite_bricks")))
            .displayItems((parameters, output) -> ITEMS.values().forEach(output::accept)).build());
    }
    private static Block furniture(BlockBehaviour.Properties properties, JsonObject supplied) {
        var descriptor = supplied.deepCopy();
        descriptor.addProperty("source", supplied.get("source").getAsString().split(":", 2)[1]);
        descriptor.addProperty("collection", "newest_furniture");
        descriptor.addProperty("kind", supplied.get("function").getAsString());
        return io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock.create(properties, descriptor);
    }
    private static Block home(BlockBehaviour.Properties properties, JsonObject supplied) {
        var descriptor = supplied.deepCopy();
        descriptor.addProperty("source", supplied.get("source").getAsString().split(":", 2)[1]);
        descriptor.addProperty("collection", "newest_home");
        return io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock.create(properties, descriptor);
    }
    private static Block seasonal(BlockBehaviour.Properties properties, JsonObject supplied) {
        var descriptor = supplied.deepCopy();
        descriptor.addProperty("source", supplied.get("source").getAsString().split(":", 2)[1]);
        descriptor.addProperty("collection", "newest_holiday");
        descriptor.addProperty("kind", "decor");
        return io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock.create(properties, descriptor);
    }
    private static Block twigs(BlockBehaviour.Properties properties, JsonObject supplied) {
        var descriptor = supplied.deepCopy();
        descriptor.addProperty("source", supplied.get("source").getAsString().split(":", 2)[1]);
        descriptor.addProperty("collection", "newest_twigs"); descriptor.addProperty("kind", "decor");
        return io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock.create(properties, descriptor);
    }
    private static class LowPath extends Block {
        static final VoxelShape SHAPE = Block.box(0, .01, 0, 16, .99, 16);
        LowPath(Properties p) { super(p); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    }
    private static final class SlidingDoor extends DoorBlock {
        SlidingDoor(Properties p) { super(BlockSetType.OAK, p); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return s.getValue(FACING).getAxis() == Direction.Axis.Z ? Block.box(0, 0, 7, 16, 16, 9) : Block.box(7, 0, 0, 9, 16, 16);
        }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return s.getValue(OPEN) ? Shapes.empty() : getShape(s, l, p, c);
        }
    }
    private static final class StableDoor extends DoorBlock {
        StableDoor(Properties p) { super(BlockSetType.OAK, p); }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return s.getValue(OPEN) ? Shapes.empty() : super.getCollisionShape(s, l, p, c);
        }
    }
    private static final class FacingPath extends LowPath {
        FacingPath(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BlockStateProperties.HORIZONTAL_FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, c.getHorizontalDirection().getOpposite()); }
        @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(BlockStateProperties.HORIZONTAL_FACING, r.rotate(s.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
        @Override protected BlockState mirror(BlockState s, Mirror m) { return rotate(s, m.getRotation(s.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
    }
    private abstract static class WorkedSurface extends Block {
        WorkedSurface(Properties p) { super(p); }
        abstract BooleanProperty worked();
        abstract boolean correctTool(ItemStack stack);
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(0, 0, 0, 16, s.getValue(worked()) ? 15 : 16, 16); }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!correctTool(stack)) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos, s.cycle(worked()));
                level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1, 1);
                if (!player.isCreative()) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
    }
    private static final class Engraved extends WorkedSurface {
        static final BooleanProperty ENGRAVED = BooleanProperty.create("engraved");
        Engraved(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(ENGRAVED, false)); }
        @Override BooleanProperty worked() { return ENGRAVED; }
        @Override boolean correctTool(ItemStack s) { return s.is(ItemTags.PICKAXES); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ENGRAVED); }
    }
    private static final class Flattened extends WorkedSurface {
        static final BooleanProperty FLATTENED = BooleanProperty.create("flattened");
        Flattened(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(FLATTENED, false)); }
        @Override BooleanProperty worked() { return FLATTENED; }
        @Override boolean correctTool(ItemStack s) { return s.is(ItemTags.SHOVELS); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FLATTENED); }
    }
}

package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.logistics.BrassWrenchItem;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

/** Source tool roles translated to shared JE, protection and native item interactions. */
public final class DigitalEquipment {
    public static DataComponentType<String> PRESS_NAME;
    public static DataComponentType<Settings> MEMORY;
    public static DataComponentType<ItemStack> AMMO;
    public static final List<String> ITEMS = new ArrayList<>();
    private DigitalEquipment() {}
    public record Settings(String kind, ItemStack filter, int threshold, boolean below) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.string(1, 64).fieldOf("kind").forGetter(Settings::kind), ItemStack.OPTIONAL_CODEC.fieldOf("filter").forGetter(Settings::filter), Codec.intRange(1, 1_000_000).fieldOf("threshold").forGetter(Settings::threshold), Codec.BOOL.fieldOf("below").forGetter(Settings::below)).apply(i, Settings::new));
        public Settings { filter = filter.isEmpty() ? ItemStack.EMPTY : new ItemStack(filter.getItem()); }
        @Override public ItemStack filter() { return filter.copy(); }
    }
    private static <T> DataComponentType<T> component(String name, Codec<T> codec) { return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id(name), DataComponentType.<T>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec)).build()); }
    public static void register() {
        PRESS_NAME = component("jimta_press_name", Codec.string(0, 256)); MEMORY = component("jimta_machine_settings", Settings.CODEC); AMMO = component("jimta_cannon_ammo", ItemStack.OPTIONAL_CODEC);
        for (String material : List.of("certus_quartz", "nether_quartz", "fluix")) {
            var repair = TagKey.create(Registries.ITEM, Jugcraft.id("jimta_" + material + "_tool_material"));
            ToolMaterial tier = material.equals("fluix") ? new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 750, 7.2000003f, 2.4f, 14, repair) : ToolMaterial.IRON;
            for (String tool : List.of("axe", "hoe", "shovel", "pickaxe", "sword")) add(material + "_" + tool, p -> {
                p = switch (tool) { case "axe" -> p.axe(tier, 6, -3.1f); case "hoe" -> p.hoe(tier, -2, -1); case "shovel" -> p.shovel(tier, 1.5f, -3); case "sword" -> p.sword(tier, 3, -2.4f); default -> p.pickaxe(tier, 1, -2.8f); };
                p.repairable(repair); return material.equals("fluix") ? new IntrinsicTool(p, tool.equals("sword") ? Enchantments.LOOTING : Enchantments.FORTUNE) : new Item(p);
            });
        }
        for (String material : List.of("certus_quartz", "nether_quartz")) { add(material + "_wrench", p -> new Wrench(p.stacksTo(1))); add(material + "_cutting_knife", p -> new Knife(p.durability(50))); }
        add("name_press", p -> new Item(p.stacksTo(1))); add("fluix_upgrade_smithing_template", Item::new);
        add("memory_card", p -> new Memory(p.stacksTo(1))); add("network_tool", p -> new NetworkTool(p.stacksTo(1)));
        add("guide", DigitalGuide::new);
        add("charged_staff", p -> new Staff(p.stacksTo(1))); add("entropy_manipulator", p -> new Entropy(p.stacksTo(1)));
        add("color_applicator", p -> new Applicator(p.stacksTo(1))); add("matter_cannon", p -> new Cannon(p.stacksTo(1))); add("matter_ball", Item::new);
        DigitalPressMenu.register();
    }
    private static void add(String name, Function<Item.Properties, Item> factory) {
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name)); ITEMS.add(name);
        DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key))));
    }
    private static boolean allowed(UseOnContext c) { return c.getPlayer() instanceof ServerPlayer p && !p.isSpectator() && p.isAlive() && Authority.mayChange(p.level(), p, c.getClickedPos()); }
    public static final class IntrinsicTool extends Item {
        final ResourceKey<Enchantment> enchantment;
        IntrinsicTool(Properties p, ResourceKey<Enchantment> enchantment) { super(p); this.enchantment = enchantment; }
        private void ensure(ItemStack s, Level level) {
            var holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
            if (EnchantmentHelper.getItemEnchantmentLevel(holder, s) < 1) s.enchant(holder, 1);
        }
        @Override public void inventoryTick(ItemStack s, ServerLevel level, Entity entity, EquipmentSlot slot) { ensure(s, level); }
        @Override public void onCraftedPostProcess(ItemStack s, Level level) { ensure(s, level); }
        @Override public boolean isFoil(ItemStack s) { return true; }
    }
    public static class Wrench extends BrassWrenchItem {
        Wrench(Properties p) { super(p); }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (c.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            if (!allowed(c)) return InteractionResult.FAIL;
            if (c.getLevel().getBlockEntity(c.getClickedPos()) instanceof DigitalFacades.Covers covers
                    && covers.parts.nodes().stream().anyMatch(n -> !n.permitted((ServerPlayer)c.getPlayer()))) return InteractionResult.FAIL;
            if (DigitalFacades.remove(c)) return InteractionResult.SUCCESS;
            var level = c.getLevel(); var pos = c.getClickedPos(); var state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) instanceof DigitalNode node && !node.permitted((ServerPlayer) c.getPlayer())) return InteractionResult.FAIL;
            if (!(state.getBlock() instanceof DigitalNodeBlock) && !(state.getBlock() instanceof DigitalCableBlock) && !(state.getBlock() instanceof DigitalFixtures.Growth)) return super.useOn(c);
            if (c.getPlayer().isShiftKeyDown()) { level.destroyBlock(pos, !c.getPlayer().isCreative(), c.getPlayer()); return InteractionResult.SUCCESS; }
            if (state.hasProperty(DigitalNodeBlock.FACING)) {
                Direction old = state.getValue(DigitalNodeBlock.FACING);
                boolean sided = state.getBlock() instanceof DigitalNodeBlock b && (b.kind.endsWith("_bus") || b.kind.equals("pattern_provider"));
                Direction next = sided ? Direction.values()[(old.ordinal() + 1) % 6] : old.getAxis().isHorizontal() ? old.getClockWise() : Direction.NORTH;
                level.setBlockAndUpdate(pos, state.setValue(DigitalNodeBlock.FACING, next));
            }
            else if (state.hasProperty(DigitalFixtures.Growth.FACING)) level.setBlockAndUpdate(pos, state.setValue(DigitalFixtures.Growth.FACING, Direction.values()[(state.getValue(DigitalFixtures.Growth.FACING).ordinal() + 1) % 6]));
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Knife extends Item {
        Knife(Properties p) { super(p); }
        @Override public InteractionResult use(Level level, Player p, InteractionHand hand) {
            if (p instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, who) -> new DigitalPressMenu.Menu(id, inv, hand, p.getItemInHand(hand)), Component.literal("Quartz Cutting Knife")));
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Memory extends Item {
        Memory(Properties p) { super(p); }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (c.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            var node=DigitalParts.pointed(c);
            if (!allowed(c) || node==null || !node.permitted((ServerPlayer) c.getPlayer())) return InteractionResult.FAIL;
            if (DigitalLinks.tunnel(node.kind())) return DigitalLinks.memory(c, node, (ServerPlayer) c.getPlayer());
            if (c.getPlayer().isShiftKeyDown()) {
                var settings = new Settings(node.kind(), node.filter(), node.threshold, node.emitBelow); c.getItemInHand().set(MEMORY, settings);
                int hash = Objects.hash(settings.kind(), BuiltInRegistries.ITEM.getKey(settings.filter().getItem()), settings.threshold(), settings.below());
                var colors = new ArrayList<Integer>(); colors.add(0x66CC88);
                for (int i = 0; i < 8; i++) colors.add(DigitalMatterAmmo.colors(DyeColor.byId(hash >>> (i * 4) & 15)).get(1));
                c.getItemInHand().set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(List.of(), List.of(), List.of(), colors));
                c.getPlayer().sendOverlayMessage(Component.literal("Copied filter and control settings."));
            }
            else { var s = c.getItemInHand().get(MEMORY); if (s == null || !s.kind().equals(node.kind())) { c.getPlayer().sendOverlayMessage(Component.literal("Copy settings from the same machine type first: sneak-use.")); return InteractionResult.FAIL; }
                node.filter(s.filter()); node.threshold = s.threshold(); node.emitBelow = s.below(); node.setChanged();
                if (DigitalControls.controls(node.kind())) DigitalControls.update(node, DigitalNetwork.find(node, (ServerPlayer) c.getPlayer()));
                c.getPlayer().sendOverlayMessage(Component.literal("Settings applied. Stored items and ownership are unchanged.")); }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class NetworkTool extends Wrench {
        NetworkTool(Properties p) { super(p); }
        @Override public boolean canFitInsideContainerItems() { return false; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) { if (player instanceof ServerPlayer p) DigitalCardCase.open(p, hand); return InteractionResult.SUCCESS; }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (c.getPlayer() != null && c.getPlayer().isShiftKeyDown()) return super.useOn(c);
            if (c.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            var node=DigitalParts.pointed(c);
            if (!allowed(c) || node==null || !node.permitted((ServerPlayer) c.getPlayer())) return InteractionResult.FAIL;
            var n = DigitalNetwork.find(node, (ServerPlayer) c.getPlayer()); long energy = n.nodes().stream().filter(DigitalNode::poweredSource).mapToLong(v -> v.energy.getAmount()).sum();
            c.getPlayer().sendSystemMessage(Component.literal("ME network: " + (n.truncated() ? "too large (256-block traversal limit)" : n.online() ? "online" : "offline or no channel") + " · " + n.nodes().size() + " active devices · " + energy + " JE · " + n.nodes().stream().mapToInt(DigitalNode::cellSlots).sum() + " cell slots. Cable capacity: 8 ordinary / 32 dense. Controller-free networks: 8 devices."));
            return InteractionResult.SUCCESS;
        }
    }
    public static class Powered extends Item implements Chargeable {
        final long capacity; Powered(Properties p, long capacity) { super(p); this.capacity = capacity; }
        @Override public long baseCapacity() { return capacity; }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay d, java.util.function.Consumer<Component> out, TooltipFlag f) { out.accept(PoweredToolItem.energyLine(s)); }
    }
    public static final class Staff extends Powered {
        Staff(Properties p) { super(p, 32_000); }
        @Override public void hurtEnemy(ItemStack s, LivingEntity target, LivingEntity attacker) {
            if (attacker instanceof ServerPlayer p && Authority.mayStrike(p, target) && ConcordanceEffects.mayHarm(p, target) && Chargeable.drain(s, 300)) {
                target.hurtServer(p.level(), p.damageSources().playerAttack(p), 6); p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 8, .2, .2, .2, .02);
            }
        }
    }
    public static final class Entropy extends Powered {
        Entropy(Properties p) { super(p, 200_000); }
        @Override public InteractionResult use(Level level, Player p, InteractionHand hand) {
            var hit = getPlayerPOVHitResult(level, p, ClipContext.Fluid.ANY);
            return hit.getType() == HitResult.Type.BLOCK && !level.getBlockState(hit.getBlockPos()).getFluidState().isEmpty() ? useOn(new UseOnContext(p, hand, hit)) : InteractionResult.PASS;
        }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (c.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            if (!allowed(c) || Chargeable.energy(c.getItemInHand()) < 1600) return InteractionResult.FAIL;
            ServerPlayer p = (ServerPlayer) c.getPlayer(); var level = p.level(); var pos = c.getClickedPos(); var old = level.getBlockState(pos);
            if (old.hasBlockEntity() || old.getDestroySpeed(level, pos) < 0) return InteractionResult.FAIL;
            Block replacement = null; ItemStack drop = ItemStack.EMPTY; boolean cool = p.isShiftKeyDown();
            if (cool) {
                if (old.is(Blocks.WATER)) { if (old.getFluidState().isSource()) replacement = Blocks.ICE; else { replacement = Blocks.AIR; drop = new ItemStack(Items.SNOWBALL); } }
                else if (old.is(Blocks.LAVA) && old.getFluidState().isSource()) replacement = Blocks.OBSIDIAN;
                else if (old.is(Blocks.GRASS_BLOCK)) replacement = Blocks.DIRT;
                else if (old.is(Blocks.STONE_BRICKS)) replacement = Blocks.CRACKED_STONE_BRICKS;
                else if (old.is(Blocks.STONE)) replacement = Blocks.COBBLESTONE;
            } else {
                if (old.is(Blocks.COBBLESTONE)) replacement = Blocks.STONE;
                else if (old.is(Blocks.ICE) || old.is(Blocks.SNOW)) replacement = Blocks.WATER;
                else if (old.is(Blocks.WATER)) replacement = Blocks.AIR;
                else if (old.is(Blocks.SAND)) replacement = Blocks.GLASS;
                else if (old.is(Blocks.CLAY)) replacement = Blocks.TERRACOTTA;
                else if (old.is(Blocks.NETHERRACK)) { replacement = Blocks.AIR; drop = new ItemStack(Items.NETHER_BRICK); }
            }
            if (replacement == null && !cool && old.getBlock().asItem() != Items.AIR) {
                var recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(old.getBlock())), level);
                if (recipe.isPresent()) {
                    var output = recipe.get().value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(old.getBlock())));
                    if (!output.isEmpty() && output.getCount() == 1 && output.getItem() instanceof BlockItem item && !item.getBlock().defaultBlockState().hasBlockEntity() && item.getBlock().defaultBlockState().isCollisionShapeFullBlock(level, pos)) replacement = item.getBlock();
                    else if (!output.isEmpty()) { replacement = Blocks.AIR; drop = output; }
                }
            }
            if (replacement == null) return InteractionResult.PASS;
            // Water may immediately flow: do not create it against a protected or unloaded boundary.
            if (replacement == Blocks.WATER) for (Direction d : Direction.values()) if (!level.isLoaded(pos.relative(d)) || !Authority.mayChange(level, p, pos.relative(d))) return InteractionResult.FAIL;
            if (!level.setBlockAndUpdate(pos, replacement.defaultBlockState())) return InteractionResult.FAIL;
            Chargeable.drain(c.getItemInHand(), 1600); if (!drop.isEmpty()) Block.popResource(level, pos, drop);
            level.sendParticles(cool ? ParticleTypes.SNOWFLAKE : ParticleTypes.SMOKE, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 8, .3, .3, .3, .01); return InteractionResult.SUCCESS;
        }
        @Override public void hurtEnemy(ItemStack s, LivingEntity target, LivingEntity attacker) { if (attacker instanceof ServerPlayer p && Authority.mayStrike(p, target) && ConcordanceEffects.mayHarm(p, target) && Chargeable.drain(s, 1600)) target.igniteForSeconds(8); }
    }
    public static final class Applicator extends Powered {
        Applicator(Properties p) { super(p, 200_000); }
        @Override public InteractionResult use(Level world, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND || !p.isShiftKeyDown()) return InteractionResult.PASS;
            var tool = p.getItemInHand(hand); var old = tool.getOrDefault(AMMO, ItemStack.EMPTY).copy(); var held = p.getOffhandItem();
            if (held.isEmpty() && !old.isEmpty()) { tool.remove(AMMO); p.getInventory().placeItemBackInInventory(old, net.minecraft.util.Prediction.SERVER_ONLY); return InteractionResult.SUCCESS; }
            if (!(held.getItem() instanceof DigitalPaint.Ball) || !old.isEmpty() && !ItemStack.isSameItemSameComponents(old, held)) { p.sendOverlayMessage(Component.literal("Sneak-use: load offhand paintballs, or unload with an empty offhand.")); return InteractionResult.FAIL; }
            int n = Math.min(held.getCount(), 64 - old.getCount()); if (n <= 0) return InteractionResult.FAIL;
            if (old.isEmpty()) old = held.copyWithCount(n); else old.grow(n); held.shrink(n); tool.set(AMMO, old); setColor(tool, ((DigitalPaint.Ball) old.getItem()).color); return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (c.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            if (!allowed(c) || Chargeable.energy(c.getItemInHand()) < 100 || c.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.FAIL;
            Player p = c.getPlayer(); var dye = p.getOffhandItem(); var loaded = c.getItemInHand().getOrDefault(AMMO, ItemStack.EMPTY).copy();
            var paint = dye.getItem() instanceof DigitalPaint.Ball ? dye : loaded;
            if (paint.getItem() instanceof DigitalPaint.Ball ball) {
                if (!DigitalPaint.paint((ServerPlayer) p, c.getClickedPos(), c.getClickedFace(), ball.color, ball.lumen)) return InteractionResult.FAIL;
                Chargeable.drain(c.getItemInHand(), 100); if (!p.isCreative()) { paint.shrink(1); if (paint == loaded) c.getItemInHand().set(AMMO, loaded); } setColor(c.getItemInHand(), ball.color); return InteractionResult.SUCCESS;
            }
            if (!(dye.getItem() instanceof DyeItem color)) { p.sendOverlayMessage(Component.literal("Hold dye/paintballs in your offhand, or sneak-use to load paintballs.")); return InteractionResult.FAIL; }
            var old = c.getLevel().getBlockState(c.getClickedPos()); BlockState changed = recolor(old, color.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE));
            if (changed == null || changed.equals(old) || old.hasBlockEntity()) return InteractionResult.PASS;
            if (!c.getLevel().setBlockAndUpdate(c.getClickedPos(), changed)) return InteractionResult.FAIL;
            Chargeable.drain(c.getItemInHand(), 100); if (!p.isCreative()) dye.shrink(1); setColor(c.getItemInHand(), color.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE)); return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult interactLivingEntity(ItemStack stack, Player actor, LivingEntity target, InteractionHand hand) {
            if (!(actor instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND || !(target instanceof Sheep sheep) || !(p.getOffhandItem().getItem() instanceof DyeItem dye) || sheep.getColor() == dye.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE) || !Authority.mayStrike(p, target) || !Authority.mayChange(p.level(), p, target.blockPosition()) || !Chargeable.drain(stack, 100)) return InteractionResult.PASS;
            sheep.setColor(dye.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE)); if (!p.isCreative()) p.getOffhandItem().shrink(1); setColor(stack, dye.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE)); return InteractionResult.SUCCESS;
        }
    }
    public static void setColor(ItemStack stack, DyeColor color) { stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(List.of(), List.of(true), List.of(), DigitalMatterAmmo.colors(color))); }
    public static BlockState recolor(BlockState old, DyeColor color) {
        var id = BuiltInRegistries.BLOCK.getKey(old.getBlock()); if (!id.getNamespace().equals("minecraft")) return null;
        String path = id.getPath(), suffix = null;
        for (String kind : List.of("wool", "carpet", "terracotta", "concrete", "concrete_powder", "stained_glass", "stained_glass_pane"))
            if (Arrays.stream(DyeColor.values()).anyMatch(c -> path.equals(c.getName() + "_" + kind)) || kind.equals("terracotta") && path.equals("terracotta") || kind.equals("stained_glass") && path.equals("glass") || kind.equals("stained_glass_pane") && path.equals("glass_pane")) { suffix = kind; break; }
        if (suffix == null) return null;
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color.getName() + "_" + suffix));
        return block == Blocks.AIR ? null : block.withPropertiesOf(old);
    }
    public static final class Cannon extends Powered {
        Cannon(Properties p) { super(p, 200_000); }
        @Override public InteractionResult use(Level world, Player actor, InteractionHand hand) {
            if (!(actor instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            ItemStack gun = p.getItemInHand(hand); if (hand != InteractionHand.MAIN_HAND || !p.isAlive() || p.isSpectator() || !RateGate.allow(p, "jimta_matter_cannon", 5)) return InteractionResult.FAIL;
            ItemStack loaded = gun.getOrDefault(AMMO, ItemStack.EMPTY).copy();
            if (p.isShiftKeyDown()) {
                ItemStack source = p.getOffhandItem();
                if (source.isEmpty() && !loaded.isEmpty()) { gun.remove(AMMO); p.getInventory().placeItemBackInInventory(loaded, net.minecraft.util.Prediction.SERVER_ONLY); return InteractionResult.SUCCESS; }
                if (weight(source) <= 0 || !loaded.isEmpty() && !ItemStack.isSameItemSameComponents(loaded, source)) { p.sendOverlayMessage(Component.literal("Hold compatible nuggets or matter balls in your offhand; sneak-use to load. Empty offhand unloads.")); return InteractionResult.FAIL; }
                int count = Math.min(source.getCount(), 64 - loaded.getCount()); if (count < 1) return InteractionResult.FAIL;
                if (loaded.isEmpty()) loaded = source.copyWithCount(count); else loaded.grow(count); source.shrink(count); gun.set(AMMO, loaded); return InteractionResult.SUCCESS;
            }
            if (loaded.isEmpty() || weight(loaded) <= 0 || Chargeable.energy(gun) < 1600) { p.sendOverlayMessage(Component.literal("Matter cannon needs charge and ammunition. Sneak-use to load from offhand.")); return InteractionResult.FAIL; }
            if (loaded.getItem() instanceof DigitalPaint.Ball ball) { loaded.shrink(1); gun.set(AMMO, loaded); Chargeable.drain(gun, 1600); DigitalPaint.fire(p, ball); return InteractionResult.SUCCESS; }
            float penetration = weight(loaded); loaded.shrink(1); gun.set(AMMO, loaded); Chargeable.drain(gun, 1600);
            var level = p.level(); Vec3 from = p.getEyePosition(), direction = p.getLookAngle(), to = from;
            for (int distance = 1; distance <= 255; distance++) { Vec3 next = from.add(direction.scale(distance)); if (!level.isLoaded(BlockPos.containing(next))) break; to = next; }
            for (int attempt = 0; attempt < 4 && penetration > 0; attempt++) {
                var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                Vec3 endpoint = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation(); LivingEntity selected = null; double closest = from.distanceToSqr(endpoint);
                int scanned = 0;
                for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, endpoint).inflate(.3), e -> e != p && e.isAlive() && !e.isSpectator())) {
                    if (++scanned > 128) break; var intercept = candidate.getBoundingBox().inflate(.3).clip(from, endpoint); if (intercept.isPresent() && from.distanceToSqr(intercept.get()) < closest) { closest = from.distanceToSqr(intercept.get()); selected = candidate; endpoint = intercept.get(); }
                }
                for (int step = 1, count = Math.min(32, (int) from.distanceTo(endpoint)); step <= count; step++) { var point = from.lerp(endpoint, step / (double) count); level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0); }
                if (selected != null) { if (Authority.mayStrike(p, selected) && ConcordanceEffects.mayHarm(p, selected)) selected.hurtServer(level, p.damageSources().playerAttack(p), (float) Math.ceil(penetration / 20)); break; }
                if (hit.getType() == HitResult.Type.MISS) break;
                var pos = hit.getBlockPos(); var state = level.getBlockState(pos); float hardness = state.getDestroySpeed(level, pos) * 9;
                if (state.hasBlockEntity() || hardness < 0 || penetration <= hardness || !Authority.mayChange(level, p, pos) || !p.gameMode.destroyBlock(pos)) break;
                penetration = (penetration - hardness) * .6f;
            }
            return InteractionResult.SUCCESS;
        }
        public static float weight(ItemStack ammo) {
            if (ammo.getItem() instanceof DigitalPaint.Ball) return 1;
            if (ammo.is(DigitalStorage.ITEMS.get("matter_ball"))) return 32;
            return DigitalMatterAmmo.weight(ammo);
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay d, java.util.function.Consumer<Component> out, TooltipFlag f) { super.appendHoverText(s, c, d, out, f); var ammo = s.getOrDefault(AMMO, ItemStack.EMPTY); out.accept(Component.literal(ammo.isEmpty() ? "No ammunition" : ammo.getCount() + " × " + ammo.getHoverName().getString())); out.accept(Component.literal("Sneak-use: load offhand / unload with empty offhand.")); }
    }
}

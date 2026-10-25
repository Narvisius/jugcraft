package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Source barrel recipes, beer ageing and rum timing on a bounded native, persistent brewing vessel. */
public final class BreweryBarrel {
    public static Block BLOCK;
    public static BlockEntityType<Node> TYPE;
    public static final Map<String, Item> DRINKS = new LinkedHashMap<>();
    public static final List<String> STAGES = List.of("brew", "youngster", "beer", "ale", "dragonblood", "black_stuff");
    public static final BooleanProperty TAPPED = BooleanProperty.create("tapped");
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private BreweryBarrel() {}
    public static void register() {
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_brewing_barrel"));
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new Barrel(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).setId(key)));
        var item = ResourceKey.create(Registries.ITEM, key.identifier());
        Registry.register(BuiltInRegistries.ITEM, item, new BlockItem(BLOCK, new Item.Properties().setId(item).stacksTo(1).useBlockDescriptionPrefix()) {
            @Override public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay d, java.util.function.Consumer<Component> lines, TooltipFlag f) {
                lines.accept(Component.literal("Beer: water buckets, wheat and hops. Rum: sugar cane. Leave untapped to age."));
                lines.accept(Component.literal("Fit a new hand treetap to stop ageing; use empty mugs to draw drinks. Breaking preserves the batch."));
            }
        });
        var names = new ArrayList<>(STAGES); names.add("rum");
        for (String name : names) {
            var drink = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_barrel_" + name));
            DRINKS.put(name, Registry.register(BuiltInRegistries.ITEM, drink, new Drink(new Item.Properties().setId(drink).stacksTo(16), name)));
        }
        TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key.identifier(), FabricBlockEntityTypeBuilder.create(Node::new, BLOCK).build());
        PlayerBlockBreakEvents.BEFORE.register((level, p, pos, state, entity) -> !(entity instanceof Node node) || node.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(out -> { out.accept(BLOCK); DRINKS.values().forEach(out::accept); });
    }
    static final class Barrel extends Block implements EntityBlock {
        Barrel(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TAPPED, false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING, TAPPED); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Node(p,s); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> type) {
            return !l.isClientSide() && type == TYPE ? (world,p,state,entity) -> { if(world.getGameTime()%20 == 0)((Node)entity).age(20); } : null;
        }
        @Override public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity entity, ItemStack stack) {
            if (l.getBlockEntity(p) instanceof Node node) {
                node.restore(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag());
                if (entity != null) node.owner = entity.getUUID(); node.appearance(); node.setChanged();
            }
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit) {
            if (l.isClientSide()) return InteractionResult.SUCCESS;
            return l.getBlockEntity(pos) instanceof Node node ? node.interact(p,hand,hit.getDirection()) : InteractionResult.FAIL;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit) {
            if (l.getBlockEntity(pos) instanceof Node node && node.allowed(p)) {
                if (!l.isClientSide() && p.isShiftKeyDown() && node.tap) {
                    node.tap = false; p.getInventory().placeItemBackInInventory(new ItemStack(IndustrialGear.ITEMS.get("jimta_hand_treetap")), net.minecraft.util.Prediction.SERVER_ONLY);
                    node.appearance(); node.setChanged();
                }
                p.sendOverlayMessage(Component.literal(node.describe()));
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity {
        public UUID owner;
        public int kind, portions, wheat, hops, stage, age, body, bitterness;
        public boolean tap, locked;
        public Node(BlockPos p, BlockState s) { super(TYPE,p,s); }
        boolean identity(Player p) { return owner == null || owner.equals(p.getUUID()) || JugcraftParties.sameParty(owner,p.getUUID()); }
        boolean allowed(Player p) { return level != null && !isRemoved() && p.isAlive() && !p.isSpectator() && identity(p) && Authority.mayChange(level,p,worldPosition); }
        public static int rumTime(int amount) { return (int)(1200 * amount * Math.pow(.95, amount-1)); }
        public void age(int ticks) {
            if (tap || portions == 0 || kind == 0 || kind == 1 && wheat + hops == 0) return;
            if (kind == 2) age = Math.min(rumTime(portions), age + ticks);
            else if (stage < 5) {
                age += ticks; int needed = 24000 * (int)Math.pow(3,stage == 4 ? 6 : stage);
                if (age >= needed) { age -= needed; stage++; }
            }
            setChanged();
        }
        public String describe() {
            return portions + " mugs · " + (kind == 2 ? "Rum " + Math.min(100,age*100/Math.max(1,rumTime(portions))) + "%"
                    : "Beer: " + wheat + " wheat / " + hops + " hops · " + STAGES.get(stage))
                    + (tap ? " · tapped; use empty mugs" : " · ageing; fit a hand treetap");
        }
        InteractionResult interact(Player p, InteractionHand hand, Direction side) {
            if (!allowed(p)) return InteractionResult.FAIL;
            var held = p.getItemInHand(hand);
            if (held.is(IndustrialGear.ITEMS.get("jimta_hand_treetap")) && held.getDamageValue() == 0 && !tap && side.getAxis().isHorizontal()) {
                if (portions == 0) return InteractionResult.FAIL;
                if (!locked) { ratios(); locked = true; }
                tap = true; held.consume(1,p);
                level.setBlock(worldPosition,getBlockState().setValue(FACING,side).setValue(TAPPED,true),Block.UPDATE_CLIENTS); setChanged();
            } else if (held.is(IndustrialCrops.ITEMS.get("jimta_empty_mug")) && tap && portions > 0) {
                var drink = draw(); held.consume(1,p);
                p.getInventory().placeItemBackInInventory(drink,net.minecraft.util.Prediction.SERVER_ONLY); setChanged();
            } else if (!tap && !locked) {
                boolean changed = false;
                if (held.is(Items.WATER_BUCKET) && kind != 2 && portions < 32) {
                    portions++; kind = 1; changed = true;
                    p.setItemInHand(hand,ItemUtils.createFilledResult(held,p,new ItemStack(Items.BUCKET)));
                } else if (held.is(Items.SUGAR_CANE) && (kind == 0 || kind == 2 && age <= 600) && portions < 32) {
                    int n = Math.min(p.isShiftKeyDown()?1:held.getCount(),32-portions); portions += n; kind = 2; held.consume(n,p); changed = true;
                } else if (kind != 2 && (held.is(Items.WHEAT) || held.is(IndustrialCrops.ITEMS.get("jimta_hops")))) {
                    boolean grain = held.is(Items.WHEAT); int n = Math.min(p.isShiftKeyDown()?1:held.getCount(),64-(grain?wheat:hops));
                    if (n > 0) { if (grain) wheat += n; else hops += n; held.consume(n,p); kind = 1; changed = true;
                        if (stage == 0) age = 0; else stage = 5;
                    }
                }
                if (!changed) return InteractionResult.PASS; setChanged();
            } else return InteractionResult.PASS;
            p.sendOverlayMessage(Component.literal(describe())); return InteractionResult.SUCCESS;
        }
        private void ratios() {
            double solids = (wheat+hops)/(double)Math.max(1,portions), ratio = wheat == 0 ? (hops == 0?0:10) : hops/(double)wheat;
            body = solids <= 5./12 ? 0 : solids <= .5 ? 1 : solids < 1 ? 2 : solids == 1 ? 3 : solids < 2 ? 4 : solids < 2.4 ? 5 : 6;
            bitterness = ratio <= .25 ? 0 : ratio <= 1./3 ? 1 : ratio <= .5 ? 2 : ratio < 2 ? 3 : ratio < 3 ? 4 : ratio < 4 ? 5 : 6;
            if (kind == 1 && (solids >= 4 || ratio >= 5)) stage = 5;
        }
        public ItemStack draw() {
            if (!tap || portions <= 0) return ItemStack.EMPTY;
            int progress = kind == 2 ? Math.min(100,age*100/Math.max(1,rumTime(portions))) : 0;
            var result = new ItemStack(DRINKS.get(kind == 2 ? "rum" : STAGES.get(stage)));
            CustomData.update(DataComponents.CUSTOM_DATA,result,t -> { t.putInt("Body",body); t.putInt("Bitterness",bitterness); t.putInt("Maturity",progress); });
            portions--;
            if (portions == 0) { kind = wheat = hops = stage = age = body = bitterness = 0; locked = false; }
            else if (kind == 2) age = progress * rumTime(portions) / 100;
            return result;
        }
        void appearance() { if(level != null && !level.isClientSide()) level.setBlock(worldPosition,getBlockState().setValue(TAPPED,tap),Block.UPDATE_CLIENTS); }
        public CompoundTag contents() {
            var t = new CompoundTag(); t.putInt("Kind",kind); t.putInt("Portions",portions); t.putInt("Wheat",wheat); t.putInt("Hops",hops);
            t.putInt("Stage",stage); t.putInt("Age",age); t.putInt("Body",body); t.putInt("Bitterness",bitterness); t.putBoolean("Tapped",tap); t.putBoolean("Locked",locked); return t;
        }
        public void restore(CompoundTag t) {
            kind = Math.clamp(t.getIntOr("Kind",0),0,2); portions = Math.clamp(t.getIntOr("Portions",0),0,32);
            wheat = Math.clamp(t.getIntOr("Wheat",0),0,64); hops = Math.clamp(t.getIntOr("Hops",0),0,64);
            stage = Math.clamp(t.getIntOr("Stage",0),0,5); age = Math.clamp(t.getIntOr("Age",0),0,24000*729);
            body = Math.clamp(t.getIntOr("Body",0),0,6); bitterness = Math.clamp(t.getIntOr("Bitterness",0),0,6);
            tap = t.getBooleanOr("Tapped",false); locked = t.getBooleanOr("Locked",false);
        }
        @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("Batch",CompoundTag.CODEC,contents()); if(owner != null)out.store("Owner",UUIDUtil.CODEC,owner); }
        @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); restore(in.read("Batch",CompoundTag.CODEC).orElseGet(CompoundTag::new)); owner=in.read("Owner",UUIDUtil.CODEC).orElse(null); }
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s) {
            if(level != null && !level.isClientSide()) { var barrel = new ItemStack(BLOCK); barrel.set(DataComponents.CUSTOM_DATA,CustomData.of(contents())); Block.popResource(level,p,barrel); }
        }
    }
    static final class Drink extends Item {
        final String name;
        Drink(Properties p,String name) { super(p); this.name=name; }
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand) { if(p.getCooldowns().isOnCooldown(p.getItemInHand(hand)))return InteractionResult.FAIL; p.startUsingItem(hand); return InteractionResult.CONSUME; }
        @Override public ItemUseAnimation getUseAnimation(ItemStack s) { return ItemUseAnimation.DRINK; }
        @Override public int getUseDuration(ItemStack s,LivingEntity e) { return 32; }
        @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity user) {
            if(!(l instanceof ServerLevel)||!(user instanceof Player p))return s;
            var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); int stage=STAGES.indexOf(name);
            boolean bad=stage==5||name.equals("rum")&&data.getIntOr("Maturity",0)<100;
            if(bad) p.addEffect(new MobEffectInstance(MobEffects.NAUSEA,400,0));
            else {p.getFoodData().eat(2,.2F); if(name.equals("rum"))p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,600,0));
                else if(stage>=2)p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100+50*Math.clamp(data.getIntOr("Body",0),0,6),0));
                if(stage>=3&&Math.clamp(data.getIntOr("Bitterness",0),0,6)>=3)p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,400,0));}
            // One shared cooldown prevents rotating through seven drink IDs for repeated buffs.
            for(var drink:DRINKS.values())p.getCooldowns().addCooldown(new ItemStack(drink),200);
            if(!p.hasInfiniteMaterials()){s.shrink(1);var mug=new ItemStack(IndustrialCrops.ITEMS.get("jimta_empty_mug"));if(s.isEmpty())return mug;if(!p.addItem(mug))p.drop(mug,false,net.minecraft.util.Prediction.SERVER_ONLY);}return s;
        }
    }
}

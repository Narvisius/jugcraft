package io.github.jimbozoomer.jugcraft.loot;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.vehicle.minecart.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Structure rewards in rail carts and display frames. Ordinary player carts/frames are never converted. */
public final class OwnerLootEntities {
    private OwnerLootEntities(){}
    public static EntityType<Cart> CART;public static EntityType<Frame> FRAME;
    public static Item CART_ITEM,FRAME_ITEM;
    public static final EntityDataSerializer<List<UUID>> VISITORS=EntityDataSerializer.forValueType(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()));
    public interface Personal {OwnerLootRewards rewards();boolean seen(UUID player);}
    private static boolean valid(Entity e,Player p){return !e.isRemoved()&&p.level()==e.level()&&OwnerLoot.mayOpen(p,e.level(),e.blockPosition());}
    public static void register(){
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry.register(Jugcraft.id("personal_loot_visitors"),VISITORS);
        var cartKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("personal_loot_minecart"));
        CART=Registry.register(BuiltInRegistries.ENTITY_TYPE,cartKey,EntityType.Builder.<Cart>of(Cart::new,MobCategory.MISC).sized(.98f,.7f).clientTrackingRange(8).build(cartKey));
        var frameKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("personal_loot_item_frame"));
        FRAME=Registry.register(BuiltInRegistries.ENTITY_TYPE,frameKey,EntityType.Builder.<Frame>of(Frame::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(10).build(frameKey));
        var cartItem=ResourceKey.create(Registries.ITEM,cartKey.identifier());
        CART_ITEM=Registry.register(BuiltInRegistries.ITEM,cartItem,new MinecartItem(CART,new Item.Properties().setId(cartItem).stacksTo(1)){
            @Override public InteractionResult useOn(UseOnContext c){return c.getPlayer()!=null&&c.getPlayer().isCreative()&&OwnerLoot.mayOpen(c.getPlayer(),c.getLevel(),c.getClickedPos())?super.useOn(c):InteractionResult.FAIL;}
        });
        var frameItem=ResourceKey.create(Registries.ITEM,frameKey.identifier());
        FRAME_ITEM=Registry.register(BuiltInRegistries.ITEM,frameItem,new Item(new Item.Properties().setId(frameItem)){
            @Override public InteractionResult useOn(UseOnContext c){
                var p=c.getPlayer();var pos=c.getClickedPos().relative(c.getClickedFace());if(p==null||!p.isCreative()||!OwnerLoot.mayOpen(p,c.getLevel(),pos))return InteractionResult.FAIL;
                var frame=new Frame(c.getLevel(),pos,c.getClickedFace());if(!frame.survives())return InteractionResult.FAIL;
                if(c.getLevel() instanceof ServerLevel server){server.addFreshEntity(frame);frame.playPlacementSound();}return InteractionResult.SUCCESS;
            }
        });
        UseEntityCallback.EVENT.register((p,level,hand,e,hit)->{
            if(!(p instanceof ServerPlayer player)||p.isShiftKeyDown()||e.getType()!=net.minecraft.world.entity.EntityTypes.CHEST_MINECART||!(e instanceof MinecartChest original)||original.getContainerLootTable()==null)return InteractionResult.PASS;
            if(!valid(e,p))return InteractionResult.FAIL;
            var table=original.getContainerLootTable();long seed=original.getContainerLootTableSeed();
            var saved=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());original.saveWithoutId(saved);
            var data=saved.buildResult();data.remove("UUID");data.remove("LootTable");data.remove("LootTableSeed");data.remove("Items");
            var replacement=new Cart(CART,level);replacement.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),data));replacement.rewards.configure(table,seed);
            if(!((ServerLevel)level).addFreshEntity(replacement))return InteractionResult.FAIL;
            original.setContainerLootTable(null);original.discard();replacement.rewards.open(player);return InteractionResult.SUCCESS;
        });
    }
    public static final class Cart extends MinecartChest implements Personal {
        private static final EntityDataAccessor<List<UUID>> SEEN=SynchedEntityData.defineId(Cart.class,VISITORS);
        private final OwnerLootRewards rewards=new OwnerLootRewards(new OwnerLootRewards.Host(){
            public boolean valid(Player p){return OwnerLootEntities.valid(Cart.this,p);}public Vec3 origin(){return position();}public int capacity(){return 27;}public boolean archaeology(){return false;}
            public Component name(){return Component.literal("Your minecart loot");}public void changed(){if(!level().isClientSide())entityData.set(SEEN,List.copyOf(rewards.seen()));}public void opened(boolean open){}
        });
        public Cart(EntityType<? extends MinecartChest> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SEEN,List.of());}
        @Override public OwnerLootRewards rewards(){return rewards;}@Override public boolean seen(UUID player){return entityData.get(SEEN).contains(player);}
        @Override public InteractionResult interact(Player p,InteractionHand h,Vec3 hit){if(p instanceof ServerPlayer owner)rewards.open(owner);return InteractionResult.SUCCESS;}
        @Override public void tick(){super.tick();if(level() instanceof ServerLevel server&&tickCount%20==0)rewards.tick(server.getServer());}
        @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){return s.getEntity() instanceof ServerPlayer p&&p.isCreative()&&valid(this,p)&&super.hurtServer(l,s,amount);}
        @Override public BlockState getDefaultDisplayBlockState(){return Blocks.CHEST.defaultBlockState();}
        @Override protected Item getDropItem(){return CART_ITEM;}@Override public ItemStack getPickResult(){return new ItemStack(CART_ITEM);}
        // The inherited container is permanently empty. Only private menus expose reward inventories.
        @Override public int getContainerSize(){return 0;}
        @Override public ItemStack getItem(int slot){return ItemStack.EMPTY;}
        @Override public ItemStack removeItem(int slot,int amount){return ItemStack.EMPTY;}
        @Override public ItemStack removeItemNoUpdate(int slot){return ItemStack.EMPTY;}
        @Override public void setItem(int slot,ItemStack stack){}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return false;}
        @Override public NonNullList<ItemStack> getItemStacks(){return NonNullList.create();}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv){return null;}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return null;}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);rewards.save(out);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);setContainerLootTable(null);rewards.load(in);entityData.set(SEEN,List.copyOf(rewards.seen()));}
    }
    public static final class Frame extends ItemFrame implements Personal {
        private static final EntityDataAccessor<List<UUID>> SEEN=SynchedEntityData.defineId(Frame.class,VISITORS);
        private final OwnerLootRewards rewards=new OwnerLootRewards(new OwnerLootRewards.Host(){
            public boolean valid(Player p){return OwnerLootEntities.valid(Frame.this,p);}public Vec3 origin(){return position();}public int capacity(){return 9;}public boolean archaeology(){return false;}
            public Component name(){return Component.literal("Your framed reward");}public void changed(){if(!level().isClientSide())entityData.set(SEEN,List.copyOf(rewards.seen()));}public void opened(boolean open){}
        });
        public Frame(EntityType<? extends ItemFrame> type,Level level){super(type,level);}private Frame(Level level,BlockPos pos,Direction face){super(FRAME,level,pos,face);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SEEN,List.of());}
        @Override public OwnerLootRewards rewards(){return rewards;}@Override public boolean seen(UUID player){return entityData.get(SEEN).contains(player);}
        @Override public InteractionResult interact(Player p,InteractionHand h,Vec3 hit){
            if(!valid(this,p))return InteractionResult.FAIL;
            if(p.isCreative()&&p.isShiftKeyDown())return super.interact(p,h,hit);
            if(p instanceof ServerPlayer owner)rewards.open(owner);return InteractionResult.SUCCESS;
        }
        @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){return s.getEntity() instanceof ServerPlayer p&&p.isCreative()&&valid(this,p)&&super.hurtServer(l,s,amount);}
        @Override public void tick(){super.tick();if(level() instanceof ServerLevel server&&tickCount%20==0)rewards.tick(server.getServer());}
        @Override protected ItemStack getFrameItemStack(){return new ItemStack(FRAME_ITEM);}@Override public ItemStack getPickResult(){return new ItemStack(FRAME_ITEM);}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);rewards.save(out);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);rewards.load(in);entityData.set(SEEN,List.copyOf(rewards.seen()));}
    }
}

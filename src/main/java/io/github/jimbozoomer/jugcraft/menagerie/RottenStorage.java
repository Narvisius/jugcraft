package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Supplied treasure cache and wolf support, adapted to native persistence and claims. */
public final class RottenStorage {
    public static BlockEntityType<Cache> CACHE;
    public static EntityType<HunterWolf> HUNTER_WOLF;
    private RottenStorage(){}
    static void register(){
        String id="rotten_treasure_chest";
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));
        var block=Registry.register(BuiltInRegistries.BLOCK,key,new CacheBlock(BlockBehaviour.Properties.of().setId(key).strength(2.5F).noOcclusion()));
        Menagerie.BLOCKS.put(id,block);Menagerie.item(id,p->new BlockItem(block,p.useBlockDescriptionPrefix()));
        CACHE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id(id),FabricBlockEntityTypeBuilder.create(Cache::new,block).build());
        var explosiveKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("rotten_tnt_barrel"));
        var explosive=Registry.register(BuiltInRegistries.BLOCK,explosiveKey,new TntBlock(BlockBehaviour.Properties.of().setId(explosiveKey).strength(.5F)){
            @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
                if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
                return super.useItemOn(stack,state,level,pos,player,hand,hit);
            }
        });
        Menagerie.BLOCKS.put("rotten_tnt_barrel",explosive);Menagerie.item("rotten_tnt_barrel",p->new BlockItem(explosive,p.useBlockDescriptionPrefix()));
        var wolfKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("rotten_hunter_wolf"));
        HUNTER_WOLF=Registry.register(BuiltInRegistries.ENTITY_TYPE,wolfKey,EntityType.Builder.<HunterWolf>of(HunterWolf::new,MobCategory.CREATURE).sized(.7F,1).clientTrackingRange(8).build(wolfKey));
        FabricDefaultAttributeRegistry.register(HUNTER_WOLF,Wolf.createAttributes());
        Menagerie.item("rotten_hunter_wolf_spawn_egg",p->new Item(p){
            @Override public InteractionResult useOn(UseOnContext c){
                if(!(c.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
                var at=c.getClickedPos().relative(c.getClickedFace());if(!Menagerie.mayChange(player.level(),player,at))return InteractionResult.FAIL;
                var wolf=HUNTER_WOLF.create(player.level(),EntitySpawnReason.SPAWN_ITEM_USE);if(wolf==null)return InteractionResult.FAIL;
                wolf.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);if(!player.level().noCollision(wolf)||!player.level().addFreshEntity(wolf))return InteractionResult.FAIL;
                c.getItemInHand().consume(1,player);return InteractionResult.SUCCESS;
            }
        });
    }
    static boolean summonWolf(RottenCreatures.Undead hunter,ServerLevel level,LivingEntity target){
        var wolf=HUNTER_WOLF.create(level,EntitySpawnReason.MOB_SUMMONED);if(wolf==null)return false;
        wolf.snapTo(hunter.getX()+1.2,hunter.getY(),hunter.getZ(),hunter.getYRot(),0);
        if(!level.hasChunkAt(wolf.blockPosition())||!level.noCollision(wolf))return false;
        wolf.hunter=hunter.getUUID();wolf.setTarget(target);return level.addFreshEntity(wolf);
    }
    public static final class HunterWolf extends Wolf {
        private UUID hunter;
        HunterWolf(EntityType<? extends Wolf> type,Level level){super(type,level);}
        @Override protected void customServerAiStep(ServerLevel level){
            super.customServerAiStep(level);
            if(hunter!=null&&tickCount%10==0&&level.getEntity(hunter) instanceof RottenCreatures.Undead parent&&parent.isAlive()){
                setTarget(parent.getTarget());if(getTarget()==null&&distanceToSqr(parent)>16)getNavigation().moveTo(parent,1.1);
            }
        }
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(hunter!=null)out.putString("Hunter",hunter.toString());}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);try{hunter=UUID.fromString(in.getStringOr("Hunter",""));}catch(IllegalArgumentException e){hunter=null;}}
    }
    private static final class CacheBlock extends BaseEntityBlock {
        CacheBlock(Properties p){super(p);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Cache(p,s);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(level.getBlockEntity(pos) instanceof Cache cache&&player instanceof ServerPlayer p)return cache.deposit(p,stack)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(level.getBlockEntity(pos) instanceof Cache cache&&player instanceof ServerPlayer p)return cache.withdraw(p)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Cache extends BlockEntity {
        private NonNullList<ItemStack> items=NonNullList.withSize(64,ItemStack.EMPTY);
        private UUID owner;
        Cache(BlockPos p,BlockState s){super(CACHE,p,s);}
        private boolean allowed(Player p){return owner==null||owner.equals(p.getUUID());}
        boolean deposit(ServerPlayer player,ItemStack stack){
            if(!allowed(player)||stack.isEmpty())return false;
            int count=items.stream().mapToInt(ItemStack::getCount).sum();
            int capacity=items.stream().filter(s->!s.isEmpty()).findFirst().map(ItemStack::getMaxStackSize).orElse(stack.getMaxStackSize());
            if(capacity!=stack.getMaxStackSize()||count>=capacity)return false;
            int amount=Math.min(stack.getCount(),capacity-count);
            for(int i=0;i<items.size()&&amount>0;i++)if(items.get(i).isEmpty()){
                items.set(i,stack.copyWithCount(amount));stack.shrink(amount);owner=player.getUUID();setChanged();return true;
            }
            return false;
        }
        boolean withdraw(ServerPlayer player){
            if(!allowed(player))return false;boolean changed=false;
            for(int i=0;i<items.size();i++){var stack=items.get(i);if(stack.isEmpty())continue;int before=stack.getCount();player.getInventory().add(stack);changed|=stack.getCount()!=before;if(stack.isEmpty())items.set(i,ItemStack.EMPTY);}
            if(changed)setChanged();return changed;
        }
        @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){super.preRemoveSideEffects(pos,state);if(level instanceof ServerLevel server){for(var stack:items)if(!stack.isEmpty())Block.popResource(server,pos,stack);items.clear();}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.putString("Owner",owner.toString());}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(64,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);try{owner=UUID.fromString(in.getStringOr("Owner",""));}catch(IllegalArgumentException e){owner=null;}}
    }
}

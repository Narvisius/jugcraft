package io.github.jimbozoomer.jugcraft.aquatic;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** A physical trophy: native hanging persistence and conserved drops, with supplied fish/plaque rendering. */
public final class OwnerFishMount extends ItemFrame {
    public static EntityType<OwnerFishMount> TYPE;
    public static Item ITEM;
    public OwnerFishMount(EntityType<? extends ItemFrame> type,Level level){super(type,level);}
    private OwnerFishMount(Level level,BlockPos pos,Direction direction){super(TYPE,level,pos,direction);}
    public static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("aquatic_fish_mount"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerFishMount>of(OwnerFishMount::new,MobCategory.MISC).sized(.75f,.5f).clientTrackingRange(10).build(key));
        ITEM=OwnerAquaculture.item("fish_mount",p->new Item(p){
            @Override public InteractionResult useOn(UseOnContext c){
                var player=c.getPlayer();var level=c.getLevel();var pos=c.getClickedPos().relative(c.getClickedFace());
                if(player==null||!player.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand()))return InteractionResult.FAIL;
                if(player instanceof ServerPlayer owner&&(!Authority.mayChange(owner.level(),owner,pos)||!Authority.mayChange(owner.level(),owner,c.getClickedPos())))return InteractionResult.FAIL;
                var mount=new OwnerFishMount(level,pos,c.getClickedFace());if(!mount.survives())return InteractionResult.FAIL;
                if(level instanceof ServerLevel server){server.addFreshEntity(mount);mount.playPlacementSound();server.gameEvent(player,GameEvent.ENTITY_PLACE,pos);c.getItemInHand().consume(1,player);}
                return InteractionResult.SUCCESS;
            }
        });
    }
    public static EntityType<?> fishType(ItemStack stack){
        if(stack.is(Items.COD))return net.minecraft.world.entity.EntityTypes.COD;if(stack.is(Items.SALMON))return net.minecraft.world.entity.EntityTypes.SALMON;
        if(stack.is(Items.PUFFERFISH))return net.minecraft.world.entity.EntityTypes.PUFFERFISH;if(stack.is(Items.TROPICAL_FISH))return net.minecraft.world.entity.EntityTypes.TROPICAL_FISH;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return id.getNamespace().equals("jugcraft")?OwnerAquaculture.TYPES.get(id.getPath().replace("aquatic_","")):null;
    }
    @Override public InteractionResult interact(Player p,InteractionHand hand,Vec3 hit){
        if(p instanceof ServerPlayer owner&&!Authority.mayChange(owner.level(),owner,blockPosition()))return InteractionResult.FAIL;
        if(!getItem().isEmpty()||fishType(p.getItemInHand(hand))==null)return InteractionResult.PASS;
        return super.interact(p,hand,hit);
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
        if(source.getEntity() instanceof ServerPlayer owner&&!Authority.mayChange(level,owner,blockPosition()))return false;
        return super.hurtServer(level,source,damage);
    }
    @Override protected ItemStack getFrameItemStack(){return new ItemStack(ITEM);}
    @Override public ItemStack getPickResult(){return new ItemStack(ITEM);}
}

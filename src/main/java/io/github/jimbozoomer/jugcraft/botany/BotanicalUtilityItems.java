package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.ClipContext;

import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.Set;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.*;

/** Small supplied tools with their actual utility, bounded loaded-world work and no research locks. */
public final class BotanicalUtilityItems {
    static final Set<String> ITEMS=Set.of("open_bucket","world_seed","ender_hand","obedience_stick","slime_bottle","spell_cloth");
    public static DataComponentType<Boolean> SLIME;
    static void register(){
        SLIME=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_slime_chunk"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_spell_cloth"),Disenchant.SERIALIZER);
    }
    static Item create(String name,Item.Properties properties){return switch(name){
        case "open_bucket"->new VoidBucket(properties.stacksTo(1));case "world_seed"->new WorldSeed(properties);case "ender_hand"->new EnderHand(properties.stacksTo(1));case "obedience_stick"->new Obedience(properties.stacksTo(1));case "spell_cloth"->new Item(properties.durability(35));default->new SlimeBottle(properties.stacksTo(1));};}
    static void tick(ServerPlayer player,ItemStack stack){
        if(!(stack.getItem() instanceof SlimeBottle)||player.tickCount%20!=0)return;
        var chunk=player.chunkPosition();boolean slime=player.level().dimension()==Level.OVERWORLD&&WorldgenRandom.seedSlimeChunk(chunk.x(),chunk.z(),player.level().getSeed(),987234911L).nextInt(10)==0;
        if(slime)stack.set(SLIME,true);else stack.remove(SLIME);
    }
    private static final class SlimeBottle extends Item {
        SlimeBottle(Properties p){super(p);}
        @Override public InteractionResult use(Level world,Player player,InteractionHand hand){if(player instanceof ServerPlayer p){var chunk=p.chunkPosition();boolean slime=p.level().dimension()==Level.OVERWORLD&&WorldgenRandom.seedSlimeChunk(chunk.x(),chunk.z(),p.level().getSeed(),987234911L).nextInt(10)==0;p.sendOverlayMessage(Component.literal(slime?"Slime chunk — slimes can spawn below Y 40 when conditions allow.":"This is not a slime chunk."));}return InteractionResult.SUCCESS;}
    }
    private static final class Obedience extends Item {
        Obedience(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var at=context.getClickedPos();if(!(p.level().getBlockEntity(at) instanceof BotanicalWorkshop target)||!target.permitted(p))return InteractionResult.PASS;
            if(target.generating()||target.functional()){target.unbind();p.sendOverlayMessage(Component.literal("Flower connection cleared."));return InteractionResult.SUCCESS;}
            boolean pool=target.kind().endsWith("pool"),spreader=target.kind().contains("spreader");if(!pool&&!spreader)return InteractionResult.PASS;
            int range=pool?10:6,count=0;
            for(var pos:BlockPos.betweenClosed(at.offset(-range,-range,-range),at.offset(range,range,range))){
                if(pos.distSqr(at)>range*range||!p.level().hasChunkAt(pos))continue;
                if(p.level().getBlockEntity(pos) instanceof BotanicalWorkshop flower&&(pool?flower.functional():flower.generating())&&flower.bind(p,at)){count++;p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,3,.1,.1,.1,0);}
            }p.sendOverlayMessage(Component.literal("Connected "+count+" flowers."));return InteractionResult.SUCCESS;
        }
    }
    private static final class EnderHand extends Item {
        EnderHand(Properties p){super(p);}
        @Override public InteractionResult use(Level world,Player user,InteractionHand hand){
            if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(p.isSpectator()||!Authority.mayChange(p.level(),p,p.blockPosition())||BotanicalManaItems.available(p)<250)return InteractionResult.FAIL;
            // Only the player's own chest is opened. No implicit access to another player's private inventory.
            if(p.openMenu(new SimpleMenuProvider((id,inventory,player)->ChestMenu.threeRows(id,inventory,p.getEnderChestInventory()),Component.translatable("container.enderchest"))).isEmpty())return InteractionResult.FAIL;
            if(!BotanicalManaItems.pay(p,250)){p.closeContainer();return InteractionResult.FAIL;}
            p.level().playSound(null,p.blockPosition(),SoundEvents.ENDER_CHEST_OPEN,SoundSource.PLAYERS,1,1);return InteractionResult.SUCCESS;
        }
    }
    private static final class VoidBucket extends Item {
        VoidBucket(Properties p){super(p);}
        @Override public InteractionResult use(Level world,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var hit=getPlayerPOVHitResult(world,p,ClipContext.Fluid.SOURCE_ONLY);return hit.getType()==HitResult.Type.BLOCK?empty(p,hit.getBlockPos()):InteractionResult.PASS;
        }
        @Override public InteractionResult useOn(UseOnContext c){return c.getPlayer() instanceof ServerPlayer p?empty(p,c.getClickedPos()):InteractionResult.SUCCESS;}
        private InteractionResult empty(ServerPlayer p,BlockPos at){
            var world=p.level();if(!world.hasChunkAt(at)||!Authority.mayChange(world,p,at))return InteractionResult.FAIL;var state=world.getBlockState(at);boolean emptied=false;
            if(state.getBlock() instanceof BucketPickup pickup){var removed=pickup.pickupBlock(p,world,at,state);emptied=!removed.isEmpty();if(emptied)pickup.getPickupSound().ifPresent(sound->world.playSound(null,at,sound,SoundSource.PLAYERS,1,1));}
            else if(state.is(Blocks.WATER_CAULDRON)||state.is(Blocks.LAVA_CAULDRON)||state.is(Blocks.POWDER_SNOW_CAULDRON))emptied=world.setBlockAndUpdate(at,Blocks.CAULDRON.defaultBlockState());
            if(emptied){world.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,at.getX()+.5,at.getY()+.8,at.getZ()+.5,5,.2,.2,.2,.01);return InteractionResult.SUCCESS;}return InteractionResult.PASS;
        }
    }
    private static final class WorldSeed extends Item {
        WorldSeed(Properties p){super(p);}
        @Override public InteractionResult use(Level world,Player user,InteractionHand hand){
            if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(world.dimension()!=Level.OVERWORLD||p.isPassenger()||p.isSpectator())return InteractionResult.FAIL;
            var spawn=p.level().getRespawnData().pos();if(spawn.distSqr(p.blockPosition())<=576)return InteractionResult.PASS;
            // Deliberate player teleport may load its one destination chunk, never search/generate an unbounded region.
            p.level().getChunk(spawn);Vec3 landing=null;
            for(int dy=0;dy<=32&&spawn.getY()+dy<p.level().getMaxY()-1;dy++){
                var pos=spawn.above(dy);var point=Vec3.atBottomCenterOf(pos).add(0,.01,0);var box=p.getBoundingBox().move(point.subtract(p.position()));
                if(p.level().getWorldBorder().isWithinBounds(pos)&&p.level().noCollision(p,box)&&!p.level().containsAnyLiquid(box)&&!p.level().getBlockState(pos.below()).getCollisionShape(p.level(),pos.below()).isEmpty()){landing=point;break;}
            }
            if(landing==null){p.sendOverlayMessage(Component.literal("No safe arrival at world spawn. The seed was not consumed."));return InteractionResult.FAIL;}
            p.teleportTo(landing.x,landing.y,landing.z);p.fallDistance=0;
            if(!p.hasInfiniteMaterials())p.getItemInHand(hand).shrink(1);
            p.level().playSound(null,p.blockPosition(),BotanicalSounds.get("world_seed_teleport"),SoundSource.PLAYERS,1,1);return InteractionResult.SUCCESS;
        }
    }
    public static final class Disenchant extends CustomRecipe {
        private static final Disenchant INSTANCE=new Disenchant();public static final RecipeSerializer<Disenchant> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Disenchant>unit(INSTANCE));
        private record Match(ItemStack target,ItemStack cloth,int slot){}
        private Match find(CraftingInput in){ItemStack target=ItemStack.EMPTY,cloth=ItemStack.EMPTY;int slot=-1;
            for(int i=0;i<in.size();i++){var s=in.getItem(i);if(s.isEmpty())continue;if(s.is(Jugtania.item("spell_cloth"))&&cloth.isEmpty()&&s.getDamageValue()<s.getMaxDamage()){cloth=s;slot=i;}else if(s.isEnchanted()&&target.isEmpty())target=s;else return null;}
            return target.isEmpty()||cloth.isEmpty()?null:new Match(target,cloth,slot);
        }
        @Override public boolean matches(CraftingInput in,Level world){return find(in)!=null;}
        @Override public ItemStack assemble(CraftingInput in){var m=find(in);if(m==null)return ItemStack.EMPTY;var result=m.target.copyWithCount(1);result.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);result.remove(DataComponents.REPAIR_COST);return result;}
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput in){var out=NonNullList.withSize(in.size(),ItemStack.EMPTY);var m=find(in);if(m!=null&&m.cloth.getDamageValue()+1<m.cloth.getMaxDamage()){var cloth=m.cloth.copyWithCount(1);cloth.setDamageValue(cloth.getDamageValue()+1);out.set(m.slot,cloth);}return out;}
        @Override public RecipeSerializer<Disenchant> getSerializer(){return SERIALIZER;}
    }
}

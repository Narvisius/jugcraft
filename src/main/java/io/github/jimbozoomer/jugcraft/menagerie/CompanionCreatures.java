package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.gear.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Native companion subset from the mixed second-batch creature folder. */
public final class CompanionCreatures {
    private CompanionCreatures(){}
    public static void register(){
        JsonObject catalog;
        try(var in=CompanionCreatures.class.getResourceAsStream("/jugcraft/menagerie/companion.json")){
            if(in==null)return;
            catalog=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("companion_sea_bunny_slime_block"));
        var slime=Registry.register(BuiltInRegistries.BLOCK,key,new SlimeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK).setId(key)));
        Menagerie.BLOCKS.put("companion_sea_bunny_slime_block",slime);
        Menagerie.item("companion_sea_bunny_slime_block",p->new BlockItem(slime,p.useBlockDescriptionPrefix()));
        for(var entry:catalog.getAsJsonObject("items").entrySet()){
            String id=entry.getKey();var value=entry.getValue().getAsJsonObject();
            Menagerie.item(id,p->{
                if(value.get("kind").getAsString().equals("spawn"))return new MenagerieItems.Spawn(value.get("species").getAsString(),p);
                String source=value.get("source").getAsString();
                if(source.endsWith("_bucket"))return new MenagerieItems.Captured(source,p.stacksTo(1));
                if(source.equals("acorn_hat"))return new GroveCreatures.Wearable("companion_acorn",EquipmentSlot.HEAD,p);
                if(source.equals("silk_lead"))return new LeadItem(p);
                if(source.equals("grappling_hook"))return new SilkGrapple(p.durability(128));
                if(source.startsWith("pearl_necklace_"))return new Necklace(p.stacksTo(1).component(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.OFFHAND).build()));
                if(source.endsWith("slime_bottle"))return new SlimeDrink(source.startsWith("sea_bunny"),p.stacksTo(16).food(new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build(),Consumables.defaultDrink().build()).usingConvertsTo(Items.GLASS_BOTTLE));
                if(source.equals("clam"))return new Clam(p);
                if(source.equals("silk_cocoon"))return new Cocoon(p);
                if(source.equals("koi_fish"))p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(.1F).build());
                return new Item(p);
            });
        }
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%20!=0)return;
            for(var player:server.getPlayerList().getPlayers()){
                String id=BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).getPath();
                if(!id.startsWith("companion_pearl_necklace_")||!player.isInWater())continue;
                int tier=id.charAt(id.length()-1)-'0';
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,60,0,true,false));
                if(tier>=2)player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,60,0,true,false));
                if(tier>=3)player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,60,0,true,false));
            }
        });
    }
    public static InteractionResult interact(MenagerieAnimal animal,Player player,InteractionHand hand){
        if(!animal.species().id().startsWith("companion_"))return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);String species=animal.species().source();
        if(held.is(Items.GLASS_BOTTLE)&&(species.equals("sea_bunny")||species.equals("snail"))){
            if(animal.isBaby()||!animal.readyToHarvest()||animal.owner()!=null&&!animal.ownedBy(player))return InteractionResult.FAIL;
            if(player instanceof ServerPlayer p){
                var bottle=new ItemStack(Menagerie.item("companion_"+species+"_slime_bottle"));
                if(held.getCount()>1&&p.getInventory().getFreeSlot()<0)return InteractionResult.FAIL;
                held.consume(1,p);if(held.isEmpty())p.setItemInHand(hand,bottle);else if(!p.getInventory().add(bottle))p.spawnAtLocation(p.level(),bottle);
                animal.harvested();
            }
            return InteractionResult.SUCCESS;
        }
        if(species.equals("dragonfly")&&animal.ownedBy(player)&&BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().matches("companion_(iron|gold|diamond|netherite)_dragonfly_armor")){
            if(player instanceof ServerPlayer p){var old=animal.getItemBySlot(EquipmentSlot.BODY).copy();animal.equipBody(held.copyWithCount(1));held.consume(1,p);if(!old.isEmpty()&&!p.getInventory().add(old))p.spawnAtLocation(p.level(),old);animal.refreshArmor();}
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    private static final class Necklace extends Item {
        Necklace(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){return player.getItemInHand(hand).get(DataComponents.EQUIPPABLE).swapWithEquipmentSlot(player.getItemInHand(hand),player);}
    }
    private static final class Clam extends Item {
        Clam(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p){var result=new ItemStack(p.getRandom().nextInt(4)==0?Menagerie.item("companion_pearl"):Items.BONE_MEAL);p.getItemInHand(hand).consume(1,p);if(!p.getInventory().add(result))p.spawnAtLocation(p.level(),result);p.getCooldowns().addCooldown(new ItemStack(this),10);}return InteractionResult.SUCCESS;
        }
    }
    private static final class Cocoon extends Item {
        Cocoon(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p){var result=new ItemStack(Menagerie.item("companion_silk"),4);p.getItemInHand(hand).consume(1,p);if(!p.getInventory().add(result))p.spawnAtLocation(p.level(),result);}return InteractionResult.SUCCESS;
        }
    }
    private static final class SlimeDrink extends Item {
        private final boolean marine;
        SlimeDrink(boolean marine,Properties p){super(p);this.marine=marine;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
            if(!level.isClientSide()){entity.removeEffect(MobEffects.POISON);entity.addEffect(new MobEffectInstance(marine?MobEffects.WATER_BREATHING:MobEffects.REGENERATION,marine?1200:100));}
            return super.finishUsingItem(stack,level,entity);
        }
    }
    /** Shares the existing bounded hook/claim checks; silk gear spends durability, not nitrogen. */
    public static final class SilkGrapple extends Item {
        SilkGrapple(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var active=GrappleHook.active(p);if(active!=null){active.discard();return InteractionResult.SUCCESS;}
            var held=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(held))return InteractionResult.PASS;
            if(!Authority.mayChange(p.level(),p,p.blockPosition()))return InteractionResult.FAIL;
            var hook=Projectile.spawnProjectileFromRotation(GrappleHook::new,p.level(),held.copyWithCount(1),p,0,JugcraftGrapple.LAUNCH_SPEED,0);
            GrappleHook.track(p,hook);held.hurtAndBreak(1,p,hand.asEquipmentSlot());p.getCooldowns().addCooldown(held,20);return InteractionResult.SUCCESS;
        }
    }
}

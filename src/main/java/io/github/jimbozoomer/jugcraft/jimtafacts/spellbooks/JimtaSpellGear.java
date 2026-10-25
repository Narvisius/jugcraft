package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.spell_power.api.*;

/** Source equipment retains its melee stats; casting uses the same finite book list and Focus gate. */
public final class JimtaSpellGear {
    public static DataComponentType<Integer> RELOAD;
    public static void register(){
        RELOAD=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimtafacts_autoload_ticks"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,6000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        for(var entry:JimtaSpellbooks.data("gear").entrySet()){
            String name=entry.getKey();var row=entry.getValue().getAsJsonObject();var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));
            Item.Properties p=new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant();
            int durability=row.get("durability").getAsInt();String school=row.get("school").getAsString(),spell=row.get("spell").getAsString();
            if(durability>0){if(!name.equals("autoloader_crossbow"))p.sword(ToolMaterial.DIAMOND,0,row.get("speed").getAsFloat());p.durability(durability);}
            var attrs=ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,row.get("damage").getAsDouble(),AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,row.get("speed").getAsDouble(),AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
            if(!school.isEmpty()){
                String nativeSchool=switch(school){case "ice"->"frost";case "blood"->"soul";default->school;};
                attrs.add(SpellSchools.getSchool("spell_power:"+nativeSchool).attributeEntry,new AttributeModifier(Jugcraft.id("jimtafacts/staff_school"),school.equals("arcane")?.1:.15,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.MAINHAND);
                if(name.equals("artificer_cane")||name.equals("pyrium_staff"))attrs.add(SpellPowerMechanics.HASTE.attributeEntry,new AttributeModifier(Jugcraft.id("jimtafacts/staff_haste"),.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.MAINHAND);
            }
            p.attributes(attrs.build());
            Item item=name.equals("autoloader_crossbow")?new Autoloader(p):new Weapon(p,name,school,!school.isEmpty()?4:spell.isEmpty()?0:1,spell.isEmpty()?new String[0]:new String[]{spell});
            JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,item));
        }
    }
    public static final class Weapon extends JimtaSpellbooks.Book {
        public final String school;
        Weapon(Properties p,String name,String school,int slots,String... fixed){super(p,name,slots,fixed);this.school=school;}
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
            if(name.equals("staff_of_the_nines")){
                if(!(user instanceof ServerPlayer player))return InteractionResult.SUCCESS;
                if(!JimtaSpellbooks.enabled()||!player.isAlive()||player.isSpectator()||!JimtaSpellcasting.ready(player,name)||ConcordanceProgress.currentFocus(player)<4)return InteractionResult.FAIL;
                var target=JimtaSpellcasting.target(player,64);if(target!=null&&(!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(player,player.level(),target)||!io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.mayHarm(player,target)||JimtaSpellcasting.ally(player,target)))return InteractionResult.FAIL;
                if(target!=null)JimtaShot.hit(player,target,10,"",player.getEyePosition());
                ConcordanceProgress.spendFocus(player,4);JimtaSpellcasting.cooldown(player,name,20);
                level.playSound(null,player.blockPosition(),SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.PLAYERS,1.5F,1.5F);
                player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,player.getEyePosition().x,player.getEyePosition().y,player.getEyePosition().z,5,.1,.1,.1,.01);
                return InteractionResult.SUCCESS;
            }
            if(slots==0)return InteractionResult.PASS;
            return super.use(level,user,hand);
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag){
            if(name.equals("staff_of_the_nines")){lines.accept(Component.literal("Use: 64-block arcane shot · 4 Focus · one second cooldown").withStyle(ChatFormatting.AQUA));return;}
            if(slots>0)super.appendHoverText(stack,context,display,lines,flag);
            if(name.equals("magehunter"))lines.accept(Component.literal("While held: 15% less incoming spell damage").withStyle(ChatFormatting.AQUA));
        }
    }
    public static final class Autoloader extends CrossbowItem {
        Autoloader(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!JimtaSpellbooks.enabled()||player.isSpectator())return InteractionResult.FAIL;
            var stack=player.getItemInHand(hand);if(level.isClientSide())return InteractionResult.SUCCESS;
            if(isCharged(stack)){var result=super.use(level,player,hand);if(!player.getProjectile(stack).isEmpty())stack.set(RELOAD,0);return result;}
            if(stack.has(RELOAD)){if(player.isShiftKeyDown())stack.remove(RELOAD);return InteractionResult.SUCCESS;}
            if(player.getProjectile(stack).isEmpty()&&!player.getAbilities().instabuild)return InteractionResult.FAIL;
            stack.set(RELOAD,0);return InteractionResult.SUCCESS;
        }
        @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){
            super.inventoryTick(stack,level,entity,slot);if(!(entity instanceof ServerPlayer player)||!stack.has(RELOAD)||!JimtaSpellbooks.enabled())return;
            int ticks=stack.getOrDefault(RELOAD,0)+1;
            if(ticks<getChargeDuration(stack,player)*3){stack.set(RELOAD,ticks);return;}
            stack.remove(RELOAD);if(isCharged(stack))return;
            var ammo=player.getProjectile(stack);if(ammo.isEmpty()&&player.getAbilities().instabuild)ammo=new ItemStack(Items.ARROW);
            if(ammo.isEmpty())return;
            var projectiles=draw(stack,ammo,player);if(projectiles.isEmpty())return;
            stack.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(projectiles));
            level.playSound(null,player.blockPosition(),SoundEvents.CROSSBOW_LOADING_END.value(),SoundSource.PLAYERS,1,1);
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,display,lines,flag);lines.accept(Component.literal("Automatically reloads after firing. Uses carried arrows; loading takes three times normal. Sneak-use cancels.").withStyle(ChatFormatting.AQUA));}
    }
}

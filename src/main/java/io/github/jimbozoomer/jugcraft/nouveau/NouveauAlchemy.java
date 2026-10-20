package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.*;

/** Shared dose transactions for jars, couriers, Wixies, melders and diffusers. */
public final class NouveauAlchemy {
    private NouveauAlchemy(){}
    public static List<NouveauWorkshop> jars(ServerLevel l,ServerPlayer p,NouveauWorkshop source){
        List<NouveauWorkshop> jars=new ArrayList<>();
        var positions=new LinkedHashSet<BlockPos>(source.links());for(var direction:Direction.values())positions.add(source.getBlockPos().relative(direction));
        for(BlockPos pos:positions)if(l.hasChunkAt(pos)&&l.getBlockEntity(pos) instanceof NouveauWorkshop jar&&jar.kind().equals("potion_jar")&&jar.permitted(p))jars.add(jar);
        return jars;
    }
    public static ItemStack bottle(PotionContents potion){var bottle=new ItemStack(Items.POTION);bottle.set(DataComponents.POTION_CONTENTS,potion);return bottle;}
    public static InteractionResult interact(ServerPlayer p,NouveauWorkshop jar,InteractionHand hand){
        if(!jar.mayUse(p)||!RateGate.allow(p,"nouveau_jar",4))return InteractionResult.FAIL;
        var held=p.getItemInHand(hand);
        if(held.is(JugNouveau.WAND)&&p.isShiftKeyDown()){jar.toggleLock();p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(jar.locked()?"Potion type locked":"Potion type unlocked"));return InteractionResult.SUCCESS;}
        if(held.getItem() instanceof NouveauPotions.Flask){
            int stored=held.getOrDefault(NouveauPotions.DOSES,0);
            var contents=held.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            if(p.isShiftKeyDown()){
                int n=Math.min(stored,100-jar.doses());if(n==0||!jar.fillPotion(contents,n))return InteractionResult.FAIL;held.set(NouveauPotions.DOSES,stored-n);
            }else{
                int n=Math.min(8-stored,jar.doses());if(n==0||stored>0&&!contents.equals(jar.potion()))return InteractionResult.FAIL;
                held.set(DataComponents.POTION_CONTENTS,jar.potion());jar.drainPotion(n);held.set(NouveauPotions.DOSES,stored+n);
            }return InteractionResult.SUCCESS;
        }
        if(held.is(Items.POTION)){
            var contents=held.get(DataComponents.POTION_CONTENTS);if(contents==null||!jar.fillPotion(contents,1))return InteractionResult.FAIL;
            held.consume(1,p);give(p,new ItemStack(Items.GLASS_BOTTLE));return InteractionResult.SUCCESS;
        }
        if(held.is(Items.GLASS_BOTTLE)&&jar.doses()>0){var output=bottle(jar.potion());jar.drainPotion(1);held.consume(1,p);give(p,output);return InteractionResult.SUCCESS;}
        if(held.is(Items.ARROW)&&jar.doses()>0){int count=Math.min(8,held.getCount());var arrows=new ItemStack(Items.TIPPED_ARROW,count);arrows.set(DataComponents.POTION_CONTENTS,jar.potion());jar.drainPotion(1);held.consume(count,p);give(p,arrows);return InteractionResult.SUCCESS;}
        return InteractionResult.PASS;
    }
    private static void give(ServerPlayer p,ItemStack stack){if(!p.getInventory().add(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
    public static void diffuse(ServerLevel l,ServerPlayer p,NouveauWorkshop diffuser){
        if(!diffuser.diffusing())for(var jar:jars(l,p,diffuser))if(jar.doses()>0&&jar.potion().hasEffects()){
            diffuser.diffusion(jar.potion(),600);jar.drainPotion(1);break;
        }
        if(!diffuser.diffusing()){diffuser.status("Link a filled potion jar");return;}
        diffuser.advanceDiffuser();if(!diffuser.every(3))return;
        for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(diffuser.getBlockPos()).inflate(5),e->e.isAlive()).stream().limit(16).toList()){
            var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>();
            for(var effect:diffuser.potion().getAllEffects())effects.add(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(),Math.min(100,effect.getDuration()),effect.getAmplifier()));
            NouveauPotions.apply(p,new PotionContents(Optional.empty(),Optional.empty(),effects,Optional.empty()),target,1);
        }
        diffuser.setActive(true);diffuser.status("Diffusing linked potion");
    }
    public static boolean meld(ServerLevel l,ServerPlayer p,NouveauWorkshop melder){
        var jars=jars(l,p,melder);if(jars.size()<3)return false;
        var a=jars.get(0);var b=jars.get(1);var out=jars.get(2);
        if(a==b||a==out||b==out||a.doses()<3||b.doses()<3)return false;
        Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,net.minecraft.world.effect.MobEffectInstance> effects=new LinkedHashMap<>();
        for(var potion:List.of(a.potion(),b.potion()))for(var e:potion.getAllEffects()){
            var old=effects.get(e.getEffect());if(old==null||e.getAmplifier()>old.getAmplifier()||e.getAmplifier()==old.getAmplifier()&&e.getDuration()>old.getDuration())effects.put(e.getEffect(),new net.minecraft.world.effect.MobEffectInstance(e));
        }
        if(effects.size()>8||effects.isEmpty())return false;
        var mixed=new PotionContents(Optional.empty(),Optional.empty(),List.copyOf(effects.values()),Optional.empty());
        if(!out.acceptsPotion(mixed,3)||!NouveauSource.pay(l,p,melder,4))return false;
        a.drainPotion(3);b.drainPotion(3);out.fillPotion(mixed,3);melder.status("Three combined doses brewed");NouveauSource.brewing(l,p,melder.getBlockPos(),6);return true;
    }
    public static boolean brew(ServerLevel l,ServerPlayer p,NouveauWorkshop home){
        var wanted=home.sample().get(DataComponents.POTION_CONTENTS);if(wanted==null)return false;
        var jars=jars(l,p,home);if(jars.isEmpty())return false;
        List<Container> supplies=new ArrayList<>();supplies.add(home);
        supplies.addAll(NouveauDevices.inventories(l,p,home));
        var inputs=new ArrayList<NouveauWorkshop>(jars);inputs.add(null);
        for(var source:inputs)if(source==null||source.doses()>=3)for(Container supply:supplies){
            for(int slot=supply==home?1:0;slot<Math.min(supply==home?9:54,supply.getContainerSize());slot++){
                var reagent=supply.getItem(slot);if(reagent.isEmpty())continue;
                var input=new BrewingInput(bottle(source==null?new PotionContents(net.minecraft.world.item.alchemy.Potions.WATER):source.potion()),reagent);
                var recipe=l.getServer().getRecipeManager().getRecipeFor(RecipeType.BREWING,input,l);if(recipe.isEmpty())continue;
                var result=recipe.get().value().assemble(input);var potion=result.get(DataComponents.POTION_CONTENTS);
                if(potion==null||!potion.equals(wanted))continue;
                for(var destination:jars)if(destination!=source&&destination.acceptsPotion(potion,3)){
                    ItemStack remainder=reagent.getItem().getCraftingRemainder()==null?ItemStack.EMPTY:reagent.getItem().getCraftingRemainder().create();
                    if(!remainder.isEmpty()&&!home.canOutput(remainder))return false;
                    if(source!=null)source.drainPotion(3);destination.fillPotion(potion,3);supply.removeItem(slot,1);supply.setChanged();if(!remainder.isEmpty())home.output(remainder);
                    NouveauSource.brewing(l,p,home.getBlockPos(),3);return true;
                }
            }
        }return false;
    }
}

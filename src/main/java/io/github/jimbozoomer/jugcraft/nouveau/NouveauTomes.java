package io.github.jimbozoomer.jugcraft.nouveau;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;
import org.jspecify.annotations.Nullable;

/** Fixed server-owned supplied presets. Repeated augments are retained within bounded native effect limits.
 * No client-supplied plan, global grammar relaxation, recursive spell casting or separate resource pool.
 */
public final class NouveauTomes {
    private static final int MAX_PARTS=32,MAX_STAGES=12;
    private static final Ledger.Limits LIMITS=new Ledger.Limits(64,384,12);
    private NouveauTomes(){}
    public static String name(NouveauRecipes.Working r){String title=r.data().has("name")?r.data().get("name").getAsString():"";return title.isBlank()?r.id().substring(r.id().lastIndexOf('/')+1).replace('_',' '):title;}
    public static ItemStack stack(NouveauRecipes.Working r){
        var stack=new ItemStack(NouveauCatalog.item("caster_tome"));stack.set(NouveauGear.TOME,r.id());
        stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(name(r)));
        var lore=new ArrayList<net.minecraft.network.chat.Component>();
        if(r.data().has("flavour_text"))lore.add(net.minecraft.network.chat.Component.literal(r.data().get("flavour_text").getAsString()));
        lore.add(net.minecraft.network.chat.Component.literal("Use to cast • Sneak-use to inspect • Costs Focus"));
        stack.set(DataComponents.LORE,new ItemLore(lore));
        if(r.data().has("color")){var c=r.data().getAsJsonObject("color");int rgb=(Math.clamp(c.get("r").getAsInt(),0,255)<<16)|(Math.clamp(c.get("g").getAsInt(),0,255)<<8)|Math.clamp(c.get("b").getAsInt(),0,255);stack.set(DataComponents.DYED_COLOR,new DyedItemColor(rgb));}
        if(r.data().has("sound")){var config=r.data().getAsJsonObject("sound");if(config.has("sound")){var sound=config.getAsJsonObject("sound").get("id").getAsString();for(int i=0;i<NouveauControls.SOUNDS.length;i++)if(sound.endsWith(NouveauControls.SOUNDS[i]))stack.set(NouveauControls.SOUND,i);}}
        return stack;
    }
    private static final class Stage {
        String delivery;Component.Form form;int range,radius;Map<String,Integer> deliveryMods=new HashMap<>();
        List<Plan.Step> steps=new ArrayList<>();
        Stage(String delivery,Component.Delivery d){this.delivery=delivery;form=d.form();range=d.range();}
    }
    public static @Nullable Plan plan(NouveauRecipes.Working recipe){
        var parts=recipe.data().getAsJsonArray("spell");if(parts==null||parts.isEmpty()||parts.size()>MAX_PARTS)return null;
        var catalog=ConcordanceData.rules().catalog();var stages=new ArrayList<Stage>();Stage current=null;String last="";
        int cost=0,index=0;
        for(var token:parts){
            String name=token.getAsString().substring(token.getAsString().indexOf(':')+1).replaceFirst("^nouveau_","").replaceFirst("^glyph_","");
            String id="jugcraft:nouveau_"+name;var component=catalog.component(id);if(component==null)return null;cost+=component.focus();
            if(component.part() instanceof Component.Delivery delivery){if(stages.size()>=MAX_STAGES)return null;current=new Stage(id,delivery);stages.add(current);last="delivery";}
            else if(current==null)return null;
            else if(component.part() instanceof Component.Selection selection){current.radius=selection.radius();last="selection";}
            else if(component.part() instanceof Component.Operation operation){var effect=operation.effect();if(current.radius>0)effect=radius(effect,current.radius);current.steps.add(new Plan.Step(index++,id,effect,operation.principle(),operation.scaling()));last="operation";}
            else if(component.part() instanceof Component.Modifier modifier){
                String aspect=modifier.aspect().id();
                if(last.equals("delivery"))current.deliveryMods.merge("delivery_"+aspect,1,Integer::sum);
                else if(last.equals("selection")){if(aspect.equals("radius"))current.radius=Math.min(6,current.radius+modifier.amount());else current.deliveryMods.merge("delivery_"+aspect,1,Integer::sum);}
                else if(!current.steps.isEmpty()){
                    int n=current.steps.size()-1;var old=current.steps.get(n);var e=old.effect();
                    e=switch(aspect){
                        case "magnitude"->e.withMagnitude(Math.min(40,e.magnitude()+Math.max(1,e.magnitude()*modifier.amount()/100)));
                        case "dampen"->e.withMagnitude(Math.max(0,e.magnitude()-Math.max(1,e.magnitude()*modifier.amount()/100)));
                        case "duration"->e.withDuration(e.duration()+Math.max(1,e.duration()*modifier.amount()/100));
                        case "duration_down"->e.withDuration(Math.max(1,e.duration()-Math.max(1,e.duration()*modifier.amount()/100)));
                        case "radius"->radius(e,Math.min(6,e.augment("radius")+modifier.amount()));
                        default->e.withAugment(aspect,Math.min(4,e.augment(aspect)+1));
                    };current.steps.set(n,new Plan.Step(old.index(),old.component(),e,old.principle(),old.scaling()));
                }else return null;
            }else return null;
        }
        if(index==0)return null;Plan.Node next=null;
        for(int i=stages.size()-1;i>=0;i--){
            var stage=stages.get(i);var steps=new ArrayList<Plan.Step>();
            for(var step:stage.steps){var e=step.effect();for(var entry:stage.deliveryMods.entrySet())e=e.withAugment(entry.getKey(),entry.getValue());steps.add(new Plan.Step(step.index(),step.component(),e,step.principle(),step.scaling()));}
            // Empty delivery stages also carry their flags so nested orbit/linger/rune shapes retain their settings.
            if(steps.isEmpty()&&!stage.deliveryMods.isEmpty()){var e=new EffectSpec(EffectKind.NOUVEAU,Intent.HELPFUL,0,0,"jugcraft:nouveau/reset",Stacking.STRONGEST,null);for(var entry:stage.deliveryMods.entrySet())e=e.withAugment(entry.getKey(),entry.getValue());steps.add(new Plan.Step(index++,"jugcraft:nouveau_reset",e,"radiance",0));}
            next=new Plan.Node(i,stage.delivery,stage.form,stage.range,"jugcraft:struck",Component.Pick.STRUCK,0,64,steps,null,1,0,next);
        }
        int focus=Math.max(1,(cost+1)/2);return new Plan("Supplied tome: "+recipe.id(),next,focus,Math.max(20,focus*5),parts.size(),LIMITS,0);
    }
    private static EffectSpec radius(EffectSpec effect,int radius){var augments=new HashMap<>(effect.augments());augments.put("radius",Math.clamp(radius,0,6));return new EffectSpec(effect.kind(),effect.intent(),effect.magnitude(),effect.duration(),effect.status(),effect.stacking(),effect.school(),augments);}
    public static boolean preset(Cause cause){return cause.source().startsWith("jugcraft:nouveau_tome/");}
    public static boolean next(ServerLevel level,ServerPlayer caster,Plan.Node node,Cause cause,Ledger ledger,ComposedSpells.Impact impact){
        if(node.then()==null||!ledger.branch())return false;
        var next=new Plan("Supplied tome",node.then(),0,0,0,LIMITS,0);
        // These are stages of one finite authored preset, not independently triggered spells.
        return NouveauSpellDelivery.device(level,caster,next,cause,ledger,impact,caster.getLookAngle());
    }
    public static InteractionResult cast(ServerPlayer player,ItemStack stack){
        var recipe=NouveauRecipes.all().stream().filter(r->r.operation().equals("caster_tome")&&r.id().equals(stack.get(NouveauGear.TOME))).findFirst().orElse(null);
        var plan=recipe==null?null:plan(recipe);if(plan==null)return InteractionResult.FAIL;
        if(player.isShiftKeyDown()){player.sendSystemMessage(net.minecraft.network.chat.Component.literal(name(recipe)+" • "+plan.focus()+" Focus • "+recipe.data().get("spell")));return InteractionResult.SUCCESS;}
        int cost=NouveauGear.spellCost(player,plan);
        if(ConcordanceProgress.currentFocus(player)<cost||!RateGate.allow(player,"nouveau_cast",plan.cooldown()))return InteractionResult.FAIL;
        var result=ComposedSpells.perform(player.level(),player,plan,Cause.of(player.getUUID(),Cause.Origin.ITEM,"jugcraft:nouveau_tome/"+recipe.id(),ConcordanceEffects.nextSerial()),new Ledger(LIMITS));
        if(result.applied())ConcordanceProgress.spendFocus(player,cost);return result.applied()?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
}

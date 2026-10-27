package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.*;

/** Stock accounting around native staged/aging jobs. It never processes or invents items. */
final class CompanionAdditionalStock {
    static boolean supports(BlockEntity b){return b instanceof CiderPressBlockEntity || b instanceof CiderBarrelBlockEntity
        || b instanceof CanningKettleBlockEntity || b instanceof CandyKettleBlockEntity || b instanceof SpinningWheelBlockEntity
        || b instanceof PunchBowlBlockEntity || b instanceof BrewingStandBlockEntity || b instanceof CrucibleBlockEntity;}
    private static ItemStack item(String id){return new ItemStack(JugcraftAgriculture.item(id));}
    static ItemStack product(BlockEntity b){
        if(b instanceof CiderPressBlockEntity)return item("sweet_cider");
        if(b instanceof CiderBarrelBlockEntity c)return item(CiderBarrelBlockEntity.STAGES.get(c.companionStage()));
        if(b instanceof PunchBowlBlockEntity)return item("witchs_brew_punch");
        if(b instanceof CandyKettleBlockEntity c)return c.companion.stockProduct();
        if(b instanceof CrucibleBlockEntity c)return c.companion.stockProduct();
        if(b instanceof BrewingStandBlockEntity)return VanillaCompanionPort.of(b).brewingStock().stockProduct();
        return ItemStack.EMPTY;
    }
    static List<ItemStack> results(BlockEntity b,ItemStack raw){
        if(b instanceof SpinningWheelBlockEntity){var color=Knitting.woolColor(raw);return color==null?List.of():List.of(Knitting.yarn(color.getTextureDiffuseColor(),Knitting.YARN_PER_WOOL));}
        if(b instanceof CanningKettleBlockEntity){if(!(raw.getItem() instanceof PreserveJarItem))return List.of();var out=raw.copyWithCount(1);PreserveJarItem.seal(out);return List.of(out);}
        var p=product(b);return p.isEmpty()?List.of():List.of(p);
    }
    private static int matching(List<ItemStack> stacks,java.util.function.Predicate<ItemStack> predicate){return stacks.stream().filter(predicate).mapToInt(ItemStack::getCount).sum();}
    static boolean busy(BlockEntity b){
        if(b instanceof CiderPressBlockEntity c)return c.pendingCider()>0;
        if(b instanceof CiderBarrelBlockEntity c)return c.servings()>0;
        if(b instanceof CanningKettleBlockEntity c)return !c.jars().isEmpty();
        if(b instanceof CandyKettleBlockEntity c)return c.companion.stockBusy();
        if(b instanceof SpinningWheelBlockEntity c)return c.wool()!=null;
        if(b instanceof PunchBowlBlockEntity c)return c.companion.servings()>0;
        if(b instanceof BrewingStandBlockEntity)return VanillaCompanionPort.of(b).brewingStock().stockBusy();
        return b instanceof CrucibleBlockEntity c && c.companion.stockBusy();
    }
    private static Storage<ItemVariant> outputs(BlockEntity b){
        if(b instanceof CiderPressBlockEntity c)return c.companionOutputs();
        if(b instanceof CiderBarrelBlockEntity c)return c.companionOutputs();
        if(b instanceof CanningKettleBlockEntity c)return c.companionOutputs();
        if(b instanceof CandyKettleBlockEntity c)return c.companion.outputs();
        if(b instanceof SpinningWheelBlockEntity c)return c.companionOutputs();
        if(b instanceof BrewingStandBlockEntity)return VanillaCompanionPort.of(b).outputs();
        if(b instanceof CrucibleBlockEntity c)return c.companion.outputs();
        return Storage.empty();
    }
    static int finished(BlockEntity b,ItemStack icon,CompanionProductionStock stock){
        if(b instanceof PunchBowlBlockEntity c)return stock.matches(icon,c.bottled)?c.bottled.getCount():0;
        int n=0;for(var view:outputs(b))if(!view.isResourceBlank() && stock.matches(icon,view.getResource().toStack(1)))n+=(int)view.getAmount();return n;
    }
    static int pending(BlockEntity b,ItemStack icon,List<ItemStack> incoming,CompanionProductionStock stock){
        if(b instanceof SpinningWheelBlockEntity c){
            int n=c.wool()!=null && stock.matches(icon,Knitting.yarn(c.wool().getTextureDiffuseColor(),1))?Knitting.YARN_PER_WOOL:0;
            for(var raw:incoming)for(var out:results(b,raw))if(stock.matches(icon,out))n+=raw.getCount()*out.getCount();return n;
        }
        if(b instanceof CanningKettleBlockEntity c){
            int n=0;var jars=new ArrayList<>(c.jars());jars.addAll(incoming);
            for(var raw:jars)if(PreserveJarItem.sealable(raw,b.getLevel().getGameTime()))for(var out:results(b,raw))if(stock.matches(icon,out))n+=raw.getCount();return n;
        }
        var product=product(b);if(product.isEmpty() || !stock.matches(icon,product))return 0;
        if(b instanceof CiderPressBlockEntity c)return c.pendingCider()+matching(incoming,s->s.is(CiderPressBlock.APPLES));
        if(b instanceof CiderBarrelBlockEntity c)return c.servings()-finished(b,icon,stock)+matching(incoming,s->s.is(JugcraftAgriculture.item("sweet_cider")));
        if(b instanceof PunchBowlBlockEntity c)return c.companion.servings()+PunchBowlBlock.PER_BERRY*matching(incoming,s->s.is(PunchBowlBlock.INGREDIENTS));
        if(b instanceof CandyKettleBlockEntity c)return c.companion.stockPending(incoming);
        if(b instanceof BrewingStandBlockEntity)return VanillaCompanionPort.of(b).brewingStock().stockPending(incoming);
        return b instanceof CrucibleBlockEntity c?c.companion.stockPending(incoming):0;
    }
    private static int deficit(PeepoEntity npc,BlockEntity b,ItemStack out,CompanionProductionStock stock){
        if(out.isEmpty() || !CompanionFilters.get(b).allows(out))return 0;
        int goal=stock.inputGoal(npc,b,out);return goal==Integer.MAX_VALUE?goal:Math.max(0,goal-pending(b,out,stock.incoming(),stock));
    }
    private static int cap(int nativeNeed,int missing,int yield){return missing==Integer.MAX_VALUE?nativeNeed:Math.min(nativeNeed,(missing+yield-1)/yield);}
    static int limit(PeepoEntity npc,BlockEntity b,ItemStack raw,int nativeNeed,CompanionProductionStock stock){
        if(nativeNeed<=0)return 0;var incoming=stock.incoming();int reserved=matching(incoming,s->ItemStack.isSameItemSameComponents(s,raw));
        if(b instanceof SpinningWheelBlockEntity || b instanceof CanningKettleBlockEntity && raw.getItem() instanceof PreserveJarItem){
            var results=results(b,raw);if(results.isEmpty())return 0;var out=results.getFirst();return cap(nativeNeed,deficit(npc,b,out,stock),out.getCount());
        }
        if(b instanceof CanningKettleBlockEntity){
            // Water is reusable infrastructure, not a finished jar or consumable production batch.
            boolean wanted=busy(b) || !incoming.isEmpty();var filter=CompanionFilters.get(b);
            for(int i=0;i<9 && !wanted;i++){var icon=filter.get(i);wanted=!icon.isEmpty() && stock.wants(npc,b,icon);}
            return wanted?Math.max(0,nativeNeed-reserved):0;
        }
        if(b instanceof CiderPressBlockEntity && raw.is(CiderPressBlock.APPLES))return cap(nativeNeed,deficit(npc,b,product(b),stock),1);
        if(b instanceof CiderBarrelBlockEntity && raw.is(JugcraftAgriculture.item("sweet_cider")))return cap(nativeNeed,deficit(npc,b,product(b),stock),1);
        if(b instanceof PunchBowlBlockEntity && raw.is(PunchBowlBlock.INGREDIENTS))return cap(nativeNeed,deficit(npc,b,product(b),stock),PunchBowlBlock.PER_BERRY);
        if(b instanceof BrewingStandBlockEntity && VanillaCompanionPort.of(b).brewingStock().startingBottle(raw))return cap(nativeNeed,deficit(npc,b,product(b),stock),1);
        // Containers, reagents, trays and later formula steps must finish an existing/reserved batch.
        boolean committed=busy(b) || !incoming.isEmpty();
        return committed || deficit(npc,b,product(b),stock)>0?Math.max(0,nativeNeed-reserved):0;
    }
    static void prepare(PeepoEntity npc,BlockEntity b,Storage<ItemVariant> source,CompanionLogistics.Port base,CompanionProductionStock stock){
        if(b instanceof BrewingStandBlockEntity){
            var brewing=VanillaCompanionPort.of(b).brewingStock();
            if(!stock.incoming().isEmpty())base.prepare(source);
            else brewing.prepare(source,s->stock.inputGoal(npc,b,s),s->stock.priority(npc,b,s));
            return;
        }
        if(b instanceof CandyKettleBlockEntity c){
            if(!stock.incoming().isEmpty())return;
            c.companion.prepare(source,s->stock.inputGoal(npc,b,s),s->stock.priority(npc,b,s));return;
        }
        if(b instanceof CiderBarrelBlockEntity c && c.servings()==0 && stock.incoming().isEmpty()){
            int stage=0;double best=-Double.MAX_VALUE;
            for(int i=1;i<=2;i++){var out=item(CiderBarrelBlockEntity.STAGES.get(i));if(c.filter().allows(out)){double score=stock.priority(npc,b,out);if(score>best){stage=i;best=score;}}}
            if(stage>0)c.stockStage(stage);
        }
        if(b instanceof CrucibleBlockEntity c && !c.companion.stockBusy() && stock.incoming().isEmpty()){
            var draught=new ItemStack(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.DRAUGHT);var salve=new ItemStack(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.SALVE);
            c.companion.stockForm(c.companion.filter.allows(salve) && (!c.companion.filter.allows(draught) || stock.priority(npc,b,salve)>stock.priority(npc,b,draught)));
        }
        base.prepare(source);
    }
}

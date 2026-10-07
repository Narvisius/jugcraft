package io.github.jimbozoomer.jugcraft.creatures.scary;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;

public final class DropClientTests implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public void runTest(ClientGameTestContext context) {
        if("1".equals(System.getenv("SCARY_SHOWCASE")))return;
        try(var world=context.worldBuilder().create()) {
        world.getConnection().waitForChunksRender();
        world.getServer().runOnServer(server->{
            for(var kind:ScaryDrops.Kind.values()) {
                var seen=new java.util.HashSet<String>();var rng=RandomSource.create(73);
                for(int i=0;i<10000;i++) for(var stack:ScaryDrops.roll(kind,true,rng,id->BuiltInRegistries.ITEM.getOptional(id).orElse(null))) {
                    seen.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
                    if(stack.getItem() instanceof io.github.jimbozoomer.jugcraft.gear.ScubaTankItem)
                        check(io.github.jimbozoomer.jugcraft.gear.ScubaTankItem.oxygen(stack)==0,"Tank must be empty");
                }
                var expected=switch(kind) {
                    case SPACE_KOOK -> java.util.Set.of("aluminum_ingot","aluminum_plate","titanium_ingot","basic_circuit");
                    case CAPTAIN_CUTLER -> java.util.Set.of("kelp","brass_ingot","rubber","scuba_mask","scuba_tank");
                    case BLACK_KNIGHT -> java.util.Set.of("steel_ingot","steel_greatsword","steel_helmet","steel_chestplate","steel_leggings","steel_boots");
                };
                check(seen.equals(expected),"Real Jugcraft loot: "+seen);
            }
            // Standalone registry: optional IDs are absent, kelp still drops, no invalid stacks.
            for(var kind:ScaryDrops.Kind.values())for(int i=0;i<100;i++) {
                var drops=ScaryDrops.roll(kind,true,RandomSource.create(i),id->BuiltInRegistries.ITEM.getOptional(id).orElse(null));
                if(!BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.parse("jugcraft:steel_ingot"))) {
                    check(drops.size()==(kind==ScaryDrops.Kind.CAPTAIN_CUTLER?1:0),"Standalone fallback");
                    if(!drops.isEmpty())check(drops.getFirst().is(Items.KELP),"Standalone kelp");
                }
            }
            // Representative registry items test all configured rolls, counts, armor choice and durability.
            Map<String,Item> items=new HashMap<>();
            items.put("aluminum_ingot",Items.IRON_INGOT);items.put("aluminum_plate",Items.PAPER);
            items.put("titanium_ingot",Items.GOLD_INGOT);items.put("basic_circuit",Items.REDSTONE);
            items.put("kelp",Items.KELP);items.put("brass_ingot",Items.COPPER_INGOT);items.put("rubber",Items.SLIME_BALL);
            items.put("scuba_mask",Items.LEATHER_HELMET);items.put("scuba_tank",Items.LEATHER_CHESTPLATE);
            items.put("steel_ingot",Items.IRON_INGOT);items.put("steel_greatsword",Items.IRON_SWORD);
            items.put("steel_helmet",Items.IRON_HELMET);items.put("steel_chestplate",Items.IRON_CHESTPLATE);
            items.put("steel_leggings",Items.IRON_LEGGINGS);items.put("steel_boots",Items.IRON_BOOTS);
            Map<Item,Double> rates=Map.of(Items.PAPER,.25,Items.GOLD_INGOT,.10,Items.REDSTONE,.05,
                Items.SLIME_BALL,.15,Items.LEATHER_HELMET,.03,Items.LEATHER_CHESTPLATE,.02,
                Items.IRON_SWORD,.04,Items.IRON_HELMET,.0075,Items.IRON_CHESTPLATE,.0075);
            Map<Item,Integer> counts=new HashMap<>();int n=50000;
            for(var kind:ScaryDrops.Kind.values()) {
                var random=RandomSource.create(874324);
                for(int i=0;i<n;i++) {
                    var drops=ScaryDrops.roll(kind,true,random,id->items.get(id.getPath()));
                    int ingots=0,kelp=0;
                    for(var stack:drops) {
                        check(!stack.isEmpty(),"Empty stack");
                        counts.merge(stack.getItem(),1,Integer::sum);
                        if(stack.is(Items.IRON_INGOT))ingots+=stack.getCount();
                        if(stack.is(Items.KELP))kelp+=stack.getCount();
                        if(stack.isDamageableItem()) {
                            double used=(double)stack.getDamageValue()/stack.getMaxDamage();
                            check(used>=.39 && used<=.77 && stack.getDamageValue()<stack.getMaxDamage(),"Equipment durability");
                        }
                    }
                    if(kind==ScaryDrops.Kind.SPACE_KOOK)check(ingots>=1 && ingots<=3,"Kook ingot range");
                    if(kind==ScaryDrops.Kind.BLACK_KNIGHT)check(ingots>=1 && ingots<=4,"Knight ingot range");
                    if(kind==ScaryDrops.Kind.CAPTAIN_CUTLER)check(kelp>=1 && kelp<=3,"Kelp range");
                    for(var stack:ScaryDrops.roll(kind,false,random,id->items.get(id.getPath())))
                        check(!stack.isDamageableItem(),"Equipment from non-player kill");
                }
            }
            rates.forEach((item,expected)->check(Math.abs(counts.getOrDefault(item,0)/(double)n-expected)<.01,"Drop rate: "+item));
        });
        }
    }
}

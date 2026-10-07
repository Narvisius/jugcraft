package io.github.jimbozoomer.jugcraft.creatures.scary;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;

/** Optional Jugcraft loot, resolved at death so the standalone mod needs no Jugcraft classes. */
public final class ScaryDrops {
    public enum Kind { SPACE_KOOK, CAPTAIN_CUTLER, BLACK_KNIGHT }
    private record Drop(String[] ids,int min,int max,float chance,boolean equipment) {}
    private static Drop material(String id,int min,int max,float chance) {
        return new Drop(new String[]{id},min,max,chance,false);
    }
    private static Drop equipment(float chance,String... ids) {return new Drop(ids,1,1,chance,true);}
    private static final Map<Kind,List<Drop>> DROPS=Map.of(
        Kind.SPACE_KOOK,List.of(
            material("jugcraft:aluminum_ingot",1,3,1),
            material("jugcraft:aluminum_plate",1,1,.25F),
            material("jugcraft:titanium_ingot",1,1,.10F),
            material("jugcraft:basic_circuit",1,1,.05F)),
        Kind.CAPTAIN_CUTLER,List.of(
            material("minecraft:kelp",1,3,1),
            material("jugcraft:brass_ingot",0,2,1),
            material("jugcraft:rubber",1,1,.15F),
            equipment(.03F,"jugcraft:scuba_mask"),
            equipment(.02F,"jugcraft:scuba_tank")),
        Kind.BLACK_KNIGHT,List.of(
            material("jugcraft:steel_ingot",1,3,1),
            material("jugcraft:steel_ingot",1,1,.20F),
            equipment(.04F,"jugcraft:steel_greatsword"),
            equipment(.03F,"jugcraft:steel_helmet","jugcraft:steel_chestplate","jugcraft:steel_leggings","jugcraft:steel_boots")));

    public static void drop(Mob mob,ServerLevel level,boolean playerKilled,Kind kind) {
        for(var stack:roll(kind,playerKilled,mob.getRandom(),id->BuiltInRegistries.ITEM.getOptional(id).orElse(null)))
            mob.spawnAtLocation(level,stack);
    }

    // Package-visible resolver also permits deterministic compatibility tests without installing Jugcraft.
    static List<ItemStack> roll(Kind kind,boolean playerKilled,RandomSource random,Function<Identifier,Item> items) {
        List<ItemStack> result=new ArrayList<>();
        for(var rule:DROPS.get(kind)) {
            if(rule.equipment && !playerKilled)continue;
            if(random.nextFloat()>=rule.chance)continue;
            int count=rule.min+random.nextInt(rule.max-rule.min+1);
            if(count==0)continue;
            String id=rule.ids[random.nextInt(rule.ids.length)];
            Item item=items.apply(Identifier.parse(id));
            // Missing optional items are omitted, never substituted with air or duplicate registrations.
            if(item==null || item==Items.AIR)continue;
            ItemStack stack=new ItemStack(item,count);
            if(rule.equipment && stack.isDamageableItem()) {
                // 25–60% remaining durability. New scuba tanks retain their registered zero-oxygen default.
                int damage=(int)Math.ceil(stack.getMaxDamage()*(.40F+random.nextFloat()*.35F));
                stack.setDamageValue(Math.min(stack.getMaxDamage()-1,Math.max(1,damage)));
            }
            result.add(stack);
        }
        return result;
    }
}

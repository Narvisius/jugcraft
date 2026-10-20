package io.github.jimbozoomer.jugcraft.nouveau;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
public final class NouveauFilters {
    private NouveauFilters(){}
    public static boolean allows(ItemStack filter,ItemStack item){
        if(filter.isEmpty())return true;
        if(filter.is(NouveauCatalog.item("allow_scroll"))||filter.is(NouveauCatalog.item("deny_scroll"))){boolean match=filter.getOrDefault(NouveauUtilities.FILTER,List.of()).contains(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());return filter.is(NouveauCatalog.item("deny_scroll"))?!match:match;}
        return ItemStack.isSameItemSameComponents(filter,item);
    }
    public static boolean frames(ServerLevel l,BlockPos at,ItemStack stack){
        var frames=l.getEntitiesOfClass(ItemFrame.class,new AABB(at).inflate(1),f->!f.getItem().isEmpty()&&f.getPos().relative(f.getDirection().getOpposite()).equals(at));
        return frames.isEmpty()||frames.stream().anyMatch(f->allows(f.getItem(),stack));
    }
}

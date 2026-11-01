package io.github.jimbozoomer.jugcraft.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Counts are serialized separately: vanilla ItemStack codecs do not accept huge stacks. */
public final class OwnerBackpackStacks {
    private OwnerBackpackStacks(){}
    public record Entry(ItemStack sample,int count){
        public Entry {sample=sample.isEmpty()?ItemStack.EMPTY:sample.copyWithCount(1);count=sample.isEmpty()?0:Math.max(0,count);}
        public static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(Entry::sample),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("count").forGetter(Entry::count)).apply(i,Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf,Entry> STREAM=StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,Entry::sample,ByteBufCodecs.VAR_INT,Entry::count,Entry::new);
        public ItemStack stack(){return count==0?ItemStack.EMPTY:sample.copyWithCount(count);}
    }
    public static double factor(ItemStack item){
        if(!(item.getItem() instanceof OwnerBackpacks.UpgradeItem upgrade))return 1;
        return switch(upgrade.name){
            case "stack_upgrade_starter_tier"->1.5;
            case "stack_upgrade_tier_1"->2;
            case "stack_upgrade_tier_2"->4;
            case "stack_upgrade_tier_3"->8;
            case "stack_upgrade_tier_4"->16;
            case "stack_upgrade_omega_tier"->Integer.MAX_VALUE;
            case "stack_downgrade_tier_1"->.125;
            case "stack_downgrade_tier_2"->.0625;
            case "stack_downgrade_tier_3"->.03125;
            default->1;
        };
    }
    public static boolean module(ItemStack item){return factor(item)!=1;}
    public static int limit(ItemStack pack,ItemStack sample){return limit(pack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY),sample);}
    public static int limit(ItemContainerContents modules,ItemStack sample){
        if(sample.isEmpty())return 0;
        // Tools, charged equipment and nested packs remain individually addressable.
        if(sample.getMaxStackSize()==1)return 1;
        double multiplier=1;for(var entry:modules.itemCopies().toList())multiplier*=factor(entry);
        return (int)Math.clamp(Math.floor(sample.getMaxStackSize()*multiplier),1,Integer.MAX_VALUE);
    }
    public static boolean fits(net.minecraft.world.Container inventory,ItemContainerContents modules){
        for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).getCount()>limit(modules,inventory.getItem(i)))return false;
        return true;
    }
}

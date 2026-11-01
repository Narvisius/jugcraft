package io.github.jimbozoomer.jugcraft.wilds;

import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;

/** Original weighted grove offers; old field names resolved with official 1.20.1 mapping data. */
final class WildGroveTrades {
    private record Offer(Item buy,int count,Item sell,int output,int weight){}
    static MerchantOffers choose(RandomSource random){
        var gold=Items.GOLD_NUGGET;
        var offers=List.of(
            new Offer(gold,4,OwnerWilds.item("clawed_log"),1,9),new Offer(gold,7,OwnerWilds.item("clawed_log"),2,9),
            new Offer(gold,5,Items.COOKED_CHICKEN,2,2),new Offer(gold,4,Items.COOKED_CHICKEN,1,2),
            new Offer(gold,7,Items.COOKED_PORKCHOP,2,2),new Offer(gold,4,Items.COOKED_PORKCHOP,1,2),
            new Offer(gold,1,Items.FEATHER,4,2),new Offer(gold,1,Items.STICK,12,2),new Offer(gold,3,Items.CAMPFIRE,1,2),
            new Offer(Items.MELON_SLICE,3,gold,5,2),new Offer(Items.CHICKEN,1,gold,3,2),new Offer(Items.CHICKEN,1,gold,4,2),
            new Offer(Items.PORKCHOP,1,gold,6,2),new Offer(Items.BEETROOT,3,gold,6,2),new Offer(Items.SALMON,2,gold,8,1),
            new Offer(Items.COD,2,gold,7,1),new Offer(Items.FLINT,2,Items.FEATHER,5,2),new Offer(Items.FEATHER,5,Items.FLINT,2,2),
            new Offer(Items.ACACIA_SAPLING,2,gold,4,2),new Offer(Items.BONE,3,gold,4,1),new Offer(Items.BONE,2,gold,2,1));
        int draw=random.nextInt(offers.stream().mapToInt(Offer::weight).sum());
        for(var entry:offers){draw-=entry.weight;if(draw<0){var result=new MerchantOffers();result.add(new MerchantOffer(new ItemCost(entry.buy,entry.count),new ItemStack(entry.sell,entry.output),Integer.MAX_VALUE,0,0));return result;}}
        throw new IllegalStateException("Empty grove offer weights");
    }
}

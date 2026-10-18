package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Equipment draws the wallet first, then physical carried stars in inventory order. No nested-container access. */
public final class AlchemyPower {
    private AlchemyPower() {}
    public static long available(Player p){
        long total=Assaying.balance(p);
        for(int i=0;i<p.getInventory().getContainerSize();i++)total+=KleinStars.stored(p.getInventory().getItem(i));
        return total;
    }
    public static boolean pay(ServerPlayer p,long cost){
        if(cost<0||!AlchemicalGear.ready(p))return false;
        if(available(p)<cost){AlchemyTelemetry.low(p);return false;}
        AlchemyTelemetry.paid(p,cost);
        long wallet=Math.min(cost,Assaying.balance(p));Assaying.setBalance(p,Assaying.balance(p)-wallet);cost-=wallet;
        for(int i=0;i<p.getInventory().getContainerSize()&&cost>0;i++){
            var star=p.getInventory().getItem(i);int n=(int)Math.min(cost,KleinStars.stored(star));
            if(n>0){KleinStars.setStored(star,KleinStars.stored(star)-n);cost-=n;}
        }
        p.getInventory().setChanged();return true;
    }
    /** Used only synchronously after a failed paid operation. Refunds conserve total available essence. */
    public static void refund(ServerPlayer p,long amount){
        long wallet=Math.min(amount,Assay.MAX_BALANCE-Assaying.balance(p));Assaying.setBalance(p,Assaying.balance(p)+wallet);amount-=wallet;
        for(int i=0;i<p.getInventory().getContainerSize()&&amount>0;i++){
            var star=p.getInventory().getItem(i);int n=(int)Math.min(amount,KleinStars.capacity(star)-KleinStars.stored(star));
            if(n>0){KleinStars.setStored(star,KleinStars.stored(star)+n);amount-=n;}
        }
        p.getInventory().setChanged();
    }
}

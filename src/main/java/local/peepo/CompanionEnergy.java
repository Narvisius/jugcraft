package local.peepo;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.food.FoodProperties;

/** Internal JE reserve. The future wheel can extract inside the cable network's transaction. */
public final class CompanionEnergy extends SnapshotParticipant<CompanionEnergy.Snapshot> {
    public static final int CAPACITY = 128_000;
    public static final int MAX_OUTPUT = 64;
    public enum Rest { NONE, SITTING, SLEEPING }
    public record Meal(int bonusPerTick, int durationTicks) {}
    record Snapshot(int energy, long outputTick, int outputAmount, long lastWorkTick,int fraction) {}
    private final PeepoEntity owner;
    private long outputTick = Long.MIN_VALUE, lastWorkTick = Long.MIN_VALUE;
    private int outputAmount,discountFraction=99;
    int discountFraction(){return discountFraction;}
    void discountFraction(int value){discountFraction=Math.clamp(value,0,99);}

    CompanionEnergy(PeepoEntity owner) { this.owner = owner; }
    public static Meal meal(FoodProperties food) {
        float saturation = Float.isFinite(food.saturation()) ? Math.clamp(food.saturation(),0,32) : 0;
        int quality = Math.clamp(Math.clamp(food.nutrition(),0,16) + Math.round(saturation/2),1,16);
        return new Meal(quality*2, (30+quality*10)*20);
    }
    public int extract(int requested, TransactionContext transaction) {return extractDiscounted(requested,0,transaction);}
    /** Return accepted work/output units while charging fractional reserve fairly, with rollback. */
    int extractDiscounted(int requested,int saving,TransactionContext transaction) {
        if (owner.level().isClientSide() || !owner.isAlive() || !owner.preferences.canWork() || owner.isEating()
                || owner.getRestMode()!=Rest.NONE || requested<=0) return 0;
        long now=owner.level().getGameTime();
        int used=outputTick==now ? outputAmount : 0;
        if(owner.getEnergy()<=0)return 0;
        int rate=100-Math.clamp(saving,0,25),taken=Math.min(requested,MAX_OUTPUT-used);
        int cost=(taken*rate+(saving==0?0:discountFraction))/100;
        while(taken>0 && cost>owner.getEnergy()){taken--;cost=(taken*rate+(saving==0?0:discountFraction))/100;}
        if(taken<=0)return 0;
        updateSnapshots(transaction);
        outputTick=now;outputAmount=used+taken;lastWorkTick=now;
        if(saving!=0)discountFraction=(taken*rate+discountFraction)%100;
        owner.setStoredEnergy(owner.getEnergy()-cost);
        return taken;
    }
    boolean recentlyWorking() {
        long now=owner.level().getGameTime();
        return lastWorkTick==now || lastWorkTick==now-1;
    }
    @Override protected Snapshot createSnapshot() {
        return new Snapshot(owner.getEnergy(),outputTick,outputAmount,lastWorkTick,discountFraction);
    }
    @Override protected void readSnapshot(Snapshot snapshot) {
        owner.setStoredEnergy(snapshot.energy());outputTick=snapshot.outputTick();
        outputAmount=snapshot.outputAmount();lastWorkTick=snapshot.lastWorkTick();discountFraction=snapshot.fraction();
    }
    @Override protected void onFinalCommit() { owner.updateRecoveryState(); }
}

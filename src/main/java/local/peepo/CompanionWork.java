package local.peepo;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

public final class CompanionWork {
    private CompanionWork() {}
    /** No reservation of output without a matching committed reserve debit. */
    public static int transfer(PeepoEntity npc, CompanionStation wheel) {
        npc.setWheelRunning(false);
        if(wheel.kind()!=CompanionStation.Kind.WHEEL || !wheel.availableTo(npc)
                || npc.isRecovering() || npc.isEating() || npc.getRestMode()!=CompanionEnergy.Rest.NONE) return 0;
        int requested=Math.min(CompanionEnergy.MAX_OUTPUT,Math.min(npc.getEnergy(),wheel.energySpace()));
        if(requested<=0)return 0;
        try(var tx=Transaction.openOuter()) {
            int accepted=wheel.insertEnergy(requested,tx);
            if(accepted<=0 || accepted>requested)return 0;
            if(npc.extractEnergy(accepted,tx)!=accepted)return 0;
            tx.commit();
            npc.setWheelRunning(true);
            return accepted;
        }
    }
}

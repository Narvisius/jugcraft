package local.peepo;

import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Explicit sided access, checked again at each pickup and delivery. No adjacent inventory scans. */
public final class CompanionStorage {
    private static boolean open(Level level,BlockPos pos){
        return level.hasChunkAt(pos) && !io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(level,pos)
            && !(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity c && c.isLocked());
    }
    public static Storage<ItemVariant> find(PeepoEntity npc,CompanionAssignments.Target target){
        if(target==null || !target.present(npc.level()) || !open(npc.level(),target.at().pos()))return null;
        var pos=target.at().pos();var state=npc.level().getBlockState(pos);
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE
            && !open(npc.level(),pos.relative(ChestBlock.getConnectedDirection(state))))return null;
        if(npc.level().getBlockEntity(pos) instanceof LunchBlockEntity lunch)return lunch.feeds(npc)?lunch.source():null;
        return ItemStorage.SIDED.find(npc.level(),pos,target.face());
    }
}

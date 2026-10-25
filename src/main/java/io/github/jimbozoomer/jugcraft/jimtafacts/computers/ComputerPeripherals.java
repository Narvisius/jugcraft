package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/** Bounded native inventory/tank peripherals, resolved afresh through permitted wired hardware. */
public final class ComputerPeripherals {
    private ComputerPeripherals() {}
    public record Port(BlockPos pos, Direction side) {}
    public static Map<String, Port> ports(ComputerNode computer, ServerPlayer player) {
        var ports = new LinkedHashMap<String, Port>();
        var origin = computer.getBlockPos();
        for (Direction side : Direction.values()) add(ports, side.getName(), origin, side, player);
        for (BlockPos pos : computer.network(player)) {
            if (ports.size() >= 32) break;
            if (!(player.level().getBlockEntity(pos) instanceof ComputerNode modem) || !modem.kind().startsWith("wired_modem") || !modem.permitted(player)) continue;
            for (Direction side : Direction.values()) {
                if (ports.size() >= 32) break;
                add(ports, pos.getX()+"_"+pos.getY()+"_"+pos.getZ()+":"+side.getName(), pos, side, player);
            }
        }
        return ports;
    }
    private static void add(Map<String, Port> ports, String name, BlockPos modem, Direction side, ServerPlayer player) {
        var pos = modem.relative(side); var level = player.level();
        if (!level.isLoaded(pos) || !Authority.mayChange(level, player, pos) || !ClaimTransfers.allowed(level, modem, pos)) return;
        if (ItemStorage.SIDED.find(level, pos, side.getOpposite()) != null || FluidStorage.SIDED.find(level, pos, side.getOpposite()) != null)
            ports.put(name, new Port(pos.immutable(), side.getOpposite()));
    }
    public static Port port(ComputerNode computer, ServerPlayer player, String name) {
        var port = ports(computer, player).get(name);
        if (port == null) throw new IllegalArgumentException("Unknown, disconnected or protected port. Run ports to list names.");
        return port;
    }
    public static long count(ComputerNode computer,ServerPlayer player,String name,String itemId){
        var port=port(computer,player,name);var id=net.minecraft.resources.Identifier.tryParse(itemId);
        if(id==null||!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Unknown item identifier.");
        var storage=ItemStorage.SIDED.find(player.level(),port.pos,port.side);if(storage==null)throw new IllegalArgumentException("Port has no item storage.");
        int examined=0;long count=0;for(var view:storage){if(++examined>128)throw new IllegalArgumentException("Count needs an inventory of at most 128 slots.");
            if(!view.isResourceBlank()&&BuiltInRegistries.ITEM.getKey(view.getResource().getItem()).equals(id))count=Math.min(1_000_000_000L,count+Math.min(1_000_000_000L,Math.max(0,view.getAmount())));}
        return count;
    }
    public static void describe(ComputerNode computer, ServerPlayer player, String name, boolean fluid) {
        var port = port(computer, player, name); int examined = 0, shown = 0;
        if (fluid) {
            var storage = FluidStorage.SIDED.find(player.level(), port.pos, port.side);
            if (storage == null) throw new IllegalArgumentException("Port has no fluid interface.");
            for (var view : storage) {
                if (++examined > 128 || shown >= 16) break;
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                computer.append(BuiltInRegistries.FLUID.getKey(view.getResource().getFluid())+": "+view.getAmount()/81+" mB"); shown++;
            }
        } else {
            var storage = ItemStorage.SIDED.find(player.level(), port.pos, port.side);
            if (storage == null) throw new IllegalArgumentException("Port has no item interface.");
            for (var view : storage) {
                if (++examined > 128 || shown >= 16) break;
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                computer.append(view.getResource().toStack().getHoverName().getString()+": "+view.getAmount()); shown++;
            }
        }
        if (shown == 0) computer.append("Port is empty.");
        if (examined > 128 || shown >= 16) computer.append("Display limited to 16 entries / 128 examined slots.");
    }
    public static void move(ComputerNode computer, ServerPlayer player, String from, String to, int count, boolean fluid) {
        var ports = ports(computer, player); Port a = ports.get(from), b = ports.get(to);
        if (a == null || b == null || a.pos.equals(b.pos) || !ClaimTransfers.allowed(player.level(), a.pos, b.pos))
            throw new IllegalArgumentException("Use two different permitted ports from ports.");
        if (computer.energy.getAmount() < 10) throw new IllegalArgumentException("Peripheral transfer needs 10 JE.");
        long moved;
        try (var tx = Transaction.openOuter()) {
            if (fluid) {
                if (count < 1 || count > 1000) throw new IllegalArgumentException("Fluid transfer: 1..1000 mB.");
                moved = transfer(FluidStorage.SIDED.find(player.level(), a.pos, a.side), FluidStorage.SIDED.find(player.level(), b.pos, b.side), count * 81L, tx);
            } else {
                if (count < 1 || count > 64) throw new IllegalArgumentException("Item transfer: 1..64 items.");
                moved = transfer(ItemStorage.SIDED.find(player.level(), a.pos, a.side), ItemStorage.SIDED.find(player.level(), b.pos, b.side), count, tx);
            }
            if (moved == 0 || computer.energy.extract(10, tx) != 10) { computer.append("No transfer: empty source, full destination or insufficient power."); return; }
            tx.commit();
        }
        computer.append("Moved " + (fluid ? moved / 81 + " mB" : moved + " items"));
    }
    private static <T> long transfer(Storage<T> from, Storage<T> to, long limit, Transaction parent) {
        if (from == null || to == null || from == to) return 0;
        int examined=0;
        for (var view : from) {
            if (++examined > 128) return 0;
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            T resource = view.getResource();
            long possible;
            try (var probe = parent.openNested()) { possible = to.insert(resource, Math.min(limit, view.getAmount()), probe); }
            if (possible <= 0) continue;
            try (var move = parent.openNested()) {
                long taken = from.extract(resource, possible, move);
                if (taken > 0 && to.insert(resource, taken, move) == taken) { move.commit(); return taken; }
            }
        }
        return 0;
    }
}

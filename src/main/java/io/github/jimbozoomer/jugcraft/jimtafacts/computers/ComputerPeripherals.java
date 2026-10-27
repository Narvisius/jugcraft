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
    /** Stable slot indices are exposed only by actual slotted adapters, not arbitrary storage iteration order. */
    private static SlottedStorage<ItemVariant> slots(Port port,ServerPlayer player){
        var storage=ItemStorage.SIDED.find(player.level(),port.pos,port.side);
        if(!(storage instanceof SlottedStorage<ItemVariant> slotted)||slotted.getSlotCount()>128)
            throw new IllegalArgumentException("Peripheral requires a slotted inventory with at most 128 slots");
        return slotted;
    }
    public static String lua(ComputerNode computer,ServerPlayer player,String request){
        var args=request.split("\t",-1);
        if(args.length==1&&args[0].equals("names"))return String.join("\n",ports(computer,player).keySet());
        if(args.length<2||args.length>6)throw new IllegalArgumentException("Invalid peripheral call");
        var source=port(computer,player,args[0]);String method=args[1];
        if(method.equals("type"))return ItemStorage.SIDED.find(player.level(),source.pos,source.side) instanceof SlottedStorage<?>?"inventory":FluidStorage.SIDED.find(player.level(),source.pos,source.side)!=null?"fluid_storage":"item_storage";
        if(method.equals("tanks")){
            var fluid=FluidStorage.SIDED.find(player.level(),source.pos,source.side);if(fluid==null)throw new IllegalArgumentException("Not a fluid peripheral");
            var result=new StringBuilder();int slot=0;
            for(var view:fluid){if(++slot>128)throw new IllegalArgumentException("Tank has more than 128 views");if(view.isResourceBlank()||view.getAmount()==0)continue;
                String row=slot+"\t"+BuiltInRegistries.FLUID.getKey(view.getResource().getFluid())+"\t"+(view.getAmount()/81)+"\n";
                if(result.length()+row.length()>4096)throw new IllegalArgumentException("Tank listing exceeds reply budget");result.append(row);
            }return result.toString();
        }
        if(method.equals("pushFluid")||method.equals("pullFluid")){
            if(args.length<3||args.length>5)throw new IllegalArgumentException("Fluid transfer requires another port, optional millibuckets and fluid id");
            var other=port(computer,player,args[2]);
            if(source.pos.equals(other.pos)||!ClaimTransfers.allowed(player.level(),source.pos,other.pos))throw new IllegalArgumentException("Protected or identical transfer endpoints");
            var fromPort=method.equals("pushFluid")?source:other;var toPort=method.equals("pushFluid")?other:source;
            var from=FluidStorage.SIDED.find(player.level(),fromPort.pos,fromPort.side);var to=FluidStorage.SIDED.find(player.level(),toPort.pos,toPort.side);
            if(from==null||to==null)throw new IllegalArgumentException("Both endpoints need fluid storage");
            int amount=args.length>=4?Integer.parseInt(args[3]):1000;if(amount<1||amount>1000)throw new IllegalArgumentException("Transfer 1..1000 mB per call");
            if(computer.energy.getAmount()<10)throw new IllegalArgumentException("Fluid transfer needs 10 JE");
            int scanned=0;for(var view:from){if(++scanned>128)throw new IllegalArgumentException("Tank has more than 128 views");if(view.isResourceBlank())continue;
                var fluid=view.getResource();if(args.length==5&&!BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(args[4]))continue;
                try(var tx=Transaction.openOuter()){
                    long fit;try(var probe=tx.openNested()){fit=to.insert(fluid,Math.min(amount*81L,view.getAmount()),probe);}
                    fit=fit/81*81;if(fit==0)continue;
                    long taken=view.extract(fluid,fit,tx);if(taken!=fit||to.insert(fluid,taken,tx)!=taken||computer.energy.extract(10,tx)!=10)continue;
                    tx.commit();return Long.toString(taken/81);
                }
            }return "0";
        }
        var storage=slots(source,player);
        if(method.equals("size"))return Integer.toString(storage.getSlotCount());
        if(method.equals("list")){
            var result=new StringBuilder();
            for(int i=0;i<storage.getSlotCount();i++){
                var slot=storage.getSlot(i);if(slot.isResourceBlank()||slot.getAmount()<=0)continue;
                String row=(i+1)+"\t"+BuiltInRegistries.ITEM.getKey(slot.getResource().getItem())+"\t"+slot.getAmount()+"\n";
                if(result.length()+row.length()>4096)throw new IllegalArgumentException("Inventory listing exceeds Lua reply budget");result.append(row);
            }
            return result.toString();
        }
        if(!method.equals("pushItems")&&!method.equals("pullItems")||args.length<4)throw new IllegalArgumentException("Unknown inventory method");
        var other=port(computer,player,args[2]);
        if(source.pos.equals(other.pos)||!ClaimTransfers.allowed(player.level(),source.pos,other.pos))throw new IllegalArgumentException("Protected or identical transfer endpoints");
        var otherStorage=slots(other,player);
        var from=method.equals("pushItems")?storage:otherStorage;var to=method.equals("pushItems")?otherStorage:storage;
        int index=Integer.parseInt(args[3])-1,limit=args.length>=5?Integer.parseInt(args[4]):64;
        int destination=args.length>=6?Integer.parseInt(args[5])-1:-1;
        if(index<0||index>=from.getSlotCount()||limit<1||limit>64||destination< -1||destination>=to.getSlotCount())throw new IllegalArgumentException("Invalid slot/count; maximum 64 items per call");
        if(computer.energy.getAmount()<10)throw new IllegalArgumentException("Inventory transfer needs 10 JE");
        var input=from.getSlot(index);if(input.isResourceBlank())return "0";var item=input.getResource();
        Storage<ItemVariant> output=destination<0?to:to.getSlot(destination);
        try(var tx=Transaction.openOuter()){
            long fit;try(var probe=tx.openNested()){fit=output.insert(item,Math.min(limit,input.getAmount()),probe);}
            if(fit==0)return "0";
            long taken=input.extract(item,fit,tx);
            if(taken==0||output.insert(item,taken,tx)!=taken||computer.energy.extract(10,tx)!=10)return "0";
            tx.commit();return Long.toString(taken);
        }
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

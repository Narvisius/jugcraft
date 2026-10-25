package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Machine patterns deliver real inputs and wait for real outputs. They never manufacture a declared result. */
public final class DigitalProcessing {
    public static DataComponentType<Pattern> PATTERN;
    private DigitalProcessing() {}
    public static void register() {
        PATTERN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_processing_pattern"), DataComponentType.<Pattern>builder()
                .persistent(Pattern.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Pattern.CODEC)).build());
        PatternEncoding.register();
    }
    public record Pattern(List<ItemStack> inputs, List<ItemStack> outputs) {
        public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(i -> i.group(ItemStack.CODEC.listOf(1, 9).fieldOf("inputs").forGetter(Pattern::inputs),
                ItemStack.CODEC.listOf(1, 3).fieldOf("outputs").forGetter(Pattern::outputs)).apply(i, Pattern::new));
        public Pattern { inputs = samples(inputs); outputs = samples(outputs); }
        private static List<ItemStack> samples(List<ItemStack> items) { return items.stream().filter(s -> !s.isEmpty()).map(s -> new ItemStack(s.getItem(), Math.clamp(s.getCount(), 1, s.getMaxStackSize()))).toList(); }
        @Override public List<ItemStack> inputs() { return inputs.stream().map(ItemStack::copy).toList(); }
        @Override public List<ItemStack> outputs() { return outputs.stream().map(ItemStack::copy).toList(); }
        @Override public boolean equals(Object o) { return o instanceof Pattern p && same(inputs, p.inputs) && same(outputs, p.outputs); }
        private static boolean same(List<ItemStack> a, List<ItemStack> b) { return a.size() == b.size() && java.util.stream.IntStream.range(0, a.size()).allMatch(i -> ItemStack.matches(a.get(i), b.get(i))); }
        @Override public int hashCode() { return Objects.hash(inputs.stream().map(s -> Objects.hash(s.getItem(), s.getCount())).toList(), outputs.stream().map(s -> Objects.hash(s.getItem(), s.getCount())).toList()); }
    }
    public record Job(Pattern pattern, int remaining, boolean delivered, List<ItemStack> pending, UUID root) {
        public static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(Pattern.CODEC.fieldOf("pattern").forGetter(Job::pattern),
                Codec.intRange(1, 64).fieldOf("remaining").forGetter(Job::remaining), Codec.BOOL.fieldOf("delivered").forGetter(Job::delivered),
                ItemStack.CODEC.listOf(0, 3).optionalFieldOf("pending", List.of()).forGetter(Job::pending),
                net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("root",new UUID(0,0)).forGetter(Job::root)).apply(i, Job::new));
        public Job(Pattern pattern, int remaining, boolean delivered) { this(pattern, remaining, delivered, pattern.outputs(),UUID.randomUUID()); }
        public Job(Pattern pattern, int remaining, boolean delivered,List<ItemStack> pending) { this(pattern,remaining,delivered,pending,UUID.randomUUID()); }
        public Job { pending = (pending.isEmpty() ? pattern.outputs() : pending).stream().map(ItemStack::copy).toList();if(root.equals(new UUID(0,0)))root=UUID.randomUUID(); }
        @Override public List<ItemStack> pending() { return pending.stream().map(ItemStack::copy).toList(); }
    }
    public record Binding(DigitalNode provider, Pattern pattern) {}
    public static List<Binding> available(DigitalNetwork network) {
        var result = new ArrayList<Binding>();
        for (var node : network.nodes()) if (node.kind().equals("pattern_provider")) {
            for (int i = 0; i < node.getContainerSize() && result.size() < 256; i++) {
                var pattern = node.getItem(i).get(PATTERN); if (pattern != null && !pattern.inputs.isEmpty() && !pattern.outputs.isEmpty()) result.add(new Binding(node, pattern));
            }
        }
        return List.copyOf(result);
    }
    public static boolean request(Binding binding, int batches) {
        if (batches < 1 || batches > 64 || binding.provider.processing.size() >= 16) return false;
        var job=new Job(binding.pattern,batches,false);
        if(!DigitalJobLedger.get(binding.provider).track(binding.provider,job.root))return false;
        binding.provider.processing.add(job); binding.provider.setChanged(); return true;
    }
    /** A prerequisite may precede an undelivered parent, never interrupt a batch already in a machine. */
    public static boolean requestDependency(Binding binding, UUID root) {
        var provider=binding.provider;
        if(provider.processing.size()>=16||!provider.processing.isEmpty()&&provider.processing.getFirst().delivered)return false;
        if(provider.processing.stream().anyMatch(j->j.pattern.equals(binding.pattern)))return false;
        if(!DigitalJobLedger.get(provider).track(provider,root))return false;
        provider.processing.addFirst(new Job(binding.pattern,1,false,binding.pattern.outputs(),root));provider.setChanged();return true;
    }
    public static void cancelTree(DigitalNetwork network,UUID root) {
        if(!network.nodes().isEmpty())DigitalJobLedger.get(network.nodes().getFirst()).cancel(root);
        for(var node:network.nodes()){
            cancelNode(node,root);
            node.reservations.release(root,network);
            DigitalJobLedger.get(node).finished(node,root);
        }
    }
    static void cancelNode(DigitalNode node,UUID root){
        boolean changed=node.orders.removeIf(order->order.root().equals(root));
        for(int i=node.processing.size()-1;i>=0;i--){var job=node.processing.get(i);if(!job.root.equals(root))continue;
            // Keep tracking the already-paid batch; never refund and keep physical ingredients as well.
            if(job.delivered)node.processing.set(i,new Job(job.pattern,1,true,job.pending,UUID.randomUUID()));
            else node.processing.remove(i);changed=true;
        }
        if(changed){node.processingStatus="Cancelled tree; paid batches drain to storage";node.setChanged();}
    }
    public static String status(DigitalNode node) {
        if (node.processing.isEmpty()) return "No machine batches queued.";
        var job = node.processing.getFirst(); return node.processing.size() + " jobs · " + job.remaining + " batches left · " + node.processingStatus;
    }
    public static void cancel(DigitalNode node) {
        var actor=node.actor();if(actor==null||!node.permitted(actor))return;
        var network=DigitalNetwork.find(node,actor);
        for(var root:node.processing.stream().map(Job::root).distinct().toList())cancelTree(network,root);
    }
    private static net.minecraft.core.BlockPos identity(ServerPlayer player, net.minecraft.core.BlockPos pos) {
        var state = player.level().getBlockState(pos);
        return state.getBlock() instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlock m ? m.masterPos(pos, state) : pos;
    }
    /** Called only after an import bus actually commits a transfer out of the job's machine. */
    public static void received(DigitalNetwork network, net.minecraft.core.BlockPos source, ItemStack item, long amount) {
        var player = network.actor(); if (player == null || !player.level().isLoaded(source)) return;
        var sourceId = identity(player, source); long left = amount;
        for (var node : network.nodes()) {
            if (left <= 0) break;
            if (!node.kind().equals("pattern_provider") || node.processing.isEmpty() || !node.permitted(player)) continue;
            var target = node.getBlockPos().relative(node.getBlockState().getValue(DigitalNodeBlock.FACING));
            if (!player.level().isLoaded(target) || !identity(player, target).equals(sourceId)) continue;
            var job = node.processing.getFirst(); if (!job.delivered) continue;
            var pending = new ArrayList<ItemStack>(); boolean changed = false;
            for (var need : job.pending()) {
                if (left > 0 && ItemStack.isSameItemSameComponents(need, item)) { int used = (int) Math.min(left, need.getCount()); left -= used; need.shrink(used); changed |= used > 0; }
                if (!need.isEmpty()) pending.add(need);
            }
            if (!changed) continue;
            if (pending.isEmpty()) complete(node, job);
            else { node.processing.set(0, new Job(job.pattern, job.remaining, true, pending,job.root)); node.setChanged(); }
        }
    }
    private static void complete(DigitalNode node, Job job) {
        if (job.remaining == 1) node.processing.removeFirst(); else node.processing.set(0, new Job(job.pattern, job.remaining - 1, false,job.pattern.outputs(),job.root));
        node.processingStatus = "Batch returned to storage"; node.setChanged();
        DigitalJobLedger.get(node).finished(node,job.root);
    }
    public static void tick(DigitalNode node, DigitalNetwork network, ServerPlayer player) {
        node.reservations.releaseFinished(network);
        if (!node.kind().equals("pattern_provider") || node.processing.isEmpty()) return;
        var face = node.getBlockState().getValue(DigitalNodeBlock.FACING); var target = node.getBlockPos().relative(face); var level = player.level();
        if (!level.isLoaded(target) || level.getBlockEntity(target) instanceof DigitalNode || level.getBlockEntity(target) instanceof DigitalFacades.Covers || !Authority.mayChange(level, player, target)
                || !ClaimTransfers.allowed(level, node.getBlockPos(), target)) { node.processingStatus = "Machine unavailable or protected"; return; }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, target, face.getOpposite());
        if (storage == null) { node.processingStatus = "Point provider toward a machine inventory"; return; }
        var job = node.processing.getFirst();
        DigitalReservations.reserve(node,job.root,job.delivered?List.of():job.pattern.inputs(),network);
        network=network.forJob(job.root);
        if (!job.delivered) {
            if (available(network).stream().noneMatch(b -> b.provider == node && b.pattern.equals(job.pattern))) { node.processingStatus = "Pattern removed"; return; }
            // Drain the previous batch before starting another; never count old outputs as newly completed work.
            int checked = 0;
            for (var view : storage) {
                if (++checked > 128) { node.processingStatus = "Machine inventory is too large"; return; }
                if (view.getAmount() > 0 && job.pattern.outputs.stream().anyMatch(s -> ItemVariant.of(s).equals(view.getResource()))) {
                    node.processingStatus = "Remove old results before starting this batch"; return;
                }
            }
            boolean supplied = true;
            try (var tx = Transaction.openOuter()) {
                for (var stack : job.pattern.inputs) if (network.extract(stack, stack.getCount(), tx) != stack.getCount()
                        || storage.insert(ItemVariant.of(stack), stack.getCount(), tx) != stack.getCount()) {
                    supplied = false; break;
                }
                if (supplied) tx.commit();
            }
            if (!supplied) { node.processingStatus = DigitalDependencies.advance(job.pattern, network, level,job.root); return; }
            node.processing.set(0, new Job(job.pattern, job.remaining, true,job.pattern.outputs(),job.root)); node.processingStatus = "Machine working; waiting for real outputs"; node.setChanged();
        } else {
            try (var tx = Transaction.openOuter()) {
                for (var output : job.pending) if (storage.extract(ItemVariant.of(output), output.getCount(), tx) != output.getCount()
                        || network.insert(output, output.getCount(), tx) != output.getCount()) {
                    node.processingStatus = "Waiting for results, storage room or JE"; return;
                }
                tx.commit();
            }
            complete(node, job);
        }
    }
}

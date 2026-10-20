package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
/** Sampled detectors, finite sensor pulses and directed wireless redstone without feedback latching. */
public final class NouveauSignals {
    private static final Set<NouveauWorkshop> SENSORS=Collections.newSetFromMap(new WeakHashMap<>());
    private NouveauSignals(){}
    public static void register(){
        AttackBlockCallback.EVENT.register((p,l,hand,at,face)->{if(p instanceof ServerPlayer s&&l.getBlockEntity(at) instanceof NouveauWorkshop b&&b.kind().equals("item_detector")&&b.mayUse(s)){b.threshold(b.threshold()-(p.isShiftKeyDown()?16:1));p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Threshold: "+b.threshold()));return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->SENSORS.clear());
    }
    public static void tick(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        if(b.kind().equals("spell_sensor")){SENSORS.add(b);if(l.getGameTime()>=b.signalUntil())b.signal(0,0);return;}
        if(b.kind().equals("spell_prism")){if(l.getGameTime()>=b.signalUntil())b.signal(0,0);return;}
        if(b.kind().equals("item_detector")){
            long count=0;for(var at:b.links()){var inv=NouveauStorage.accessible(l,p,at);if(inv==null)continue;for(int i=0;i<Math.min(inv.getContainerSize(),1728);i++)if(NouveauFilters.allows(b.filter(),inv.getItem(i)))count+=inv.getItem(i).getCount();}
            boolean over=count>=b.threshold();b.signal(over!=b.inverted()?15:0,Long.MAX_VALUE);b.status(count+" / "+b.threshold()+" items");return;
        }
        if(b.kind().equals("redstone_relay")){
            if(l.getGameTime()>=b.signalUntil())b.signal(0,0);
            Direction input=b.getBlockState().getValue(NouveauWorkshopBlock.FACING).getOpposite();var at=b.getBlockPos().relative(input);
            int signal=l.hasChunkAt(at)&&!(l.getBlockEntity(at) instanceof NouveauWorkshop other&&other.kind().equals("redstone_relay"))?l.getSignal(at,input):0;
            if(signal>0)relay(l,p,b,signal,new HashSet<>());
        }
    }
    private static void relay(ServerLevel l,ServerPlayer p,NouveauWorkshop b,int power,Set<BlockPos> visited){
        if(visited.size()>=32||!visited.add(b.getBlockPos())||!b.permitted(p))return;b.signal(Math.max(power,b.signal()),l.getGameTime()+25);
        for(var at:b.links())if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof NouveauWorkshop other&&other.kind().equals("redstone_relay"))relay(l,p,other,power,visited);
    }
    public static void spell(ServerLevel l,ServerPlayer caster,Vec3 origin,String text,int words,boolean resolved){
        for(var b:List.copyOf(SENSORS))if(!b.isRemoved()&&b.getLevel()==l&&l.hasChunkAt(b.getBlockPos())&&b.getBlockPos().distToCenterSqr(origin)<=64&&b.inverted()==resolved){
            var owner=Authority.answering(l,b.owner());if(owner==null||!b.permitted(owner))continue;
            var match=b.filter().get(JugcraftConcordance.INSCRIPTION);if(match!=null&&!match.text().equals(text))continue;b.signal(Math.clamp(words,1,15),l.getGameTime()+20);
        }
    }
}

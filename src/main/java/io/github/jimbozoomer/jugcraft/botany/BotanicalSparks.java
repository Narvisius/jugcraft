package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;

/** Loaded-chunk mana networks. An installed spark and augment remain physical, recoverable items. */
public final class BotanicalSparks {
    private static final Map<Level,Map<Long,Set<BotanicalWorkshop>>> LIVE=new WeakHashMap<>();
    private final BotanicalWorkshop host;private boolean installed;private int channel;private String augment="";
    private long lastSearch=Long.MIN_VALUE;private final List<BotanicalWorkshop> nearby=new ArrayList<>();private int cursor;
    BotanicalSparks(BotanicalWorkshop host){this.host=host;}
    public boolean installed(){return installed;}
    public int channel(){return channel;}
    public String augment(){return augment;}
    static boolean supports(String kind){return kind.endsWith("pool")||kind.equals("terra_plate")||kind.equals("enchanter");}
    static void track(BotanicalWorkshop host){if(host.getLevel()!=null&&supports(host.kind()))synchronized(LIVE){LIVE.computeIfAbsent(host.getLevel(),l->new HashMap<>()).computeIfAbsent(ChunkPos.containing(host.getBlockPos()).pack(),l->Collections.newSetFromMap(new WeakHashMap<>())).add(host);}}
    static void forget(BotanicalWorkshop host){synchronized(LIVE){var world=LIVE.get(host.getLevel());if(world!=null){long key=ChunkPos.containing(host.getBlockPos()).pack();var set=world.get(key);if(set!=null){set.remove(host);if(set.isEmpty())world.remove(key);}if(world.isEmpty())LIVE.remove(host.getLevel());}}}
    private void search(ServerLevel level){
        long now=level.getGameTime();if(lastSearch!=Long.MIN_VALUE&&now>=lastSearch&&now-lastSearch<20)return;lastSearch=now;nearby.clear();var chunk=ChunkPos.containing(host.getBlockPos());
        synchronized(LIVE){var world=LIVE.getOrDefault(level,Map.of());for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(var other:world.getOrDefault(ChunkPos.pack(chunk.x()+x,chunk.z()+z),Set.of())){
            if(other!=host&&!other.isRemoved()&&other.getLevel()==level&&Math.abs(other.getBlockPos().getX()-host.getBlockPos().getX())<=12&&Math.abs(other.getBlockPos().getY()-host.getBlockPos().getY())<=12&&Math.abs(other.getBlockPos().getZ()-host.getBlockPos().getZ())<=12){nearby.add(other);if(nearby.size()>=128)return;}
        }}nearby.sort(Comparator.comparingLong(b->b.getBlockPos().asLong()));
    }
    InteractionResult use(ServerPlayer actor,InteractionHand hand){
        var stack=actor.getItemInHand(hand);String name=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if(stack.is(Jugtania.item("spark"))){if(installed)return InteractionResult.FAIL;installed=true;if(!actor.getAbilities().instabuild)stack.shrink(1);host.changed();return InteractionResult.SUCCESS;}
        if(!installed)return InteractionResult.PASS;
        if(stack.getItem() instanceof DyeItem dye){channel=dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getId();if(!actor.getAbilities().instabuild)stack.shrink(1);host.changed();actor.sendOverlayMessage(Component.literal("Spark channel: "+dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getName()));return InteractionResult.SUCCESS;}
        if(name.startsWith("jugtania_spark_upgrade_")&&Set.of("dispersive","dominant","recessive","isolated").contains(name.substring(23))){
            if(!host.kind().endsWith("pool"))return InteractionResult.FAIL;String next=name.substring(23);if(augment.equals(next))return InteractionResult.FAIL;
            if(!augment.isEmpty())actor.getInventory().placeItemBackInInventory(new ItemStack(Jugtania.item("spark_upgrade_"+augment)),net.minecraft.util.Prediction.SERVER_ONLY);augment=next;if(!actor.getAbilities().instabuild)stack.shrink(1);host.changed();return InteractionResult.SUCCESS;
        }
        if(stack.isEmpty()&&actor.isShiftKeyDown()){
            if(!augment.isEmpty()){actor.getInventory().placeItemBackInInventory(new ItemStack(Jugtania.item("spark_upgrade_"+augment)),net.minecraft.util.Prediction.SERVER_ONLY);augment="";}
            else{actor.getInventory().placeItemBackInInventory(new ItemStack(Jugtania.item("spark")),net.minecraft.util.Prediction.SERVER_ONLY);installed=false;}host.changed();return InteractionResult.SUCCESS;
        }return InteractionResult.PASS;
    }
    ItemStack swap(ItemStack incoming){
        if(!installed||!host.kind().endsWith("pool")||!incoming.isEmpty()&&!BotanicalDevices.augment(incoming))return null;
        String next=incoming.isEmpty()?"":net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(incoming.getItem()).getPath().substring("jugtania_spark_upgrade_".length());
        if(next.equals(augment))return null;var previous=augment.isEmpty()?ItemStack.EMPTY:new ItemStack(Jugtania.item("spark_upgrade_"+augment));augment=next;host.changed();return previous;
    }
    private boolean active(ServerLevel level,BotanicalWorkshop other,ServerPlayer actor){return !other.isRemoved()&&level.hasChunkAt(other.getBlockPos())&&level.getBlockEntity(other.getBlockPos())==other&&other.sparks.installed&&other.sparks.channel==channel&&other.permitted(actor)&&ClaimTransfers.allowed(level,host.getBlockPos(),other.getBlockPos());}
    private boolean routesTo(BotanicalWorkshop receiver){
        if(receiver.kind().equals("terra_plate")||receiver.kind().equals("enchanter"))return receiver.sparkDemand()>0;
        return augment.equals("recessive")&&(receiver.sparks.augment.isEmpty()||receiver.sparks.augment.equals("dispersive"))||augment.isEmpty()&&receiver.sparks.augment.equals("dominant");
    }
    void tick(ServerLevel level,ServerPlayer actor){
        if(!installed||!host.kind().endsWith("pool")||host.mana()<=0)return;search(level);
        if(augment.equals("dispersive"))disperse(level,actor);
        if(nearby.isEmpty())return;int start=Math.floorMod(cursor++,nearby.size());
        // Rotate recipients so small supplies do not permanently starve later endpoints.
        for(int i=0;i<nearby.size();i++){
            var target=nearby.get((start+i)%nearby.size());if(!active(level,target,actor)||!routesTo(target))continue;
            int amount=Math.min(1000,Math.min(host.mana(),Math.min(target.freeMana(),target.sparkDemand())));if(amount<=0)continue;
            if(host.draw(amount)){int accepted=target.fill(amount);if(accepted<amount)host.fill(amount-accepted);if(accepted>0&&level.getGameTime()%10==0){var to=net.minecraft.world.phys.Vec3.atCenterOf(target.getBlockPos());level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,to.x,to.y+.8,to.z,2,.1,.1,.1,.02);}}
            if(host.mana()<=0)break;
        }
    }
    private void disperse(ServerLevel level,ServerPlayer actor){
        var players=level.players();if(players.isEmpty())return;int offset=Math.floorMod((int)level.getGameTime(),players.size());
        for(int n=0;n<players.size();n++){
            var player=players.get((offset+n)%players.size());if(player.isSpectator()||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(host.getBlockPos()))>144||!Authority.mayChange(level,actor,player.blockPosition())||!ClaimTransfers.allowed(level,host.getBlockPos(),player.blockPosition()))continue;
            int budget=Math.min(1000,host.mana());if(budget>0){int accepted=BotanicalManaItems.fill(player,budget);if(accepted>0){host.draw(accepted);return;}}

        }
    }
    void drop(ServerLevel level){if(!installed)return;Block.popResource(level,host.getBlockPos(),new ItemStack(Jugtania.item("spark")));if(!augment.isEmpty())Block.popResource(level,host.getBlockPos(),new ItemStack(Jugtania.item("spark_upgrade_"+augment)));installed=false;augment="";}
    String status(){return installed?" · Spark: "+DyeColor.byId(channel).getName()+" / "+(augment.isEmpty()?"standard":augment):"";}
    void save(ValueOutput out){out.putBoolean("spark_installed",installed);out.putInt("spark_channel",channel);out.putString("spark_augment",augment);}
    void load(ValueInput in){installed=in.getBooleanOr("spark_installed",false);channel=Math.clamp(in.getIntOr("spark_channel",0),0,15);augment=in.getStringOr("spark_augment","");if(!Set.of("","dispersive","dominant","recessive","isolated").contains(augment))augment="";lastSearch=Long.MIN_VALUE;nearby.clear();}
}

package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A held, finite-use target memory. Recall resolves through the ordinary effect authority and ledger. */
public final class ArsReliquary extends Item {
    public static ArsReliquary ITEM;
    public static DataComponentType<Mark> MARK;
    public record Mark(GlobalPos location,Optional<UUID> entity){
        static final Codec<Mark> CODEC=RecordCodecBuilder.create(i->i.group(GlobalPos.CODEC.fieldOf("location").forGetter(Mark::location),UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(Mark::entity)).apply(i,Mark::new));
    }
    private ArsReliquary(Properties p){super(p);}
    public static void register(){
        MARK=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_reliquary_mark"),DataComponentType.<Mark>builder().persistent(Mark.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(Mark.CODEC)).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_unstable_reliquary"));ITEM=Registry.register(BuiltInRegistries.ITEM,key,new ArsReliquary(new Properties().setId(key).durability(1000)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(ITEM));
    }
    private static ItemStack held(ServerPlayer p){return p.getOffhandItem().is(ITEM)?p.getOffhandItem():p.getMainHandItem().is(ITEM)?p.getMainHandItem():ItemStack.EMPTY;}
    private record Relic(ItemStack stack,Runnable changed){}
    private static Relic relic(ServerLevel level,ServerPlayer p,Cause cause){
        if(!cause.source().startsWith("jugcraft:nouveau_device/"))return new Relic(held(p),()->p.getInventory().setChanged());
        var device=NouveauDevices.source(level,p,cause);if(device==null)return new Relic(ItemStack.EMPTY,()->{});
        var inventories=new ArrayList<net.minecraft.world.Container>();inventories.add(device);inventories.addAll(NouveauDevices.inventories(level,p,device));
        for(var inventory:inventories)for(int slot=0;slot<Math.min(54,inventory.getContainerSize());slot++)if(inventory.getItem(slot).is(ITEM))return new Relic(inventory.getItem(slot),inventory::setChanged);
        return new Relic(ItemStack.EMPTY,()->{});
    }
    public static boolean mark(ConcordanceEffects.Context context,ServerPlayer p,BlockPos pos,LivingEntity target){
        var access=relic(context.level(),p,context.cause());var stack=access.stack();if(stack.isEmpty()||!JugNouveau.enabled()||!p.level().hasChunkAt(pos)||!Authority.mayChange(p.level(),p,pos))return false;
        if(target!=null&&target!=p&&!io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(p.getUUID(),target.getUUID())&&(!Authority.mayStrike(p,target)||!ConcordanceEffects.mayHarm(p,target)))return false;
        if(target instanceof Player&&!NouveauStatuses.give(target,"marked",6000,0,p))return false;
        stack.set(MARK,new Mark(GlobalPos.of(p.level().dimension(),pos),Optional.ofNullable(target==null?null:target.getUUID())));
        access.changed().run();
        p.sendOverlayMessage(Component.literal(target==null?"Location marked in the Reliquary.":"Marked "+target.getName().getString()+" in the Reliquary."));return true;
    }
    /** null means another delivery; a failed special delivery never falls back to ordinary aim. */
    public static ComposedSpells.Outcome deliver(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger){
        String mode=plan.root().delivery();if(!mode.equals("jugcraft:nouveau_recall")&&!mode.equals("jugcraft:nouveau_retaliate"))return null;
        var failed=new ComposedSpells.Outcome(false,false);if(!JugNouveau.enabled()||plan.root().steps().stream().anyMatch(s->s.component().equals("jugcraft:nouveau_mark")))return failed;
        var access=relic(l,p,cause);ItemStack relic=ItemStack.EMPTY;ComposedSpells.Impact impact;int damage=0;
        if(mode.endsWith("retaliate")){
            var target=p.getLastHurtByMob();if(target==null||target==p||!target.isAlive()||target.level()!=l||!l.hasChunkAt(target.blockPosition()))return failed;
            impact=NouveauEquipment.impact(p,target);
        }else{
            relic=access.stack();var mark=relic.get(MARK);if(mark==null||!mark.location().dimension().equals(l.dimension()))return failed;
            if(mark.entity().isPresent()){
                var entity=l.getEntity(mark.entity().get());
                if(!(entity instanceof LivingEntity target)||!target.isAlive()||target instanceof Player&&!NouveauStatuses.has(target,"marked")){relic.remove(MARK);access.changed().run();return failed;}
                if(!l.hasChunkAt(target.blockPosition()))return failed;impact=NouveauEquipment.impact(p,target);damage=target instanceof Player?1000:250;
            }else{
                var at=mark.location().pos();if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at))return failed;
                impact=new ComposedSpells.Impact(Vec3.atCenterOf(at),null,at.above(),at,p.getEyePosition());damage=50;
            }
        }
        var result=ComposedSpells.performAt(l,p,plan,cause,ledger,impact);
        if(result.applied()&&!relic.isEmpty()){relic.hurtAndBreak(damage,l,p,i->{});access.changed().run();}
        return result;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p){var stack=p.getItemInHand(hand);var mark=stack.get(MARK);
            if(p.isShiftKeyDown()){stack.remove(MARK);p.sendOverlayMessage(Component.literal("Reliquary mark cleared."));}
            else p.sendOverlayMessage(Component.literal(mark==null?"Hold this while casting Mark, then cast Recall + an effect.":"Marked "+mark.location().dimension().identifier()+" "+mark.location().pos().toShortString()+". Sneak-use to clear."));
        }return InteractionResult.SUCCESS;
    }
}

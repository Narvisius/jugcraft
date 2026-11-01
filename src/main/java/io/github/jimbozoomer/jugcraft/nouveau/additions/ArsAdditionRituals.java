package io.github.jimbozoomer.jugcraft.nouveau.additions;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.*;

/** Paid, expiring local chunk leases and source-authored structure-search augments. */
public final class ArsAdditionRituals {
    public static Item WAYFINDER,EXPLORATION_SCROLL;
    public static net.minecraft.core.component.DataComponentType<String> EXPLORATION_TARGET;
    private static final TicketType TICKET=new TicketType(40,TicketType.FLAG_LOADING|TicketType.FLAG_SIMULATION|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private record Lease(UUID owner,long until){}
    private static final Map<MinecraftServer,Map<GlobalPos,Lease>> LEASES=new WeakHashMap<>();
    private ArsAdditionRituals(){}
    public static void register(){
        EXPLORATION_TARGET=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_exploration_target"),net.minecraft.core.component.DataComponentType.<String>builder().persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8).build());
        Registry.register(BuiltInRegistries.TICKET_TYPE,Jugcraft.id("ars_permanence"),TICKET);
        ServerLifecycleEvents.SERVER_STOPPED.register(LEASES::remove);
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var leases=LEASES.get(server);if(leases==null)return;long now=server.getTickCount();
            leases.entrySet().removeIf(entry->{var l=server.getLevel(entry.getKey().dimension());var p=server.getPlayerList().getPlayer(entry.getValue().owner());
                return entry.getValue().until()<=now||l==null||p==null||!p.isAlive()||p.isSpectator()||p.level()!=l||!JugNouveau.enabled()||!l.hasChunkAt(entry.getKey().pos())||!(l.getBlockEntity(entry.getKey().pos()) instanceof NouveauWorkshop b)||b.paused()||!b.ritualRunning()||!b.ritual().equals("chunk_loading")||!b.permitted(p);
            });
            // Tickets expire after 40 ticks even when a station is destroyed or its owner disconnects.
        });
        WAYFINDER=item("wayfinder",false);EXPLORATION_SCROLL=item("exploration_warp_scroll",true);
    }
    private static Item item(String name,boolean scroll){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_"+name));
        var item=Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(1)){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!JugNouveau.enabled()||p.isSpectator()||!p.isAlive())return InteractionResult.FAIL;
                var stack=p.getItemInHand(hand);var tracker=stack.get(DataComponents.LODESTONE_TRACKER);
                if(scroll&&tracker==null&&stack.has(EXPLORATION_TARGET)){
                    if(!RateGate.allow(p,"ars_structure_search",1200))return InteractionResult.FAIL;
                    String id=stack.get(EXPLORATION_TARGET);if(!id.startsWith("#"))return InteractionResult.FAIL;
                    var tag=TagKey.create(Registries.STRUCTURE,net.minecraft.resources.Identifier.parse(id.substring(1)));
                    if(p.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).get(tag).isEmpty())return InteractionResult.FAIL;
                    var found=p.level().findNearestMapStructure(tag,p.blockPosition(),32,true);
                    if(found==null){p.sendOverlayMessage(Component.literal("No unexplored destination found nearby. Try from another region."));return InteractionResult.FAIL;}
                    tracker=new LodestoneTracker(Optional.of(GlobalPos.of(p.level().dimension(),found)),false);stack.set(DataComponents.LODESTONE_TRACKER,tracker);
                    p.sendOverlayMessage(Component.literal("Expedition located. Use again to travel."));return InteractionResult.SUCCESS;
                }
                if(scroll&&tracker==null&&p.getOffhandItem().is(WAYFINDER)){tracker=p.getOffhandItem().get(DataComponents.LODESTONE_TRACKER);if(tracker!=null)stack.set(DataComponents.LODESTONE_TRACKER,tracker);}
                if(tracker==null||tracker.target().isEmpty()){p.sendOverlayMessage(Component.literal(scroll?"Hold a located Wayfinder in your off hand to choose the expedition.":"Use the locating ritual with a structure's augment to bind a Wayfinder."));return InteractionResult.FAIL;}
                var destination=tracker.target().get();
                if(!scroll){p.sendOverlayMessage(Component.literal(destination.dimension().identifier()+" · "+destination.pos().toShortString()+(destination.dimension().equals(p.level().dimension())?" · "+(int)Math.sqrt(destination.pos().distSqr(p.blockPosition()))+" blocks away":" · another dimension")));return InteractionResult.SUCCESS;}
                if(!RateGate.allow(p,"ars_exploration_travel",40))return InteractionResult.FAIL;
                var target=p.level().getServer().getLevel(destination.dimension());if(target==null||!target.getWorldBorder().isWithinBounds(destination.pos()))return InteractionResult.FAIL;
                target.getChunkAt(destination.pos());
                var landing=target.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,destination.pos());
                if(!target.getFluidState(landing).isEmpty()||target.getBlockState(landing.below()).is(net.minecraft.tags.BlockTags.FIRE))return InteractionResult.FAIL;
                var arrival=GlobalPos.of(target.dimension(),landing);stack.set(NouveauUtilities.DESTINATION,arrival);
                if(p.isShiftKeyDown()){p.sendOverlayMessage(Component.literal("Portal destination prepared. Throw this into a sourcestone frame beside a full Source Jar."));return InteractionResult.SUCCESS;}
                if(NouveauUtilities.teleport(p,arrival)){stack.consume(1,p);p.fallDistance=0;return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
            }
        });CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(item));return item;
    }
    public static boolean tick(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        if(b.ritual().equals("chunk_loading")){permanence(l,p,b);return true;}
        if(!b.ritual().equals("locate_structure"))return false;
        var recipe=NouveauRecipes.all().stream().filter(r->r.operation().equals("ars_locate_structure")&&matches(r,b)).findFirst().orElse(null);
        if(recipe==null){finishFailure(b,"No locating recipe matches these augments");return true;}
        if(!RateGate.allow(p,"ars_structure_search",1200)){b.status("Waiting for the structure-search cooldown");b.setActive(false);return true;}
        var tag=TagKey.create(Registries.STRUCTURE,Jugcraft.id("ars_additions/locate/"+recipe.data().get("locator").getAsString()));
        if(l.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(tag).isEmpty()){finishFailure(b,"That structure is not available in this world");return true;}
        // One bounded native search per player/minute; never touch chunk internals on an asynchronous thread.
        var at=l.findNearestMapStructure(tag,b.getBlockPos(),32,false);
        if(at==null){finishFailure(b,"No matching structure found within the search radius");return true;}
        var result=new ItemStack(WAYFINDER);result.set(DataComponents.LODESTONE_TRACKER,new LodestoneTracker(Optional.of(GlobalPos.of(l.dimension(),at)),false));
        result.set(DataComponents.CUSTOM_NAME,Component.literal("Wayfinder · "+recipe.label()));
        if(!b.canOutput(result)){b.status("Clear ritual output space before locating");b.setActive(false);return true;}
        b.output(result);b.ritual("",0);b.setActive(false);b.status("Located "+at.toShortString());return true;
    }
    private static boolean matches(NouveauRecipes.Working recipe,NouveauWorkshop b){
        if(!recipe.data().has("augments"))return false;boolean[] used=new boolean[8];
        for(var ingredient:recipe.data().getAsJsonArray("augments")){int slot=-1;for(int i=0;i<8;i++)if(!used[i]&&NouveauRecipes.ingredient(ingredient,b.ritualAugment(i))){slot=i;break;}if(slot<0)return false;used[slot]=true;}return true;
    }
    private static void finishFailure(NouveauWorkshop b,String message){
        var refund=new ArrayList<ItemStack>();refund.add(new ItemStack(NouveauCatalog.item("ritual_locate_structure")));for(int i=0;i<8;i++)if(!b.ritualAugment(i).isEmpty())refund.add(b.ritualAugment(i).copy());
        // Refund only after every stack can be placed together, so retries cannot multiply rewards.
        var scratch=new SimpleContainer(9);for(int i=0;i<9;i++)scratch.setItem(i,b.getItem(NouveauWorkshop.OUTPUT+i).copy());
        for(var stack:refund)if(!scratch.addItem(stack).isEmpty()){b.status(message+"; clear outputs to receive the refund");b.setActive(false);return;}
        for(int i=0;i<9;i++)b.setItem(NouveauWorkshop.OUTPUT+i,scratch.getItem(i));b.ritual("",0);b.setActive(false);b.status(message+"; inputs returned");
    }
    private static void permanence(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        var server=l.getServer();var leases=LEASES.computeIfAbsent(server,s->new HashMap<>());long now=server.getTickCount();
        leases.values().removeIf(lease->lease.until()<=now);var key=GlobalPos.of(l.dimension(),b.getBlockPos());
        if(!leases.containsKey(key)&&(leases.size()>=16||leases.values().stream().anyMatch(lease->lease.owner().equals(p.getUUID())))){b.status("Permanence limit: one per owner, sixteen per server");b.setActive(false);return;}
        if(l.hasNeighborSignal(b.getBlockPos())){leases.remove(key);b.status("Permanence paused by redstone");b.setActive(false);return;}
        int elapsed=b.ritualCursor();
        if(elapsed%24000==0&&!NouveauSource.pay(l,p,b,100)){leases.remove(key);b.status("Permanence needs 100 Ley per 24,000 active ticks");b.setActive(false);return;}
        l.getChunkSource().addTicketWithRadius(TICKET,ChunkPos.containing(b.getBlockPos()),2);leases.put(key,new Lease(p.getUUID(),now+40));
        b.ritual("chunk_loading",(elapsed+20)%24000);b.setActive(true);b.status("Keeping this chunk active while its owner is online");
    }
}

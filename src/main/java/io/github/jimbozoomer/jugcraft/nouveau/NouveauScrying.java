package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;

/** A remote viewport only: the player's body, damage, chunk tickets and game mode stay at home. */
public final class NouveauScrying {
    public static MenuType<Menu> MENU;
    private record Session(GlobalPos crystal,Vec3 body,long until){}
    private static final Map<UUID,Session> SESSIONS=new java.util.concurrent.ConcurrentHashMap<>();
    private NouveauScrying(){}
    public static void register(){
        NouveauScrySight.register();
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_scrying"),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.clientboundPlay().register(View.TYPE,View.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Exit.TYPE,Exit.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Exit.TYPE,(packet,context)->close(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            var s=SESSIONS.get(p.getUUID());if(s==null)continue;
            if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||p.isPassenger()||p.level().getGameTime()>s.until||p.position().distanceToSqr(s.body)>1||!valid(p,s.crystal)){close(p);continue;}
            if(p.tickCount%10==0)p.level().getChunkSource().move(p);
            if(p.tickCount%40==0)p.sendOverlayMessage(Component.literal("Scrying · Sneak to return · Your body remains behind"));
        }});
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,damage,blocked)->{if(entity instanceof ServerPlayer p&&damage>0)close(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->SESSIONS.clear());
    }
    public static boolean active(ServerPlayer p){return SESSIONS.containsKey(p.getUUID());}
    public static Vec3 viewPosition(ServerPlayer p){var s=SESSIONS.get(p.getUUID());return s==null?p.position():Vec3.atCenterOf(s.crystal.pos()).add(0,.35,0);}
    public static ChunkPos viewChunk(ServerPlayer p){return active(p)?new ChunkPos(net.minecraft.core.SectionPos.blockToSectionCoord(viewPosition(p).x),net.minecraft.core.SectionPos.blockToSectionCoord(viewPosition(p).z)):p.chunkPosition();}
    public static boolean valid(ServerPlayer p,GlobalPos pos){return p.level().dimension().equals(pos.dimension())&&p.level().hasChunkAt(pos.pos())&&p.level().getBlockEntity(pos.pos()) instanceof NouveauWorkshop crystal&&crystal.kind().equals("scryers_crystal")&&crystal.permitted(p);}
    public static boolean open(ServerPlayer p,GlobalPos crystal){
        if(!JugNouveau.enabled()||p.isSpectator()||p.isPassenger()||!p.isAlive()||!valid(p,crystal)||!RateGate.allow(p,"nouveau_scry",10))return false;
        p.closeContainer();SESSIONS.put(p.getUUID(),new Session(crystal,p.position(),p.level().getGameTime()+2400));
        p.level().getChunkSource().move(p);ServerPlayNetworking.send(p,new View(true,crystal.pos()));return true;
    }
    public static void close(ServerPlayer p){if(SESSIONS.remove(p.getUUID())==null)return;p.level().getChunkSource().move(p);ServerPlayNetworking.send(p,new View(false,p.blockPosition()));}
    public static void oculus(ServerPlayer p,NouveauWorkshop oculus){
        List<ItemStack> scrolls=new ArrayList<>();
        for(BlockPos at:BlockPos.betweenClosed(oculus.getBlockPos().offset(-5,-2,-5),oculus.getBlockPos().offset(5,2,5))){
            if(!p.level().hasChunkAt(at)||!(p.level().getBlockEntity(at) instanceof NouveauWorkshop stand)||!stand.pedestal()||!stand.permitted(p))continue;
            var stack=stand.getItem(0);if(stack.is(NouveauCatalog.item("scryer_scroll"))&&stack.has(NouveauUtilities.DESTINATION))scrolls.add(stack.copy());if(scrolls.size()==16)break;
        }
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new Menu(id,inv,oculus,List.copyOf(scrolls)),Component.literal("Scryer's Oculus")));
    }
    public static boolean castEye(ServerPlayer p,ItemStack eye){
        GlobalPos bound=eye.get(NouveauUtilities.DESTINATION);if(p.getOffhandItem().is(NouveauCatalog.item("scryer_scroll")))bound=p.getOffhandItem().get(NouveauUtilities.DESTINATION);
        if(bound==null||!valid(p,bound)){p.sendOverlayMessage(Component.literal("Bind the eye to a loaded Scry Crystal, or hold its parchment."));return false;}
        var direction=p.level().getBlockState(bound.pos()).getValue(NouveauWorkshopBlock.FACING);var look=new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());
        Vec3 start=Vec3.atCenterOf(bound.pos()).add(look.scale(.7));
        Vec3 end=start.add(look.scale(16));
        var hit=p.level().clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        BlockPos at=hit.getBlockPos();return NouveauEquipment.castAt(p,eye,new ComposedSpells.Impact(hit.getLocation(),null,at.relative(hit.getDirection()),at,start),1);
    }
    public record View(boolean active,BlockPos position) implements CustomPacketPayload {
        public static final Type<View> TYPE=new Type<>(Jugcraft.id("nouveau_scry_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=new StreamCodec<>(){
            public View decode(RegistryFriendlyByteBuf b){return new View(b.readBoolean(),b.readBlockPos());}
            public void encode(RegistryFriendlyByteBuf b,View p){b.writeBoolean(p.active);b.writeBlockPos(p.position);}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Exit() implements CustomPacketPayload {
        public static final Type<Exit> TYPE=new Type<>(Jugcraft.id("nouveau_scry_exit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Exit> CODEC=StreamCodec.unit(new Exit());
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Inventory inv;private final NouveauWorkshop oculus;private final List<ItemStack> sources;
        private final net.minecraft.world.SimpleContainer samples=new net.minecraft.world.SimpleContainer(16);
        public Menu(int id,Inventory inv){this(id,inv,null,List.of());}
        public Menu(int id,Inventory inv,NouveauWorkshop oculus,List<ItemStack> sources){super(MENU,id);this.inv=inv;this.oculus=oculus;this.sources=sources;
            for(int i=0;i<16;i++){if(i<sources.size())samples.setItem(i,sources.get(i));addSlot(new Slot(samples,i,-1000,-1000){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});}}
        public ItemStack source(int n){return samples.getItem(n);}
        public boolean stillValid(Player p){return p==inv.player&&p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer)||oculus!=null&&oculus.stillValid(p));}
        public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
        @Override public void clicked(int slot,int button,ContainerInput input,Player p){}
        public boolean clickMenuButton(Player p,int index){if(!(p instanceof ServerPlayer s)||!stillValid(p)||index<0||index>=sources.size())return false;var dest=sources.get(index).get(NouveauUtilities.DESTINATION);return dest!=null&&open(s,dest);}
    }
}

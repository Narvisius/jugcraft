package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.Vec3;

/** Private, persistent destination scrolls shared by every nexus, never copied into a public station. */
public final class ArsWarpNexus {
    public static Scroll SCROLL;
    private ArsWarpNexus(){}
    public static void register(){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_nexus_warp_scroll"));
        SCROLL=Registry.register(BuiltInRegistries.ITEM,key,new Scroll(new Item.Properties().setId(key).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(SCROLL));
    }
    public static final class Scroll extends Item {
        Scroll(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var nexus=ArsWarpNexusBlock.at(p.level(),c.getClickedPos());if(!accessible(p,nexus))return InteractionResult.FAIL;
            c.getItemInHand().set(NouveauUtilities.DESTINATION,GlobalPos.of(p.level().dimension(),nexus.getBlockPos()));
            p.sendOverlayMessage(Component.literal("Nexus recorded. Sneak-use a nexus to store this scroll; use normally to travel."));return InteractionResult.SUCCESS;
        }
    }
    private static final class Journal extends SavedData {
        private final Map<UUID,SimpleContainer> inventories=new HashMap<>();
        private static final Codec<Journal> CODEC=Codec.unboundedMap(UUIDUtil.STRING_CODEC,ItemStack.OPTIONAL_CODEC.listOf(0,27)).xmap(Journal::new,Journal::snapshot);
        private static final SavedDataType<Journal> TYPE=new SavedDataType<>(Jugcraft.id("ars_nexus_scrolls"),()->new Journal(Map.of()),CODEC,null);
        Journal(Map<UUID,List<ItemStack>> entries){entries.forEach((id,list)->{var c=create();for(int i=0;i<list.size();i++)c.setItem(i,list.get(i));inventories.put(id,c);});}
        private SimpleContainer create(){return new SimpleContainer(27){@Override public void setChanged(){super.setChanged();Journal.this.setDirty();}};}
        SimpleContainer inventory(UUID player){return inventories.computeIfAbsent(player,id->{setDirty();return create();});}
        private Map<UUID,List<ItemStack>> snapshot(){var result=new HashMap<UUID,List<ItemStack>>();inventories.forEach((id,c)->{var list=new ArrayList<ItemStack>();for(int i=0;i<27;i++)list.add(c.getItem(i).copy());result.put(id,list);});return result;}
    }
    private static SimpleContainer scrolls(ServerPlayer p){return p.level().getServer().overworld().getDataStorage().computeIfAbsent(Journal.TYPE).inventory(p.getUUID());}
    public static boolean accessible(ServerPlayer p,NouveauWorkshop nexus){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||nexus==null||nexus.isRemoved()||nexus.getLevel()!=p.level()||!p.level().hasChunkAt(nexus.getBlockPos())||p.distanceToSqr(Vec3.atCenterOf(nexus.getBlockPos()))>64)return false;
        if(!Authority.mayChange(p.level(),p,nexus.getBlockPos()))return false;
        // Generated free nexuses stay public; constructed powered nexuses use ordinary ownership.
        return !nexus.getBlockState().getValue(ArsWarpNexusBlock.REQUIRES_SOURCE)||nexus.mayUse(p);
    }
    public static InteractionResult open(ServerPlayer p,NouveauWorkshop nexus){
        if(!accessible(p,nexus)||!RateGate.allow(p,"ars_nexus_open",4))return InteractionResult.FAIL;
        // Supplied tower processors may place one destination scroll in the nexus.
        // Only that physical stack is awarded; private journals are never copied here.
        if(nexus.getItem(0).is(SCROLL)){
            var gift=nexus.getItem(0).copyWithCount(1);gift.set(NouveauUtilities.DESTINATION,GlobalPos.of(p.level().dimension(),nexus.getBlockPos()));
            if(!p.getInventory().add(gift)){p.sendOverlayMessage(Component.literal("Make an inventory slot for the Nexus scroll."));return InteractionResult.FAIL;}
            nexus.removeItem(0,1);nexus.setChanged();return InteractionResult.SUCCESS;
        }
        boolean edit=p.isShiftKeyDown();var inventory=scrolls(p);
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new NexusMenu(id,inv,nexus,inventory,edit),Component.literal(edit?"Nexus · Store destination scrolls":"Nexus · Select a destination")));
        return InteractionResult.SUCCESS;
    }
    private static boolean scroll(ItemStack s){return s.is(SCROLL)&&s.has(NouveauUtilities.DESTINATION);}
    private static boolean travel(ServerPlayer p,NouveauWorkshop origin,ItemStack scroll){
        if(!accessible(p,origin)||!scroll(scroll)||!RateGate.allow(p,"ars_nexus_travel",20))return false;
        var destination=scroll.get(NouveauUtilities.DESTINATION);var target=p.level().getServer().getLevel(destination.dimension());if(target==null||!target.getWorldBorder().isWithinBounds(destination.pos()))return false;
        // This deliberate travel may load its one destination chunk, never persistent chunk tickets.
        target.getChunkAt(destination.pos());var other=ArsWarpNexusBlock.at(target,destination.pos());
        if(other==null||!Authority.mayChange(target,p,destination.pos()))return false;
        if(other.getBlockState().getValue(ArsWarpNexusBlock.REQUIRES_SOURCE)&&other.owner()!=null&&!other.belongs(p.getUUID()))return false;
        BlockPos arrival=null;
        for(int radius=2;radius<=3&&arrival==null;radius++)for(Direction side:Direction.Plane.HORIZONTAL){
            var at=other.getBlockPos().relative(side,radius);var point=Vec3.atBottomCenterOf(at);
            if(target.hasChunkAt(at)&&target.getWorldBorder().isWithinBounds(at)&&Authority.mayChange(target,p,at)&&target.getBlockState(at.below()).isSolidRender()&&target.getFluidState(at).isEmpty()&&target.noCollision(p,p.getBoundingBox().move(point.subtract(p.position())))){arrival=at;break;}
        }
        if(arrival==null){p.sendOverlayMessage(Component.literal("Destination has no safe landing space."));return false;}
        var source=p.level();boolean paid=origin.getBlockState().getValue(ArsWarpNexusBlock.REQUIRES_SOURCE);
        if(paid&&!NouveauSource.pay(source,p,origin,10)){p.sendOverlayMessage(Component.literal("Nexus needs 10 Ley (1,000 Source). Connect an authorized Source Jar."));return false;}
        if(!NouveauUtilities.teleport(p,GlobalPos.of(target.dimension(),arrival))){if(paid)origin.fill(10);return false;}
        p.fallDistance=0;p.closeContainer();return true;
    }
    private static final class NexusMenu extends ChestMenu {
        private final NouveauWorkshop nexus;private final SimpleContainer stored;private final boolean edit;
        NexusMenu(int id,Inventory inv,NouveauWorkshop nexus,SimpleContainer stored,boolean edit){
            super(MenuType.GENERIC_9x3,id,inv,stored,3);this.nexus=nexus;this.stored=stored;this.edit=edit;
            for(int i=0;i<27;i++){var old=slots.get(i);var slot=new Slot(stored,i,old.x,old.y){@Override public boolean mayPlace(ItemStack stack){return scroll(stack);}@Override public int getMaxStackSize(){return 1;}};slot.index=i;slots.set(i,slot);}
        }
        @Override public boolean stillValid(Player p){return p instanceof ServerPlayer s&&accessible(s,nexus);}
        @Override public void clicked(int slot,int button,ContainerInput type,Player player){
            if(!(player instanceof ServerPlayer p)||!stillValid(p))return;
            if(edit)super.clicked(slot,button,type,player);
            else if(type==ContainerInput.PICKUP&&slot>=0&&slot<27)travel(p,nexus,stored.getItem(slot));
        }
        @Override public ItemStack quickMoveStack(Player p,int index){return edit&&stillValid(p)?super.quickMoveStack(p,index):ItemStack.EMPTY;}
    }
}

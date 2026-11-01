package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Server-selected single/multiple links; reverse order never reverses permission checks. */
public final class ArsAdvancedWand extends NouveauItem {
    public static ArsAdvancedWand ITEM;
    private static DataComponentType<Integer> MODE;
    private static final String[] MODES={"One target · Forward","One target · Reverse","Connected targets · Forward","Connected targets · Reverse"};
    private ArsAdvancedWand(Properties p){super(p,Kind.WAND);}
    public static void register(){
        MODE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_dominion_mode"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,3)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_advanced_dominion_wand"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new ArsAdvancedWand(new Properties().setId(key).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(ITEM));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||p.isSpectator()||!p.isAlive()||!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
        var stack=c.getItemInHand();var selected=stack.get(JugNouveau.WAND_LECTERN);var at=c.getClickedPos();
        if(stack.has(JugNouveau.WAND_WORKER))return super.useOn(c);
        if(p.isShiftKeyDown()||selected==null){
            stack.set(JugNouveau.WAND_LECTERN,GlobalPos.of(p.level().dimension(),at));p.sendOverlayMessage(Component.literal("Anchor selected. Use on targets to connect; use in air for direction and batch mode."));return InteractionResult.SUCCESS;
        }
        if(!selected.dimension().equals(p.level().dimension())||!p.level().hasChunkAt(selected.pos())||!Authority.mayChange(p.level(),p,selected.pos())||!RateGate.allow(p,"ars_multi_link",5))return InteractionResult.FAIL;
        int mode=stack.getOrDefault(MODE,0);var targets=new ArrayList<BlockPos>();targets.add(at);
        if(mode>=2){
            Set<BlockPos> seen=new HashSet<>();seen.add(at);var block=p.level().getBlockState(at).getBlock();
            for(int cursor=0;cursor<targets.size()&&targets.size()<32;cursor++)for(var d:Direction.values()){
                var next=targets.get(cursor).relative(d);if(targets.size()>=32)break;
                if(next.distSqr(at)>16*16||!seen.add(next)||!p.level().hasChunkAt(next)||!p.level().getBlockState(next).is(block)||!Authority.mayChange(p.level(),p,next))continue;
                targets.add(next.immutable());
            }
        }
        int linked=0;for(var target:targets){
            var from=mode%2==0?selected.pos():target;var to=mode%2==0?target:selected.pos();
            if(from.equals(to))continue;var be=p.level().getBlockEntity(from);boolean ok=false;
            if(be instanceof NouveauWorkshop workshop&&workshop.mayUse(p))ok=workshop.links().contains(to)||workshop.link(p,to);
            else if(be instanceof StorageLectern lectern&&lectern.mayUse(p))ok=lectern.links().contains(to)||lectern.link(p,to);
            if(ok){linked++;p.level().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,false,to.getX()+.5,to.getY()+1,to.getZ()+.5,3,.1,.1,.1,0);}
        }
        p.sendOverlayMessage(Component.literal("Connected "+linked+" / "+targets.size()+" targets. Existing range, capacity and access limits apply."));return linked>0?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p&&JugNouveau.enabled()&&!p.isSpectator())p.openMenu(new SimpleMenuProvider((id,inv,who)->new Controls(id,inv,hand),Component.literal("Advanced Dominion Wand")));
        return InteractionResult.SUCCESS;
    }
    private static final class Controls extends ChestMenu {
        final ItemStack wand;final InteractionHand hand;final Player owner;
        Controls(int id,Inventory inv,InteractionHand hand){super(MenuType.GENERIC_9x1,id,inv,new SimpleContainer(9),1);owner=inv.player;this.hand=hand;wand=owner.getItemInHand(hand);refresh();}
        private void refresh(){for(int i=0;i<4;i++){var icon=new ItemStack(i==wand.getOrDefault(MODE,0)?Items.ENDER_EYE:Items.ENDER_PEARL);icon.set(DataComponents.CUSTOM_NAME,Component.literal(MODES[i]));getContainer().setItem(i,icon);}var clear=new ItemStack(Items.BARRIER);clear.set(DataComponents.CUSTOM_NAME,Component.literal("Clear anchor / selected helper"));getContainer().setItem(8,clear);broadcastChanges();}
        @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&!p.isSpectator()&&p.getItemInHand(hand)==wand&&wand.is(ITEM)&&JugNouveau.enabled();}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(!(p instanceof ServerPlayer)||!stillValid(p)||type!=ContainerInput.PICKUP)return;if(slot>=0&&slot<4)wand.set(MODE,slot);else if(slot==8){wand.remove(JugNouveau.WAND_LECTERN);wand.remove(JugNouveau.WAND_WORKER);wand.remove(io.github.jimbozoomer.jugcraft.concordance.spirits.Workers.PORTER_ROUTE);}refresh();}
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    }
}

package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** In-place MFE upgrade; detach the old entity before replacement so inventories cannot spill twice. */
public final class IndustrialStorageUpgrade extends Item {
    public static Item ITEM;
    private IndustrialStorageUpgrade(Properties p){super(p);}
    public static void register(){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_mfsu_upgrade_kit"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new IndustrialStorageUpgrade(new Properties().setId(key)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->o.accept(ITEM));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var level=p.level();var at=c.getClickedPos();var state=level.getBlockState(at);
        if(!p.isAlive()||p.isSpectator()||!Authority.mayChange(level,p,at))return InteractionResult.FAIL;
        Block target=state.is(JimtaIndustry.MACHINES.get("jimta_mfe"))?JimtaIndustry.MACHINES.get("jimta_mfsu"):
                state.is(IndustrialInfrastructure.BLOCKS.get("chargepad_mfe"))?IndustrialInfrastructure.BLOCKS.get("chargepad_mfsu"):null;
        if(target==null)return InteractionResult.PASS;
        BlockEntity old=level.getBlockEntity(at);
        if(!(old instanceof MachineBlockEntity)&&!(old instanceof IndustrialInfrastructure.Node))return InteractionResult.FAIL;
        if(old instanceof IndustrialInfrastructure.Node pad&&pad.owner!=null&&!pad.owner.equals(p.getUUID())
                &&!io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(pad.owner,p.getUUID()))return InteractionResult.FAIL;
        var snapshot=old.saveWithoutMetadata(level.registryAccess());var next=target.withPropertiesOf(state);
        // Decode into a fresh entity BEFORE touching the world. Both supported families preserve their slot layout.
        BlockEntity replacement=((EntityBlock)target).newBlockEntity(at,next);
        if(replacement==null)return InteractionResult.FAIL;
        replacement.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),snapshot));
        if(old instanceof Container from&&replacement instanceof Container to){
            if(from.getContainerSize()!=to.getContainerSize())return InteractionResult.FAIL;
            for(int slot=0;slot<from.getContainerSize();slot++)if(!ItemStack.matches(from.getItem(slot),to.getItem(slot)))return InteractionResult.FAIL;
        }
        // Invalidate old menus and cached capability handles; no intermediate tick or user action can occur here.
        if(old instanceof Container inventory)inventory.clearContent();
        if(old instanceof MachineBlockEntity machine&&machine.energyFor(null) instanceof io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage energy)energy.setAmount(0);
        if(old instanceof IndustrialInfrastructure.Node pad)pad.energy.setAmount(0);
        level.removeBlockEntity(at);
        try{
            if(!level.setBlock(at,next,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))throw new IllegalStateException("Storage replacement refused");
            level.setBlockEntity(replacement);replacement.setChanged();
        }catch(RuntimeException failure){
            level.removeBlockEntity(at);level.setBlock(at,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            old.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),snapshot));
            old.clearRemoved();level.setBlockEntity(old);old.setChanged();EnergyNetworks.invalidate(level);
            org.slf4j.LoggerFactory.getLogger("JugcraftIndustry").error("MFE upgrade reverted",failure);
            p.sendOverlayMessage(Component.literal("Upgrade could not be applied; original storage restored."));return InteractionResult.FAIL;
        }
        c.getItemInHand().consume(1,p);EnergyNetworks.invalidate(level);level.updateNeighborsAt(at,target);
        level.sendBlockUpdated(at,state,next,Block.UPDATE_CLIENTS);
        p.sendOverlayMessage(Component.literal("MFSU installed; charge, settings and contents retained."));return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
        out.accept(Component.literal("Use on an MFE or MFE Charge Pad to upgrade it in place."));
        out.accept(Component.literal("Keeps charge, facing, stored items and configuration."));
    }
}

package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalFacades;
import io.github.jimbozoomer.jugcraft.tools.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Supplied obscurator: paid surface sampling/reskinning on existing native facade faces. */
public final class IndustrialObscurator extends Item implements Chargeable {
    public static IndustrialObscurator ITEM;
    public static final long SCAN_COST=20_000,APPLY_COST=5_000;
    private IndustrialObscurator(Properties p){super(p.stacksTo(1));}
    public static void register(){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_obscurator"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new IndustrialObscurator(new Item.Properties().setId(key)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->o.accept(ITEM));
    }
    @Override public long baseCapacity(){return 100_000;}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!p.isAlive()||p.isSpectator()||!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
        var stack=c.getItemInHand();var state=p.level().getBlockState(c.getClickedPos());
        if(p.isShiftKeyDown()){
            if(!DigitalFacades.valid(state)){p.sendOverlayMessage(Component.literal("Sample a solid block without an inventory or fluid."));return InteractionResult.FAIL;}
            if(state.equals(stack.get(DigitalFacades.MATERIAL)))return InteractionResult.SUCCESS;
            if(!Chargeable.drain(stack,SCAN_COST)){p.sendOverlayMessage(Component.literal("Sampling needs 20,000 JE."));return InteractionResult.FAIL;}
            stack.set(DigitalFacades.MATERIAL,state);p.sendOverlayMessage(Component.literal("Sampled ").append(state.getBlock().getName()));return InteractionResult.SUCCESS;
        }
        var sample=stack.get(DigitalFacades.MATERIAL);
        if(sample==null||!DigitalFacades.valid(sample)||Chargeable.energy(stack)<APPLY_COST){p.sendOverlayMessage(Component.literal("Sneak-use to sample, then apply to an existing facade with 5,000 JE."));return InteractionResult.FAIL;}
        var node=p.level().getBlockEntity(c.getClickedPos());boolean changed=false;
        if(node instanceof DigitalFacades.Covers covers)changed=covers.retexture(c.getClickedFace(),sample);
        else if(node instanceof TransportAttachments.Host host&&TransportAttachments.at(p.level(),c.getClickedPos())!=null){
            var pipe=host.attachments();
            int face=c.getClickedFace().ordinal();var old=pipe.faces.get(face);
            if(!old.isAir()&&!old.equals(sample)){pipe.faces.set(face,sample);pipe.changed();changed=true;}
        }
        if(!changed)return InteractionResult.PASS;
        Chargeable.drain(stack,APPLY_COST);return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
        out.accept(PoweredToolItem.energyLine(s));var sample=s.get(DigitalFacades.MATERIAL);
        if(sample!=null)out.accept(Component.literal("Sample: ").append(sample.getBlock().getName()));
        out.accept(Component.literal("Sneak-use solid block: sample (20,000 JE). Use existing facade: reskin (5,000 JE)."));
        out.accept(Component.literal("Copies appearance only, never items, inventories or block behavior."));
    }
}

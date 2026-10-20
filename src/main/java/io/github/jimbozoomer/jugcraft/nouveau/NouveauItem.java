package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.spirits.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import java.util.function.Consumer;

public final class NouveauItem extends Item {
    public enum Kind { CHARM, WAND, STARBUNCLE }
    private final Kind kind;
    public NouveauItem(Properties properties,Kind kind){super(properties);this.kind=kind;}
    @Override public InteractionResult useOn(UseOnContext ctx){
        if(!(ctx.getLevel() instanceof ServerLevel level)||!(ctx.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||!player.isAlive()||player.isSpectator()||!RateGate.allow(player,"jug_nouveau",4))return InteractionResult.FAIL;
        var pos=ctx.getClickedPos();var stack=ctx.getItemInHand();
        if(kind==Kind.WAND&&level.dimension().equals(NouveauPlanarium.DIMENSION)){
            if(level.getBlockState(pos).is(NouveauCatalog.block("dim_boundary"))){stack.set(NouveauUtilities.MIMIC,"planarium_arrival");player.sendOverlayMessage(Component.literal("Use the wand on the new arrival floor."));return InteractionResult.SUCCESS;}
            if("planarium_arrival".equals(stack.get(NouveauUtilities.MIMIC))){if(!NouveauPlanarium.setArrival(player,pos.relative(ctx.getClickedFace())))return InteractionResult.FAIL;stack.remove(NouveauUtilities.MIMIC);return InteractionResult.SUCCESS;}
        }
        if(kind==Kind.STARBUNCLE){
            var roster=WorkerRoster.of(level.getServer());var all=roster.workers(player.getUUID());
            if(all.size()>=WorkerRoster.MAX_PER_PLAYER||all.values().stream().filter(e->e.kind().equals("starbuncle")).count()>=Starbuncle.MAX_PER_OWNER)
                return starMessage(player,"limit",false);
            var at=pos.relative(ctx.getClickedFace());
            if(!level.isLoaded(at)||!Authority.mayChange(level,player,at))return starMessage(player,"refused",false);
            var star=JugNouveau.STARBUNCLE.create(level,EntitySpawnReason.SPAWN_ITEM_USE);
            if(star==null)return InteractionResult.FAIL;
            star.setOwner(player.getUUID());star.color(stack.getOrDefault(JugNouveau.CHARM_COLOR,4));
            star.home(pos);
            star.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,player.getYRot()+180,0);
            if(!level.noCollision(star)||!level.addFreshEntity(star))return starMessage(player,"refused",false);
            roster.note(player.getUUID(),star.getUUID(),star.kind(),star.status(),level.dimension().identifier().toString(),at);
            stack.consume(1,player);return starMessage(player,"summoned",true);
        }
        if(level.getBlockEntity(pos) instanceof StorageLectern lectern){
            if(!lectern.mayUse(player))return message(player,"refused",false);
            if(kind==Kind.WAND){var old=stack.get(JugNouveau.WAND_LECTERN);if(!player.isShiftKeyDown()&&old!=null&&old.dimension().equals(level.dimension())&&!old.pos().equals(pos)&&level.hasChunkAt(old.pos())&&level.getBlockEntity(old.pos()) instanceof StorageLectern selectedLectern){if(!selectedLectern.proxyTo(player,lectern))return message(player,"refused",false);player.sendOverlayMessage(Component.literal("Remote lectern connected. Sneak-use a lectern with the wand to select it."));return InteractionResult.SUCCESS;}stack.remove(JugNouveau.WAND_WORKER);stack.remove(Workers.PORTER_ROUTE);stack.set(JugNouveau.WAND_LECTERN,GlobalPos.of(level.dimension(),pos));return message(player,"selected",true);}
            if(lectern.bind(player)){stack.consume(1,player);return message(player,"bound",true);}
            return message(player,"full",false);
        }
        if(kind!=Kind.WAND)return InteractionResult.PASS;
        var selectedWorker=stack.get(JugNouveau.WAND_WORKER);if(selectedWorker!=null&&level.getEntity(selectedWorker) instanceof NouveauBeast beast&&beast.mayCommand(player)){beast.guard(player.getUUID(),pos);stack.remove(JugNouveau.WAND_WORKER);return InteractionResult.SUCCESS;}
        var selected=stack.get(JugNouveau.WAND_LECTERN);
        if(selected!=null&&selected.dimension().equals(level.dimension())&&level.hasChunkAt(selected.pos())&&level.getBlockEntity(selected.pos()) instanceof NouveauWorkshop workshop&&!player.isShiftKeyDown()){
            return workshop.link(player,pos)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
        if(level.getBlockEntity(pos) instanceof NouveauWorkshop workshop&&!stack.has(JugNouveau.WAND_WORKER)){
            if(!workshop.mayUse(player))return InteractionResult.FAIL;
            if(player.isShiftKeyDown()&&java.util.Set.of("spell_sensor","item_detector").contains(workshop.kind())){workshop.invert();return InteractionResult.SUCCESS;}
            if(player.isShiftKeyDown()&&workshop.kind().startsWith("relay")){workshop.clearLinks();return InteractionResult.SUCCESS;}
            if(workshop.kind().equals("potion_jar")&&player.isShiftKeyDown())return NouveauAlchemy.interact(player,workshop,ctx.getHand());
            stack.remove(JugNouveau.WAND_WORKER);stack.remove(Workers.PORTER_ROUTE);stack.set(JugNouveau.WAND_LECTERN,GlobalPos.of(level.dimension(),pos));
            player.sendOverlayMessage(Component.literal("Workshop selected. Use the wand on sources, inventories or devices to link them; sneak-use another workshop to select it."));return InteractionResult.SUCCESS;
        }
        var worker=stack.get(JugNouveau.WAND_WORKER);
        if(worker!=null){
            if(level.getEntity(worker) instanceof NouveauHelper helper){
                if(!player.getUUID().equals(helper.owner())||helper.distanceToSqr(player)>1024||!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
                helper.bind(pos);return InteractionResult.SUCCESS;
            }
            if(!(level.getEntity(worker) instanceof Starbuncle star)||!star.mayServe(player)||star.distanceToSqr(player)>Starbuncle.RANGE*Starbuncle.RANGE)return starMessage(player,"refused",false);
            if(level.getBlockState(pos).getBlock() instanceof NouveauDecor.PetBed&&Authority.mayChange(level,player,pos)){star.home(pos);return InteractionResult.SUCCESS;}
            if(!star.endpoint(level,player,pos))return starMessage(player,"refused",false);
            var route=stack.get(Workers.PORTER_ROUTE);String dimension=level.dimension().identifier().toString();
            if(route==null||player.isShiftKeyDown()||!route.dimension().equals(dimension)||!route.source().equals(route.target())){
                stack.set(Workers.PORTER_ROUTE,new ClockworkPorterEntity.Route(pos.immutable(),pos.immutable(),dimension));
                star.destination(player,pos);
                return starMessage(player,"source",true);
            }
            var next=new ClockworkPorterEntity.Route(route.source(),pos.immutable(),dimension);
            if(!star.assign(player,next))return starMessage(player,"route_refused",false);
            stack.set(Workers.PORTER_ROUTE,next);return starMessage(player,"assigned",true);
        }
        var target=stack.get(JugNouveau.WAND_LECTERN);
        if(target==null)return message(player,"select_first",false);
        if(!target.dimension().equals(level.dimension())||!level.isLoaded(target.pos())
            ||!(level.getBlockEntity(target.pos()) instanceof StorageLectern lectern)||!lectern.mayUse(player))return message(player,"refused",false);
        if(player.isShiftKeyDown())return message(player,lectern.unlink(pos)?"unlinked":"refused",true);
        if(!lectern.link(player,pos))return message(player,"refused",false);
        player.sendOverlayMessage(Component.translatable("message.jugcraft.bookwyrm.linked",lectern.links().size(),lectern.capacity()));
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(kind==Kind.WAND&&target instanceof NouveauBeast beast&&player instanceof ServerPlayer server&&beast.mayCommand(server)){stack.remove(JugNouveau.WAND_LECTERN);stack.set(JugNouveau.WAND_WORKER,beast.getUUID());return InteractionResult.SUCCESS;}
        if(kind==Kind.WAND&&target instanceof NouveauHelper helper&&player instanceof ServerPlayer server){
            if(!player.getUUID().equals(helper.owner())||player.distanceToSqr(helper)>64||!Authority.mayChange(server.level(),player,helper.blockPosition()))return InteractionResult.FAIL;
            stack.remove(JugNouveau.WAND_LECTERN);stack.set(JugNouveau.WAND_WORKER,helper.getUUID());server.sendOverlayMessage(Component.literal("Helper selected. Use the wand on its home or inventory."));return InteractionResult.SUCCESS;
        }
        if(kind!=Kind.WAND||!(target instanceof Starbuncle star))return InteractionResult.PASS;
        if(!(player instanceof ServerPlayer server))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||!star.mayServe(player)||player.distanceToSqr(star)>64
            ||!Authority.mayChange(server.level(),player,star.blockPosition())||!RateGate.allow(server,"jug_nouveau",4))return InteractionResult.FAIL;
        stack.remove(JugNouveau.WAND_LECTERN);stack.remove(Workers.PORTER_ROUTE);stack.set(JugNouveau.WAND_WORKER,star.getUUID());
        return starMessage(server,"selected",true);
    }
    private static InteractionResult starMessage(ServerPlayer player,String id,boolean success){player.sendOverlayMessage(Component.translatable("message.jugcraft.starbuncle."+id));return success?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    private static InteractionResult message(ServerPlayer player,String id,boolean success){player.sendOverlayMessage(Component.translatable("message.jugcraft.bookwyrm."+id));return success?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> tooltip,TooltipFlag flag){
        tooltip.accept(Component.translatable("tooltip.jugcraft."+switch(kind){case WAND->"dominion_wand";case CHARM->"bookwyrm_charm";case STARBUNCLE->"starbuncle_charm";}).withStyle(ChatFormatting.GRAY));
        if(kind==Kind.WAND&&stack.has(JugNouveau.WAND_WORKER)){
            var route=stack.get(Workers.PORTER_ROUTE);
            tooltip.accept(Component.translatable("tooltip.jugcraft.dominion_wand.starbuncle").withStyle(ChatFormatting.GOLD));
            if(route!=null)tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.porter_key.route",route.source().toShortString(),route.source().equals(route.target())?"?":route.target().toShortString()));
        }
    }
}

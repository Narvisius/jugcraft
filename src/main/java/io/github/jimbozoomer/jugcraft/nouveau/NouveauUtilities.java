package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

public final class NouveauUtilities {
    public static DataComponentType<GlobalPos> DESTINATION;
    public static DataComponentType<List<String>> FILTER;
    public static DataComponentType<String> MIMIC;
    private NouveauUtilities(){}
    public static void register(){
        NouveauFollowingLight.register();NouveauVoiding.register();
        DESTINATION=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_destination"),DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(GlobalPos.CODEC)).build());
        FILTER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_filter"),DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf(0,16)).networkSynchronized(ByteBufCodecs.fromCodec(Codec.STRING.listOf(0,16))).build());
        MIMIC=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_mimic"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        for(var e:NouveauCatalog.ITEM_ENTRIES.values())if(e.kind().equals("scroll"))NouveauCatalog.registerItem(e,p->new Scroll(e.source(),p.stacksTo(1)));
    }
    public static InteractionResult use(ServerPlayer p,ItemStack stack,String name,InteractionHand hand){
        if(!RateGate.allow(p,"nouveau_utility",10))return InteractionResult.FAIL;
        if(name.equals("jar_of_light")){NouveauFollowingLight.toggle(p);return InteractionResult.SUCCESS;}
        if(name.endsWith("warp_scroll")){
            if(p.isShiftKeyDown()){stack.set(DESTINATION,GlobalPos.of(p.level().dimension(),p.blockPosition()));p.sendOverlayMessage(Component.literal("Destination recorded"));return InteractionResult.SUCCESS;}
            GlobalPos destination=stack.get(DESTINATION);if(destination==null||!name.equals("stable_warp_scroll")&&!destination.dimension().equals(p.level().dimension()))return InteractionResult.FAIL;
            if(teleport(p,destination)){if(!name.equals("stable_warp_scroll"))stack.consume(1,p);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
        }
        if(name.equals("allow_scroll")||name.equals("deny_scroll")){
            var sample=p.getOffhandItem();if(hand!=InteractionHand.MAIN_HAND||sample.isEmpty())return InteractionResult.FAIL;
            var names=new ArrayList<>(stack.getOrDefault(FILTER,List.of()));String id=BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();
            if(!names.remove(id)&&names.size()<16)names.add(id);stack.set(FILTER,List.copyOf(names));p.sendOverlayMessage(Component.literal("Filter: "+names.size()+" item types"));return InteractionResult.SUCCESS;
        }
        if(name.equals("void_jar"))return NouveauVoiding.use(p,stack);
        if(name.equals("dowsing_rod")){
            var held=p.getOffhandItem();Block target=held.getItem() instanceof BlockItem block?block.getBlock():Blocks.BUDDING_AMETHYST;NouveauStatuses.give(p,"magic_find",200,0,p);NouveauScrySight.begin(p,target,200);
            stack.hurtAndBreak(1,p,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            int seen=0;for(BlockPos at:BlockPos.betweenClosed(p.blockPosition().offset(-8,-4,-8),p.blockPosition().offset(8,4,8))){
                if(!p.level().hasChunkAt(at)||!p.level().getBlockState(at).is(target)||!Authority.mayChange(p.level(),p,at))continue;
                p.level().sendParticles(p,ParticleTypes.END_ROD,true,false,at.getX()+.5,at.getY()+.5,at.getZ()+.5,2,0,0,0,0);if(++seen>=16)break;
            }return InteractionResult.SUCCESS;
        }
        if(name.equals("starby_gift")){
            var stars=p.level().getEntitiesOfClass(Starbuncle.class,p.getBoundingBox().inflate(8),s->s.mayServe(p));
            if(stars.isEmpty())return InteractionResult.FAIL;var star=stars.getFirst();star.heal(8);stack.consume(1,p);return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public static boolean teleport(ServerPlayer p,GlobalPos destination){
        ServerLevel target=p.level().getServer().getLevel(destination.dimension());if(target==null||!target.getWorldBorder().isWithinBounds(destination.pos())||destination.pos().getY()<target.getMinY()||destination.pos().getY()>=target.getMaxY())return false;
        target.getChunkAt(destination.pos());
        var at=destination.pos();if(!target.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(target,p,at)||!target.getBlockState(at.below()).isSolidRender())return false;
        Vec3 point=Vec3.atBottomCenterOf(at);if(!target.noCollision(p,p.getBoundingBox().move(point.subtract(p.position()))))return false;
        p.teleportTo(target,point.x,point.y,point.z,Set.of(),p.getYRot(),p.getXRot(),true);return true;
    }
    public static boolean teleportEntity(ServerPlayer actor,net.minecraft.world.entity.LivingEntity entity,GlobalPos destination){
        if(entity instanceof ServerPlayer player){if(player!=actor&&!io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(player.getUUID(),actor.getUUID()))return false;return teleport(player,destination);}
        var source=actor.level();var target=source.getServer().getLevel(destination.dimension());var at=destination.pos();
        if(target==null||entity.isPassenger()||entity.isVehicle()||!Authority.mayChange(source,actor,entity.blockPosition())||!ConcordanceEffects.mayHarm(actor,entity)&&!(entity instanceof net.minecraft.world.entity.TamableAnimal pet&&pet.getOwner()==actor)&&!NouveauSummons.ownedBy(entity,actor.getUUID()))return false;
        if(!target.getWorldBorder().isWithinBounds(at)||at.getY()<target.getMinY()||at.getY()>=target.getMaxY())return false;target.getChunkAt(at);
        var point=Vec3.atBottomCenterOf(at);if(!Authority.mayChange(target,actor,at)||!target.getBlockState(at.below()).isSolidRender()||!target.noCollision(entity,entity.getBoundingBox().move(point.subtract(entity.position()))))return false;
        return entity.teleport(new net.minecraft.world.level.portal.TeleportTransition(target,point,Vec3.ZERO,entity.getYRot(),entity.getXRot(),net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING))!=null;
    }
    public static InteractionResult useOn(UseOnContext c,String name){
        if(!(c.getPlayer() instanceof ServerPlayer p)||!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||p.isSpectator()||!p.isAlive()||!Authority.mayChange(l,p,c.getClickedPos()))return InteractionResult.FAIL;
        var stack=c.getItemInHand();var at=c.getClickedPos();
        if(name.equals("mimic_scroll")){stack.set(MIMIC,BuiltInRegistries.BLOCK.getKey(l.getBlockState(at).getBlock()).toString());return InteractionResult.SUCCESS;}
        if(name.equals("scryer_scroll")||name.equals("enchanters_eye")||name.equals("blank_parchment")){
            var destination=GlobalPos.of(l.dimension(),at);if(!NouveauScrying.valid(p,destination))return InteractionResult.PASS;
            if(name.equals("blank_parchment")){var scroll=new ItemStack(NouveauCatalog.item("scryer_scroll"));scroll.set(DESTINATION,destination);stack.consume(1,p);if(!p.getInventory().add(scroll))p.drop(scroll,false,net.minecraft.util.Prediction.SERVER_ONLY);}
            else stack.set(DESTINATION,destination);return InteractionResult.SUCCESS;
        }
        if(name.equals("jar_of_light"))return use(p,stack,name,c.getHand());
        if(name.equals("runic_chalk")){
            var parchment=p.getOffhandItem();if(!parchment.is(NouveauCatalog.item("spell_parchment"))||!parchment.has(JugcraftConcordance.INSCRIPTION))return InteractionResult.FAIL;
            var target=at.relative(c.getClickedFace());if(!l.hasChunkAt(target)||!l.getBlockState(target).canBeReplaced()||!Authority.mayChange(l,p,target))return InteractionResult.FAIL;
            l.setBlock(target,NouveauCatalog.block("rune").defaultBlockState(),Block.UPDATE_ALL);
            if(l.getBlockEntity(target) instanceof NouveauWorkshop rune){rune.placedBy(p);rune.setItem(0,parchment.split(1));}
            stack.consume(1,p);return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public static final class Scroll extends Item {
        private final String name;
        public Scroll(String name,Properties p){super(p);this.name=name;}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){return p instanceof ServerPlayer s?NouveauUtilities.use(s,p.getItemInHand(hand),name,hand):InteractionResult.SUCCESS;}
        @Override public InteractionResult useOn(UseOnContext c){return NouveauUtilities.useOn(c,name);}
    }
}

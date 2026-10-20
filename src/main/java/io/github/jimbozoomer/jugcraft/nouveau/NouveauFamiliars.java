package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Familiar identity belongs to its player and persists independently of books or workshop workers. */
public final class NouveauFamiliars {
    public static AttachmentType<List<String>> KNOWN;
    public static AttachmentType<String> ACTIVE;
    public static AttachmentType<Boolean> MAGNET;
    public static EntityType<NouveauFamiliar> TYPE;
    public static net.minecraft.world.inventory.MenuType<NouveauFamiliarMenu> MENU;
    public static final List<String> SPECIES=List.of("bookwyrm","starbuncle","drygmy","whirlisprig","wixie","amethyst_golem");
    private NouveauFamiliars(){}
    public static void register(){
        KNOWN=AttachmentRegistry.<List<String>>builder().persistent(Codec.STRING.listOf(0,6)).copyOnDeath().buildAndRegister(Jugcraft.id("nouveau_familiars"));
        ACTIVE=AttachmentRegistry.<String>builder().persistent(Codec.STRING).copyOnDeath().buildAndRegister(Jugcraft.id("nouveau_familiar"));
        MAGNET=AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).copyOnDeath().buildAndRegister(Jugcraft.id("nouveau_familiar_magnet"));
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_familiar"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauFamiliar>of(NouveauFamiliar::new,MobCategory.MISC).sized(.5F,.6F).clientTrackingRange(10).build(key));
        FabricDefaultAttributeRegistry.register(TYPE,NouveauFamiliar.attributes());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_familiars"),new net.minecraft.world.inventory.MenuType<>(NouveauFamiliarMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
        for(var e:NouveauCatalog.ITEM_ENTRIES.values())if(e.kind().equals("familiar"))NouveauCatalog.registerItem(e,p->new Script(e.source().substring(9),p));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%100==0)for(var p:server.getPlayerList().getPlayers())ensure(p);});
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,damage,blocked)->{
            if(entity instanceof ServerPlayer p&&active(p).equals("amethyst_golem")&&damage>0&&source.getEntity() instanceof LivingEntity attacker&&ConcordanceEffects.mayHarm(p,attacker)&&Authority.mayStrike(p,attacker)&&attacker.distanceToSqr(p)<=64){var push=attacker.position().subtract(p.position()).normalize().scale(.4);attacker.setDeltaMovement(attacker.getDeltaMovement().add(push.x,.2,push.z));attacker.syncVelocity=true;}
        });
    }
    public static String active(Player p){return p.getAttachedOrElse(ACTIVE,"");}
    public static void choose(ServerPlayer p,String species){
        if(!species.isEmpty()&&!p.getAttachedOrElse(KNOWN,List.of()).contains(species))return;
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||!species.isEmpty()&&p.hasEffect(NouveauStatuses.EFFECTS.get("summoning_sickness"))||!RateGate.allow(p,"nouveau_familiar_switch",20))return;
        p.setAttached(ACTIVE,species);for(var old:p.level().getEntitiesOfClass(NouveauFamiliar.class,p.getBoundingBox().inflate(48),f->p.getUUID().equals(f.owner())))old.discard();ensure(p);
        if(!species.isEmpty())NouveauStatuses.give(p,"summoning_sickness",100,0,p);
    }
    public static boolean bind(ServerLevel level,ServerPlayer p,NouveauWorkshop altar){
        for(var e:level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(NouveauRitualWork.center(altar)).inflate(5),e->e.isAlive()).stream().limit(16).toList()){
            String species=e instanceof Starbuncle?"starbuncle":e instanceof BookWyrm?"bookwyrm":e instanceof NouveauHelper helper?helper.species():"";
            if(!Set.of("starbuncle","bookwyrm","drygmy","whirlisprig","wixie","amethyst_golem").contains(species)||!Authority.mayChange(level,p,e.blockPosition()))continue;
            if(e instanceof Starbuncle s&&!s.mayServe(p))continue;
            if(e instanceof BookWyrm w&&(w.owner()==null||!w.owner().equals(p.getUUID())))continue;
            if(e instanceof NouveauHelper h&&(h.owner()==null||!h.owner().equals(p.getUUID())))continue;
            var script=new ItemStack(NouveauCatalog.item("familiar_"+species));if(!altar.canOutput(script))return false;
            // A pact is recorded without destroying a working helper or its inventory.
            altar.output(script);NouveauAdvancements.award(p,"familiar");return true;
        }return false;
    }
    public static void ensure(ServerPlayer p){
        String species=active(p);if(species.isEmpty()||!JugNouveau.enabled()||!p.isAlive()||p.isSpectator())return;
        var present=p.level().getEntitiesOfClass(NouveauFamiliar.class,p.getBoundingBox().inflate(32),e->p.getUUID().equals(e.owner()));
        if(!present.isEmpty())return;var familiar=TYPE.create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(familiar==null)return;
        familiar.bind(p.getUUID(),species);familiar.snapTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);p.level().addFreshEntity(familiar);
    }
    private static final class Script extends Item {
        private final String species;Script(String species,Properties p){super(p);this.species=species;}
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||!RateGate.allow(p,"nouveau_familiar",100))return InteractionResult.FAIL;
            var known=new ArrayList<>(p.getAttachedOrElse(KNOWN,List.of()));if(!known.contains(species)){known.add(species);p.setAttached(KNOWN,List.copyOf(known));}
            p.openMenu(new SimpleMenuProvider((id,inventory,viewer)->new NouveauFamiliarMenu(id,inventory),net.minecraft.network.chat.Component.literal("Bound Familiars")));return InteractionResult.SUCCESS;
        }
    }
}

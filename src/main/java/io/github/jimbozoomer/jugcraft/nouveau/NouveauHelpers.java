package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class NouveauHelpers {
    public static final Map<String,EntityType<NouveauHelper>> TYPES=new LinkedHashMap<>();
    public static final Map<String,String> HOMES=Map.of("drygmy","drygmy_stone","whirlisprig","whirlisprig_flower","wixie","wixie_cauldron","amethyst_golem","imbuement_chamber","alakarkinos","repository");
    private NouveauHelpers(){}
    public static void register(){
        for(String name:List.of("drygmy","whirlisprig","wixie","amethyst_golem","alakarkinos")){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_"+name));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauHelper>of((t,l)->new NouveauHelper(t,l,name),MobCategory.CREATURE).sized(name.equals("alakarkinos")?1.3F:.65F,name.equals("amethyst_golem")?1.5F:.85F).clientTrackingRange(10).build(key));
            TYPES.put(name,type);FabricDefaultAttributeRegistry.register(type,NouveauHelper.attributes());NouveauSpawns.register(type,false);
            if(Set.of("drygmy","whirlisprig","alakarkinos").contains(name))net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld().and(c->name.equals("alakarkinos")?c.getBiomeKey().identifier().getPath().contains("beach"):c.getBiomeKey().identifier().getPath().contains("forest")),MobCategory.CREATURE,type,3,1,2);
            String charm=name+"_charm";var entry=NouveauCatalog.ITEM_ENTRIES.get(charm);
            NouveauCatalog.registerItem(entry,p->new Charm(name,p));
        }
    }
    public static final class Charm extends Item {
        public final String species;
        public Charm(String name,Properties p){super(p);species=name;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p)||!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
            if(!JugNouveau.enabled()||p.isSpectator()||!Authority.mayChange(l,p,c.getClickedPos()))return InteractionResult.FAIL;
            var original=l.getBlockState(c.getClickedPos());boolean flexible=species.equals("amethyst_golem")||species.equals("alakarkinos");
            boolean transform=switch(species){case "drygmy"->original.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE);case "wixie"->original.is(net.minecraft.world.level.block.Blocks.CAULDRON);case "whirlisprig"->original.is(net.minecraft.tags.BlockTags.FLOWERS);default->false;};
            if(!flexible&&!transform&&(!(l.getBlockEntity(c.getClickedPos()) instanceof NouveauWorkshop home)||!home.mayUse(p)||!home.kind().equals(HOMES.get(species))))return InteractionResult.FAIL;
            var roster=WorkerRoster.of(l.getServer());if(roster.workers(p.getUUID()).size()>=WorkerRoster.MAX_PER_PLAYER)return InteractionResult.FAIL;
            if(l.getEntitiesOfClass(NouveauHelper.class,new net.minecraft.world.phys.AABB(c.getClickedPos()).inflate(16),h->h.species().equals(species)&&c.getClickedPos().equals(h.home())).size()>=4)return InteractionResult.FAIL;
            var helper=TYPES.get(species).create(l,EntitySpawnReason.MOB_SUMMONED);if(helper==null)return InteractionResult.FAIL;
            helper.setOwner(p.getUUID());helper.bind(c.getClickedPos());var at=c.getClickedPos().relative(c.getClickedFace());helper.snapTo(at.getX()+.5,at.getY()+1,at.getZ()+.5,0,0);
            if(!l.noCollision(helper)||!l.addFreshEntity(helper))return InteractionResult.FAIL;
            if(transform){l.setBlock(c.getClickedPos(),NouveauCatalog.block(HOMES.get(species)).defaultBlockState(),3);if(l.getBlockEntity(c.getClickedPos()) instanceof NouveauWorkshop home)home.placedBy(p);}
            roster.note(p.getUUID(),helper.getUUID(),helper.kind(),helper.status(),l.dimension().identifier().toString(),helper.blockPosition());c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
        }
    }
}

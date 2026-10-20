package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

public final class NouveauEncounters {
    public static final Map<String,EntityType<NouveauBeast>> TYPES=new LinkedHashMap<>();
    private NouveauEncounters(){}
    public static void register(){
        List<String> names=new ArrayList<>(List.of("wilden_hunter","wilden_guardian","wilden_stalker","wilden_chimera"));
        for(String tree:List.of("blazing","cascading","flourishing","vexing")){names.add(tree+"_weald_walker");names.add(tree+"_weald_waddler");}
        for(String name:names){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_"+name));boolean boss=name.equals("wilden_chimera"),walker=name.endsWith("walker"),baby=name.endsWith("waddler");
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauBeast>of((t,l)->name.startsWith("wilden")?new NouveauBeast.Wilden(t,l,name):new NouveauBeast(t,l,name),name.startsWith("wilden")?MobCategory.MONSTER:MobCategory.CREATURE)
                .sized(boss?2.5F:walker?1.5F:baby?.7F:.9F,boss?3F:walker?3.8F:baby?.9F:1.8F).clientTrackingRange(boss?16:10).build(key));
            TYPES.put(name,type);FabricDefaultAttributeRegistry.register(type,NouveauBeast.attributes(name));if(!boss)NouveauSpawns.register(type,name.startsWith("wilden"));
            if(name.startsWith("wilden")&&!boss)BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(c->c.getBiome().getBaseTemperature()<1.2F),MobCategory.MONSTER,type,8,1,2);
        }
        for(var e:NouveauCatalog.ITEM_ENTRIES.values())if(e.kind().equals("spawn_egg"))NouveauCatalog.registerItem(e,p->new Egg(e.source().replace("_se",""),p));
    }
    public static boolean summon(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        boolean horn=false,spike=false,wing=false;for(int i=0;i<8;i++){horn|=b.ritualAugment(i).is(NouveauCatalog.item("wilden_horn"));spike|=b.ritualAugment(i).is(NouveauCatalog.item("wilden_spike"));wing|=b.ritualAugment(i).is(NouveauCatalog.item("wilden_wing"));}
        String name=horn&&spike&&wing?"wilden_chimera":List.of("wilden_hunter","wilden_guardian","wilden_stalker").get(l.getRandom().nextInt(3));
        if(name.equals("wilden_chimera")&&!l.getEntitiesOfClass(NouveauBeast.class,new net.minecraft.world.phys.AABB(NouveauRitualWork.center(b)).inflate(64),NouveauBeast::boss).isEmpty())return false;
        var entity=TYPES.get(name).create(l,EntitySpawnReason.MOB_SUMMONED);if(entity==null)return false;
        BlockPos at=NouveauRitualWork.center(b).offset(3,1,0);if(!Authority.mayChange(l,p,at))return false;entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        if(!l.noCollision(entity)||!l.addFreshEntity(entity))return false;
        return true;
    }
    public static boolean awaken(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        if(NouveauRitualWork.count(b,Items.WRITABLE_BOOK)>0&&b.canOutput(new ItemStack(JugNouveau.CHARM))){b.output(new ItemStack(JugNouveau.CHARM));return true;}
        for(BlockPos at:BlockPos.betweenClosed(NouveauRitualWork.center(b).offset(-5,-1,-5),NouveauRitualWork.center(b).offset(5,3,5))){
            if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at))continue;var s=l.getBlockState(at);
            if(s.is(Blocks.BUDDING_AMETHYST)&&b.canOutput(new ItemStack(NouveauCatalog.item("amethyst_golem_charm")))){b.output(new ItemStack(NouveauCatalog.item("amethyst_golem_charm")));l.setBlock(at,Blocks.AMETHYST_BLOCK.defaultBlockState(),3);return true;}
            for(var tree:Map.of("red","blazing","blue","cascading","green","flourishing","purple","vexing").entrySet())if(s.is(NouveauCatalog.block(tree.getKey()+"_archwood_log"))){
                var walker=TYPES.get(tree.getValue()+"_weald_walker").create(l,EntitySpawnReason.MOB_SUMMONED);if(walker==null)return false;
                var treeBlocks=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var pending=new ArrayDeque<BlockPos>();pending.add(at.immutable());boolean blocked=false;
                var log=NouveauCatalog.block(tree.getKey()+"_archwood_log");var leaves=NouveauCatalog.block(tree.getKey()+"_archwood_leaves");
                while(!pending.isEmpty()&&treeBlocks.size()<512){var pos=pending.removeFirst();if(treeBlocks.containsKey(pos)||pos.distSqr(at)>24*24||!l.hasChunkAt(pos))continue;var state=l.getBlockState(pos);if(!state.is(log)&&!state.is(leaves))continue;if(!Authority.mayChange(l,p,pos)){blocked=true;break;}treeBlocks.put(pos.immutable(),state);for(var d:Direction.values())pending.add(pos.relative(d));}
                if(blocked||treeBlocks.size()>=512)continue;
                walker.guard(p.getUUID(),at);walker.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
                treeBlocks.forEach((pos,state)->l.setBlock(pos,Blocks.AIR.defaultBlockState(),2));
                if(!l.noCollision(walker)||!l.addFreshEntity(walker)){treeBlocks.forEach((pos,state)->l.setBlock(pos,state,3));continue;}
                treeBlocks.keySet().forEach(pos->l.updateNeighborsAt(pos,Blocks.AIR));return true;
            }
        }return false;
    }
    private static final class Egg extends Item {
        private final String kind;Egg(String k,Properties p){super(p);kind=k;}
        @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return net.minecraft.world.InteractionResult.SUCCESS;
            var type=TYPES.containsKey(kind)?TYPES.get(kind):NouveauHelpers.TYPES.containsKey(kind)?NouveauHelpers.TYPES.get(kind):kind.equals("starbuncle")?JugNouveau.STARBUNCLE:null;
            var at=c.getClickedPos().relative(c.getClickedFace());if(type==null||!Authority.mayChange(p.level(),p,at))return net.minecraft.world.InteractionResult.FAIL;
            var entity=type.create(p.level(),EntitySpawnReason.SPAWN_ITEM_USE);if(entity==null)return net.minecraft.world.InteractionResult.FAIL;entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
            if(!p.level().noCollision(entity)||!p.level().addFreshEntity(entity))return net.minecraft.world.InteractionResult.FAIL;c.getItemInHand().consume(1,p);return net.minecraft.world.InteractionResult.SUCCESS;
        }
    }
}

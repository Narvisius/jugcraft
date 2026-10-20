package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Bounded terrain and creature work shared by the braziers and their remote relays. */
public final class NouveauRitualWork {
    private NouveauRitualWork(){}
    public static int count(NouveauWorkshop b,Item item){int count=0;for(int i=0;i<8;i++)if(b.ritualAugment(i).is(item))count+=b.ritualAugment(i).getCount();return count;}
    public static BlockPos center(NouveauWorkshop b){
        if(b.getLevel() instanceof ServerLevel l){var actor=Authority.answering(l,b.owner());if(actor!=null)for(var at:b.links())if(at.distSqr(b.getBlockPos())<=225&&l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof NouveauWorkshop relay&&relay.kind().equals("brazier_relay")&&relay.permitted(actor))return at;}return b.getBlockPos();
    }
    public static int radius(NouveauWorkshop b,int base,int cap){return Math.min(cap,base+count(b,NouveauCatalog.item("source_gem")));}
    public static boolean restore(ServerLevel l,ServerPlayer p,LivingEntity e){
        if(!Authority.mayChange(l,p,e.blockPosition()))return false;
        if(e instanceof ZombieVillager zombie){((io.github.jimbozoomer.jugcraft.mixin.NouveauZombieAccess)zombie).jugcraft$beginRestoration(p.getUUID(),1);return true;}
        if(e.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD))return ConcordanceEffects.mayHarm(p,e)&&Authority.mayStrike(p,e)&&e.hurtServer(l,l.damageSources().indirectMagic(p,p),2);
        if(e.getHealth()>=e.getMaxHealth())return false;e.heal(2);return true;
    }
    public static boolean flower(ServerLevel l,ServerPlayer p,NouveauWorkshop b,BlockPos ground,int cursor){
        var pos=ground.above();if(!Authority.mayChange(l,p,pos)||!l.getBlockState(pos).isAir())return false;
        boolean desert=count(b,Items.SAND)>0;Block flower=desert?(cursor%3==0?Blocks.CACTUS:Blocks.DEAD_BUSH):new Block[]{Blocks.DANDELION,Blocks.POPPY,Blocks.AZURE_BLUET,Blocks.OXEYE_DAISY,Blocks.CORNFLOWER,Blocks.SHORT_GRASS}[Math.floorMod(cursor,6)];
        if(desert){if(!l.getBlockState(ground).is(net.minecraft.tags.BlockTags.SAND))return false;}else if(!l.getBlockState(ground).is(Blocks.GRASS_BLOCK))return false;
        return flower.defaultBlockState().canSurvive(l,pos)&&l.setBlock(pos,flower.defaultBlockState(),Block.UPDATE_ALL);
    }
    public static boolean forest(ServerLevel l,ServerPlayer p,NouveauWorkshop b,BlockPos ground,int cursor){
        var state=l.getBlockState(ground);var above=ground.above();boolean taiga=count(b,Items.BROWN_MUSHROOM)>0,jungle=count(b,Items.GLOW_BERRIES)>0;
        if(!state.is(Blocks.GRASS_BLOCK)&&!state.is(Blocks.DIRT)&&!state.is(Blocks.PODZOL))return false;
        if(taiga&&Authority.mayChange(l,p,ground))l.setBlock(ground,Blocks.PODZOL.defaultBlockState(),Block.UPDATE_ALL);
        if(cursor%19!=0||!l.getBlockState(above).isAir())return false;
        // Native tree growth can touch a canopy. Reserve its entire bounded footprint first.
        for(var at:BlockPos.betweenClosed(above.offset(-7,0,-7),above.offset(7,17,7)))if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)||l.getBlockEntity(at)!=null)return false;
        var sapling=(SaplingBlock)(taiga?Blocks.SPRUCE_SAPLING:jungle?Blocks.JUNGLE_SAPLING:cursor%2==0?Blocks.OAK_SAPLING:Blocks.BIRCH_SAPLING);
        l.setBlock(above,sapling.defaultBlockState().setValue(SaplingBlock.STAGE,1),Block.UPDATE_ALL);sapling.advanceTree(l,above,l.getBlockState(above),l.getRandom());return true;
    }
    public static boolean warp(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        GlobalPos destination=null;for(int i=0;i<8&&destination==null;i++)destination=b.ritualAugment(i).get(NouveauUtilities.DESTINATION);if(destination==null)return false;
        boolean changed=false;for(var e:l.getEntitiesOfClass(LivingEntity.class,new AABB(center(b)).inflate(3),LivingEntity::isAlive).stream().limit(16).toList())changed|=NouveauUtilities.teleportEntity(p,e,destination);return changed;
    }
    public static void islandBiome(ServerLevel l,ServerPlayer p,NouveauWorkshop b,boolean desert){
        var origin=center(b);int radius=radius(b,7,16);
        var key=desert?(count(b,Items.TERRACOTTA)>0?net.minecraft.world.level.biome.Biomes.BADLANDS:net.minecraft.world.level.biome.Biomes.DESERT):(count(b,NouveauCatalog.item("frostaya_pod"))>0?net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS:net.minecraft.world.level.biome.Biomes.PLAINS);
        var biome=l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(key);
        java.util.List<net.minecraft.world.level.chunk.ChunkAccess> changed=new java.util.ArrayList<>();
        for(int cx=(origin.getX()-radius)>>4;cx<=(origin.getX()+radius)>>4;cx++)for(int cz=(origin.getZ()-radius)>>4;cz<=(origin.getZ()+radius)>>4;cz++){
            var cp=new BlockPos(cx*16,origin.getY(),cz*16);if(!l.hasChunkAt(cp))continue;var chunk=l.getChunkAt(cp);boolean touched=false;
            for(int sy=Math.max(l.getMinY()>>4,(origin.getY()-8)>>4);sy<=Math.min((l.getMaxY()-1)>>4,(origin.getY()+24)>>4);sy++){
                var section=chunk.getSection(chunk.getSectionIndex(sy*16));final int qy=sy*4;
                java.util.Set<BlockPos> allowed=new java.util.HashSet<>();
                for(int x=0;x<4;x++)for(int z=0;z<4;z++)for(int y=0;y<4;y++){
                    var cell=new BlockPos(cx*16+x*4,sy*16+y*4,cz*16+z*4);if(Math.pow(cell.getX()+2-origin.getX(),2)+Math.pow(cell.getZ()+2-origin.getZ(),2)>radius*radius)continue;
                    boolean safe=true;for(var at:BlockPos.betweenClosed(cell,cell.offset(3,3,3)))if(!Authority.mayChange(l,p,at)){safe=false;break;}if(safe)allowed.add(new BlockPos(cx*4+x,qy+y,cz*4+z));
                }
                if(allowed.isEmpty())continue;section.fillBiomesFromNoise((x,y,z)->allowed.contains(new BlockPos(x,y,z))?biome:section.getNoiseBiome(x&3,y&3,z&3),cx*4,qy,cz*4);touched=true;
            }if(touched){chunk.markUnsaved();changed.add(chunk);}
        }if(!changed.isEmpty())l.getChunkSource().chunkMap.resendBiomesForChunks(changed);
    }
    public static boolean harvest(ServerLevel l,ServerPlayer p,NouveauWorkshop b,BlockPos at){
        var crop=l.getBlockState(at);if(!(crop.getBlock() instanceof CropBlock plant)||!plant.isMaxAge(crop))return false;
        var drops=Block.getDrops(crop,l,at,null,p,ItemStack.EMPTY);var seed=crop.getCloneItemStack(l,at,false);boolean reserved=false;
        for(var drop:drops)if(!reserved&&drop.is(seed.getItem())&&!drop.isEmpty()){drop.shrink(1);reserved=true;}
        if(!reserved)return false;l.setBlock(at,plant.getStateForAge(0),Block.UPDATE_ALL);
        for(var drop:drops){if(drop.isEmpty())continue;for(var face:Direction.values()){var store=NouveauStorage.accessible(l,p,b.getBlockPos().relative(face));if(store!=null&&NouveauFilters.frames(l,b.getBlockPos().relative(face),drop)&&NouveauCrafting.deposit(store,drop,true)){NouveauCrafting.deposit(store,drop,false);drop.setCount(0);break;}}if(!drop.isEmpty())Block.popResource(l,at,drop);}
        return true;
    }
    public static boolean growBabies(ServerLevel l,ServerPlayer p,NouveauWorkshop b){boolean changed=false;for(var animal:l.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(center(b)).inflate(7),e->e.isBaby()).stream().limit(8).toList())if(Authority.mayChange(l,p,animal.blockPosition())){animal.ageUp(60,true);changed=true;}return changed;}
}

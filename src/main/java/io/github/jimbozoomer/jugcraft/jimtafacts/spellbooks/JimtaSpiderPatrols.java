package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Source storm-hunt cadence with loaded-chunk, claim and local population bounds. */
public final class JimtaSpiderPatrols {
    private static final Map<ServerLevel,Integer> NEXT=new WeakHashMap<>();
    private static final TagKey<Biome> HABITAT=TagKey.create(Registries.BIOME,Jugcraft.id("jimtafacts/spellbooks/ice_spider_patrols"));
    public static void register(){ServerTickEvents.END_LEVEL_TICK.register(JimtaSpiderPatrols::tick);ServerLifecycleEvents.SERVER_STOPPED.register(s->NEXT.clear());}
    private static void tick(ServerLevel level){
        if(!JimtaSpellbooks.enabled()||!level.dimension().equals(Level.OVERWORLD)||level.getDifficulty()==Difficulty.PEACEFUL||!level.getGameRules().get(GameRules.SPAWN_MOBS)||!level.getGameRules().get(GameRules.SPAWN_MONSTERS)||!level.getGameRules().get(GameRules.SPAWN_PATROLS))return;
        var players=level.players().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()).toList();if(players.isEmpty())return;
        int left=NEXT.getOrDefault(level,1)-1;if(left>0){NEXT.put(level,left);return;}
        var groups=new ArrayList<BlockPos>();for(var player:players)if(groups.stream().noneMatch(p->p.distSqr(player.blockPosition())<48*48))groups.add(player.blockPosition());
        NEXT.put(level,9600/Math.max(1,Math.min(64,groups.size()))+level.getRandom().nextInt(3600));
        if(!level.isRaining()||level.getRandom().nextBoolean())return;var target=players.get(level.getRandom().nextInt(players.size()));
        if(Town.isInside(level,target.blockPosition())||LandRegistry.isClaimed(level,target.blockPosition())||level.getEntitiesOfClass(JimtaEncounterCreatures.IceSpider.class,target.getBoundingBox().inflate(80)).size()>=8)return;
        for(int i=0;i<4;i++){
            int x=(24+level.getRandom().nextInt(24))*(level.getRandom().nextBoolean()?1:-1),z=(24+level.getRandom().nextInt(24))*(level.getRandom().nextBoolean()?1:-1);var at=target.blockPosition().offset(x,0,z);
            if(!level.hasChunkAt(at.offset(-10,0,-10))||!level.hasChunkAt(at.offset(10,0,10))||!level.hasChunkAt(at.offset(-10,0,10))||!level.hasChunkAt(at.offset(10,0,-10)))continue;
            at=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,at);
            if(!level.getBiome(at).is(HABITAT)||Town.isInside(level,at)||LandRegistry.isClaimed(level,at)||level.getBrightness(LightLayer.BLOCK,at)>8||!Monster.checkMobSpawnRules(JimtaEncounterCreatures.ICE_SPIDER,level,EntitySpawnReason.PATROL,at,level.getRandom()))continue;
            var spider=JimtaEncounterCreatures.ICE_SPIDER.create(level,EntitySpawnReason.PATROL);if(spider==null)continue;spider.setPos(Vec3.atBottomCenterOf(at));if(!level.noCollision(spider)||level.containsAnyLiquid(spider.getBoundingBox()))continue;
            spider.setTarget(target);spider.emerge();if(level.addFreshEntity(spider)){level.playSound(null,at,JimtaEncounterCreatures.SOUNDS.get("ice_spider_howl"),SoundSource.HOSTILE,4,1);return;}
        }
    }
}

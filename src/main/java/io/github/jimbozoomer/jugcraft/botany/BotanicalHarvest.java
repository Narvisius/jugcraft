package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Expensive biological generators retain their source conditions and persist flower-variety history. */
final class BotanicalHarvest {
    static final String NATURAL_SLIME="jugcraft:botanical_natural_slime";
    private static final int[] VALUES={2000,2100,2200,2300,3280,4033,4657,5150,6622,7860,10418,12600,14769,16671,19000,25400,33471,40900,47579,53600,59057,64264,69217,74483,79352,83869,88059,92129,96669,100940,105239,112044,118442,124612,130583,136228,141703,178442,213959,247725,279956,313671,345833,377227,437689,495526,553702,638554};
    private final BotanicalWorkshop flower;private int cursor,streak=-1,repeats;
    private final List<String> history=new ArrayList<>();
    BotanicalHarvest(BotanicalWorkshop flower){this.flower=flower;}
    void tick(ServerLevel level,ServerPlayer actor){
        if(flower.kind().equals("rafflowsia")){if(level.getGameTime()%40==0)flowers(level,actor);return;}
        if(level.getGameTime()%5!=0)return;
        if(flower.kind().equals("narslimmus")){
            int checked=0;
            for(var slime:level.getEntitiesOfClass(Slime.class,new AABB(flower.effectivePos()).inflate(2))){
                if(++checked>32)break;
                int value=1200*(1<<Math.clamp(slime.getSize(),1,4));
                if(!slime.entityTags().contains(NATURAL_SLIME)||!BotanicalDefenses.eligible(level,actor,slime)||flower.capacity()-flower.mana()<value)continue;
                slime.discard();flower.generate(value);effect(level,slime.position(),ParticleTypes.HAPPY_VILLAGER);return;
            }
        }else if(flower.kind().equals("shulk_me_not")&&flower.mana()==0){
            int checked=0;var center=Vec3.atCenterOf(flower.effectivePos());
            for(var shulker:level.getEntitiesOfClass(Shulker.class,new AABB(flower.effectivePos()).inflate(8))){
                if(++checked>32)break;var target=shulker.getTarget();
                if(target==shulker||!(target instanceof Enemy)||!target.hasEffect(MobEffects.LEVITATION)||shulker.distanceToSqr(center)>=64||target.distanceToSqr(center)>=64)continue;
                if(!BotanicalDefenses.eligible(level,actor,shulker)||!BotanicalDefenses.eligible(level,actor,target))continue;
                effect(level,shulker.position(),ParticleTypes.PORTAL);effect(level,target.position(),ParticleTypes.PORTAL);
                target.discard();shulker.discard();flower.generate(75000);return;
            }
        }
    }
    private void flowers(ServerLevel level,ServerPlayer actor){
        if(flower.capacity()-flower.mana()<2100)return;
        for(int checked=0;checked<128;checked++){
            int i=Math.floorMod(cursor++,1331);var at=flower.effectivePos().offset(i%11-5,(i/11)%11-5,i/121-5);
            if(!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at))continue;
            if(!(level.getBlockEntity(at) instanceof BotanicalWorkshop other)||other==flower||other.kind().equals("rafflowsia")||!other.generating()&&!other.functional()&&!other.kind().equals("pure_daisy"))continue;
            // Never consume a workshop's stored ingredients or mana along with the flower.
            if(other.mana()>0||!other.isEmpty()||!other.permitted(actor))continue;
            String id=BuiltInRegistries.BLOCK.getKey(other.getBlockState().getBlock()).toString();int index=history.indexOf(id);
            int next=Math.min(streak+1,index<0?VALUES.length-1:index),times=next==0?Math.min(repeats+1,1000000):1;
            int value=Math.max(1,VALUES[next]/times);if(flower.capacity()-flower.mana()<value)continue;
            if(!level.setBlock(at,Blocks.AIR.defaultBlockState(),3))continue;
            history.remove(id);history.addFirst(id);while(history.size()>VALUES.length-1)history.removeLast();
            streak=next;repeats=times;flower.generate(value);flower.setChanged();effect(level,Vec3.atCenterOf(at),ParticleTypes.HAPPY_VILLAGER);return;
        }flower.setChanged();
    }
    private static void effect(ServerLevel level,Vec3 at,net.minecraft.core.particles.SimpleParticleType type){level.sendParticles(type,at.x,at.y+.4,at.z,12,.3,.3,.3,.04);}
    void save(ValueOutput out){out.putInt("harvest_cursor",cursor);out.putInt("harvest_streak",streak);out.putInt("harvest_repeats",repeats);out.store("harvest_history",Codec.STRING.listOf(),history);}
    void load(ValueInput in){cursor=Math.floorMod(in.getIntOr("harvest_cursor",0),1331);streak=Math.clamp(in.getIntOr("harvest_streak",-1),-1,VALUES.length-1);repeats=Math.clamp(in.getIntOr("harvest_repeats",0),0,1000000);history.clear();in.read("harvest_history",Codec.STRING.listOf()).orElse(List.of()).stream().distinct().limit(VALUES.length-1).forEach(history::add);}
}

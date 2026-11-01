package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
public final class WildParticles {
    public static final Map<String,SimpleParticleType> TYPES=new LinkedHashMap<>();
    static void register(){for(String name:List.of("moon_full","moon_gibbous","moon_half","moon_crescent","moon_new","pixel","snowflake","glow"))TYPES.put(name,Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("wilds_"+name),FabricParticleTypes.simple()));}
    public static void moon(ServerLevel l,LivingEntity caster,String phase){l.sendParticles(TYPES.get("moon_"+phase),caster.getX(),caster.getY()+caster.getBbHeight()+.65,caster.getZ(),1,0,0,0,0);l.sendParticles(TYPES.get("pixel"),caster.getX(),caster.getY()+caster.getBbHeight()/2,caster.getZ(),30,.4,.5,.4,.12);}
}

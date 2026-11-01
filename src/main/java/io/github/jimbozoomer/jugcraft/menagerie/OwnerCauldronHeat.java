package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Heat follows the supplied heat-source tag without replacing native water cauldrons. */
public final class OwnerCauldronHeat {
    private static final TagKey<Block> HEAT=TagKey.create(Registries.BLOCK,Jugcraft.id("amends_cauldron_heat_source"));
    private static final ResourceKey<DamageType> BOILING=ResourceKey.create(Registries.DAMAGE_TYPE,Jugcraft.id("amends_boiling"));
    private OwnerCauldronHeat(){}
    public static boolean boiling(Level level,BlockPos pos){
        if(!level.hasChunkAt(pos.below()))return false;
        var heat=level.getBlockState(pos.below());return heat.is(HEAT)&&(!heat.hasProperty(BlockStateProperties.LIT)||heat.getValue(BlockStateProperties.LIT));
    }
    public static double surface(BlockState state){
        int layer=state.hasProperty(LayeredCauldronBlock.LEVEL)?state.getValue(LayeredCauldronBlock.LEVEL):Integer.parseInt(HouseholdBlock.value(state,"level"));return (6+3*layer)/16.0;
    }
    public static void inside(BlockState state,Level level,BlockPos pos,Entity entity){
        if(!(level instanceof ServerLevel server)||!(entity instanceof LivingEntity living)||!boiling(level,pos)||living.fireImmune())return;
        if(living.getY()>=pos.getY()+surface(state)||!living.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos).deflate(.125)))return;
        // Ambient hazards have no player identity: never use them to bypass town or claim combat protection.
        if(io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(level,pos)||io.github.jimbozoomer.jugcraft.finance.LandRegistry.isClaimed(level,pos))return;
        living.hurtServer(server,new DamageSource(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BOILING)),1);
    }
    public static void animate(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(!boiling(level,pos))return;
        for(int i=0;i<2;i++)level.addParticle(OwnerAmendmentCharge.PARTICLES.get("boiling_bubble"),pos.getX()+.1875+random.nextDouble()*.625,pos.getY()+surface(state),pos.getZ()+.1875+random.nextDouble()*.625,0,0,0);
        if(random.nextInt(4)==0)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,.4F,.4F,false);
    }
}

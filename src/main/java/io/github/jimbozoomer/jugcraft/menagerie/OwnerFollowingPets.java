package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.OwnerPetGoalsAccess;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;
/** Bring already-following nearby pets along a successful same-world player teleport. */
public final class OwnerFollowingPets {
    private static final TagKey<EntityType<?>> EXTRA=TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("amends_pets_follow_teleports_whitelist"));
    private OwnerFollowingPets(){}
    public record Nearby(ServerLevel level,Vec3 origin,List<TamableAnimal> pets){}
    private static boolean following(TamableAnimal pet,ServerPlayer owner){return pet.isAlive()&&pet.isOwnedBy(owner)&&!pet.unableToMoveToOwner()&&!pet.isOrderedToSit()&&!pet.isPassenger()&&!pet.isVehicle()&&!pet.isLeashed()&&(pet.getType().builtInRegistryHolder().is(EXTRA)||((OwnerPetGoalsAccess)pet).jugcraft$petGoals().getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof FollowOwnerGoal));}
    public static Nearby before(ServerPlayer owner){
        var pets=new ArrayList<TamableAnimal>();owner.level().getEntities(EntityTypeTest.forClass(TamableAnimal.class),AABB.ofSize(owner.position(),24,12,24),p->following(p,owner),pets,16);
        return new Nearby(owner.level(),owner.position(),List.copyOf(pets));
    }
    public static void after(ServerPlayer owner,Nearby nearby){
        if(!owner.isAlive()||owner.isSpectator()||owner.level()!=nearby.level||owner.position().distanceToSqr(nearby.origin)<144)return;
        for(var pet:nearby.pets)if(pet.level()==nearby.level&&following(pet,owner)&&pet.shouldTryTeleportToOwner())OwnerSupplementFlute.recall(owner,pet,Double.POSITIVE_INFINITY);
    }
}

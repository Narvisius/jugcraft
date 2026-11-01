package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Native capture/release with saved UUIDs, ownership checks and consume-after-success semantics. */
public final class ArcaneCapture extends Item {
    public static DataComponentType<CustomData> CREATURE;
    private final boolean boss;
    public ArcaneCapture(boolean boss,Properties p){super(p.stacksTo(1));this.boss=boss;}
    public static void register(){CREATURE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("arcane_captured_creature"),DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(CustomData.CODEC)).build());}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player actor,LivingEntity target,InteractionHand hand){
        if(!(actor instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(stack.has(CREATURE))return InteractionResult.PASS;
        if(!(target instanceof Mob mob)||!mob.isAlive()||mob.isPassenger()||mob.isVehicle()||mob.getLeashData()!=null||p.distanceToSqr(mob)>36||!p.hasLineOfSight(mob)||!Authority.mayChange(p.level(),p,mob.blockPosition()))return InteractionResult.FAIL;
        if(mob.getType().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_"+(boss?"boss":"quantum")+"_catcher_blacklisted"))))return InteractionResult.FAIL;
        if(mob instanceof OwnableEntity own&&own.getOwnerReference()!=null&&!own.getOwnerReference().getUUID().equals(p.getUUID()))return fail(p,"Only the creature's owner may capture it.");
        if(mob instanceof io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal animal&&animal.owner()!=null&&!animal.ownedBy(p))return fail(p,"Only the creature's owner may capture it.");
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        // Encounter and town lifecycle objects are not portable mobs. Capturing them would bypass rewards and resident tracking.
        if(id.startsWith("jugcraft:")&&!(mob instanceof io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal)&&!(mob instanceof ArcaneLostSoul)||mob.entityTags().stream().anyMatch(t->t.contains("arena")||t.contains("resident")||t.contains("guard")||t.equals("jugcraft_boss"))||id.equals("minecraft:ender_dragon"))return fail(p,"This resident or managed encounter cannot be moved with a catcher.");
        int cost=Math.max(1,mob.getType().getCategory().isFriendly()?Math.round(mob.getMaxHealth())/2:(int)(Math.round(mob.getMaxHealth())*1.1F));
        int stored=p.getAttachedOrCreate(ArcaneEssences.AUREAL);if(stored<cost)return fail(p,"Requires "+cost+" Aureal. Drink Aureal bottles to replenish it.");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());if(!mob.save(out))return InteractionResult.FAIL;var saved=out.buildResult();if(saved.toString().length()>65536)return fail(p,"This creature has too much attached data to capture safely.");
        var data=new CompoundTag();data.put("entity",saved);data.putString("name",mob.getName().getString());stack.set(CREATURE,CustomData.of(data));mob.discard();p.setAttached(ArcaneEssences.AUREAL,stored-cost);p.getInventory().setChanged();p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,.75F,1);return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        var data=c.getItemInHand().get(CREATURE);if(data==null)return InteractionResult.PASS;if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var l=p.level();var at=l.getBlockState(c.getClickedPos()).canBeReplaced()?c.getClickedPos():c.getClickedPos().relative(c.getClickedFace());
        if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)||p.distanceToSqr(Vec3.atCenterOf(at))>64)return InteractionResult.FAIL;
        var tag=data.copyTag().getCompound("entity");if(tag.isEmpty())return InteractionResult.FAIL;
        var entity=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),tag.get()),l,EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
        if(!(entity instanceof Mob)||entity.isPassenger()||entity.isVehicle()||entity.getBbWidth()>16||entity.getBbHeight()>16)return InteractionResult.FAIL;
        for(var world:p.level().getServer().getAllLevels())if(world.getEntity(entity.getUUID())!=null)return fail(p,"This creature is already present in the world.");
        entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);if(!l.noCollision(entity)||!l.getWorldBorder().isWithinBounds(entity.getBoundingBox()))return fail(p,"The creature needs more clear space.");
        for(var q:BlockPos.betweenClosed(BlockPos.containing(entity.getBoundingBox().minX,entity.getBoundingBox().minY,entity.getBoundingBox().minZ),BlockPos.containing(entity.getBoundingBox().maxX,entity.getBoundingBox().maxY,entity.getBoundingBox().maxZ)))if(!l.hasChunkAt(q)||!Authority.mayChange(l,p,q))return InteractionResult.FAIL;
        if(!l.addFreshEntity(entity))return InteractionResult.FAIL;c.getItemInHand().remove(CREATURE);p.getInventory().setChanged();l.playSound(null,at,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,.75F,.9F);return InteractionResult.SUCCESS;
    }
    private static InteractionResult fail(ServerPlayer p,String text){p.sendOverlayMessage(Component.literal(text));return InteractionResult.FAIL;}
}

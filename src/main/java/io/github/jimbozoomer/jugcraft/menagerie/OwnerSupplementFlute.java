package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;

/** Source flute songs and bounded, owner-checked pet recall. No chunk loading. */
public final class OwnerSupplementFlute extends Item {
    private static final int DURATION=72000;
    private static final String PETS="SuppsFlutePets";
    private static List<Song> songs;
    private static final Map<LivingEntity,Playing> playing=new WeakHashMap<>();
    private record Song(String name,int tempo,int weight,int[] notes){}
    private record Playing(ItemStack stack,Song song){}

    public OwnerSupplementFlute(Properties properties){super(properties.durability(64).repairable(Items.BAMBOO));}

    private static List<UUID> pets(ItemStack stack){
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                .read(PETS,UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().distinct().limit(6).toList();
    }
    private static boolean owned(Player player,LivingEntity pet){
        return pet instanceof net.minecraft.world.entity.animal.fox.Fox fox&&((io.github.jimbozoomer.jugcraft.mixin.OwnerFluteFoxAccess)fox).jugcraft$trusts(player)
                ||pet instanceof TamableAnimal tame&&tame.isTame()&&tame.isOwnedBy(player)
                ||pet instanceof AbstractHorse horse&&horse.isTamed()&&horse.getOwnerReference()!=null&&horse.getOwnerReference().matches(player);
    }
    @Override public boolean isFoil(ItemStack stack){return !pets(stack).isEmpty()||super.isFoil(stack);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(player.isSpectator()||!owned(player,target))return InteractionResult.PASS;
        if(!(player instanceof ServerPlayer owner))return InteractionResult.SUCCESS;
        if(!Authority.mayChange(owner.level(),owner,target.blockPosition()))return InteractionResult.FAIL;
        var bound=new ArrayList<>(pets(stack));
        if(bound.contains(target.getUUID())){
            if(player.isShiftKeyDown())bound.remove(target.getUUID());
            else{player.sendOverlayMessage(Component.literal("This companion is already bound. Crouch-use it to unbind."));return InteractionResult.SUCCESS;}
        }else if(bound.size()<6)bound.add(target.getUUID());
        else{player.sendOverlayMessage(Component.literal("This flute already remembers six companions."));return InteractionResult.FAIL;}
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->{if(bound.isEmpty())tag.remove(PETS);else tag.store(PETS,UUIDUtil.CODEC.listOf(),bound);});
        player.sendOverlayMessage(Component.literal("Flute: "+bound.size()+" / 6 companions"));
        player.getCooldowns().addCooldown(stack,20);return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(player.isSpectator())return InteractionResult.PASS;
        ItemStack stack=player.getItemInHand(hand);player.startUsingItem(hand);
        if(player instanceof ServerPlayer owner){
            playing.put(player,new Playing(stack,choose(stack,level)));
            List<LivingEntity> candidates=new ArrayList<>();var bound=pets(stack);
            if(bound.isEmpty())level.getEntities(EntityTypeTest.forClass(LivingEntity.class),player.getBoundingBox().inflate(32),
                    e->owned(player,e)&&(!(e instanceof TamableAnimal tame)||!tame.isOrderedToSit()),candidates,16);
            else for(UUID id:bound){var entity=owner.level().getEntity(id);if(entity instanceof LivingEntity living)candidates.add(living);}
            int recalled=0;
            for(var pet:candidates)if(recall(owner,pet))recalled++;
            if(!bound.isEmpty())player.sendOverlayMessage(Component.literal("Recalled "+recalled+" / "+bound.size()+" companions; others may be too far away or unloaded."));
            player.getCooldowns().addCooldown(stack,20);
        }
        return InteractionResult.SUCCESS;
    }
    private static boolean recall(ServerPlayer owner,LivingEntity pet){return recall(owner,pet,128*128);}
    static boolean recall(ServerPlayer owner,LivingEntity pet,double maximumDistanceSquared){
        ServerLevel level=owner.level();
        if(pet.getBbWidth()>4||pet.getBbHeight()>4)return false;
        if(pet.level()!=level||!pet.isAlive()||!owned(owner,pet)||pet.isPassenger()||pet.isVehicle()||pet.distanceToSqr(owner)>maximumDistanceSquared
                ||pet instanceof Mob mob&&mob.isLeashed()||!Authority.mayChange(level,owner,pet.blockPosition()))return false;
        // Never randomTeleport: its vertical search could enter an unchecked claim or unloaded cell.
        for(int radius=2;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
            for(int dy=0;dy<=2;dy++){
                BlockPos at=owner.blockPosition().offset(dx,dy,dz);
                var box=pet.getBoundingBox().move(at.getX()+.5-pet.getX(),at.getY()-pet.getY(),at.getZ()+.5-pet.getZ());
                if(!level.hasChunksAt(BlockPos.containing(box.minX,box.minY-1,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))continue;
                if(!level.getWorldBorder().isWithinBounds(box)||!level.getBlockState(at.below()).isCollisionShapeFullBlock(level,at.below())||!level.noCollision(pet,box)||level.containsAnyLiquid(box))continue;
                boolean allowed=true;
                for(BlockPos cell:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
                    if(!Authority.mayChange(level,owner,cell)||level.getBlockState(cell).is(net.minecraft.world.level.block.Blocks.FIRE)){allowed=false;break;}
                if(!allowed)continue;
                if(!pet.teleportTo(level,at.getX()+.5,at.getY(),at.getZ()+.5,Set.of(),pet.getYRot(),pet.getXRot(),true))continue;
                pet.stopSleeping();pet.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);pet.fallDistance=0;
                if(pet instanceof Mob mob)mob.getNavigation().stop();
                if(pet instanceof TamableAnimal tame)tame.setOrderedToSit(false);
                if(pet instanceof net.minecraft.world.entity.animal.fox.Fox fox){fox.setSitting(false);((io.github.jimbozoomer.jugcraft.mixin.OwnerFluteFoxAccess)fox).jugcraft$sleeping(false);}
                return true;
            }
        }
        return false;
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return DURATION;}
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.NONE;}
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!(level instanceof ServerLevel server))return;
        var current=playing.get(user);if(current==null||current.stack()!=stack)return;
        int elapsed=DURATION-remaining;Song song=current.song();
        if(elapsed%10==0)level.gameEvent(user,GameEvent.INSTRUMENT_PLAY,user.position());
        if(elapsed%song.tempo()!=0)return;
        int chord=song.notes()[(elapsed/song.tempo())%song.notes().length];
        for(int i=0;chord>0&&i<5;i++,chord/=100){
            int note=Math.clamp(chord%100,1,25);
            level.playSound(null,user.getX(),user.getY(),user.getZ(),SoundEvents.NOTE_BLOCK_FLUTE.value(),SoundSource.PLAYERS,1,(float)Math.pow(2,(note-13)/12.0));
            server.sendParticles(ParticleTypes.NOTE,user.getX(),user.getEyeY()+.1,user.getZ(),0,note/24.0,0,0,1);
        }
    }
    @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
        if(!level.isClientSide()){playing.remove(user);stack.hurtAndBreak(1,user,user.getUsedItemHand().asEquipmentSlot());}
        return true;
    }
    private static Song choose(ItemStack stack,Level level){
        if(songs==null){
            List<Song> parsed=new ArrayList<>();
            for(var value:OwnerBackpacks.source("supps_flute_songs.json").getAsJsonArray("songs")){
                var data=value.getAsJsonObject();List<Integer> expanded=new ArrayList<>();
                for(var valueNote:data.getAsJsonArray("notes")){
                    int n=valueNote.getAsInt();if(n>0)expanded.add(n);else for(int i=0;i<Math.min(1000,Math.max(0,-n-1));i++)expanded.add(0);
                    if(expanded.size()>20000)break;
                }
                if(!expanded.isEmpty())parsed.add(new Song(data.get("name").getAsString(),Math.clamp(data.has("tempo")?data.get("tempo").getAsInt():1,1,1000),Math.clamp(data.has("weight")?data.get("weight").getAsInt():100,0,10000),expanded.stream().mapToInt(Integer::intValue).toArray()));
            }
            songs=List.copyOf(parsed);
        }
        String name=stack.getHoverName().getString().toLowerCase(Locale.ROOT).replace(' ','_');
        for(Song song:songs)if(song.name().equalsIgnoreCase(name))return song;
        int total=songs.stream().mapToInt(Song::weight).sum();
        if(total>0){int pick=level.getRandom().nextInt(total);for(Song song:songs){pick-=song.weight();if(pick<0)return song;}}
        return songs.isEmpty()?new Song("simple",4,1,new int[]{13,15,17,20,17,15}):songs.getFirst();
    }
}

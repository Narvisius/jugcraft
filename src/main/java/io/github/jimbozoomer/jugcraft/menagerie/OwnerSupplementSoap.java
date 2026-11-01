package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Owner soap appearance with source animal washing and Jugcraft's existing cleansing use. */
final class OwnerSupplementSoap extends io.github.jimbozoomer.jugcraft.chemistry.SoapItem {
    OwnerSupplementSoap(Properties p){super(p);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!(player.level() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        if(!Authority.mayChange(level,player,target.blockPosition())||!(target instanceof TamableAnimal tame&&tame.isOwnedBy(player))&&!Authority.mayStrike(level,player,player.getUUID(),target))return InteractionResult.FAIL;
        boolean changed=false;
        if(target instanceof Sheep sheep&&sheep.getColor()!=DyeColor.WHITE){sheep.setColor(DyeColor.WHITE);changed=true;}
        else if(target.getType()==net.minecraft.world.entity.EntityTypes.WITHER_SKELETON&&target instanceof Mob mob){changed=mob.convertTo(net.minecraft.world.entity.EntityTypes.SKELETON,ConversionParams.single(mob,true,true),converted->{})!=null;}
        else if(target.getType()==net.minecraft.world.entity.EntityTypes.SLIME){changed=target.hurtServer(level,level.damageSources().playerAttack(player),2);}
        else if(target instanceof TamableAnimal tame&&tame.isOwnedBy(player)){tame.setOrderedToSit(true);changed=true;}
        if(!changed)return InteractionResult.PASS;wash(level,player,stack,target.position());return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        var other=player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(other.has(DataComponents.DYED_COLOR)){if(level instanceof ServerLevel server){other.remove(DataComponents.DYED_COLOR);wash(server,player,player.getItemInHand(hand),player.position());}return InteractionResult.SUCCESS;}
        return super.use(level,player,hand);
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null||!Menagerie.mayChange(c.getLevel(),c.getPlayer(),c.getClickedPos()))return InteractionResult.FAIL;
        if(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof OwnerSupplementData d&&d.block().function.equals("blackboard")){
            if(d.boardWaxed)return InteractionResult.FAIL;
            if(c.getLevel() instanceof ServerLevel level){java.util.Arrays.fill(d.boardPixels,(byte)0);d.boardGlow=false;d.boardRevision++;d.changed();wash(level,c.getPlayer(),c.getItemInHand(),net.minecraft.world.phys.Vec3.atCenterOf(c.getClickedPos()));}return InteractionResult.SUCCESS;
        }return super.useOn(c);
    }
    private static void wash(ServerLevel level,Player player,ItemStack stack,net.minecraft.world.phys.Vec3 at){stack.consume(1,player);level.sendParticles(ParticleTypes.BUBBLE_POP,at.x,at.y+.6,at.z,12,.35,.4,.35,.04);level.playSound(null,net.minecraft.core.BlockPos.containing(at),OwnerSupplementaries.SOUNDS.get("soap_wash"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
}

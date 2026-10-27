package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Source thermal influence adapted to one paid, bounded native pulse; no unrestricted heat explosion. */
public final class IndustrialPlasmaLauncher extends Item implements Chargeable {
    public static final long COST=1000;
    public IndustrialPlasmaLauncher(Properties p){super(p);}
    @Override public long baseCapacity(){return 40_000;}
    @Override public boolean isBarVisible(ItemStack stack){return false;}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var stack=p.getItemInHand(hand);
        if(!p.isAlive()||p.isSpectator()||p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
        if(Chargeable.energy(stack)<COST){p.sendOverlayMessage(Component.literal("Plasma pulse needs 1,000 JE."));return InteractionResult.FAIL;}
        var start=p.getEyePosition();var look=p.getLookAngle();Vec3 end=start;
        // Never let the ray query unloaded terrain.
        for(int i=1;i<=32;i++){var next=start.add(look.scale(i));if(!p.level().hasChunkAt(BlockPos.containing(next)))break;end=next;}
        if(end.equals(start))return InteractionResult.FAIL;
        var block=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.SOURCE_ONLY,p));
        if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        var entity=ProjectileUtil.getEntityHitResult(p,start,end,new AABB(start,end).inflate(1),e->e instanceof LivingEntity&&e.isAlive()&&e!=p,start.distanceToSqr(end));
        boolean applied=false;
        if(entity!=null&&entity.getEntity() instanceof LivingEntity target){
            end=entity.getLocation();
            if(Authority.mayStrike(p,target)&&ConcordanceEffects.mayHarm(p,target)&&Authority.mayChange(p.level(),p,target.blockPosition())){
                // A permitted combat discharge costs power even when armor or invulnerability absorbs it.
                Chargeable.drain(stack,COST);target.hurtServer(p.level(),p.level().damageSources().indirectMagic(p,p),8);applied=true;
            }
        }else if(block.getType()==HitResult.Type.BLOCK)applied=heat(p,stack,block.getBlockPos());
        if(!applied){p.sendOverlayMessage(Component.literal("No permitted thermal target: aim at a smeltable solid block, water or an enemy."));return InteractionResult.FAIL;}
        p.getCooldowns().addCooldown(stack,20);
        int samples=Math.min(32,Math.max(1,(int)Math.ceil(start.distanceTo(end))));
        for(int i=0;i<=samples;i++){var v=start.lerp(end,i/(double)samples);p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,v.x,v.y,v.z,1,.01,.01,.01,0);}
        p.level().playSound(null,p.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,.5F,1.8F);return InteractionResult.SUCCESS;
    }
    public static boolean heat(ServerPlayer player,ItemStack stack,BlockPos pos){
        var level=player.level();if(!(stack.getItem() instanceof IndustrialPlasmaLauncher)||Chargeable.energy(stack)<COST||!player.isAlive()||player.isSpectator()||!level.hasChunkAt(pos)||!Authority.mayChange(level,player,pos))return false;
        var before=level.getBlockState(pos);if(before.hasBlockEntity()||before.getDestroySpeed(level,pos)<0)return false;
        net.minecraft.world.level.block.state.BlockState replacement;
        if(before.is(Blocks.WATER))replacement=Blocks.AIR.defaultBlockState();
        else {
            if(!before.getFluidState().isEmpty()||before.getBlock().asItem()==Items.AIR)return false;
            var input=new SingleRecipeInput(new ItemStack(before.getBlock()));
            var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
            if(recipe.isEmpty())return false;var output=recipe.get().value().assemble(input);
            if(output.getCount()!=1||!(output.getItem() instanceof BlockItem item))return false;
            replacement=item.getBlock().defaultBlockState();
            if(replacement.hasBlockEntity()||!replacement.getFluidState().isEmpty()||!replacement.isCollisionShapeFullBlock(level,pos)||replacement.equals(before))return false;
        }
        if(!level.setBlock(pos,replacement,Block.UPDATE_ALL))return false;
        Chargeable.drain(stack,COST);return true;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
        out.accept(PoweredToolItem.energyLine(s));out.accept(Component.literal("Thermal pulse · 1,000 JE · 32 blocks · one second cooldown."));
        out.accept(Component.literal("Smelts one block in place, evaporates water, or hits a permitted creature for 8 damage."));
        out.accept(Component.literal("No fire spread, containers, ore-to-item duplication or area heat explosion."));
    }
}

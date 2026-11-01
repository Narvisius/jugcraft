package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import io.github.jimbozoomer.jugcraft.mixin.AlchemyExperienceAccess;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Original dropped-item formation and item/XP attraction, bounded and ownership aware. */
public final class ArcaneBlackHole extends BlockEntity {
    private UUID owner;
    private int experience;
    private static final String EJECTED="jugcraft_arcane_hole_output";
    private static final TagKey<EntityType<?>> AFFECTED=TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_black_hole_affected"));
    private static final TagKey<Item> IMMUNE=TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_black_hole_unaffected"));
    public ArcaneBlackHole(BlockPos p,BlockState s){super(ForbiddenArcane.BLACK_HOLE,p,s);}
    public static void dropped(ItemEntity matter){
        if(!(matter.level() instanceof ServerLevel l)||matter.isRemoved()||matter.tickCount%10!=0||!matter.getItem().is(ForbiddenArcane.item("dark_matter"))||!(matter.getOwner() instanceof ServerPlayer p))return;
        var at=matter.blockPosition();if(!l.isEmptyBlock(at)||!Authority.mayChange(l,p,at)||!allowed(matter,p))return;
        List<ItemEntity> nearby=new ArrayList<>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(at).inflate(.5),e->e.isAlive()&&e.getItem().is(ForbiddenArcane.item("corrupti_dust"))&&e.getOwner()==p&&allowed(e,p),nearby,8);
        if(nearby.isEmpty())return;
        if(!l.setBlock(at,ForbiddenArcane.block("black_hole").defaultBlockState(),3))return;
        if(l.getBlockEntity(at) instanceof ArcaneBlackHole hole){hole.owner=p.getUUID();hole.setChanged();matter.getItem().shrink(1);nearby.getFirst().getItem().shrink(1);if(matter.getItem().isEmpty())matter.discard();if(nearby.getFirst().getItem().isEmpty())nearby.getFirst().discard();}
    }
    private static boolean allowed(Entity e,ServerPlayer p){
        if(!Authority.mayChange(p.level(),p,e.blockPosition()))return false;
        if(e instanceof ItemEntity item){UUID reserved=((AlchemicalItemAccess)item).jugcraft$pickupTarget();if(reserved!=null&&!reserved.equals(p.getUUID()))return false;if(item.getOwner()!=null&&item.getOwner()!=p)return false;}
        if(e instanceof Projectile projectile&&projectile.getOwner()!=null&&projectile.getOwner()!=p)return false;
        return true;
    }
    public static void tick(Level raw,BlockPos at,BlockState state,ArcaneBlackHole hole){
        if(!(raw instanceof ServerLevel l))return;var p=Authority.present(l,hole.owner);if(p==null||!Authority.mayChange(l,p,at))return;
        List<Entity> targets=new ArrayList<>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),new AABB(at).inflate(5),e->e.isAlive()&&e.getType().builtInRegistryHolder().is(AFFECTED)&&!e.entityTags().contains(EJECTED)&&(!(e instanceof ItemEntity item)||!item.getItem().is(IMMUNE))&&allowed(e,p),targets,64);
        var center=net.minecraft.world.phys.Vec3.atCenterOf(at);
        for(Entity target:targets){var delta=center.subtract(target.position());double distance=delta.length();if(distance>5)continue;
            if(distance<.6){
                if(target instanceof ExperienceOrb xp){int value=xp.getValue();if(value>0&&hole.experience<=1000000-value){hole.experience+=value;var merged=(AlchemyExperienceAccess)xp;if(merged.jugcraft$count()>1)merged.jugcraft$count(merged.jugcraft$count()-1);else xp.discard();hole.setChanged();}}
                else if(target instanceof ItemEntity item){item.hurtServer(l,l.damageSources().magic(),4);}
                else if(target instanceof Projectile)target.discard();
            }else{target.setDeltaMovement(target.getDeltaMovement().add(delta.add(0,1.25,0).scale(distance<=3?.035:.02)));target.needsSync=true;}
        }
        // Same value as redeeming the orb: the source's 60 -> 91 conversion was an infinite XP loop.
        if(hole.experience>=91){var output=new ItemEntity(l,at.getX()+.5,at.getY()+1.1,at.getZ()+.5,new ItemStack(ForbiddenArcane.item("xpetrified_orb")));output.addTag(EJECTED);output.setThrower(p);output.setDefaultPickUpDelay();output.setDeltaMovement(p.distanceToSqr(center)<=36?p.position().subtract(output.position()).add(0,1.25,0).scale(.09):new Vec3((l.getRandom().nextDouble()-.5)*.15,.3,(l.getRandom().nextDouble()-.5)*.15));if(l.addFreshEntity(output)){hole.experience-=91;hole.setChanged();}}
    }
    @Override public void preRemoveSideEffects(BlockPos at,BlockState replacement){
        // The block entity is already detached by affectNeighborsAfterRemoval.
        // Pay the conserved remainder here, once, while its state still exists.
        if(level instanceof ServerLevel server&&experience>0){int value=experience;experience=0;setChanged();ExperienceOrb.award(server,net.minecraft.world.phys.Vec3.atCenterOf(at),value);}
        super.preRemoveSideEffects(at,replacement);
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("experience",experience);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);experience=Math.clamp(in.getIntOr("experience",0),0,1000000);}
    public static final class HoleBlock extends BaseEntityBlock {
        public HoleBlock(Properties p){super(p.noOcclusion().noLootTable());}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(5,5,5,11,11,11);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArcaneBlackHole(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()?null:createTickerHelper(type,ForbiddenArcane.BLACK_HOLE,ArcaneBlackHole::tick);}
    }
}

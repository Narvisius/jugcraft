package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Owner-bound redstone machinery. No terrain writes are delegated to an unchecked explosion. */
public final class CavernControl extends BlockEntity {
    static final Set<String> CAVE=Set.of("azure_magnet","scarlet_magnet","magnetic_activator","magnetic_levitation_rail","nuclear_bomb","nuclear_siren","siren_light","tesla_bulb","drain","copper_valve");
    static final Set<String> BEAST=Set.of("void_worm_beak","sculk_boomer","end_pirate_anchor_winch","end_pirate_ship_wheel");
    public static BlockEntityType<CavernControl> TYPE;
    private UUID owner;
    private int fuse=-1,charge,cooldown;
    private boolean lastPower;
    private final Set<Integer> deployed=new TreeSet<>();
    CavernControl(BlockPos p,BlockState s){super(TYPE,p,s);}
    static void register(){
        var blocks=new ArrayList<Block>();for(var name:CAVE)blocks.add(Menagerie.BLOCKS.get("cavern_"+name));for(var name:BEAST)blocks.add(Menagerie.BLOCKS.get("beast_"+name));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("cavern_control"),FabricBlockEntityTypeBuilder.create(CavernControl::new,blocks.toArray(Block[]::new)).build());
    }
    void owner(Player p){owner=p.getUUID();setChanged();}
    boolean permitted(ServerPlayer p){return (owner==null||owner.equals(p.getUUID())||p.isCreative())&&Authority.mayChange(p.level(),p,worldPosition);}
    public boolean arm(ServerPlayer p){if(!permitted(p)||!name().equals("nuclear_bomb")||fuse>=0)return false;owner(p);fuse=200;setChanged();p.sendOverlayMessage(Component.literal("Charge armed: 10 seconds. Crouch and use with an empty hand to disarm."));return true;}
    boolean loadChain(Player p,ItemStack stack){if(!name().equals("end_pirate_anchor_winch")||charge>=12||!stack.is(Items.IRON_CHAIN))return false;int n=Math.min(12-charge,stack.getCount());stack.consume(n,p);charge+=n;setChanged();return true;}
    boolean disarm(ServerPlayer p){if(!permitted(p)||fuse<0)return false;fuse=-1;setChanged();return true;}
    String name(){return ((HouseholdBlock)getBlockState().getBlock()).name;}
    public static void tick(ServerLevel l,BlockPos pos,BlockState state,CavernControl b){
        if(l.getGameTime()%5!=0)return;
        var actor=Authority.answering(l,b.owner);if(actor==null||!Authority.mayChange(l,actor,pos))return;
        if(b.cooldown>0)b.cooldown-=5;
        String name=b.name();boolean power=l.hasNeighborSignal(pos);
        if(name.equals("nuclear_bomb")){
            if(power&&!b.lastPower&&b.fuse<0)b.arm(actor);b.lastPower=power;
            if(b.fuse>=0){b.fuse-=5;b.setChanged();l.sendParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,2,.2,.1,.2,.01);if(b.fuse<=0)b.blast(l,actor);}
            return;
        }
        if(!power){if(b.lastPower&&name.equals("end_pirate_anchor_winch"))b.retract(l,actor);b.lastPower=false;return;}
        Direction face=HouseholdBlock.facing(state);Vec3 center=Vec3.atCenterOf(pos),end=center.add(new Vec3(face.getStepX(),face.getStepY(),face.getStepZ()).scale(8));
        if(name.endsWith("magnet")||name.equals("magnetic_activator")){
            for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(center,end).inflate(2),Entity::isAlive).stream().limit(24).toList()){
                if(!Authority.mayChange(l,actor,e.blockPosition()))continue;
                var delta=(name.equals("scarlet_magnet")?e.position().subtract(center):center.subtract(e.position())).normalize().scale(.12);e.push(delta.x,delta.y,delta.z);
            }
        }else if(name.equals("magnetic_levitation_rail")){
            for(var e:l.getEntitiesOfClass(Entity.class,new AABB(pos).inflate(0,2,0),e->e instanceof ItemEntity||e instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart).stream().limit(8).toList())if(Authority.mayChange(l,actor,e.blockPosition())){var delta=e.getDeltaMovement();e.setDeltaMovement(delta.x,Math.min(.1,delta.y+.05),delta.z);}
        }else if(Set.of("void_worm_beak","sculk_boomer","tesla_bulb").contains(name)&&b.cooldown<=0){
            double radius=name.equals("sculk_boomer")?6:2;
            for(var mob:l.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(radius),m->m instanceof Enemy&&m.isAlive()).stream().limit(8).toList())if(TwoHanded.allowed(actor,l,mob)&&actor.hasLineOfSight(mob))mob.hurtServer(l,actor.damageSources().playerAttack(actor),name.equals("sculk_boomer")?5:3);
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,8,.4,.4,.4,.03);b.cooldown=40;
        }else if(name.equals("end_pirate_anchor_winch")&&!b.lastPower){
            for(int depth=1;depth<=12&&b.charge>0;depth++){var at=pos.below(depth);if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at))break;if(b.deployed.contains(depth)&&l.getBlockState(at).is(Blocks.IRON_CHAIN))continue;if(!l.getBlockState(at).canBeReplaced())break;if(l.setBlock(at,Blocks.IRON_CHAIN.defaultBlockState(),Block.UPDATE_ALL)){b.charge--;b.deployed.add(depth);b.setChanged();}}
        }else if(name.equals("end_pirate_ship_wheel")){
            // A native dock control: only moves the operator's occupied boat, never somebody else's craft.
            if(actor.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat boat&&boat.distanceToSqr(center)<144&&Authority.mayChange(l,actor,boat.blockPosition())){boat.setYRot(face.toYRot());boat.push(face.getStepX()*.08,0,face.getStepZ()*.08);}
        }else if(name.equals("nuclear_siren")||name.equals("siren_light")){
            if(b.cooldown<=0){l.playSound(null,pos,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),net.minecraft.sounds.SoundSource.BLOCKS,.6F,.65F);b.cooldown=80;}
        }
        b.lastPower=power;
    }
    private void retract(ServerLevel l,ServerPlayer actor){for(var iterator=deployed.iterator();iterator.hasNext();){int depth=iterator.next();var at=worldPosition.below(depth);if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at))continue;if(l.getBlockState(at).is(Blocks.IRON_CHAIN)&&charge<12){if(!l.removeBlock(at,false))continue;charge++;}else if(l.getBlockState(at).is(Blocks.IRON_CHAIN))continue;iterator.remove();setChanged();}}
    @Override public void preRemoveSideEffects(BlockPos p,BlockState replacement){if(level instanceof ServerLevel l&&charge>0){Block.popResource(l,p,new ItemStack(Items.IRON_CHAIN,charge));charge=0;}super.preRemoveSideEffects(p,replacement);}
    private void blast(ServerLevel l,ServerPlayer actor){
        if(isRemoved())return;
        // One bounded 9-block-radius blast. Containers and protected blocks remain intact.
        var pos=worldPosition;fuse=-1;l.removeBlock(pos,false);
        var center=Vec3.atCenterOf(pos);
        for(var mob:l.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(12),LivingEntity::isAlive).stream().limit(64).toList())
            if(TwoHanded.allowed(actor,l,mob)&&l.clip(new net.minecraft.world.level.ClipContext(center,mob.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,actor)).getType()==HitResult.Type.MISS)
                mob.hurtServer(l,actor.damageSources().playerAttack(actor),(float)Math.max(0,32-mob.distanceToSqr(center)/5));
        int drops=0;var tool=new ItemStack(Items.DIAMOND_PICKAXE);
        for(var at:BlockPos.betweenClosed(pos.offset(-9,-9,-9),pos.offset(9,9,9))){
            if(at.distSqr(pos)>81||!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at)||l.getBlockEntity(at)!=null)continue;
            var s=l.getBlockState(at);float hardness=s.getDestroySpeed(l,at);if(s.isAir()||hardness<0||hardness>30||!s.getFluidState().isEmpty())continue;
            var loot=drops<128?Block.getDrops(s,l,at,null,actor,tool):List.<ItemStack>of();if(!l.removeBlock(at,false))continue;
            for(var stack:loot){if(drops++>=128)break;Block.popResource(l,at,stack);}
        }
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,0,0,0,0);l.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.BLOCKS,4,.6F);
    }
    @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);if(owner!=null)o.putString("Owner",owner.toString());o.putInt("Fuse",fuse);o.putInt("Charge",charge);o.putBoolean("Power",lastPower);o.putString("Deployed",deployed.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));}
    @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);try{owner=UUID.fromString(i.getStringOr("Owner",""));}catch(IllegalArgumentException ignored){owner=null;}fuse=Math.clamp(i.getIntOr("Fuse",-1),-1,200);charge=Math.clamp(i.getIntOr("Charge",0),0,12);lastPower=i.getBooleanOr("Power",false);deployed.clear();for(String value:i.getStringOr("Deployed","").split(","))try{int depth=Integer.parseInt(value);if(depth>0&&depth<=12)deployed.add(depth);}catch(NumberFormatException ignored){}}
}

package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Advanced original tool functions, with bounded jobs using actual player mining and claims. */
public final class BotanicalAdvancedTools {
    static final Set<String> ITEMS=Set.of("elementium_axe","elementium_sword","terra_pick","terra_axe","terra_sword","glass_pickaxe","star_sword","thunder_sword","temperance_stone");
    public static DataComponentType<Boolean> ACTIVE;
    private static final int[] THRESHOLDS={0,10000,1000000,10000000,100000000,1000000000};
    private static final ToolMaterial TERRA=new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,2300,9,4,26,TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_terrasteel_repair")));
    private static final ToolMaterial ELEMENTIUM=new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,720,6.2F,2,20,TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_elementium_repair")));
    private static final ToolMaterial GLASS=new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL,125,4.8F,0,10,TagKey.create(Registries.ITEM,net.minecraft.resources.Identifier.fromNamespaceAndPath("c","glass_blocks")));
    private static final Map<ServerPlayer,Job> JOBS=new WeakHashMap<>();private static final Set<UUID> BREAKING=new HashSet<>();
    private record Step(BlockPos pos,int range){}
    private static final class Job {
        final ServerLevel level;final ItemStack tool;final BlockPos origin;final boolean tree;final ArrayDeque<Step> queue=new ArrayDeque<>();final Set<BlockPos> seen=new HashSet<>();int ticks;
        Job(ServerPlayer p,BlockPos origin,boolean tree){this.level=p.level();this.tool=p.getMainHandItem();this.origin=origin.immutable();this.tree=tree;}
        void offer(BlockPos p,int range){if(range>=0&&seen.size()<4096&&seen.add(p.immutable()))queue.add(new Step(p.immutable(),range));}
    }
    static Item create(String name,Item.Properties properties){
        if(name.equals("temperance_stone"))return new Temperance(properties.stacksTo(1));
        var material=name.startsWith("elementium_")?ELEMENTIUM:name.equals("glass_pickaxe")?GLASS:TERRA;
        var p=name.endsWith("axe")&&!name.endsWith("pickaxe")?properties.axe(material,6,-3.1F):name.equals("terra_pick")||name.equals("glass_pickaxe")?properties.pickaxe(material,1,-2.8F):properties.sword(material,3,-2.4F);
        return new Tool(name,p);
    }
    static void register(){
        ACTIVE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_tool_active"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());BotanicalBolt.register();
        PlayerBlockBreakEvents.AFTER.register((level,actor,pos,state,be)->{if(actor instanceof ServerPlayer p&&!BREAKING.contains(p.getUUID())&&!BotanicalRelics.replicating(p))startMining(p,pos,state);});
        AttackEntityCallback.EVENT.register((player,level,hand,target,hit)->{if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND)fire(p,p.getMainHandItem());return InteractionResult.PASS;});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:server.getPlayerList().getPlayers()){
                if(!p.isAlive()||p.isSpectator())continue;
                for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var stack=p.getInventory().getItem(slot);if(stack.is(Jugtania.item("terra_pick"))&&active(stack)){int rank=rank(stack);if(rank<=0)setActive(stack,false);else if(!p.isSwinging())BotanicalManaItems.stored(stack,Math.max(0,BotanicalManaItems.stored(stack)-rank));}}
                var job=JOBS.get(p);if(job!=null)step(p,job);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{JOBS.clear();BREAKING.clear();});
        ServerLivingEntityEvents.AFTER_DEATH.register(BotanicalAdvancedTools::behead);
    }
    public static boolean active(ItemStack stack){return stack.getOrDefault(ACTIVE,false);}
    private static void setActive(ItemStack stack,boolean active){stack.set(ACTIVE,active);stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(active),List.of(),List.of()));}
    public static int rank(ItemStack stack){int mana=BotanicalManaItems.stored(stack);for(int i=THRESHOLDS.length-1;i>0;i--)if(mana>=THRESHOLDS[i])return i;return 0;}
    static boolean temperance(Player player){for(int i=0;i<player.getInventory().getContainerSize();i++){var s=player.getInventory().getItem(i);if(s.is(Jugtania.item("temperance_stone"))&&active(s))return true;}return false;}
    private static void startMining(ServerPlayer p,BlockPos origin,BlockState original){
        var stack=p.getMainHandItem();boolean tree=stack.is(Jugtania.item("terra_axe"));
        if(tree){if(p.isShiftKeyDown()||temperance(p)||!original.is(BlockTags.LOGS))return;}
        else if(!stack.is(Jugtania.item("terra_pick"))||!active(stack)||rank(stack)<1||!original.is(BlockTags.MINEABLE_WITH_PICKAXE))return;
        var job=new Job(p,origin,tree);if(tree){job.seen.add(origin.immutable());neighbors(job,origin,32);}
        else{
            int rank=rank(stack);if(temperance(p))rank=Math.min(rank,2);int radius=rank-1+(BotanicalRelics.worn(p,"thor_ring")&&!temperance(p)?1:0);Direction.Axis axis=Direction.getApproximateNearest(p.getLookAngle()).getAxis();
            for(int x=axis==Direction.Axis.X?0:-radius;x<=(axis==Direction.Axis.X?0:radius);x++)for(int y=axis==Direction.Axis.Y?0:-1;y<=(axis==Direction.Axis.Y?0:Math.max(1,radius)*2-1);y++)for(int z=axis==Direction.Axis.Z?0:-radius;z<=(axis==Direction.Axis.Z?0:radius);z++)if(x!=0||y!=0||z!=0)job.offer(origin.offset(x,y,z),0);
        }if(!job.queue.isEmpty())JOBS.put(p,job);
    }
    private static void neighbors(Job job,BlockPos pos,int range){if(range<=0)return;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(x!=0||y!=0||z!=0)job.offer(pos.offset(x,y,z),range-1);}
    private static void step(ServerPlayer p,Job job){
        if(p.isShiftKeyDown()||job.tree&&temperance(p)||!job.tree&&(!active(job.tool)||rank(job.tool)<1)||!p.isAlive()||p.isSpectator()||p.level()!=job.level||p.getMainHandItem()!=job.tool||job.tool.isEmpty()||++job.ticks>400||p.distanceToSqr(Vec3.atCenterOf(job.origin))>4096){JOBS.remove(p);return;}
        if(!BREAKING.add(p.getUUID()))return;
        try{int processed=0;while(!job.queue.isEmpty()&&processed++<10){
            var step=job.queue.removeFirst();var pos=step.pos;if(!job.level.hasChunkAt(pos)||!Authority.mayChange(job.level,p,pos)||job.level.getBlockEntity(pos)!=null)continue;
            var state=job.level.getBlockState(pos);boolean leaves=state.is(BlockTags.LEAVES);
            boolean eligible=job.tree?(state.is(BlockTags.LOGS)||leaves&&!state.getValue(LeavesBlock.PERSISTENT)):state.is(BlockTags.MINEABLE_WITH_PICKAXE)&&(!state.requiresCorrectToolForDrops()||job.tool.isCorrectToolForDrops(state));
            if(!eligible||state.getDestroySpeed(job.level,pos)<0)continue;
            if(p.gameMode.destroyBlock(pos)&&job.tree)neighbors(job,pos,leaves?Math.min(3,step.range):step.range);
            if(job.tool.isEmpty()||p.getMainHandItem()!=job.tool)break;
        }}finally{BREAKING.remove(p.getUUID());}if(job.queue.isEmpty()||job.tool.isEmpty())JOBS.remove(p);
    }
    /** Charged attack or right-click. Cooldowns are server-owned and never tied to research. */
    public static void fire(ServerPlayer p,ItemStack stack){
        boolean star=stack.is(Jugtania.item("star_sword"));if(!star&&!stack.is(Jugtania.item("terra_sword"))||p.isSpectator()||p.getAttackStrengthScale(0)<1||p.getCooldowns().isOnCooldown(stack))return;
        boolean spawned;
        if(star){
            Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(48));for(int i=0;i<=48;i+=4)if(!p.level().hasChunkAt(BlockPos.containing(from.lerp(to,i/48D))))return;
            var hit=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()!=HitResult.Type.BLOCK||!Authority.mayChange(p.level(),p,hit.getBlockPos()))return;
            Vec3 offset=new Vec3((p.getRandom().nextDouble()-.5)*9,24,(p.getRandom().nextDouble()-.5)*9);Vec3 start=hit.getLocation().add(offset),motion=offset.scale(-1).normalize().scale(1.5);
            spawned=BotanicalBolt.launch(p,start,motion,true);
            long time=Math.floorMod(p.level().getDefaultClockTime(),24000);if(spawned&&!p.level().isRaining()&&Math.abs(time-18000)<1800&&p.getRandom().nextFloat()<.125F)BotanicalBolt.launch(p,start,motion.add(p.getRandom().nextDouble()-.5,0,p.getRandom().nextDouble()-.5),true);
        }else spawned=BotanicalBolt.launch(p,p.getEyePosition(),p.getLookAngle().scale(3.5),false);
        if(spawned){stack.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);p.getCooldowns().addCooldown(stack,12);p.level().playSound(null,p.blockPosition(),BotanicalSounds.get(star?"starcaller":"terra_blade"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
    }
    private static boolean enemy(ServerPlayer p,LivingEntity target){return target!=p&&target instanceof Enemy&&target.isAlive()&&Authority.mayStrike(p,target)&&Authority.mayChange(p.level(),p,target.blockPosition());}
    private static void thunder(ServerPlayer p,LivingEntity initial){
        var hit=new HashSet<UUID>();hit.add(initial.getUUID());LivingEntity previous=initial;int hops=p.level().isThundering()?10:4;
        for(int i=0;i<hops;i++){
            var source=previous;var targets=p.level().getEntitiesOfClass(LivingEntity.class,source.getBoundingBox().inflate(8),e->!hit.contains(e.getUUID())&&enemy(p,e)&&source.hasLineOfSight(e));if(targets.isEmpty())break;var target=targets.get(p.getRandom().nextInt(targets.size()));
            Vec3 a=source.getBoundingBox().getCenter(),b=target.getBoundingBox().getCenter();for(int part=0;part<16;part++){var v=a.lerp(b,part/15D);p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,v.x,v.y,v.z,1,0,0,0,0);}
            target.hurtServer(p.level(),p.damageSources().playerAttack(p),hops+1-i);hit.add(target.getUUID());previous=target;
        }
    }
    private static void behead(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer p)||!p.getMainHandItem().is(Jugtania.item("elementium_axe"))||!p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS)||victim.entityTags().contains(BotanicalLoonium.MARKER))return;
        var type=victim.getType();Item head;double base=.0769,increment=.0769;
        if(type==net.minecraft.world.entity.EntityTypes.WITHER_SKELETON){head=Items.WITHER_SKELETON_SKULL;base=.1154;increment=.0385;}
        else if(type.builtInRegistryHolder().is(EntityTypeTags.SKELETONS)){head=Items.SKELETON_SKULL;base=.1154;increment=.0385;}
        else if(Set.of(net.minecraft.world.entity.EntityTypes.ZOMBIE,net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER,net.minecraft.world.entity.EntityTypes.HUSK,net.minecraft.world.entity.EntityTypes.DROWNED).contains(type))head=Items.ZOMBIE_HEAD;
        else if(type==net.minecraft.world.entity.EntityTypes.PIGLIN||type==net.minecraft.world.entity.EntityTypes.PIGLIN_BRUTE)head=Items.PIGLIN_HEAD;
        else if(type==net.minecraft.world.entity.EntityTypes.CREEPER)head=Items.CREEPER_HEAD;
        else return;
        int looting=p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.LOOTING).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,p.getMainHandItem())).orElse(0);
        if(p.getRandom().nextDouble()<Math.min(1,base+increment*looting))victim.spawnAtLocation(p.level(),new ItemStack(head));
    }
    private static final class Tool extends Item implements BotanicalEquipment.ManaGear {
        private final String name;Tool(String name,Properties p){super(p);this.name=name;}
        @Override public int manaCost(){return name.equals("glass_pickaxe")?160:name.equals("star_sword")?120:name.startsWith("elementium_")?60:100;}
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
            if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var stack=p.getItemInHand(hand);
            if(name.equals("terra_pick")&&p.isShiftKeyDown()){setActive(stack,!active(stack)&&rank(stack)>0);p.sendOverlayMessage(Component.literal("Terra Shatterer: "+List.of("D","C","B","A","S","SS").get(rank(stack))+" · "+(active(stack)?"area mining on":"single block")));return InteractionResult.SUCCESS;}
            if(name.equals("terra_sword")||name.equals("star_sword")){if(hand==InteractionHand.MAIN_HAND)fire(p,stack);return InteractionResult.SUCCESS;}return super.use(level,user,hand);
        }
        @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer() instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;return super.useOn(c);}
        @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){super.hurtEnemy(stack,target,attacker);if(attacker instanceof ServerPlayer p&&name.equals("thunder_sword")&&Authority.mayStrike(p,target))thunder(p,target);}
    }
    private static final class Temperance extends Item {
        Temperance(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer p){var stack=p.getItemInHand(hand);setActive(stack,!active(stack));p.sendOverlayMessage(Component.literal(active(stack)?"Temperance: tree felling disabled; shatterer capped at rank B":"Temperance disabled"));}return InteractionResult.SUCCESS;}
    }
}

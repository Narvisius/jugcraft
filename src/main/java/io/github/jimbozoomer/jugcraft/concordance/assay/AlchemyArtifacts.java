package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.concordance.ember.Ember;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;

/** Original, bounded artifact gameplay. All payment and world actions remain on the server. */
public final class AlchemyArtifacts {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();
    public static final String[] IDS = {"swiftwolf_rending_gale", "harvest_goddess_band", "ignition_ring", "zero_ring", "arcana_ring",
            "archangel_smite", "body_stone", "soul_stone", "life_stone", "watch_of_flowing_time", "divining_rod_1", "divining_rod_2", "divining_rod_3",
            "destruction_catalyst", "catalytic_lens", "hyperkinetic_lens", "mercurial_eye"};
    public static final String[] SOUND_NAMES = {"chargetick","gust","kinesis","launch","nova","pecharge","pedestruct","peheal","pepower","petransmute","peuncharge","pewatermagic","pewindmagic","philball","tock"};
    public static DataComponentType<Integer> MODE;
    public static final List<DataComponentType<Unit>> MODE_FLAGS = new ArrayList<>();
    private static final Map<ServerPlayer, Long> FLIGHT = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> EXTERNAL_FLIGHT = new WeakHashMap<>();
    /** Short paid ritual leases share the existing ability owner, so providers do not revoke one another. */
    public static void grantRitualFlight(ServerPlayer p,int ticks){EXTERNAL_FLIGHT.put(p,p.level().getGameTime()+Math.clamp(ticks,1,100));}
    private static AttachmentType<Boolean> FLIGHT_LEASE;
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.parse("c:ores"));
    private AlchemyArtifacts() {}
    public static void register() {
        FLIGHT_LEASE=AttachmentRegistry.<Boolean>builder().persistent(Saved.versioned("alchemy_flight_lease",Codec.BOOL)).buildAndRegister(Jugcraft.id("alchemy_flight_lease"));
        MODE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("artifact_mode"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(0,3)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        for(int i=1;i<=3;i++) MODE_FLAGS.add(Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("artifact_mode_"+i),
                DataComponentType.<Unit>builder().persistent(MapCodec.unitCodec(Unit.INSTANCE)).networkSynchronized(Unit.STREAM_CODEC).build()));
        for(String name:SOUND_NAMES) SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id("alchemy_"+name),SoundEvent.createVariableRangeEvent(Jugcraft.id("alchemy_"+name))));
        for(String id:IDS) {
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));
            var properties=new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.RARE).component(MODE,0);
            if(miner(id)) properties.pickaxe(DarkMatter.MATERIAL,0,-2.8F);
            ITEMS.put(id,Registry.register(BuiltInRegistries.ITEM,key,new Artifact(properties,id)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.values().forEach(e::accept));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()) {
            if(p.tickCount%20==0)pulse(p);
            if(FLIGHT.containsKey(p)&&(!AlchemicalGear.ready(p)||!flightEquipped(p)||p.level().getGameTime()>FLIGHT.get(p)))releaseFlight(p);
        }});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->releaseFlight(handler.getPlayer()));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->releaseFlight(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var p:new ArrayList<>(FLIGHT.keySet()))releaseFlight(p);});
    }
    public static boolean pay(ServerPlayer p,int cost) {
        return AlchemyPower.pay(p,cost);
    }
    private static void refund(ServerPlayer p,int cost) {AlchemyPower.refund(p,cost);}
    public static void sound(ServerPlayer p,String name) {p.level().playSound(null,p.getX(),p.getY(),p.getZ(),SOUNDS.get(name),SoundSource.PLAYERS,.5F,1F);}
    public static int mode(ItemStack stack) {return Math.floorMod(stack.getOrDefault(MODE,0),stack.is(ITEMS.get("swiftwolf_rending_gale"))?3:4);}
    public static void setMode(ItemStack stack,int mode) {
        mode=Math.floorMod(mode,4);stack.set(MODE,mode);for(var flag:MODE_FLAGS)stack.remove(flag);
        if(mode>0&&mode<=3)stack.set(MODE_FLAGS.get(mode-1),Unit.INSTANCE);
    }
    private static boolean miner(String id) {return id.equals("destruction_catalyst")||id.equals("catalytic_lens")||id.equals("hyperkinetic_lens");}
    private static boolean passive(String id) {return !id.startsWith("divining_")&&!miner(id)&&!id.equals("mercurial_eye");}
    private static ItemStack equipped(ServerPlayer p,String id) {
        Item wanted=ITEMS.get(id); if(wanted==null)return ItemStack.EMPTY;
        if(p.getMainHandItem().is(wanted)&&p.getMainHandItem().has(AlchemicalGear.ACTIVE))return p.getMainHandItem();
        if(p.getOffhandItem().is(wanted)&&p.getOffhandItem().has(AlchemicalGear.ACTIVE))return p.getOffhandItem();
        var slot=Wayfaring.worn(p,s->s.is(wanted)&&s.has(AlchemicalGear.ACTIVE));return slot==null?ItemStack.EMPTY:slot.get();
    }
    private static boolean on(ServerPlayer p,String id){return !equipped(p,id).isEmpty();}
    private static boolean arcana(ServerPlayer p,int mode){var stack=equipped(p,"arcana_ring");return !stack.isEmpty()&&mode(stack)==mode;}
    private static boolean flightEquipped(ServerPlayer p){var s=equipped(p,"swiftwolf_rending_gale");return !s.isEmpty()&&mode(s)!=1||arcana(p,3)||GemAbilities.flight(p);}
    public static void releaseFlight(ServerPlayer p) {
        boolean ours=FLIGHT.remove(p)!=null||Boolean.TRUE.equals(p.getAttached(FLIGHT_LEASE));
        p.removeAttached(FLIGHT_LEASE);
        if(!ours||p.isCreative()||p.isSpectator())return;
        p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();
        if(p.isAlive())p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100,0,false,true));
    }
    /** Payment renews a short lease; removal, death, disconnect, disable and exhaustion revoke only our lease. */
    public static void pulse(ServerPlayer p) {
        if(!AlchemicalGear.ready(p)){releaseFlight(p);return;}
        boolean external=EXTERNAL_FLIGHT.getOrDefault(p,0L)>p.level().getGameTime();
        if(!external)EXTERNAL_FLIGHT.remove(p);
        if((flightEquipped(p)||external)&&!p.isCreative()&&!p.isSpectator()&&(!p.getAbilities().mayfly||FLIGHT.containsKey(p))) {
            if(external||pay(p,16)) {
                if(!p.getAbilities().mayfly||FLIGHT.containsKey(p)) {
                    FLIGHT.put(p,p.level().getGameTime()+25);p.setAttached(FLIGHT_LEASE,true);p.getAbilities().mayfly=true;p.onUpdateAbilities();
                }
            }else releaseFlight(p);
        }else releaseFlight(p);
        var wind=equipped(p,"swiftwolf_rending_gale");
        if(!wind.isEmpty()&&mode(wind)!=0||arcana(p,3))affectMonsters(p,"wind",5,8);
        if(on(p,"harvest_goddess_band")||arcana(p,2))grow(p,p.blockPosition(),3,4);
        if(on(p,"ignition_ring")||arcana(p,1)) {
            if(p.isOnFire()||p.isInLava())fireWard(p);
            affectMonsters(p,"fire",3,16);
        }
        if(on(p,"zero_ring")||arcana(p,0))affectMonsters(p,"ice",5,8);
        boolean life=on(p,"life_stone");
        if((life||on(p,"body_stone"))&&p.getFoodData().needsFood()&&pay(p,64))p.getFoodData().eat(1,0);
        if((life||on(p,"soul_stone"))&&p.getHealth()<p.getMaxHealth()&&pay(p,64))p.heal(1);
        if(on(p,"archangel_smite"))shoot(p,true);
        if(on(p,"watch_of_flowing_time")) {
            affectMonsters(p,"time",6,16);
            var hit=p.pick(6,1,false);
            if(hit instanceof BlockHitResult b&&hit.getType()==HitResult.Type.BLOCK&&pay(p,128)) {
                var be=p.level().getBlockEntity(b.getBlockPos());boolean advanced=false;
                if(be instanceof AlchemicalWorkshopBlockEntity w&&!w.collector()&&p.getUUID().equals(w.owner())&&w.mayUse(p)&&!w.paused()) {
                    advanced=w.bonusPulse(p);
                }else advanced=AlchemyMachines.bonusWork(p,b.getBlockPos());
                if(!advanced)refund(p,128);
            }
        }
    }
    private static void fireWard(ServerPlayer player) {
        var previous=player.getEffect(MobEffects.FIRE_RESISTANCE);
        if(previous!=null&&(previous.getAmplifier()>0||previous.getDuration()>40)||!pay(player,8))return;
        if(!player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,60,0,false,true)))refund(player,8);
    }
    private static List<LivingEntity> monsters(ServerPlayer p,double radius) {
        var list=new ArrayList<LivingEntity>();
        p.level().getEntities(EntityTypeTest.forClass(LivingEntity.class),p.getBoundingBox().inflate(radius),
                e->e instanceof Enemy&&e.isAlive()&&e.distanceToSqr(p)<=radius*radius,list,24);
        return list;
    }
    public static boolean mayAffect(ServerPlayer p,LivingEntity target) {
        return target.isAlive()&&!target.isSpectator()&&!(target instanceof Player v&&v.getAbilities().invulnerable)
                &&!target.is(ConcordanceEffects.IMMUNE)&&ConcordanceEffects.mayHarm(p,target)&&Authority.mayStrike(p,target)
                &&Authority.mayChange(p.level(),p,target.blockPosition())&&p.hasLineOfSight(target);
    }
    public static int affectMonsters(ServerPlayer p,String effect,int radius,int cost) {
        int done=0;
        var cause=Cause.of(p.getUUID(),Cause.Origin.ITEM,"jugcraft:artifact_"+effect,ConcordanceEffects.nextSerial());
        var ledger=new Ledger(new Ledger.Limits(4,8,0));
        for(var target:monsters(p,radius)) {
            if(done==4)break;if(!mayAffect(p,target)||!pay(p,cost))continue;
            boolean applied;
            switch(effect) {
                case "wind"->applied=effect(p,target,cause,ledger,"push",EffectSpec.of(EffectKind.MOVEMENT,Intent.HARMFUL,6,0));
                case "fire"->{
                    applied=effect(p,target,cause,ledger,"smoulder",new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,60,"jugcraft:smoulder",Stacking.STRONGEST,null));
                    applied|=effect(p,target,cause,ledger,"scorch",new EffectSpec(EffectKind.DAMAGE,Intent.HARMFUL,2,0,null,Stacking.STRONGEST,"spell_power:fire"));
                }
                case "ice"->applied=effect(p,target,cause,ledger,"chill",new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,1,60,"minecraft:slowness",Stacking.STRONGEST,null));
                case "time"->applied=effect(p,target,cause,ledger,"slow",new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,2,60,"minecraft:slowness",Stacking.STRONGEST,null));
                default->{refund(p,cost);continue;}
            }
            if(applied)done++;else refund(p,cost);
        }
        return done;
    }
    private static boolean effect(ServerPlayer player,LivingEntity target,Cause cause,Ledger ledger,String step,EffectSpec effect) {
        return ConcordanceEffects.apply(new ConcordanceEffects.Context(player.level(),cause,player,ledger,step,player.position()),effect,target).applied();
    }
    public static int grow(ServerPlayer p,BlockPos centre,int radius,int limit) {
        if(!AlchemicalGear.ready(p))return 0;int done=0;var level=p.level();
        for(BlockPos at:BlockPos.betweenClosed(centre.offset(-radius,-1,-radius),centre.offset(radius,1,radius))) {
            if(done>=limit)break;if(!level.hasChunkAt(at)||!Authority.mayChange(level,p,at))continue;
            var s=level.getBlockState(at);
            if(!(s.getBlock() instanceof CropBlock crop)||crop.isMaxAge(s)||!s.canSurvive(level,at)
                    ||crop instanceof OrganismCropBlock&&!Authority.mayChange(level,p,at.below())||!pay(p,16))continue;
            boolean grew;
            if(crop instanceof OrganismCropBlock organism)grew=organism.hasten(level,at,s,level.getRandom())==OrganismCropBlock.Growth.GREW;
            else {grew=level.setBlock(at,crop.getStateForAge(crop.getAge(s)+1),Block.UPDATE_CLIENTS);}
            if(!grew)refund(p,16);else {done++;Signs.show(level,Vec3.atCenterOf(at).add(0,.1,0),Sign.WORK,null);}
        }return done;
    }
    public static boolean shoot(ServerPlayer p,boolean auto) {
        LivingEntity target=null;
        if(auto)target=monsters(p,12).stream().filter(e->mayAffect(p,e)).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        else {var hit=ProjectileUtil.getHitResultOnViewVector(p,e->e instanceof LivingEntity&&e!=p,24);
            if(hit instanceof EntityHitResult entity&&entity.getEntity() instanceof LivingEntity victim)target=victim;}
        if(target==null||!mayAffect(p,target)||!pay(p,64))return false;
        var cause=Cause.of(p.getUUID(),Cause.Origin.ITEM,"jugcraft:archangel_smite",ConcordanceEffects.nextSerial());
        boolean hurt=effect(p,target,cause,new Ledger(new Ledger.Limits(1,EffectKind.DAMAGE.work,0)),"smite",EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,8,0));
        if(!hurt){refund(p,64);return false;}
        Vec3 from=p.getEyePosition(),to=target.getBoundingBox().getCenter();
        Signs.show(p.level(),to,Sign.FLOW,from);
        sound(p,"launch");return true;
    }
    public static int mine(ServerPlayer p,ItemStack tool,BlockPos origin,Direction face,String id) {
        if(!AlchemicalGear.ready(p)||p.getMainHandItem()!=tool||p.distanceToSqr(Vec3.atCenterOf(origin))>64)return 0;
        int radius=id.equals("hyperkinetic_lens")?2:1,depth=id.equals("catalytic_lens")?3:1,cost=id.equals("destruction_catalyst")?16:32,done=0;
        for(int d=0;d<depth;d++)for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++) {
            var at=plane(origin.relative(face.getOpposite(),d),face,a,b);var level=p.level();
            if(!level.hasChunkAt(at)||!Authority.mayChange(level,p,at))continue;var s=level.getBlockState(at);
            if(s.isAir()||s.hasBlockEntity()||!s.getFluidState().isEmpty()||s.getDestroySpeed(level,at)<0||s.getDestroySpeed(level,at)>20
                    ||!tool.isCorrectToolForDrops(s)||!pay(p,cost))continue;
            if(p.gameMode.destroyBlock(at))done++;else refund(p,cost);
            if(tool.isEmpty()||p.getMainHandItem()!=tool)return done;
        }
        if(done>0)sound(p,"pedestruct");return done;
    }
    private static BlockPos plane(BlockPos origin,Direction normal,int a,int b) {
        return switch(normal.getAxis()){case X->origin.offset(0,a,b);case Y->origin.offset(a,0,b);case Z->origin.offset(a,b,0);};
    }
    public static int build(ServerPlayer p,BlockHitResult hit) {
        var eye=p.getMainHandItem();
        if(eye.is(ITEMS.get("mercurial_eye"))&&eye.has(AlchemyPatterns.TARGET))return MercurialBuilding.build(p,eye,hit);
        ItemStack materials=p.getOffhandItem();
        if(!(materials.getItem() instanceof BlockItem block)||materials.isEmpty()||!Assaying.eligibility(materials).isEmpty()
                ||block.getBlock().defaultBlockState().hasBlockEntity()||!AlchemicalGear.ready(p)||p.distanceToSqr(hit.getLocation())>64)return 0;
        BlockPos origin=hit.getBlockPos().relative(hit.getDirection());int done=0;
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++) {
            var at=plane(origin,hit.getDirection(),a,b);
            if(materials.isEmpty()||!(materials.getItem() instanceof BlockItem))return done;
            if(!p.level().hasChunkAt(at)||!p.level().getBlockState(at).isAir()||!Authority.mayChange(p.level(),p,at))continue;
            var context=BlockPlaceContext.at(new BlockPlaceContext(p,InteractionHand.OFF_HAND,materials,hit),at,hit.getDirection());
            if(!context.getClickedPos().equals(at)||!pay(p,8))continue;
            if(block.place(context).consumesAction())done++;else refund(p,8);
        }
        if(done>0)sound(p,"kinesis");return done;
    }
    public static Map<String,Integer> survey(ServerPlayer p,BlockPos centre,int tier) {
        var found=new TreeMap<String,Integer>();int radius=Math.clamp(tier,1,3);
        if(p.distanceToSqr(Vec3.atCenterOf(centre))>32*32||!pay(p,new int[]{4,16,64}[radius-1]))return found;
        for(BlockPos at:BlockPos.betweenClosed(centre.offset(-radius,-radius,-radius),centre.offset(radius,radius,radius)))
            if(p.level().hasChunkAt(at)&&Authority.mayChange(p.level(),p,at)&&p.level().getBlockState(at).is(ORES))
                found.merge(p.level().getBlockState(at).getBlock().getName().getString(),1,Integer::sum);
        sound(p,"chargetick");return found;
    }
    public static boolean freeze(ServerPlayer p,BlockPos pos) {
        if(!p.level().hasChunkAt(pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>64||!Authority.mayChange(p.level(),p,pos))return false;
        var state=p.level().getBlockState(pos);
        if(!state.is(Blocks.WATER)||!state.getFluidState().isSource()||!pay(p,32))return false;
        if(!p.level().setBlockAndUpdate(pos,Blocks.FROSTED_ICE.defaultBlockState())){refund(p,32);return false;}
        p.level().scheduleTick(pos,Blocks.FROSTED_ICE,60);return true;
    }
    /** Keep a special crop's authored yield/replant stage and honor the bed whose resources it uses. */
    public static boolean harvest(ServerPlayer p,BlockPos pos,ItemStack tool) {
        var level=p.level();
        if(!level.hasChunkAt(pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>64||!Authority.mayChange(level,p,pos))return false;
        var before=level.getBlockState(pos);
        if(!(before.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(before))return false;
        if(crop instanceof OrganismCropBlock&&(!Garden.enabled()||!Authority.mayChange(level,p,pos.below())))return false;
        if(!pay(p,16))return false;
        if(crop instanceof OrganismCropBlock organism) {
            var drops=organism.harvest(level,pos,before);
            if(drops.isEmpty()||level.getBlockState(pos).equals(before)){refund(p,16);return false;}
            for(var drop:drops)Block.popResource(level,pos,drop);
            ConcordanceProgress.record(p,new Evidence.Practiced(Garden.ACTIVITY,organism.organismId()));
        } else {
            ArmItem.reap(level,pos,p,tool);
            if(level.getBlockState(pos).equals(before)){refund(p,16);return false;}
        }
        return true;
    }
    /** Native waterlogged/unlit eligibility, with payment only for a successful state change. */
    public static boolean kindle(ServerPlayer p,BlockPos pos) {
        var level=p.level();
        if(!level.hasChunkAt(pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>64||!Authority.mayChange(level,p,pos))return false;
        var state=level.getBlockState(pos);
        if(!Ember.kindleable(state)||!pay(p,8))return false;
        if(!level.setBlockAndUpdate(pos,state.setValue(BlockStateProperties.LIT,true))){refund(p,8);return false;}
        level.gameEvent(p,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos);
        return true;
    }
    private static final class Artifact extends Item implements TrinketCallback {
        private final String id;
        Artifact(Properties properties,String id){super(properties);this.id=id;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!RateGate.allow(p,"alchemy_artifact",10)||!AlchemicalGear.ready(p))return InteractionResult.FAIL;
            var stack=p.getItemInHand(hand);
            if(!p.isShiftKeyDown()&&(id.equals("zero_ring")||id.equals("arcana_ring")&&mode(stack)==0)) {
                var water=getPlayerPOVHitResult(level,p,ClipContext.Fluid.SOURCE_ONLY);
                if(water.getType()==HitResult.Type.BLOCK&&freeze(p,water.getBlockPos())){sound(p,"pewatermagic");return InteractionResult.SUCCESS;}
            }
            if(id.startsWith("divining_rod_")) {
                int tier=Integer.parseInt(id.substring(id.length()-1));var hit=p.pick(8*tier,1,false);
                if(hit instanceof BlockHitResult b&&hit.getType()==HitResult.Type.BLOCK) {
                    var found=survey(p,b.getBlockPos(),tier);
                    p.sendSystemMessage(Component.translatable("message.jugcraft.artifact.survey",found.isEmpty()?"—":String.join(", ",found.entrySet().stream().limit(6).map(e->e.getKey()+" × "+e.getValue()).toList())));
                }return InteractionResult.SUCCESS;
            }
            if(id.equals("archangel_smite")&&!p.isShiftKeyDown()) {shoot(p,false);return InteractionResult.SUCCESS;}
            if(id.equals("mercurial_eye")){
                p.openMenu(new SimpleMenuProvider((menuId,inv,who)->new AlchemyPatternMenu(menuId,inv,()->p.getItemInHand(hand)==stack,stack),Component.translatable("screen.jugcraft.pattern.build")));
                return InteractionResult.SUCCESS;
            }
            if(!passive(id))return InteractionResult.PASS;
            if(p.isShiftKeyDown()&&(id.equals("arcana_ring")||id.equals("swiftwolf_rending_gale")))setMode(stack,(mode(stack)+1)%(id.equals("arcana_ring")?4:3));
            else {if(stack.has(AlchemicalGear.ACTIVE))stack.remove(AlchemicalGear.ACTIVE);else stack.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);}
            if(!flightEquipped(p))releaseFlight(p);
            sound(p,stack.has(AlchemicalGear.ACTIVE)?"pecharge":"peuncharge");
            p.sendOverlayMessage(Component.translatable("message.jugcraft.artifact.state",stack.getHoverName(),
                    Component.translatable("screen.jugcraft.alchemy."+(stack.has(AlchemicalGear.ACTIVE)?"on":"off")),mode(stack)+1));
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c) {
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(p.isShiftKeyDown()||id.startsWith("divining_rod_"))return use(c.getLevel(),p,c.getHand());
            if(!RateGate.allow(p,"alchemy_artifact_block",10)||!AlchemicalGear.ready(p))return InteractionResult.FAIL;
            var pos=c.getClickedPos();var state=c.getLevel().getBlockState(pos);boolean used=false;
            if(miner(id)){mine(p,c.getItemInHand(),pos,c.getClickedFace(),id);return InteractionResult.SUCCESS;}
            if(id.equals("mercurial_eye")){build(p,new BlockHitResult(c.getClickLocation(),c.getClickedFace(),pos,false));return InteractionResult.SUCCESS;}
            boolean harvest=id.equals("harvest_goddess_band")||id.equals("arcana_ring")&&mode(c.getItemInHand())==2;
            boolean ice=id.equals("zero_ring")||id.equals("arcana_ring")&&mode(c.getItemInHand())==0;
            boolean fire=id.equals("ignition_ring")||id.equals("arcana_ring")&&mode(c.getItemInHand())==1;
            if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            if(harvest&&state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state))used=harvest(p,pos,c.getItemInHand());
            else if(ice&&freeze(p,pos))used=true;
            else if(fire)used=kindle(p,pos);
            if(used){sound(p,"pepower");return InteractionResult.SUCCESS;}
            return use(c.getLevel(),p,c.getHand());
        }
        @Override public boolean canEquip(ItemStack stack,TrinketSlotAccess slot,LivingEntity entity){return passive(id)&&entity instanceof Player;}
        @Override public boolean isFoil(ItemStack stack){return stack.has(AlchemicalGear.ACTIVE)||super.isFoil(stack);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag){
            text.accept(Component.translatable("tooltip.jugcraft."+id).withStyle(ChatFormatting.GRAY));
            if(passive(id))text.accept(Component.translatable("message.jugcraft.artifact.state",stack.getHoverName(),Component.translatable("screen.jugcraft.alchemy."+(stack.has(AlchemicalGear.ACTIVE)?"on":"off")),mode(stack)+1).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}

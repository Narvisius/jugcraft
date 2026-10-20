package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animation.RawAnimation;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity.Route;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** A real walking courier. Cargo is removed at pickup, saved once on this entity and deposited only at arrival. */
public final class Starbuncle extends WorkerEntity<Starbuncle> {
    public String style(){if(!hasCustomName())return "starbuncle";String name=getCustomName().getString().toLowerCase(java.util.Locale.ROOT).replace(' ','_');return switch(name){case "goo","goobuncle"->"goobuncle";case "jarva","starbuncle_jarva"->"starbuncle_jarva";case "gift","gift_starby"->"gift_starby";case "nook","lily","sir_squirrely","xacris","zieg","bootybuncle"->name;default->"starbuncle";};}
    public static final int RANGE = 32, LOAD = 16, MAX_PER_OWNER = 8;
    public static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(Starbuncle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CARGO_DISPLAY = SynchedEntityData.defineId(Starbuncle.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> STACKS=SynchedEntityData.defineId(Starbuncle.class,EntityDataSerializers.INT);
    public String renderStyle(){int count=entityData.get(STACKS);return style()+(count==0?"":"_stack"+(count+1));}
    private static final EntityDataAccessor<Integer> ACCESSORY=SynchedEntityData.defineId(Starbuncle.class,EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), RUN = RawAnimation.begin().thenLoop("run");
    private @Nullable Route route;
    private ItemStack cargo = ItemStack.EMPTY;
    private final List<ItemStack> queuedCargo=new ArrayList<>();
    private net.minecraft.resources.Identifier preferredPath;
    private final List<Route> routes=new ArrayList<>();
    private final List<BlockPos> destinations=new ArrayList<>();
    private @Nullable BlockPos bed;
    private ItemStack filter=ItemStack.EMPTY;
    private int routeCursor, upgrades;
    private long nextWild;
    public int accessory(){return entityData.get(ACCESSORY);}
    public int load(){return LOAD;}
    public void home(BlockPos p){bed=p.immutable();}
    public boolean endpoint(ServerLevel l,ServerPlayer p,BlockPos pos){return NouveauStorage.accessible(l,p,pos)!=null||accessory()==2&&l.hasChunkAt(pos)&&l.getBlockEntity(pos) instanceof NouveauWorkshop n&&n.kind().equals("potion_jar")&&n.permitted(p);}
    public boolean destination(ServerPlayer p,BlockPos pos){if(!mayServe(p)||distanceToSqr(p)>1024||!endpoint(p.level(),p,pos)||destinations.size()>=8)return false;if(!destinations.contains(pos))destinations.add(pos.immutable());return true;}
    public void clearAccessory(ServerLevel l){if(accessory()>0){spawnAtLocation(l,new ItemStack(NouveauCatalog.item(accessory()==1?"starbuncle_shades":"wixie_hat")));entityData.set(ACCESSORY,0);}}
    public void clearRoutes(){if(!cargo.isEmpty()||!queuedCargo.isEmpty())return;route=null;routes.clear();destinations.clear();}
    public Starbuncle(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16).add(Attributes.MOVEMENT_SPEED, .3).add(Attributes.FOLLOW_RANGE, 32);
    }
    @Override public String kind() { return "starbuncle"; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(COLOR, 4); b.define(CARGO_DISPLAY, ItemStack.EMPTY);b.define(ACCESSORY,0);b.define(STACKS,0); }
    public int color() { return Math.clamp(entityData.get(COLOR), 0, COLORS.length - 1); }
    public void color(int value) { entityData.set(COLOR, Math.clamp(value, 0, COLORS.length - 1)); }
    public @Nullable Route route() { return route; }
    public ItemStack cargo() { return cargo.copy(); }
    /** Display-only copy. Server transfers never read this synced item. */
    public ItemStack cargoDisplay() { return entityData.get(CARGO_DISPLAY).copy(); }
    private void syncCargo() { entityData.set(CARGO_DISPLAY, cargo.copy()); }
    public boolean mayServe(Player player) {
        return owner != null && (owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID()))
            && player.isAlive() && !player.isSpectator() && player.level() == level();
    }
    private static boolean valid(Route r, ServerLevel level) {
        return r.dimension().equals(level.dimension().identifier().toString()) && !r.source().equals(r.target())
            && r.source().distSqr(r.target()) <= RANGE * RANGE && level.isInWorldBounds(r.source()) && level.isInWorldBounds(r.target());
    }
    /** Refuse rerouting in flight; callers cannot abandon or duplicate an undelivered stack. */
    public boolean assign(ServerPlayer player, Route next) {
        if (!(level() instanceof ServerLevel level) || !JugNouveau.enabled() || !mayServe(player) || !cargo.isEmpty()
            || !queuedCargo.isEmpty() || !valid(next, level) || distanceToSqr(player) > RANGE * RANGE
            || !endpoint(level,player,next.source())||!endpoint(level,player,next.target())) return false;
        if(!routes.contains(next)){if(routes.size()>=8)return false;routes.add(next);}destination(player,next.target());
        route = next; arrived(); setStatus(Status.IDLE); return true;
    }
    @Override public Status think(ServerLevel level, long time, long gameTime) {
        if(cargo.isEmpty()&&!queuedCargo.isEmpty())cargo=queuedCargo.removeFirst();
        Status result = decide(level, gameTime);
        syncCargo();
        if (result != Status.TRAVELLING) stop();
        setStatus(result); return result;
    }
    private Status decide(ServerLevel level, long now) {
        if (!JugNouveau.enabled()) return Status.DISABLED;
        if(owner==null)return wild(level,now);
        ServerPlayer actor = Authority.answering(level, owner);
        if (actor == null) return Status.OWNER_OFFLINE;
        if(bed!=null&&level.hasChunkAt(bed)&&level.hasNeighborSignal(bed))return near(centre(bed),2)?Status.IDLE:walkTo(centre(bed),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
        if(route==null&&!routes.isEmpty())route=routes.get(Math.floorMod(routeCursor++,routes.size()));
        if(cargo.isEmpty()&&accessory()==0){var gathered=gather(level,actor,now);if(gathered!=null)return gathered;}
        if(route==null){
            if(!cargo.isEmpty())return depositLoose(level,actor,now);
            return bed!=null&&!near(centre(bed),3)?walkTo(centre(bed),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE:Status.IDLE;
        }
        if (!valid(route, level)) return Status.OTHER_DIMENSION;
        BlockPos target = cargo.isEmpty() ? route.source() : route.target();
        if (!level.isLoaded(target)) return Status.DESTINATION_UNLOADED;
        if(accessory()==2&&level.getBlockEntity(target) instanceof NouveauWorkshop jar&&jar.kind().equals("potion_jar")){
            if(!jar.permitted(actor))return Status.BLOCKED_BY_ACCESS;
            if(!near(centre(target),2))return walkTo(centre(target),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
            if(cargo.isEmpty()){if(jar.doses()==0)return Status.WAITING_FOR_RESOURCES;cargo=NouveauAlchemy.bottle(jar.potion());jar.drainPotion(1);return Status.WORKING;}
            var contents=cargo.get(DataComponents.POTION_CONTENTS);if(contents==null||!jar.fillPotion(contents,1))return Status.FULL;cargo=ItemStack.EMPTY;return Status.FINISHED;
        }
        Container chest = NouveauStorage.accessible(level, actor, target);
        if (chest == null) return Status.BLOCKED_BY_ACCESS;
        if (!near(centre(target), 2)) return walkTo(centre(target), 1.0F, 1, now) ? Status.TRAVELLING : Status.CANNOT_NAVIGATE;
        arrived();
        if (cargo.isEmpty()) {
            // Only the linked 27-slot chest half/barrel is searched, once per decision.
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (stack.isEmpty()||!allowed(level,target,stack)) continue;
                cargo = chest.removeItem(slot, Math.min(load(), stack.getCount()));
                for(int other=slot+1;other<Math.min(1728,chest.getContainerSize())&&queuedCargo.size()<upgrades;other++){var extra=chest.getItem(other);if(!extra.isEmpty()&&allowed(level,target,extra))queuedCargo.add(chest.removeItem(other,Math.min(load(),extra.getCount())));}
                chest.setChanged(); return Status.WORKING;
            }
            route=null;return Status.WAITING_FOR_RESOURCES;
        }
        for (int slot = 0; slot < chest.getContainerSize() && !cargo.isEmpty(); slot++) {
            if (!chest.canPlaceItem(slot, cargo)) continue;
            ItemStack in = chest.getItem(slot);
            int limit = Math.min(chest.getMaxStackSize(), cargo.getMaxStackSize());
            if (in.isEmpty()) chest.setItem(slot, cargo.split(Math.min(limit, cargo.getCount())));
            else if (ItemStack.isSameItemSameComponents(in, cargo) && in.getCount() < limit) {
                int count = Math.min(limit - in.getCount(), cargo.getCount()); in.grow(count); cargo.shrink(count);
            }
        }
        chest.setChanged();
        if(cargo.isEmpty()){if(queuedCargo.isEmpty())route=null;return Status.FINISHED;}return Status.FULL;
    }
    private boolean allowed(ServerLevel l,BlockPos pos,ItemStack stack){
        if(!filter.isEmpty()){
            boolean listed=filter.getOrDefault(NouveauUtilities.FILTER,List.of()).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if(filter.is(NouveauCatalog.item("allow_scroll"))&&!listed||filter.is(NouveauCatalog.item("deny_scroll"))&&listed)return false;
        }
        var frames=l.getEntitiesOfClass(ItemFrame.class,new AABB(pos).inflate(1.25),f->!f.getItem().isEmpty());
        return frames.isEmpty()||frames.stream().limit(6).anyMatch(f->NouveauFilters.allows(f.getItem(),stack));
    }
    private Status wild(ServerLevel l,long now){
        if(now<nextWild)return Status.IDLE;nextWild=now+20;
        for(var drop:l.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(8),e->e.getItem().is(Items.GOLD_NUGGET)&&e.isAlive()).stream().limit(4).toList()){
            if(!near(drop.position(),1.5))return walkTo(drop.position(),1.2F,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
            drop.getItem().shrink(1);if(drop.getItem().isEmpty())drop.discard();spawnAtLocation(l,new ItemStack(NouveauCatalog.item("starbuncle_shards")));discard();return Status.FINISHED;
        }
        var spot=blockPosition().offset(l.getRandom().nextInt(9)-4,0,l.getRandom().nextInt(9)-4);
        return l.hasChunkAt(spot)&&walkTo(centre(spot),.8F,1,now)?Status.TRAVELLING:Status.IDLE;
    }
    private @Nullable Status gather(ServerLevel l,ServerPlayer p,long now){
        if(destinations.isEmpty()&&route==null)return null;
        for(var drop:l.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(6),e->e.isAlive()&&!e.hasPickUpDelay()).stream().limit(12).toList()){
            if(!Authority.mayChange(l,p,drop.blockPosition())||!allowed(l,drop.blockPosition(),drop.getItem()))continue;
            if(!near(drop.position(),1.5))return walkTo(drop.position(),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
            cargo=drop.getItem().split(Math.min(load(),drop.getItem().getCount()));if(drop.getItem().isEmpty())drop.discard();return Status.WORKING;
        }
        for(BlockPos pos:BlockPos.betweenClosed(blockPosition().offset(-3,-1,-3),blockPosition().offset(3,1,3))){
            if(!l.hasChunkAt(pos)||!Authority.mayChange(l,p,pos))continue;var state=l.getBlockState(pos);
            if(!(state.getBlock() instanceof NouveauNature.Sourceberries)||state.getValue(SweetBerryBushBlock.AGE)<2)continue;
            var berries=new ItemStack(NouveauCatalog.item("sourceberry_bush"),2);if(!allowed(l,pos,berries))continue;
            if(!near(centre(pos),2))return walkTo(centre(pos),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
            if(l.setBlock(pos,state.setValue(SweetBerryBushBlock.AGE,1),Block.UPDATE_ALL)){cargo=berries;return Status.WORKING;}
        }return null;
    }
    private Status depositLoose(ServerLevel l,ServerPlayer p,long now){
        for(BlockPos destination:destinations){
            var container=NouveauStorage.accessible(l,p,destination);if(container==null||!allowed(l,destination,cargo))continue;
            if(!NouveauCrafting.deposit(container,cargo,true))continue;
            if(!near(centre(destination),2))return walkTo(centre(destination),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
            if(NouveauCrafting.deposit(container,cargo,false)){cargo=ItemStack.EMPTY;return Status.FINISHED;}
        }return Status.FULL;
    }
    @Override protected boolean walkTo(net.minecraft.world.phys.Vec3 target,float speed,int closeEnough,long now){
        var preferred=preferredPath==null?net.minecraft.world.level.block.Blocks.DIRT_PATH:BuiltInRegistries.BLOCK.getValue(preferredPath);
        if(distanceToSqr(target)>16&&preferred!=null){BlockPos best=null;double score=distanceToSqr(target);
            for(var pos:BlockPos.betweenClosed(blockPosition().offset(-3,-1,-3),blockPosition().offset(3,1,3))){if(!level().hasChunkAt(pos)||!level().getBlockState(pos.below()).is(preferred)||!level().getBlockState(pos).canBeReplaced()||!level().getBlockState(pos.above()).canBeReplaced())continue;double d=net.minecraft.world.phys.Vec3.atBottomCenterOf(pos).distanceToSqr(target);if(d<score-1){best=pos.immutable();score=d;}}
            if(best!=null)return super.walkTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(best),speed,0,now);
        }return super.walkTo(target,speed,closeEnough,now);
    }
    @Override protected RawAnimation animation(Status status) { return status == Status.TRAVELLING ? RUN : IDLE; }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        if (held.is(JugNouveau.WAND)) {if(player instanceof ServerPlayer p&&mayServe(p)&&Authority.mayChange(p.level(),p,blockPosition())){clearAccessory(p.level());if(p.isShiftKeyDown())clearRoutes();}return held.getItem().interactLivingEntity(held, player, this, hand);}
        if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
        if (!mayServe(player) || !Authority.mayChange(server.level(), player, blockPosition())) return InteractionResult.FAIL;
        if(held.is(NouveauCatalog.item("starbuncle_shades"))||held.is(NouveauCatalog.item("wixie_hat"))){clearAccessory(server.level());entityData.set(ACCESSORY,held.is(NouveauCatalog.item("wixie_hat"))?2:1);held.consume(1,player);return InteractionResult.SUCCESS;}
        if(held.is(NouveauCatalog.item("allow_scroll"))||held.is(NouveauCatalog.item("deny_scroll"))){if(!filter.isEmpty())spawnAtLocation(server.level(),filter);filter=held.split(1);return InteractionResult.SUCCESS;}
        if(held.is(JugNouveau.STARBUNCLE_CHARM)&&upgrades<3){upgrades++;entityData.set(STACKS,upgrades);held.consume(1,player);return InteractionResult.SUCCESS;}
        if(held.getItem() instanceof BlockItem block){preferredPath=BuiltInRegistries.BLOCK.getKey(block.getBlock());server.sendOverlayMessage(Component.literal("Preferred path: "+held.getHoverName().getString()));return InteractionResult.SUCCESS;}
        DyeColor dye = ScarecrowBlock.dyeColor(held);
        if (dye != null && RateGate.allow(server, "starbuncle", 4)) {
            int index = java.util.Arrays.asList(COLORS).indexOf(dye.getName());
            if (index >= 0 && color() != index) { color(index); held.consume(1, server); }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (!RateGate.allow(server, "starbuncle", 4)) return InteractionResult.FAIL;
            // Destruction returns cargo and a color-preserving charm exactly once.
            discard(); return InteractionResult.SUCCESS;
        }
        server.sendOverlayMessage(Component.translatable("message.jugcraft.starbuncle.status", describe(), cargo.getCount(),
            route == null ? "—" : route.source().toShortString(), route == null ? "—" : route.target().toShortString()));
        return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && (!mayServe(p) || !Authority.mayChange(level, p, blockPosition()))) return false;
        return super.hurtServer(level, source, amount);
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()&&!NouveauContainment.capturing(this) && !isRemoved() && level() instanceof ServerLevel level) {
            if (!cargo.isEmpty()) { spawnAtLocation(level, cargo); cargo = ItemStack.EMPTY; }
            for(var extra:queuedCargo)if(!extra.isEmpty())spawnAtLocation(level,extra);queuedCargo.clear();
            clearAccessory(level);if(!filter.isEmpty()){spawnAtLocation(level,filter);filter=ItemStack.EMPTY;}
            if (owner != null) {
                var charm = new ItemStack(JugNouveau.STARBUNCLE_CHARM); charm.set(JugNouveau.CHARM_COLOR, color());
                spawnAtLocation(level, charm); WorkerRoster.of(level.getServer()).remove(owner, getUUID()); owner = null;
                if(upgrades>0){spawnAtLocation(level,new ItemStack(JugNouveau.STARBUNCLE_CHARM,upgrades));upgrades=0;}
            }
        }
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out); out.putInt("color", color());
        if (route != null) out.store("route", Route.CODEC, route);
        out.store("cargo", ItemStack.OPTIONAL_CODEC, cargo);out.store("queued_cargo",ItemStack.CODEC.listOf(),queuedCargo);if(preferredPath!=null)out.store("preferred_path",net.minecraft.resources.Identifier.CODEC,preferredPath);
        out.store("routes",Route.CODEC.listOf(),routes);out.store("destinations",BlockPos.CODEC.listOf(),destinations);if(bed!=null)out.store("bed",BlockPos.CODEC,bed);
        out.putInt("accessory",accessory());out.putInt("upgrades",upgrades);out.store("filter",ItemStack.OPTIONAL_CODEC,filter);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in); color(in.getIntOr("color", 4)); route = in.read("route", Route.CODEC).orElse(null);
        cargo = in.read("cargo", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);queuedCargo.clear();queuedCargo.addAll(in.read("queued_cargo",ItemStack.CODEC.listOf()).orElse(List.of()).stream().filter(s->!s.isEmpty()).limit(3).toList());preferredPath=in.read("preferred_path",net.minecraft.resources.Identifier.CODEC).orElse(null);
        routes.clear();routes.addAll(in.read("routes",Route.CODEC.listOf()).orElse(route==null?List.of():List.of(route)).stream().distinct().limit(8).toList());
        destinations.clear();destinations.addAll(in.read("destinations",BlockPos.CODEC.listOf()).orElse(List.of()).stream().distinct().limit(8).toList());bed=in.read("bed",BlockPos.CODEC).orElse(null);
        entityData.set(ACCESSORY,Math.clamp(in.getIntOr("accessory",0),0,2));upgrades=Math.clamp(in.getIntOr("upgrades",0),0,3);entityData.set(STACKS,upgrades);filter=in.read("filter",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        syncCargo();
    }
}

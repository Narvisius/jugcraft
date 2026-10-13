package local.peepo;

import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import io.github.jimbozoomer.jugcraft.town.TownProtection;

/** Item-only crate; all capture/release decisions occur on the server's main thread. */
public final class MobTransportCrate extends Item {
    public static Item WOOD,IRON;
    public static final TagKey<EntityType<?>> ALLOWED=TagKey.create(Registries.ENTITY_TYPE,PeepoMod.id("transport_allowed"));
    public static final TagKey<EntityType<?>> DENIED=TagKey.create(Registries.ENTITY_TYPE,PeepoMod.id("transport_denied"));
    private static final String CONTENTS="TransportMobs";
    private final int capacity;
    public MobTransportCrate(Properties p,int capacity){super(p);this.capacity=capacity;}
    public static int capacity(ItemStack stack){return stack.getItem() instanceof MobTransportCrate c?c.capacity:0;}
    public static List<CompoundTag> occupants(ItemStack stack){
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        var tags=data.getListOrEmpty(CONTENTS);var out=new ArrayList<CompoundTag>();
        for(int i=0;i<Math.min(capacity(stack),tags.size());i++)out.add(tags.getCompoundOrEmpty(i));
        return out;
    }
    public static int count(ItemStack stack){return (int)occupants(stack).stream().filter(t->!t.getStringOr("Claim","").isEmpty()).count();}
    private static void write(ItemStack stack,List<CompoundTag> contents){
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var list=new ListTag();contents.forEach(t->list.add(t.copy()));
        data.put(CONTENTS,list);CustomData.set(DataComponents.CUSTOM_DATA,stack,data);
    }
    private static void message(Player p,String text){p.sendOverlayMessage(Component.literal(text));}
    public static boolean owned(Player player,Mob mob){
        if(mob instanceof PeepoEntity p && p.orders.tamed())return p.orders.allowed(player);
        if(mob instanceof OwnableEntity pet && pet.getOwnerReference()!=null)return pet.getOwnerReference().getUUID().equals(player.getUUID());
        return true;
    }
    public static boolean friendly(Mob mob){
        return mob.isAlive() && !mob.isRemoved() && mob.getType().canSerialize() && !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType()).is(DENIED)
            && !(mob instanceof Enemy) && mob.getTarget()==null && !(mob instanceof NeutralMob neutral && neutral.isAngry())
            && (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType()).is(ALLOWED) || mob.getType().getCategory()!=MobCategory.MONSTER && mob.getType().getCategory()!=MobCategory.MISC);
    }
    private static boolean permitted(ServerPlayer p,BlockPos at){
        var level=p.level();return !p.isSpectator() && p.mayBuild() && level.hasChunkAt(at) && level.mayInteract(p,at) && !TownProtection.denies(p,level,at);
    }
    /** Visual-only fields; no inventories, brain memories, owner data or recursive passengers reach clients. */
    public static CompoundTag preview(CompoundTag mob){
        var out=new CompoundTag();
        for(String key:List.of("Age","Variant","variant","Color","Sheared","CollarColor","PumpkinCostume","Size","Type","RabbitType","CatType","MooshroomType"))
            if(mob.contains(key))out.put(key,mob.get(key).copy());
        return out;
    }
    public static boolean capture(ServerPlayer p,ItemStack stack,Mob mob){
        if(capacity(stack)==0 || stack.getCount()!=1 || mob.level()!=p.level() || !permitted(p,mob.blockPosition()) || p.distanceToSqr(mob)>36)return false;
        if(!friendly(mob)){message(p,"Only calm, friendly mobs can travel in this crate.");return false;}
        if(!owned(p,mob)){message(p,"This mob belongs to another player.");return false;}
        if(mob.isPassenger() || mob.isVehicle() || mob instanceof Leashable leash && leash.isLeashed()){
            message(p,"Dismount and remove leads before picking up this mob.");return false;
        }
        var entries=occupants(stack);int slot=0;while(slot<entries.size() && !entries.get(slot).getStringOr("Claim","").isEmpty())slot++;
        if(slot>=capacity(stack)){message(p,"The crate is full.");return false;}
        if(mob instanceof PeepoEntity npc){npc.social.cancel();npc.resetCompanionRoutine();npc.transport.stop();npc.leaveCompanionBed();npc.setRestMode(CompanionEnergy.Rest.NONE);npc.setNoGravity(false);}
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.level().registryAccess());mob.saveWithoutId(out);
        var saved=out.buildResult();saved.putString("TransportType",BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        var entry=new CompoundTag();entry.putString("Claim",TransportLedger.get(p.level().getServer()).add(saved));
        entry.putString("Type",saved.getStringOr("TransportType",""));entry.putString("Name",mob.getName().getString());entry.put("Preview",preview(saved));
        if(slot==entries.size())entries.add(entry);else entries.set(slot,entry);
        write(stack,entries);mob.discard();message(p,"Picked up "+entry.getStringOr("Name","mob")+" ("+count(stack)+"/"+capacity(stack)+").");return true;
    }
    public static Mob restore(Level level,CompoundTag data){
        var id=net.minecraft.resources.Identifier.tryParse(data.getStringOr("TransportType",""));if(id==null)return null;
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);if(type==null)return null;
        var entity=type.create(level,EntitySpawnReason.LOAD);if(!(entity instanceof Mob mob))return null;
        mob.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));return mob;
    }
    public static boolean release(ServerPlayer p,ItemStack stack,int index,Vec3 requested){
        var entries=occupants(stack);if(index<0 || index>=entries.size() || stack.getCount()!=1 || p.isSpectator())return false;
        var entry=entries.get(index);String claim=entry.getStringOr("Claim","");if(claim.isEmpty())return false;
        var ledger=TransportLedger.get(p.level().getServer());var saved=ledger.get(claim);
        if(saved==null){entries.set(index,new CompoundTag());write(stack,entries);message(p,"This mob was already released from another copy of the crate.");return false;}
        Mob mob;
        try{mob=restore(p.level(),saved);}catch(RuntimeException error){message(p,"This mob could not be loaded. It is still safely stored.");return false;}
        if(mob==null || !mob.isAlive() || !mob.getType().canSpawn(p.level()) || !owned(p,mob)){message(p,"This mob cannot be released by you here.");return false;}
        for(var level:p.level().getServer().getAllLevels())if(level.getEntity(mob.getUUID())!=null){message(p,"This mob is already present in the world.");return false;}
        var at=findSpace(p,mob,requested);
        if(at==null){message(p,"There is not enough safe space here. Try clear ground nearby.");return false;}
        mob.snapTo(at.x,at.y,at.z,p.getYRot()+180,0);mob.setDeltaMovement(Vec3.ZERO);mob.fallDistance=0;
        if(!p.level().addFreshEntity(mob))return false;
        ledger.remove(claim);entries.set(index,new CompoundTag());write(stack,entries);
        message(p,"Released "+entry.getStringOr("Name","mob")+".");return true;
    }
    private static Vec3 findSpace(ServerPlayer p,Mob mob,Vec3 requested){
        var level=p.level();
        // Bounded nearby probes; never request an unloaded chunk. Ground clicks use precisely the clicked surface.
        for(int i=0;i<(requested==null?24:1);i++){
            Vec3 at=requested;
            if(at==null){double angle=(i%8)*Math.PI/4;var base=p.position().add(Math.cos(angle)*2,0,Math.sin(angle)*2);at=new Vec3(base.x,Math.floor(base.y)+(i/8-1),base.z);}
            var pos=BlockPos.containing(at);if(p.distanceToSqr(at)>49 || !permitted(p,pos) || !permitted(p,pos.below()))continue;
            mob.snapTo(at.x,at.y,at.z,0,0);var box=mob.getBoundingBox();
            if(box.getXsize()>16 || box.getYsize()>16 || box.getZsize()>16 || !level.getWorldBorder().isWithinBounds(box) || box.minY<level.getMinY() || box.maxY>level.getMaxY())continue;
            boolean loaded=true;for(var corner:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(!permitted(p,corner)){loaded=false;break;}
            if(!loaded || !level.noCollision(mob,box) || !level.getEntities(mob,box,e->e.isAlive() && !e.isSpectator()).isEmpty() || level.containsAnyLiquid(box))continue;
            if(level.noCollision(mob,box.move(0,-.12,0)) || mob.getType().isBlockDangerous(level.getBlockState(pos.below())))continue;
            return at;
        }
        return null;
    }
    public static void open(ServerPlayer player,InteractionHand hand){
        player.openMenu(new SimpleMenuProvider((id,inv,p)->new TransportCrateMenu(id,inv,hand),player.getItemInHand(hand).getHoverName()));
    }
    private static InteractionResult action(Player player,Level level,InteractionHand hand,Entity entity,Vec3 ground){
        var stack=player.getItemInHand(hand);if(capacity(stack)==0)return InteractionResult.PASS;
        if(level.isClientSide() || !(player instanceof ServerPlayer p) || p.isSpectator())return InteractionResult.SUCCESS;
        if(p.isShiftKeyDown()){open(p,hand);return InteractionResult.SUCCESS;}
        if(p.getCooldowns().isOnCooldown(stack))return InteractionResult.SUCCESS;
        p.getCooldowns().addCooldown(stack,5);
        if(entity instanceof Mob mob)capture(p,stack,mob);
        else if(ground!=null){var entries=occupants(stack);for(int i=0;i<entries.size();i++)if(!entries.get(i).getStringOr("Claim","").isEmpty()){release(p,stack,i,ground);break;}}
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){return action(player,level,hand,null,null);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        out.accept(Component.literal(count(stack)+" / "+capacity(stack)+" friendly mobs").withColor(0xD6B57D));
        for(var e:occupants(stack))if(!e.getStringOr("Claim","").isEmpty())out.accept(Component.literal("• "+e.getStringOr("Name","Mob")));
        out.accept(Component.literal("Use on a mob to pick up; on ground to release one.").withColor(0xAAAAAA));
        out.accept(Component.literal("Sneak + use: choose a mob to release beside you.").withColor(0xAAAAAA));
    }
    public static void initialize(){
        WOOD=register("mob_transport_crate",4);IRON=register("iron_mob_transport_crate",8);TransportCrateMenu.initialize();
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,PeepoMod.id("transport_crate_upgrade"),TransportCrateUpgrade.SERIALIZER);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->{e.accept(WOOD);e.accept(IRON);});
        UseEntityCallback.EVENT.register((p,l,h,e,hit)->action(p,l,h,e,null));
        UseBlockCallback.EVENT.register((p,l,h,hit)->action(p,l,h,null,hit.getDirection()==Direction.UP?hit.getLocation():null));
    }
    private static Item register(String name,int size){var id=PeepoMod.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new MobTransportCrate(new Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1),size));}
}

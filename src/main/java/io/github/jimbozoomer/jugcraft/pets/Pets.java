package io.github.jimbozoomer.jugcraft.pets;

import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.storage.*;

/** Server-authoritative companion storage. Unloaded pets are recalled only after their real entity loads. */
public final class Pets {
    private static final Set<Mob> LIVE = Collections.newSetFromMap(new IdentityHashMap<>());
    public static final com.google.gson.Gson JSON = new com.google.gson.Gson();
    public static final List<String> MODES = List.of("FOLLOW", "STAY", "DEFEND", "PASSIVE");
    public static UUID owner(Mob mob) {
        if (io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons.temporary(mob)) return null;
        if (mob instanceof io.github.jimbozoomer.jugcraft.agriculture.Pumpkling pet) return pet.owner();
        if (mob instanceof local.peepo.PeepoEntity pet) return pet.orders.ownerId();
        if (mob instanceof MenagerieAnimal animal) return animal.owner();
        if (mob instanceof io.github.jimbozoomer.jugcraft.creatures.chima.Chima chima) return chima.owner();
        if (mob instanceof OwnableEntity owned
                && owned.getOwnerReference() != null) return owned.getOwnerReference().getUUID();
        return null;
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(View.TYPE, View.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (action, context) -> {
            var p = context.player();
            if (!io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(p, "pet_inventory", 5)) return;
            if(action.operation.equals("page")){try{view(p,"",Math.max(0,Integer.parseInt(action.id)));}catch(NumberFormatException ignored){}return;}
            String message = action(p, action.id, action.operation);
            var ids=PetRoster.get(p.level().getServer()).entries.values().stream().filter(e->p.getUUID().toString().equals(e.getStringOr("owner",""))).map(e->e.getStringOr("id","")).toList();
            view(p, message, Math.max(0,ids.indexOf(action.id))/200*200);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, selection) -> dispatcher.register(
            Commands.literal("pets").executes(c -> { view(c.getSource().getPlayerOrException(), ""); return 1; })));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if (entity instanceof Mob mob) {
            var e = PetRoster.get(level.getServer()).entries.get(mob.getUUID());
            if(e!=null && e.getIntOr("generation",0)==0 && e.getStringOr("state","ACTIVE").equals("ACTIVE") && owner(mob)!=null && owner(mob).toString().equals(e.getStringOr("owner","")))mob.addTag(e.getStringOr("incarnation",""));
            if (e != null && (!e.getStringOr("state", "ACTIVE").equals("ACTIVE") || !mob.entityTags().contains(e.getStringOr("incarnation", "")))) { mob.discard(); return; }
            LIVE.add(mob);
        } });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> { if (entity instanceof Mob mob) { LIVE.remove(mob); PetBehaviors.detach(mob); } });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { for(var mob:LIVE)PetBehaviors.detach(mob); LIVE.clear(); PetBehaviors.reset(); });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim,source,amount)->{
            if(!(source.getEntity() instanceof Mob pet)||owner(pet)==null)return true;
            var e=PetRoster.get(pet.level().getServer()).entries.get(pet.getUUID());if(e==null)return true;
            String mode=e.getStringOr("mode","FOLLOW");
            if(!(pet instanceof local.peepo.PeepoEntity)&&(mode.equals("PASSIVE")||mode.equals("STAY")))return false;
            var player=pet.level().getServer().getPlayerList().getPlayer(owner(pet));
            return player!=null && !(victim instanceof net.minecraft.world.entity.player.Player) && !PetBehaviors.owned(victim)
                && io.github.jimbozoomer.jugcraft.concordance.Authority.mayStrike(player,victim);
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damage) -> {
            if (!(entity instanceof Mob mob) || owner(mob) == null) return true;
            var entry = enroll(mob);
            if (entry == null) return true;
            // Save before death produces drops or experience, including lethal hits between enrollment ticks.
            mob.setHealth(Math.max(1, mob.getMaxHealth() * .5F));
            if (!store(mob, entry, true)) return true;
            var p = mob.level().getServer().getPlayerList().getPlayer(owner(mob));
            if (p != null) p.sendSystemMessage(Component.literal(mob.getName().getString() + " was knocked out. Recover them in /pets."));
            return false;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (var mob : List.copyOf(LIVE)) {
                if (mob.isRemoved() || !mob.isAlive() || owner(mob) == null) continue;
                var entry = enroll(mob); if (entry == null) continue;
                if (!entry.getStringOr("state", "ACTIVE").equals("ACTIVE")) { mob.discard(); continue; }
                if (entry.getBooleanOr("recall", false)) { store(mob, entry, false); continue; }
                PetBehaviors.bonuses(mob, PetRoster.level(entry));
                var p = server.getPlayerList().getPlayer(owner(mob));
                if (p != null && p.level() == mob.level() && p.isAlive() && !p.isSpectator()) {
                    PetBehaviors.tick(p, mob, entry.getStringOr("mode", "FOLLOW"), PetRoster.level(entry));
                    // Bond grows from actual nearby travel; six moving seconds per point, no kill/drop farming.
                    if (mob.distanceToSqr(p) < 256 && mob.getDeltaMovement().horizontalDistanceSqr() > .0001) {
                        int travel = entry.getIntOr("travel", 0) + 1;
                        entry.putInt("travel", travel % 6);
                        if (travel >= 6) entry.putInt("xp", Math.min(2280, entry.getIntOr("xp", 0) + 1));
                        PetRoster.get(server).setDirty();
                    }
                }
            }
        });
    }
    public static CompoundTag enroll(Mob mob) {
        UUID owner = owner(mob); if (owner == null || mob.level().isClientSide()) return null;
        var roster = PetRoster.get(mob.level().getServer());
        var entry = roster.entries.get(mob.getUUID());
        if (entry != null) {
            if (!entry.getStringOr("owner", "").equals(owner.toString())) {
                entry.putString("owner", owner.toString()); roster.setDirty();
            }
            String name = mob.getName().getString();
            if (!name.equals(entry.getStringOr("name", ""))) { entry.putString("name", name); roster.setDirty(); }
            return entry;
        }
        entry = new CompoundTag(); entry.putString("id", mob.getUUID().toString()); entry.putString("owner", owner.toString());
        entry.putString("name", mob.getName().getString()); entry.putString("state", "ACTIVE"); entry.putString("mode", PetBehaviors.sitting(mob) ? "STAY" : "FOLLOW"); entry.putBoolean("own_orders",mob instanceof local.peepo.PeepoEntity);
        entry.putString("type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        String incarnation = "jugcraft_pet_" + UUID.randomUUID(); mob.addTag(incarnation); entry.putString("incarnation", incarnation);
        roster.entries.put(mob.getUUID(), entry); roster.setDirty(); return entry;
    }
    public static boolean store(Mob mob, CompoundTag entry, boolean knockedOut) {
        if (!knockedOut && (mob.isVehicle() || mob.isPassenger() || mob.getLeashData() != null)) return false;
        if(knockedOut){mob.ejectPassengers();mob.stopRiding();if(mob.isLeashed())mob.dropLeash();}
        if (mob instanceof local.peepo.PeepoEntity pet) pet.preparePetStorage();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mob.registryAccess());
        if (!mob.save(output)) return false;
        // Close nearby entity inventories before removing their backing entity.
        for (var viewer : mob.level().getServer().getPlayerList().getPlayers())
            if (viewer.level() == mob.level() && viewer.distanceToSqr(mob) < 1024 && viewer.containerMenu != viewer.inventoryMenu) viewer.closeContainer();
        entry.put("entity", output.buildResult()); entry.putString("state", knockedOut ? "KNOCKED_OUT" : "STORED");
        // For the stable screen: how it was when it went in (a stored entity is not rebuilt just to read its health).
        entry.putFloat("health", mob.getHealth()); entry.putFloat("max_health", mob.getMaxHealth());
        entry.putLong("ready", mob.level().getServer().overworld().getGameTime() + (knockedOut ? 1200 : 0));
        entry.putBoolean("recall", false); PetRoster.get(mob.level().getServer()).setDirty();
        PetBehaviors.detach(mob); mob.discard(); return true;
    }
    public static String action(ServerPlayer p, String id, String operation) {
        if (operation.equals("open")) return "";
        if (!p.isAlive() || p.isSpectator()) return "Pets are available when you are alive.";
        UUID uuid; try { uuid = UUID.fromString(id); } catch (IllegalArgumentException e) { return "Unknown pet."; }
        var roster = PetRoster.get(p.level().getServer()); var entry = roster.entries.get(uuid);
        if (entry == null || !p.getUUID().toString().equals(entry.getStringOr("owner", ""))) return "This is not your pet.";
        for(var world:p.level().getServer().getAllLevels())if(world.getEntity(uuid) instanceof Mob pet && !p.getUUID().equals(owner(pet))) {enroll(pet);return "Pet ownership has changed.";}
        if (MODES.contains(operation)) {
            if(entry.getBooleanOr("own_orders",false))return "This companion keeps its own Orders menu; pet levels still apply.";
            entry.putString("mode", operation); roster.setDirty();
            for(var level:p.level().getServer().getAllLevels())if(level.getEntity(uuid) instanceof Mob pet && p.level()==pet.level()) {
                if(pet instanceof local.peepo.PeepoEntity)return "This companion keeps its own Orders menu; pet levels still apply.";
                if(!operation.equals("STAY"))PetBehaviors.sit(pet,false);
                PetBehaviors.tick(p,pet,operation,PetRoster.level(entry));
            }
            return "Behavior updated.";
        }
        if (operation.equals("recall") && entry.getStringOr("state", "").equals("ACTIVE")) {
            for (var level : p.level().getServer().getAllLevels()) if (level.getEntity(uuid) instanceof Mob mob) {
                if (!p.getUUID().equals(owner(mob))) return "Pet ownership has changed.";
                return store(mob, entry, false) ? "Pet recalled." : "Dismount and remove passengers/leads before recalling.";
            }
            entry.putBoolean("recall", true); roster.setDirty(); return "Recall queued until the pet's chunk loads; no duplicate will be created.";
        }
        if (!operation.equals("summon") || entry.getStringOr("state", "ACTIVE").equals("ACTIVE")) return "Pet is already active.";
        if (entry.getLongOr("ready", 0) > p.level().getServer().overworld().getGameTime()) return "Your pet is recovering (up to one minute).";
        for (var level : p.level().getServer().getAllLevels()) if (level.getEntity(uuid) != null) return "The original pet is still present.";
        var saved = entry.getCompound("entity"); if (saved.isEmpty()) return "No saved pet data.";
        var entity = EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING, p.registryAccess(), saved.get().copy()), p.level(), EntitySpawnReason.MOB_SUMMONED, EntityProcessor.NOP);
        if (!(entity instanceof Mob mob) || !p.getUUID().equals(owner(mob))) return "Pet data could not be restored safely.";
        boolean aquatic = mob instanceof net.minecraft.world.entity.animal.fish.WaterAnimal || mob instanceof MenagerieAnimal a && a.species().aquatic();
        boolean lava = mob instanceof MenagerieAnimal a && a.species().aquatic() && (a.species().habitat().equals("nether") || a.species().source().equals("radgill") || a.species().source().equals("sweetish_fish"));
        var fluid = lava ? net.minecraft.tags.FluidTags.LAVA : net.minecraft.tags.FluidTags.WATER;
        for (int r = 2; r <= 5; r++) for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (Math.abs(x) != r && Math.abs(z) != r) continue;
            var pos = p.blockPosition().offset(x, 0, z);
            if (!p.level().hasChunkAt(pos) || !io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(p.level(), p, pos)) continue;
            if (aquatic) {
                boolean submerged = true;
                for (int dy=0; dy<Math.max(1, (int)Math.ceil(mob.getBbHeight())); dy++) if (!p.level().getFluidState(pos.above(dy)).is(fluid)) submerged=false;
                if (!submerged) continue;
            } else if (!p.level().getFluidState(pos).isEmpty()) continue;
            var floor = p.level().getBlockState(pos.below());
            if (floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || floor.is(net.minecraft.world.level.block.Blocks.CACTUS) || floor.is(net.minecraft.world.level.block.Blocks.CAMPFIRE) || floor.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) continue;
            if (!aquatic && !p.level().getBlockState(pos.below()).isCollisionShapeFullBlock(p.level(), pos.below())) continue;
            mob.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, p.getYRot(), 0);
            var box=mob.getBoundingBox();
            if (!p.level().hasChunksAt(net.minecraft.core.BlockPos.containing(box.minX,box.minY-1,box.minZ), net.minecraft.core.BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                || !p.level().noCollision(mob) || !p.level().getWorldBorder().isWithinBounds(box) || !PetBehaviors.permitted(p.level(),p,box)) continue;
            if (aquatic ? !PetBehaviors.filled(p.level(),box,p.level().getFluidState(pos)) : p.level().containsAnyLiquid(box)) continue;
            mob.clearFire(); mob.fallDistance = 0;
            PetBehaviors.bonuses(mob, PetRoster.level(entry));
            String previous = entry.getStringOr("state", "STORED"); entry.putString("state", "ACTIVE");
            String oldIncarnation = entry.getStringOr("incarnation", "");
            mob.removeTag(oldIncarnation); String incarnation = "jugcraft_pet_" + UUID.randomUUID(); mob.addTag(incarnation); entry.putString("incarnation", incarnation);
            if (!p.level().addFreshEntity(mob)) { entry.putString("state", previous); entry.putString("incarnation", oldIncarnation); return "Could not summon pet."; }
            entry.putInt("generation",entry.getIntOr("generation",0)+1); roster.setDirty(); return "Pet summoned.";
        }
        return aquatic ? (lava ? "Find nearby lava for this lava-dwelling pet." : "Find nearby open water for this pet.") : "Find a clear, solid area for this pet.";
    }
    public static void retired(Mob pet) {
        var roster=PetRoster.get(pet.level().getServer());
        if(roster.entries.remove(pet.getUUID())!=null)roster.setDirty();
    }
    public static void converted(Mob before, Mob after) {
        var roster=PetRoster.get(after.level().getServer()); var old=roster.entries.get(before.getUUID());
        if(old==null || owner(after)==null || !owner(after).toString().equals(old.getStringOr("owner","")))return;
        var next=enroll(after); next.putInt("xp",old.getIntOr("xp",0)); next.putString("mode",old.getStringOr("mode","FOLLOW"));
        roster.entries.remove(before.getUUID()); roster.setDirty();
    }
    public static void view(ServerPlayer p, String message) { view(p,message,0); }
    public static void view(ServerPlayer p, String message, int offset) {
        var owned=PetRoster.get(p.level().getServer()).entries.values().stream().filter(e->p.getUUID().toString().equals(e.getStringOr("owner",""))).toList();
        int first=Math.min(offset,Math.max(0,owned.size()-1))/200*200;
        var rows = new ArrayList<Row>();
        for(var e:owned.stream().skip(first).limit(200).toList()){
            var visual=e.getCompoundOrEmpty("entity");
            UUID id=UUID.fromString(e.getStringOr("id",""));
            float health=e.getFloatOr("health",visual.getFloatOr("Health",-1)), maxHealth=e.getFloatOr("max_health",-1);
            for(var world:p.level().getServer().getAllLevels())if(world.getEntity(id) instanceof Mob mob){
                var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());mob.saveWithoutId(out);visual=out.buildResult();
                health=mob.getHealth();maxHealth=mob.getMaxHealth();break;
            }
            long left=e.getLongOr("ready",0)-p.level().getServer().overworld().getGameTime();
            int recovery=e.getStringOr("state","").equals("KNOCKED_OUT")&&left>0?(int)((left+19)/20):0;
            var preview=local.peepo.MobTransportCrate.preview(visual);
            String encoded=CompoundTag.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,preview).result().map(Object::toString).orElse("{}");
            String name=e.getStringOr("name","Pet");
            rows.add(new Row(e.getStringOr("id",""),name.substring(0,Math.min(80,name.length())),e.getStringOr("type",""),e.getStringOr("state","ACTIVE"),e.getStringOr("mode","FOLLOW"),PetRoster.level(e),e.getIntOr("xp",0),encoded,
                    health,maxHealth,recovery,e.getBooleanOr("own_orders",false)));
        }
        ServerPlayNetworking.send(p,new View(JSON.toJson(new Snapshot(rows,message,first,owned.size()))));
    }
    /**
     * One companion as the stable screen shows it: health is the live pet's, or how it was when stored (-1 unknown);
     * recovery is the seconds until a knocked-out pet can be deployed (0 once it can); ownOrders marks a companion that
     * keeps its own Orders menu (its mode is not set here).
     */
    public record Row(String id, String name, String type, String state, String mode, int level, int xp, String preview,
                      float health, float maxHealth, int recovery, boolean ownOrders) {}
    /** Bond points per level and the most there are (PetRoster.level: 1 + xp / 120, at most 20). */
    public static final int XP_PER_LEVEL = 120, MAX_XP = 2280;
    public record Snapshot(List<Row> pets, String message, int offset, int total) {}
    public record Action(String id, String operation) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Jugcraft.id("pet_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.ofMember((v,b)->{b.writeUtf(v.id,40);b.writeUtf(v.operation,20);},b->new Action(b.readUtf(40),b.readUtf(20)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record View(String json) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(Jugcraft.id("pet_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.ofMember((v,b)->b.writeUtf(v.json,100000),b->new View(b.readUtf(100000)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}

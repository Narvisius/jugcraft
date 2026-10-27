package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.*;

/** One original 16x32x16 room per player. Allocation is saved and never regenerates occupied rooms. */
public final class JimtaPocketRooms extends SavedData {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,Jugcraft.id("jimtafacts_pocket_dimension"));
    public static AttachmentType<Visit> VISIT;
    public static AttachmentType<List<UUID>> GUESTS;
    public static Block VOIDSTONE,FRAME;
    public record Visit(int room,GlobalPos home,Vec3 position,float yaw,float pitch){
        static final Codec<Visit> CODEC=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("room").forGetter(Visit::room),GlobalPos.CODEC.fieldOf("home").forGetter(Visit::home),Vec3.CODEC.fieldOf("position").forGetter(Visit::position),Codec.FLOAT.fieldOf("yaw").forGetter(Visit::yaw),Codec.FLOAT.fieldOf("pitch").forGetter(Visit::pitch)).apply(i,Visit::new));
    }
    private final List<UUID> owners;
    private static final Codec<JimtaPocketRooms> CODEC=UUIDUtil.CODEC.listOf().xmap(JimtaPocketRooms::new,d->List.copyOf(d.owners));
    private static final SavedDataType<JimtaPocketRooms> TYPE=new SavedDataType<>(Jugcraft.id("jimtafacts_pocket_rooms"),JimtaPocketRooms::new,CODEC,null);
    public JimtaPocketRooms(){this(List.of());}private JimtaPocketRooms(List<UUID> owners){this.owners=new ArrayList<>(owners);}
    static BlockPos origin(int room){return new BlockPos(room%64*256,32,room/64*256);}
    private static JimtaPocketRooms data(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public static void register(){
        var stoneKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_spellbooks_voidstone"));VOIDSTONE=Registry.register(BuiltInRegistries.BLOCK,stoneKey,new Stone(BlockBehaviour.Properties.of().setId(stoneKey).strength(-1,3600000)));
        var frameKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_spellbooks_pocket_dimension_portal_frame"));FRAME=Registry.register(BuiltInRegistries.BLOCK,frameKey,new Frame(BlockBehaviour.Properties.of().setId(frameKey).strength(-1,3600000).noOcclusion()));
        VISIT=AttachmentRegistry.<Visit>builder().persistent(Visit.CODEC).copyOnDeath().buildAndRegister(Jugcraft.id("jimtafacts_pocket_visit"));
        GUESTS=AttachmentRegistry.<List<UUID>>builder().persistent(UUIDUtil.CODEC.listOf(0,16)).copyOnDeath().buildAndRegister(Jugcraft.id("jimtafacts_pocket_guests"));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
            net.minecraft.commands.Commands.literal("pocketroom")
                .then(net.minecraft.commands.Commands.literal("invite").then(net.minecraft.commands.Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->invite(c.getSource().getPlayerOrException(),net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player"),false))))
                .then(net.minecraft.commands.Commands.literal("revoke").then(net.minecraft.commands.Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->invite(c.getSource().getPlayerOrException(),net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player"),true))))
                .then(net.minecraft.commands.Commands.literal("clear").executes(c->{var player=c.getSource().getPlayerOrException();player.removeAttached(GUESTS);player.sendSystemMessage(net.minecraft.network.chat.Component.literal("All pocket-room invitations revoked."));return 1;}))
                .then(net.minecraft.commands.Commands.literal("accept").then(net.minecraft.commands.Commands.argument("host",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->visit(c.getSource().getPlayerOrException(),net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"host"))?1:0)))
                .then(net.minecraft.commands.Commands.literal("leave").executes(c->{var player=c.getSource().getPlayerOrException();if(!player.level().dimension().equals(DIMENSION))return 0;leave(player);return 1;}))));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10!=0)return;for(var p:server.getPlayerList().getPlayers())if(p.level().dimension().equals(DIMENSION)){
            var v=p.getAttached(VISIT);if(v==null||v.room()<0||v.room()>=data(p.level()).owners.size()||!admitted(p,v.room())||!inside(v.room(),p.blockPosition())||!JimtaSpellbooks.enabled()){leave(p);continue;}
            if(server.getTickCount()%200==0){var start=origin(v.room()).offset(7,2,10);JimtaWardEntity.pocketPortal(p,p.level(),Vec3.atBottomCenterOf(start));}
        }});
        // Guest access is opt-in sightseeing, never a second path to another room's inventories.
        var guestPhase=Jugcraft.id("pocket_guest_access");
        UseBlockCallback.EVENT.addPhaseOrdering(guestPhase,net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(guestPhase,(p,l,h,hit)->{
            if(!readOnlyGuest(p))return InteractionResult.PASS;
            if(l.getBlockState(hit.getBlockPos()).is(FRAME)&&p instanceof ServerPlayer server){leave(server);return InteractionResult.SUCCESS;}
            return InteractionResult.FAIL;
        });
        UseItemCallback.EVENT.addPhaseOrdering(guestPhase,net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        UseItemCallback.EVENT.register(guestPhase,(p,l,h)->readOnlyGuest(p)?InteractionResult.FAIL:InteractionResult.PASS);
        UseEntityCallback.EVENT.addPhaseOrdering(guestPhase,net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.register(guestPhase,(p,l,h,e,hit)->readOnlyGuest(p)?InteractionResult.FAIL:InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((p,l,h,e,hit)->readOnlyGuest(p)?InteractionResult.FAIL:InteractionResult.PASS);
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->!(source.getEntity() instanceof Player p&&readOnlyGuest(p)));
        ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->{if(p.getAttached(VISIT)!=null)leave(p);});
        UseBlockCallback.EVENT.register((p,l,h,hit)->{if(p instanceof ServerPlayer s&&l.dimension().equals(DIMENSION)&&l.getBlockState(hit.getBlockPos()).is(FRAME)){leave(s);return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
        AttackBlockCallback.EVENT.register((p,l,h,pos,face)->{if(p instanceof ServerPlayer s&&l.dimension().equals(DIMENSION)&&l.getBlockState(pos).is(Blocks.BARRIER)){leave(s);return InteractionResult.FAIL;}return InteractionResult.PASS;});
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->!l.dimension().equals(DIMENSION)||allowed(p,pos)&&!state.is(VOIDSTONE)&&!state.is(FRAME)&&!state.is(Blocks.BARRIER));
        UseBlockCallback.EVENT.register((p,l,h,hit)->l.dimension().equals(DIMENSION)&&!allowed(p,hit.getBlockPos())?InteractionResult.FAIL:InteractionResult.PASS);
    }
    private static boolean inside(int room,BlockPos pos){var c=origin(room);return pos.getX()>=c.getX()&&pos.getX()<c.getX()+16&&pos.getY()>c.getY()&&pos.getY()<c.getY()+32&&pos.getZ()>=c.getZ()&&pos.getZ()<c.getZ()+16;}
    private static boolean allowed(Player p,BlockPos pos){var v=p.getAttached(VISIT);return p instanceof ServerPlayer sp&&v!=null&&inside(v.room(),pos)&&owns(sp,v.room());}
    public static boolean readOnlyGuest(Player p){
        if(!(p instanceof ServerPlayer server)||!p.level().dimension().equals(DIMENSION)||VISIT==null)return false;
        var visit=p.getAttached(VISIT);return visit==null||!owns(server,visit.room());
    }
    private static boolean owns(ServerPlayer player,int room){var owners=data(player.level()).owners;return room>=0&&room<owners.size()&&owners.get(room).equals(player.getUUID());}
    private static boolean admitted(ServerPlayer player,int room){
        if(owns(player,room))return true;var owners=data(player.level()).owners;if(room<0||room>=owners.size())return false;
        var host=player.level().getServer().getPlayerList().getPlayer(owners.get(room));
        if(host==null||!host.isAlive()||!host.level().dimension().equals(DIMENSION)||!inside(room,host.blockPosition()))return false;
        return host.getAttachedOrElse(GUESTS,List.of()).contains(player.getUUID());
    }
    public static int invite(ServerPlayer host,ServerPlayer guest,boolean revoke){
        if(host==guest||!host.isAlive()||host.isSpectator()||!JimtaSpellbooks.enabled())return 0;
        var guests=new ArrayList<>(host.getAttachedOrElse(GUESTS,List.<UUID>of()));
        if(revoke){if(!guests.remove(guest.getUUID()))return 0;}
        else {if(guests.contains(guest.getUUID())||guests.size()>=16)return 0;guests.add(guest.getUUID());}
        host.setAttached(GUESTS,List.copyOf(guests));
        host.sendSystemMessage(net.minecraft.network.chat.Component.literal(revoke?"Pocket-room invitation revoked.":"Pocket-room invitation sent. Guests can visit while you are inside your room."));
        if(!revoke)guest.sendSystemMessage(net.minecraft.network.chat.Component.literal(host.getName().getString()+" invited you to visit their pocket room. Use /pocketroom accept "+host.getName().getString()+". Visits do not grant building or container access."));return 1;
    }
    public static boolean visit(ServerPlayer guest,ServerPlayer host){
        if(guest==host||!guest.isAlive()||guest.isSpectator()||guest.isPassenger()||guest.isVehicle()||guest.level().dimension().equals(DIMENSION)||!JimtaSpellbooks.enabled()
                ||!JimtaWardSpells.outOfCombat(guest)||!Authority.mayChange(guest.level(),guest,guest.blockPosition()))return false;
        var data=data(host.level());int room=data.owners.indexOf(host.getUUID());if(room<0||!admitted(guest,room))return false;
        var pocket=host.level();var origin=origin(room).offset(7,2,9);pocket.getChunk(origin);Vec3 arrival=null;
        for(var offset:List.of(BlockPos.ZERO,new BlockPos(1,0,0),new BlockPos(-1,0,0),new BlockPos(0,0,1),new BlockPos(0,0,-1))){
            var candidate=Vec3.atBottomCenterOf(origin.offset(offset));if(safeGuestArrival(guest,pocket,room,candidate)){arrival=candidate;break;}
        }
        if(arrival==null)return false;
        guest.closeContainer();var old=guest.getAttached(VISIT);var visit=new Visit(room,GlobalPos.of(guest.level().dimension(),guest.blockPosition()),guest.position(),guest.getYRot(),guest.getXRot());guest.setAttached(VISIT,visit);
        if(!guest.teleportTo(pocket,arrival.x,arrival.y,arrival.z,Set.of(),180,0,true)){if(old==null)guest.removeAttached(VISIT);else guest.setAttached(VISIT,old);return false;}
        JimtaWardEntity.travelCooldown(guest);guest.resetFallDistance();JimtaSpellbookCurios.afterTeleport(guest);return true;
    }
    /** Invitation authorizes presence, not block editing; the ordinary teleport helper requires edit rights. */
    private static boolean safeGuestArrival(ServerPlayer guest,ServerLevel pocket,int room,Vec3 at){
        if(!pocket.dimension().equals(DIMENSION)||!admitted(guest,room))return false;
        var box=guest.getBoundingBox().move(at.subtract(guest.position()));
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!inside(room,pos)||!pocket.hasChunkAt(pos)||!pocket.isInWorldBounds(pos)||!pocket.getWorldBorder().isWithinBounds(pos))return false;
        return pocket.noCollision(guest,box)&&!pocket.containsAnyLiquid(box);
    }
    public static boolean enter(ServerPlayer p){
        if(p.level().dimension().equals(DIMENSION)||p.isPassenger()||!JimtaWardSpells.outOfCombat(p)||!Authority.mayChange(p.level(),p,p.blockPosition()))return false;
        var pocket=p.level().getServer().getLevel(DIMENSION);if(pocket==null)return false;var data=data(p.level());int index=data.owners.indexOf(p.getUUID());
        if(index<0){
            if(data.owners.size()>=2048)return false;index=data.owners.size();var start=origin(index);
            var template=pocket.getStructureTemplateManager().get(Jugcraft.id("jimtafacts/pocket_room")).orElse(null);
            if(template==null||template.getSize().getX()!=16||template.getSize().getY()!=32||template.getSize().getZ()!=16)return false;
            // Explicit first-entry allocation: exactly one original room, no ongoing chunk loader.
            pocket.getChunk(start);if(!template.placeInWorld(pocket,start,start,new StructurePlaceSettings().setIgnoreEntities(true),pocket.getRandom(),Block.UPDATE_CLIENTS))return false;
            data.owners.add(p.getUUID());data.setDirty();
        }
        var arrival=Vec3.atBottomCenterOf(origin(index).offset(7,2,9));pocket.getChunk(BlockPos.containing(arrival));
        if(!pocket.noCollision(p,p.getBoundingBox().move(arrival.subtract(p.position()))))return false;
        var visit=new Visit(index,GlobalPos.of(p.level().dimension(),p.blockPosition()),p.position(),p.getYRot(),p.getXRot());
        JimtaWardEntity.pocketPortal(p,p.level(),p.position());p.setAttached(VISIT,visit);JimtaWardEntity.travelCooldown(p);
        if(!p.teleportTo(pocket,arrival.x,arrival.y,arrival.z,Set.of(),180,0,true)){p.removeAttached(VISIT);return false;}
        JimtaWardEntity.pocketPortal(p,pocket,Vec3.atBottomCenterOf(origin(index).offset(7,2,10)));p.resetFallDistance();JimtaSpellbookCurios.afterTeleport(p);return true;
    }
    public static void leave(ServerPlayer p){
        JimtaWardEntity.travelCooldown(p);
        var visit=p.getAttached(VISIT);var target=visit==null?null:p.level().getServer().getLevel(visit.home().dimension());
        if(target==null||target.dimension().equals(DIMENSION))target=p.level().getServer().overworld();
        Vec3 point=visit!=null&&target.dimension().equals(visit.home().dimension())?visit.position():Vec3.atBottomCenterOf(target.getRespawnData().pos());target.getChunk(BlockPos.containing(point));
        Vec3 safe=null;for(int y=0;y<12;y++){var at=point.add(0,y,0);if(JimtaWardSpells.safe(p,target,at)){safe=at;break;}}
        if(safe==null){var fallback=p.findRespawnPositionAndUseSpawnBlock(true,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);if(fallback==null)return;if(p.teleport(fallback)!=null){p.removeAttached(VISIT);JimtaSpellbookCurios.afterTeleport(p);}return;}
        if(p.teleportTo(target,safe.x,safe.y,safe.z,Set.of(),visit==null?0:visit.yaw(),visit==null?0:visit.pitch(),true)){p.removeAttached(VISIT);p.resetFallDistance();JimtaSpellbookCurios.afterTeleport(p);}
    }
    private static final class Stone extends Block {
        Stone(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.NORTH,false).setValue(BlockStateProperties.EAST,false).setValue(BlockStateProperties.SOUTH,false).setValue(BlockStateProperties.WEST,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.NORTH,BlockStateProperties.EAST,BlockStateProperties.SOUTH,BlockStateProperties.WEST);}
    }
    private static final class Frame extends JimtaPortalFrames.Frame {
        Frame(Properties p){super(p);}
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return null;}
        @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return null;}
    }
}

package io.github.jimbozoomer.jugcraft.server;

import com.mojang.serialization.Codec;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.*;

/** Persistent administrator destinations; ordinary commands never grant operator authority. */
public final class ServerUtilities extends SavedData {
    final Map<String,CompoundTag> records=new LinkedHashMap<>();
    public static final Codec<ServerUtilities> CODEC=CompoundTag.CODEC.listOf().fieldOf("records").codec().xmap(list->{var d=new ServerUtilities();for(var t:list)d.records.put(t.getStringOr("key",""),t.copy());return d;},d->d.records.values().stream().map(CompoundTag::copy).toList());
    private static final SavedDataType<ServerUtilities> TYPE=new SavedDataType<>(Jugcraft.id("server_utilities"),ServerUtilities::new,CODEC,null);
    public static ServerUtilities get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public static boolean op(CommandSourceStack s){return Commands.LEVEL_GAMEMASTERS.check(s.permissions());}
    public static void tell(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    public static void register(){
        CommandRegistrationCallback.EVENT.register((d,a,e)->{
            d.register(Commands.literal("warp").executes(c->{c.getSource().sendSuccess(()->Component.literal("Warps: "+String.join(", ",get(c.getSource().getServer()).records.keySet().stream().filter(k->k.startsWith("warp:")).map(k->k.substring(5)).sorted().toList())),false);return 1;}).then(Commands.argument("name",StringArgumentType.word()).suggests((c,b)->{get(c.getSource().getServer()).records.keySet().stream().filter(k->k.startsWith("warp:")).forEach(k->b.suggest(k.substring(5)));return b.buildFuture();}).executes(c->travel(c.getSource().getPlayerOrException(),"warp:"+StringArgumentType.getString(c,"name")))));
            d.register(Commands.literal("spawn").executes(c->travel(c.getSource().getPlayerOrException(),"spawn")));
            d.register(Commands.literal("setwarp").requires(ServerUtilities::op).then(Commands.argument("name",StringArgumentType.word()).executes(c->save(c.getSource().getPlayerOrException(),"warp:"+StringArgumentType.getString(c,"name")))));
            d.register(Commands.literal("delwarp").requires(ServerUtilities::op).then(Commands.argument("name",StringArgumentType.word()).executes(c->{var data=get(c.getSource().getServer());data.records.remove("warp:"+StringArgumentType.getString(c,"name"));data.setDirty();return 1;})));
            d.register(Commands.literal("setspawn").requires(ServerUtilities::op).executes(c->save(c.getSource().getPlayerOrException(),"spawn")));
            d.register(Commands.literal("god").requires(ServerUtilities::op).executes(c->god(c.getSource().getPlayerOrException())).then(Commands.argument("player",EntityArgument.player()).executes(c->god(EntityArgument.getPlayer(c,"player")))));
            d.register(Commands.literal("invsee").requires(ServerUtilities::op).then(Commands.argument("player",EntityArgument.player()).executes(c->{inspect(c.getSource().getPlayerOrException(),EntityArgument.getPlayer(c,"player"));return 1;})));
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((v,s,n)->!(v instanceof ServerPlayer p)||!get(p.level().getServer()).records.containsKey("god:"+p.getUUID()));
        NearbyTrade.register(); OperatorRegions.register();
    }
    static int god(ServerPlayer p){var data=get(p.level().getServer());String key="god:"+p.getUUID();boolean on=data.records.remove(key)==null;if(on){var t=new CompoundTag();t.putString("key",key);data.records.put(key,t);}data.setDirty();tell(p,"God mode "+(on?"ON":"OFF"));return 1;}
    public static int save(ServerPlayer p,String key){if(key.length()>70||!key.matches("[a-zA-Z0-9_:-]+")){tell(p,"Use a short name containing letters, numbers, _ or -.");return 0;}var t=new CompoundTag();t.putString("key",key);t.putString("dimension",p.level().dimension().identifier().toString());t.putDouble("x",p.getX());t.putDouble("y",p.getY());t.putDouble("z",p.getZ());t.putFloat("yaw",p.getYRot());t.putFloat("pitch",p.getXRot());var d=get(p.level().getServer());d.records.put(key,t);d.setDirty();tell(p,"Saved "+key);return 1;}
    public static int travel(ServerPlayer p,String key){
        if(io.github.jimbozoomer.jugcraft.lair.Lairs.keepsBelongings(p)||!p.isAlive()||p.isPassenger()||p.isVehicle()||p.isSpectator()||!io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(p,"public_warp",100)){tell(p,"Cannot teleport now; dismount and wait five seconds between trips.");return 0;}
        var s=p.level().getServer();var t=get(s).records.get(key);var level=s.overworld();double x,y,z;float yaw=0,pitch=0;
        if(t==null){if(!key.equals("spawn")){tell(p,"Unknown warp. Use /warp to list destinations.");return 0;}var at=level.getRespawnData().pos();x=at.getX()+.5;y=at.getY();z=at.getZ()+.5;}
        else{var id=Identifier.tryParse(t.getStringOr("dimension",""));if(id==null)return 0;level=s.getLevel(ResourceKey.create(Registries.DIMENSION,id));if(level==null){tell(p,"Destination dimension unavailable.");return 0;}x=t.getDoubleOr("x",0);y=t.getDoubleOr("y",0);z=t.getDoubleOr("z",0);yaw=t.getFloatOr("yaw",0);pitch=t.getFloatOr("pitch",0);}
        var pos=BlockPos.containing(x,y,z);level.getChunkAt(pos);var box=p.getBoundingBox().move(x-p.getX(),y-p.getY(),z-p.getZ());
        if(!level.getWorldBorder().isWithinBounds(pos)||!level.noCollision(p,box)||level.containsAnyLiquid(box)||level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty()){tell(p,"Destination is obstructed or unsafe; ask an operator to reset it.");return 0;}
        NearbyTrade.cancel(p);p.closeContainer();p.teleportTo(level,x,y,z,Set.of(),yaw,pitch,true);p.resetFallDistance();return 1;
    }
    static void inspect(ServerPlayer viewer,ServerPlayer target){
        var contents=new SimpleContainer(54);for(int i=0;i<Math.min(54,target.getInventory().getContainerSize());i++)contents.setItem(i,target.getInventory().getItem(i).copy());
        viewer.openMenu(new SimpleMenuProvider((id,inv,p)->new ChestMenu(MenuType.GENERIC_9x6,id,inv,contents,6){
            @Override public void clicked(int slot,int button,ContainerInput type,Player player){} // Read-only, including hotkeys/creative cloning.
            @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(Player player){return op(viewer.createCommandSourceStack())&&target.isAlive()&&!target.hasDisconnected();}
        },Component.literal(target.getName().getString()+" inventory (read-only snapshot)")));
    }
}

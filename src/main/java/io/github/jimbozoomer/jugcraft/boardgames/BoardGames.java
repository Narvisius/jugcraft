package io.github.jimbozoomer.jugcraft.boardgames;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class BoardGames {
 public static TableBlock CHECKERS,CHESS;
 public static BlockEntityType<TableEntity> ENTITY;
 public static final com.google.gson.Gson JSON=new com.google.gson.Gson();
 public static void register(){
  net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->{if(entity instanceof TableEntity t&&t.funded()){if(player instanceof net.minecraft.server.level.ServerPlayer user)user.sendSystemMessage(net.minecraft.network.chat.Component.literal("Finish or resign the wagered game before breaking its table."));return false;}return true;});
  CHECKERS=block("checkers_table",BoardPosition.Kind.CHECKERS);CHESS=block("chess_table",BoardPosition.Kind.CHESS);
  ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("board_game_table"),FabricBlockEntityTypeBuilder.create(TableEntity::new,CHECKERS,CHESS).build());
  PayloadTypeRegistry.clientboundPlay().register(View.TYPE,View.CODEC);PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(p,c)->{var user=c.player();var pos=BlockPos.of(p.pos);if(user.level().hasChunkAt(pos)&&user.level().getBlockEntity(pos) instanceof TableEntity table)table.act(user,p);});
 }
 private static TableBlock block(String name,BoardPosition.Kind kind){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(name));var b=Registry.register(BuiltInRegistries.BLOCK,key,new TableBlock(BlockBehaviour.Properties.of().setId(key).strength(2).explosionResistance(3600000).sound(SoundType.WOOD).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE),kind));
  var ik=ResourceKey.create(Registries.ITEM,Jugcraft.id(name));var item=Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(b,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o->o.accept(item));return b;
 }
 public record View(long pos,boolean open,String json) implements CustomPacketPayload {
  public static final Type<View> TYPE=new Type<>(Jugcraft.id("board_view"));
  public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=StreamCodec.ofMember((v,b)->{b.writeLong(v.pos);b.writeBoolean(v.open);b.writeUtf(v.json,30000);},b->new View(b.readLong(),b.readBoolean(),b.readUtf(30000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Action(long pos,String instance,int revision,int action,int from,int to,int promotion) implements CustomPacketPayload {
  public static final Type<Action> TYPE=new Type<>(Jugcraft.id("board_action"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.ofMember((v,b)->{b.writeLong(v.pos);b.writeUtf(v.instance,40);b.writeInt(v.revision);b.writeInt(v.action);b.writeInt(v.from);b.writeInt(v.to);b.writeInt(v.promotion);},b->new Action(b.readLong(),b.readUtf(40),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt()));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Snapshot(String instance,int revision,BoardPosition position,String[] names,int seat,boolean[] ready,int drawOffer,boolean active,java.util.List<BoardPosition.Move> moves,int stake,boolean[] agreed,boolean funded,String wagerMessage){}
}

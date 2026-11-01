package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/** Native book editor, server-owned session and filtered, compare-before-write lectern updates. */
public final class OwnerLecternEditing {
    private static final Map<UUID,Edit> EDITS=new HashMap<>();
    private static final class Edit {
        final UUID token=UUID.randomUUID();final LecternBlockEntity lectern;final ItemStack actual,snapshot;
        final long expires=System.nanoTime()+600_000_000_000L;boolean pending;
        Edit(LecternBlockEntity lectern){this.lectern=lectern;actual=lectern.getBook();snapshot=actual.copy();}
    }
    public record Open(UUID token,BlockPos pos,WritableBookContent contents,List<Integer> styles) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(Jugcraft.id("amends_lectern_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeBlockPos(p.pos);WritableBookContent.STREAM_CODEC.encode(b,p.contents);writeStyles(b,p.styles);},b->new Open(b.readUUID(),b.readBlockPos(),WritableBookContent.STREAM_CODEC.decode(b),readStyles(b)));
        public Type<Open> type(){return TYPE;}
    }
    public record Save(UUID token,List<String> pages,Optional<String> title,List<Integer> styles) implements CustomPacketPayload {
        public static final Type<Save> TYPE=new Type<>(Jugcraft.id("amends_lectern_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Save> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeVarInt(p.pages.size());for(var page:p.pages)b.writeUtf(page,1024);b.writeBoolean(p.title.isPresent());p.title.ifPresent(t->b.writeUtf(t,32));writeStyles(b,p.styles);},b->{var token=b.readUUID();int size=b.readVarInt();if(size<0||size>100)throw new IllegalArgumentException("Invalid book page count");var pages=new ArrayList<String>();for(int i=0;i<size;i++)pages.add(b.readUtf(1024));return new Save(token,List.copyOf(pages),b.readBoolean()?Optional.of(b.readUtf(32)):Optional.empty(),readStyles(b));});
        public Type<Save> type(){return TYPE;}
    }
    private OwnerLecternEditing(){}
    private static void writeStyles(RegistryFriendlyByteBuf b,List<Integer> values){b.writeVarInt(values.size());for(int v:values)b.writeVarInt(v);}
    private static List<Integer> readStyles(RegistryFriendlyByteBuf b){int size=b.readVarInt();if(size<0||size>100)throw new IllegalArgumentException("Invalid book style count");var values=new ArrayList<Integer>();for(int i=0;i<size;i++)values.add(Math.clamp(b.readVarInt(),0,95));return List.copyOf(values);}
    public static net.minecraft.network.chat.Style style(int value){
        int color=Math.floorMod(value,16),font=Math.clamp(value/16,0,5);
        return net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.ChatFormatting.getByCode("0123456789abcdef".charAt(color))).withBold(font==2).withItalic(font==1).withUnderlined(font==3).withStrikethrough(font==4).withObfuscated(font==5);
    }
    private static List<Integer> styles(ItemStack book){var raw=book.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntArray("JugcraftPageStyles").orElse(new int[0]);return java.util.Arrays.stream(raw).limit(100).map(v->Math.clamp(v,0,95)).boxed().toList();}
    public static void register(){
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);PayloadTypeRegistry.serverboundPlay().register(Save.TYPE,Save.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Save.TYPE,(p,c)->save(c.player(),p));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->EDITS.remove(h.player.getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->EDITS.clear());
    }
    public static boolean open(ServerPlayer player,LecternBlockEntity lectern){
        var edit=new Edit(lectern);if(!valid(player,edit))return false;
        EDITS.put(player.getUUID(),edit);ServerPlayNetworking.send(player,new Open(edit.token,lectern.getBlockPos(),edit.snapshot.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT,WritableBookContent.EMPTY),styles(edit.snapshot)));return true;
    }
    private static boolean valid(ServerPlayer player,Edit edit){
        var lectern=edit.lectern;return System.nanoTime()<edit.expires&&player.isAlive()&&!player.isSpectator()&&player.connection.isAcceptingMessages()
                &&lectern.getLevel()==player.level()&&!lectern.isRemoved()&&player.level().hasChunkAt(lectern.getBlockPos())&&player.level().getBlockEntity(lectern.getBlockPos())==lectern
                &&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(lectern.getBlockPos()))<=64&&Menagerie.mayChange(player.level(),player,lectern.getBlockPos())
                &&lectern.getBook()==edit.actual&&edit.actual.is(Items.WRITABLE_BOOK)&&ItemStack.matches(edit.snapshot,edit.actual);
    }
    private static void save(ServerPlayer player,Save packet){
        var edit=EDITS.get(player.getUUID());if(edit==null||edit.pending||!edit.token.equals(packet.token))return;
        if(!valid(player,edit)||packet.title.isPresent()&&packet.title.get().isBlank()){EDITS.remove(player.getUUID());player.sendSystemMessage(Component.literal("Book not saved: the lectern changed, you moved away, or the title is empty."));return;}
        edit.pending=true;var lines=new ArrayList<>(packet.pages);packet.title.ifPresent(lines::add);
        player.getTextFilter().processMessageBundle(lines).whenCompleteAsync((filtered,error)->{
            if(EDITS.get(player.getUUID())!=edit)return;
            EDITS.remove(player.getUUID());
            if(error!=null||filtered==null||filtered.size()!=lines.size()||!valid(player,edit)){player.sendSystemMessage(Component.literal("Book not saved: filtering failed or the lectern changed. Your original book is intact."));return;}
            var pages=new ArrayList<Filterable<String>>();for(int i=0;i<packet.pages.size();i++)pages.add(filtered(player,filtered.get(i)));
            ItemStack updated=edit.actual.copy();
            var styling=updated.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();styling.putIntArray("JugcraftPageStyles",packet.styles.stream().mapToInt(v->v).toArray());updated.set(DataComponents.CUSTOM_DATA,CustomData.of(styling));
            if(packet.title.isPresent()){
                updated=updated.transmuteCopy(Items.WRITTEN_BOOK);updated.remove(DataComponents.WRITABLE_BOOK_CONTENT);
                updated.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(filtered(player,filtered.getLast()),player.getName().getString(),0,java.util.stream.IntStream.range(0,pages.size()).mapToObj(i->pages.get(i).map(text->(Component)Component.literal(text).setStyle(style(i<packet.styles.size()?packet.styles.get(i):0)))).toList(),true));
            }else updated.set(DataComponents.WRITABLE_BOOK_CONTENT,new WritableBookContent(List.copyOf(pages)));
            edit.lectern.setBook(updated,player);edit.lectern.setChanged();var pos=edit.lectern.getBlockPos();var state=edit.lectern.getBlockState();player.level().sendBlockUpdated(pos,state,state,3);player.level().updateNeighbourForOutputSignal(pos,state.getBlock());
            player.sendOverlayMessage(Component.literal(packet.title.isPresent()?"Lectern book signed.":"Lectern book saved."));
        },player.level().getServer());
    }
    private static Filterable<String> filtered(ServerPlayer player,FilteredText text){return player.isTextFilteringEnabled()?Filterable.passThrough(text.filteredOrEmpty()):Filterable.from(text);}
}

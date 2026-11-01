package io.github.jimbozoomer.jugcraft.menagerie;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
/** Source antique glyphs on signs and written books, without replacing text or ownership. */
public final class OwnerAntiqueInk extends Item implements SignApplicator {
 public static final FontDescription FONT=new FontDescription.Resource(Jugcraft.id("newest/supps/antiquable"));
 public OwnerAntiqueInk(Properties p){super(p);}
 public static Component ink(Component text,boolean enabled){return text.copy().withStyle(s->s.withFont(enabled?FONT:Style.EMPTY.getFont()));}
 @Override public boolean tryApplyToSign(Level level,SignBlockEntity sign,SignTextSlot side,ItemStack item,Player player){if(sign.isWaxed()||!Menagerie.mayChange(level,player,sign.getBlockPos()))return false;var old=sign.getText(side);var next=new SignText(old.getMessages(false).stream().map(c->ink(c,true)).toList(),old.getMessages(true).stream().map(c->ink(c,true)).toList(),old.getColor(),old.hasGlowingText());if(old.equals(next))return false;sign.setText(next,side);return true;}
 static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("supps_antique_book"),Book.SERIALIZER);net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,l,h,hit)->{
  var held=p.getItemInHand(h);
  if(l.getBlockEntity(hit.getBlockPos()) instanceof OwnerSupplementData data&&java.util.Set.of("way_sign","doormat","text","speaker").contains(data.block().function)){
   boolean apply=held.is(OwnerSupplementaries.ITEMS.get("antique_ink")),clear=held.is(Items.INK_SAC)||held.is(OwnerSupplementaries.ITEMS.get("soap"));
   if((apply||clear)&&data.antiqueText!=apply){if(data.boardWaxed||!data.mayEdit(p))return net.minecraft.world.InteractionResult.FAIL;if(!l.isClientSide()){data.antiqueText=apply;data.boardRevision++;data.changed();held.consume(1,p);}return net.minecraft.world.InteractionResult.SUCCESS;}
  }
  if(!held.is(Items.INK_SAC)&&!held.is(OwnerSupplementaries.ITEMS.get("soap")))return net.minecraft.world.InteractionResult.PASS;
  if(!(l.getBlockEntity(hit.getBlockPos()) instanceof SignBlockEntity sign)||sign.isWaxed())return net.minecraft.world.InteractionResult.PASS;var side=sign.getSlotPlayerIsFacing(p);var text=sign.getText(side);if(text.getMessages(false).stream().noneMatch(c->c.getStyle().getFont().equals(FONT)))return net.minecraft.world.InteractionResult.PASS;
  if(!Menagerie.mayChange(l,p,hit.getBlockPos()))return net.minecraft.world.InteractionResult.FAIL;if(!l.isClientSide()){sign.setText(new SignText(text.getMessages(false).stream().map(c->ink(c,false)).toList(),text.getMessages(true).stream().map(c->ink(c,false)).toList(),text.getColor(),text.hasGlowingText()),side);held.consume(1,p);}return net.minecraft.world.InteractionResult.SUCCESS;
 });}
 public static final class Book extends CustomRecipe {
  static final Book INSTANCE=new Book();static final RecipeSerializer<Book> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Book>unit(INSTANCE));
  ItemStack result(CraftingInput input){ItemStack book=ItemStack.EMPTY,ink=ItemStack.EMPTY;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(book.isEmpty()&&(s.has(DataComponents.WRITTEN_BOOK_CONTENT)||s.has(DataComponents.MAP_ID)))book=s;else if(ink.isEmpty()&&(s.is(OwnerSupplementaries.ITEMS.get("antique_ink"))||s.is(OwnerSupplementaries.ITEMS.get("soap"))))ink=s;else return ItemStack.EMPTY;}if(book.isEmpty()||ink.isEmpty())return ItemStack.EMPTY;
   boolean enabled=ink.is(OwnerSupplementaries.ITEMS.get("antique_ink"));
   if(book.has(DataComponents.MAP_ID)){
    var tag=book.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(tag.getBooleanOr("JugcraftAntiqueMap",false)==enabled)return ItemStack.EMPTY;
    var out=book.copyWithCount(1);tag.putBoolean("JugcraftAntiqueMap",enabled);out.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    if(enabled)out.set(DataComponents.ITEM_MODEL,Jugcraft.id("supps_antique_map"));else if(Jugcraft.id("supps_antique_map").equals(out.get(DataComponents.ITEM_MODEL)))out.remove(DataComponents.ITEM_MODEL);return out;
   }
   var contents=book.get(DataComponents.WRITTEN_BOOK_CONTENT);boolean before=contents.pages().stream().anyMatch(p->p.raw().getStyle().getFont().equals(FONT));if(before==enabled)return ItemStack.EMPTY;var out=book.copyWithCount(1);out.set(DataComponents.WRITTEN_BOOK_CONTENT,contents.withReplacedPages(contents.pages().stream().map(p->p.map(c->OwnerAntiqueInk.ink(c,enabled))).toList()));return out;
  }
  @Override public boolean matches(CraftingInput i,Level l){return !result(i).isEmpty();}
  @Override public ItemStack assemble(CraftingInput i){return result(i);}
  @Override public RecipeSerializer<Book> getSerializer(){return SERIALIZER;}
 }
}

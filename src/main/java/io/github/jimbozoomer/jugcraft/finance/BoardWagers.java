package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.*;

/** Exchange-backed, idempotent board-game escrow. Latest receipt per placed table survives chunk rollback. */
public final class BoardWagers {
 public static final int[] STAKES={0,1,5,10,25,50};
 public static final long EXPIRY_MILLIS=7L*24*60*60*1000;
 public record Ticket(String id,UUID white,UUID black,long stake,long expires,boolean closed) {}
 public static final Codec<Ticket> CODEC=RecordCodecBuilder.create(i->i.group(
  Codec.STRING.fieldOf("id").forGetter(Ticket::id),UUIDUtil.CODEC.fieldOf("white").forGetter(Ticket::white),
  UUIDUtil.CODEC.fieldOf("black").forGetter(Ticket::black),Codec.LONG.validate(v->v>0&&v<=50?com.mojang.serialization.DataResult.success(v):com.mojang.serialization.DataResult.error(()->"Invalid board stake")).fieldOf("stake").forGetter(Ticket::stake),
  Codec.LONG.fieldOf("expires").forGetter(Ticket::expires),Codec.BOOL.fieldOf("closed").forGetter(Ticket::closed)
 ).apply(i,Ticket::new));
 private final Map<String,Ticket> receipts=new HashMap<>();
 public Map<String,Ticket> receipts(){return Collections.unmodifiableMap(receipts);}
 public void restore(Map<String,Ticket> saved){receipts.putAll(saved);}
 public boolean open(String key,String id){var t=receipts.get(key);return t!=null&&t.id.equals(id)&&!t.closed;}
 public FinanceBook.Result fund(FinanceBook book,String key,String id,UUID white,UUID black,int stake,long day,long now){
  var previous=receipts.get(key);
  if(white.equals(black)||stake<=0||Arrays.stream(STAKES).noneMatch(v->v==stake)||(previous!=null&&(!previous.closed||previous.id.equals(id))))return FinanceBook.Result.fail("This wager cannot be funded.");
  var first=book.stake(white,stake,day);if(!first.ok())return first;
  var second=book.stake(black,stake,day);if(!second.ok()){book.refundStake(white,stake,day);return second;}
  receipts.put(key,new Ticket(id,white,black,stake,now+EXPIRY_MILLIS,false));return new FinanceBook.Result(true,"");
 }
 /** winner -1 refunds; 0/1 awards exactly the pot, never a house subsidy. */
 public boolean settle(FinanceBook book,String key,String id,int winner){
  var t=receipts.get(key);if(t==null||t.closed||!t.id.equals(id)||winner < -1||winner>1||book.escrow()<t.stake*2)return false;
  if(winner<0){book.escrowTo(t.white,t.stake);book.escrowTo(t.black,t.stake);}else book.escrowTo(winner==0?t.white:t.black,t.stake*2);
  receipts.put(key,new Ticket(t.id,t.white,t.black,t.stake,t.expires,true));return true;
 }
 public boolean cancelLocation(FinanceBook book,String prefix){boolean changed=false;for(var e:new ArrayList<>(receipts.entrySet()))if(e.getKey().startsWith(prefix))changed|=settle(book,e.getKey(),e.getValue().id,-1);return changed;}
 public boolean expire(FinanceBook book,long now){boolean changed=false;for(var e:new ArrayList<>(receipts.entrySet()))if(!e.getValue().closed&&now>=e.getValue().expires)changed|=settle(book,e.getKey(),e.getValue().id,-1);return changed;}
}

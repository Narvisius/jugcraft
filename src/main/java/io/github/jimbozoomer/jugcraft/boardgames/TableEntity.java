package io.github.jimbozoomer.jugcraft.boardgames;
import java.util.*;
import io.github.jimbozoomer.jugcraft.finance.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Persistent board seats; optional wagers use the shared Exchange ledger. */
public final class TableEntity extends BlockEntity {
 public BoardPosition game;
 public String instance=UUID.randomUUID().toString();
 public String[] seats={"",""},names={"Open seat","Open seat"};
 public boolean[] ready={false,false};
 public int revision,drawOffer=-1;
 public int stake;public String betId="",wagerMessage="";public boolean[] agreed={false,false};
 public String wagerKey(){return locationPrefix(level,worldPosition)+instance;}
 public static String locationPrefix(net.minecraft.world.level.Level l,BlockPos p){return l.dimension().identifier()+"/"+p.asLong()+"/";}
 public boolean funded(){return !betId.isEmpty()&&game.result.isEmpty();}
 private void resetWager(){stake=0;betId="";wagerMessage="";Arrays.fill(agreed,false);}
 private void settle(){if(level==null||level.isClientSide()||betId.isEmpty()||game.result.isEmpty())return;var ledger=Ledger.get(level.getServer());int winner=game.result.startsWith("White wins")?0:game.result.startsWith("Black wins")?1:-1;if(ledger.boardWagers.settle(ledger.book(),wagerKey(),betId,winner)){ledger.setDirty();wagerMessage=winner<0?"Both stakes refunded":(stake*2)+" Jugs paid to "+(winner==0?"White":"Black");}}
 private boolean wagerAction(int seat,int action){
  if(game.ply!=0||!game.result.isEmpty()||!betId.isEmpty())return false;
  if(action==8){int i=0;while(i<BoardWagers.STAKES.length&&BoardWagers.STAKES[i]!=stake)i++;stake=BoardWagers.STAKES[(i+1)%BoardWagers.STAKES.length];Arrays.fill(agreed,false);wagerMessage="";return true;}
  if(action==10){resetWager();return true;}
  if(action!=9||stake<=0||seats[0].isEmpty()||seats[1].isEmpty()||agreed[seat]||level==null||level.isClientSide())return false;
  agreed[seat]=true;wagerMessage="Waiting for both confirmations";
  if(agreed[0]&&agreed[1]){var server=level.getServer();for(String id:seats){var player=server.getPlayerList().getPlayer(UUID.fromString(id));if(player==null||!allowed(player)){Arrays.fill(agreed,false);wagerMessage="Both players must be nearby";return true;}}
   var ledger=Ledger.get(server);String next=UUID.randomUUID().toString();var result=ledger.boardWagers.fund(ledger.book(),wagerKey(),next,UUID.fromString(seats[0]),UUID.fromString(seats[1]),stake,FinanceService.day(server),System.currentTimeMillis());ledger.setDirty();
   if(result.ok()){betId=next;wagerMessage="Pot: "+stake*2+" Jugs";}else{Arrays.fill(agreed,false);wagerMessage=result.message();}
  }return true;
 }
 private final Set<UUID> watchers=new HashSet<>();
 private final Map<UUID,Long> throttle=new HashMap<>();
 private int ticks;
 public TableEntity(BlockPos p,BlockState s){super(BoardGames.ENTITY,p,s);game=new BoardPosition(((TableBlock)s.getBlock()).kind);}
 public int seat(UUID id){for(int i=0;i<2;i++)if(seats[i].equals(id.toString()))return i;return -1;}
 public boolean active(){return !seats[0].isEmpty()&&!seats[1].isEmpty()&&game.result.isEmpty()&&(stake==0||!betId.isEmpty());}
 private boolean allowed(ServerPlayer p){return !isRemoved()&&p.isAlive()&&p.level()==level&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64&&!p.isSpectator()&&io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(p,level,worldPosition);}
 public void open(ServerPlayer p){if(!allowed(p))return;watchers.add(p.getUUID());send(p,true);}
 public BoardGames.Snapshot snapshot(UUID id){var view=game.copy();view.repetitions.clear();return new BoardGames.Snapshot(instance,revision,view,names.clone(),seat(id),ready.clone(),drawOffer,active(),active()?game.legal():List.of(),stake,agreed.clone(),funded(),wagerMessage);}
 private void send(ServerPlayer p,boolean open){if(ServerPlayNetworking.canSend(p,BoardGames.View.TYPE))ServerPlayNetworking.send(p,new BoardGames.View(worldPosition.asLong(),open,BoardGames.JSON.toJson(snapshot(p.getUUID()))));}
 public void act(ServerPlayer p,BoardGames.Action a){if(!allowed(p)||!watchers.contains(p.getUUID())||!instance.equals(a.instance()))return;
  if(a.action()==7){watchers.remove(p.getUUID());throttle.remove(p.getUUID());return;}
  if(a.revision()!=revision){send(p,false);return;}
  long now=level.getGameTime();if(throttle.getOrDefault(p.getUUID(),-1L)==now)return;throttle.put(p.getUUID(),now);
  if(change(p.getUUID(),p.getName().getString(),a.action(),a.from(),a.to(),a.promotion())){revision++;setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);broadcast();}
 }
 /** The identity-checked state transition is shared by network handling and regression tests. */
 public boolean change(UUID who,String name,int action,int from,int to,int promotion){int seat=seat(who);
  if(funded()&&level!=null&&!level.isClientSide()&&!Ledger.get(level.getServer()).boardWagers.open(wagerKey(),betId)){game.result="Draw — wager refunded or recovered";return true;}
  if(action==0&&seat<0){int free=seats[0].isEmpty()?0:seats[1].isEmpty()?1:-1;if(free<0)return false;seats[free]=who.toString();names[free]=name;ready[free]=false;return true;}
  if(seat<0)return false;
  if(action>=8&&action<=10)return wagerAction(seat,action);
  if(action==1&&active()&&game.turn==(seat==0?1:-1)){if(!game.move(new BoardPosition.Move(from,to,promotion)))return false;drawOffer=-1;Arrays.fill(ready,false);settle();return true;}
  if(action==2&&active()){game.result=(seat==0?"Black":"White")+" wins — resignation";drawOffer=-1;settle();return true;}
  if(action==3&&active()&&drawOffer!=seat){drawOffer=seat;return true;}
  if(action==4&&active()&&drawOffer==1-seat){game.result="Draw — agreed";drawOffer=-1;settle();return true;}
  if(action==5&&!game.result.isEmpty()){ready[seat]=true;if(ready[0]&&ready[1]&&!seats[0].isEmpty()&&!seats[1].isEmpty()){settle();game.reset();resetWager();drawOffer=-1;Arrays.fill(ready,false);}return true;}
  if(action==6){if(active())game.result=(seat==0?"Black":"White")+" wins — opponent left";settle();if(betId.isEmpty())resetWager();seats[seat]="";names[seat]="Open seat";Arrays.fill(ready,false);drawOffer=-1;if(seats[0].isEmpty()&&seats[1].isEmpty()){game.reset();resetWager();}return true;}
  return false;
 }
 private void broadcast(){if(level==null||level.getServer()==null)return;watchers.removeIf(id->{var p=level.getServer().getPlayerList().getPlayer(id);if(p==null||!allowed(p)){if(p!=null&&ServerPlayNetworking.canSend(p,BoardGames.View.TYPE))ServerPlayNetworking.send(p,new BoardGames.View(worldPosition.asLong(),false,""));throttle.remove(id);return true;}send(p,false);return false;});}
 public void tick(){if(++ticks%20!=0)return;
  if(funded()&&!Ledger.get(level.getServer()).boardWagers.open(wagerKey(),betId)){game.result="Draw — wager refunded or recovered";revision++;setChanged();}
  if(betId.isEmpty()&&stake>0){for(int i=0;i<2;i++)if(agreed[i]&&(seats[i].isEmpty()||level.getServer().getPlayerList().getPlayer(UUID.fromString(seats[i]))==null)){Arrays.fill(agreed,false);revision++;setChanged();break;}}
  if(!watchers.isEmpty())broadcast();
 }
 @Override public void setRemoved(){if(level!=null&&!level.isClientSide()){for(var id:watchers){var p=level.getServer().getPlayerList().getPlayer(id);if(p!=null&&ServerPlayNetworking.canSend(p,BoardGames.View.TYPE))ServerPlayNetworking.send(p,new BoardGames.View(worldPosition.asLong(),false,""));}}watchers.clear();super.setRemoved();}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){var tag=new net.minecraft.nbt.CompoundTag();var shown=game.copy();shown.repetitions.clear();tag.putString("board",BoardGames.JSON.toJson(shown));return tag;}
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("stake",stake);out.putString("bet_id",betId);out.putString("board",BoardGames.JSON.toJson(game));out.putString("instance",instance);out.putInt("revision",revision);out.putInt("draw",drawOffer);for(int i=0;i<2;i++){out.putString("seat"+i,seats[i]);out.putString("name"+i,names[i]);out.putBoolean("ready"+i,ready[i]);}}
 @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);stake=in.getIntOr("stake",0);if(Arrays.stream(BoardWagers.STAKES).noneMatch(v->v==stake))stake=0;betId=in.getStringOr("bet_id","");wagerMessage=betId.isEmpty()?"":"Stake: "+stake+" each; see result / Exchange";Arrays.fill(agreed,false);try{var read=BoardGames.JSON.fromJson(in.getStringOr("board",""),BoardPosition.class);if(read!=null&&read.squares!=null&&read.squares.length==64&&read.kind==((TableBlock)getBlockState().getBlock()).kind&&Math.abs(read.turn)==1&&read.repetitions!=null&&read.result!=null&&read.chain>=-1&&read.chain<64&&read.ep>=-1&&read.ep<64&&Arrays.stream(read.squares).allMatch(piece->Math.abs((long)piece)<=(read.kind==BoardPosition.Kind.CHESS?6:2)))game=read;}catch(RuntimeException ignored){}
  instance=in.getStringOr("instance",instance);revision=in.getIntOr("revision",0);drawOffer=in.getIntOr("draw",-1);for(int i=0;i<2;i++){seats[i]=in.getStringOr("seat"+i,"");names[i]=in.getStringOr("name"+i,"Open seat");ready[i]=in.getBooleanOr("ready"+i,false);}
 }
}

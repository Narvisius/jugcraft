package local.peepo;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
/** Transaction-local composting; simulations never change the world or consume its random stream. */
public final class ComposterCompanionPort implements CompanionLogistics.Port {
 private final ServerLevel level;private final BlockPos pos;private int projected=-1,attempt;
 public ComposterCompanionPort(ServerLevel level,BlockPos pos){this.level=level;this.pos=pos.immutable();}
 private boolean live(){return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof ComposterBlock;}
 private int fill(){return projected>=0?projected:live()?level.getBlockState(pos).getValue(ComposterBlock.LEVEL):8;}
 private final SnapshotParticipant<int[]> snapshot=new SnapshotParticipant<>(){
  protected int[] createSnapshot(){return new int[]{projected,attempt};}
  protected void readSnapshot(int[] s){projected=s[0];attempt=s[1];}
  protected void onFinalCommit(){if(live() && projected>=0){var state=level.getBlockState(pos);level.setBlockAndUpdate(pos,state.setValue(ComposterBlock.LEVEL,projected));if(projected==7)level.scheduleTick(pos,state.getBlock(),20);level.levelEvent(1500,pos,1);}projected=-1;attempt=0;}
 };
 public Identifier plan(){return PeepoMod.id("compost");}
 public int needed(ItemStack stack){return live() && fill()<7 && stack.has(DataComponents.COMPOSTABLE)?32:0;}
 public Storage<ItemVariant> inputs(){return new InsertionOnlyStorage<>(){
  public long insert(ItemVariant item,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(item,amount);if(amount==0 || needed(item.toStack(1))==0)return 0;
   snapshot.updateSnapshots(tx);projected=fill();long moved=0;
   while(moved<Math.min(32,amount) && projected<7){var state=level.getBlockState(pos).setValue(ComposterBlock.LEVEL,projected);
    var params=new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).create(LootContextParamSets.BLOCK_INTERACT);
    var context=new LootContext.Builder(params).withOptionalRandomSource(RandomSource.create(pos.asLong()^level.getGameTime()^(++attempt*0x9e3779b97f4a7c15L))).create(Optional.empty());
    projected=Math.clamp(projected+item.toStack(1).get(DataComponents.COMPOSTABLE).layers().get(context,0),0,7);moved++;
   }return moved;
  }
 };}
 public Storage<ItemVariant> outputs(){return new SingleSlotStorage<>(){
  public long insert(ItemVariant v,long n,TransactionContext tx){return 0;}
  public boolean supportsInsertion(){return false;}
  public long extract(ItemVariant v,long n,TransactionContext tx){StoragePreconditions.notBlankNotNegative(v,n);if(!live() || n==0 || fill()!=8 || !v.equals(ItemVariant.of(Items.BONE_MEAL)))return 0;snapshot.updateSnapshots(tx);projected=0;return 1;}
  public ItemVariant getResource(){return live() && fill()==8?ItemVariant.of(Items.BONE_MEAL):ItemVariant.blank();}
  public boolean isResourceBlank(){return getResource().isBlank();}
  public long getAmount(){return isResourceBlank()?0:1;}
  public long getCapacity(){return 1;}
 };}
 public CompanionStatus status(){return fill()==8?CompanionStatus.FULL:CompanionStatus.IDLE;}
}

package io.github.jimbozoomer.jugcraft.loot;

import java.util.*;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;

/** Never implements Container: pipes/hoppers cannot expose someone else's personal rewards. */
public final class OwnerLootContainer extends BlockEntity {
    private net.minecraft.world.level.block.entity.PotDecorations decorations=net.minecraft.world.level.block.entity.PotDecorations.EMPTY;
    private final Set<UUID> clientSeen=new HashSet<>();
    private final OwnerLootRewards rewards=new OwnerLootRewards(new OwnerLootRewards.Host(){
        public boolean valid(Player p){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==OwnerLootContainer.this&&OwnerLoot.mayOpen(p,level,worldPosition);}
        public Vec3 origin(){return Vec3.atCenterOf(worldPosition);}
        public int capacity(){return OwnerLootContainer.this.capacity();}
        public boolean archaeology(){return OwnerLootContainer.this.archaeology();}
        public Component name(){return Component.literal("Your loot — ").append(getBlockState().getBlock().getName());}
        public void changed(){setChanged();if(level!=null&&!level.isClientSide())sync();}
        public void opened(boolean value){setOpen(value);}
    });
    private final Map<UUID,Integer> brushProgress=new HashMap<>();
    private final Map<UUID,Long> lastBrush=new HashMap<>();
    public float openness,previousOpenness;
    public OwnerLootContainer(BlockPos pos,BlockState state){super(OwnerLoot.TYPE,pos,state);}
    public void configure(ResourceKey<LootTable> table,long seed){rewards.configure(table,seed^worldPosition.asLong());}
    public boolean seen(UUID player){return level!=null&&level.isClientSide()?clientSeen.contains(player):rewards.seen().contains(player);}
    public net.minecraft.world.level.block.entity.PotDecorations decorations(){return decorations;}
    public void decorations(net.minecraft.world.level.block.entity.PotDecorations value){decorations=value;setChanged();sync();}
    public int capacity(){return ((OwnerLootBlock)getBlockState().getBlock()).kind.equals("decorated_pot")?9:27;}
    public boolean archaeology(){return ((OwnerLootBlock)getBlockState().getBlock()).kind.startsWith("suspicious_");}
    public void brush(ServerPlayer player,ItemStack tool){
        if(!archaeology()||!OwnerLoot.mayOpen(player,level,worldPosition)||!rewards.configured())return;
        if(seen(player.getUUID())){open(player);player.stopUsingItem();return;}
        long now=level.getGameTime(),last=lastBrush.getOrDefault(player.getUUID(),now-10);if(now-last<10)return;
        int progress=(now-last>80?0:brushProgress.getOrDefault(player.getUUID(),0))+1;
        lastBrush.put(player.getUUID(),now);brushProgress.put(player.getUUID(),progress);
        player.sendOverlayMessage(Component.literal("Brushing your reward: "+Math.min(100,progress*10)+"%"));
        if(level instanceof ServerLevel server)server.sendParticles(player,new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,getBlockState()),false,false,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,5,.25,.05,.25,.01);
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.BRUSH_GENERIC,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        if(progress>=10){brushProgress.remove(player.getUUID());lastBrush.remove(player.getUUID());
            tool.hurtAndBreak(1,player,player.getUsedItemHand()==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            open(player);player.stopUsingItem();
        }
    }
    public void open(ServerPlayer player){rewards.open(player);}
    public void tick(){
        previousOpenness=openness;openness=Math.clamp(openness+(getBlockState().getValue(OwnerLootBlock.OPEN)?.1F:-.1F),0,1);
        if(level instanceof ServerLevel server&&level.getGameTime()%20==0){
            rewards.tick(server.getServer());
            lastBrush.entrySet().removeIf(e->{if(level.getGameTime()-e.getValue()<=80)return false;brushProgress.remove(e.getKey());return true;});
        }
    }
    private void setOpen(boolean open){
        if(level==null||level.isClientSide()||getBlockState().getValue(OwnerLootBlock.OPEN)==open)return;
        level.setBlockAndUpdate(worldPosition,getBlockState().setValue(OwnerLootBlock.OPEN,open));level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
        String kind=((OwnerLootBlock)getBlockState().getBlock()).kind;
        var sound=kind.equals("barrel")?(open?net.minecraft.sounds.SoundEvents.BARREL_OPEN:net.minecraft.sounds.SoundEvents.BARREL_CLOSE)
            :kind.equals("shulker_box")?(open?net.minecraft.sounds.SoundEvents.SHULKER_BOX_OPEN:net.minecraft.sounds.SoundEvents.SHULKER_BOX_CLOSE)
            :(open?net.minecraft.sounds.SoundEvents.CHEST_OPEN:net.minecraft.sounds.SoundEvents.CHEST_CLOSE);
        if(!archaeology()&&!kind.equals("decorated_pot"))level.playSound(null,worldPosition,sound,net.minecraft.sounds.SoundSource.BLOCKS,.5F,.9F+level.getRandom().nextFloat()*.1F);
    }
    private void sync(){if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);rewards.save(out);out.store("Decorations",net.minecraft.world.level.block.entity.PotDecorations.CODEC,decorations);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);rewards.load(in);clientSeen.clear();clientSeen.addAll(in.read("Seen",UUIDUtil.CODEC.listOf()).orElse(List.of()));
        decorations=in.read("Decorations",net.minecraft.world.level.block.entity.PotDecorations.CODEC).orElse(net.minecraft.world.level.block.entity.PotDecorations.EMPTY);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){
        // Inventories/loot seeds remain server-only. Client needs only opened appearance.
        var tag=new net.minecraft.nbt.CompoundTag();tag.store("Seen",UUIDUtil.CODEC.listOf(),List.copyOf(rewards.seen()));tag.store("Decorations",net.minecraft.world.level.block.entity.PotDecorations.CODEC,lookup.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),decorations);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

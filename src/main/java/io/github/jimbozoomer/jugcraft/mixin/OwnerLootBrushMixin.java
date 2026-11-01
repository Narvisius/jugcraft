package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.loot.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BrushItem.class)
public abstract class OwnerLootBrushMixin {
    @Inject(method="onUseTick",at=@At("HEAD"),cancellable=true)
    private void jugcraft$personalDig(Level level,LivingEntity user,ItemStack brush,int remaining,CallbackInfo ci){
        if(!(user instanceof net.minecraft.world.entity.player.Player player)||!(player.pick(5,0,false) instanceof BlockHitResult hit))return;
        var pos=hit.getBlockPos();var be=level.getBlockEntity(pos);
        if(player instanceof ServerPlayer p&&be instanceof BrushableBlockEntity original&&OwnerLoot.mayOpen(player,level,pos)){
            var data=(OwnerLootBrushAccess)original;var table=data.jugcraft$lootTable();
            if(table!=null){
                var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(original.getBlockState().getBlock());
                if(id.getNamespace().equals("minecraft")&&OwnerLoot.BLOCKS.containsKey(id.getPath())){
                    var old=original.getBlockState();long seed=data.jugcraft$lootSeed();
                    level.setBlock(pos,OwnerLoot.BLOCKS.get(id.getPath()).defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);
                    be=level.getBlockEntity(pos);
                    if(be instanceof OwnerLootContainer target)target.configure(table,seed);
                    else {level.setBlock(pos,old,Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);if(level.getBlockEntity(pos) instanceof BrushableBlockEntity restored)restored.setLootTable(table,seed);return;}
                }
            }
        }
        if(be instanceof OwnerLootContainer container&&container.archaeology()){
            if(player instanceof ServerPlayer p)container.brush(p,brush);ci.cancel();
        }
    }
}

package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

/** Destructive optics share per-position claim checks and reject inventories, portals and unbreakable blocks. */
final class BotanicalLensTerrain {
    private static boolean safe(ServerLevel l,ServerPlayer p,BlockPos from,BlockPos at){return l.hasChunkAt(at)&&l.getWorldBorder().isWithinBounds(at)&&Authority.mayChange(l,p,at)&&ClaimTransfers.allowed(l,from,at);}
    private static ItemStack tool(BlockState s){for(var item:List.of(Items.DIAMOND_PICKAXE,Items.DIAMOND_AXE,Items.DIAMOND_SHOVEL,Items.DIAMOND_HOE)){var tool=new ItemStack(item);if(!s.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(s))return tool;}return ItemStack.EMPTY;}
    static boolean bore(BotanicalBurst b,ServerLevel l,ServerPlayer p,BlockPos at){
        var state=l.getBlockState(at);if(b.mana()<24||state.hasBlockEntity()||state.getDestroySpeed(l,at)<0||state.getBlock() instanceof BotanicalPlatforms.Platform||!safe(l,p,b.source(),at))return false;var tool=tool(state);if(tool.isEmpty())return false;
        BlockPos destination=at;
        if(b.has("warp")&&!b.handheld()&&l.hasChunkAt(b.source())&&l.getBlockEntity(b.source()) instanceof BotanicalWorkshop spreader&&spreader.kind().contains("spreader")&&spreader.permitted(p)){
            var behind=BlockPos.containing(Vec3.atCenterOf(b.source()).subtract(spreader.aim().normalize()));
            if(safe(l,p,at,behind)&&l.getBlockState(behind).getCollisionShape(l,behind).isEmpty())destination=behind;
        }
        boolean warp=!destination.equals(at);
        var drops=warp?Block.getDrops(state,l,at,null,p,tool):List.<ItemStack>of();
        if(!l.destroyBlock(at,false,p))return false;
        if(warp){for(var drop:drops)Block.popResource(l,destination,drop);state.spawnAfterBreak(l,at,tool,true);}
        else Block.dropResources(state,l,at,null,p,tool);
        b.spend(24);return true;
    }
    static void weight(BotanicalBurst b,ServerLevel l,ServerPlayer p,BlockPos at){
        var state=l.getBlockState(at);if(state.hasBlockEntity()||state.getDestroySpeed(l,at)<0||tool(state).isEmpty()||!safe(l,p,b.source(),at)||!safe(l,p,b.source(),at.below())||!FallingBlock.isFree(l.getBlockState(at.below())))return;
        // The destination column must remain in the same permitted territory through its eventual landing.
        boolean ground=false;for(int y=at.getY()-1;y>=Math.max(l.getMinY(),at.getY()-64);y--){var pos=new BlockPos(at.getX(),y,at.getZ());if(!safe(l,p,b.source(),pos))return;if(!FallingBlock.isFree(l.getBlockState(pos))){ground=true;break;}}if(!ground)return;
        var silk=tool(state);silk.enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        if(Block.getDrops(state,l,at,null,p,silk).stream().noneMatch(s->s.is(state.getBlock().asItem())))return;FallingBlockEntity.fall(l,at,state);
    }
    static void fire(BotanicalBurst b,ServerLevel l,ServerPlayer p,BlockHitResult hit){var at=hit.getBlockPos();var neighbor=at.relative(hit.getDirection());
        if(l.getBlockState(at).getBlock() instanceof BaseFireBlock){if(safe(l,p,b.source(),at))l.removeBlock(at,false);return;}
        if(!safe(l,p,b.source(),neighbor)||!l.getBlockState(neighbor).isAir())return;
        // Reserve possible nearby spread cells; native fire spread is still subject to the server's claim protection.
        for(var pos:BlockPos.betweenClosed(neighbor.offset(-2,-1,-2),neighbor.offset(2,5,2)))if(!safe(l,p,b.source(),pos))return;
        var flame=BaseFireBlock.getState(l,neighbor);if(flame.canSurvive(l,neighbor))l.setBlockAndUpdate(neighbor,flame);
    }
    static void light(BotanicalBurst b,ServerLevel l,ServerPlayer p,BlockHitResult hit){var at=hit.getBlockPos();if(l.getBlockState(at).getBlock() instanceof BotanicalFlame){if(safe(l,p,b.source(),at))l.removeBlock(at,false);return;}at=at.relative(hit.getDirection());if(!safe(l,p,b.source(),at)||l.getBlockState(at).hasBlockEntity()||!l.getBlockState(at).canBeReplaced())return;
        l.setBlockAndUpdate(at,Jugtania.block("mana_flame").defaultBlockState().setValue(BotanicalFlame.COLOR,b.dye(l)).setValue(BlockStateProperties.WATERLOGGED,l.getFluidState(at).isSource()&&l.getFluidState(at).is(net.minecraft.tags.FluidTags.WATER)));
    }
    static void explode(BotanicalBurst b,ServerLevel l,ServerPlayer p,Vec3 at){float radius=b.has("storm")?5:Math.clamp(b.mana()/50F,.1F,5);if(b.has("storm")&&!p.hasInfiniteMaterials())return;int reach=(int)Math.ceil(radius*2+2);var center=BlockPos.containing(at);
        for(var pos:BlockPos.betweenClosed(center.offset(-reach,-reach,-reach),center.offset(reach,reach,reach)))if(!safe(l,p,b.source(),pos))return;
        for(var entity:l.getEntities(b,new AABB(center).inflate(reach)))if(entity instanceof LivingEntity living&&!Authority.mayStrike(p,living))return;
        l.explode(b,at.x,at.y,at.z,radius,Level.ExplosionInteraction.BLOCK);
    }
    static void paint(BotanicalBurst b,ServerLevel l,ServerPlayer p,BlockPos origin){
        if(!b.colored())return;var old=l.getBlockState(origin);if(old.hasBlockEntity())return;
        var replacement=BotanicalPaints.target(old.getBlock(),DyeColor.byId(b.dye(l)));if(replacement==null||replacement==old.getBlock()||replacement.defaultBlockState().hasBlockEntity())return;
        var visited=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(origin);int count=0;
        while(!queue.isEmpty()&&count<1000&&visited.size()<6000){
            var at=queue.removeFirst();if(!visited.add(at)||!safe(l,p,b.source(),at)||!l.getBlockState(at).is(old.getBlock()))continue;
            var state=l.getBlockState(at);if(l.setBlockAndUpdate(at,replacement.withPropertiesOf(state)))count++;
            for(var side:Direction.values())queue.add(at.relative(side));
        }
    }
    static void sheep(BotanicalBurst b,ServerLevel l,ServerPlayer p,Sheep target){if(!b.colored())return;var color=target.getColor();int count=0;for(var sheep:l.getEntitiesOfClass(Sheep.class,target.getBoundingBox().inflate(20))){if(++count>128)break;if(sheep.getColor()==color&&safe(l,p,b.source(),sheep.blockPosition())&&Authority.mayStrike(p,sheep))sheep.setColor(DyeColor.byId(b.dye(l)));}}
}

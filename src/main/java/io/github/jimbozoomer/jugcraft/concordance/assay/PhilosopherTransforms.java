package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

public final class PhilosopherTransforms {
    private PhilosopherTransforms() {}
    public static void crafting(ServerPlayer p){
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new CraftingMenu(id,inv,ContainerLevelAccess.create(p.level(),p.blockPosition())){
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return player==p&&AlchemicalGear.ready(p)
                    &&(p.getMainHandItem().is(AlchemicalGear.STONE)||p.getOffhandItem().is(AlchemicalGear.STONE));}
        },net.minecraft.network.chat.Component.translatable("container.crafting")));
    }
    private static Block next(Block b){
        if(b==Blocks.STONE)return Blocks.COBBLESTONE;if(b==Blocks.COBBLESTONE)return Blocks.STONE;
        if(b==Blocks.DIRT)return Blocks.SAND;if(b==Blocks.SAND)return Blocks.GRASS_BLOCK;if(b==Blocks.GRASS_BLOCK)return Blocks.DIRT;
        if(b==Blocks.GRAVEL)return Blocks.SANDSTONE;if(b==Blocks.SANDSTONE)return Blocks.GRAVEL;return null;
    }
    public static int transform(ServerPlayer p,ItemStack stone,BlockPos center,Direction face){
        if(!p.mayBuild()||!AlchemicalGear.ready(p)||!stone.is(AlchemicalGear.STONE)||p.distanceToSqr(Vec3.atCenterOf(center))>64)return 0;
        int radius=Math.min(2,AlchemyArtifacts.mode(stone)),done=0;
        var original=p.level().getBlockState(center).getBlock();var replacement=next(original);if(replacement==null)return 0;
        var in=AlchemicalWorkshopBlockEntity.material(new ItemStack(original.asItem()));var out=AlchemicalWorkshopBlockEntity.material(new ItemStack(replacement.asItem()));
        long price=Math.max(8,(out==null?0:Assay.dissolve(out,1))-(in==null?0:Assay.dissolve(in,1)));
        for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
            BlockPos pos=switch(face.getAxis()){case X->center.offset(0,a,b);case Y->center.offset(a,0,b);case Z->center.offset(a,b,0);};
            if(!p.level().hasChunkAt(pos)||!Authority.mayChange(p.level(),p,pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>64)continue;
            var state=p.level().getBlockState(pos);if(!state.is(original)||state.hasBlockEntity()||!state.getFluidState().isEmpty()||!replacement.defaultBlockState().canSurvive(p.level(),pos))continue;
            if(!AlchemyPower.pay(p,price))break;
            if(p.level().setBlockAndUpdate(pos,replacement.defaultBlockState())){done++;p.level().levelEvent(2001,pos,Block.getId(state));}else AlchemyPower.refund(p,price);
        }
        if(done>0)AlchemyArtifacts.sound(p,"petransmute");return done;
    }
}

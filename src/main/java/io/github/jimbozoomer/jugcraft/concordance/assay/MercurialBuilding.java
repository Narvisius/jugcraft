package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.*;

/** A learned material becomes a normal placed block, paid per successful placement at the catalogue formation price. */
public final class MercurialBuilding {
    private MercurialBuilding(){}
    public static int build(ServerPlayer p,ItemStack eye,BlockHitResult hit){
        String id=eye.getOrDefault(AlchemyPatterns.TARGET,"");var key=Identifier.tryParse(id);
        if(key==null||!p.mayBuild()||!AlchemicalGear.ready(p)||!AlchemyPatterns.known(p).contains(id)||p.getMainHandItem()!=eye
                ||p.distanceToSqr(hit.getLocation())>64)return 0;
        var sample=new ItemStack(BuiltInRegistries.ITEM.getValue(key));
        if(!AlchemyPatterns.building(sample)||!(sample.getItem() instanceof BlockItem block))return 0;
        long cost=Assay.form(AlchemicalWorkshopBlockEntity.material(sample),1)+8;
        var face=hit.getDirection();var center=hit.getBlockPos().relative(face);int done=0;
        int radius=Math.clamp(eye.getOrDefault(AlchemyPatterns.AREA,3),1,5)/2;
        for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
            var at=switch(face.getAxis()){case X->center.offset(0,a,b);case Y->center.offset(a,0,b);case Z->center.offset(a,b,0);};
            if(!p.level().hasChunkAt(at)||!p.level().getBlockState(at).isAir()||!Authority.mayChange(p.level(),p,at)
                    ||p.distanceToSqr(Vec3.atCenterOf(at))>64||!p.mayUseItemAt(at,face,sample))continue;
            var context=BlockPlaceContext.at(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,sample.copy(),hit),at,face);
            if(!context.getClickedPos().equals(at)||!AlchemyPower.pay(p,cost))continue;
            if(block.place(context).consumesAction())done++;else AlchemyPower.refund(p,cost);
        }
        if(done>0)AlchemyArtifacts.sound(p,"kinesis");return done;
    }
}

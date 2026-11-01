package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
/** Tint extraction for copied original block models on the 26.3 rendering API. */
final class OwnerBlockTints {
    private OwnerBlockTints(){}
    static int color(BlockState state,Object level,BlockPos pos,int index){
        var sources=Minecraft.getInstance().getBlockColors().getTintSources(state);
        if(index<0||index>=sources.size())return -1;
        var source=sources.get(index);
        return level instanceof BlockAndTintGetter view?source.colorInWorld(state,view,pos):source.color(state);
    }
}

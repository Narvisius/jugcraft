package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Replaceable, retained blades in the shared sawmill's existing upgrade slots. */
public final class IndustrialCuttingBlade extends Item {
    public final int hardness;
    public IndustrialCuttingBlade(Properties p,int hardness){super(p.stacksTo(1));this.hardness=hardness;}
    public static int installed(MachineBlockEntity machine){
        int best=0;
        for(int i=machine.kind().slots;i<machine.kind().containerSize();i++)
            if(machine.getItem(i).getItem() instanceof IndustrialCuttingBlade blade)best=Math.max(best,blade.hardness);
        return best;
    }
    public static String requiredName(int hardness){return hardness<=2?"Iron":hardness<=5?"Steel":"Diamond";}
    @Override public void appendHoverText(ItemStack stack,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
        out.accept(Component.literal("Sawmill / Industrial Cutting Mill upgrade slot. Retained, not consumed."));
        out.accept(Component.literal("Cuts materials of hardness "+hardness+" or lower. Blades do not stack their strength."));
        out.accept(Component.literal(hardness==2?"Lead, gold, bronze, tin and copper blocks.":hardness==5?"Also iron and lapis blocks.":"Also steel blocks and obsidian."));
    }
}

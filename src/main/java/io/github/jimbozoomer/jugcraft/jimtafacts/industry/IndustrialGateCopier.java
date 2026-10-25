package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Copies bounded gate rules only. Owner, output state, inventories and access rights never leave the source. */
public final class IndustrialGateCopier extends Item {
    public static Item ITEM;
    private static final String DATA="jimta_gate_rules";
    private IndustrialGateCopier(Properties p){super(p);}
    public static void register(){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_gate_copier"));ITEM=Registry.register(BuiltInRegistries.ITEM,key,new IndustrialGateCopier(new Properties().setId(key).stacksTo(1)));CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(ITEM));}
    public static boolean valid(int[] rules){
        if(rules.length<6||rules.length>48||rules.length%6!=0)return false;
        for(int row=0;row<rules.length;row+=6)if(rules[row]<0||rules[row]>1||rules[row+1]<0||rules[row+1]>15||rules[row+2]<0||rules[row+2]>1||rules[row+3]<0||rules[row+3]>100||rules[row+4]<0||rules[row+4]>15||rules[row+5]<0||rules[row+5]>1)return false;
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof IndustrialAutomation.GateEntity gate))return InteractionResult.PASS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(p.isSpectator()||!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
        var body=(IndustrialAutomation.Gate)gate.getBlockState().getBlock();var stack=c.getItemInHand();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(p.isShiftKeyDown()||!data.contains(DATA)){
            int[] rules=new int[gate.ruleRows()*6];for(int row=0;row<gate.ruleRows();row++)for(int col=0;col<6;col++)rules[row*6+col]=gate.rule(row,col);
            CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putIntArray(DATA,rules);t.putBoolean("jimta_gate_and",body.and);});
            stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(1F),List.of(),List.of(),List.of()));p.sendOverlayMessage(Component.literal("Copied "+body.rows+" gate rows. Use on a compatible gate; sneak-use copies again."));return InteractionResult.SUCCESS;
        }
        int[] rules=data.getIntArray(DATA).orElse(new int[0]);
        if(!valid(rules)||rules.length>gate.ruleRows()*6||data.getBooleanOr("jimta_gate_and",!body.and)!=body.and){p.sendOverlayMessage(Component.literal("Use the same AND/OR logic and a gate with enough active rows. No settings changed."));return InteractionResult.FAIL;}
        for(int row=0;row<8;row++){
            int at=row*6;boolean included=at<rules.length;
            gate.setRule(row,included&&rules[at]==1,DyeColor.byId(included?rules[at+1]:0),included&&rules[at+2]==1,included?rules[at+3]:50,DyeColor.byId(included?rules[at+4]:0),included&&rules[at+5]==1);
        }
        c.getLevel().sendBlockUpdated(c.getClickedPos(),gate.getBlockState(),gate.getBlockState(),Block.UPDATE_CLIENTS);
        p.sendOverlayMessage(Component.literal("Primary rules applied. Owner, modifier parameters and expansion settings stay unchanged."));return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
        if(!p.isShiftKeyDown())return InteractionResult.PASS;
        if(!l.isClientSide()){var stack=p.getItemInHand(hand);CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.remove(DATA);t.remove("jimta_gate_and");});stack.remove(DataComponents.CUSTOM_MODEL_DATA);p.sendOverlayMessage(Component.literal("Gate copier cleared."));}return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("Sneak-use gate: copy rules · use gate: apply · sneak-use air: clear"));}
}

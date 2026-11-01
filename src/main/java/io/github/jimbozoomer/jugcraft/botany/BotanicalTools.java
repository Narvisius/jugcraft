package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class BotanicalTools {
    public static DataComponentType<GlobalPos> LINK;
    public static DataComponentType<Integer> MANA;
    static void register(){
        LINK=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_link"),DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
        MANA=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_mana"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,500000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    }
    public static int tablet(ItemStack stack){return Math.clamp(stack.getOrDefault(MANA,0),0,500000);}
    public static void tablet(ItemStack stack,int amount){stack.set(MANA,Math.clamp(amount,0,500000));}
    /** Explicit, whole-unit bridge: 100 botanical mana is one existing ley charge. */
    public static final class Tablet extends Item {
        public Tablet(Properties properties){super(properties);}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var level=p.level();var pos=context.getClickedPos();var held=context.getItemInHand();
            if(!(level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity pylon))return InteractionResult.PASS;
            if(!io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(level,p,pos))return InteractionResult.FAIL;
            int mana=tablet(held);
            if(p.isShiftKeyDown()){
                long units=Math.min((500000-mana)/100,pylon.ley());if(units>0&&pylon.draw(level,units))tablet(held,mana+(int)units*100);
            }else{
                long accepted=pylon.fill(level,mana/100);if(accepted>0)tablet(held,mana-(int)accepted*100);
            }
            p.sendOverlayMessage(Component.literal("Tablet: "+tablet(held)+" mana · Pylon: "+pylon.ley()+" ley (100 mana = 1 ley)"));return InteractionResult.SUCCESS;
        }
    }
    public static final class Wand extends Item {
        public Wand(Properties properties){super(properties);}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
            var level=player.level();var pos=context.getClickedPos();var wand=context.getItemInHand();
            if(BotanicalEnchanter.form(player,pos))return InteractionResult.SUCCESS;
            var start=wand.get(LINK);
            if(player.isShiftKeyDown()&&level.getBlockState(pos).is(Jugtania.block("piston_relay"))&&io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(level,player,pos)){wand.set(LINK,GlobalPos.of(level.dimension(),pos));player.sendOverlayMessage(Component.literal("Force Relay selected. Use the wand on its destination within 256 blocks."));return InteractionResult.SUCCESS;}
            if(!player.isShiftKeyDown()&&start!=null&&start.dimension().equals(level.dimension())&&level.hasChunkAt(start.pos())&&level.getBlockState(start.pos()).is(Jugtania.block("piston_relay"))){if(BotanicalForceRelay.bind(player,start.pos(),pos)){wand.remove(LINK);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;}

            if(!player.isShiftKeyDown()&&start!=null&&start.dimension().equals(level.dimension())&&level.hasChunkAt(start.pos())&&level.getBlockEntity(start.pos()) instanceof BotanicalWorkshop source&&source.kind().equals("spectranthemum")&&!(level.getBlockEntity(pos) instanceof BotanicalWorkshop receiver&&receiver.kind().endsWith("pool"))){
                if(source.function.logistics.bind(player,pos)){wand.remove(LINK);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
            }
            if(!(level.getBlockEntity(pos) instanceof BotanicalWorkshop target)||!target.permitted(player))return InteractionResult.PASS;
            if(player.isShiftKeyDown()&&(BotanicalLuminizer.TYPES.contains(target.kind())||target.generating()||target.functional()||target.kind().contains("spreader"))){
                wand.set(LINK,GlobalPos.of(level.dimension(),pos));player.sendOverlayMessage(Component.literal("Source selected. Use the wand on a compatible receiver or Luminizer."));return InteractionResult.SUCCESS;
            }
            if(!player.isShiftKeyDown()&&start!=null){
                if(start.dimension().equals(level.dimension())&&level.hasChunkAt(start.pos())&&level.getBlockEntity(start.pos()) instanceof BotanicalWorkshop source&&source.bind(player,pos)){
                    wand.remove(LINK);player.sendOverlayMessage(Component.literal("Mana connection bound."));return InteractionResult.SUCCESS;
                }
                player.sendOverlayMessage(Component.literal("Connection needs compatible devices within 16 blocks (64 for spreaders, 20 for Luminizers) and permission at both ends. Sneak-use a source to select it again."));return InteractionResult.FAIL;
            }
            target.wand(player);return InteractionResult.SUCCESS;
        }
    }
}

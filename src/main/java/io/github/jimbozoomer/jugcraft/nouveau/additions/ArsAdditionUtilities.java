package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class ArsAdditionUtilities {
    public static Item XP_JAR;
    public static DataComponentType<Integer> XP_REMAINDER;
    private ArsAdditionUtilities(){}
    public static void register(){
        XP_REMAINDER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_xp_remainder"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,1)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_xp_jar"));
        XP_JAR=Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(1)){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
                if(!JugNouveau.enabled()||p.isSpectator())return InteractionResult.FAIL;
                var stack=p.getItemInHand(hand);
                if(p.isShiftKeyDown()){
                    boolean active=!stack.getOrDefault(NouveauVoiding.ACTIVE,false);stack.set(NouveauVoiding.ACTIVE,active);
                    p.sendOverlayMessage(Component.literal("XP Jar "+(active?"enabled":"disabled")));return InteractionResult.SUCCESS;
                }
                if(hand!=InteractionHand.MAIN_HAND||p.getOffhandItem().isEmpty())return InteractionResult.PASS;
                NouveauVoiding.toggle(stack,p.getOffhandItem());p.sendOverlayMessage(Component.literal("XP filter: "+stack.getOrDefault(NouveauUtilities.FILTER,java.util.List.of()).size()+" item types"));return InteractionResult.SUCCESS;
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(XP_JAR));
    }
    public static boolean isXpJar(ItemStack stack){return XP_JAR!=null&&stack.is(XP_JAR);}
    public static void consume(ServerPlayer player,ItemStack jar,int count){
        int total=count+jar.getOrDefault(XP_REMAINDER,0);player.giveExperiencePoints(total/2);jar.set(XP_REMAINDER,total%2);
    }
}

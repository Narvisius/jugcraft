package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Remappable controls and server-owned styling; packets never supply items or spell effects. */
public final class NouveauControls {
    public static final String[] PALETTES={"Amethyst","Ember","Tide","Grove","Sunlight","Frost","Rose","Shadow"};
    public static final int[] COLORS={0xB184EF,0xF76E43,0x52B5EC,0x6DD47C,0xEFC865,0xD7F3FF,0xF287B5,0x725E91};
    public static final String[] PARTICLES={"glow","sparkle","helix","line","bubble","snow","leaf","smoke","breaking_circle","new_glow"};
    public static final String[] SOUNDS={"empty","fire_family","gaia_family","tempestry_family"};
    public static DataComponentType<Integer> COLOR,PARTICLE,SOUND;
    public static MenuType<StyleMenu> MENU;
    private NouveauControls(){}
    public record Action(int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(Jugcraft.id("nouveau_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::action,Action::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static DataComponentType<Integer> setting(String id,int max){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_style_"+id),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,max)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    public static void register(){
        COLOR=setting("color",COLORS.length-1);PARTICLE=setting("particle",PARTICLES.length-1);SOUND=setting("sound",SOUNDS.length-1);
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_style"),new MenuType<>(StyleMenu::new,FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(payload,context)->context.server().execute(()->open(context.player(),payload.action())));
    }
    private static void open(ServerPlayer p,int action){
        if(action<0||action>3||!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||NouveauScrying.active(p)||!RateGate.allow(p,"nouveau_controls",5))return;
        switch(action){
            case 0->p.openMenu(new SimpleMenuProvider((id,inv,who)->new NouveauFamiliarMenu(id,inv),Component.literal("Your Familiars")));
            case 1->{if(NouveauGear.worn(p,"alchemists_crown"))p.openMenu(new SimpleMenuProvider((id,inv,who)->new NouveauPotionMenu(id,inv,InteractionHand.MAIN_HAND,true),Component.literal("Alchemist's Crown")));}
            case 2->{if(stylable(p.getMainHandItem()))p.openMenu(new SimpleMenuProvider((id,inv,who)->new StyleMenu(id,inv),Component.literal("Spell Appearance")));}
            case 3->{if(!NouveauFamiliars.active(p).isEmpty()){boolean enabled=p.getAttachedOrElse(NouveauFamiliars.MAGNET,true);p.setAttached(NouveauFamiliars.MAGNET,!enabled);p.sendOverlayMessage(Component.literal("Familiar collection "+(enabled?"disabled":"enabled")));}}
        }
    }
    public static boolean stylable(ItemStack stack){return stack.has(JugcraftConcordance.INSCRIPTION)||ConcordanceData.rules().catalog().instrumentFor(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())!=null;}
    public static net.minecraft.world.item.DyeColor dye(ItemStack stack){return switch(Math.clamp(stack.getOrDefault(COLOR,0),0,COLORS.length-1)){case 1->net.minecraft.world.item.DyeColor.ORANGE;case 2->net.minecraft.world.item.DyeColor.LIGHT_BLUE;case 3->net.minecraft.world.item.DyeColor.LIME;case 4->net.minecraft.world.item.DyeColor.YELLOW;case 5->net.minecraft.world.item.DyeColor.WHITE;case 6->net.minecraft.world.item.DyeColor.PINK;case 7->net.minecraft.world.item.DyeColor.BLACK;default->net.minecraft.world.item.DyeColor.PURPLE;};}
    public static int color(ItemStack stack){if(stack.has(NouveauGear.TOME)&&stack.has(net.minecraft.core.component.DataComponents.DYED_COLOR))return stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR).rgb();return COLORS[Math.clamp(stack.getOrDefault(COLOR,0),0,COLORS.length-1)];}
    public static String particle(ItemStack stack){return PARTICLES[Math.clamp(stack.getOrDefault(PARTICLE,0),0,PARTICLES.length-1)];}
    public static String sound(ItemStack stack){return SOUNDS[Math.clamp(stack.getOrDefault(SOUND,0),0,SOUNDS.length-1)];}
    public static final class StyleMenu extends AbstractContainerMenu {
        private final Inventory inventory;private final ItemStack tool;private final ContainerData data=new SimpleContainerData(3);
        public StyleMenu(int id,Inventory inventory){super(MENU,id);this.inventory=inventory;tool=inventory.player.getMainHandItem();addDataSlots(data);refresh();}
        private void refresh(){if(inventory.player instanceof ServerPlayer){data.set(0,tool.getOrDefault(COLOR,0));data.set(1,tool.getOrDefault(PARTICLE,0));data.set(2,tool.getOrDefault(SOUND,0));}}
        public int selection(int row){return data.get(row);}
        @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
        @Override public boolean stillValid(Player p){return p==inventory.player&&p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer)||(p.getMainHandItem()==tool&&stylable(tool)&&JugNouveau.enabled()));}
        @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
        @Override public boolean clickMenuButton(Player p,int button){if(!(p instanceof ServerPlayer)||!stillValid(p))return false;int row=button/32,index=button%32;
            if(row==0&&index>=0&&index<COLORS.length)tool.set(COLOR,index);else if(row==1&&index>=0&&index<PARTICLES.length)tool.set(PARTICLE,index);else if(row==2&&index>=0&&index<SOUNDS.length)tool.set(SOUND,index);else return false;refresh();return true;}
    }
}

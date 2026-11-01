package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Portable mana bursts with real removable lenses. The magazine is never an ammunition counter. */
public final class BotanicalBlaster extends Item {
    public static DataComponentType<Boolean> CLIP;
    public static DataComponentType<Integer> SELECTED;
    public static DataComponentType<ItemContainerContents> LENSES;
    BotanicalBlaster(Properties properties){super(properties.stacksTo(1));}
    static void register(){
        CLIP=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_blaster_clip"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        SELECTED=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_blaster_selected"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,5)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        LENSES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_blaster_lenses"),DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_blaster_crafting"),Crafting.SERIALIZER);
    }
    public static boolean clipped(ItemStack stack){return stack.getOrDefault(CLIP,false);}
    private static int selected(ItemStack stack){return clipped(stack)?Math.clamp(stack.getOrDefault(SELECTED,0),0,5):0;}
    private static NonNullList<ItemStack> contents(ItemStack stack){var slots=NonNullList.withSize(6,ItemStack.EMPTY);stack.getOrDefault(LENSES,ItemContainerContents.EMPTY).copyInto(slots);return slots;}
    public static ItemStack lens(ItemStack stack){var value=contents(stack).get(selected(stack));return valid(value)?value.copyWithCount(1):ItemStack.EMPTY;}
    private static boolean valid(ItemStack stack){return !BotanicalLenses.name(stack).isEmpty()&&BotanicalLenses.kinds(stack).stream().noneMatch(n->(BotanicalLenses.PROPS.getOrDefault(n,63)&32)!=0||n.equals("storm"));}
    private static void lens(ItemStack stack,ItemStack value){var slots=contents(stack);slots.set(selected(stack),value.copyWithCount(value.isEmpty()?0:1));stack.set(LENSES,ItemContainerContents.fromItems(slots));}
    @Override public boolean isBarVisible(ItemStack stack){return false;}
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        if(!(user instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        var stack=player.getItemInHand(hand);if(player.isSpectator()||!Authority.mayChange(player.level(),player,player.blockPosition()))return InteractionResult.FAIL;
        if(player.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
        if(player.isShiftKeyDown()&&clipped(stack)){
            int next=(selected(stack)+1)%6;stack.set(SELECTED,next);var active=lens(stack);
            player.sendOverlayMessage(Component.literal("Lens "+(next+1)+" / 6: ").append(active.isEmpty()?Component.literal("unmodified burst"):active.getHoverName()));
            sound(player,"mana_blaster_cycle",.6F);player.getCooldowns().addCooldown(stack,4);return InteractionResult.SUCCESS;
        }
        var active=lens(stack);var properties=BotanicalBurst.properties("handheld",BotanicalLenses.kinds(active));
        if(BotanicalManaItems.available(player)<properties.capacity()){sound(player,"mana_blaster_misfire",.6F);return InteractionResult.FAIL;}
        // Nothing can tick between spawn and debit on the server thread. Failed payment discards the unpaid burst.
        var burst=BotanicalBurst.launchHandheld(player,active,properties);
        if(burst==null)return InteractionResult.FAIL;
        if(!BotanicalManaItems.pay(player,properties.capacity())){burst.discard();return InteractionResult.FAIL;}
        var haste=player.getEffect(MobEffects.HASTE);player.getCooldowns().addCooldown(stack,haste==null?30:Math.max(2,30-8*(haste.getAmplifier()+1)));
        if(!BotanicalAccessories.has(player,"knockback_belt")){var motion=burst.getDeltaMovement();player.push(-motion.x*.1,-motion.y*.3,-motion.z*.1);player.syncVelocity=true;}
        sound(player,"mana_blaster",1);return InteractionResult.SUCCESS;
    }
    private static void sound(ServerPlayer player,String name,float volume){player.level().playSound(null,player.blockPosition(),BotanicalSounds.get(name),SoundSource.PLAYERS,volume,1);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> text,TooltipFlag flag){
        text.accept(Component.literal("Uses carried mana. Craft with a lens to install; craft alone to remove."));
        if(clipped(stack))text.accept(Component.literal("Sneak-use cycles six lens slots. Selected: "+(selected(stack)+1)));
        var slots=contents(stack);for(int i=0;i<(clipped(stack)?6:1);i++)text.accept(Component.literal((i==selected(stack)?"> ":"  ")+(i+1)+": ").append(slots.get(i).isEmpty()?Component.literal("empty"):slots.get(i).getHoverName()));
    }
    public static final class Crafting extends CustomRecipe {
        private static final Crafting INSTANCE=new Crafting();public static final RecipeSerializer<Crafting> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Crafting>unit(INSTANCE));
        private record Match(ItemStack gun,ItemStack addition,int slot){}
        private Match find(CraftingInput in){ItemStack gun=ItemStack.EMPTY,addition=ItemStack.EMPTY;int slot=-1;
            for(int i=0;i<in.size();i++){var s=in.getItem(i);if(s.isEmpty())continue;if(s.getItem() instanceof BotanicalBlaster&&gun.isEmpty()){gun=s;slot=i;}else if(addition.isEmpty()&&(valid(s)||s.is(Jugtania.item("clip"))))addition=s;else return null;}
            if(gun.isEmpty())return null;
            if(addition.isEmpty())return lens(gun).isEmpty()?null:new Match(gun,addition,slot);
            if(addition.is(Jugtania.item("clip")))return clipped(gun)?null:new Match(gun,addition,slot);
            return lens(gun).isEmpty()?new Match(gun,addition,slot):null;
        }
        @Override public boolean matches(CraftingInput in,Level level){return find(in)!=null;}
        @Override public ItemStack assemble(CraftingInput in){var m=find(in);if(m==null)return ItemStack.EMPTY;var out=m.gun.copyWithCount(1);
            if(m.addition.is(Jugtania.item("clip"))){out.set(CLIP,true);out.set(SELECTED,0);}else lens(out,m.addition);return out;
        }
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput in){var out=NonNullList.withSize(in.size(),ItemStack.EMPTY);var m=find(in);if(m!=null&&m.addition.isEmpty())out.set(m.slot,lens(m.gun));return out;}
        @Override public RecipeSerializer<Crafting> getSerializer(){return SERIALIZER;}
    }
}

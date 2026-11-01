package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Cosmetic attachment is a single item identity, never a recursively nested item stack. */
public final class BotanicalCosmetics {
    public static final Set<String> HEAD=Set.of("red_glasses","engineer_goggles","eyepatch","wicked_eyepatch","red_ribbons","pink_flower_bud","polka_dotted_bows","blue_butterfly","cat_ears","googly_eyes","clock_eye","unicorn_horn","devil_horns","hyper_plus","ancient_mask","eerie_mask","alien_antenna","anaglyph_glasses","orange_shades","groucho_glasses","thick_eyebrows","tiny_potato_mask","questgiver_mark","thinking_hand");
    public static final Set<String> BODY=Set.of("black_bowtie","black_tie","puffy_scarf","witch_pin","devil_tail","kamui_eye","four_leaf_clover","botanist_emblem","lusitanic_shield");
    public static DataComponentType<String> ATTACHED;
    public static boolean cosmetic(String name){return name.equals("monocle")||name.startsWith("cosmetic_")&&(HEAD.contains(name.substring(9))||BODY.contains(name.substring(9)));}
    static void register(){
        ATTACHED=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_cosmetic"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_cosmetic_attachment"),Attachment.SERIALIZER);
    }
    static Item create(String name,Item.Properties p){return new Cosmetic(name,p.stacksTo(1));}
    public static boolean attachable(ItemStack s){return s.getItem() instanceof TrinketCallback&&!(s.getItem() instanceof Cosmetic)&&BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("jugtania_");}
    public static String attached(ItemStack stack){var name=stack.getOrDefault(ATTACHED,"");return cosmetic(name)?name:"";}
    public static boolean monocle(LivingEntity wearer){
        if(wearer.isSpectator())return false;boolean[] found={false};TrinketsApi.getAttachment(wearer).forEach((slot,stack)->{if(!slot.cosmetic()&&slot.canApplyEffects()&&(stack.is(Jugtania.item("monocle"))||attached(stack).equals("monocle")))found[0]=true;});return found[0];
    }
    private static final class Cosmetic extends Item implements TrinketCallback {
        final String name;Cosmetic(String name,Properties p){super(p);this.name=name;}
        @Override public boolean canEquip(ItemStack s,TrinketSlotAccess slot,LivingEntity e){return e instanceof Player;}
    }
    public static final class Attachment extends CustomRecipe {
        private static final Attachment INSTANCE=new Attachment();
        public static final RecipeSerializer<Attachment> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Attachment>unit(INSTANCE));
        private record Match(ItemStack host,String cosmetic,int hostSlot){}
        private Match find(CraftingInput input){
            ItemStack host=ItemStack.EMPTY;String cosmetic="";int at=-1;
            for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;
                if(attachable(s)){if(!host.isEmpty())return null;host=s;at=i;}
                else if(s.getItem() instanceof Cosmetic c){if(!cosmetic.isEmpty())return null;cosmetic=c.name;}
                else return null;
            }
            if(host.isEmpty()||cosmetic.isEmpty()&&attached(host).isEmpty()||!cosmetic.isEmpty()&&cosmetic.equals(attached(host)))return null;
            return new Match(host,cosmetic,at);
        }
        @Override public boolean matches(CraftingInput input,Level level){return find(input)!=null;}
        @Override public ItemStack assemble(CraftingInput input){var m=find(input);if(m==null)return ItemStack.EMPTY;var out=m.host.copyWithCount(1);if(m.cosmetic.isEmpty())out.remove(ATTACHED);else out.set(ATTACHED,m.cosmetic);return out;}
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){var out=NonNullList.withSize(input.size(),ItemStack.EMPTY);var m=find(input);if(m!=null&&!attached(m.host).isEmpty())out.set(m.hostSlot,new ItemStack(Jugtania.item(attached(m.host))));return out;}
        @Override public RecipeSerializer<Attachment> getSerializer(){return SERIALIZER;}
    }
}

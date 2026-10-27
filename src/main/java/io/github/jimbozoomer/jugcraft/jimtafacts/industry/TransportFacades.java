package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalFacades;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalStorage;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit factory variants; old shared covers keep their cosmetic, connection-permitting behavior. */
public final class TransportFacades {
    public static Item SOLID,HOLLOW;
    public record Phase(BlockState active,int channel){
        public static final com.mojang.serialization.Codec<Phase> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
                BlockState.CODEC.fieldOf("active").forGetter(Phase::active),com.mojang.serialization.Codec.intRange(-1,15).fieldOf("channel").forGetter(Phase::channel)).apply(i,Phase::new));
        public static final Phase NONE=new Phase(Blocks.AIR.defaultBlockState(),-1);
        public boolean valid(){return channel>=0&&(active.isAir()||DigitalFacades.valid(active));}
    }
    public static net.minecraft.core.component.DataComponentType<Phase> PHASE;
    public static final PhaseRecipe PHASE_RECIPE=new PhaseRecipe();
    public static final RecipeSerializer<PhaseRecipe> PHASE_SERIALIZER=new RecipeSerializer<>(MapCodec.unit(PHASE_RECIPE),StreamCodec.<RegistryFriendlyByteBuf,PhaseRecipe>unit(PHASE_RECIPE));
    public static final int LEGACY=0,SEALED=1,OPEN=2;
    public static final Conversion CONVERSION=new Conversion();
    public static final RecipeSerializer<Conversion> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(CONVERSION),StreamCodec.<RegistryFriendlyByteBuf,Conversion>unit(CONVERSION));
    private TransportFacades(){}
    public static void register(){
        PHASE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimta_facade_phase"),net.minecraft.core.component.DataComponentType.<Phase>builder().persistent(Phase.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(Phase.CODEC)).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("jimta_factory_facade_phase"),PHASE_SERIALIZER);
        SOLID=item("solid",SEALED);HOLLOW=item("hollow",OPEN);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("jimta_factory_facade"),SERIALIZER);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(o->{
            for(var material:java.util.List.of(Blocks.STONE,Blocks.OAK_PLANKS,Blocks.BRICKS)){
                o.accept(stack(material.defaultBlockState(),SEALED));o.accept(stack(material.defaultBlockState(),OPEN));
            }
        });
    }
    private static Item item(String name,int mode){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_pipe_facade_"+name));
        return Registry.register(BuiltInRegistries.ITEM,key,new FacadeItem(new Item.Properties().setId(key),mode));
    }
    public static int mode(ItemStack stack){return stack.is(SOLID)?SEALED:stack.is(HOLLOW)?OPEN:LEGACY;}
    public static ItemStack stack(BlockState material,int mode){
        var stack=mode==LEGACY?DigitalFacades.facade(material):new ItemStack(mode==SEALED?SOLID:HOLLOW);
        stack.set(DigitalFacades.MATERIAL,material);return stack;
    }
    private static final class FacadeItem extends Item {
        private final int mode;
        FacadeItem(Properties p,int mode){super(p);this.mode=mode;}
        @Override public InteractionResult useOn(UseOnContext c){return TransportAttachments.cover(c)?InteractionResult.SUCCESS:InteractionResult.PASS;}
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            var sample=s.get(DigitalFacades.MATERIAL);if(sample!=null)out.accept(sample.getBlock().getName());
            out.accept(Component.literal(mode==SEALED?"Solid factory cover: seals item, fluid and energy connections on this face.":"Hollow factory cover: opening keeps pipe connections accessible."));
            out.accept(Component.literal("Craft one facade alone to switch solid/hollow. Start with a sampled cable facade."));
            var phase=s.get(PHASE);if(phase!=null&&phase.valid())out.accept(Component.literal("On "+DyeColor.byId(phase.channel()).getName()+" relay: "+(phase.active().isAir()?"transparent":phase.active().getBlock().getName().getString())+". Routing stays unchanged."));
            out.accept(Component.literal("Craft with dye and a second sampled facade for a switched appearance; glass gives a transparent on-state."));
            out.accept(Component.literal("Sneak-use empty-handed to remove; hold Network Tool to see through."));
        }
    }
    /** Existing colored data-network relays provide the source wire-phase role, without a second wire network. */
    public static BlockState appearance(TransportAttachments.Fittings fittings,int face){
        var phase=fittings.phases.get(face);var level=fittings.getLevel();
        if(phase.valid()&&level!=null)for(var direction:net.minecraft.core.Direction.values()){
            var pos=fittings.getBlockPos().relative(direction);if(!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);
            if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.control.RelayBlock
                    &&state.getValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL).getId()==phase.channel()
                    &&state.getValue(io.github.jimbozoomer.jugcraft.control.RelayBlock.POWERED))return phase.active();
        }
        return fittings.faces.get(face);
    }
    public static final class PhaseRecipe extends CustomRecipe {
        private ItemStack result(CraftingInput input){
            ItemStack base=ItemStack.EMPTY;BlockState active=null;DyeColor channel=null;int count=0;
            for(var stack:input.items())if(!stack.isEmpty()){
                count++;if(count>3)return ItemStack.EMPTY;
                var dye=io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock.dyeColor(stack);
                if(dye!=null){if(channel!=null)return ItemStack.EMPTY;channel=dye;}
                else if(stack.is(Items.GLASS)){if(active!=null)return ItemStack.EMPTY;active=Blocks.AIR.defaultBlockState();}
                else if(stack.is(SOLID)||stack.is(HOLLOW)||stack.is(DigitalStorage.ITEMS.get("facade"))){
                    var sample=stack.get(DigitalFacades.MATERIAL);if(sample==null||!DigitalFacades.valid(sample))return ItemStack.EMPTY;
                    if(base.isEmpty())base=stack;else if(active==null)active=sample;else return ItemStack.EMPTY;
                }else return ItemStack.EMPTY;
            }
            if(count!=3||base.isEmpty()||channel==null||active==null)return ItemStack.EMPTY;
            var result=base.transmuteCopy(base.is(HOLLOW)?HOLLOW:SOLID,1);result.set(PHASE,new Phase(active,channel.getId()));return result;
        }
        @Override public boolean matches(CraftingInput input,Level level){return !result(input).isEmpty();}
        @Override public ItemStack assemble(CraftingInput input){return result(input);}
        @Override public RecipeSerializer<PhaseRecipe> getSerializer(){return PHASE_SERIALIZER;}
    }
    public static final class Conversion extends CustomRecipe {
        private ItemStack sample(CraftingInput input){
            ItemStack found=ItemStack.EMPTY;
            for(var stack:input.items())if(!stack.isEmpty()){
                if(!found.isEmpty()||!(stack.is(SOLID)||stack.is(HOLLOW)||stack.is(DigitalStorage.ITEMS.get("facade"))))return ItemStack.EMPTY;
                var material=stack.get(DigitalFacades.MATERIAL);if(material==null||!DigitalFacades.valid(material))return ItemStack.EMPTY;
                found=stack;
            }
            return found;
        }
        @Override public boolean matches(CraftingInput input,Level level){return !sample(input).isEmpty();}
        @Override public ItemStack assemble(CraftingInput input){var original=sample(input);return original.isEmpty()?ItemStack.EMPTY:original.transmuteCopy(original.is(SOLID)?HOLLOW:SOLID,1);}
        @Override public RecipeSerializer<Conversion> getSerializer(){return SERIALIZER;}
    }
}

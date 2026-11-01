package io.github.jimbozoomer.jugcraft.aquatic;

import java.util.Map;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** Supplied neptunium equipment, with native attributes instead of NeoForge-only hooks. */
public final class OwnerNeptunium {
    private OwnerNeptunium(){}
    public static final ArmorMaterial ARMOR=new ArmorMaterial(35,Map.of(ArmorType.BOOTS,3,ArmorType.LEGGINGS,6,ArmorType.CHESTPLATE,8,ArmorType.HELMET,3),14,SoundEvents.ARMOR_EQUIP_DIAMOND,2,0,OwnerAquaticGear.NEPTUNIUM.repairItems(),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("aquatic_neptunium")));
    public static Block BLOCK,FARMLAND;
    public static void register(){
        block("worm_farm",p->new OwnerWormFarm(p.strength(.6f).sound(SoundType.WOOD)));
        BLOCK=block("neptunium_block",p->new Block(p.strength(5,6).sound(SoundType.METAL)));
        FARMLAND=block("farmland",p->new MoistSoil(p.strength(.6f).sound(SoundType.GRAVEL).randomTicks()));
        var mat=OwnerAquaticGear.NEPTUNIUM;
        OwnerAquaculture.item("neptunium_pickaxe",p->new SeaPickaxe(p));
        OwnerAquaculture.item("neptunium_shovel",p->new SeaShovel(p));
        OwnerAquaculture.item("neptunium_axe",p->new Item(p.axe(mat,5,-3)));
        OwnerAquaculture.item("neptunium_hoe",p->new SeaHoe(p));
        OwnerAquaculture.item("neptunium_sword",p->new Item(p.sword(mat,3,-2.4f)));
        OwnerAquaculture.item("neptunium_bow",p->new BowItem(p.durability(2500).enchantable(1)));
        for(var type:new ArmorType[]{ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS,ArmorType.BOOTS})OwnerAquaculture.item("neptunium_"+type.getName(),p->new SeaArmor(p,type));
    }
    private static Block block(String name,java.util.function.Function<BlockBehaviour.Properties,Block> factory){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("aquatic_"+name));var block=Registry.register(BuiltInRegistries.BLOCK,key,factory.apply(BlockBehaviour.Properties.of().setId(key)));OwnerAquaculture.item(name,p->new BlockItem(block,p));return block;}
    public static final class MoistSoil extends FarmlandBlock {
        MoistSoil(Properties p){super(Blocks.DIRT,p);registerDefaultState(defaultBlockState().setValue(MOISTURE,7));}
        @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){if(!state.canSurvive(level,pos))super.randomTick(state,level,pos,random);else if(state.getValue(MOISTURE)!=7)level.setBlock(pos,state.setValue(MOISTURE,7),2);}
    }
    private static boolean underwater(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBooleanOr("AquaticUnderwater",false);}
    private static void waterState(ItemStack stack,Entity entity){
        boolean wet=entity instanceof Player p&&p.isEyeInFluid(FluidTags.WATER);
        if(underwater(stack)!=wet)net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putBoolean("AquaticUnderwater",wet));
    }
    private static final class SeaPickaxe extends Item {
        SeaPickaxe(Properties p){super(p.pickaxe(OwnerAquaticGear.NEPTUNIUM,1,-2.8f));}
        @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){waterState(stack,entity);}
        @Override public float getDestroySpeed(ItemStack stack,BlockState state){return super.getDestroySpeed(stack,state)*(underwater(stack)?25:1);}
    }
    private static final class SeaShovel extends Item {
        SeaShovel(Properties p){super(p.shovel(OwnerAquaticGear.NEPTUNIUM,1.5f,-3));}
        @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){waterState(stack,entity);}
        @Override public float getDestroySpeed(ItemStack stack,BlockState state){return super.getDestroySpeed(stack,state)*(underwater(stack)?25:1);}
    }
    private static final class SeaHoe extends Item {
        SeaHoe(Properties p){super(p.hoe(OwnerAquaticGear.NEPTUNIUM,-3,.2f));}
        @Override public InteractionResult useOn(UseOnContext c){
            if(c.getPlayer() instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
            var state=c.getLevel().getBlockState(c.getClickedPos());boolean till=state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT)||state.is(Blocks.DIRT_PATH);
            if(till&&c.getClickedFace()!=Direction.DOWN&&c.getLevel().isEmptyBlock(c.getClickedPos().above())){
                if(c.getLevel() instanceof ServerLevel level){level.setBlock(c.getClickedPos(),FARMLAND.defaultBlockState(),3);level.playSound(null,c.getClickedPos(),SoundEvents.HOE_TILL.value(),SoundSource.BLOCKS,1,1);if(c.getPlayer()!=null)c.getItemInHand().hurtAndBreak(1,c.getPlayer(),c.getHand().asEquipmentSlot());}return InteractionResult.SUCCESS;
            }
            return super.useOn(c);
        }
    }
    private static final class SeaArmor extends Item {
        private final ArmorType type;
        SeaArmor(Properties p,ArmorType type){super(armor(p,type));this.type=type;}
        private static Properties armor(Properties p,ArmorType type){p.humanoidArmor(ARMOR,type);if(type==ArmorType.BOOTS)p.attributes(ARMOR.createAttributes(type).withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY,new AttributeModifier(Jugcraft.id("aquatic_swimming"),1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.FEET));return p;}
        @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){
            if(!(entity instanceof Player p)||slot!=type.getSlot()||p.getItemBySlot(slot)!=stack||!p.isEyeInFluid(FluidTags.WATER))return;
            if(type==ArmorType.HELMET)p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,false,false,false));
            if(type==ArmorType.CHESTPLATE)p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,40,0,false,false,false));
            if(type==ArmorType.LEGGINGS&&!p.isCrouching()&&!p.isSwimming()&&!p.isSpectator()&&p.getDeltaMovement().y<0)p.setDeltaMovement(p.getDeltaMovement().multiply(1,.5,1));
        }
    }
}

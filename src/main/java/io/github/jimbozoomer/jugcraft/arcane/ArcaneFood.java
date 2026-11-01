package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;

/** Original owner food values and effects; soup returns its crafting/consumption bowl. */
final class ArcaneFood extends Item {
    private final String name;
    ArcaneFood(String name,Properties p){super(properties(name,p));this.name=name;}
    private static Properties properties(String name,Properties p){int n=switch(name){case "bat_soup"->7;case "bat_wing"->3;case "tentacle"->2;default->5;};float s=switch(name){case "bat_soup"->.7F;case "bat_wing"->.2F;case "tentacle"->.1F;default->.6F;};p.food(new FoodProperties.Builder().nutrition(n).saturationModifier(s).build());if(name.equals("bat_soup"))p.stacksTo(1).craftRemainder(Items.BOWL).usingConvertsTo(Items.BOWL);return p;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity e){var result=super.finishUsingItem(stack,l,e);if(!l.isClientSide()){if(name.equals("bat_soup"))e.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240));else if(name.equals("bat_wing")&&l.getRandom().nextFloat()<.9F)e.addEffect(new MobEffectInstance(MobEffects.POISON,160));}return result;}
}

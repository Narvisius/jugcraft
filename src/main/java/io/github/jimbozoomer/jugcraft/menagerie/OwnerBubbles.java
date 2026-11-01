package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.inventory.ClickAction;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Refillable soap blower and fragile, soap-preserved stasis platforms. */
public final class OwnerBubbles {
    public static final SimpleParticleType SUDS=net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple();
    public static final SimpleParticleType POP=net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple();
    public static final int CAPACITY=200;
    private OwnerBubbles(){}
    public static void register(){
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("supps_suds"),SUDS);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("supps_bubble_pop"),POP);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("supps_bubble_fill"),Fill.SERIALIZER);
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(OwnerSupplementaries.ITEMS.get("bubble_blower"),new net.minecraft.core.dispenser.DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(net.minecraft.core.dispenser.BlockSource source,ItemStack stack){
                var level=source.level();var dir=source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);var pos=source.pos().relative(dir);int cost=stasis(stack)?5:1;if(charges(stack)<cost)return stack;
                if(stasis(stack)){
                    if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos).isAir()||io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(level,pos)||!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,source.pos(),pos))return stack;
                    if(!level.setBlockAndUpdate(pos,OwnerSupplementaries.BLOCKS.get("bubble_block").defaultBlockState()))return stack;if(level.getBlockEntity(pos) instanceof OwnerSupplementData d){d.pulseUntil=level.getGameTime();d.changed();}
                }else{var at=Vec3.atCenterOf(source.pos()).add(dir.getStepX()*.65,dir.getStepY()*.65,dir.getStepZ()*.65);for(int n=0;n<5;n++)level.sendParticles(SUDS,at.x,at.y,at.z,0,dir.getStepX()*.15+level.getRandom().nextGaussian()*.02,dir.getStepY()*.15,dir.getStepZ()*.15+level.getRandom().nextGaussian()*.02,1);}
                charges(stack,charges(stack)-cost);level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get(stasis(stack)?"bubble_place":"bubble_blow"),SoundSource.BLOCKS,.6F,1);return stack;
            }
        });
    }
    public static int charges(ItemStack stack){return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("SoapCharges",0),0,CAPACITY);}
    static void charges(ItemStack stack,int value){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putInt("SoapCharges",Math.clamp(value,0,CAPACITY)));}
    static boolean stasis(ItemStack stack){return stack.getEnchantments().entrySet().stream().anyMatch(e->e.getIntValue()>0&&e.getKey().is(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,Jugcraft.id("supps_stasis"))));}
    public static boolean gentle(Entity entity){return entity instanceof LivingEntity living&&living.getItemBySlot(EquipmentSlot.FEET).getEnchantments().entrySet().stream().anyMatch(e->e.getIntValue()>0&&e.getKey().is(TagKey.create(Registries.ENCHANTMENT,Jugcraft.id("supps_prevents_bubble_pop"))));}
    public static VoxelShape collision(CollisionContext context,BlockPos pos){return context.isAbove(Shapes.block(),pos,false)&&context instanceof EntityCollisionContext ec&&ec.getEntity() instanceof LivingEntity?Shapes.block():Shapes.empty();}
    public static void tick(ServerLevel level,OwnerSupplementData data){
        data.bubbleAge=Math.min(100000,data.bubbleAge+1);
        if(data.bubbleAge%20==0){for(Direction dir:Direction.values())if(level.hasChunkAt(data.getBlockPos().relative(dir))&&level.getBlockState(data.getBlockPos().relative(dir)).is(OwnerSupplementaries.BLOCKS.get("soap_block"))){data.bubbleAge=0;break;}data.setChanged();}
        if(data.bubbleAge>1200&&level.getRandom().nextInt(500)==0)pop(level,data.getBlockPos());
    }
    public static void pop(Level world,BlockPos pos){if(!(world instanceof ServerLevel level)||!level.getBlockState(pos).is(OwnerSupplementaries.BLOCKS.get("bubble_block")))return;
        if(level.removeBlock(pos,false)){level.sendParticles(POP,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,0,0,0,0);level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get("bubble_pop"),SoundSource.BLOCKS,.8F,1);}
    }
    public static void contact(Level level,BlockPos pos,Entity entity,boolean inside){if(level.isClientSide()||gentle(entity))return;if(entity instanceof Player who&&!Menagerie.mayChange(level,who,pos))return;if(inside)pop(level,pos);else level.scheduleTick(pos,OwnerSupplementaries.BLOCKS.get("bubble_block"),5);}
    public static final class Blower extends Item {
        Blower(Properties p){super(p.stacksTo(1).enchantable(1));}
        @Override public InteractionResult use(Level level,Player who,InteractionHand hand){
            if(who.isSpectator())return InteractionResult.FAIL;var stack=who.getItemInHand(hand);
            if(charges(stack)==0){if(!level.isClientSide())who.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Refill this blower with soap in crafting, or right-click soap onto it."));return InteractionResult.FAIL;}
            if(stasis(stack)){
                if(charges(stack)<5&&!who.isCreative())return InteractionResult.FAIL;
                var hit=who.pick(who.isCreative()?5:2,1,true);if(!(hit instanceof BlockHitResult b))return InteractionResult.PASS;
                var pos=b.getBlockPos();if(hit.getType()!=HitResult.Type.MISS&&!level.getBlockState(pos).canBeReplaced())pos=pos.relative(b.getDirection());
                if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos).isAir()||!Menagerie.mayChange(level,who,pos))return InteractionResult.FAIL;
                if(!level.isClientSide()&&level.setBlockAndUpdate(pos,OwnerSupplementaries.BLOCKS.get("bubble_block").defaultBlockState())){
                    if(level.getBlockEntity(pos) instanceof OwnerSupplementData data){data.owner=who.getUUID();data.pulseUntil=level.getGameTime();data.changed();}
                    if(!who.isCreative())charges(stack,charges(stack)-5);who.getCooldowns().addCooldown(stack,5);level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get("bubble_place"),SoundSource.BLOCKS,.7F,1);
                }return InteractionResult.SUCCESS;
            }
            who.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 72000;}
        @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
            if(!(level instanceof ServerLevel server))return;if(charges(stack)<=0){user.stopUsingItem();return;}
            if(!(user instanceof Player p&&p.isCreative()))charges(stack,charges(stack)-1);
            var v=user.getLookAngle();var at=user.getEyePosition().add(v).add(0,-.12,0);server.sendParticles(SUDS,at.x,at.y,at.z,0,v.x*.15,v.y*.15,v.z*.15,1);
            if(remaining%4==0)server.playSound(null,user.blockPosition(),OwnerSupplementaries.SOUNDS.get("bubble_blow"),SoundSource.PLAYERS,.5F,.9F+server.getRandom().nextFloat()*.2F);
        }
        @Override public boolean isBarVisible(ItemStack stack){return charges(stack)>0;}
        @Override public int getBarWidth(ItemStack stack){return Math.round(13F*charges(stack)/CAPACITY);}
        @Override public int getBarColor(ItemStack stack){return 15246564;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<net.minecraft.network.chat.Component> lines,TooltipFlag f){lines.accept(net.minecraft.network.chat.Component.literal(charges(stack)+" / 200 soap charges"));lines.accept(net.minecraft.network.chat.Component.literal("Stasis enchantment creates temporary platforms. Soap blocks preserve them; Feather Falling prevents popping underfoot."));}
        @Override public boolean overrideOtherStackedOnMe(ItemStack stack,ItemStack held,Slot slot,ClickAction action,Player who,SlotAccess access){if(action!=ClickAction.SECONDARY||!slot.allowModification(who)||!held.is(OwnerSupplementaries.ITEMS.get("soap"))||charges(stack)==CAPACITY)return false;held.shrink(1);charges(stack,CAPACITY);return true;}
    }
    public static final class Fill extends CustomRecipe {
        static final Fill INSTANCE=new Fill();static final RecipeSerializer<Fill> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Fill>unit(INSTANCE));
        ItemStack result(CraftingInput input){ItemStack blower=ItemStack.EMPTY;int soap=0;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(s.getItem() instanceof Blower&&blower.isEmpty())blower=s;else if(s.is(OwnerSupplementaries.ITEMS.get("soap")))soap++;else return ItemStack.EMPTY;}if(blower.isEmpty()||soap!=1||charges(blower)==CAPACITY)return ItemStack.EMPTY;var out=blower.copyWithCount(1);charges(out,CAPACITY);return out;}
        @Override public boolean matches(CraftingInput input,Level level){return !result(input).isEmpty();}
        @Override public ItemStack assemble(CraftingInput input){return result(input);}
        @Override public RecipeSerializer<Fill> getSerializer(){return SERIALIZER;}
    }
}

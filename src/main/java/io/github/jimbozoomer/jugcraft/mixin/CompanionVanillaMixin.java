package io.github.jimbozoomer.jugcraft.mixin;
import local.peepo.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({AbstractFurnaceBlockEntity.class,BrewingStandBlockEntity.class,CampfireBlockEntity.class,CrafterBlockEntity.class,BeehiveBlockEntity.class})
public abstract class CompanionVanillaMixin implements VanillaCompanionAccess {
 @Unique private VanillaCompanionPort peepo$logistics;
 public VanillaCompanionPort peepo$port(){if(peepo$logistics==null)peepo$logistics=new VanillaCompanionPort((BlockEntity)(Object)this);return peepo$logistics;}
 public VanillaCompanionPort peepo$existingPort(){return peepo$logistics;}
 @Inject(method="loadAdditional",at=@At("RETURN")) private void peepo$load(ValueInput in,CallbackInfo ci){
  peepo$logistics=null;
  if(in.read("PeepoBrewChain",net.minecraft.resources.Identifier.CODEC.listOf()).isPresent() || in.getBooleanOr("PeepoManaged",false) || in.read("PeepoVanillaRecipe",net.minecraft.resources.Identifier.CODEC).isPresent() || in.read("CompanionFilter",net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf()).isPresent())peepo$port().load(in);
 }
 @Inject(method="saveAdditional",at=@At("RETURN")) private void peepo$save(ValueOutput out,CallbackInfo ci){if(peepo$logistics!=null)peepo$logistics.save(out);}
}

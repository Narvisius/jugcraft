package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Food for a tamed pet (the menu, the kitchen and cooking expansion's slice 3; tools/menu.py PETS): its owner uses it on
 * their {@code animal} (Dog Food on a wolf, Horse Feed on a horse) to restore {@code heal} health and give it two
 * effects for a while. A bowl of it gives the bowl back. Fed before the animal's own handling (which would sit a wolf
 * down or mount a horse), and only on the server's word: someone else's pet, or an animal that is not tamed, is left
 * to vanilla.
 */
public class PetFoodItem extends Item {
	/** An effect the food gives, for {@code seconds}. */
	public record Treat(Holder<MobEffect> effect, int seconds) {
	}

	private final EntityType<?> animal;
	private final int heal;
	private final boolean bowl;
	private final List<Treat> treats;

	public PetFoodItem(Properties properties, EntityType<?> animal, int heal, boolean bowl, List<Treat> treats) {
		super(properties);
		this.animal = animal;
		this.heal = heal;
		this.bowl = bowl;
		this.treats = List.copyOf(treats);
	}

	public EntityType<?> animal() {
		return animal;
	}

	public int heal() {
		return heal;
	}

	public List<Treat> treats() {
		return treats;
	}

	/** Whether {@code player} may feed this to {@code entity}: the right animal, tamed, and theirs. */
	public boolean feeds(Player player, Entity entity) {
		return entity.getType() == animal && entity instanceof LivingEntity living && living.isAlive()
				&& entity instanceof OwnableEntity pet && player.equals(pet.getOwner());
	}

	/** The use-entity event: feeds the pet the food in {@code hand}, or PASS when it isn't pet food for this animal. */
	public static InteractionResult feed(Player player, Level level, InteractionHand hand, Entity entity) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || !(stack.getItem() instanceof PetFoodItem food) || !food.feeds(player, entity)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			LivingEntity pet = (LivingEntity) entity;
			pet.heal(food.heal);
			for (Treat treat : food.treats) {
				pet.addEffect(new MobEffectInstance(treat.effect(), treat.seconds() * 20), player);
			}
			pet.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
			server.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() * 0.8, pet.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
			boolean free = player.hasInfiniteMaterials();
			stack.consume(1, player);
			if (food.bowl && !free) {
				ItemStack empty = new ItemStack(Items.BOWL);
				if (stack.isEmpty()) {
					player.setItemInHand(hand, empty);
				} else if (!player.getInventory().add(empty)) {
					player.spawnAtLocation(server, empty);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}

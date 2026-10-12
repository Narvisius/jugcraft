package io.github.jimbozoomer.jugcraft.guns;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Guns (docs/features/guns.md): the owner's guns, built from the owner's models and played with the owner's animations
 * through GeckoLib, the rounds they fire and the attachments fitted to them.
 * <ul>
 * <li>Left click fires (held, for an automatic), right click (held) aims down the sights, G reloads, H inspects
 * (client/GunsClient). The client asks; the server fires, loads and checks everything ({@link GunShots}).</li>
 * <li>A gun holds its loaded rounds in {@link #LOADED}; reloading takes rounds from the inventory.</li>
 * <li>Shots are hitscan: each bullet (or pellet) follows the shooter's look, strayed by the gun's spread, to the first
 * block or creature within range.</li>
 * <li>Slice 8C, the heavy weapons: the Trench Lobber lobs a Grenade a shot, the Stoker's shots are bursts of flame
 * ({@link #SHOTS}); the Thresher's barrels spin up before it fires ({@link #SPIN_UP}).</li>
 * <li>Slice 8D, the energy weapons: the Beam Pistol's beam passes through every creature in its line, the Stormlock's and
 * Linesman's arcs leap between creatures ({@link #SHOTS}). They run on charge from the energy system: a reload draws each
 * round's {@link #CHARGE} from the Energy Cells in the inventory, which the Charging Station fills.</li>
 * <li>Slice 9A, the marksman rifles: semi-automatic rifles in steel, the steadiest aimed and the farthest reaching (the
 * Picket, Ranger and Kestrel Rifles); bullets like the rest.</li>
 * <li>Slice 9B, the automatic weapons: the Rattler Pistol and Bronco SMG, held in one hand, and the Squall air rifle, each
 * firing for as long as the trigger is held.</li>
 * <li>Slice 9C, the second energy weapons, on the same cells and shots as slice 8D's: the Spikedriver's heavy beam and the
 * Seam Cutter's short one, held on, and the Caisson Pistol's arc.</li>
 * <li>Slice 9D, the pump shotguns, loaded a shell at a time: the heavy Sledge, the long Highwayman (worked by its bolt,
 * not a pump) and the quick Throttle; pellets like the other shotguns.</li>
 * <li>Slice 10A, the launchers: the Earthmover and the Skylark Rifle fire the rocketry branch's High-Explosive Rockets as
 * rockets ({@link #SHOTS}, {@link #ROCKET_SPEED}), each bursting where it hits or at the end of the gun's range; the
 * Bullfrog lobs grenades as the Trench Lobber does, one a reload.</li>
 * <li>Slice 10B, coil and plasma, on the same cells and shots as slice 8D's: the Solenoid Rifle's heavy beam, the
 * farthest-reaching, the Votive Rifle's short bolts, held on, and the Glowmouth's arc, loaded a charge at a time.</li>
 * <li>Slice 10C, the double-barrels, each firing a spread of pellets: the Mule breaks open to load both its buckshot
 * shells at once; the Fowler, a double flintlock, and the Culverin, a hand cannon, are loaded down their muzzles with
 * paper cartridges, as the slice 4 muzzle-loaders are.</li>
 * <li>Slice 10D, the sculk guns, on the rounds the other guns fire: the Undertone Rifle, one hard shot a pull; the
 * Murmur SMG, ten shots a second; and the Reverb, a double-barrel loaded a shell at a time.</li>
 * <li>Slice 10G, two guns at once: with a one-handed gun in each hand ({@link #ONE_HANDED}), left click fires the main
 * hand's and right click the other's; neither is aimed, and each strays further from the hip ({@link #DUAL_SPREAD}).</li>
 * <li>Slice 11A, the Nether guns, on the rounds the other guns fire: the Tusker, a pump shotgun loaded a shell at a
 * time; the Cinder Repeater, an automatic; and the Bastion Rifle, loaded a round at a time through its trapdoor. Their
 * shots set what they hit alight ({@link #INCENDIARY}).</li>
 * <li>Slice 11B, the rest of the Nether guns, whose shots set what they hit alight too: the Ashfall Pistol, held in one
 * hand ({@link #ONE_HANDED}), the tank under its barrel changed to reload; the Goldbristle Carbine, fed from a magazine
 * at its side; and the Crackling, a long pump shotgun of six heavy pellets, loaded a shell at a time.</li>
 * <li>Attachments ({@link #ATTACHMENTS}), one a slot, are fitted in a crafting grid ({@link GunAttachmentRecipe}) and
 * held in {@link #FITTED}; they change the gun's numbers ({@link GunItem#spec(ItemStack)}) and show on its model.
 * Slice 9E adds the Tactical Grip and the Laser Sight, whose red dot the client draws where the gun points.</li>
 * </ul>
 * The numbers are tools/guns.py's GUNS; tools/check_mod_data.py keeps the two the same.
 */
public final class JugcraftGuns {
	/** Each gun's numbers, in the order the creative tab shows them. */
	public static final Map<String, GunSpec> SPECS = new LinkedHashMap<>();

	static {
		SPECS.put("rust_midge", new GunSpec(2.0F, 1, 3, true, 20, 47, 0, 0, 0, 4.0F, 1.5F, 48, "light_round"));
		SPECS.put("patchwork_carbine", new GunSpec(6.0F, 1, 6, false, 10, 48, 0, 0, 0, 2.0F, 0.25F, 96, "rifle_round"));
		SPECS.put("thunderpipe", new GunSpec(2.5F, 8, 8, false, 2, 0, 5, 12, 13, 9.0F, 6.0F, 24, "buckshot_shell"));
		SPECS.put("warden_pistol", new GunSpec(4.0F, 1, 5, false, 12, 47, 0, 0, 0, 2.5F, 0.75F, 56, "light_round"));
		SPECS.put("riveter_smg", new GunSpec(2.5F, 1, 3, true, 30, 45, 0, 0, 0, 3.0F, 1.0F, 48, "light_round"));
		SPECS.put("haymaker", new GunSpec(3.0F, 8, 14, false, 5, 0, 7, 11, 23, 7.0F, 5.0F, 28, "buckshot_shell"));
		SPECS.put("longhorn_rifle", new GunSpec(8.0F, 1, 13, false, 6, 0, 8, 11, 14, 1.5F, 0.15F, 120, "rifle_round"));
		SPECS.put("drover_rifle", new GunSpec(6.5F, 1, 13, false, 10, 0, 8, 12, 17, 2.0F, 0.3F, 100, "rifle_round"));
		SPECS.put("coach_gun", new GunSpec(3.0F, 8, 8, false, 2, 0, 12, 13, 13, 6.0F, 4.0F, 32, "buckshot_shell"));
		SPECS.put("duelling_pistol", new GunSpec(9.0F, 1, 20, false, 1, 74, 0, 0, 0, 4.0F, 2.0F, 32, "paper_cartridge"));
		SPECS.put("line_musket", new GunSpec(14.0F, 1, 8, false, 1, 78, 0, 0, 0, 2.5F, 0.75F, 64, "paper_cartridge"));
		SPECS.put("bellmouth", new GunSpec(2.5F, 10, 10, false, 1, 78, 0, 0, 0, 12.0F, 9.0F, 20, "paper_cartridge"));
		SPECS.put("bulldog_pistol", new GunSpec(11.0F, 1, 10, false, 1, 45, 0, 0, 0, 3.0F, 1.0F, 56, "rifle_round"));
		SPECS.put("marshal_revolver", new GunSpec(5.0F, 1, 8, false, 6, 0, 11, 25, 11, 2.0F, 0.5F, 72, "light_round"));
		SPECS.put("sapper_revolver", new GunSpec(4.5F, 1, 6, false, 6, 0, 9, 12, 13, 2.5F, 0.8F, 48, "light_round"));
		SPECS.put("sentry_pistol", new GunSpec(5.0F, 1, 5, false, 8, 47, 0, 0, 0, 2.0F, 0.6F, 64, "light_round"));
		SPECS.put("garrison_rifle", new GunSpec(4.0F, 1, 3, true, 30, 53, 0, 0, 0, 3.0F, 0.6F, 80, "rifle_round"));
		SPECS.put("breacher", new GunSpec(3.0F, 8, 16, false, 6, 52, 0, 0, 0, 7.0F, 5.0F, 28, "buckshot_shell"));
		SPECS.put("trench_lobber", new GunSpec(16.0F, 1, 14, false, 6, 53, 0, 0, 0, 3.0F, 1.0F, 24, "grenade"));
		SPECS.put("thresher", new GunSpec(3.0F, 1, 2, true, 60, 78, 0, 0, 0, 4.0F, 2.0F, 64, "rifle_round"));
		SPECS.put("stoker", new GunSpec(2.0F, 1, 4, true, 32, 61, 0, 0, 0, 10.0F, 6.0F, 8, "minecraft:blaze_powder"));
		SPECS.put("beam_pistol", new GunSpec(6.0F, 1, 8, false, 8, 48, 0, 0, 0, 1.5F, 0.5F, 48, "energy_cell"));
		SPECS.put("stormlock_rifle", new GunSpec(9.0F, 1, 14, false, 5, 0, 18, 17, 20, 2.0F, 0.5F, 64, "energy_cell"));
		SPECS.put("linesman", new GunSpec(4.0F, 1, 6, true, 6, 0, 8, 13, 12, 15.0F, 10.0F, 12, "energy_cell"));
		SPECS.put("picket_rifle", new GunSpec(8.0F, 1, 8, false, 10, 43, 0, 0, 0, 2.0F, 0.1F, 128, "rifle_round"));
		SPECS.put("ranger_rifle", new GunSpec(10.0F, 1, 10, false, 10, 45, 0, 0, 0, 2.5F, 0.15F, 120, "rifle_round"));
		SPECS.put("kestrel_rifle", new GunSpec(9.0F, 1, 9, false, 8, 55, 0, 0, 0, 2.0F, 0.15F, 128, "rifle_round"));
		SPECS.put("rattler_pistol", new GunSpec(3.0F, 1, 3, true, 20, 48, 0, 0, 0, 4.0F, 2.0F, 48, "light_round"));
		SPECS.put("bronco_smg", new GunSpec(3.5F, 1, 3, true, 25, 53, 0, 0, 0, 5.0F, 2.5F, 40, "light_round"));
		SPECS.put("squall_rifle", new GunSpec(2.5F, 1, 2, true, 40, 57, 0, 0, 0, 3.0F, 0.8F, 64, "light_round"));
		SPECS.put("spikedriver", new GunSpec(12.0F, 1, 16, false, 6, 67, 0, 0, 0, 1.5F, 0.3F, 64, "energy_cell"));
		SPECS.put("seam_cutter", new GunSpec(1.5F, 1, 2, true, 60, 60, 0, 0, 0, 2.0F, 1.0F, 16, "energy_cell"));
		SPECS.put("caisson_pistol", new GunSpec(5.0F, 1, 8, false, 10, 57, 0, 0, 0, 6.0F, 3.0F, 24, "energy_cell"));
		SPECS.put("sledge", new GunSpec(4.0F, 8, 20, false, 4, 0, 8, 14, 14, 8.0F, 6.0F, 24, "buckshot_shell"));
		SPECS.put("highwayman", new GunSpec(3.0F, 8, 20, false, 7, 0, 18, 13, 16, 6.0F, 2.5F, 40, "buckshot_shell"));
		SPECS.put("throttle", new GunSpec(3.0F, 8, 16, false, 6, 0, 10, 13, 22, 6.5F, 4.5F, 28, "buckshot_shell"));
		SPECS.put("earthmover", new GunSpec(24.0F, 1, 20, false, 4, 61, 0, 0, 0, 2.5F, 1.0F, 72, "he_rocket"));
		SPECS.put("skylark_rifle", new GunSpec(24.0F, 1, 20, false, 1, 48, 0, 0, 0, 1.5F, 0.25F, 135, "he_rocket"));
		SPECS.put("bullfrog", new GunSpec(16.0F, 1, 20, false, 1, 45, 0, 0, 0, 3.0F, 1.5F, 20, "grenade"));
		SPECS.put("solenoid_rifle", new GunSpec(14.0F, 1, 16, false, 5, 52, 0, 0, 0, 1.5F, 0.15F, 128, "energy_cell"));
		SPECS.put("votive_rifle", new GunSpec(3.0F, 1, 3, true, 30, 50, 0, 0, 0, 3.0F, 1.0F, 48, "energy_cell"));
		SPECS.put("glowmouth", new GunSpec(10.0F, 1, 20, false, 4, 0, 8, 13, 18, 12.0F, 9.0F, 14, "energy_cell"));
		SPECS.put("mule", new GunSpec(2.5F, 10, 8, false, 2, 35, 0, 0, 0, 9.0F, 7.0F, 24, "buckshot_shell"));
		SPECS.put("fowler", new GunSpec(3.0F, 8, 10, false, 2, 97, 0, 0, 0, 8.0F, 5.0F, 28, "paper_cartridge"));
		SPECS.put("culverin", new GunSpec(5.0F, 5, 20, false, 1, 74, 0, 0, 0, 9.0F, 7.0F, 18, "paper_cartridge"));
		SPECS.put("undertone_rifle", new GunSpec(9.0F, 1, 7, false, 12, 48, 0, 0, 0, 2.5F, 0.3F, 96, "rifle_round"));
		SPECS.put("murmur_smg", new GunSpec(3.0F, 1, 2, true, 24, 48, 0, 0, 0, 3.5F, 1.5F, 48, "light_round"));
		SPECS.put("reverb", new GunSpec(3.5F, 10, 8, false, 2, 0, 16, 17, 19, 7.0F, 5.0F, 32, "buckshot_shell"));
		SPECS.put("tusker", new GunSpec(3.0F, 8, 14, false, 6, 0, 12, 12, 13, 6.5F, 4.5F, 28, "buckshot_shell"));
		SPECS.put("cinder_repeater", new GunSpec(3.5F, 1, 3, true, 20, 48, 0, 0, 0, 3.0F, 1.2F, 48, "light_round"));
		SPECS.put("bastion_rifle", new GunSpec(14.0F, 1, 18, false, 5, 0, 15, 13, 16, 2.0F, 0.15F, 112, "rifle_round"));
		SPECS.put("ashfall_pistol", new GunSpec(6.0F, 1, 8, false, 8, 47, 0, 0, 0, 2.5F, 0.8F, 56, "light_round"));
		SPECS.put("goldbristle_carbine", new GunSpec(7.0F, 1, 7, false, 12, 41, 0, 0, 0, 2.5F, 0.4F, 88, "rifle_round"));
		SPECS.put("crackling", new GunSpec(4.0F, 6, 20, false, 5, 0, 6, 10, 11, 5.0F, 3.0F, 36, "buckshot_shell"));
	}

	/**
	 * What the slice 8C and 8D guns fire, where it is not bullets (tools/guns.py GUNS "shot"): {@link #GRENADE}, a Grenade
	 * lobbed from the muzzle; {@link #FLAME}, a short jet of flame; {@link #BEAM}, a beam through every creature in its
	 * line; {@link #ARC}, a bolt that leaps from creature to creature; {@link #ROCKET} (slice 10A), a High-Explosive Rocket
	 * ({@link GunShots}).
	 */
	public static final Map<String, String> SHOTS = Map.ofEntries(Map.entry("trench_lobber", "grenade"), Map.entry("stoker", "flame"), Map.entry("beam_pistol", "beam"), Map.entry("stormlock_rifle", "arc"), Map.entry("linesman", "arc"), Map.entry("spikedriver", "beam"), Map.entry("seam_cutter", "beam"), Map.entry("caisson_pistol", "arc"), Map.entry("earthmover", "rocket"), Map.entry("skylark_rifle", "rocket"), Map.entry("bullfrog", "grenade"), Map.entry("solenoid_rifle", "beam"), Map.entry("votive_rifle", "beam"), Map.entry("glowmouth", "arc"));
	public static final String GRENADE = "grenade";
	public static final String FLAME = "flame";
	public static final String BEAM = "beam";
	public static final String ARC = "arc";
	public static final String ROCKET = "rocket";
	/**
	 * Slice 10A: how fast a rocket gun's rockets fly, in blocks a tick (tools/guns.py GUNS "rocket_speed"); each bursts
	 * at the end of the gun's range if it has hit nothing by then.
	 */
	public static final Map<String, Float> ROCKET_SPEED = Map.of("earthmover", 3.0F, "skylark_rifle", 4.5F);
	/**
	 * Slice 8D: the JE each round of an energy weapon draws from the Energy Cells in the inventory as it loads
	 * (tools/guns.py GUNS "charge"); a gun not listed loads rounds of its ammunition.
	 */
	public static final Map<String, Integer> CHARGE = Map.of("beam_pistol", 400, "stormlock_rifle", 750, "linesman", 250, "spikedriver", 800, "seam_cutter", 100, "caisson_pistol", 300, "solenoid_rifle", 1100, "votive_rifle", 200, "glowmouth", 750);
	/**
	 * An arc leaps on from its first creature to at most {@link #ARC_HOPS} more, each the nearest within {@link #ARC_REACH}
	 * blocks of the last, each taking {@link #ARC_SHARE} of the damage before it (tools/guns.py).
	 */
	public static final int ARC_HOPS = 2;
	public static final double ARC_REACH = 4.0;
	public static final float ARC_SHARE = 0.6F;
	/** Ticks the trigger is held, the barrels spinning up, before the gun fires (tools/guns.py GUNS "spin_up"). */
	public static final Map<String, Integer> SPIN_UP = Map.of("thresher", 15);
	/**
	 * Rounds one item of ammunition loads, where it is not one (tools/guns.py OTHER_AMMO): a blaze powder fuels four of
	 * the Stoker's bursts.
	 */
	public static final Map<String, Integer> PER_ITEM = Map.of("minecraft:blaze_powder", 4);
	/**
	 * Slice 10G, two guns at once: the guns held in one hand (tools/guns.py one_handed()), of which a player may hold one
	 * in each hand and fire both ({@link GunItem#dual}).
	 */
	public static final Set<String> ONE_HANDED = Set.of("warden_pistol", "haymaker", "duelling_pistol", "bulldog_pistol", "marshal_revolver", "sapper_revolver", "sentry_pistol", "beam_pistol", "rattler_pistol", "bronco_smg", "spikedriver", "caisson_pistol", "culverin", "ashfall_pistol");
	/** Slice 10G: how much further each of two guns strays from the hip than one held alone (tools/guns.py DUAL_SPREAD). */
	public static final float DUAL_SPREAD = 1.5F;
	/**
	 * Slices 11A and 11B, the Nether guns: the guns whose bullets set each creature they hurt alight, for
	 * {@link GunShots#BURN_SECONDS} as the Stoker's flame does (tools/guns.py INCENDIARY).
	 */
	public static final Set<String> INCENDIARY = Set.of("tusker", "cinder_repeater", "bastion_rifle", "ashfall_pistol", "goldbristle_carbine", "crackling");

	/** The attachments, in the order the creative tab shows them (tools/guns.py ATTACHMENTS). */
	public static final Map<String, GunAttachment> ATTACHMENTS = new LinkedHashMap<>();

	static {
		ATTACHMENTS.put("silencer", new GunAttachment("barrel", false, 0.95F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.35F, 0.0F));
		ATTACHMENTS.put("baffled_silencer", new GunAttachment("barrel", false, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.2F, 0.0F));
		ATTACHMENTS.put("muzzle_brake", new GunAttachment("barrel", false, 1.0F, 1.0F, 1.0F, 0.85F, 1.0F, 1.0F, 0.5F, 1.0F, 0.0F));
		ATTACHMENTS.put("extended_barrel", new GunAttachment("barrel", true, 1.0F, 1.3F, 0.85F, 0.85F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("extended_magazine", new GunAttachment("magazine", true, 1.0F, 1.0F, 1.0F, 1.0F, 1.5F, 1.15F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("speed_magazine", new GunAttachment("magazine", true, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.65F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("light_stock", new GunAttachment("stock", true, 1.0F, 1.0F, 0.85F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("weighted_stock", new GunAttachment("stock", true, 1.0F, 1.0F, 1.0F, 0.7F, 1.0F, 1.0F, 0.6F, 1.0F, 0.0F));
		ATTACHMENTS.put("wooden_stock", new GunAttachment("stock", true, 1.0F, 1.0F, 0.9F, 0.85F, 1.0F, 1.0F, 0.75F, 1.0F, 0.0F));
		ATTACHMENTS.put("light_grip", new GunAttachment("grip", false, 1.0F, 1.0F, 0.8F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("vertical_grip", new GunAttachment("grip", false, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.65F, 1.0F, 0.0F));
		ATTACHMENTS.put("iron_bayonet", new GunAttachment("grip", false, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 4.0F));
		ATTACHMENTS.put("steel_bayonet", new GunAttachment("grip", false, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 5.0F));
		ATTACHMENTS.put("diamond_bayonet", new GunAttachment("grip", false, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 5.0F));
		ATTACHMENTS.put("netherite_bayonet", new GunAttachment("grip", false, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 6.0F));
		ATTACHMENTS.put("long_scope", new GunAttachment("optic", true, 1.0F, 1.0F, 1.25F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("medium_scope", new GunAttachment("optic", true, 1.0F, 1.0F, 1.1F, 0.7F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("reflex_sight", new GunAttachment("optic", true, 1.0F, 1.0F, 1.0F, 0.85F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
		ATTACHMENTS.put("tactical_grip", new GunAttachment("grip", false, 1.0F, 1.0F, 0.9F, 1.0F, 1.0F, 1.0F, 0.8F, 1.0F, 0.0F));
		ATTACHMENTS.put("laser_sight", new GunAttachment("optic", true, 1.0F, 1.0F, 0.7F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F));
	}

	/** The attachment slots: a gun takes one attachment in each (tools/guns.py SLOTS); "optic" holds a scope. */
	public static final List<String> SLOTS = List.of("barrel", "magazine", "stock", "grip", "optic");
	/** The attachments each gun takes: those the owner made a part of that gun for (tools/guns.py fits()). */
	public static final Map<String, List<String>> ACCEPTS = new LinkedHashMap<>();

	static {
		ACCEPTS.put("rust_midge", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("patchwork_carbine", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"vertical_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet"));
		ACCEPTS.put("thunderpipe", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"light_stock", "weighted_stock", "wooden_stock", "light_grip", "vertical_grip", "iron_bayonet",
				"steel_bayonet", "diamond_bayonet", "netherite_bayonet"));
		ACCEPTS.put("warden_pistol", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine"));
		ACCEPTS.put("riveter_smg", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock", "long_scope",
				"medium_scope", "reflex_sight", "laser_sight"));
		ACCEPTS.put("haymaker", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel"));
		ACCEPTS.put("longhorn_rifle", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"light_stock", "weighted_stock", "wooden_stock", "light_grip", "vertical_grip", "iron_bayonet",
				"steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope", "reflex_sight",
				"laser_sight"));
		ACCEPTS.put("drover_rifle", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope",
				"medium_scope", "reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("coach_gun", List.of("light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
				"netherite_bayonet", "tactical_grip"));
		ACCEPTS.put("duelling_pistol", List.of("light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("line_musket", List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"vertical_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet"));
		ACCEPTS.put("bellmouth", List.of("light_grip", "vertical_grip", "iron_bayonet", "steel_bayonet",
				"diamond_bayonet", "netherite_bayonet"));
		ACCEPTS.put("bulldog_pistol", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"long_scope", "medium_scope", "reflex_sight", "laser_sight"));
		ACCEPTS.put("marshal_revolver", List.of("light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("sapper_revolver", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel"));
		ACCEPTS.put("sentry_pistol", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("garrison_rifle", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope",
				"reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("breacher", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope",
				"reflex_sight", "tactical_grip"));
		ACCEPTS.put("trench_lobber", List.of("extended_magazine", "speed_magazine", "light_stock", "weighted_stock",
				"wooden_stock", "long_scope", "medium_scope", "reflex_sight"));
		ACCEPTS.put("stoker", List.of("light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("beam_pistol", List.of("light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("stormlock_rifle", List.of("light_grip", "vertical_grip", "iron_bayonet", "steel_bayonet",
				"diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "laser_sight"));
		ACCEPTS.put("linesman", List.of("light_stock", "weighted_stock", "wooden_stock", "long_scope", "medium_scope",
				"reflex_sight", "laser_sight"));
		ACCEPTS.put("picket_rifle", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
				"netherite_bayonet", "tactical_grip"));
		ACCEPTS.put("ranger_rifle", List.of("extended_magazine", "speed_magazine", "light_stock", "weighted_stock",
				"wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet",
				"tactical_grip"));
		ACCEPTS.put("kestrel_rifle", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"light_stock", "weighted_stock", "wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet",
				"diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "tactical_grip",
				"laser_sight"));
		ACCEPTS.put("rattler_pistol", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "long_scope", "medium_scope", "reflex_sight", "laser_sight"));
		ACCEPTS.put("bronco_smg", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine"));
		ACCEPTS.put("squall_rifle", List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope",
				"reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("spikedriver", List.of("extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock"));
		ACCEPTS.put("sledge", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel", "light_stock",
				"weighted_stock", "wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
				"netherite_bayonet", "tactical_grip"));
		ACCEPTS.put("highwayman", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"light_stock", "weighted_stock", "wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet",
				"diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "tactical_grip",
				"laser_sight"));
		ACCEPTS.put("throttle", List.of("light_stock", "weighted_stock", "wooden_stock", "long_scope", "medium_scope",
				"reflex_sight", "laser_sight"));
		ACCEPTS.put("earthmover", List.of("light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
				"netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "tactical_grip"));
		ACCEPTS.put("skylark_rifle", List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "tactical_grip"));
		ACCEPTS.put("bullfrog", List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip", "iron_bayonet",
				"steel_bayonet", "diamond_bayonet", "netherite_bayonet", "tactical_grip"));
		ACCEPTS.put("solenoid_rifle", List.of("extended_magazine", "speed_magazine", "light_stock", "weighted_stock",
				"wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet",
				"long_scope", "medium_scope", "reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("votive_rifle", List.of("extended_magazine", "speed_magazine", "light_stock", "weighted_stock",
				"wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet",
				"long_scope", "medium_scope", "reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("fowler", List.of("light_grip", "vertical_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
				"netherite_bayonet"));
		ACCEPTS.put("undertone_rifle", List.of("light_stock", "weighted_stock", "wooden_stock", "long_scope", "medium_scope",
				"reflex_sight", "laser_sight"));
		ACCEPTS.put("murmur_smg", List.of("extended_magazine", "speed_magazine", "long_scope", "medium_scope", "reflex_sight",
				"laser_sight"));
		ACCEPTS.put("reverb", List.of("light_grip", "long_scope", "medium_scope", "reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("tusker", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel", "light_stock",
				"weighted_stock", "wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
				"netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("cinder_repeater", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope",
				"reflex_sight", "tactical_grip", "laser_sight"));
		ACCEPTS.put("bastion_rifle", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"light_stock", "weighted_stock", "wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet",
				"diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "tactical_grip",
				"laser_sight"));
		ACCEPTS.put("ashfall_pistol", List.of("light_stock", "weighted_stock", "wooden_stock", "long_scope", "medium_scope",
				"reflex_sight", "laser_sight"));
		ACCEPTS.put("goldbristle_carbine", List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
				"extended_magazine", "speed_magazine", "light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope",
				"reflex_sight", "tactical_grip", "laser_sight"));
		// The Ribs Glory's barrel attachments are the Killer 23's, too short for its own barrel (tools/guns.py MISFITS).
		ACCEPTS.put("crackling", List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip", "iron_bayonet",
				"steel_bayonet", "diamond_bayonet", "netherite_bayonet", "tactical_grip"));
	}

	/** The rounds. */
	public static final List<String> AMMO = List.of("light_round", "rifle_round", "buckshot_shell", "paper_cartridge");
	/** The sound events the animations and the guns play (assets/jugcraft/sounds.json, written by tools/guns.py). */
	public static final List<String> SOUND_EVENTS = List.of("bolt", "bolt_pull", "bolt_release", "clank", "dry_fire",
			"gun_rustle", "insert", "jam", "lever", "metal", "pump", "pump_half", "rack", "reload_end", "reload_mag_in",
			"reload_mag_out", "shell_in", "slap");
	/** The rounds that leave a spent case, each with its particle jugcraft:&lt;round&gt;_casing (tools/guns.py CASINGS). */
	public static final List<String> CASING_AMMO = List.of("light_round", "rifle_round", "buckshot_shell");
	/** Walking speed while aiming down the sights (vanilla's using an item is 0.2). */
	public static final float AIM_SPEED = 0.6F;

	public static final Map<String, GunItem> GUNS = new LinkedHashMap<>();
	public static final Map<String, Item> ROUNDS = new LinkedHashMap<>();
	public static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();
	public static final Map<String, Item> ATTACHMENT_ITEMS = new LinkedHashMap<>();
	/** Each round's spent case, a particle the client throws from the gun (client/guns/GunEffects). */
	public static final Map<String, SimpleParticleType> CASINGS = new LinkedHashMap<>();
	/** The Laser Sight's fitted name (slice 9E). */
	public static final String LASER_SIGHT = "laser_sight";
	/**
	 * The Laser Sight's dot (slice 9E): a particle the client draws where a gun with a Laser Sight points
	 * (client/guns/GunLaser), shown whatever the particle setting.
	 */
	public static SimpleParticleType LASER_DOT;
	public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("bullet"));
	/** The Stoker's flame: fire, so what fire spares it spares (slice 8C). */
	public static final ResourceKey<DamageType> FLAME_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("flame"));
	/** The energy weapons' beams and arcs (slice 8D): neither projectile nor fire, and they push nothing back. */
	public static final ResourceKey<DamageType> ZAP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("zap"));
	/** The energy weapons' ammunition (slice 8D): charge, filled at the Charging Station. */
	public static Item ENERGY_CELL;
	/** Rounds loaded in a gun. */
	public static DataComponentType<Integer> LOADED;
	/** The attachments fitted to a gun, oldest first; one a slot, so five at most. */
	public static DataComponentType<List<String>> FITTED;
	/**
	 * The grenade a grenade gun's magazine holds, by item id (slice 9G); absent for the frag Grenade, so a gun that has
	 * only ever held those carries nothing new.
	 */
	public static DataComponentType<String> LOADED_GRENADE;
	public static RecipeSerializer<GunAttachmentRecipe> ATTACHMENT_SERIALIZER;
	/** The Cell Rack (slice 10E): six cradles that charge the Energy Cells standing in them at once, from cables. */
	public static Block CELL_RACK;
	public static BlockEntityType<CellRackBlockEntity> CELL_RACK_ENTITY;
	public static RecipeSerializer<GunAttachmentRemovalRecipe> ATTACHMENT_REMOVAL_SERIALIZER;

	private JugcraftGuns() {
	}

	public static void register() {
		LOADED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("loaded_rounds"),
				DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 64)).networkSynchronized(ByteBufCodecs.VAR_INT)
						.build());
		FITTED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("attachments"),
				DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf(0, 5))
						.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(5))).build());
		LOADED_GRENADE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("loaded_grenade"),
				DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
		ATTACHMENT_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("gun_attachment"),
				GunAttachmentRecipe.SERIALIZER);
		ATTACHMENT_REMOVAL_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("gun_attachment_removal"),
				GunAttachmentRemovalRecipe.SERIALIZER);
		for (String event : SOUND_EVENTS) {
			sound("guns." + event);
		}
		for (String gun : SPECS.keySet()) {
			sound("guns." + gun + ".fire");
		}
		for (String round : AMMO) {
			ROUNDS.put(round, JugcraftRegistry.item(round, AmmoItem::new));
		}
		ENERGY_CELL = JugcraftRegistry.item("energy_cell", properties -> new EnergyCellItem(properties.stacksTo(1)
				.rarity(Rarity.UNCOMMON).component(JugcraftTools.ENERGY, 0L)));
		ResourceKey<Block> rackKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("cell_rack"));
		CELL_RACK = Registry.register(BuiltInRegistries.BLOCK, rackKey, new CellRackBlock(BlockBehaviour.Properties
				.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).sound(SoundType.METAL).noOcclusion()
				.lightLevel(state -> state.getValue(CellRackBlock.LIT) ? 5 : 0).setId(rackKey)));
		ResourceKey<Item> rackItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("cell_rack"));
		Registry.register(BuiltInRegistries.ITEM, rackItem,
				new BlockItem(CELL_RACK, new Item.Properties().setId(rackItem).useBlockDescriptionPrefix()));
		CELL_RACK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cell_rack"),
				FabricBlockEntityTypeBuilder.create(CellRackBlockEntity::new, CELL_RACK).build());
		// Cables reach the rack from any side.
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
				entity instanceof CellRackBlockEntity rack ? rack.energy : null, CELL_RACK);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(CELL_RACK));
		for (String round : CASING_AMMO) {
			CASINGS.put(round, Registry.register(BuiltInRegistries.PARTICLE_TYPE, Jugcraft.id(round + "_casing"),
					FabricParticleTypes.simple()));
		}
		LASER_DOT = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Jugcraft.id("laser_dot"), FabricParticleTypes.simple(true));
		SPECS.forEach((name, spec) -> GUNS.put(name, (GunItem) JugcraftRegistry.item(name, properties -> new GunItem(name, spec,
				properties.stacksTo(1).rarity(Rarity.UNCOMMON).component(LOADED, 0)
						.component(DataComponents.USE_EFFECTS, new UseEffects(false, true, AIM_SPEED))))));
		ATTACHMENTS.keySet().forEach(name -> ATTACHMENT_ITEMS.put(name, JugcraftRegistry.item(name,
				properties -> new AttachmentItem(name, properties.stacksTo(16)))));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			GUNS.values().forEach(output::accept);
			ROUNDS.values().forEach(output::accept);
			// A cell full, as the creative tab gives the powered tools.
			ItemStack cell = new ItemStack(ENERGY_CELL);
			Chargeable.setEnergy(cell, Chargeable.capacity(cell));
			output.accept(cell);
			ATTACHMENT_ITEMS.values().forEach(output::accept);
		});
		GunShots.register();
	}

	/** The sound event guns.&lt;name&gt; or guns.&lt;gun&gt;.fire. */
	public static SoundEvent sound(String path) {
		SoundEvent known = SOUNDS.get(path);
		if (known != null) {
			return known;
		}
		Identifier id = Jugcraft.id(path);
		SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
		SOUNDS.put(path, event);
		return event;
	}

	/**
	 * The round a gun fires: one of the rounds here, or another item (slice 8C: the field chemistry branch's Grenade,
	 * jugcraft:grenade, and blaze powder), named by its path in this mod or by its full id.
	 */
	public static Item ammo(GunSpec spec) {
		Item round = ROUNDS.get(spec.ammo());
		if (round != null) {
			return round;
		}
		return BuiltInRegistries.ITEM.getValue(spec.ammo().contains(":") ? Identifier.parse(spec.ammo()) : Jugcraft.id(spec.ammo()));
	}

	/** Whether this gun's rounds are grenades, of any kind (slice 9G: the Trench Lobber). */
	public static boolean takesGrenades(GunSpec spec) {
		return ammo(spec) instanceof GrenadeItem;
	}

	/**
	 * The round in this gun's magazine. A grenade gun's is the grenade it was last loaded with (slice 9G): the frag
	 * Grenade if it has never held another, or if the one it held is no longer known. Any other gun's is its ammunition.
	 */
	public static Item loadedAmmo(ItemStack stack, GunSpec spec) {
		Item ammo = ammo(spec);
		String kind = stack.get(LOADED_GRENADE);
		if (kind == null || !(ammo instanceof GrenadeItem)) {
			return ammo;
		}
		Identifier id = Identifier.tryParse(kind);
		Item grenade = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
		return grenade instanceof GrenadeItem ? grenade : ammo;
	}

	/** Records the grenade a grenade gun's magazine now holds (slice 9G); the frag Grenade is recorded as none. */
	public static void setLoadedAmmo(ItemStack stack, GunSpec spec, Item round) {
		if (round == ammo(spec)) {
			stack.remove(LOADED_GRENADE);
		} else {
			stack.set(LOADED_GRENADE, BuiltInRegistries.ITEM.getKey(round).toString());
		}
	}

	/** Rounds one item of the gun's ammunition loads ({@link #PER_ITEM}). */
	public static int perItem(GunSpec spec) {
		return PER_ITEM.getOrDefault(spec.ammo(), 1);
	}

	/** What the gun fires: "bullet", or another of {@link #SHOTS}' kinds. */
	public static String shot(GunItem gun) {
		return SHOTS.getOrDefault(gun.name(), "bullet");
	}

	/** Blocks a tick this rocket gun's rockets fly (slice 10A, {@link #ROCKET_SPEED}); 0 for any other gun. */
	public static float rocketSpeed(GunItem gun) {
		return ROCKET_SPEED.getOrDefault(gun.name(), 0.0F);
	}

	/** Ticks this gun's barrels spin up before it fires; 0 for a gun without ({@link #SPIN_UP}). */
	public static int spinUp(GunItem gun) {
		return SPIN_UP.getOrDefault(gun.name(), 0);
	}

	/** Whether this gun's bullets set what they hurt alight (slice 11A, {@link #INCENDIARY}). */
	public static boolean ignites(GunItem gun) {
		return INCENDIARY.contains(gun.name());
	}

	/** JE a round of this energy weapon draws from Energy Cells (slice 8D, {@link #CHARGE}); 0 for a gun that loads rounds. */
	public static int charge(GunItem gun) {
		return CHARGE.getOrDefault(gun.name(), 0);
	}
}

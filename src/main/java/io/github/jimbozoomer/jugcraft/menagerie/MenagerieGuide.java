package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.guide.encyclopedia.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class MenagerieGuide {
    private MenagerieGuide(){}
    public static void write(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Wildlife, companions and mounts"));
        out.fact(Component.literal("Feed adults to breed. Small companions and mounts can bond with you when fed. Empty-hand crouch interaction toggles stay/follow; saddle an adult mount and interact to ride. Use a net for small critters and water buckets for aquatic animals. Whistles call your nearby companions without loading distant chunks."));
        out.fact(Component.literal("Flying and swimming mounts follow your movement and look direction. Jump climbs; crouch dismounts. Captured animals keep their variant, age, health, name and owner. Other players cannot capture your companions."));
        out.heading(Component.literal("Pocket pets"));
        out.fact(Component.literal("Carry passive pets in your hotbar. Active abilities use one food and a cooldown; each distinct passive pet uses one food per minute. Favorite foods appear on the item tooltip. Crafting, enchanting, anvil, Ender Chest and storage pets use existing inventories. Bound return-point pets only teleport to loaded safe locations. Pets do not destroy buildings with explosions."));
        out.heading(Component.literal("Seasonal workshops"));
        out.fact(Component.literal("Spin yarn at the Herbalist Spinning Wheel, weave cloth, and craft witch or raven-black clothing. Mature Pumpkling vines wake a companion when used. Veil Ghosts work with the spirit lantern, spirit board and ectoplasm systems. These additions remain usable all year."));
        for(var s:Menagerie.SPECIES.values())if(s.id().startsWith("wildlife_")||s.id().startsWith("mount_"))species(out,s);
    }
    public static void woodland(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Woodland companions"));
        out.fact(Component.literal("Lilytads and Minipads grow flowers: shear them and let them regrow for five minutes. Minipad flowers glow. Craft Mending Tomes from the flowers; using one spends a glowstone dust and heals you, with a ten-second cooldown. Flower crowns are wearable; the glowing crown grants night vision. The Sporeling Backpack opens storage when used in hand and can be worn in the chest slot."));
        out.fact(Component.literal("Tame a Cindershell with coal, then give the adult a Cinder Furnace. Offer a smeltable item while carrying coal to smelt it; each operation costs one coal. Cinder Furnaces also work as placed furnaces with the normal menu. Baby Cindershells travel in lava buckets. Woodland nets transport small companions. End Whales can be tamed with chorus fruit, saddled and ridden in flight."));
        out.heading(Component.literal("Fable encounters"));
        out.fact(Component.literal("Fable wildlife lives in its matching habitat. Hounds, Scarecrows, Corpse Eaters and Frogmen are hostile and appear in dark places. A Fable Straw Hat wards Scarecrows and prevents their natural spawning within 32 blocks. Hounds and Mojos briefly root their target; Scarecrows cause a short stumble. Jinx retaliate with poison darts. Tame Fable Butterflies give nearby keepers occasional regeneration. The Hound Trap slows wild Hounds; use it to re-arm or disarm it. Fertile Slimeballs fertilize a 3×3 crop area. The Blowgun fires your arrows as poison darts. Wisps leave collectible purses with amethyst, glowstone or an Ender Pearl."));
        out.fact(Component.literal("Doom Embers place a temporary, non-spreading flame that hurts hostile creatures. It fades after ten seconds and does not harm players or friendly animals. New equipment can be used as soon as you acquire it."));
        out.heading(Component.literal("Companion care"));
        out.fact(Component.literal("Ferrets, red pandas, otters and small companions bond through their favorite foods. Feed two adults to breed. Woodland nets store small companions; water buckets transport koi, sea bunnies and dumbo octopuses. Otters find clams, weevils find acorns, and jumping spiders make silk. Open clams for a chance of a pearl. Bottle adult snail or sea-bunny slime once every five minutes; the drinks cure poison and grant regeneration or water breathing. Pearl necklaces work from your offhand underwater. Tier two adds swimming speed; tier three adds resistance. Silk grapples reuse the standard hook and spend durability. Dragonflies accept saddles and their own armor; Roly Polies carry a chest, opened with a stick while crouching."));
        for(var s:Menagerie.SPECIES.values())if(s.id().startsWith("companion_")||s.id().startsWith("grove_")||s.id().startsWith("fable_"))species(out,s);
    }
    public static void household(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Furnish a working home"));
        out.fact(Component.literal("Use the furniture workbench to choose a design. Its ingredient display and output preview use your actual inventory; it takes materials only when the whole output fits. Matching counters and sofas connect side by side. A wrench rotates furniture, including both halves of a bath when there is room."));
        out.heading(Component.literal("Kitchens and storage"));
        out.fact(Component.literal("Start with the furniture workbench (iron above planks). Its freezer designs use 9 iron ingots, 3 copper ingots, 4 redstone and packed ice; the dark finish also takes black dye. Ice and snow fuel freezing after construction. Paper around string makes a storage package. Cook turkey legs and venison in a furnace, smoker or campfire; cut cooked cod on a cutting board for a fish fillet."));
        out.fact(Component.literal("Cabinets and shelves store items. Cooking appliances use fuel and normal recipes; cold appliances use ice or snow. Hopper access uses the shared inventory rules. A spatula removes the cooked result. Sinks and baths take water buckets and return empty buckets; take water back with an empty bucket. Sitting uses the existing seat controls."));
        out.heading(Component.literal("Small details"));
        out.fact(Component.literal("Use a named name tag on a door mat to put a label on it. Craft a named mat with blank mats to copy only its label. Fans animate while powered. Switches and buttons provide redstone; the remote toggles a nearby television. Furniture is usable immediately, without research."));
    }
    public static void habitats(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Ecology and new landscapes"));
        out.fact(Component.literal("New woods include their planks, fences, doors, signs, boats and growable saplings. Fruit ripens on supported plants. Put a spile on sappy maple, add a bucket and let sap collect; the supplied recipes turn it into syrup. New biomes and ruins generate in newly explored terrain. Existing chunks and your buildings are not regenerated."));
        out.heading(Component.literal("Goblin ruins and relics"));
        out.fact(Component.literal("Explore goblin camps and the ruined keep, or hunt cave goblins and ogres for eyes and tusks. Craft a Goblin Crown, then crouch-use it on an urn with eight Soul Ash to summon the king in an open, permitted space. Smelt Soul Soil into Soul Ash. The Amethyst Staff uses crafted soul bullets. Activate a ring with an Amethyst Shard: Health heals six, Glory grants brief strength, Stealth grants brief invisibility, and Experience grants four experience points. Rings have a one-minute cooldown."));
        out.heading(Component.literal("Mutant encounters"));
        out.fact(Component.literal("Use Chemical X on a zombie, skeleton, creeper, Enderman, snow golem, pig or spider in an open area. Crouch while converting a creeper to make a tame minion. The snow golem and Spider Pig also bond with their creator. Hostile mutants telegraph special attacks. A zombie can rise once unless finished with fire. Mutant skeleton pieces craft bone armor: two pieces boost jumping; the full set adds resistance."));
        out.fact(Component.literal("The Hulk Hammer makes a close shockwave. Creeper Shards spend gunpowder for a bounded blast without terrain damage. The Endersoul Hand spends an Ender Pearl to teleport to the loaded, safe location you aim at. The minion tracker recalls your nearby minions. No separate research is required."));
        for(var s:Menagerie.SPECIES.values())if(s.id().startsWith("ecology_")||s.id().startsWith("fauna_")||s.id().startsWith("mutant_"))species(out,s);
    }
    public static void beasts(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Material routes"));
        out.fact(Component.literal("Animal products stay animal products: hammerhead and frilled sharks can drop their teeth, cachalots a tooth, giant squids a tentacle, snapping turtles a scute, and crimson mosquitoes a blood sac. Bottle an owned Komodo dragon, then use the cutting board to dry the spit into the existing gear reagent and recover the bottle. A Capsid takes amethyst fuel and can turn a normal music disc into Daze."));
        out.fact(Component.literal("Purpur blocks become End carpentry planks. Build doors, trapdoors, stairs and slabs from them; the wall also takes sticks so its recipe differs from the trapdoor. Iron, chains and planks make the anchor and winch. The ship wheel uses sticks around iron. These are the existing boat controls, not a block-built flying ship system."));
        out.heading(Component.literal("Creature care and equipment"));
        out.fact(Component.literal("Feed peaceful wildlife to bond. Bison can be sheared; owned skunks and Komodo dragons fill bottles. Mooses, emus and other companions periodically provide their own harvests. Crows with a chest gather nearby loose items; crouch-use a stick to open cargo. Use a falconry glove to direct your eagle toward a target or recall it, and its hood to toggle staying."));
        out.fact(Component.literal("Use an egg, ant pupa or cockroach egg case on open ground to hatch a companion, within the local population limit. A potted Flutter releases a bonded Flutter and returns its pot. A nectar feeder takes water and sugar and heals visiting hummingbirds. Feed leaves to an ant chamber, then use it empty-handed to collect fungus."));
        out.heading(Component.literal("Tools and travel"));
        out.fact(Component.literal("Straddleboards ride over water and lava; jump hops and crouch dismounts. Straddle saddles equip owned mounts; pigshoes boost a ridden mount and straddle helmets armor it. Dimensional Carvers spend an Ender Pearl to cross a short, loaded safe gap. Echolocators, Endolocators and pupfish locators report the nearest matching creature in loaded terrain. Blood, hemolymph and stink weapons consume their matching ammunition; pocket sand consumes sand."));
        out.fact(Component.literal("Put Bison Fur beside armor in crafting to insulate it. Mimicream repairs a damaged item without copying it. The capsid processes its supplied conversion recipes. The transmutation table opens the shared essence system. Power magnets to move loose items. A winch spends inserted chains to extend downward and retracts its own chains when power stops. The ship wheel is a dock control: while powered it points the owner's occupied boat along the wheel's facing within 12 blocks."));
        out.heading(Component.literal("Summoned dangers"));
        out.fact(Component.literal("Use a Mysterious Worm or Farseer Arm in the End for its encounter. Fish Oil used in water calls a Sea Bear. Only one matching summoned encounter is allowed within 96 blocks. These are native Jugcraft encounters; their area attacks do not erase your builds."));
        out.heading(Component.literal("Species directory"));
        for(var s:Menagerie.SPECIES.values())if(s.id().startsWith("beast_"))compactSpecies(out,s);
    }
    public static void caverns(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Find the six cave ecosystems"));
        out.fact(Component.literal("Craft the Spelunkery Table from a book, two flints and five planks. The submarine kit uses five steel plates, two copper blocks, glass and an electric motor, connecting cave exploration to the factory chain. Gingerbread armor coats the matching leather armor with gingerbread crumbs. Boss rewards and treasure remain encounter rewards."));
        out.fact(Component.literal("Use a Spelunkery Table and choose a cave on its map. A cave tablet and paper make the discovered map. Surveys sample a bounded region without loading chunks. Magnetic, Primordial, Toxic, Abyssal, Forlorn and Candy caves bring their own materials, wildlife and landmarks. The Beholder lists nearby creatures and can highlight permitted targets."));
        out.heading(Component.literal("Cave machinery"));
        out.fact(Component.literal("Build a 2×2×2 Nuclear Furnace from components. The lowest northwest component controls it. Put a blastable item in input, a uranium rod in fuel and an empty metal barrel in the barrel slot. One rod powers 64 crafts and produces one sealed waste drum. Outputs must have room before anything is consumed. A single legacy Nuclear Furnace block instead uses ordinary blast-furnace fuel and recipes."));
        out.fact(Component.literal("Place a quarry facing away from its excavation. Magnetic lights mark four corners: four blocks left/right, one and nine blocks forward. Insert a Quarry Smasher and redstone. It mines the inner 7×7 area, up to 64 blocks down, stopping at protected or unloaded terrain. It leaves containers, fluids and unbreakable blocks intact. Hoppers supply the top and sides and extract output below."));
        out.fact(Component.literal("A powered Confection Oven consumes gingerbread crumbs and sugar to make an owned Gingerbread Man, capped at eight nearby. Give it a chest and it collects nearby items, then deposits them into an authorized gingerbarrel. A Holocoder records a creature for a Hologram Projector. A Biome Treat records the biome where it is used; a Conversion Crucible spends the treat and a diamond to recolor a loaded 12×12×12 biome region, without regenerating terrain."));
        out.heading(Component.literal("Gear, controls and safety"));
        out.fact(Component.literal("The submarine needs water and redstone fuel. Look and move to steer; jump rises, crouch exits. Diving gear helps underwater. Full hazmat protects against local acid and radiation. Darkness gear works in darkness. Spears are thrown and recoverable; the Raygun spends uranium shards, and other weapons list their ammo in their recipes and guide controls. The Sating Sack only eats carried solid food, never drinks or dishes with a returned container."));
        out.fact(Component.literal("Bind a Game Controller to your creature to direct it; crouch-use toggles staying. A Totem of Possession can bond a weakened wild creature with at most 40 health for an Occult Gem. Link a detonator to one Nuclear Bomb: use to arm its ten-second fuse, crouch-use to disarm, within 256 loaded blocks. Charges pause when their owner is absent. The bounded blast respects claims and preserves containers."));
        out.heading(Component.literal("Cave species"));
        for(var s:Menagerie.SPECIES.values())if(s.id().startsWith("cavern_"))compactSpecies(out,s);
    }
    private static List<net.minecraft.world.item.Item> egg(Menagerie.Species s){
        var item=Menagerie.ITEMS.get(s.id()+"_spawn_egg");if(item==null){String prefix=s.id().startsWith("beast_")?"beast_":"cavern_";item=Menagerie.ITEMS.get(prefix+"spawn_egg_"+s.source());}
        return item==null?List.of():List.of(item);
    }
    private static void compactSpecies(PageWriter out,Menagerie.Species s){out.fact(Component.literal(s.name()+" — "+s.habitat().replace('_',' ')+" · "+s.movement()+" · "+s.mood()+" · Food: "+new ItemStack(Menagerie.feed(s)).getHoverName().getString()),null,egg(s));}
    private static void species(PageWriter out,Menagerie.Species s){
            out.heading(Component.literal(s.name()));
            out.fact(Component.literal(s.habitat().replace('_',' ')+" · "+s.movement()+" · "+s.mood()+" · Food: "+new ItemStack(Menagerie.feed(s)).getHoverName().getString()+" · "+s.variants().size()+" appearances"),null,egg(s));
    }
}

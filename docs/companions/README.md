# Peepo Companion
Fabric mod for Minecraft 26.3, Java 25, Fabric Loader 0.19.3+ and Fabric API.

A 0.6-block-tall, peaceful bipedal frog with a wide smile, raised eyes, blue T-shirt, green hips, and thin green limbs. Wanders gently, looks at players, floats in water, never attacks and does not despawn. Player damage is ignored. Right-click with an empty hand for hearts and pink cheeks for four seconds. Another empty-hand click refreshes the blush.

Find **Peepo Summoner** in Creative > Spawn Eggs, or use `/give @s peepo_companion:peepo_summoner`. Use it on solid ground. Also supports `/summon peepo_companion:peepo`. Natural spawning is described in SPAWNING.md. No survival summoner recipe.

Build: `./peepo-npc/build-local.ps1 -Tasks build` from the parent workspace. Install the built jar from `build/libs` into your Fabric instance's mods folder with Fabric API.

Model: a plain head-width blue torso, simple thin green limbs, a lightly shaped frog head, centered pupils and a closed smile. Regenerate with tools/generate_model.py using Python with Pillow.

Pumpkin costume: right-click with a Jack o'Lantern to equip. Uses one in Survival, none in Creative; additional clicks while equipped do not consume another. The costume is saved with Peepo. His shirt and sleeves are hidden, his arms and legs remain green, and a pumpkin lid sits on his head. The carved face is emissive (visible in darkness); it does not place light blocks. Empty-hand blush still works.

Proportions: both models are 60% of their previous size. Heads, torsos and pumpkin shells/hats are 50% deeper before the uniform scaling; limb proportions are unchanged.

Jughead: a separate variant with pink skin, a bare torso, blue shorts, and a translucent inverted water jug over a pink head bump. Find Jughead Summoner in Creative > Spawn Eggs, or use /summon peepo_companion:jughead. The frog retains the current 60% proportions; the jug adds height. Editable model: model/Jughead.bbmodel with an embedded RGBA texture.

Feeding: injured Peepo and Jughead accept one edible item on right-click, hold it with both hands, and eat for two seconds with matching food particles and sounds. A finished meal heals by the food's nutrition value (at least one health point), capped at maximum health. Healthy companions at or above 95% energy, and already-eating companions, accept no more food. Injured companions or those below 95% energy seek visible dropped edible items within eight blocks, respect pickup delay, collect one at a time, and leave extras once healthy and at or above 95% energy. Bowl/bottle remainders drop after eating; an interrupted saved meal resumes after loading.


Energy: 128,000 JE per companion, food-enhanced regeneration, and future chair/bed/wheel hooks. Sneak-right-click with an empty hand to inspect energy. See ENERGY.md for rates and integration details. Food is also accepted below 95% energy, even at full health.


Wheel running animation: all three editable Blockbench models include `animation.companion.wheel_run` (0.6-second looping stride). Open Animate, select the clip and press Play. Models use Blockbench's generic format to expose the animation workspace. Geometry, textures and variant visibility are preserved.

The Minecraft renderer uses the same stride, opposing arm swing, head nod and two small bounces per cycle. The future occupied wheel must call `setWheelRunning(true)` every server tick; call false when it stops. The signal synchronizes, prevents wandering/idle recovery, and expires after five ticks without refresh. Eating, resting or an empty reserve prevents running. This is animation support; the wheel block and NPC mounting are not connected yet. The signal itself does not spend energy; the wheel's transactional extraction does that.

Automatic routine: exhaustion triggers a break until 80% energy; healthy NPCs let food buffs digest before collecting another meal. Loaded-station discovery and capacity-aware wheel work are ready for CompanionStation block implementations. See BEHAVIOR.md for current behavior, pending furniture integration and proposed jobs.

# Companion beds

Sixteen colored beds use Minecraft 26.3's vanilla bed texture references, including the pillow, blanket and wooden legs. Each is 1 block long, 0.75 blocks wide and 0.375 blocks tall. Textures follow resource packs that replace the vanilla bed textures.

Craft with two matching wool above two planks (any kind), or recolor an existing companion bed with dye. Items are in Functional Blocks; for example `/give @s peepo_companion:red_companion_bed`.

Place on solid ground. Place another bed directly on top to make a bunk. Colors can be mixed, facing is inherited, and the lower bunk gains posts and ladder rungs. Repeat to extend the stack. Removing support breaks the unsupported beds above with their normal drops.

Leave solid ground and an unobstructed vertical column at the foot/ladder end. Every bunk searches from the same ground entrance. Companions walk to that entrance and mount their reserved bunk directly; there is no separate climbing animation. Each bunk accepts one companion. Claims expire if abandoned. On wake/removal they return to the ground entrance, or a nearby clear position. Their exit is saved to recover from reloads in upper bunks.

The existing energy routine uses beds at night (Overworld ticks 13000–22999) when resting is needed. Wheel work remains the priority until exhaustion; a sleeping companion stays in bed until 95% energy or dawn. Sleep restores 16 JE/t plus its active food bonus. Direct feeding interrupts sleep. Closed eyelids and a lying pose apply to Peepo, Jughead and pumpkin Peepo.

Manual checks: place different colors; stack three or more bunks; leave the ladder clear; drain companions' energy with a wheel; set night; observe separate occupants, lying poses and closed eyes; check sunrise, feeding, breaking a lower bed, and saving/reloading. A fresh companion starts at full energy and does not need rest immediately.

Built with assemble only. No automated tests or in-game verification were run for this change.

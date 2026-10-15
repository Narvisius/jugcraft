# Peepo and Jughead player guide

Current controls and behavior on the `peepo-companion` branch, updated 9 October 2026. Peepo and Jughead share the same commands and jobs. Older feature records describe how individual features developed; use this guide for the current setup.

## Quick start

1. Find a companion and right-click it with edible food once to tame it. Peepo's main habitats are swamps, lush swamps and bayous; Jughead's are beaches, dune beaches and overgrown beaches. Natural spawns are in the Overworld.
2. Shift-right-click the companion without the Planner to open its inventory and commands. It pauses while you edit.
3. Craft a **Companion Planner**: an iron nugget above a row of three paper, with a stick below the paper. Right-click your companion with it to select that companion.
4. Right-click a bed to set its home, then right-click a supported workstation to assign work. Assigning work selects **Work** mode.
5. Right-click a nearby ordinary chest/container to make it a **Supply**. Put the job's real ingredients and tools inside.
6. Assign another container, then right-click that same container again to change it from Supply to **Output**.
7. Open the companion's **Supplies** page to check its containers and job links. For a job that needs a tool, either put it in **Hand** or mark its Supply container **Tools**.
8. Keep walkways clear, give the companion food, and leave space in Output. Hover the job row to see what is stopping it if work does not begin.

## Inventory and commands

The companion has **eight cargo slots**, arranged as two rows of four, plus separate **Costume** and **Hand** slots. Cargo carries supplies, harvests, products and meals. Tools and the costume do not consume cargo slots.

| Command | What it does |
| --- | --- |
| Follow | Follows the player who issued the command. |
| Stay | Stays around the commanded location instead of working. |
| Home | Returns toward its home area and can rest there. |
| Work | Runs assigned jobs and enabled porter routes, taking breaks and eating as needed. |

The owner can switch **Owner only / Party: Allowed** to permit their party to manage the companion. Container locks and protected areas still apply. Other players cannot gain control just by selecting it with a Planner.

Hoes, knives and shears are drawn only for their matching work animation. They remain stored in **Hand** while stowed, even with full cargo. Other work tools are also put away when unused. **Hand: Locked** prevents automatic tool replacement; it does not force a tool to remain visibly held. Torches and tiki torches remain held during normal travel, and ordinary held items are put away for sleep, sitting, social gestures and unrelated work.

## Planner assignments

Each companion can have **one home, four work assignments, one lunch source, four Supplies and four Outputs**. Four porter routes are configured separately and do not consume work slots.

| Planner action | Result |
| --- | --- |
| Right-click a tamed companion | Selects it. The Planner name shows the selection. |
| Right-click a companion bed/bunk or vanilla bed | Sets or replaces its one home. |
| Right-click a supported workstation | Adds a work assignment, up to four. |
| Shift-right-click Planner → Farmland / Orchard | Select two opposite ground corners to assign a zone. |
| Right-click a sheep, cow or chicken | Assigns a fixed shearing, milking or egg-collection area at the ground beneath it. |
| Right-click a lunch crate or lunch cover | Assigns its separate lunch source. |
| Right-click a new ordinary container | Adds the next Supply, or an Output once all four Supplies are assigned. |
| Right-click an assigned ordinary container | Switches that container between Supply and Output if the new role has space. |
| Left-click an assigned block | Removes the assignment without mining the block. For animal jobs, click the marked ground anchor. |
| Shift-right-click with the Planner | Opens its mode menu: normal assignments, Livestock Management corners, cancel corners, or clear the selected companion. |
| Right-click two ground blocks in Livestock Management mode | Creates one combined Livestock Management job covering the rectangle, up to 16 by 16 blocks including both corners. |

The selected companion, home, jobs and lunch source have **green** frames; Supply is **blue**, Output **yellow**, and missing/replaced targets **orange**. Garden frames show the saved soil cells. Livestock Management zones show their exact rectangular bounds. The first corner is yellow while selecting; the preview turns red if the second corner exceeds the limit. Older animal assignments retain their anchor and 8-block-radius bounding square.

Use the Jobs page's **up/down arrows** to order work: the top job has highest priority. Unavailable jobs are skipped. The **x** buttons can remove assignments even when their blocks are missing or in another dimension.

Use the **pencil icon** beside the companion's name at the top of the menu, or the pencil to the left of a job row, to edit that name. Hover for the Rename tooltip. Enter a name and press **Save** (or Enter); leave it blank to restore the default. **< Jobs** returns to assignments. Assigned job rows show the job name and location without a Work 1/2/3/4 prefix. Empty rows keep their numbered placeholders. Job names follow the assignment when its priority changes. Livestock Management jobs have a **gear icon** on the right (hover: **Zone options**), in the same position as other jobs' Filter button. This opens independent **Shearing**, **Milking**, **Eggs**, **Breeding** and **Slaughter** On/Off buttons, plus **Population...** settings.

New assignments must be within 64 blocks of the companion. Planner editing requires the companion to be loaded, in your dimension and within 128 blocks of you, and you still need to reach the block or animal you click. Companions do not load chunks or teleport supplies between dimensions. Leave reachable standing space beside machines; elevated stations need an accessible approach. Jughead also needs clearance for his jug.

## Supplies, tools and delivery routes

Open **Supplies** to access these three views:

| View | Setup |
| --- | --- |
| Containers | Review/remove up to four Supplies and four Outputs. Enable **Tools** on Supplies containing knives, hoes or shears. Use **Hand: Auto tools / Hand: Locked** to control automatic swapping. |
| Job links | Choose which numbered Supplies and Outputs each work assignment may use. **All** includes all current and future containers; numbered buttons select a subset. |
| Porter routes | Configure up to four direct Supply-to-Output routes. Choose both endpoints, optionally set a Filter, then turn the route **On**. Changing an endpoint turns that route Off until you enable it again. |

Containers keep their numbers when another is removed. A container can have only one role for a given companion. The face clicked when assigning a container matters for sided inventories. A chest can serve one companion's Output and another's Supply to form a production line.

Each job has a status line beneath its controls. Hover it for the full explanation, including linked Supply/Output numbers, missing tools, Hand lock, energy recovery and blocked approaches. Selected cooking-pot and ordinary-machine recipes can show quantities still missing at the station; these do not imply the linked chests are empty. Delivery messages show real carried items and their source/destination. The companion stays paused while you edit. See [job status details](features/companion-job-details.md).

Attach a sign to a chest/container to give it a readable name in **Supplies**. Nonempty sign lines are joined into its display name, including a sign on either half of a double chest. The sign takes priority over a custom container name; removing or blanking it restores the container name. Labels refresh about once a second while the companion menu is open, without changing the actual assignment.

Tools travel in real cargo. When an automatic tool swap completes, the previous hand item moves into cargo. Leave room for fetching a replacement; an accessible Tools supply can replace a broken tool. Locking Hand is useful when you want to keep a torch or a particular tool assigned.

Porter routes run in **Work** mode, alongside station assignments. There is no separate Porter command. Ready workstation jobs take precedence over starting a generic porter route; a delivery already underway keeps its physical cargo.

A route's **Filter** allows up to nine item types. An empty route filter accepts any transferable item. **Leave** reserves that many of each item type in the source; **Keep** sets a destination stock target per item type. Both cycle through 0, 16, 32, 64, 128, 256 and 512. Leave 0 reserves nothing; Keep 0 means unlimited.

Deliveries can use all available cargo slots and normal stack sizes, including different ingredients in one trip. A raw pie and its fuel can travel together. Supplies split between chests still require physical visits to those chests. Bigger deliveries use more movement energy. Full or unavailable destinations retain the real cargo for retry; removing a link does not erase carried items.

### Supply and Output controls on each job

Each job has separate **Supply** and **Output** buttons cycling **Auto → On → Off**:

- **Auto:** allows companion transport unless supported machine automation is detected for that direction.
- **On:** explicitly requests companion transport even when external automation is detected.
- **Off:** disables companion transport in that direction. Machine speed assistance is independent.

Unsupported directions remain unavailable. Use On if you want Peepo to back up connected automation. These controls do not provide power, process fluids or ingredients the workstation adapter does not support.

## Workstation Filters

On a recipe-capable job row, click the **funnel icon** (hover label **Filter**) to open a **3×3 grid of nine ghost slots** in the companion GUI.

- Click a slot with a finished output item on your cursor to allow that output. Your real item is not consumed.
- Right-click a slot, or click it with an empty cursor, to clear it.
- An **empty grid allows any available recipe** supported by that workstation's companion integration.
- The grid is an allow-list, not a crafting pattern or priority order. Put real ingredients in linked Supply containers.
- Workstation filters belong to the station and are shared by companions assigned to it. Some filter changes reset processing progress, but do not discard stored ingredients.

Filters are available for Cooking Pots, supported standard processors, Hearth Ovens, Cutting Boards, Skillets, Kitchen Stoves, Cider Barrels, Candy Kettles, vanilla cooking/brewing stations, Beehives/Bee Nests and both Crafters. The Electroplating Bath accepts the tool/armor type as its output filter. Hearth Ovens accept whole raw or baked pies/cakes as filter icons and display the baked result. Cider Presses and Canning Kettles use their fixed workflows without this selector. Gardens have a separate single crop/seed selector.

## Supported jobs

| Assignment | What to provide and expect |
| --- | --- |
| Generator Wheel | One companion runs the wheel using its energy reserve. It stops when the buffer is full or it needs rest. Electrical output is from the two sides. The GUI shows stored power, companion energy and its name. |
| Hand Crank | One companion turns it and pauses when a connected flywheel is full. |
| Standard processing machines | Peepo assists production. Full multiblocks generally support two helpers at **+25% speed each**; compact/single-block machines generally support one at **+50%**. Supported item recipes can fetch ingredients and collect outputs. Normal power, fluid, heat and machine-formation requirements remain. Electrical generators and storage are not processor assistance jobs. |
| Electrolytic Separator | Assign the same machine to up to two companions, **+25% speed each**. Each spends **16 reserve JE per productive tick**. Extra processing pays normal machine electricity; supply water/brine and remove gases/lye with pipes. Uses the valve and wrench clips. Item Supply/Output and Filter are not applicable. |
| Furnace / Smoker / Blast Furnace | Supply valid cooking inputs and ordinary furnace fuel. Output collects results and spent fuel containers; normal cooking speed is unchanged. |
| Brewing Stand | Choose final potion(s) in Filter. Supply water bottles or other base potions, all intermediate reagents and brewing fuel. Peepo plans up to eight brewing steps, keeps intermediate potions in the stand, and delivers the finished batch to Output. Empty Filter performs any available single step. |
| Beehive / Bee Nest | Choose Honey or Honeycomb in Filter. A full hive and campfire smoke are required. Honey uses empty glass bottles from Supply; Honeycomb uses equipped shears or a Tools chest. Results go to Output. |
| Bat House | Collects accumulated guano into normal cargo and delivers it to linked Output. No tools or Supply needed; bats and production timing stay unchanged. |
| Campfire / Soul Campfire | Light it first. Supply brings up to four compatible cooking inputs and Output collects saved cooked results. Keep safe space beside the fire. |
| Composter | Supply compostable items; Output takes ready bone meal. Normal compost yields and maturation apply. Use a dedicated supply to avoid composting wanted crops. |
| Vanilla Crafter | Manually seed its complete grid, then assign and supply matching ingredients. Peepo remembers the pattern, refills it and collects results/remainders. A normal redstone clock is still required. |
| Coal Generator / Steam Generator / Large Steam Engine | Supply the machine's accepted solid fuels. Boilers can also receive water buckets; Output returns empty buckets. Pipes/source-water operation still works. No production boost. |
| Auto-Crafter | Seed the real 3×3 grid with one item in each required slot. Supply replenishes that pattern; Output collects products and container remainders. Normal energy/redstone rules apply. |
| Electroplating Bath | Supply a compatible damageable item and nickel, silver or gold ingots; pipe in sulfuric acid. Output takes the actual plated/repaired item, preserving its other components. |
| Cooking Pot | Provide heat and the recipe's ingredients/containers. Peepo brings balanced bulk ingredient loads, then stirs at **+50% cooking speed** until ingredients run out or the pot's result slots fill, before collecting and delivering the finished batch. Supply and Output use the existing links and controls. |
| Cutting Board | Provide a knife and valid cutting inputs. Peepo stands at the board and makes a two-handed chop. The knife loses durability; all recipe results are retained for Output. |
| Skillet | Heat it from below. Supply brings compatible raw food and Output collects fried food. Cooking continues without a resident helper; no speed bonus is added. |
| Kitchen Stove | Light it and keep its top clear for its own cooking. Supply brings cookable food and Output collects cooked food. A pot/skillet on top uses the stove as heat instead; assign that upper station for its workflow. |
| Cider Press | Supply valid press ingredients and glass bottles. Peepo loads, sits on the rotating handle with dangling legs while operating it, then bottles and collects products through the press's normal workflow. |
| Candy Kettle | Supply sugar, a water bottle or milk bucket, and an empty Candy Tray. The Filter selects candy types. Peepo cooks, pours, finishes the tray and delivers candy/empty containers; the tray is reused. |
| Cider Barrel | Supply Sweet Cider. Open its **Filter** and choose **Sparkling** or **Aged**; Peepo reuses the emptied bottles and delivers ready cider to Output. |
| Canning Kettle | Provide active heat, a water bucket when dry, and **full, fresh, unsealed preserve jars**. It holds four jars and processes them together; Output takes sealed jars, rejected spoiled jars and the returned empty water bucket. Empty Mason Jars are not ingredients. |
| Hearth Oven | Supply **prepared raw pies/cakes** and suitable fuel such as hearth logs, charcoal, coal or coke. One bake fits at a time. Peepo loads it, waits nearby, collects it when baked and carries it to Output. It does not craft raw bakes from dough, batter or fruit. Ordinary heat/burning rules remain. |
| Farmland zone | Harvest mature supported crops, replant, and plant empty farmland/planter boxes inside the selected corners with available seeds. A hoe improves speed and energy efficiency. See garden setup below. |
| Sheep area | Give shears in Hand or through a Tools supply. Peepo shears eligible adult woolly sheep within **8 blocks** of the anchor. Output receives wool. |
| Cow area | Give empty buckets in cargo or Supply. Peepo milks eligible adult cows within **8 blocks** of the anchor. Output receives milk buckets. Companion milking has a **60-second cooldown per cow**. |
| Chicken area | Peepo picks up dropped eggs within **8 blocks** of the anchor and delivers them to linked Output. No tools or Supply are needed. White, brown and blue eggs are supported. |

The [processor list and effort table](features/companion-jobs.md#processor-assistance-and-helper-teams) provides machine-specific support and reserve costs. Special-machine item logistics include recipe catalysts and item byproducts, including recipes whose other results are fluids. All fluids still use the ordinary pipes/tanks; Peepo does not replace power, fluid, formation or redstone requirements.

### Industrial machine assistance

The Electrolytic Separator is the first supported machine using the new industrial framework. Select a tamed companion with the Planner and right-click any Separator part; repeat with a second companion for the full **+50% speed**. Keep two reachable standing spaces around its base. Helpers prefer the front, with side/rear alternatives when pipes or other obstacles block it. Each companion still uses one work assignment and its normal priorities, meals and rest settings.

One helper costs **16 reserve JE/t (320 JE/s)**; two cost 32 JE/t combined. These are gross work costs before any applicable regeneration. The Separator's unmodified electrical draw is 256 JE per processing step: assistance raises average demand to roughly **320 JE/t with one helper or 384 JE/t with two**, without reducing a batch's total electrical cost. Upgrades retain their native effects. Missing power for extra work limits assistance; full outputs, missing feed, pause and incomplete structures stop useful work. The Jobs display reports the blocking condition.

There is no item hauling for this fluid-only machine. Its gases and lye still leave through native fluid ports. The new shared helper support is opt-in for future processing forms; the Gas Burning Generator is not enabled. See the [industrial companion record](features/companion-industrial-machines.md).

During Supply/Output trips, companions carry a visible example of their real cargo in both hands and reach to pick up or put down items. Ground planting and hand picking have their own motions; water collection holds the container in one hand and bends over to scoop, and Sprinkler filling tips the water bucket. Skillet and Kitchen Stove loading includes a brief spatula motion. These animations add no work delays, extra trips or energy costs. The existing pie/cake, hoe and overhead harvesting animations remain in use. See [task animations](features/companion-task-animations.md) for details.

### Vanilla stations and special-machine supplies

Assign each station with the Planner and link existing Supply/Output containers. Furnace-family and campfire filters use the cooked output; brewing filters use the actual finished potion, including its potion type. An empty filter chooses a compatible available recipe. Brewing now plans chains to the selected final potion, with up to eight steps. For example, select Swiftness and supply water bottles, nether wart, sugar and blaze powder; Peepo brews the awkward stage first and keeps those bottles in the stand until they become Swiftness. Supplying awkward potions lets him start later in that chain. Strong/extended and splash/lingering variants work through their registered recipes and matching final potion ghosts. He checks for all recipe reagents and starting fuel before choosing a batch of one to three bottles. Ingredients remain physical supplies, so another worker taking them can pause an active batch until restocked. Empty Filter chooses any available single step. Finished potion bottles and ingredient/fuel containers go to Output. Changing Filter while bottles are loaded finishes the existing plan before starting the new selection. Chains and their current stage survive reloads. A removed recipe or incompatible manually inserted bottle pauses automation for manual correction; no potion is discarded. Supply filled water bottles, or use a separate Water collection assignment to refill glass bottles and deliver them to the brewing Supply chest.

For the vanilla **Crafter**, put the complete desired recipe into its real grid before Peepo starts, and disable unused slots as usual. Peepo saves that layout, replenishes its exact items, and waits for all required slots before allowing a managed craft. Keep using redstone pulses. To change its pattern, replace the grid with a complete new recipe. The **Auto-Crafter** keeps its original last-item-as-pattern behavior; Peepo stocks each occupied grid slot to two items, and both product and remainder slots can be unloaded. Its existing restriction against a recipe producing multiple different container-remainder types remains.

Once Peepo has supplied a campfire or adopted a Crafter pattern, finished results enter a saved twelve-slot collection buffer instead of dropping on the ground. Peepo delivers those results through Output. **Shift-right-click with an empty hand** to recover buffered results yourself; inventory overflow stays safely stored. Breaking the station drops the buffer. Managed Crafters pause when the entire result/remainder batch cannot fit; companion campfire loading waits until stored results are collected. Ordinary unmanaged stations retain their normal behavior. Auto/On/Off still controls companion hauling; connected-automation detection remains specific to supported Jugcraft machines, not vanilla stations.

Solid-fuel generator jobs restock up to four accepted fuel items. Coal and Steam Generators stop requesting more supplies when their energy buffer is full. The Large Steam Engine retains its native kinetic demand behavior. Steam boilers accept one water bucket when tank space and empty-bucket storage allow it; otherwise continue supplying water by their existing means. Other generators are outside this integration.

For **Electroplating**, use a dedicated Supply to choose the coating metal when several are available. A ghost tool/armor item filters which item type can be processed; the real item's enchantments, name and other components are preserved. Peepo skips already fully repaired items with the same plating and will not pair an existing coating with an incompatible metal. Supply delivers one item and ingot per operation, while sulfuric acid stays on pipes.

For fluid processors, Supply can replenish recipe item inputs even when all outputs are fluid (including catalysts); Output collects all exposed item result/byproduct slots even when a recipe has no item inputs. The current fluid contents determine compatible recipes. Recipes with only fluid outputs have no item icon to filter; leave the Filter empty for these. Existing processing speed bonuses and machine-based work effort stay unchanged. Vanilla logistics and generator stocking add no continuous work-energy charge: only the existing load-based delivery cost applies.

### Candy Kettle

Assign a **Candy Kettle** with the Planner and connect its **Supply** and **Output**. Put it above a working heat source (a lit Kitchen Stove or campfire, for example) and leave safe standing space beside it. Peepo can reach from the floor one block below a raised kettle. The usual two-arm working animation is used.

Open the job's **Filter** and add any of the kettle's finished candies. A blank filter chooses a supported candy from available ingredients, preferring Hard Candy with water or Fudge with milk. Filters select candy **types**, not flavor/color components; multiple allowed types are alternatives, not a production quota or rotation.

Supply one **empty Candy Tray** to reuse, plus **sugar** (up to four per batch, two candy pieces per sugar) and the appropriate base:

| Desired candy | Base | Extras / finishing |
| --- | --- | --- |
| Hard Candy, Candy Corn, Caramel | Water bottle | Ordinary setting time: five seconds. |
| Salt Water Taffy | Water bottle | Peepo performs four one-second pulls while warm. |
| Lollipop | Water bottle | One stick for each candy piece. |
| Rock Candy | Water bottle | The poured tray crystallizes for one Minecraft day (20 minutes at normal speed). |
| Fudge, Cream Caramel, Toffee | Milk bucket | Ordinary setting time: five seconds. |
| Burnt Sugar | Water bottle | Only deliberately chosen when the filter leaves no preferred available alternative. |

Up to **two different flavors** and **one dye** per batch can also be fetched from Supply. They are optional; for predictable flavors/colors, link a dedicated Supply containing only the additions you want. The filter's ghost item does not request its specific flavor or color. Automatic candy corn uses one layer; making layered designs remains manual.

Ingredients wait in the kettle's companion storage until Peepo arrives with a complete batch and an empty tray. He then heats and pours at the required stage, uses the real tray's setting/pulling rules, and sends finished candy plus empty bottles/buckets to Output. He stays on the live batch instead of abandoning it for ordinary hauling or another job. Rest, lost access, explicit commands and other interruptions still apply: unattended hot syrup can overcook and unpulled taffy can harden, following the normal kettle rules. Actual results are preserved rather than converted into the requested candy after a missed stage.

Work costs **4 reserve JE per active tick**, plus normal delivery costs. Peepo can do other jobs while a tray sets or the empty kettle cools below its ingredient limit. The reusable tray remains at the kettle. Shift-right-click the **empty** kettle with an empty hand to recover stored ingredients, tray and outputs; with a live batch this still tips the syrup out as before. Breaking drops the stored items; poured syrup is lost as with the original kettle. Selection and stored tray progress survive saving/reloading. Changing a filter finishes an already cooking/setting batch, and returns unused staged ingredients through Output before switching recipes.

### Cider Barrel

Assign the barrel with the Planner, link its existing Supply and Output containers, and open the job's **Filter** button. Choose **Sparkling** (the default, one Minecraft day / 20 minutes) or **Aged** (three days / 60 minutes). These are game-time durations at normal server speed; Peepo does not accelerate aging. The selection belongs to the barrel and is shared by companions assigned to it. Ghost sparkling/aged items also work: an empty filter or both drinks means collect at the sparkling stage.

Put **Sweet Cider** in Supply. Peepo loads a batch up to the barrel's 16-serving capacity and available carrying space, saving its emptied bottles for the finished cider. A short five-second unloading window accommodates multiple carried stacks; additional companion pours do not restart the timer. After that window the batch ages without topping up. Peepo can do other jobs meanwhile, then bottle the actual ready drink and deliver it to Output. A sparkling batch left too long becomes aged normally, so a late collection brings **Aged Cider**, never relabeled sparkling cider.

A manually filled barrel can also be serviced: provide **glass bottles** in Supply for any missing bottling containers. Manual pouring/drawing still works. Surplus reserved bottles go to Output; shift-right-click the barrel with an empty hand to recover its reserved bottles yourself. Breaking the barrel drops these bottles and preserves the cider in the barrel item as before. Existing Supply/Output Auto/On/Off controls apply. Aging itself costs no companion energy; the normal delivery energy costs still apply.

### Farm Stand restocking

Stock each of the Farm Stand's six sale slots once and set its price through the normal stand screen. Select your companion with the Planner and right-click either half of the stand, then link **Supply**. Only a companion with the **same owner as the stand** can restock it; party access does not grant shop ownership.

Peepo remembers the exact item and components last stocked in each slot and replenishes it up to one full stack, including after it sells out. Replace a slot's goods manually to change what it stocks. Emptying it does not erase its remembered goods: use the job's **Filter** to allow only selected goods, or switch Supply Off to pause all restocking. An empty Filter allows all remembered goods. Templates and filters survive world reloads. Old stands learn their currently stocked slots; already-empty slots need to be stocked once.

Prices, purchases and Jugs still use the normal stand. Peepo never takes goods back out of sale slots or collects currency; **Output is unsupported** for this job. Full means the allowed sale slots are stocked. Restocking uses ordinary load-based delivery energy, with no extra processing cost.

### Alembic Crucible

Install a **written formula** on the crucible through its normal player interaction, provide its required heat and pipe water into its tank. Assign the crucible with the Planner and link **Supply and Output**. In the job's **Filter**, choose **Draught** (glass bottles) or **Salve** (bowls). Empty or mixed filters default to Draught. This choice is shared by all companions assigned to that crucible; the written formula determines the actual brew.

Supply must contain the formula's exact ingredients and preparations: a raw ingredient does not replace its crushed, roasted or other prepared reagent. Peepo delivers the remaining batch quantities into the native five ingredient slots, earliest missing preparation first, plus up to six required bottles/bowls. Finished brews retain their actual effects and components when delivered to Output. After a formula or form change, unused ingredients and incompatible empty containers can also return through Output. Ingredients used anywhere in the current formula stay in its buffer for reuse.

Brewing, stirring, temperature requirements and bottling run at the crucible's normal speed. Peepo does not choose or unlock formulas, award research practice, change heat, or carry water buckets for this job. Existing native automation still works. A batch with containers already loaded may finish using those containers before the new form takes effect. Changing the formula with a mixture still inside follows the native rules: finish bottling that mixture manually before the new formula can start. Status messages identify missing piped water, wrong temperature, missing ingredients/containers and blocked output. Only ordinary delivery effort is charged while hauling; waiting for the crucible costs no work energy.

### Spinning Wheel

Select Peepo or Jughead with the Planner, then right-click a **Spinning Wheel** to assign it. Link your existing **Supply** (wool) and **Output** (yarn) containers on the Supplies page; the job uses its usual Supply/Output Auto/On/Off settings. Leave a safe, clear floor in front of the wheel. Any supported wool color can be used; no tool is required.

One wool makes **four matching-color yarn balls**. Peepo feeds the thread with both hands while leaning and pumping a foot. Four one-second strokes cost **320 reserve JE total** (4 JE/t), plus normal delivery costs. One companion operates a wheel at a time. Yarn waits in the wheel's saved output until collected; the next batch waits for that output to clear. Shift-right-click the wheel with an empty hand to recover stored yarn manually. Breaking it drops the stored yarn and remaining wool.

A wheel acting as a cursed portal pauses companion input and work. This job spins wool only; it does not unravel clothing or activate the portal. Ordinary manual/redstone spinning remains available; a batch loaded or started by Peepo keeps its completed yarn in the output even if a player or redstone finishes it. Partial completed strokes survive a reload; an interrupted, incomplete stroke restarts.

### Gardens

Select your companion, shift-right-click the Planner, choose **Farmland: select two corners**, then right-click two opposite ground corners. Both corners are included, up to **16×16 blocks**, with up to three blocks of ground-height difference. One zone uses one of the four shared job slots. Peepo discovers farmland and planter boxes within those bounds, including newly tilled soil, without reassigning the zone. Gaps and paths are allowed. Existing saved eight-cell plots keep their exact boundaries, crop selection and Supply/Output links; remove and redraw them to use the new zones. Left-click either zone corner with the Planner, or remove its Jobs row, to delete it.

Use the garden row's ghost slot to choose a seed or supported raw crop; right-click/empty-cursor clears it to Automatic planting. Put real seeds in cargo or linked Supplies. Existing young crops finish growing before replacement. Harvests keep replanting material as needed and deliver surplus through Output.

Soil must already be tilled. Peepo does not irrigate or till new soil. The optional maintenance settings below enable repairing remembered farmland and applying fertilizer. Supported ordinary crops and Jugcraft tall crops are covered in the [garden record](features/companion-gardens.md#actual-crop-behavior-and-costs); orchard fruit uses the separate Orchard zone below. The additional garden crops below use these same zones; stem-grown pumpkins and melons are also harvested when attached to a stem inside the zone. Tomatoes require existing trellises. Leave aisles for tall plants.

Planter boxes share the Farmland zone and Supply/Output controls. Include planters between the corners; Peepo plants, harvests mature crops and replants with supplied seeds. For mushroom colonies, assign the supporting mushroom soil: a plot includes up to eight connected cells. Supply brings mushrooms for planting; picking requires shears or a knife and Output receives surplus harvests. Hanging-herb assignments save up to eight connected hanging spots. Supply brings **ready-made herb bundles**, Peepo hangs them to dry naturally, and Output receives dried herbs. He does not tie loose herbs and string into bundles. Reassign a plot after changing its connected layout. See the [garden record](features/companion-gardens.md) for supported soils, crops and tools.


### Orchards

For orchard harvesting, choose **Orchard: select two corners** in the Planner and click opposite ground corners. The 16×16 footprint includes fruit-bearing leaves up to **eight blocks above the higher corner**. Peepo picks ripe apples, chestnuts, pears, peaches, lemons, oranges, plums and bananas using the trees' normal yields, then leaves the foliage intact to regrow fruit. He works from safe ground below or beside the cluster, with an unobstructed overhead reach of up to eight blocks; fruit behind a trunk, floor or another canopy layer is skipped. Leave walkable space below the fruit-bearing leaves.

Orchards use the same real eight-slot cargo, job priorities and linked Output containers. Supply can stay linked, but this harvest-only job consumes no seeds, saplings or tools. It does not plant trees, cut logs, accelerate growth or collect unrelated ground items. Picking takes **80 ticks at 4 JE/t (320 JE)** plus hauling; a hoe gives no orchard bonus and suffers no wear. Full cargo leaves fruit on the tree until space is available.

Zone discovery is gradual: small batches of blocks are checked under the shared server budget, and unloaded chunks are skipped. New growth in a large zone may take time to be noticed. Garden zones retain a modest seed reserve (up to eight per compatible zone, 32 across four zones), rather than hoarding a seed for every potential soil block. Existing mushroom and herb assignments keep their eight connected positions and current supply/output behavior.

### Additional garden crops

Use the existing **Farmland: select two corners** option for sugar cane, bamboo, cactus, sweet berries, glow berries, cocoa, cranberries and rice. Existing zones gain this support automatically; each still uses one job slot and the existing Supply/Output links. The two corners define the ground-height range. Cocoa and glow-berry positions are also discovered up to **three blocks above the higher ground corner**, but Peepo must still reach them from safe nearby footing (2.5-block reach). Orchard zones remain fruit-tree harvesting.

| Crop | Harvest and planting behavior |
| --- | --- |
| Sugar cane | At three blocks tall, removes upper sections and keeps the bottom block. Valid soil and nearby water must already exist. |
| Bamboo | At four blocks or taller, harvests above the bottom block and leaves that base able to regrow. Checks at most 16 stalk blocks; taller player-built columns are skipped. |
| Cactus | At three blocks tall, harvests above the base. A cactus flower is also collected, including on a shorter cactus so the flower does not permanently stop growth. Peepo stays on safe adjacent ground. |
| Sweet berries | Picks fully mature bushes using normal harvesting loot, leaving bushes to regrow. Does not walk through them. |
| Glow berries | Picks berries without cutting their vines. Replants remembered missing vine positions only when their support still permits it. No bone meal is used. |
| Cocoa | Harvests mature pods and spends one bean from the harvest, or cargo, to replant on the same log face. With no bean available, the remembered position waits for Supply. |
| Cranberries | Picks ripe bushes for their normal 2–3 cranberries and leaves them flowering in water. Planting uses cranberries in shallow source water above bog soil. |
| Rice | Supply **Rice** for planting. Mature plants give **Rice Panicles**, reset to their native regrowth stage and keep their flooded base. Even replacing a ripe rice plant with a different selected crop leaves the water intact. |

For a mixed garden with **Automatic planting**, plant the special crops once so Peepo discovers and remembers their positions. Up to 256 special planting positions per zone persist with the companion, including cocoa faces and vine locations. Existing bushes/stalks remain planted during picking. Clearing a remembered plant allows Peepo to replace it; remove/reassign the zone to forget its planting positions. Clearing the crop selector does not erase these positions.

To establish **new ground plantings**, select cane, bamboo, cactus, sweet berries or cranberries in the garden's existing crop/seed slot and put real planting items in Supply. Peepo can then fill valid empty ground positions within the selected ground range. This is explicit expansion into valid spots: use a tightly drawn zone to leave paths untouched. Cocoa and hanging glow berries always require initial player planting to establish their log faces/support locations. Rice can fill prepared flooded soil using ordinary automatic seed selection, or select Rice/Rice Panicles to restrict the plot to it.

Leave accessible dry banks or paths beside flooded crops and safe aisles beside cactus/berry bushes. No irrigation, new-soil tilling, tree planting or construction is performed. Fertilizing is available through the optional maintenance setting below. Special picking takes **80 ticks at 4 JE/t (320 JE)**; rice retains normal garden hoe timing (40 ticks / 160 JE with one durability, otherwise 80 ticks / 320 JE). Existing planting timing and load-based delivery costs apply. Full cargo leaves the plant/column intact. Harvests use the existing small planting reserve and surplus delivery rules.

### Sprinkler restocking

Select Peepo/Jughead with the **Planner** and right-click a placed **Sprinkler** in normal assignment mode. Each sprinkler uses one work slot, independently of Farmland zones. Link existing Supply/Output containers and set the companion to Work.

- **Supply:** water buckets and the native **Fertilizer** item (`jugcraft:fertilizer`, the chemical superphosphate fertilizer). Bone meal and bat guano are not sprinkler inputs.
- **Water:** fills the existing 4,000 mB tank, adding 1,000 mB per real bucket only when the entire bucket fits.
- **Fertilizer:** keeps up to four items stocked; the native hopper still supports sixteen when supplied manually or by automation. Fertilizer is optional for ordinary watering.
- **Output:** takes returned empty buckets. Up to four are safely buffered in the sprinkler; a full bucket buffer pauses additional bucket delivery. Sneak-right-click the sprinkler with an empty hand to retrieve them manually. Buffered buckets survive reloads and drop when it is broken.

The native sprinkler still uses 50 mB every five seconds and its existing fertilizer pulses. Peepo adds no speed multiplier and spends normal load-scaled hauling energy, with no energy drain while the sprinkler runs unattended. Pipes and fertilizer hoppers remain usable; Supply On/Auto tops up missing stock, while Off disables companion deliveries. Auto does not disable this job merely because a pipe is nearby; actual remaining capacity determines demand.

For a closed bucket loop, use two chests: Water collection takes empty buckets from one and delivers water buckets to the other; the Sprinkler uses those same chests in the opposite roles. Use an infinite-water pool for repeated collection. One companion can have both assignments if it has two free work slots. See the [sprinkler feature record](features/companion-sprinklers.md).

### Water collection

Select Peepo/Jughead with the **Companion Planner**, use normal assignment mode, and right-click a **source-water block**. This uses one of the four work slots. Leave an accessible dry bank beside the water, link **Supply** and **Output**, and set the companion to Work.

Supply **glass bottles**, **empty buckets**, or **empty/partly filled Gourd Canteens**. Peepo fetches up to four containers total, fills them one at a time, and delivers filled containers through the existing Output routes. Containers already in his inventory can also be used. Each uninterrupted fill takes **two seconds and 160 reserve JE**, plus normal hauling costs. The existing general work animation and bottle/bucket filling sounds are used.

Bottles become ordinary water potions; canteens refill to all three charges and keep their custom components. Both preserve the source. Buckets use normal Minecraft pickup and **remove the source block**: use a renewable/infinite-water pool for continuous bucket filling. Flowing water, cauldrons and waterlogged blocks are not collection targets. A missing source pauses the assignment without losing already-filled cargo; it resumes when source water returns. Full cargo or Output pauses collection rather than discarding containers.

A collection Output chest can also be Supply for brewing, cooking or another job. This collects portable water containers; it does not irrigate crops or fill machine tanks directly. Mob-griefing, town protection, job priorities, resting and settings-menu pauses apply. See the [water collection feature record](features/companion-water-collection.md).

### Bat House collection

Select Peepo/Jughead with the Planner, then right-click a **Bat House** in normal assignment mode. Link an **Output** container and set the companion to Work. Each house uses one of the four job slots; Supply is unsupported and no tool is required.

Peepo uses the general two-arm interaction animation to scoop the house's accumulated guano. Collection takes **40 ticks / 2 seconds at 4 JE/t (160 JE total)**, followed by normal load-based delivery effort. It collects the whole current batch, up to the house's native sixteen-guano capacity. The guano occupies the existing eight cargo slots, with the same saved provenance and Output routing as garden harvests.

Leave safe standing room in front of or beside the house. Peepo can reach a house one block above its standing level; higher houses need a platform. Full cargo leaves the guano on the ledge. A full/missing Output leaves collected goods in cargo until they can be delivered. Only one companion collects at a house at a time; a player's manual collection is rechecked before the companion takes anything. The status shows **Waiting for guano** when empty.

Collection does not release or summon bats, speed up production, or apply fertilizer. The existing agriculture feature switch, mob-griefing rule, town protection, work priorities, rest and settings-menu pause apply. See the [Bat House feature record](features/companion-bat-house.md).

### Serving stations

Assign each **Harvest Feast Table** length with the Planner, then link Supply. Stock its two dishes once with the foods you want; Peepo remembers them and refills each to eight servings, even after diners empty them. Its **Filter** limits which remembered foods may be restocked. Turn Supply Off before clearing a dish you no longer want replenished.

Assign a **Candy Bowl** and link Supply for native treats. Peepo fills up to 64 treats; its Filter can restrict the candy types. The bowl's owner-access rules and visitors' nightly limits still apply. These two stations have no Output transport.

Assign a **Witch's Brew Punch Bowl** with Supply and Output. Supply berries accepted by the bowl plus empty glass bottles. Each berry makes two servings; each bottle takes one serving. Up to twelve finished drinks wait for Output. Empty-hand shift-right-click retrieves buffered drinks manually; breaking the bowl drops them. Existing manual berry and bottle use still works.

### Pumpkins, melons and optional garden maintenance

Farmland zones now pick **pumpkins and melons** attached to matching stems within the zone, leaving the stems to regrow. Supply provides real seeds; choose pumpkin/melon seeds in the planting slot to establish new stems. Leave empty fruit spaces beside them. Native fruit drops go to Output; decorative pumpkins without an attached matching stem are left alone.

Use the **gear beside a Farmland job** to enable either maintenance option. Both start Off:

- **Repair farmland:** give a hoe, or allow a Tools supply chest. Peepo re-tills previously observed farmland that has reverted to dirt, including rich-soil farmland. He does not till paths or arbitrary dirt. Let him observe intact farmland first; already-trampled ground must initially be tilled by you.
- **Fertilizing - Auto:** uses available bone meal, bat guano or the native Fertilizer item (`jugcraft:fertilizer`, chemical superphosphate) from cargo/Supply. Automatic restocking keeps at most **four fertilizer items total**. Maintenance is limited to **one application per zone every 20 seconds**, with a **60-second crop cooldown shared across helpers**. Harvesting and replanting take priority within the garden's current discovery window. Area fertilizer stays inside the zone and uses native growth rules.

Repair uses the hoe animation, one durability and **160 JE per completed action**. Fertilizing and stem-fruit picking use **320 JE per completed action**, plus normal hauling costs. Fertilizing excludes magic crops, trees and stems; it does not provide water or repair supports. See the [serving and maintenance feature record](features/companion-serving-maintenance.md) for limits and remaining manual checks.

### Magic gardens

Use the existing **Farmland** two-corner zone around your **Verdant Beds**. A player must awaken the beds first. Link Supply and Output as usual; select Sunpetal, Dewmoss, Gloamcap or Mendvetch in the garden planting slot, or leave it empty for automatic compatible planting. Supply brings planting items for empty beds. Existing magic plants are always preserved, including when the planting selection changes, so a mixed habitat is not uprooted. Harvesting produces the native crop items and Verdant Chaff and resets the plant to its native regrowth stage (currently stage 1). Output takes marked surplus after the usual planting reserve.

Magic planting and harvesting use the ordinary reaching animation, take **80 ticks / 4 seconds at 4 JE/t (320 JE per completed action)**, and do not use or wear a hoe. Delivery effort still scales with the load. Peepo cannot force growth, awaken beds, earn your hand-harvesting research practice, or fix moisture, nutrients, lighting or habitat conditions. Full cargo leaves the plant unchanged. If a Gleaner harvests a target first, Peepo discards that stale action and looks for another crop.

Assign a **Mulch Maw** as a separate job with the Planner and link **Supply**. Its **Filter** initially selects only Verdant Chaff; you can choose up to nine compostable items. Clearing the filter allows any native mulch item, including valuable produce, so use an explicit list when needed. Peepo maintains up to **16 items**, pauses restocking when the Maw has undelivered whole nutrients / reports full beds, and uses the Maw's existing digestion rate. The Maw has no Output job direction. Filtering is shared by companions assigned to that Maw and persists with the block.

Assign a **Gleaner** as a separate job and link **Output**. Peepo unloads its existing nine-slot harvest inventory. It needs no Supply and receives no harvesting speed bonus. The Gleaner still draws Verdance and spends **1 Verdance per native harvest**. Choose direct companion gardening or Gleaner harvesting plus companion hauling for a given patch to avoid wasted approach work.

The magic feature must be enabled, and beds/devices must be awake. Companions use their owner's identity and require access to the keeper's party. The magic system's owner-presence policy and protection checks apply; by default the companion owner must be online in the same dimension. Server administrators can use the existing Concordance absent-owner authority option. Maintenance, Verdant Heart operation and Habitat Gauge settings remain manual/native. See the [magic-garden integration record](features/companion-magic-gardens.md).

### Animal pens

For a combined **Livestock Management** zone, select your companion, shift-right-click with the Planner, choose **Livestock Management: select two corners**, then right-click two opposite ground corners. Both corners count toward the **16×16** maximum. Corners can differ by up to three blocks in height; the zone covers the space above the lower corner through three blocks above the higher corner. Each zone consumes **one work slot**, with Shearing, Milking and Eggs enabled initially. Open that job's **gear (Zone options)** button, and turn individual tasks On or Off. Breeding and Slaughter start **Off**. Turning all five tasks Off pauses new work; any overflow products already on the ground can still be recovered. Remove it through its job row or by left-clicking the first corner with the Planner.

The enabled tasks take turns as work becomes available. Supply provides empty buckets; a linked Supply with **Tools** enabled provides shears. Output receives wool, milk and collected eggs. Missing equipment pauses that activity without stopping another ready activity. Existing action costs, milk cooldowns, inventory limits and gate handling still apply.

To manage populations, open the job's **gear (Zone options) -> Population...**. Click the animal name to cycle through types currently inside the zone, then **Add type**. Compatible modded animals appear here too; **Refresh animals** updates the choices after moving animals into the pen. Only added types can be bred or slaughtered. Configure up to **eight animal types per zone**, each with:

- **Breed toward (total):** target population, initially **10**, including babies and pending pairs. Zero disables breeding for that type.
- **Adults to keep:** minimum adults retained, initially **6**. Values are bounded from 0 to 64.

Enable **Breeding** to feed eligible pairs using real breeding food from cargo or linked Supply. A pair must stand within eight blocks of each other, near enough to find each other once fed; animals farther apart wait until they wander closer. Each animal's normal breeding-food rules are used. This supports ordinary Minecraft-style modded breeding, including Jugcraft turkeys. Tamed/owned animals are excluded from automatic population management; animals with special breeding rituals need their own integration. A pair takes two three-second feeding actions at 4 reserve JE/t (480 JE total), plus deliveries, and retains normal mating and cooldown behavior.

Enable **Slaughter** to cull surplus eligible adults of selected types with a sword or axe from Hand or a Tools-enabled Supply. It requires an accessible linked Output and three free unreserved cargo slots after equipping the weapon. Babies, named, tamed/owned, leashed, in-love and breeding-cooldown animals are protected. Each normal attack takes two seconds at 6 reserve JE/t (240 JE); tougher animals may require multiple attacks. Weapon durability and ordinary loot rules apply. Products go to cargo and Output; overflow remains real ground items for later collection. Helpers coordinate claims and recount before breeding or attacking to respect population limits. Zones must be fully loaded; pens over 256 animals pause population work.

The normal Planner's sheep/cow/chicken shortcuts remain available for separate legacy radius jobs. Selecting an animal establishes a **fixed area**, not a job following that individual: eight blocks around the anchor and up to three blocks vertically from the point above it. Existing saved radius jobs keep their behavior and each consumes one work slot.

During shearing and milking, Peepo briefly holds the animal still. Babies, leashed animals, passengers, and animals in water, on fire or recently hurt are skipped. Shearing uses two-handed snipping; milking uses a bucket beneath the cow. An uninterrupted action takes three seconds and uses 240 reserve JE, plus delivery costs.

For **egg collection**, select Peepo with the Planner and right-click a chicken, or enable Eggs in a combined zone. Peepo picks up eggs already on the ground; laying rates stay unchanged and no chicken is held still. Each pickup takes one second and uses 80 reserve JE, plus delivery costs. He collects as much of a stack as fits, leaves the rest on the ground, and sends collected eggs to Output. Player-given eggs already in cargo remain personal inventory. Eggs outside the area, in water/fire, inaccessible or reserved by another helper are skipped.

Peepo opens and closes permitted, unpowered fence gates along its path. Animals retain a collision barrier during a companion-operated opening. Gates opened by players/redstone and other gaps still need normal pen management. Keep ground approaches accessible, including in diagonal-fence layouts. Animal jobs and automatic gate opening require `mobGriefing` to be enabled.

## Food, energy and rest

Companions store **128,000 reserve JE**. Ordinary passive recovery is **2 JE/t** while not running a wheel or actively spending work energy, even when moving. Sitting gives 8 JE/t; sleeping gives 16 JE/t at night. Better food grants a stronger, longer regeneration bonus and also restores health. These are companion reserves; electricity reaches your network through the Generator Wheel.

A tamed companion accepts food when hurt or below **95% energy**. When full-health and sufficiently charged, it refuses more food. It can eat carried food, suitable nearby dropped food or food from an accessible lunch source. It avoids automatically eating another energy meal while the current buff is still sufficient, unless hurt.

Place a **Lunch Crate**, or put a **Lunch Cover** over a compatible accessible container, then assign the crate/cover with the Planner. Lunch is separate from work Supply: ingredients in a Supply chest do not by themselves make that chest an assigned lunch source.

On **Routine**, set the work schedule, break/resume energy thresholds, food preference, carried meal target, alerts and social behavior. Auto works whenever energy allows; Day/Night shifts use Overworld time. Exhausted companions rest before resuming. Beds restore more energy than seats, but sleeping happens only at night in the Overworld. Other dimensions use seated/passive rest.

Companion beds are small, come in the vanilla bed colors and stack into bunks. Assign the specific bunk as home and leave the bottom entrance accessible. Vanilla beds can also be homes. Stools, supported player seats, bed edges and supported fence rails provide sitting places. They close their eyes for sleep and kick their legs while sitting.

## Personal touches and moving companions

- Use a name tag to name a companion; its name appears in relevant interfaces.
- Empty-hand right-click makes it blush. Enable **Social** in Routine for greetings and little conversations during suitable idle time.
- Give it a Jack o'Lantern for the pumpkin costume, or manage the Costume slot in its inventory. Remove the costume from that slot to restore its default outfit.
- Wooden **Mob Transport Crates** hold four friendly mobs; iron crates hold eight. Right-click a suitable mob to capture it, right-click the top of a block to release one, or shift-right-click to choose a stored mob to release beside you. Companions retain their inventory, owner, energy and assignments. Assignments remain tied to their original dimension, so check them after moving. See [crate controls and restrictions](features/mob-transport-crates.md#using-a-crate).

## When a companion is not working

| Symptom | Check |
| --- | --- |
| Path Blocked | Provide a reachable approach, solid safe footing and enough headroom. Move hazards away. Leave room beside the pot/board and in front of an oven. Unloaded routes cannot be used. |
| Missing tool / needs shears | Equip the correct tool in Hand, or enable Tools on an allowed Supply. Check Hand lock, durability and cargo space. A stowed tool is still available. |
| Needs buckets / no input | Put the actual bucket or ingredients in cargo/Supply. Check job links, Filter, transport mode and container access. |
| Full | Clear the machine's result storage or a permitted Output and leave cargo room. Milk buckets do not stack. |
| Waiting for animals | Check the fixed anchor, adult animals, wool regrowth, cow cooldown and safe standing space. |
| No heat / no power | Supply the workstation's ordinary heat, electricity, fuel or fluids. Assistance does not replace those requirements. |
| Scheduled rest / recovering | Check Routine settings, food and home access. Opening the settings menu intentionally pauses work. |
| Supply or Output suppressed in Auto | Hover the direction button. Use On if companion backup is wanted, or Off to stop that direction. |
| Unsupported job | The block has no companion adapter; assigning arbitrary blocks does not add an integration. |

Companions avoid known damaging surfaces, including lit stoves, fire, lava, magma, lit campfires, ember beds and barbed wire. Avoidance is not damage immunity: player placement, knockback and unknown hazards can still hurt them. Do not build an essential route across a hot surface.

Server administrators can inspect shared search/path usage with **`/peepobudget`**. The existing server settings `companions.paths_per_tick` and `companions.searches_per_tick` control admission budgets. These settings do not change the number of assignments or the animal radius.

## Version and verification

Use matching client/server builds. This guide documents implemented behavior, not a guarantee that every layout or mod interaction has been play-tested. Recorded focused livestock checks are in the [livestock feature record](features/companion-livestock.md). The additional crops and [magic-garden integration](features/companion-magic-gardens.md) extend existing garden zones and logistics; their feature records distinguish compilation from unperformed gameplay tests. Multiplayer load and additional gate-layout verification remain separate checks.

Prepared with OpenAI Codex (GPT-6). Detailed implementation history and recorded checks remain in the linked feature records.

### Hive harvesting

Assign a Beehive or Bee Nest with the Planner, link Supply and Output, then use its **Filter** button to select **Honey** or **Honeycomb**. Empty or mixed honey/honeycomb filters default to Honey. The hive must have honey level 5 and receive campfire smoke using vanilla smoke rules. Keep a safe standing space beside the hive; Peepo can also reach from one block below. Enclose the campfire so neither Peepo nor bees can walk into the fire.

- **Honey:** Supply empty glass bottles. Peepo stocks up to four bottles at the hive and consumes one per harvest.
- **Honeycomb:** Equip shears or provide them through an existing Tools supply chest. Each harvest consumes one durability. Unused stocked bottles are sent to Output when switching to Honeycomb.
- Harvesting takes three seconds at **4 JE/tick (240 JE per completed harvest)**, plus normal delivery effort. He uses the existing two-handed reaching/snipping motion with a bottle or real shears and vanilla sounds.
- No smoke, missing bottles/shears, full collection storage, blocked access or disabled mob griefing pauses harvesting. Bees are neither released nor angered by the harvesting action.
- Collected products occupy twelve saved collection slots at the hive until transported. Empty-hand sneak-use retrieves stored products and unused bottles manually; breaking the hive drops them. Filters are station-wide, so companions assigned to the same hive share its selection and only one harvest worker can claim it at a time.

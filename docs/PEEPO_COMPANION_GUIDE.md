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
| Right-click farmland or a crop on farmland | Assigns a garden plot. |
| Right-click a sheep or cow | Assigns a fixed animal-work area at the ground beneath it. |
| Right-click a lunch crate or lunch cover | Assigns its separate lunch source. |
| Right-click a new ordinary container | Adds the next Supply, or an Output once all four Supplies are assigned. |
| Right-click an assigned ordinary container | Switches that container between Supply and Output if the new role has space. |
| Left-click an assigned block | Removes the assignment without mining the block. For animal jobs, click the marked ground anchor. |
| Shift-right-click air | Clears the Planner's selection. |

The selected companion, home, jobs and lunch source have **green** frames; Supply is **blue**, Output **yellow**, and missing/replaced targets **orange**. Garden frames show the saved soil cells. Animal areas show their anchor and an 8-block-radius bounding square.

Use the Jobs page's **up/down arrows** to order work: the top job has highest priority. Unavailable jobs are skipped. The **x** buttons can remove assignments even when their blocks are missing or in another dimension.

New assignments must be within 64 blocks of the companion. Planner editing requires the companion to be loaded, in your dimension and within 128 blocks of you, and you still need to reach the block or animal you click. Companions do not load chunks or teleport supplies between dimensions. Leave reachable standing space beside machines; elevated stations need an accessible approach. Jughead also needs clearance for his jug.

## Supplies, tools and delivery routes

Open **Supplies** to access these three views:

| View | Setup |
| --- | --- |
| Containers | Review/remove up to four Supplies and four Outputs. Enable **Tools** on Supplies containing knives, hoes or shears. Use **Hand: Auto tools / Hand: Locked** to control automatic swapping. |
| Job links | Choose which numbered Supplies and Outputs each work assignment may use. **All** includes all current and future containers; numbered buttons select a subset. |
| Porter routes | Configure up to four direct Supply-to-Output routes. Choose both endpoints, optionally set a Filter, then turn the route **On**. Changing an endpoint turns that route Off until you enable it again. |

Containers keep their numbers when another is removed. A container can have only one role for a given companion. The face clicked when assigning a container matters for sided inventories. A chest can serve one companion's Output and another's Supply to form a production line.

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

On a recipe-capable job row, click **F** (hover label **Filter**) to open a **3×3 grid of nine ghost slots** in the companion GUI.

- Click a slot with a finished output item on your cursor to allow that output. Your real item is not consumed.
- Right-click a slot, or click it with an empty cursor, to clear it.
- An **empty grid allows any available recipe** supported by that workstation's companion integration.
- The grid is an allow-list, not a crafting pattern or priority order. Put real ingredients in linked Supply containers.
- Workstation filters belong to the station and are shared by companions assigned to it. Some filter changes reset processing progress, but do not discard stored ingredients.

Filters are available for Cooking Pots, supported standard processors, Hearth Ovens, Cutting Boards, Skillets and Kitchen Stoves. Hearth Ovens accept whole raw or baked pies/cakes as filter icons and display the baked result. Cider Presses and Canning Kettles use their fixed workflows without this selector. Gardens have a separate single crop/seed selector.

## Supported jobs

| Assignment | What to provide and expect |
| --- | --- |
| Generator Wheel | One companion runs the wheel using its energy reserve. It stops when the buffer is full or it needs rest. Electrical output is from the two sides. The GUI shows stored power, companion energy and its name. |
| Hand Crank | One companion turns it and pauses when a connected flywheel is full. |
| Standard processing machines | Peepo assists production. Full multiblocks generally support two helpers at **+25% speed each**; compact/single-block machines generally support one at **+50%**. Supported item recipes can fetch ingredients and collect outputs. Normal power, fluid, heat and machine-formation requirements remain. Electrical generators and storage are not processor assistance jobs. |
| Cooking Pot | Provide heat and the recipe's ingredients/containers. One helper stirs from the rim and gives **+50% cooking speed**; Supply and Output handle the supported recipe items and results. |
| Cutting Board | Provide a knife and valid cutting inputs. Peepo stands at the board and makes a two-handed chop. The knife loses durability; all recipe results are retained for Output. |
| Skillet | Heat it from below. Supply brings compatible raw food and Output collects fried food. Cooking continues without a resident helper; no speed bonus is added. |
| Kitchen Stove | Light it and keep its top clear for its own cooking. Supply brings cookable food and Output collects cooked food. A pot/skillet on top uses the stove as heat instead; assign that upper station for its workflow. |
| Cider Press | Supply valid press ingredients and glass bottles. Peepo loads, presses, bottles and collects products through the press's normal workflow. |
| Canning Kettle | Provide active heat, a water bucket when dry, and **full, fresh, unsealed preserve jars**. It holds four jars and processes them together; Output takes sealed jars, rejected spoiled jars and the returned empty water bucket. Empty Mason Jars are not ingredients. |
| Hearth Oven | Supply **prepared raw pies/cakes** and suitable fuel such as hearth logs, charcoal, coal or coke. One bake fits at a time. Peepo loads it, waits nearby, collects it when baked and carries it to Output. It does not craft raw bakes from dough, batter or fruit. Ordinary heat/burning rules remain. |
| Farmland | Harvest mature supported crops, replant, and plant empty saved soil cells with available seeds. A hoe improves speed and energy efficiency. See garden setup below. |
| Sheep area | Give shears in Hand or through a Tools supply. Peepo shears eligible adult woolly sheep within **8 blocks** of the anchor. Output receives wool. |
| Cow area | Give empty buckets in cargo or Supply. Peepo milks eligible adult cows within **8 blocks** of the anchor. Output receives milk buckets. Companion milking has a **60-second cooldown per cow**. |

The [processor list and effort table](features/companion-jobs.md#processor-assistance-and-helper-teams) provides machine-specific support and reserve costs. Workstation item transport does not cover every special setup: fluid-only processes and unsupported pattern/ingredient workflows still need their ordinary infrastructure.

### Gardens

Each assignment saves up to **eight edge-connected farmland blocks at the same height**. Four garden assignments can cover at most 32 soil blocks; gardening shares the four work slots with all other jobs. Reassign a plot after extending or reshaping it.

Use the garden row's ghost slot to choose a seed or supported raw crop; right-click/empty-cursor clears it to Automatic planting. Put real seeds in cargo or linked Supplies. Existing young crops finish growing before replacement. Harvests keep replanting material as needed and deliver surplus through Output.

Soil must already be tilled. Peepo does not irrigate, fertilize, expand plots or accelerate growth. Supported ordinary crops and Jugcraft tall crops are covered in the [garden record](features/companion-gardens.md#actual-crop-behavior-and-costs); trees, sugar cane and other unsupported growth systems need different automation. Tomatoes require existing trellises. Leave aisles for tall plants.

### Animal pens

The selected animal establishes a **fixed area**, not a job following that individual. The area can include other eligible animals of the same kind, within eight blocks and up to three blocks vertically from the point above the anchor. Each area consumes one work slot.

During work, Peepo briefly holds the animal still. Babies, leashed animals, passengers, and animals in water, on fire or recently hurt are skipped. Shearing uses two-handed snipping; milking uses a bucket beneath the cow. An uninterrupted action takes three seconds and uses 240 reserve JE, plus delivery costs.

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

Use matching client/server builds. This guide documents implemented behavior, not a guarantee that every layout or mod interaction has been play-tested. The latest tool-stowing and livestock changes compiled and were packaged; their gameplay, gate/reload behavior and large-server performance still need verification. No new tests were run for this documentation update.

Prepared with OpenAI Codex (GPT-6). Detailed implementation history and recorded checks remain in the linked feature records.

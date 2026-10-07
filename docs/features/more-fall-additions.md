# More Fall Additions

Status: all ten are implemented in source, none yet played by hand: the candy kitchen (addition 11), autumn foraging (addition 12), the Bat House (addition 13), the Hay Golem (addition 14), knitting (addition 15), pie baking (addition 16), the Spirit Board (addition 17), wild turkeys (addition 18), the theremin (addition 19) and the Día de Muertos ofrenda (addition 20). The Build workflow compiles them, and CI's game tests and client screenshots are recorded below.
Proposal issue: none; requested directly by the owner on 2 October 2026 ("lets do another 10 detailed halloween and fall themed additions", then "start them now stacked on #33"). They follow the ten [fall additions](fall-additions.md), numbered on from them, one per pull request, each stacked on the one before:
11. the candy kitchen: the Candy Kettle, its thermometer and the candy stages, the Candy Tray, and ten candies;
12. autumn foraging: five wild mushrooms that spread in the shade and sprout fairy rings under the full moon, the Foraging Basket, and four mushroom dishes;
13. the Bat House: a roost for bats that fly out at dusk and back at dawn, leaving guano, a fertilizer and a source of phosphate;
14. the Hay Golem: a straw man with a carved pumpkin head that guards crops from crows and tends them, carrying the harvest home;
15. knitting: the Spinning Wheel spins wool into yarn, Knitting Needles knit it into beanies, socks and five sweaters, and knitwear keeps you cosy by a campfire;
16. pie baking: the Hearth Oven, a brick bread oven that bakes five pies golden (or black, left too long), placed on the table and eaten or cut a slice at a time;
17. the Spirit Board: a candlelit séance on which a restless spirit spells its name and the one thing it wishes for, and the gift that lays it to rest;
18. wild turkeys: flocks in woods and meadows, toms that strut and gobble, hens that lay eggs, and a roast turkey carved at the table;
19. the theremin: an eerie instrument played without touching, singing higher the nearer you stand, that doubles as a proximity sensor;
20. the Día de Muertos ofrenda: a home altar of remembrance, set with cempasúchil marigolds, candles, pan de muerto, sugar skulls and a drink, with papel picado and a path of petals, that welcomes the spirits near.

Owner: @jimbozoomer-byte
Target milestone and tier: the candy kitchen is Discovery tier: copper ingots and a glass pane (the kettle), iron nuggets (the tray), sugar, a water bottle or a milk bucket, a campfire, dyes, sticks (for lollipops), and the flavours, all early: cocoa beans, sweet berries, glow berries, a honey bottle, and the Festival Crops' cranberries, the cider mill's mulling spices and roasted chestnuts. Autumn foraging is Discovery tier: the mushrooms are found on the forest floor from the first day, the basket is sugar cane and a stick, and the dishes cook in a furnace, smoker, campfire or the Cooking Pot. The Bat House is Discovery tier: seven planks and a stick. The Hay Golem is Discovery tier: four hay bales (36 wheat) and a carved pumpkin. Knitting is Discovery tier: planks, sticks and string (the wheel), two iron nuggets and two sticks (the needles), and wool. Pie baking is Discovery tier: seven bricks and a furnace (the oven), coal, charcoal or logs to burn, and wheat, an egg, sugar and the fillings: apples, a pumpkin and milk, the Festival Crops' cranberries, the Kitchen Garden's sweet potatoes or the cider mill's roasted chestnuts. The Spirit Board is Discovery tier: two birch slabs, an ink sac and a glass pane, and a candle; the spirits it speaks with come with ghost hunting, and seeing them takes a Spirit Lantern. Wild turkeys are Discovery tier: they wander into woods, taiga, plains and meadows by day, seeds bring them and breed them, and a raw turkey roasts in a furnace, smoker or on a campfire. The theremin is Mechanical tier: two copper wires (from the Wire Drawer), two planks, a note block and two sticks. The ofrenda is Discovery tier: planks and white carpet; marigolds grow wild with the mums and are planted anywhere; pan de muerto is wheat, an egg and sugar; sugar skulls are sugar, an egg and dye.
Primary specialty and supported player role: cooking and crafting. A candy maker turns sugar cane into the treats that fill Candy Bowls and Candy Bags, and flavoured candy is a pocketful of short effects (speed, haste, night vision, resistance, fire resistance, absorption, regeneration) to hand round before a dig or a fight. Candy is easy to trade: each piece says its flavours in its name. Autumn foraging is for explorers and cooks: mushrooms to find, farm in the shade and cook, and fairy rings to find or plant for a night's Luck before fishing or opening loot. The Bat House is for farmers: a free trickle of fertilizer from a block on a wall, and phosphate for a chemist with no phosphorite near. The Hay Golem is a farmhand: it keeps crows off a field and brings in the ripe crops, so a small farm runs while its farmer is away mining. Knitting is for crafters and shepherds: wool becomes clothes in any colour, warm against powder snow, and a set of knitwear by a campfire heals a party back up between trips. Pie baking is for cooks: a pie is four filling slices from one bake, to set out at a feast or cut and carried on a trip, and baking it right takes watching the oven. The Spirit Board is for ghost hunters and friends together: a séance goes faster with more hands on the planchette, and finding what a spirit wishes for sends the group to the baker, the knitter, the candy maker or the orchard. Wild turkeys are for farmers and hunters: a second bird to keep, whose hens lay eggs and whose toms put on a show, and a roast that feeds a table of six. The theremin is for builders and redstone engineers: the haunted house's music, played by whoever walks past, and a sensor that tells how near someone is. The ofrenda is for builders, cooks and ghost hunters together: an altar made of other players' work (the baker's bread, the cider maker's drink, the chandler's candles, the gardener's flowers) that calls the wandering spirits home and shows them to everyone.

Nothing here depends on the Halloween event: the kettle boils candy all year, the mushrooms grow all year, and fairy rings bless on every full moon. Bats come and go every night, a Hay Golem works all year, knitwear is worn in any season, pies bake all year, spirits answer on any night, turkeys come in every season, a theremin plays all year, and an ofrenda can be set up on any night (Día de Muertos falls on 1 and 2 November, but nothing waits for the date).

## Player experience
### The Candy Kettle
1. A polished copper sugar pot with two handles and a round candy thermometer clipped to its front (seven copper ingots and a glass pane). It faces you when you place it. Set it over a heat source: a lit campfire, fire, soul fire, lava or a magma block (the same `jugcraft:heat_sources` the Cooking Pot uses).
2. **Fill it before it boils** (anything added at 100°C or hotter is refused: "put everything in before it boils"):
   - **a base:** a water bottle for sugar syrup (the bottle comes back), or a milk bucket for cream (the bucket comes back). One base a batch.
   - **sugar:** up to four. Each sugar makes two pieces of candy.
   - **flavours:** up to two, each once (listed below).
   - **dyes:** any number, mixed as on leather. Undyed candy takes its first flavour's colour, or its own.
3. **Heat it.** With a base and sugar in, over the heat it warms a degree every 4 ticks to the boil (100°C); slowly, a degree every 12 ticks, while its water boils off to 110°C; then a degree every 6 ticks, up to 190°C. Bubbles rise from it from the boil, steam past caramel, and smoke once it burns.
4. **Watch the thermometer.** The needle swings round a dial painted with the stages in their colours. A bell rings a note higher as the batch reaches each stage; burning hisses. With an empty hand, use the kettle to read it: "Candy Kettle: 117°C (Soft Ball). Sugar Syrup, 3 sugar, Chocolate. Pour now for Candy Corn".
5. **The hottest it has been decides the candy**, so taking it off the heat (putting the campfire out with a shovel, say) holds it at its stage while it cools. Off the heat it cools a degree every 8 ticks.
6. **Pour** by using it with an empty Candy Tray (one tray of a stack). Pouring empties the kettle and earns **Sweet Science**. Sneaking with an empty hand tips the batch out instead.
7. **Comparators** read the stage a batch has reached: 0 for syrup (or an empty kettle) up to 8 for burnt.

### The stages
| Stage | From | Sugar syrup sets into | Cream sets into |
| --- | --- | --- | --- |
| Syrup | below 110°C | not ready | not ready |
| Thread | 110°C | **Rock Candy** | not ready (too runny) |
| Soft Ball | 115°C | **Candy Corn** | **Fudge** |
| Firm Ball | 120°C | **Candy Corn** | **Cream Caramel** |
| Hard Ball | 125°C | **Salt Water Taffy** | **Cream Caramel** |
| Soft Crack | 132°C | **Salt Water Taffy** | **Toffee** |
| Hard Crack | 145°C | **Hard Candy** (or lollipops) | **Toffee** |
| Caramel | 155°C | **Caramel** (the Halloween harvest's, for caramel apples) | **Burnt Sugar** |
| Burnt | 175°C | **Burnt Sugar** | **Burnt Sugar** |

From the boil, each stage lasts between 30 ticks (thread, soft ball, firm ball) and 120 ticks (caramel) of heating, so a cook has a second or more to pour at the narrowest, or can take the kettle off the heat to stop it.

### The Candy Tray
1. A tin tray of five iron nuggets, stacking to 16; a tray of candy doesn't stack. It shows its candy in the candy's colour and is named for it: "Tray of Chocolate Fudge".
2. **Setting:** use a tray of candy to break it into its pieces once it has set (five seconds), and the tray is empty again. Until then: "Still setting".
3. **Rock candy** grows its crystals for a whole day (24,000 ticks) before it can be broken up.
4. **Taffy** must be pulled while it is warm (for 30 seconds after pouring): hold use for a second to pull it, four times. Pulled ("light and chewy"), it cuts into Salt Water Taffy, and earns **Pulling Power**. Left to go cold before it's pulled, it sets hard: it breaks up as hard candy.
5. **Hard candy** broken up with sticks in your other hand makes a lollipop a stick (the sticks are used), and hard candy with the rest.
6. **Candy corn** is poured in layers: pour another batch of candy corn onto a tray of it, up to three layers, each its own colour. The pieces have three bands, tip to base: one layer colours all three, two the tip and the rest, three one band each. An undyed layer takes the classic colour of its band (white, orange, yellow), so three undyed layers make plain candy corn, which stacks with candy corn from trick-or-treating. Two flavours at most across the layers.

### Candy
- **Rock Candy** (2 food), **Salt Water Taffy** (2), **Hard Candy** (1), **Lollipop** (2), **Fudge** (3), **Cream Caramel** (2), **Toffee** (2) and **Burnt Sugar** (1), plus the Halloween harvest's **Candy Corn** and **Caramel**.
- Candy is eaten quickly (0.8 seconds), and even on a full stomach. Every candy counts as candy (`c:foods/candy`), so Candy Bowls, Candy Caches and Candy Bags take it.
- **Flavoured candy** is named for its flavours ("Chocolate Fudge", "Glow Berry and Honey Lollipop"), lists them in its tooltip, and gives each flavour's effect when eaten:

| Flavour | Stirred in with | Effect when eaten | Colour (undyed) |
| --- | --- | --- | --- |
| Chocolate | cocoa beans | Speed, 10 s | dark brown |
| Berry | sweet berries | Regeneration, 4 s | red |
| Glow Berry | glow berries | Night Vision, 30 s | gold |
| Honey | a honey bottle (the bottle comes back) | Absorption, 10 s | amber |
| Cranberry | cranberries | Resistance, 10 s | crimson |
| Spiced | mulling spices | Fire Resistance, 15 s | cinnamon |
| Chestnut | roasted chestnuts | Haste, 15 s | chestnut brown |

- Burnt sugar keeps no flavour: it tastes of nothing but burning. Caramel keeps its flavours but not its dye.

### Wild mushrooms
1. Five wild autumn mushrooms grow in patches on the forest floor, each in its own biomes:

| Mushroom | Found in | Use |
| --- | --- | --- |
| **Chanterelle** (golden, frilled) | forests, birch forests | Sautéed Chanterelles, Forager's Stew |
| **Porcini** (a fat brown cap on a pale stem) | taiga, forests | Roasted Porcini, Forager's Stew |
| **Puffball** (a white ball) | plains, forests | Fried Puffball, Forager's Stew; bursts in a cloud of spores when picked |
| **Fly Agaric** (red with white spots) | birch forests, taiga | not food; fairy rings and decoration |
| **Jack o'Lantern Mushroom** (an orange cluster) | spooky biomes, forests | not food; it glows (light 9) |

2. They grow on soil: grass, dirt, coarse or rooted dirt, podzol, mycelium and moss (the block tag `jugcraft:mushroom_soil`), not on stone or sand.
3. **They spread in the shade.** One random tick in 25, a mushroom with fewer than five of its kind within four blocks puts out another nearby (two blocks across, a block up or down), where the light is below 13. Under trees a patch fills in; in the open it doesn't.
4. **Bone meal** makes it spread at once, in any light, still up to five.
5. Picked, each drops itself; they go in a composter.

### Fairy rings
1. **A fairy ring** is eight or more wild mushrooms in a circle between 2.5 and 3.6 blocks from a centre (a block up or down counts), of any kinds.
2. **Mushrooms sprout rings under a full moon.** On a full-moon night, one random tick in 40 a wild mushroom plants a ring of its own kind: a circle of radius three that it stands on. A mushroom goes wherever the ground takes one, and nothing sprouts where a ring already stands. Anyone can plant a ring by hand too.
3. **Stand in its centre on a full-moon night** (in the Overworld) and you are blessed, once a night: Luck II for five minutes, a chime, a shimmer round the ring, "You dance in the fairy ring under the full moon. You feel lucky", and the advancement **Away with the Fairies**. Luck improves fishing and loot from chests.

### The Foraging Basket
1. A wicker basket (five sugar cane and a stick). It holds forage, like a bundle: wild mushrooms, vanilla mushrooms, sweet berries, glow berries, apples, cocoa beans, chestnuts and cranberries (the item tag `jugcraft:forage`), and nothing else.
2. **Pick mushrooms with it in either hand** and they go straight into it (what doesn't fit drops as usual).
3. A basket holding all five wild mushrooms earns **Forager**.

### Mushroom dishes
- **Sautéed Chanterelles** (5 food), **Roasted Porcini** (6) and **Fried Puffball** (4): the mushroom cooked in a furnace, smoker or campfire.
- **Forager's Stew** (10 food): a chanterelle, a porcini, a puffball, a potato and a bowl in the Cooking Pot.
- All four are tagged `c:foods`, so the Harvest Feast Table counts each as a different food.

### The Bat House
1. A slatted wooden roost with a little roof and a landing tray below it (seven planks and a stick). Hang it on the side of a block (it faces out from it) or stand it on top of one (it faces you).
2. **Bats roost in it by day.** At **dusk** every roosting bat pours out of it, a puff and the sound of wings, and anyone within 16 blocks earns **Night Shift**. Bats let out are ordinary bats, tagged as the house's.
3. **At dawn** the bats nearest it, within 32 blocks (its own and wild ones), come in to roost, up to four. Each leaves a **Bat Guano** on the tray, up to 16 waiting. The tray shows none, a little, more or a pile.
4. **A new house fills by itself:** at dusk, while mobs spawn (the `spawn_mobs` game rule), a house with room has an even chance of a bat moving in first, so a house with no bats near still has residents in a few days.
5. **Use it with an empty hand** to scoop up the guano ("Bat House: 3 of 4 bats roosting. You scoop up 7 guano"), or just to see how many bats roost.
6. **Comparators** read the roosting bats: 0 for none, 15 for a full house.
7. Broken, it lets its bats out (none are lost) and drops its guano and itself.

### Bat Guano
- **A fertilizer:** use it on a crop or the ground, and every crop in the 3x3 patch round it (a block up or down too) gets a dose of bone meal, by its own rules. One guano is used if anything grew. Only crops (`minecraft:crops`), never grass or saplings. It is the superphosphate fertilizer's rule over a smaller area and one dose.
- **Phosphate:** four guano make a phosphate (crafting), the same phosphate that is mined from phosphorite ore.
- It goes in a composter.

### The Hay Golem
1. **Build it like an iron golem, from hay:** two hay bales stacked, a hay bale either side of the upper one for arms (nothing under the arms), then use a carved head on the top of the upper bale: a carved pumpkin, a jack o'lantern, or a hand-carved pumpkin with a face cut (any of the `jugcraft:hay_golem_heads`). The hay comes to life as a straw man two and a half blocks tall, wearing the head and facing its builder, and the builder earns **Man of Straw**. A plain pumpkin, an uncut one, a missing arm or a block under an arm leaves it as hay, so pumpkins can still decorate hay bales.
2. **Its post:** it keeps to where it was built, wandering within eight blocks. Hold wheat near it and it follows you; where you leave it is its new post.
3. **A walking scarecrow:** crows won't raid crops within eight blocks of it, or twelve if its head is lit (a jack o'lantern, or a hand-carved pumpkin with a torch), as a scarecrow wearing that head would guard.
4. **It tends the crops round its post:** every five seconds it looks for a ripe crop within eight blocks of its post (two up or down). It walks to it, bends over it a moment and harvests it, replanting it from its own drops when they include its seed (wheat seeds, a carrot, a potato, a beetroot seed, Jugcraft's seeds). The rest goes in its pouch of nine stacks.
5. **It carries the harvest home:** with 32 items or more, or once nothing has been ripe for ten seconds, it walks back to its post and puts its pouch into the chest, barrel or hopper under it, or sets it down there if there is none. If it can't get home in fifteen seconds it sets its load down where it is.
6. It harvests only while the `mob_griefing` game rule is on, as crows only peck while it is.
7. **Looking after it:** wheat heals it 4 health. Shears take it apart again: its four hay bales, its head and its pouch. Fire hurts it twice as much. It never despawns. Killed, it drops two to five wheat, its head and its pouch.

### The Spinning Wheel
1. A big spoked wheel on a slanted plank bench, a treadle underneath, the distaff at the front and the spindle between its maidens (three planks, three sticks and a string). It faces you when you place it.
2. **Put a skein on the distaff:** use it with any wool. One skein at a time ("there is wool on the distaff already").
3. **Work the treadle:** use it with an empty hand, or give it a redstone pulse. The wheel spins for a second after each turn. After four turns the skein is spun: four balls of yarn in the wool's colour, set out in front of the wheel, where a hopper can take them.
4. **Unravel knitwear:** use a garment on it and you get its yarn back, a ball a row, less one ball too worn to use: a sweater gives four, a beanie or socks one.
5. Comparators read it: 0 empty, then 1 plus the turns so far.

### Knitting Needles and yarn
1. **Knitting Needles** (two iron nuggets and two sticks): hold them with a ball of yarn in your other hand and hold use to knit a row, two seconds a row, using the ball. The needles say which row you are on.
2. **Projects:** sneak and use with no rows on the needles to change project: a Knit Beanie (two rows), Wool Socks (two rows), or a sweater (five rows): the Knit Sweater, the Striped Sweater, the Pumpkin Sweater, the Bat Sweater or the Autumn Leaf Sweater, each a pattern knitted into it.
3. **Colour:** each row is the colour of its yarn, and the garment comes out the blend of its rows: knit three orange rows and two black for a dark orange sweater. Undyed yarn is cream.
4. **Unpicking:** with rows on the needles, sneak and use to unpick them: the yarn comes back, in the rows' blend.
5. When the last row is done the garment is yours (**Knit One, Purl Two**) and the needles take a point of wear (128 garments).
6. The work belongs to the needles, so it keeps when they are put away or handed to a friend to finish.
7. **Dyeing:** yarn and knitwear take dye at a crafting table as leather armour does in Minecraft 26.3: the item and any dye, through a dyeing recipe of the same kind as vanilla's. A water cauldron washes the dye out again.

### Knitwear
1. Beanies go on the head, socks on the feet, sweaters on the chest. They show on the player in their colour, a sweater's pattern over it. They have no armour value.
2. **Warm:** knitwear keeps out powder snow's cold, as leather does.
3. **Cosy:** wearing two or more pieces within four blocks of a lit campfire (two up or down), you are cosy: Regeneration I, renewed every two seconds while you stay. A beanie, a sweater and socks by the fire earn **Snug as a Bug**.

### The Hearth Oven
1. A domed brick bread oven on a stone hearth, its arched mouth to the front and a chimney out of the back (seven bricks round a furnace). It faces you when you place it.
2. **Fire:** use fuel on it to feed the fire: coal (80 seconds), charcoal (60) and coke (160) burn as long as they do in Jugcraft's generators; a log burns 15 seconds, as in a furnace. It banks up to 160 seconds of fire ("the fire is banked as high as it goes"). Lit, it glows (light 13), flames flicker in its mouth and smoke rises from the chimney.
3. **Heat:** burning, it heats a degree every two ticks up to 100; out, it cools a degree every four ticks. Use it with an empty hand and no pie in to read its heat and the fire left.
4. **Baking:** put a raw pie in (one at a time). It bakes only while the oven is 50 degrees or hotter: a point a tick, two at 100. At 600 points (30 seconds, 15 at full heat) it is baked, and a chime rings; at 1,200 it burns, with a hiss.
5. **Taking it out:** an empty hand takes the pie out: still raw if it went too soon (put back in, it starts over), the pie if baked (**As Easy as Pie**), or a Burnt Pie if left too long. Broken, the oven drops its pie as it is.
6. You can watch it bake: the pie sits in the oven's mouth, pale dough going golden, then black as it burns, its filling showing through the vent. Comparators read it: 0 empty, 1 to 12 baking, 15 baked, 1 burnt.

### Pies
1. **Pastry Dough:** two wheat and an egg make two.
2. **Raw pies:** dough, two of a filling and sugar: Apple (two apples), Pumpkin Cream (a pumpkin and a milk bucket, which comes back), Cranberry (two cranberries), Sweet Potato (two sweet potatoes) and Chestnut (two roasted chestnuts). They stack to sixteen.
3. **A baked pie is placed** on a table like a cake, in its tin, under a lattice crust. Each pie is four slices: a hungry player eats a slice with an empty hand, or cuts one off with a Carving Knife to carry away. The last slice takes the tin. Only a whole pie can be picked up again.
4. **Slices:** Apple, Pumpkin Cream and Sweet Potato 4 hunger, Cranberry 3, Chestnut 5 (saturation 0.6, or 0.7 for sweet potato and chestnut). Comparators read the slices left.
5. **A Burnt Pie** is eaten the same way, but a slice is only 1 hunger, and one time in three gives Hunger ("that was very burnt"). It can't be cut.

### The Spirit Board
1. A lettered talking board of pale birch: YES by a sun and NO by a moon, two arcs of letters, the numbers and GOODBYE, inside an inked border; on it a walnut planchette shaped like a heart, gold-rimmed, with a glass lens (two birch slabs, an ink sac and a glass pane). It is read from the side it faces.
2. **Hold a séance:** light a candle within four blocks of it (any candle, an aura candle, a floating candle or a candle skull), then use it with an empty hand to rest your fingers on the planchette. You must be within three blocks of it ("sit closer to the board").
3. **Friends join in:** up to four players can put their fingers on the planchette. With two or more it moves faster: a stop every 12 ticks instead of 20.
4. **Who answers:** the nearest restless spirit within 16 blocks. The planchette slides to YES (everyone with fingers on it earns **Is Anybody There?**), spells the spirit's name and the one thing it wishes for, letter by letter, then goes to GOODBYE. Everyone within eight blocks sees the letters as they come ("The planchette spells: MABEL PIE"), and at the end what it means ("Mabel wishes for a pie, or a slice of one"). With no spirit near, it goes to NO and GOODBYE.
5. **What spirits wish for:** a pie (any pie or slice), a candle, a bottle of cider, a knitted sweater, a piece of candy, an apple (or a golden or caramel apple), a rose (a poppy or a rose bush), or a pumpkin (plain, carved or a jack o'lantern). A spirit keeps the name and wish it first gave; there are sixteen names.
6. **Hands lift** if a player wanders more than four blocks off; with no hands left the séance breaks off and the spirit slips away. After a séance the board rests for two seconds.
7. **Laying a spirit to rest:** reveal it (a Spirit Lantern, or a Revealing candle) and give it what it wished for. It takes one and rises away in a column of light: the giver gets 20 experience and Luck for five minutes, and earns **Unfinished Business**. Anything else, it turns away from.
8. Comparators read where the planchette is: 15 on YES, 1 on NO, 4 on GOODBYE, 8 on a letter, 0 at rest.

### Wild turkeys
1. **Wild Turkeys** live in flocks in forests, taiga, plains, sunflower plains and meadows (biome tag `jugcraft:turkey_habitat`). By day a flock of three to five, a tom among them, now and then wanders onto open grass 24 to 48 blocks from a player. No new flock comes while ten turkeys are within 64 blocks of the player, or forty are in the world; once come, they stay, like any farm animal.
2. **Toms and hens:** half are toms: bronze, barred in black, with a dark breast and beard, white-barred wings, a pale blue head, a red wattle and a snood over the beak, and a great chestnut tail. Hens are mottled brown with a grey-blue head and a shorter tail. Poults are buff down striped brown.
3. **Strutting:** a tom with an audience (a player or a grown hen within six blocks) now and then stops, raises his tail into a full fan, puffs out his breast, drops his wings to the ground and gobbles, for four seconds.
4. **Eggs:** a hen lays an egg (vanilla's) every five to ten minutes.
5. **Farming:** seeds (wheat, beetroot, melon, pumpkin, the Fall Farm's corn kernels, sunflower seeds, oat and barley seeds) and sweet berries tempt turkeys and breed them; breeding two earns **Gobble Gobble**. A poult grows up in twenty minutes. Turkeys flutter down rather than fall, and take no fall damage.
6. **Drops:** a raw turkey (3 hunger) and one to three feathers.
7. **Roast Turkey:** a raw turkey roasts in a furnace (10 seconds), a smoker (5) or on a campfire (30) into a Roast Turkey, golden on a silver platter with cranberries and herbs. Place it on a table:
   - a hungry player using it eats a serving (3 hunger, saturation 0.6);
   - a Carving Knife carves off a Slice of Roast Turkey to take away (the same food), earning **Carving the Bird**;
   - it has six servings: first the drumsticks go, then the breast is carved, then only the carcass is left; the last serving takes the platter and leaves a bone;
   - comparators read the servings left. Only a whole roast can be picked up again.

### The Theremin
1. A walnut cabinet on four slender legs, its lid edged in brass; a tall copper pitch antenna on a bakelite insulator at its player's right and a copper volume loop held out at the left; a speaker grille, two bakelite knobs and a magic-eye tube in front (two copper wires, two planks, a note block and two sticks). It faces whoever places it, as its player.
2. **Switch it on** with an empty hand ("The theremin hums into life: wave a hand at its antenna"), and off again; or a redstone signal plays it while it lasts. Playing, its magic eye glows green and it gives off a little light (7).
3. **Played without touching:** five times a second it listens for the nearest creature (a player or any mob; not a spectator or an armour stand) within eight blocks of its pitch antenna, and sings for them: a note block's flute that rises over two octaves as they come in from eight blocks off to the antenna, with a slow vibrato, louder the nearer they are. A note rises from the antenna in the colour of its pitch.
4. Playing it from within three blocks earns **Good Vibrations**.
5. **A proximity sensor:** comparators read how near the nearest creature is, playing or silent: 15 at the antenna, 8 at four blocks, 0 at eight and beyond. A silent theremin by a door can open it for whoever comes near; a playing one in a haunted house is sung by every mob that wanders past.

### The Día de Muertos ofrenda
Día de Muertos, the Day of the Dead, is a living tradition, above all of Mexico, of remembering family and friends who have died. It is not Halloween, though it falls just after it. Families build an ofrenda, a home altar, and set on it what the dead loved, with cempasúchil marigolds, whose colour and scent are said to guide them home, candles, bread and sugar skulls. This addition is meant as a warm, respectful remembrance, not a fright: it borrows the altar's look and its welcome, and nothing on it is spooky.
1. **Ofrenda** (three white carpets over six planks): a three-tier altar stepping up from the front under a white linen cloth, its risers embroidered with little flowers above a scalloped lace edge. It faces the player who places it.
2. **Offerings:** use an offering on it to set it out, one at a time, up to six: two to a tier, from the top tier down, standing on the cloth facing the front. An empty hand takes back the last.
3. **What may be offered:** the five kinds below, and keepsakes: a painting, a player's head (a likeness), any food (a favourite dish), papel picado, a poppy, or an orange or yellow mum.
4. **Complete:** with one of each of the five kinds, it glows softly (light 8) and marigold-gold motes drift up from it.
   - **Flowers:** a Cempasúchil Marigold, or Marigold Petals;
   - **Light:** any candle, or an aura candle;
   - **Bread:** Pan de Muerto, or bread;
   - **Sugar:** a Sugar Skull;
   - **A drink:** a potion or water bottle, a honey bottle, a milk bucket, or sweet or mulled cider.
5. **The welcome:** at night, every two seconds, a complete ofrenda welcomes the restless spirits within 16 blocks. Each makes the ofrenda its home and drifts there, shows itself to everyone (no Spirit Lantern needed) and no longer shies away, until it fades at dawn. When a welcomed spirit has come within three blocks of it, every player within eight blocks earns **Remembered**. A shown spirit can be given its wish (from the Spirit Board) and laid to rest.
6. **Cempasúchil Marigold:** a bright orange flower that grows wild in the mum patches of floral biomes and forests, and is planted, potted, and loved by bees. It makes orange dye, or four Marigold Petals to strew on the ground in a path to the ofrenda. In suspicious stew it gives Luck.
7. **Papel Picado** (three paper, a string, pink and orange dye makes six): a string of cut-paper flags, pink, orange and purple, each cut through with a flower and scallops, hung on a wall.
8. **Sugar Skull** (three sugar, an egg and light blue dye makes two): a small calavera of moulded white sugar, its eyes ringed with icing petals, a heart for a nose, a stitched smile and a flower on the brow. Placed, it faces you.
9. **Pan de Muerto** (three wheat, an egg and sugar makes two doughs; bake in a furnace, smoker or on a campfire): a round sweet loaf crossed with dough "bones" (6 hunger, saturation 0.7).

## Connections
- Candy kitchen, input producer: sugar cane (sugar); bottles of water and cows (milk); cocoa, sweet berries, glow berries and bees; the Festival Crops (cranberries, roasted chestnuts) and the cider mill (mulling spices); dyes; sticks; vanilla copper and iron; Jugcraft's `jugcraft:heat_sources`.
- Candy kitchen, output consumer:
  - Candy Bowls, Candy Caches and Candy Bags (`c:foods/candy`);
  - caramel for caramel apples (the Halloween harvest);
  - short effects for anyone (miners, explorers, fighters);
  - food for the Harvest Feast Table, which counts every kind of candy as a different food.
- Candy kitchen, technology connection: comparators read the stage, so redstone can watch a kettle.
- Candy kitchen, entry path: everything is early-game; no candy needs another candy, and the kettle needs no other Jugcraft block.
- Candy kitchen, required vs optional: optional and all year. One cook can make every candy alone.
- Candy kitchen, trade and solo routes: candy stacks (identical pieces) and says what it is, so a candy maker can sell to others.
- Autumn foraging, input producer: the world (forest, taiga, plains and spooky biomes), bone meal, and sugar cane and sticks for the basket.
- Autumn foraging, output consumer:
  - the four dishes, and through them the Harvest Feast Table (`c:foods`);
  - Luck for fishing and loot;
  - the Foraging Basket carries the Festival Crops' chestnuts and cranberries and the cider mill's apples too;
  - the jack o'lantern mushroom is a soft light for spooky builds.
- Autumn foraging, entry path: found from the first day; nothing needs another Jugcraft item.
- Autumn foraging, required vs optional: optional; nothing else needs a mushroom. A player who finds none can grow a patch from one by bone meal and shade, or trade for them.
- Autumn foraging, trade and solo routes: mushrooms and dishes stack and trade; a forager can plant fairy rings for others.
- Bat House, input producer: planks and a stick; vanilla bats (from caves, or moving in by themselves).
- Bat House, output consumer: farms (guano grows crops); phosphate for the existing recipes that use it (superphosphate fertilizer in the chemical reactor, silicon boules); composters.
- Bat House, technology connection: comparators read the bats; guano is a second source of phosphate, so a player with no phosphorite near can still make superphosphate.
- Bat House, entry path: two early items; no other Jugcraft block needed.
- Bat House, required vs optional: optional. Phosphate is still mined from phosphorite as before.
- Bat House, trade and solo routes: guano stacks and trades; anyone can hang houses alone.
- Hay Golem, input producer: wheat (hay bales); the carved pumpkins of the Halloween harvest and pumpkin carving.
- Hay Golem, output consumer: the crops it harvests (into a chest, barrel or hopper, and on into any storage or machine line); crows' working scarecrows (it guards as one does).
- Hay Golem, technology connection: a hopper under its post feeds the harvest into storage or machines.
- Hay Golem, entry path: early-game wheat and a carved pumpkin; no other Jugcraft block needed.
- Hay Golem, required vs optional: optional; farming by hand is unchanged.
- Hay Golem, trade and solo routes: anyone can build one alone; it harvests for whoever's farm it stands in.
- Knitting, input producer: wool (sheep, in any of the sixteen colours); string; iron nuggets; dyes.
- Knitting, output consumer: players (warmth in powder snow, Regeneration by a campfire); the Spinning Wheel takes knitwear back; dyed yarn and garments trade.
- Knitting, technology connection: the wheel is worked by a redstone pulse and emptied by a hopper, so a sheep farm can feed an automatic yarn line; comparators read its progress.
- Knitting, entry path: early items only; no other Jugcraft block needed.
- Knitting, required vs optional: optional; leather still keeps out the cold.
- Knitting, trade and solo routes: one player can shear, spin and knit alone; a knitter can sell sweaters in any colour, and needles part-way through a garment can be handed on.
- Pie baking, input producer: bricks (clay); a furnace; coal, charcoal, Jugcraft's coke, or logs; wheat, eggs and sugar; apples (vanilla and the cider mill's orchard), pumpkins and milk, the Festival Crops' cranberries, the Kitchen Garden's sweet potatoes, the chestnut trees' roasted chestnuts.
- Pie baking, output consumer: players (food); the Harvest Feast Table (each kind of slice is another food on the table); trade.
- Pie baking, technology connection: comparators read the oven (baked or burnt) and the pies (slices left), so a redstone line can call the baker; the oven burns the same coal and coke as the generators.
- Pie baking, entry path: early items only; no other Jugcraft block needed.
- Pie baking, required vs optional: optional; vanilla's pumpkin pie is unchanged.
- Pie baking, trade and solo routes: one player can bake alone; a pie is a trade good, and slices carry.
- Spirit Board, input producer: birch slabs, an ink sac, a glass pane; candles (vanilla, and the chandlery's aura candles, floating candles and candle skulls); ghost hunting's restless spirits, rising from the graveyard decorations' graves.
- Spirit Board, output consumer: the wishes send players to other additions (pies, knitted sweaters, the candy kitchen's candy, the cider mill's cider, caramel apples, candles, pumpkins); laying a spirit to rest gives experience and Luck (for fishing or opening loot).
- Spirit Board, technology connection: comparators read the planchette, so a séance can drive redstone (a door that opens on YES).
- Spirit Board, entry path: early items only; a séance needs a candle, and its answer needs a restless spirit near.
- Spirit Board, required vs optional: optional; ghost hunting is unchanged without it.
- Spirit Board, trade and solo routes: one player can hold a séance alone (slower); the things spirits wish for are trade goods.
- Wild turkeys, input producer: seeds and sweet berries (vanilla, and the Fall Farm's corn kernels, sunflower seeds, oat and barley seeds); fuel for roasting.
- Wild turkeys, output consumer: players (food); the Harvest Feast Table (a raw turkey and a slice are foods on it, `c:foods`); eggs for cakes and pie baking's pastry dough; feathers for arrows and books and quills; the roast's bone for bone meal; trade.
- Wild turkeys, technology connection: comparators read a roast's servings left.
- Wild turkeys, entry path: turkeys come to the woods and meadows by themselves; no Jugcraft block is needed. A Carving Knife (from pumpkin carving) is needed only to carve slices to carry.
- Wild turkeys, required vs optional: optional; chickens are unchanged.
- Wild turkeys, trade and solo routes: one player can keep and breed turkeys alone; a roast is a trade good, and slices carry.
- Theremin, input producer: copper wire from the machines branch's Wire Drawer; planks, a note block, sticks.
- Theremin, output consumer: redstone (a comparator reads how near the nearest creature is); players (music, and the haunted house and mad lab decorations' mood).
- Theremin, technology connection: it needs the Wire Drawer's copper wire, so it asks for the machines branch; it uses no power. As a sensor it reads distance, which a pressure plate or tripwire can't.
- Theremin, entry path: Mechanical tier, once a Wire Drawer runs; the recipe needs the machines feature as well as agriculture.
- Theremin, required vs optional: optional; nothing needs it.
- Theremin, trade and solo routes: one player can build and play it alone; copper wire and theremins trade.
- Ofrenda, input producer: planks and white carpet; marigolds (wild, or planted); wheat, eggs and sugar; candles (vanilla, and the chandlery's aura candles); drinks (vanilla, and the cider mill's sweet and mulled cider); paper, string and dyes; any food as a keepsake.
- Ofrenda, output consumer: ghost hunting (a complete ofrenda gathers and shows the restless spirits near, and the Spirit Board tells what each wishes for); decoration (papel picado, petals, sugar skulls, potted marigolds); orange dye; pan de muerto as food (`c:foods/bread`, and on the Harvest Feast Table).
- Ofrenda, technology connection: none; it is a decoration with a ghost-hunting use.
- Ofrenda, entry path: early items only; the drink and the light have vanilla choices.
- Ofrenda, required vs optional: optional; nothing needs it.
- Ofrenda, trade and solo routes: one player can set one up alone with vanilla bread, a candle and a water bottle; its offerings are trade goods, and a group can fill one with each other's work.

## Balance and automation
- **Candy kitchen:**
  - Costs: a kettle of seven copper ingots and a glass pane; a tray of five iron nuggets (reused). A batch is a water bottle or a milk bucket (both containers come back), one to four sugar, and up to two flavour items and any dyes, used once a batch.
  - Units: degrees Celsius; ticks (20 a second); pieces (two a sugar, eight at most a batch, 24 at most on a tray of three candy corn layers).
  - Heating from room temperature (20°C): 320 ticks to the boil, 120 more to 110°C, then 30 ticks to each 5°C stage (6 ticks a degree): about 22 seconds to thread, 33 to hard crack, 41 to burnt. Cooling: 8 ticks a degree.
  - Setting: 100 ticks; rock candy 24,000 ticks; taffy pulled 4 times, 20 ticks each, within 600 ticks.
  - Effects are short (4 to 30 seconds a piece) and the same as vanilla potions'; candy gives no effect stronger than level I.
  - Nothing is made from nothing: candy comes only from sugar, and candy makes nothing else (caramel goes into caramel apples, as before). No loop.
- **Autumn foraging:**
  - Costs: the basket is five sugar cane and a stick. A dish is one mushroom (Forager's Stew three and a potato). Bone meal, once a try.
  - Units: random ticks (vanilla's random tick speed); blocks; ticks for Luck (6,000, five minutes).
  - Spreading: one random tick in 25, at most five of a kind within four blocks, only below light 13 (bone meal in any light). A shady patch fills in over a few in-game days; it never covers a forest.
  - Worldgen: each mushroom's patch is placed once in 16 chunks of its biomes, 12 tries over a 9 by 9 area, on soil only.
  - Fairy rings: one random tick in 40 on a full-moon night (one night in eight). The blessing is once a night a player, Luck II for five minutes.
  - The food values sit with vanilla's cooked foods (a baked potato is 5, the stew is a mushroom stew and more).
  - Mushrooms only multiply in place, a few at a time, and nothing converts them into more of anything. No loop.
- **Bat House:**
  - Costs: seven planks and a stick. Guano costs nothing but time.
  - Units: bats (four a house at most), guano (one a bat a dawn, 16 waiting at most), blocks, ticks.
  - Yield: at most four guano a house a day. Four guano make one phosphate, so a full house gives at most one phosphate a day; a phosphorite ore drops one to three.
  - Fertilizer: one guano grows each crop in a 3x3 patch by one bone-meal dose, against superphosphate's 5x5 and two doses: a guano is about a fifth of a superphosphate.
  - New bats: an even chance a dusk for a house with room, only while mobs spawn.
  - No loop: guano and phosphate make nothing that makes bats or houses.
- **Hay Golem:**
  - Costs: four hay bales (36 wheat) and a carved pumpkin; shears give back all of it.
  - Units: blocks, ticks, items.
  - Work: at most one crop every few seconds (it walks to each and works at it half a second); villager farmers do the same kind of work.
  - It only replants from the crop's own drops: no seed is made from nothing, and a crop whose drops hold no seed isn't replanted.
  - No loop: a golem makes nothing that makes golems; its wheat harvest could build another, as wheat grown by hand could.
- **Knitting:**
  - Costs: the wheel three planks, three sticks and a string; the needles two iron nuggets and two sticks.
  - Units: balls of yarn, rows, ticks.
  - Yarn: a wool block spins into four balls in four turns. A beanie or socks take two balls (half a wool), a sweater five (a wool and a quarter).
  - Unravelling gives back a ball a row less one, so knitting and unravelling always loses yarn: no loop.
  - Cosiness: Regeneration I only, only near a lit campfire, and only while wearing two pieces; it doesn't stack with anything new.
  - Knitwear gives no armour, so it isn't a cheap alternative to leather in a fight.
- **Pie baking:**
  - Costs: the oven seven bricks and a furnace; a pie a pastry dough (a wheat and half an egg), two of a filling and a sugar, and its fire.
  - Units: degrees (0 to 100), baking points, ticks, slices, hunger.
  - Fuel: coal, charcoal and coke as long as generators burn them, a log as long as a furnace, so the oven is never a cheaper way to use fuel.
  - Food: a pie is four slices, 12 to 20 hunger in all, against its fillings' own food (two apples or two roasted chestnuts are 8, two cranberries 4). Baking adds to them, as vanilla cooking does, for the dough, sugar, fuel and the baker's watching; a burnt pie is nearly worthless.
  - No loop: a pie makes nothing that makes pies.
- **Spirit Board:**
  - Costs: two birch slabs, an ink sac and a glass pane; a lit candle near.
  - Units: stops (letters, YES, NO, GOODBYE), ticks, blocks.
  - Reward: 20 experience and five minutes of Luck for each spirit laid to rest, and the wished-for item is used up. Spirits rise only at night from graves, at most three near a grave.
  - No loop: nothing the board or a spirit gives makes spirits or boards.
- **Wild turkeys:**
  - Costs: seeds to breed them, fuel to roast.
  - Units: hunger points, saturation, ticks, blocks.
  - Food: a turkey is one raw turkey (3 hunger raw); roasted, six servings of 3, 18 hunger in all. That is more than cooked chicken's 6, for a bird with 8 health that comes one roast per bird and takes breeding to replace. A hen's eggs are vanilla's, at about a chicken's rate.
  - Spawning: a try every 20 seconds for each player, a quarter of the time, capped near players and in the world.
  - No loop: a turkey makes nothing that makes turkeys; seeds breed them.
- **Theremin:**
  - Costs: two copper wires (two thirds of a copper ingot through the Wire Drawer), two planks, a note block, two sticks. It uses no power and burns nothing.
  - Units: blocks (from the pitch antenna), ticks, pitch (0.5 to 2.0, a note block's range), comparator levels (0 to 15, one for each eight-fifteenths of a block).
  - No loop: it makes nothing.
- **Ofrenda:**
  - Costs: six planks and three white carpets; five offerings, which stay on it and can be taken back (nothing is used up).
  - Food: two loaves of pan de muerto, 12 hunger, from three wheat, an egg and sugar (bread is 5 from three wheat), paid for by the egg, the sugar and the baking.
  - Units: offerings, ticks, blocks.
  - Reward: only Remembered and the spirits' company; a shown spirit's wish is still the Spirit Board's reward.
  - No loop: an ofrenda makes nothing; the welcome calls only spirits already risen.

## Multiplayer and persistence
- **Candy kitchen, server authority:** filling, reading, tipping out and pouring all go through vanilla's block use path (reach, spawn protection, adventure mode) and are decided on the server, which checks the base, the sugar and flavour limits, and the temperature. Pulling and breaking up a tray go through vanilla's item use; the server checks the tray's own record of when it was poured and how often it has been pulled, by its own game time.
- **Candy kitchen, what clients get:** the kettle's batch and temperature, to draw the syrup and the needle; the tray's batch, to draw it. Clients decide nothing.
- **Candy kitchen, concurrent use:** two players at one kettle share its batch; whoever pours first gets it, and the second finds it empty.
- **Candy kitchen, persistence:** the kettle saves its base, sugar, flavours, dye sums, temperature and the hottest it has reached; a tray keeps its batch (a data component), so trays of candy keep across a restart and in chests, and rock candy keeps growing by game time.
- **Candy kitchen, bounded work:** a kettle does nothing unless it is warmer than the room or heating a batch; a degree changes at most every 4 ticks, and only then is it sent to clients.
- **Candy kitchen, IDs:** blocks/items `candy_kettle`, `candy_tray`, `rock_candy`, `salt_water_taffy`, `hard_candy`, `lollipop`, `fudge`, `cream_caramel`, `toffee`, `burnt_sugar`; block entity `candy_kettle`; data component `jugcraft:candy_batch`; item tags `jugcraft:candy_flavours/*`; advancements `candy_maker` and `taffy_puller`; the kettle's and tray's recipes. All new. Candy corn's item model now takes its band colours from the vanilla `custom_model_data` component (plain candy corn keeps its white, orange and yellow).
- **Candy kitchen, disable behaviour:** with the agriculture feature off, the kettle's and tray's recipes don't load; kettles, trays and candy already made still work, and everything stays registered.
- **Autumn foraging, server authority:** spreading, sprouting rings and blessing are the server's (random ticks and a server tick). Picking goes through vanilla's block breaking; the basket fill happens on the server, from the block's own drops. The blessing checks the player's own position on the server; nothing is asked of the client.
- **Autumn foraging, what clients get:** the blocks and the basket's contents (vanilla's bundle component); particles and sounds.
- **Autumn foraging, persistence:** mushrooms are blocks. Who was blessed tonight is kept in the server's memory only: a restart in the middle of a full-moon night lets players be blessed once more that night.
- **Autumn foraging, bounded work:** a random tick looks at the 243 blocks within four blocks and up to four spots. The server looks at each Overworld player every 20 ticks, only on full-moon nights, reading the blocks of the ring round them (the columns between 2.5 and 3.6 blocks off, three blocks high).
- **Autumn foraging, IDs:** blocks/items `chanterelle`, `porcini`, `puffball`, `fly_agaric`, `jack_o_lantern_mushroom`; items `foraging_basket`, `sauteed_chanterelles`, `roasted_porcini`, `fried_puffball`, `foragers_stew`; placed features `patch_<mushroom>`; block tags `jugcraft:mushroom_soil` and `jugcraft:wild_mushrooms`; item tags `jugcraft:forage` and `jugcraft:wild_mushrooms`; advancements `forager` and `fairy_ring`; their recipes. All new.
- **Autumn foraging, disable behaviour:** with the agriculture feature off, the recipes don't load; the mushrooms still generate and grow, the basket still works, and everything stays registered.
- **Bat House, server authority:** dusk, dawn and moving in are the server's, by the Overworld clock. Scooping goes through vanilla's block use path (reach, spawn protection, adventure mode) and is decided on the server.
- **Bat House, what clients get:** the block state (facing, how much guano shows), and the house's bats and guano for the status message; vanilla bats as ever.
- **Bat House, persistence:** the house saves its roosting bats, its guano and whether it last saw night, so a restart at dusk or dawn neither loses nor doubles the change. Bats out at night are ordinary bats, saved as vanilla saves them; a house doesn't keep track of which bats are its own.
- **Bat House, bounded work:** a house looks at the clock every 20 ticks. At dawn it looks for bats within 32 blocks once, through the entity lookup (it never loads a chunk), and takes at most four.
- **Bat House, IDs:** block and item `bat_house`, item `bat_guano`, block entity `bat_house`, entity tag `jugcraft.bat_house`, advancement `night_shift`, recipes `bat_house` and `phosphate_from_bat_guano`. All new. `FertilizerItem` now takes its area and doses (superphosphate's are as before).
- **Bat House, disable behaviour:** with the agriculture feature off, the recipes don't load and no bat moves in; bats still roost and leave guano, and everything stays registered. With the phosphate feature off, the phosphate recipe doesn't load.
- **Hay Golem, server authority:** building is checked on the server: the T of hay, a head with a face, the builder able to build there (adventure mode can't), through vanilla's block use path (reach, spawn protection). Its tending, guarding, healing and shearing are the server's.
- **Hay Golem, what clients get:** the golem, its head (an item stack, to draw), and whether it is bent over a crop.
- **Hay Golem, persistence:** the golem saves its head, its post and its pouch; it never despawns.
- **Hay Golem, bounded work:** it looks for a ripe crop at most every 100 ticks (and right after a harvest), over the circle of eight blocks round its post, five blocks high, in loaded chunks only. A crow looking for a guard asks for golems within twelve blocks through the entity lookup.
- **Hay Golem, IDs:** entity `hay_golem`, item tag `jugcraft:hay_golem_heads`, loot table `jugcraft:entities/hay_golem`, advancement `man_of_straw`. All new.
- **Hay Golem, disable behaviour:** with the agriculture feature off, no new golem can be built and golems don't harvest; they still stand and guard, and everything stays registered.
- **Knitting, server authority:** spinning, unravelling, knitting a row, unpicking and changing project happen on the server, through vanilla's block and item use paths (reach, spawn protection, adventure mode). The needles check for yarn in the other hand on the server.
- **Knitting, what clients get:** the wheel's skein colour and when it last turned (to draw it spinning); the needles' project and rows as a data component (for the tooltip).
- **Knitting, persistence:** the wheel saves its skein and turns; the needles' work is a data component on the item (`jugcraft:knitting`); garments and yarn carry their colour in vanilla's `dyed_color`.
- **Knitting, bounded work:** cosiness looks at each player every 40 ticks, and looks for a campfire (9 by 5 by 9 blocks) only round a player wearing two pieces or more.
- **Knitting, IDs:** block and item `spinning_wheel`, block entity `spinning_wheel`, items `yarn`, `knitting_needles`, `knit_beanie`, `wool_socks`, `knit_sweater`, `striped_sweater`, `pumpkin_sweater`, `bat_sweater`, `leaf_sweater`; data component `jugcraft:knitting`; equipment assets `knit`, `knit_striped`, `knit_pumpkin`, `knit_bat`, `knit_leaf`; item tag `jugcraft:knitwear`; recipes `spinning_wheel`, `knitting_needles` and a `<item>_dyed` recipe for the yarn and each garment; advancements `knit_one_purl_two`, `snug_as_a_bug`. All new. The garments and yarn join vanilla's `minecraft:freeze_immune_wearables` (garments) and `minecraft:cauldron_can_remove_dye`.
- **Knitting, disable behaviour:** with the agriculture feature off, the recipes don't load and no one is made cosy; the wheel and needles still work, and everything stays registered.
- **Pie baking, server authority:** feeding, putting in, taking out, eating and cutting go through vanilla's block use paths (reach, spawn protection, adventure mode) and are decided on the server; the fire, heat and baking run on the server.
- **Pie baking, what clients get:** the oven's facing and whether it is lit (block state); its pie and how far it is baked, sent as it passes each tenth, to draw it; the pies' slices (block state).
- **Pie baking, persistence:** the oven saves its fire, heat, pie and baking points; a pie's slices are its block state.
- **Pie baking, bounded work:** an oven's tick is a few counters, and it sends an update only when its fire goes out or lights, a pie goes in or out, or the pie passes another tenth of its baking.
- **Pie baking, IDs:** blocks and items `hearth_oven` (block entity too), `apple_pie`, `pumpkin_cream_pie`, `cranberry_pie`, `sweet_potato_pie`, `chestnut_pie`, `burnt_pie`; items `pastry_dough`, `raw_<filling>_pie` and `<filling>_pie_slice` for each filling; item tag `jugcraft:hearth_oven_wood`; recipes `hearth_oven`, `pastry_dough`, `raw_<filling>_pie`; advancement `as_easy_as_pie`. All new.
- **Pie baking, disable behaviour:** with the agriculture feature off, the recipes don't load; ovens still bake and pies are still eaten, and everything stays registered.
- **Spirit Board, server authority:** who may put their fingers on the planchette (distance, a lit candle, room for more hands), which spirit answers and what it spells, and laying a spirit to rest are all the server's. Use goes through vanilla's block and entity use paths (reach, spawn protection, adventure mode). Clients are told only where the planchette is going.
- **Spirit Board, what clients get:** the planchette's last stop, its next and when it set off (to slide it there), and whether a séance is going on (to circle it); the letters as overlay messages to the players near.
- **Spirit Board, persistence:** the board saves the hands on it, what it is spelling and how far it has got, and its rest; a spirit saves its name and wish.
- **Spirit Board, bounded work:** the board does nothing between séances. Starting one looks at 9 by 9 by 9 blocks for a candle and asks the entity lookup for spirits within 16 blocks, once; each tick of a séance checks its hands (four at most).
- **Spirit Board, IDs:** block and item `spirit_board`, block entity `spirit_board`; block tag `jugcraft:seance_candles`; item tags `jugcraft:spirit_wishes/<wish>` for pie, candle, cider, sweater, candy, apple, rose and pumpkin; recipe `spirit_board`; advancements `is_anybody_there` and `unfinished_business`. All new. A restless spirit's save gains `spirit_name` and `wish`.
- **Spirit Board, disable behaviour:** with the agriculture feature off, the recipe doesn't load and no spirits rise to answer; the board stays registered.
- **Wild turkeys, server authority:** spawning, strutting, laying, breeding and every serving are the server's. Feeding and breeding go through vanilla's animal interaction; eating and carving through vanilla's block use path (reach, spawn protection, adventure mode).
- **Wild turkeys, what clients get:** whether a turkey is a tom and how long he has left to strut (synced entity data), from which the client fans the tail; the roast's servings are its block state.
- **Wild turkeys, persistence:** a turkey saves whether it is a tom and a hen's time to her next egg, besides vanilla's animal data (age, love).
- **Wild turkeys, bounded work:** spawning runs once every 400 ticks: it counts the world's turkeys once, then for a quarter of the players counts the turkeys within 64 blocks and reads one spot's height, block and biome, in a loaded chunk only; it never loads a chunk. A tom looks for an audience only on the tick he might strut (one in 200).
- **Wild turkeys, IDs:** entity `turkey`; block and item `roast_turkey`; items `raw_turkey`, `turkey_slice`; item tag `jugcraft:turkey_food`; biome tag `jugcraft:turkey_habitat`; recipes `roast_turkey`, `roast_turkey_from_smoking`, `roast_turkey_from_campfire_cooking`; loot tables `entities/turkey`, `blocks/roast_turkey`; advancements `gobble_gobble`, `carving_the_bird`. All new. The raw turkey joins `c:foods/raw_meat`, the slice `c:foods/cooked_meat`, and both `c:foods`.
- **Wild turkeys, disable behaviour:** with the agriculture feature off, no flocks come and the roasting recipes don't load; turkeys already in the world stay, and everything stays registered.
- **Theremin, server authority:** switching it on goes through vanilla's block use path (reach, spawn protection, adventure mode); redstone, listening, the comparator, the notes and Good Vibrations are all decided on the server.
- **Theremin, what clients get:** its block state (on, powered: the glowing eye and the light), and the notes and particles the server sends to players near. The notes are in the jukebox and note block sound category, so players can turn them down.
- **Theremin, persistence:** its block state saves whether it is on or powered; the block entity saves its last comparator reading.
- **Theremin, bounded work:** every four ticks, one entity lookup in a 16-block box round the antenna; while playing with someone near, one sound and one particle.
- **Theremin, IDs:** block and item `theremin`, block entity `theremin`; recipe `theremin`; advancement `good_vibrations`. All new.
- **Theremin, disable behaviour:** with the agriculture or machines feature off, the recipe doesn't load; theremins already built still play and sense, and everything stays registered.
- **Ofrenda, server authority:** setting out and taking back offerings go through vanilla's block use path (reach, spawn protection, adventure mode); the server checks the offerings tag and the six places, and decides completeness, the welcome and Remembered.
- **Ofrenda, what clients get:** the offerings on it (to draw them) and whether it is complete (its block state: the glow).
- **Ofrenda, persistence:** the ofrenda saves its offerings (at most six are read back); a welcomed spirit saves that it was welcomed, and its home.
- **Ofrenda, bounded work:** every 40 ticks, a complete ofrenda at night asks the entity lookup for spirits within 16 blocks, once, and looks through the players only if one has arrived.
- **Ofrenda, IDs:** blocks and items `ofrenda` (block entity too), `marigold`, `marigold_petals`, `papel_picado`, `sugar_skull`; block `potted_marigold`; items `pan_de_muerto_dough`, `pan_de_muerto`; item tags `jugcraft:ofrenda/offerings`, `jugcraft:ofrenda/flowers`, `…/light`, `…/bread`, `…/sugar`, `…/drink`; recipes `ofrenda`, `papel_picado`, `sugar_skull`, `marigold_petals`, `orange_dye_from_marigold`, `pan_de_muerto_dough`, `pan_de_muerto` (and from smoking and campfire cooking); advancement `remembered`. All new. A restless spirit's save gains `welcomed`. The marigold joins `minecraft:small_flowers`, `flower_pots` and `bee_attractive`, and pan de muerto `c:foods/bread`.
- **Ofrenda, disable behaviour:** with the agriculture feature off, the recipes don't load and spirits fade; ofrendas keep their offerings, and everything stays registered.

## Dependencies and assets
Candy kitchen:
- No new dependencies. Textures are drawn by code in `tools/candy_textures.py`: the kettle's polished copper and dark inside, its thermometer dial (the stages painted round it in their colours, a tick where each starts), the syrup's surface and the needle for the renderer, the tray empty and the candy on it, and each candy, drawn pale where it takes its colour, with its stick or wrapper in a layer of its own; candy corn in three bands and an outline.
- The models, blockstates, item models (each candy tinted by the vanilla `dyed_color` component; candy corn's bands by `custom_model_data`), names, tooltips, messages, loot and tags come from `tools/candy_data.py`; the numbers from `CANDY` in `tools/agriculture.py`.
- The client's `CandyKettleRenderer` draws the batch at the height of its sugar, browning past hard crack and black once burnt, and the needle on the dial.
- Sounds are vanilla's (a note block's bell at each stage, fire going out when it burns, sand, a bottle, a bucket, dye, honey, amethyst breaking for hard candy).

Autumn foraging:
- No new dependencies. Textures are drawn by code in `tools/foraging_textures.py`: each mushroom's cross-shaped plant (gills and spots, a cluster for the jack o'lantern mushroom), the basket (a red cloth and a chanterelle peeking out), and the dishes.
- Models, item models, names, loot, recipes, tags, worldgen and advancements come from `tools/foraging_data.py`; the numbers and biomes from `FORAGING` in `tools/agriculture.py`. Each mushroom's patch is added to its biomes by Fabric's biome modification API.
- Sounds and particles are vanilla's (wool breaking and white ash for a bursting puffball, an amethyst chime and end-rod sparkles at a fairy ring).

Bat House:
- No new dependencies. Textures are drawn by code in `tools/bat_textures.py`: weathered planks, the slatted front, the guano on the tray, and the guano item.
- Models (the house and its tray with one, two or three piles), blockstates, names, messages, loot and tags come from `tools/bat_data.py`; the numbers from `BATS` in `tools/agriculture.py`. The recipes are in its shaped and shapeless tables.
- Sounds and particles are vanilla's (a bat taking off and smoke at dusk).

Hay Golem:
- No new dependencies. Its texture is drawn by code in `tools/hay_golem_textures.py`: golden straw running along each bundle, twine bands round the body, wrists and ankles, a sackcloth patch on the chest, and loose straw at the collar, wrists and ankles.
- The client's `HayGolemModel` is a hay-bale body with a straw collar, straw-bundle arms and legs with tufts at the ends; it walks with a stiff swing, sways standing, and bends with its arms down to harvest. `HayGolemRenderer` draws the head it wears on the collar: the pumpkin's own item model, and a hand-carved pumpkin's carving over it (lit at full brightness), as the scarecrow draws its head.
- Its name, loot, head tag and advancement come from `tools/hay_golem_data.py`; the numbers from `HAY_GOLEM` in `tools/agriculture.py`.
- Sounds are vanilla's (grass for its steps, hurt and death; shearing; hay placed when it is built).

Knitting:
- No new dependencies. Textures are drawn by code in `tools/knitting_textures.py`: the wheel's wood, its felloes and spokes, the wool on the distaff and the yarn on the bobbin; the yarn, needles and garments as items (drawn pale, to take their colour, a sweater's pattern in a layer of its own); and the knit worn on the player, with each sweater's pattern.
- Models, blockstates, item models (tinted by vanilla's `dyed_color`), equipment assets (the dyeable knit, a pattern over it), names, messages, tooltips, loot, tags and the dyeing recipes come from `tools/knitting_data.py`; the numbers from `KNITTING` in `tools/agriculture.py`. The recipe audit in `tools/check_mod_data.py` knows 26.3's dyeing recipe type.
- The client's `SpinningWheelRenderer` draws the wheel turning while it spins, the skein on the distaff and the yarn growing on the bobbin.
- Sounds are vanilla's (wool as it spins, is knitted and unravels, a bundle as the yarn is set out, leather armour as a garment is finished).

Pie baking:
- No new dependencies. Textures are drawn by code in `tools/pie_textures.py`: the oven's bricks and mortar, soot, embers and stone hearth; each pie's lattice top and its filling where cut, the fluted crust and the tin, a burnt pie; the pale crust the renderer tints; the raw pies, slices and dough as items.
- Models (the oven lit and unlit; each pie whole and with one, two and three slices gone), blockstates, item models, names, messages, loot (a pie only while whole) and tags come from `tools/pie_data.py`; the numbers from `PIES` in `tools/agriculture.py`.
- The client's `HearthOvenRenderer` draws the pie in the oven's mouth, its crust dough-pale, golden or black as it bakes.
- Sounds are vanilla's (a fire charge as fuel goes in, wood for a pie in and out, a note block's chime when it is baked, fire going out when it burns, eating).

Spirit Board:
- No new dependencies. Textures are drawn by code in `tools/spirit_board_textures.py`: the board's lettered face (64 by 48: YES, NO, two arcs of letters, the numbers and GOODBYE in a three-by-five letter of its own, a sun and moon, a double border), the walnut planchette with its gold rim and glass lens, its wood, the board's birch top and edge, and the item.
- The model, blockstate, words (the messages and what each wish is called), loot and tags come from `tools/spirit_board_data.py`; the numbers, names and wishes from `SPIRIT_BOARD` in `tools/agriculture.py`. The checker matches every letter's place on the face against `SpiritBoard.place`.
- The client's `SpiritBoardRenderer` lays the face over the board and slides the planchette from stop to stop, easing in and out, swivelling towards the board's ends, and circling slowly on a letter while fingers are on it.
- Sounds are vanilla's (an amethyst hum as a séance starts, a wooden tap at each stop, an amethyst chime as a spirit is laid to rest).

Wild turkeys:
- No new dependencies. Textures are drawn by code in `tools/turkey_textures.py`: the tom, hen and poult (64 by 64, laid out for the client's `TurkeyModel`), the roast turkey (crisp golden skin, carved meat, bone, a silver platter, cranberries and herbs), and the raw turkey and slice as items.
- The roast's models (whole; the drumsticks gone; the breast carved; the carcass), blockstate, words, loot and tags come from `tools/turkey_data.py`; the numbers from `TURKEYS` in `tools/agriculture.py`. The checker matches each of the model's boxes against where the textures paint it.
- The client's `TurkeyModel` and `TurkeyRenderer` draw a deep round body and full breast, folded wings, long legs with spread toes, a bare neck and small head with beak, wattle and (on a tom) the snood, and a tail of feathers. Walking, the legs stride and the head bobs; strutting, a tom's tail rises and spreads into a fan, his breast puffs out, his wings droop and his neck draws back. Poults are drawn at half size.
- Sounds are vanilla's (a parrot's call pitched low for the gobble and warble, wool for a laid egg, grass for steps, eating, honey sliding as a slice is carved).

Theremin:
- No new dependencies. Textures are drawn by code in `tools/theremin_textures.py`: figured walnut, the front's speaker grille of woven cloth behind walnut bars over a brass inlay, polished brass, copper, black bakelite, and the magic-eye tube dark and lit.
- The models (silent, and playing with the eye lit and glowing in the dark), blockstate, words and loot come from `tools/theremin_data.py`; the numbers from `THEREMIN` in `tools/agriculture.py`.
- Sounds are vanilla's (a note block's flute, bent in pitch; a lever's click as it is switched).

Ofrenda:
- No new dependencies. Textures are drawn by code in `tools/ofrenda_textures.py`: the altar's linen and its embroidered, lace-edged riser fronts, scattered marigold petals, papel picado's three cut-paper flags, the sugar skull's iced face and sugar sides, and pan de muerto and its dough. The marigold is drawn with the mums.
- The models, blockstates, words, loot and tags come from `tools/ofrenda_data.py`; the numbers and the five kinds from `OFRENDA` in `tools/agriculture.py`; the marigold is one of `MUMS`.
- The client's `OfrendaRenderer` stands the offerings on the tiers, two to a tier, in the order they were set out.
- Sounds are vanilla's (a decorated pot's insert as an offering is set out, an item frame's as one is taken back).

## Verification
### Candy kitchen verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-25` stacked on #33:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the candy kitchen with Java: the batch limits, temperatures and heating rates; the tray's setting, pulling, crystal and layer times; the eating time and candy corn's bands; each stage and where it starts; which candy each base sets into at each stage; each kind's colour; each flavour's effect, time and colour; the candies' food; and checks every candy, stage, base and flavour has its words, textures and tags, and the recipes and advancements exist) | Pass, 631 IDs |
| `./gradlew build` on `fdbbbae` (Build workflow run 37067878969) | Pass |
| Game tests on the headless server, same run: 396 in total, 7 of them new here (`CandyGameTests`) | **All 396 pass**. They also all passed on `f4235ef` (run 37066473716) |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `fdbbbae` (run 37067878969), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-candy-kitchen) |

The 7 new game tests (`CandyGameTests`):
1. a water bottle sets the base and leaves its bottle, and milk is then refused; sugar goes in up to four, two flavours at most and each once, and dyes mix; near the boil nothing more goes in; sneaking with an empty hand tips it all out;
2. over a fire a batch heats a degree every four ticks below the boil, and a kettle with sugar but no base doesn't; off the heat it cools but keeps the stage it reached, which comparators read;
3. syrup at soft ball pours candy corn and cream fudge; cream at thread is too runny to pour; syrup past 175°C is burnt; pouring fills one tray (two pieces a sugar, with the batch's flavours and colour), empties the kettle and earns Sweet Science;
4. a tray breaks up into its named, flavoured pieces once set, and not while still setting; hard candy with sticks in the other hand makes a lollipop a stick; rock candy takes a day to grow;
5. warm taffy pulled four times earns Pulling Power and cuts into taffy; left to go cold unpulled, it sets hard, as hard candy;
6. candy corn takes up to three layers, each its own colour; three undyed layers make plain candy corn, which stacks with any other, and a dyed one keeps its bands;
7. flavoured candy is named for its flavours and gives their effects when eaten, even on a full stomach; burnt sugar keeps no flavour; the candies count as candy, the flavours' items are tagged, and the recipes load.

Found by CI and fixed before this record:
- 26.3's `LivingEntity.drop` takes different arguments: a tray's spare pieces now go to the ground with `Block.popResource` (`d1f4304`).
- 26.3 has no `Items` field for each dye: the tests look the dyes up by ID (`1ac7690`).
- In 26.3 every item has a name and lore component by default, so two assertions that a piece had its own name were always true; they now compare with a plain stack (`f4235ef`).
- The first screenshots showed the kettles floating over their campfires with no syrup in them: the needle's turn leaked into the syrup's pose. The needle is now drawn in its own pose, and the kettle stands on four iron trivet legs (`fdbbbae`).

Not run: a two-client dedicated-server playtest, and any play by hand.

### Autumn foraging verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-26` stacked on the candy kitchen:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares autumn foraging with Java: spreading, the fairy ring's size, chance, check interval and blessing; checks the mushrooms (the jack o'lantern mushroom's light), the basket and the foods are registered, every mushroom has its texture, words, loot and worldgen, the tags hold the mushrooms and the forage, the cooked foods have their recipes, and the two advancements exist) | Pass, 641 IDs |
| `./gradlew build` on `2e14ff6` (Build workflow run 37069783476) | Pass |
| Game tests on the headless server, same run: 400 in total, 4 of them new here (`ForagingGameTests`) | **All 400 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `c42f88d` (run 37067986302) and `2e14ff6` (run 37069783476), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#autumn-foraging) from the first |

The 4 new game tests (`ForagingGameTests`):
1. a porcini stands on grass but not on stone; spreading (as bone meal makes it) it puts out another of its kind, one at a time, until five are near, and no more; the jack o'lantern mushroom glows (light 9) and the others don't;
2. seven fly agarics round a circle are no ring and eight are; a player at the centre is blessed with Luck II for five minutes and earns Away with the Fairies, once a night; rings bless on full-moon nights only; one chanterelle sprouts a ring of its kind round it;
3. the basket refuses stone and takes berries; each of the five mushrooms picked with the basket in the other hand goes straight into it; all five earn Forager;
4. the dishes', basket's and stew's recipes and every mushroom's patch load.

Found by CI and fixed before this record:
- 26.3's `playerDestroy` takes a `ServerLevel` and a `ServerPlayer` (`d9d3786`, `a3efb90`).
- 26.3's `minecraft:dirt` block tag doesn't include grass, so a mushroom couldn't stand on a grass block: wild mushrooms now grow on a tag of their own, `jugcraft:mushroom_soil`, which their patches use too (`c42f88d`).
- `main`'s `PetroGameTests.heliostatsHeatASolarReceiver` failed twice on this branch's `c42f88d` (run 37067986302 and its re-run: "The receiver made 48 JE/t, expected 36 on tick 25"). The solar receiver counts its heliostats on its first tick, before the test's stone roof has cut off the sky light, and again only when the game time is a multiple of 100, so the test passes or fails by the game time it starts at. `main` has already fixed the test (it gives the receiver water only after its next count); this branch carries the same change (`f23f040`), and the test passes since.

Not run: a two-client dedicated-server playtest, and any play by hand.


### Bat House verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-27` stacked on autumn foraging:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the Bat House with Java: its room, guano cap, return range, moving-in chance, check interval, the bats' tag, and guano's area and doses; checks the house and guano are registered (guano as a fertilizer), the house has a model for each guano level, its words, loot, recipe and advancement, and phosphate takes four guano) | Pass, 643 IDs |
| `./gradlew build` on `b1a7915` (Build workflow run 37071059060) | Pass |
| Game tests on the headless server, same run: 402 in total, 2 of them new here (`BatHouseGameTests`) | **All 402 pass**. They also all passed on `2217bd2` (run 37068610115) |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `b1a7915` (run 37071059060), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-bat-house), and on `2217bd2` (run 37068610115) |

The 2 new game tests (`BatHouseGameTests`):
1. at dusk a house's three roosting bats fly out, tagged as its bats, and a player near earns Night Shift; at dawn the four nearest bats come in, wild ones too, two are left outside, each leaves a guano and the tray shows it; a full house reads 15 on a comparator; an empty house gains bats at dusk while mobs spawn;
2. an empty hand scoops up the house's seven guano; one guano grows the nine crops of a 3x3 patch and none beyond it; the house's and phosphate's recipes load.

Found by CI and fixed before this record:
- 26.3's `BlockPos` has no `getCenter`: the house finds the nearest bats with `Vec3.atCenterOf` (`2217bd2`).
- The first screenshots showed dark houses on a dark wall with the guano hard to see; the wall is now pale birch and a second shot looks down into the trays (`b1a7915`). The guano piles are still small at this size.

Not run: a two-client dedicated-server playtest, and any play by hand.

### Hay Golem verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-28` stacked on the Bat House:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the Hay Golem with Java: health, speed, post radius, search height, tending, work, give-up and idle times, pouch, carry, wheat's healing, fire, hay bales, reach and leading range; checks it is registered with its size, attributes and building callback, scarecrows count golems, it is named, drops wheat, has its head tag, texture and advancement, and its model's boxes sit where its texture paints them) | Pass, 643 IDs (the golem is an entity, not an item or block) |
| `./gradlew build` on `709a0c2` (Build workflow run 37071260396) | Pass |
| Game tests on the headless server, same run: 408 in total, 6 of them new here (`HayGolemGameTests`) | **All 408 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `709a0c2` (run 37071260396), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-hay-golem) |

The 6 new game tests (`HayGolemGameTests`):
1. carved pumpkins and jack o'lanterns are heads, a plain pumpkin and an uncut hand-carved one are not; one arm short, or with stone under an arm, the hay stays hay; a carved head on a full T makes one golem where the legs stood, facing its builder, the hay gone and the head used, and the builder earns Man of Straw;
2. a golem in a carved pumpkin guards seven blocks off (eight at most), twelve in a jack o'lantern, four headless (and not six); a crow won't raid ripe wheat a golem guards;
3. four ripe carrots harvested are each replanted from a carrot, the rest in its pouch; unripe carrots are left; unloading puts the carrots in the chest under its post, or with no chest sets them on the ground; the pouch holds nine stacks and no more;
4. with its own AI a golem walks five blocks to ripe carrots and harvests and replants them;
5. fire hurts it double; a wheat heals it four; shears take it apart into four hay bales, its jack o'lantern and its carrots;
6. killed by a player it drops two to five wheat and its head; its loot table, head tag and advancement load.

Found by CI and fixed before this record:
- 26.3's `Entity.invulnerableTime` is private, so the damage test couldn't reset it between hits; the test now burns the golem first and heals it after (`709a0c2`). The golem's own code compiled first time.

Not run: a two-client dedicated-server playtest, and any play by hand. The golem's walking and harvesting were seen only in the game test, on a flat stone floor.

### Knitting verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-29` stacked on the Hay Golem:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares knitting with Java: the needles' wear and row time, the wheel's turns and spin, yarn per wool, the unravelling loss, the undyed colour and cosiness; checks the registrations, each garment's slot, rows, item model, equipment asset and tags, the yarn's and every garment's dyeing recipe and cauldron tag, the recipes and advancements; and the recipe audit reads 26.3's `minecraft:crafting_dye` recipes) | Pass, 653 IDs |
| `./gradlew build` on `26b570e` (Build workflow run 37077362634) | Pass |
| Game tests on the headless server, same run: 413 in total, 5 of them new here (`KnittingGameTests`) | **All 413 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `26b570e` (run 37077362634), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#knitting) |

The 5 new game tests (`KnittingGameTests`):
1. with no wool the wheel doesn't turn; red wool goes on the distaff (a second skein doesn't); four turns spin it into four balls of red yarn set out in front of the wheel; a redstone pulse works the treadle as a hand does;
2. a pumpkin sweater used on the wheel unravels into four balls of its yarn (five rows, less one);
3. sneaking changes the needles' project (beanie, then socks); each row uses a ball of yarn; a beanie of one red row and one white comes out their blend, earning Knit One, Purl Two, and the needles take a point of wear; unpicking a row gives its yarn back;
4. a leather helmet and red dye make vanilla's dyeing recipe; the yarn and every garment take red dye by a recipe of the same kind and come out dyed; a water cauldron washes a red sweater back to undyed;
5. each garment is worn in its slot and is freeze-immune; one piece, or an unlit campfire, doesn't make a player cosy; two pieces by a lit campfire give Regeneration; a beanie, sweater and socks earn Snug as a Bug; the recipes load.

Found by CI and fixed before this record:
- Minecraft 26.3 doesn't dye leather by the `minecraft:dyeable` tag: leather isn't in it. A test that reported leather's tags and components, then the recipe that dyes it, showed a data-driven recipe per item instead (`minecraft:leather_helmet_dyed`: a `target`, any `#minecraft:dyes`, the item back). The yarn and each garment now have one of the same kind, and join `minecraft:cauldron_can_remove_dye` (`75ca09d`).
- 26.3's `Items` has no dye fields, so the tests look `red_dye` up by ID (`ff5669e`).
- The first screenshots had campfire smoke in front of the armour stands and the stands turned away; the scene was rearranged (`93a3a3b`, `26b570e`).

Not run: a two-client dedicated-server playtest, and any play by hand.

### Pie baking verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-30` stacked on knitting:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares pie baking with Java: the oven's fuel bank, a log's burn and the wood tag, heat, heating and cooling, baking heat, baked and burnt points and light; the pie's slices and a burnt slice's food and chance of Hunger; each filling's slice food and colour, in order; checks the registrations, every pie's models for each slice gone, its words, textures and loot (only while whole), the raw pies' recipes and the advancement) | Pass, 671 IDs |
| `./gradlew build` on `e5847b6` (Build workflow run 37075993468) | Pass |
| Game tests on the headless server, same run: 417 in total, 4 of them new here (`PieGameTests`) | **All 417 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `e5847b6` (run 37075993468), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#pie-baking) |

The 4 new game tests (`PieGameTests`):
1. coal banks a generator's 1,600 ticks of fire, twice, but not a third time past the 3,200 bank; a log burns 300 ticks and dirt doesn't burn; a raw pie goes in (not a second) and, taken out at once, comes back raw; out and below 50 degrees a pie doesn't bake; burning, the oven is lit;
2. at full heat a pie bakes two points a tick: in time it is baked, a comparator reads 15, and it comes out as the pie, earning As Easy as Pie; left in too long it burns and comes out burnt;
3. a hungry player eats a slice (four food for apple), a carving knife cuts one to take away, and the last slice takes the pie; comparators read the slices left; a whole pie drops itself and a cut one doesn't; a burnt pie's slice is one food;
4. the oven's, the pastry's and the raw pies' recipes load, and the slices are foods.

Found by CI and fixed before this record:
- 26.3's `Level` has no `fuelValues()`, so the oven can't read vanilla's furnace fuel list. It burns what Jugcraft's generators burn, as long (`GeneratorFuels`: coal, charcoal, coke), and logs (`jugcraft:hearth_oven_wood`, `#minecraft:logs_that_burn`) for 300 ticks, as a furnace does (`2a91810`). A lava bucket and the other furnace fuels aren't oven fuel.

Not run: a two-client dedicated-server playtest, and any play by hand.

### Spirit Board verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-31` stacked on pie baking:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the Spirit Board with Java: the candle, spirit, hand and watching ranges, the most hands, the two letter times, the rest after a séance, a spirit's reward, the face's size, the names and the wishes in order; checks that YES, NO, GOODBYE and every letter and number sit on the face where the texture draws them, the registration, the face and planchette textures at their sizes, words, tags, recipe, loot and advancements) | Pass, 672 IDs |
| `./gradlew build` on `acbf7c9` (Build workflow run 37077527188) | Pass |
| Game tests on the headless server, same run: 423 in total, 6 of them new here (`SpiritBoardGameTests`) | **All 423 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `acbf7c9` (run 37077527188), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-spirit-board) |

The 6 new game tests (`SpiritBoardGameTests`):
1. with no candle, or an unlit one, no séance starts; a lit candle two blocks off lets one start; a player too far off can't touch the planchette; four pairs of hands fit, not a fifth;
2. with no spirit near, the planchette goes to NO (a comparator reads 1), then GOODBYE, a stop every 20 ticks under one pair of hands, and rests, having spelled "NO … GOODBYE" (if another test's spirit happens to be near, it answers instead and the test checks for YES);
3. with a spirit near, named Mabel and wishing for a pie, and two pairs of hands: YES (a comparator reads 15, and both players earn Is Anybody There?), then "MABEL", "PIE" and GOODBYE, a stop every 12 ticks; then the board rests and won't start again at once;
4. a player who walks away lifts their hands, and with none left the séance breaks off;
5. a revealed spirit wishing for an apple turns from a poppy; given an apple it takes one and is laid to rest: 20 experience, Luck and Unfinished Business;
6. the wishes' tags hold what they should (a pie slice is a pie, wool socks aren't a sweater), candles and aura candles light a séance, and the recipe, loot table and advancements load.

Found by CI and fixed before this record:
- 26.3 has no `PushReaction.POPPED`; the board pops off when pushed (`POPPED`), as the pies do (`292af1a`).
- The first screenshots were too far off to read the letters; the cameras moved closer (`acbf7c9`).

Not run: a two-client dedicated-server playtest, and any play by hand. A séance has been seen only in the game tests, with mock players and posed spirits.

### Wild turkeys verification

Actual results (3 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-32` stacked on the Spirit Board, with main merged in (the agriculture branch as merged, #116 and #118):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares wild turkeys with Java: health, speed, the strut's range, length and chance, the hens' laying times, the spawning's interval, chance, distances, flock size and caps, and the roast's servings and their food; checks the turkey's registration, size, attributes and spawning, its name and loot, the food and habitat tags, its textures, the roast's models, cooking and loot, the advancements, and that the model lays its boxes where the textures paint them) | Pass, 991 IDs |
| `./gradlew build` on `62af9ff` (Build workflow run 37082602698) | Pass |
| Game tests on the headless server, same run: 541 in total, 6 of them new here (`TurkeyGameTests`) | **All 541 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `62af9ff` (run 37082602698), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#wild-turkeys) |

The 6 new game tests (`TurkeyGameTests`):
1. a tom and a hen, each fed wheat seeds by the same player, fall in love and breed a poult, and the player earns Gobble Gobble;
2. a hen doesn't strut; a player three blocks off is an audience for a tom, who struts, and the strut ends after 80 ticks;
3. when her time comes a hen lays an egg (vanilla's) and starts on the next; a tom lays none;
4. a flock comes as three to five turkeys with a tom among them, and never to bare stone;
5. a whole roast turkey drops itself; a hungry player eats a serving (3 hunger) and a carved one drops nothing; a Carving Knife carves a slice off (Carving the Bird); the last serving takes the platter and leaves a bone;
6. seeds and corn kernels tempt turkeys and wheat doesn't; plains and forests are turkey country and deserts aren't; the three roasting recipes, the loot tables and the advancements load.

Found by CI and fixed before this record:
- 26.3's `SoundEvents` has no chicken sounds (they belong to each chicken variant); turkeys use a parrot's call pitched low, wool for a laid egg and grass for their steps (`fb07cf7`).
- The first screenshots showed the turkeys from behind (a mob's yaw 0 faces south); they were turned to face the camera (`68af67e`).
- Main's client tests now run in three parallel jobs, and only the first job's opening test hides the HUD; in another job the first screenshots showed the hotbar and chat. The test now hides them itself (`62af9ff`).

Not run: a two-client dedicated-server playtest, and any play by hand. Turkeys coming to a player's woods by themselves haven't been watched; the tests call the flock spawning directly.

### Theremin verification

Actual results (3 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-33` stacked on wild turkeys, with main merged in:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the theremin with Java: how often it listens, its range and the range that earns Good Vibrations, its lowest and highest pitch, its vibrato and its light; checks the registration, both models (silent and playing) and the blockstate, the textures, words, recipe, loot and advancement) | Pass, 992 IDs |
| `./gradlew build` on `65ce0f7` (Build workflow run 37082604826) | Pass |
| Game tests on the headless server, same run: 545 in total, 4 of them new here (`ThereminGameTests`) | **All 545 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `65ce0f7` (run 37082604826), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-theremin) |

The 4 new game tests (`ThereminGameTests`):
1. the pitch runs from 0.5 at the edge of its range to 2.0 at the antenna, rising all the way in; a comparator reads 15 at the antenna, 8 at four blocks, and 0 at eight blocks and beyond;
2. switched off, it still listens: a player two blocks from the antenna reads high (11 or more), moved to six blocks reads 3 to 5, and moved away is no longer heard;
3. an empty hand switches it on, and a player a block and a half off earns Good Vibrations; switched off again, a redstone block beside it plays it, and taken away, it is silent;
4. the recipe (with the machines feature on), loot table and advancement load.

Found before this record:
- The first screenshots showed the lid as a slab of brass and were taken from too far off to see the magic eye. The lid is now walnut edged in brass, the eye is larger, and the cameras moved closer (`7004a5f`).
- Main's client tests now run in three parallel jobs, and only the first job's opening test hides the HUD; in another job the first screenshots showed the hotbar and chat. The test now hides them itself (`65ce0f7`).

Not run: a two-client dedicated-server playtest, and any play by hand. The theremin has not been heard: CI has no sound, so the pitches are checked by numbers, not by ear.

### Ofrenda verification

Actual results (3 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-34` stacked on the theremin, with main merged in:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the ofrenda with Java: its six places, how often it looks, the welcoming, arriving and witnessing ranges, its light, and the five kinds of offering in order; checks that the ofrenda, petals, papel picado and sugar skull are registered and the marigold is one of the mums; their models, words and loot, the offering tags, pan de muerto's baking and the advancement) | Pass, 1000 IDs |
| `./gradlew build` on `8db38aa` (Build workflow run 37082606603) | Pass |
| Game tests on the headless server, same run: 549 in total, 4 of them new here (`OfrendaGameTests`) | **All 549 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `8db38aa` (run 37082606603), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-ofrenda) |

The 4 new game tests (`OfrendaGameTests`):
1. a stick isn't an offering; marigolds are set out one at a time, six and no seventh; an empty hand takes back the last;
2. a marigold, a candle, pan de muerto, a sugar skull and a honey bottle make it complete, and it glows; it welcomes a restless spirit near, which makes the ofrenda its home and shows itself; once the spirit has come to it, a player near earns Remembered (not before);
3. missing one kind (no drink), it isn't complete and stays dark;
4. each kind's tag holds its offering; a poppy, papel picado and an apple may be offered, a stone may not; the recipes, the baking and the advancement load.

Found before this record:
- In the first test a held stone would have been placed as a block rather than refused, so the test offers a stick instead.
- Main's client tests now run in three parallel jobs, and only the first job's opening test hides the HUD; in another job the first screenshots showed the hotbar and chat. The test now hides them itself (`8db38aa`).

Not run: a two-client dedicated-server playtest, and any play by hand. A welcomed spirit drifting to the ofrenda by itself hasn't been watched; the test moves it there.

## World and event applicability
- A Candy Kettle works anywhere there is heat under it, in every dimension, all year. Nothing is seasonal.
- Wild mushrooms generate only in newly generated Overworld chunks of their biomes; existing chunks don't get them, but one mushroom brought in spreads. They can be planted and spread in any dimension with soil and shade. Fairy rings bless only in the Overworld, on full-moon nights (by the Overworld's moon).
- A Bat House keeps the Overworld's dusk and dawn wherever it is hung.
- A Hay Golem works in any dimension.
- Knitting works anywhere; cosiness needs a lit campfire, in any dimension.
- The Hearth Oven bakes in any dimension.
- A séance works in any dimension, but restless spirits rise only on the Overworld's night.
- Wild turkeys come only to the Overworld, by day, while mobs spawn (the `spawn_mobs` game rule), onto grass in their biomes; existing worlds get them as players walk. Brought elsewhere, they live and breed anywhere.
- A theremin plays in any dimension.
- An ofrenda can be set up anywhere, but welcomes spirits only at night, and restless spirits rise only on the Overworld's night. Wild marigolds generate only in newly generated Overworld chunks of floral biomes and forests, with the mums; existing chunks don't get them, but a marigold brought in can be planted anywhere.

## Rollout and open questions
- Candy can't be poured by hoppers or dispensers; trays are filled and broken up by hand.
- A tray of candy doesn't stack, so a candy maker carries one tray a batch.
- The stages' temperatures are real candy makers' (roughly); the heating rates are compressed so a batch takes under a minute.
- Every effect is open to balance review in play, Absorption and Resistance in particular.
- The fairy-ring blessing isn't saved: a restart during a full-moon night lets a player be blessed again that night.
- Mushrooms aren't food raw. The fly agaric and the jack o'lantern mushroom aren't cooked into anything yet; a later addition could use them (dyes, a potion).
- How often patches generate is a first guess, open to review once worlds are explored.
- Bats out at night may despawn as vanilla bats do far from players; the house fills again by moving in at dusk.
- Hoppers don't take guano from a Bat House; it is scooped by hand.
- A Hay Golem harvests only single-block crops (`CropBlock`s); tall crops, gourds, berries and the giant pumpkin are left to the farmer, as crows leave them.
- Following wheat moves its post: a farmer walking past with wheat in hand will lead it off. Holding something else keeps it where it is.
- A golem doesn't open doors or cross fences; build its field where it can walk.
- Knitwear has no armour value and no enchantments of its own.
- A garment's pattern is fixed by its kind; only its colour changes.
- Cosiness looks for a campfire only, not other fires or heat.
- The oven holds one pie at a time, and hoppers don't feed it or take the pie out; baking is done by hand.
- A coal block (800 seconds) is more than the oven can bank, so it is refused.
- Vanilla's pumpkin pie is unchanged and isn't baked in the oven.
- The board spells only a spirit's name and wish; players can't ask it questions of their own.
- A spirit's wish is one of eight, chosen at random the first time a séance asks it; it fades at dawn whether or not it was given its wish.
- Turkeys don't spawn with world generation: flocks come to players as they explore. Spawn eggs aren't added.
- Turkeys use a parrot's call pitched low; there is no recorded gobble.
- A tom fans his tail only when strutting; there is no display between toms.
- The theremin sings with a note block's flute, the nearest vanilla sound to its wail; it is a quick run of notes, five a second, not one gliding tone.
- It hears only the nearest creature; two players can't play a duet on one theremin. The volume loop is for show: it is louder the nearer the player, not by a second hand.
- The ofrenda doesn't know the date: it welcomes spirits on any night of the year, not only on 1 and 2 November. Nothing seasonal gates it.
- An ofrenda holds six offerings; hoppers don't fill it. Its welcome reaches only restless spirits, which rise from graves; it doesn't raise any.
- There is no photograph item, so a painting or a player's head stands in for a picture of the person remembered.

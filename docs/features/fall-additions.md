# Fall Additions

Status: the chandlery (addition 1), the cider mill (addition 2), the preserves pantry (addition 3), crows and working scarecrows (addition 4), spooky fireworks (addition 5), the sky lantern festival (addition 6), the Harvest Feast Table (addition 7), the corn maze (addition 8), ghost hunting (addition 9) and face paint (addition 10) are implemented in source, not yet played. The Build workflow compiles it, and CI's game tests pass (recorded below).
Proposal issue: none; requested directly by the owner on 2 October 2026 ("ok lets build another 10 more thorough and well thought out festive halloween and fall additions, maybe for one we do candle making with an interesting process to make them allowing you to make a bunch of different combinations and then light them to give different cool effects to an aoe area like beacons do"). The ten additions ship one per pull request, each stacked on the one before:
1. the chandlery: the Wax Melting Pot and Aura Candles;
2. the cider mill: apple trees, the Cider Press, the Cider Barrel and four ciders;
3. the preserves pantry: Mason Jars, eight preserves, the Canning Kettle and the Pantry Shelf;
4. crows and working scarecrows: crows that raid ripe crops, and scarecrows that keep them off;
5. spooky fireworks: rockets that burst into a bat, a jack o'lantern, a ghost or a skull drawn in sparks, and the Show Launcher;
6. the sky lantern festival: Sky Lanterns that drift up together, the festival eight of them make, and mooncakes for a full moon;
7. the Harvest Feast Table: a long table to set with the harvest, and a feast that grows with its variety and its company;
8. the corn maze: a gate that plants a maze of corn from its own seed, and times runners through it;
9. ghost hunting: restless spirits that rise from graves at night, the Spirit Lantern that reveals them, and Ectoplasm, the Ghostly candle scent;
10. face paint: a kit that paints six designs on players' faces, which count as costumes.

Owner: @jimbozoomer-byte
Target milestone and tier: the chandlery is Discovery tier: copper ingots (the pot), honeycomb or rotten flesh (wax), string (wicks), dyes, and vanilla items an early player can gather (sugar, a rabbit's foot, a golden carrot, a feather, a pufferfish, magma cream, an amethyst shard, a ghast tear, a fermented spider eye, bone meal, a glow ink sac, glowstone dust and redstone). The ghast tear and magma cream are Nether items; every other scent is from the Overworld. The cider mill is Discovery tier too: planks, an iron ingot, a grindstone and wooden slabs (the press); a barrel, iron and gold nuggets and sticks (the barrel); apples (from oak leaves, and then apple trees), glass bottles, and for the extras sugar, sweet berries, cocoa beans, wheat and an egg. So is the pantry: glass and an iron nugget (jars), a cauldron, blue dye and iron bars (the kettle), planks and slabs (the shelf), and what the preserves are made of (sweet berries, apples, a pumpkin, cranberries, glow berries, beetroot, peppers, corn, onions, sugar, and the cider mill's sweet cider, mulling spices, aged cider and pomace). Spooky fireworks are Discovery tier too: paper, gunpowder and the picture's ingredients (a feather and black dye, a carved pumpkin, a phantom membrane or a bone), and glowstone dust to twinkle; the Show Launcher is a dispenser, iron ingots, planks and redstone. Crows need nothing to come but a ripe crop; the scarecrow that keeps them off is the existing one (a hay bale, wool and sticks), and its pumpkin heads come from any pumpkin patch. The sky lantern festival is Discovery tier too: paper, string and a candle (lanterns), any dye for a colour and an anvil for a wish; mooncakes are baked in the Kitchen Garden's Cooking Pot from wheat, sugar, an egg and beans, roasted chestnuts or a pumpkin. The Harvest Feast Table is Discovery tier too: wooden slabs and logs; what it rewards is a varied harvest, so its best feasts come as a farm grows. The corn maze is Discovery tier too: a hay bale and sticks (the gate) and corn kernels, a kernel a stalk of wall. Ghost hunting is Discovery tier too: gold nuggets, glass panes, an amethyst shard and a candle (the Spirit Lantern), glass bottles, and the graveyard decorations' gravestones and grave mounds. Face paint is Discovery tier too: a bowl and white, black, orange and green dye.
Primary specialty and supported player role: crafting and support. Chandlers make candles for builders (light in any colour), farmers (the harvest aura), explorers and miners (night vision, water breathing, fire resistance, haste), and groups (one candle covers everyone near it). Candles are easy to trade: each one carries its own wax, colour, scents and burn time. The cider mill is for farmers and cooks: orchard keepers grow apples, and a cider maker presses them and ages the cider. Its drinks are for anyone (haste for miners, jump boost, absorption before a fight, regeneration after), and a barrel of aged cider is the centrepiece of a harvest party. The pantry is for cooks who put up the harvest: preserves keep (once sealed) and travel, so a stocked pantry feeds a long expedition or a harvest feast. Crows are for farmers: a little pressure on open fields, answered by building and dressing scarecrows, and a source of feathers for fletchers and chandlers. Spooky fireworks are for anyone throwing a party: one rocket for a moment, or a Show Launcher's nine tubes for a planned show. Sky lanterns are for gatherings: one player can let eight go and hold a festival alone, but a crowd fills the sky faster, and everyone there shares the Luck; mooncakes are for cooks, and for anyone out under a full moon. The feast table is for farmers and cooks to share their harvest: a host sets a long table, and everyone who eats there shares the blessing, so a group fills up before a dig, a raid or a boss. The corn maze is for farmers who grow corn and for anyone at a harvest party: one player plants it, and anyone can run it against the clock. Ghost hunting is for explorers and chandlers: a hunter with a lantern reveals the spirits haunting a graveyard to everyone near, friends corner them, and the Ectoplasm they catch scents candles. Face paint is for anyone dressing up: a painter does a friend's face in a moment, and a painted face is a costume that leaves the head free for a hat, or for a helmet once the party's over.

Nothing here depends on the Halloween event: candles are made and burned, and cider pressed and aged, and preserves put up, all year; crows come to fields all year too, and fireworks and sky lanterns go up whenever someone lets them go. A mooncake's Luck comes with every full moon, one night in eight, and a feast can be held, or a maze run, any day.

## Player experience
### The Wax Melting Pot
1. A hammered copper pot with a rolled rim, a pouring lip and two handles (seven copper ingots, shaped like a cauldron). Set it over a heat source: a lit campfire, fire, soul fire, lava or a magma block (the same `jugcraft:heat_sources` the Cooking Pot uses).
2. **Put wax in:** honeycomb is beeswax (two measures a comb); rotten flesh renders down to tallow (one measure). A pot holds one wax at a time, up to eight measures. Over the heat a measure melts every five seconds, and the wax rises in the pot. Away from heat, molten wax sets again, a measure every fifteen seconds.
3. **Mix the molten wax:**
   - **Dyes** colour it. Several dyes mix as they do on leather (red and blue make purple), so any colour a leather tunic can take, a candle can.
   - **Scents** give the candle its aura. Two at most go in a pot; a third is refused, and so is one already in it. The eleven scents and what stirs them in are listed below.
   - **Glowstone dust** brightens it: a stronger aura (level II), but the candle burns twice as fast.
   - **Redstone** makes it burn half as long again.
   - Each of these lasts until the pot is empty, so one scent item flavours up to eight layers.
   Coloured scent wisps drift up from the pot, and the wax bubbles. Use it with an empty hand to see what is in it ("Beeswax: 6 of 8 measures molten, Swiftness, Moonlight, brightened"); sneak with an empty hand to pour it all away.
4. **Dip a candle.** Use string on the molten wax and it comes out as a one-layer candle of that wax, colour and mixture. That uses a measure.
5. **Dip it again to build it up,** but only once its last layer has cooled: two seconds, shown as a cooldown on the hotbar. Dipped while still warm, the new layer slides off and its wax is lost. Each layer:
   - makes the candle taller and its aura wider (5, 8, 12, then 16 blocks);
   - makes its light stronger (8, 10, 12, then 14);
   - adds its own burn time;
   - turns its outside the new wax's colour;
   - adds the new wax's scents to the ones the candle already carries.
   Four layers is as big as a candle gets.
6. **Combinations:** a candle carries every scent of every layer it was dipped in. Two scents work together; a third muddles it ("Muddled Beeswax Candle"): it lights, but gives no aura. So the art is in dipping different pots in the right order: a layer of swiftness wax under a layer of moonlight wax makes a candle of both. With 11 scents, 2 waxes, 16 dyes (and their mixes), brightening, extending and four sizes, there are thousands of candles to make.
7. A candle is named for what it carries ("Beeswax Candle of Swiftness and Moonlight", in the scents' colours). Its tooltip shows the aura's reach, how long it burns, its layers and whether it is brightened or long-burning.

### The scents
| Scent | Stirred in with | Aura |
| --- | --- | --- |
| Swiftness | sugar | players get Speed |
| Leaping | a rabbit's foot | players get Jump Boost |
| Moonlight | a golden carrot | players get Night Vision |
| Featherfall | a feather | players get Slow Falling |
| Tide | a pufferfish | players get Water Breathing |
| Ember | magma cream | players get Fire Resistance |
| Diligence | an amethyst shard | players get Haste |
| Mending | a ghast tear | players get Regeneration |
| Warding | a fermented spider eye | hostile mobs are slowed and weakened |
| Harvest | bone meal | growing plants get extra growth ticks |
| Revealing | a glow ink sac | other creatures glow, so they show through walls (and restless spirits are revealed) |
| Ghostly | Ectoplasm (caught from restless spirits) | players turn invisible |

Each scent is an item tag (`jugcraft:candle_scents/<scent>`), so packs can add items to it.

### Aura Candles
8. Place a candle on any block with a solid top: it stands on a brass dish with a finger ring. Light it with flint and steel or a fire charge; use it with an empty hand to snuff it, keeping what is left.
9. **Lit, it works like a small beacon:**
   - Every four seconds its aura pulses over a box reaching its radius in every direction. Players inside get each scent's effect for nine seconds, so it never runs out while they stay.
   - A ring of the scents' colours shows the edge of the aura at each pulse, and their wisps rise from the flame.
   - The flame is tinted by the first scent (a warm yellow for an unscented or muddled candle), and a tallow candle smokes a little.
10. **It burns down as it burns:** it shrinks to a quarter of its height, then goes out for good in a puff of smoke. A layer of beeswax burns four minutes; a layer of tallow two. Brightening halves that; redstone makes it half as long again. A long-burning beeswax candle of four layers burns 24 minutes.
11. Broken, a candle drops itself as it is, part-burned, with its name, colour and scents, to be placed and lit again later. Burned out, it is gone.

### The apple tree
12. **Apple trees** grow wild in plains and flower-rich places (meadows, flower forests, sunflower plains): a short oak trunk under a rounded crown of apple leaves.
13. Their leaves (the ones the tree grew, with air below them) **blossom** white and pink and then **hang with ripe red apples**, about a Minecraft day from bare leaves to ripe. Use ripe leaves with an empty hand to pick them: 1 to 3 apples fall, and the leaves start again. Leaves a player placed never fruit.
14. **Apple seeds** plant an apple sapling on dirt or grass, which grows like any sapling (bone meal works). Broken apple leaves drop seeds now and then (as oak leaves drop saplings), sticks, and now and then an apple. The pomace from the press gives seeds too, so vanilla apples alone are enough to start an orchard.

### The Cider Press
15. A slatted oak basket bound with iron, on a trough, under a beam with an iron screw. Behind it is the grinder (the scratter): a drum under a hopper, with a crank. It faces you when placed.
16. **Grind:** put apples in the hopper, one at a time, up to eight. Use the press with an empty hand to **turn the crank**: an apple is ground into pulp in the basket, with a crunch and a splash. The crank turns once every 8 ticks, however fast anyone clicks.
17. **Press:** once the hopper is empty, an empty hand **turns the screw** instead.
    - The plate comes down on the pulp (the "cheese"), and each of four turns squeezes out a quarter of its juice into the trough, a serving for every apple.
    - The capstan bar on the beam turns a quarter turn each time, and the pulp squashes lower.
    - Nothing more goes in until the cheese is pressed out.
    - The last turn frees the screw and **knocks out the pomace** (one for every two apples).
18. **Bottle it:** a glass bottle draws a serving of juice from the trough: **Sweet Cider**. The trough holds eight servings; the screw won't turn while a turn's juice wouldn't fit. Sneak with an empty hand to see what is in the press. Comparators read the juice.

### The Cider Barrel
19. An oak cask on its side in a cradle, with iron hoops, a brass tap and a bung. Pour **Sweet Cider** in by the bottle (the bottles come back), up to sixteen servings.
20. **It ages by itself:**
    - A day after the last fresh serving went in, the batch is **Sparkling Cider** (it fizzes at the bung); three days after, **Aged Cider**.
    - A chalk mark on its head shows the stage (one stroke, two, three).
    - A batch that has begun to ferment takes no more fresh juice, so new juice can't be slipped into an old batch; topping up a sweet batch starts its day again.
21. A glass bottle draws off a serving of whatever it has become. Use it with an empty hand to see how much is in it and how long to the next stage. Comparators read how full it is.
22. Broken, a barrel keeps its cider, and the cider keeps ageing while it is carried.

### Ciders and what they make
| Drink | How it's made | Food | Effect |
| --- | --- | --- | --- |
| Sweet Cider | bottled from the press | 3 | Haste, 30 s |
| Sparkling Cider | a day in the barrel | 3 | Jump Boost, 60 s |
| Aged Cider | three days in the barrel | 4 | Absorption, 120 s |
| Mulled Cider | sparkling cider and mulling spices in the Cooking Pot | 6 | Regeneration, 15 s |

23. Every cider is drunk even on a full stomach and leaves its bottle.
24. **Mulling Spices:** sugar, sweet berries and cocoa beans make two.
25. **Apple Cider Donuts:** two wheat, sugar, an egg and a Sweet Cider make four cinnamon-sugar donuts (3 food each); the cider's bottle comes back. They count as candy, for Candy Bowls and Candy Bags.
26. **Apple Pomace** feeds pigs and goes in the composter; crafted, it gives apple seeds.

### Preserves
27. **Mason Jars** (glass round an iron nugget lid, three a craft) are cooked full of preserves in the **Cooking Pot**:

| Preserve | In the pot with a Mason Jar | A serving | Effect |
| --- | --- | --- | --- |
| Sweet Berry Jam | 6 sweet berries, 2 sugar | 3 food | |
| Apple Butter | 3 apples, sugar, a Sweet Cider (apple butter is cooked down in cider) | 4 food | |
| Pumpkin Butter | a pumpkin, 2 sugar, Mulling Spices | 4 food | |
| Cranberry Preserves | 6 cranberries, 2 sugar | 3 food | |
| Glow Berry Jelly | 6 glow berries, 2 sugar | 2 food | Night Vision, 30 s |
| Pickled Beets | 4 beetroot, Cider Vinegar | 2 food | |
| Pickled Peppers | 4 peppers, Cider Vinegar | 2 food | Fire Resistance, 15 s |
| Corn Relish | 2 corn, a pepper, an onion, Cider Vinegar | 3 food | |

28. **Cider Vinegar:** Aged Cider soured on Apple Pomace in the Cooking Pot (a bottle a batch). Pickling with it leaves its bottle in one of the pot's result slots, where a hopper can take it.
29. **A jar holds four servings.** Eat it like food, a serving at a time; the last serving leaves the empty Mason Jar to fill again. Its tooltip says how many servings are left and whether it is sealed.
30. **Unsealed jars spoil.** Fresh from the pot a jar is unsealed: it keeps three days from when it was cooked. After that a serving is 1 food, with Hunger and Nausea ("Ugh! That jar had gone off"). An opened jar also keeps three days from when it was opened.

### The Canning Kettle
31. A big speckled-blue enamel kettle with a jar rack (a cauldron, blue dye and iron bars). Fill it with a **water bucket** and set it over a fire (anything in `jugcraft:heat_sources`).
32. Stand up to **four full, fresh jars** in the water. An opened jar, or one already sealed, is refused, and so is any jar while there's no water.
33. The water comes to a **rolling boil** after ten seconds over the fire (bubbles and steam). Each jar that has been **twenty seconds in boiling water seals**. Off the fire the water cools again and nothing seals.
34. Use the kettle with an empty hand to **lift out the sealed jars**, with a pop. Sneak to lift out every jar, sealed or not (or, with none ready, to see how it's doing). An empty bucket takes the water back while no jars are in it. Comparators read how many jars have sealed.
35. **A sealed jar keeps until it is opened**, stacks with other sealed jars of the same preserve, and wears a red gingham cap tied with string.

### The Pantry Shelf
36. An open oak cupboard with a beadboard back and two shelves. Use it holding a jar (full, opened, sealed or empty) to put it up, three to a shelf; use it with an empty hand to take the last one down. Comparators read how full it is; broken, it spills its jars.

### Crows
37. **Crows come to fields by day.** Every ten seconds, for each player in the Overworld, there is a three-in-ten chance the game looks at one spot 16 to 40 blocks away. If a ripe crop open to the sky is within six blocks of it, a flock of two or three crows arrives in the sky above. There are at most six crows near a player and 32 in the world. They come only while mobs spawn (the `spawn_mobs` game rule).
38. **A crow wheels a few blocks over the ground, cawing.** Now and then it spots a ripe crop open to the sky within 12 blocks, drops onto it and pecks at it for two seconds, its head bobbing; then the crop is **three growth stages back** (for wheat, from ripe to age 4). It rests 30 to 60 seconds before it raids again. Crows go for any fully grown crop of the vanilla crop kind: wheat, carrots, potatoes, beetroot, and Jugcraft's beans, sweet potatoes, flax, onions, garlic, cabbage, oats, barley and turnips. Tall crops, gourds and pumpkins on stems, bushes and trees are safe, and so is anything under a roof.
39. **Pecking follows the `mob_griefing` game rule.** With it off, crows still come and wheel about but leave crops alone.
40. **Scaring them off:** a crow takes flight from a player within six blocks (sneaking, you can get within two and a half), from a blow, and from a scarecrow; the crop it was after is spared. Crows don't attack.
41. **At nightfall** crows give up their crops and climb away out of sight.
42. **Crows drop up to two feathers** (for arrows, and for the chandlery's Featherfall scent). They have two hearts.

### Working scarecrows
43. **A Scarecrow now guards the crops round it:** within 4 blocks bare, 8 wearing a pumpkin head, and 12 wearing a lit one (a jack o'lantern, or a hand-carved pumpkin with a torch in it), measured across the ground from its head, and up to 6 blocks above or below it. Crows won't go for a guarded crop, and a crow after a crop takes flight within half a second of a scarecrow going up beside it.
44. Nothing else about the scarecrow changes: it is placed, dressed, dyed and broken as before. It stands on any block with a solid top, so in a field it goes on a dirt, grass or path block rather than on farmland.

### Spooky fireworks
45. **Four spooky fireworks**, each bursting into a picture drawn in coloured sparks: a **bat** (violet wings, red eyes), a **jack o'lantern** (orange, with a green stem and a glowing yellow face), a **ghost** (white, with a pale blue hem) and a **skull** (bone white). Dark details (eyes, sockets, a mouth) are left as holes in the picture, since dark sparks would vanish against the night.
46. **Every player sees the picture the right way round:** each client draws it facing its own player, so a crowd round a launcher all see the bat's wings spread, not edge on.
47. **Crafting:** paper, one to three gunpowder (the flight, as vanilla's rockets) and the picture's ingredients (a feather and black dye, a carved pumpkin, a phantom membrane, a bone) make three. Add glowstone dust and the sparks **twinkle** as they fade.
48. **Setting one off:** use it on a block and it climbs, trailing sparks, for a second or two by its flight, and bursts with a bang. Spooky fireworks hurt nothing and break nothing, and can't boost elytra flight. A dispenser facing up sets one off as by hand; facing any other way, it fires it straight out.

### The Show Launcher
49. A painted crate of **nine mortar tubes**, three rows of three, in orange and black, with a brass dial on its front. Each tube holds **up to sixteen rockets** of one kind, spooky or vanilla; the nose of the next rocket peeks out of each loaded tube.
50. Use it holding rockets to load them (matching tubes first, then empty ones). Hoppers can load it from any side and unload it from below.
51. **Starting a show:** use it with an empty hand, or give it a rising redstone signal; do it again to stop. Sneak and use it with an empty hand to turn the dial between three ways of firing:
    - **in sequence:** one rocket every half second, round the loaded tubes in turn;
    - **in volleys:** a row of three every second, front row first;
    - **as a finale:** one from every tube at once.
52. Rockets leave the tubes **fanned out from the middle**, so a volley or a finale spreads across the sky. A show runs until the tubes are empty (a finale fires once). Comparators read how many tubes are loaded; broken, it spills its rockets.

### Sky lanterns
53. **Sky Lanterns:** five paper, two string and a candle make two. Dye one in the crafting grid, as leather armour is dyed (warm red undyed; a water cauldron washes the dye out), and name it in an anvil: the name is its **wish**, shown over it as it rises.
54. **Letting one go:** use it and it is lit and let go just in front of you. It rises 0.7 blocks a second, bobbing a little and turning slowly, and drifts on **a wind every lantern shares**: 0.3 blocks a second, its direction turning full circle every three days. Lanterns let go together drift together, so a festival's lanterns rise as one cloud.
55. **Glowing and burning out:** the paper glows in its colour, lighter where the flame shines through, and flickers. A lantern burns for two minutes to two and a half, dims over its last five seconds and is gone; one that rises above the world is gone too. A blow tears it and puts it out with a hiss. It lights no blocks and sets nothing alight.

### The lantern festival
56. **Eight lanterns let go within 32 blocks of each other in two minutes**, by one player or many, make a festival. Everyone within 32 blocks is told "The sky fills with lanterns! Make a wish", gets **Luck for five minutes** and earns **A Sky Full of Wishes**. The same place holds no second festival for a day.

### Mooncakes
57. **Three mooncakes**, baked four at a time in the Cooking Pot (15 seconds) from two wheat, a sugar, an egg and a filling: **red bean** (two beans), **chestnut** (two roasted chestnuts) or **pumpkin** (a pumpkin).
58. Each is 3 food. Eaten **outdoors on a full-moon night** (the sky open above you), a mooncake also gives **Luck for five minutes**.

### The Harvest Feast Table
59. **A long trestle table** of honey oak with an orange runner: three wooden slabs over two logs make two lengths. Lengths placed end to end along the same line **join into one table**, with legs only at its ends; a length placed against a table's end carries on along it, and one placed across stands alone.
60. **Serving:** each length holds **two dishes**, one at each half. Use it holding any food or drink to serve it on the dish you point at: **up to eight servings**, one food to a dish. Each dish sits on a cream plate, heaped higher the more servings are left. Sneak and use it with an empty hand to take a dish back; broken, a length spills its dishes.
61. **Eating:** use it with an empty hand to eat a serving from the dish you point at. It feeds you just as eating it would, and a stew leaves its bowl. You can eat a serving whether or not you are hungry.

### The feast
62. **A feast's score** is how many different foods are on the whole table plus how many players have eaten at it in the last two minutes. Every serving eaten works it out and blesses every recent diner within 16 blocks, the eater included:
    - 3 or more, **a good meal**: Regeneration for 10 seconds;
    - 5 or more, **a feast**: Absorption for two minutes as well;
    - 8 or more, **a harvest feast**: Haste and Luck for five minutes as well;
    - 11 or more, **a grand feast**: Health Boost for five minutes as well, and the advancement **Harvest Home**.
63. The eater is told how the meal went ("A harvest feast! (7 dishes, 1 at the table)"). Alone at a table of seven foods you reach a harvest feast; a grand feast needs friends (six foods and five diners) or a very varied table (ten foods alone).

### The corn maze
64. **The Corn Maze Gate:** two hay-wrapped posts and a crossbar with a green pennant; a hay bale and four sticks make one. It faces away from whoever placed it, into the maze it will plant ahead of it.
65. **Choosing a size:** sneak and use it with an empty hand to step through **tiny** (7 by 7 blocks), **small** (11 by 11), **medium** (15 by 15) and **large** (19 by 19).
66. **Planting:** use it holding corn kernels. The gate carves a new maze from a fresh seed: a perfect maze, with one way through and every part reachable, the entrance at the gate and the exit straight across on the far side. It plants **maze corn** along the walls, **a kernel a stalk**: 30 kernels for a tiny maze on clear, flat ground, 70 for small, 126 for medium and 198 for large. It plants only where the ground is solid and the three blocks above are clear, and never replaces a block, so plant on open, level ground: a tree or a wall in the way stays where it is. The **finish post**, the gate's twin with a chequered flag, goes at the exit. If that square isn't clear the gate says so and plants nothing; if you carry too few kernels it says how many it needs. A planted maze isn't planted again.
67. **Maze corn:** three blocks of tall, ripe corn planted close as a wall. It can't be walked through, doesn't grow, and stands on any solid ground. Breaking a stalk anywhere brings down what is above; its bottom gives back the kernel it was planted from.

### Running the maze
68. **Walk out through the gate** to start the clock ("Go! Find the way through"), and reach the finish post to stop it. Standing in the gate restarts your clock.
69. **Fair runs:** the server follows each runner every tick. A run is void if the runner flies or glides, climbs up over the corn, leaves the maze, takes more than ten minutes, or reaches the finish having walked less than four fifths of the shortest way through (a shortcut, say through a broken wall). Sixteen can run at once.
70. **The board:** the runner is told their time, and each runner's best goes on the gate's board of the top three. The first time a runner places on a gate's board they get a prize ribbon, first, second or third (the Harvest Scale's ribbons); finishing at all earns **A-maze-ing**. Use the gate with an empty hand to see the maze's size and its board.

### Ghost hunting
71. **Graves stir at night.** Gravestones (rounded, cross and obelisk) and grave mounds now take random ticks, about one a minute each at the default rate; at night on the Overworld clock, one in four raises a **restless spirit** above the grave, until three are within 16 blocks. Spirits need the `spawn_mobs` game rule and the agriculture feature.
72. **Restless spirits** are pale, hooded shapes of see-through wisps, arms reaching, tails trailing away to nothing, with hollow eyes and a moaning mouth. Each haunts its grave, drifting about it up to 6 blocks across the ground and from half a block to 3 blocks above it. **No one sees a spirit until it is revealed**; hidden, it can't be aimed at, and it heeds no one.
73. **The Spirit Lantern:** a brass lantern with a pale green flame behind amethyst glass (three gold nuggets, two glass panes, an amethyst shard and a candle). **Held in either hand, it reveals every spirit within 12 blocks, to everyone near**, not only its bearer. A revealed spirit fades into view with a shimmer, glows faintly, moans now and then and stares at whoever is nearest. A **Revealing** aura candle reveals the spirits in its glow too.
74. **The hunt:** a revealed spirit **shies away** from anyone within 3 blocks (a sneaking player gets to a block and a half), but it never leaves its haunt, so it can be cornered at the edge, or by friends coming at it from both sides. Blows pass straight through it. At dawn spirits fade.
75. **Catching one:** use a glass bottle on a revealed spirit and the bottle fills with **Ectoplasm**, glowing green ooze. The first catch earns **Ghost Hunter**. A spirit drops nothing else.
76. **Ectoplasm** is the **Ghostly** candle scent: stirred into molten wax it gives its bottle back, and a lit Ghostly candle turns the players in its aura **invisible**.

### Face paint
77. **The Face Paint Kit:** a tin palette of greasepaints and a brush (a bowl and white, black, orange and green dye), good for **16 faces**. Its dial is set to one of six designs: a **skull**, a **jack o'lantern**, a **black cat**, a **vampire**, a **witch** or a **scarecrow**. Sneak and use it to turn the dial; the kit's tooltip shows the design.
78. **Painting:** use the kit on another player to paint their face at once, or hold use for a second and a half to paint your own. Painting over a painted face replaces the design. Each face wears the kit by one use. Painting a friend's face earns **Face Painter**.
79. **On the face:** the design is drawn over the face of the painted player's skin, for everyone who can see them, turning and nodding with the head. Designs leave the skin showing where they don't paint (the cat's whiskers, the scarecrow's rosy cheeks), and some leave the eyes clear (the vampire, the witch). A helmet or a pumpkin on the head covers the face as it would.
80. **Washing off:** paint lasts until the player's head goes under water, or they die.
81. **A costume:** a painted face counts as a costume for trick-or-treating (with nothing on the head) and for the costume contest's runway. A costume hat's extra treat still needs the hat.

## Connections
- Existing input producer: vanilla copper (the pot), bees (honeycomb), zombies (rotten flesh), spiders (string), dyes, and the scent items above; Jugcraft's `jugcraft:heat_sources` (the Cooking Pot's heat).
- Existing output consumer:
  - Light in any colour and size, for builders.
  - Status effects for every player in range.
  - Growth for farms: the harvest aura speeds up every plant that takes random ticks and bone meal, including Jugcraft's own crops (corn and the other tall crops, gourds, cranberries and giant pumpkins).
  - Candles go back into the pot to be built up.
- Technology connection: none needed. The pot uses the same heat rule as the Cooking Pot, so any heat source added to that tag later heats both.
- Magic connection: none yet. The planned ghost hunting addition (9) is to add a scent made from ectoplasm, through the same `jugcraft:candle_scents/*` tags.
- Reachable entry path: copper, honeycomb or rotten flesh, string and a campfire are all early-game; no candle needs another candle or anything from later tiers. Tallow candles need nothing from bees.
- Required vs optional: all optional; nothing in progression needs a candle. Nothing here is gated by the Halloween event.
- Trade and solo routes: a solo player can make every candle; candles also stack (16) and carry everything they are, so a chandler can make them for others.
- How this stays useful without other branches: every scent's effect is useful on its own, and the harvest aura helps any farm.
- Cider mill, input producer: vanilla apples (oak leaves, villagers, chests) and then apple trees; the Cooking Pot (mulled cider) with its heat sources; wheat, sugar, eggs, sweet berries and cocoa beans; glass bottles.
- Cider mill, output consumer:
  - drinks for everyone;
  - donuts for Candy Bowls and Candy Bags (the existing `c:foods/candy` tag);
  - pomace for pigs (the vanilla `minecraft:pig_food` tag) and composters;
  - seeds for orchards.
- Pantry, input producer: the Cooking Pot and its heat sources; Agriculture's crops (cranberries, peppers, corn, onions) and vanilla ones (sweet berries, glow berries, beetroot, apples, pumpkins); the cider mill's sweet cider, mulling spices, aged cider and pomace.
- Pantry, output consumer: food that keeps (sealed jars), for expeditions and for the planned harvest feast (addition 7), which is to count dishes; preserves join `c:foods`.
- Pantry, entry path: glass, an iron nugget, a cauldron, dye and iron bars are early-game; every preserve has a crop that grows wild or is vanilla. Only pickles need the cider mill (for vinegar).
- Cider mill, entry path: apples drop from vanilla oak leaves, and a pressed apple's pomace gives apple seeds, so the first orchard needs no wild apple tree. The press and barrel are crafted from vanilla materials.
- Crows, input producer: any ripe crop open to the sky; the existing Scarecrow, and pumpkins, jack o'lanterns and hand-carved pumpkins for its head.
- Crows, output consumer: feathers, for vanilla arrows and the chandlery's Featherfall scent (`jugcraft:candle_scents/featherfall`); a use for scarecrows beyond decoration.
- Crows, entry path: none needed to meet them; a scarecrow is a hay bale, wool and three sticks, all early-game.
- Crows, required vs optional: crows are a nuisance, never a gate. A pecked crop is set back, never destroyed, and grows again; nothing in storage is touched. The game rules turn spawning and pecking off.
- Fireworks, input producer: paper (sugar cane), gunpowder (creepers), feathers (chickens, and now crows), black dye, carved pumpkins, phantom membranes, bones, glowstone dust; vanilla firework rockets in the launcher.
- Fireworks, output consumer: celebrations: the Halloween events, the Carving Contest and the regatta, and the harvest feast to come. Not a progression item.
- Fireworks, entry path: everything is early-game; the phantom membrane needs a few sleepless nights, and the other three pictures don't.
- Fireworks, required vs optional: purely celebratory, all year, reachable solo.
- Lanterns, input producer: paper (sugar cane), string (spiders), candles, dyes, an anvil; for mooncakes, wheat, sugar, eggs, the Kitchen Garden's beans, the festival crops' roasted chestnuts, pumpkins and the Cooking Pot.
- Lanterns, output consumer: gatherings: the Halloween events, the harvest feast to come, and any celebration. Luck (vanilla's: better loot rolls and fishing) for everyone at a festival or under a full moon. Not a progression item.
- Lanterns, entry path: everything is early-game; the Cooking Pot is the Kitchen Garden's first station.
- Lanterns, required vs optional: purely celebratory, all year, reachable solo (one player can let eight go).
- Feast, input producer: every food and drink in the game: the Kitchen Garden's Cooking Pot dishes, the festival crops, the cider mill's ciders, mooncakes and vanilla food; wooden slabs and logs for the table.
- Feast, output consumer: groups before a dig, a raid or a boss (Regeneration, Absorption, Haste, Luck and Health Boost), and the advancement Harvest Home. It gives every food a second use and rewards growing many crops rather than one.
- Feast, entry path: the table is wood, and any food at all makes a meal; its higher feasts come with a varied farm and with friends.
- Feast, required vs optional: optional and all year; reachable solo up to a harvest feast.
- Maze, input producer: corn kernels (from corn, the Fall Harvest's first crop), a hay bale and sticks.
- Maze, output consumer: harvest parties and friendly races; the Harvest Scale's prize ribbons and the advancement A-maze-ing. Clearing a maze gives its kernels back for the next.
- Maze, entry path: corn is an early crop, and a tiny maze needs only 30 kernels.
- Maze, required vs optional: optional and all year; one player can plant and run a maze alone, and a board of best times gives friends something to beat.
- Ghosts, input producer: the graveyard decorations' gravestones and grave mounds (now where spirits rise), gold nuggets, glass panes, an amethyst shard and a candle (the lantern), glass bottles; the chandlery's Revealing candles reveal spirits too.
- Ghosts, output consumer: the chandlery: Ectoplasm is its twelfth scent, Ghostly (Invisibility), for hide-and-seek, haunted houses and sneaking past mobs. The advancement Ghost Hunter.
- Ghosts, entry path: the lantern is early-game (an amethyst shard from a geode); a gravestone is cut from stone on a stonecutter, and a grave mound is two dirt and a rotten flesh; spirits need only a grave and a night.
- Ghosts, required vs optional: optional and all year; one hunter can corner a spirit alone, and a group, with one lantern among them, all see the spirits.
- Face paint, input producer: dyes (white, black, orange, green) and a bowl.
- Face paint, output consumer: trick-or-treating and the costume contest, which now take a painted face as a costume; parties and screenshots.
- Face paint, entry path: everything is early-game.
- Face paint, required vs optional: cosmetic, all year; one player can paint their own face.
- Crows and the plans: [AGRICULTURE.md](../branches/AGRICULTURE.md) promised that the scarecrow would one day keep crop-eating birds away; it now does.
- Cider mill and the plans:
  - [AGRICULTURE.md](../branches/AGRICULTURE.md) plans orchards (slice 4) and a Fruit and Seed Press (apples into cider, grapes into juice, seed oil for engineers).
  - The apple tree is the first orchard tree, on the chestnut tree's pattern (now a shared `FruitingLeavesBlock`).
  - The Cider Press is the apple half of that press. Grapes and seed oil are not built.

## Balance and automation
- No energy. A pot costs seven copper ingots. A candle costs one string and one measure of wax a layer: half a honeycomb of beeswax, or one rotten flesh of tallow. A dye, a scent, a glowstone dust or a redstone is used once per pot of wax, however many layers that wax makes (up to eight).
- Units: wax in **measures** (a pot holds 8); burn time in ticks (20 a second).

| | Beeswax layer | Tallow layer |
| --- | --- | --- |
| Measures from one item | 2 (a honeycomb) | 1 (a rotten flesh) |
| Burn time | 4,800 ticks (4 min) | 2,400 ticks (2 min) |
| Brightened (×0.5) | 2,400 ticks | 1,200 ticks |
| Long-burning (×1.5) | 7,200 ticks | 3,600 ticks |
| Both | 3,600 ticks | 1,800 ticks |

- A candle's burn time is the sum of its layers' times, so a candle of mixed layers burns for each layer's own time.
- Radius and light by layers: 5, 8, 12 and 16 blocks; light 8, 10, 12 and 14.
- Melting: a measure every 100 ticks over heat; setting: a measure every 300 ticks without heat; cooling between dips: 40 ticks.
- **Effects:**
  - Every 80 ticks, a lit candle gives players in range 180 ticks of each scent's effect: level I, or level II when brightened.
  - Warding gives hostile mobs Slowness (level II when bright) and Weakness I.
  - Revealing gives other creatures Glowing.
  - None of these stack between candles: two candles of the same scent only refresh the same effect.
  - Compared with a beacon, a candle reaches less far (16 blocks at most against a beacon's 20 to 50), lasts minutes rather than for ever, and costs wax and string each time. In return it needs no pyramid and gives two effects at level II from one candle.
- **Harvest:**
  - Each pulse gives radius² ÷ 4 random ticks (twice as many when bright) to random spots within the radius and two blocks above or below the candle. A spot is ticked only if it holds a plant that takes bone meal and random ticks.
  - That is 6, 16, 36 or 64 a pulse by size, spread over the area. Each plant in range gets roughly a fifth more growth ticks than the world gives it (about two fifths when bright), at any size of candle.
  - Several harvest candles add up, each burning its own wax.
- **No positive-gain loop.**
  - A candle never turns back into wax, scents or anything else; burned out, it is gone. The pot makes nothing but candles.
  - The harvest aura only speeds growth that bone meal could give anyway: vanilla already lets crops be composted into bone meal. A candle spends wax and string to spread that over time, and gives no item of its own.
- Automation: candles are dipped by hand (a player's use and cooldown), so the pot can't be automated. Lit candles need no attention until they burn out.

- **Cider mill:**
  - Costs:
    - the press: four planks, an iron ingot, a grindstone and three wooden slabs;
    - the barrel: a barrel, two iron nuggets, a gold nugget and two sticks.
  - Units: juice and cider in **servings** (a bottle each). An apple gives one serving of juice, and the press a pomace for every two apples (rounded up). The trough holds 8 servings, the barrel 16.
  - Time:
    - a crank or screw turn every 8 ticks at most; a cheese takes 4 turns;
    - fruit: a stage every 10 random ticks on average, two stages (about a Minecraft day), 1 to 3 apples a pick;
    - the barrel: sparkling at 24,000 ticks (a day, 20 minutes) after the last fresh serving, aged at 72,000 (three days, an hour).
  - Food:
    - a raw apple is 4 food; its serving of Sweet Cider is 3 (with 30 s of Haste) and half a pomace;
    - ageing adds food (aged cider 4) and a better effect for time, not material.
  - Effects are level I. Aged Cider's two minutes of Absorption match a golden apple's Absorption (without its Regeneration). A barrel makes sixteen, but only after three days and only from apples. This is open to balance review in play.
  - **Bottles are never made from nothing:**
    - the press turns a bottle into a cider;
    - the barrel gives a bottle back for each serving poured in and takes one for each drawn off;
    - every cider leaves its bottle when drunk.
    - Only Sweet Cider gives its bottle back in a recipe (the donuts). Sparkling Cider doesn't, because the Cooking Pot hands back crafting remainders and the mulled cider keeps that bottle.
    - The checker refuses a drink that gives its bottle back and cooks into another bottled drink.
  - **No positive-gain loop.** Nothing turns back into apples. Pomace makes one seed; seeds make trees, which need land and time as any orchard does.
  - Automation: grinding, pressing and bottling are by hand (a player's use); a barrel ages by itself. Comparators read both.

- **Pantry:**
  - Costs:
    - three Mason Jars: five glass and an iron nugget;
    - a kettle: a cauldron, a blue dye and iron bars;
    - a shelf: six wooden slabs and two planks.
  - Units: a jar is **four servings**.
  - Food (a jar's four servings, against what goes in):
    - jam: 12, from six sweet berries (12);
    - apple butter: 16, from three apples and a sweet cider (15);
    - cranberry preserves: 12, from six cranberries (12);
    - jelly: 8, from six glow berries (12), with Night Vision;
    - pumpkin butter: 16, from a pumpkin, which isn't food raw (vanilla's pumpkin pie gives 8);
    - pickles: 8, from four beetroot (4) or four peppers;
    - corn relish: 12, from two corn, a pepper and an onion.
    - Cooking adds a little, as vanilla's cooking does; the real gain is that sealed food keeps and stacks.
  - Time:
    - 300 ticks in the pot for preserves, 200 for vinegar;
    - the kettle boils after 200 ticks over heat and seals a jar after 400 at the boil;
    - an unsealed or opened jar keeps 72,000 ticks (three days).
  - **No container from nothing.**
    - A jar goes into every preserve and comes back after the last serving.
    - Vinegar's bottle comes back when it pickles; aged cider's bottle carries into the vinegar.
    - Sealing changes no item count.
  - Automation:
    - The Cooking Pot already takes hoppers, so preserves can be cooked automatically.
    - The kettle and shelf are filled and emptied by hand.

- **Crows:**
  - Costs: a scarecrow is a hay bale, a wool and three sticks; a head is a pumpkin (carved, jack o'lantern, or hand-carved and lit).
  - Units: blocks across and growth stages.
  - A raid sets a ripe crop back three growth stages (wheat from 7 back to 4: three sevenths of its growing). A crow pecks for 40 ticks, then rests 600 to 1,200 ticks, so it raids once or twice a minute; a flock of three, three to six crops a minute, if no one is near and no scarecrow guards them.
  - Guarded area: 4 blocks across (about 50 blocks of field), 8 (about 200) and 12 (about 450).
  - **No positive-gain loop:** crows don't breed and aren't bred; a crow gives 0 to 2 feathers once and nothing else; feathers make no crows.
  - Automation: a scarecrow works on its own, all day, and needs nothing.

- **Fireworks:**
  - Costs: three rockets from a paper, 1 to 3 gunpowder and the picture's ingredients (as vanilla's three rockets from a paper and gunpowder), and a glowstone dust to twinkle. The launcher: a dispenser, three iron ingots, four planks and a redstone.
  - Units: flight in ticks: 10 for each flight level plus one, and up to 12 more (flight 1: 20 to 32 ticks).
  - A burst is one picture of 87 to 117 sparks and sixteen loose ones, each lasting two to three seconds.
  - Nothing is made back from a rocket: it is gone when it bursts. No damage, so no use as a weapon.
  - Automation: dispensers and the launcher both fire them, by redstone; hoppers load the launcher.

- **Sky lanterns:**
  - Costs: two lanterns from five paper, two string and a candle; a dye to colour one. A festival's eight lanterns are four crafts: 20 paper, 8 string and 4 candles.
  - Units: a lantern rises 0.035 blocks a tick and drifts 0.015, the wind's direction turning full circle every 72,000 ticks; it burns 2,400 to 3,000 ticks and dims over the last 100.
  - The festival: 8 lanterns within 32 blocks in 2,400 ticks; Luck for 6,000 ticks; one festival a place a day (24,000 ticks).
  - Nothing is made back: a lantern is gone when it burns out, torn or not. The reward is an effect and an advancement, never an item, so there is no loop.
- **Mooncakes:**
  - Costs: four cakes from two wheat, a sugar, an egg and the filling, in 300 ticks in the Cooking Pot.
  - 3 food each, saturation modifier 0.6. Luck for 6,000 ticks, only outdoors on a full-moon night: one night in eight.
- **The feast table:**
  - Costs: two lengths from three wooden slabs and two logs. Every blessing costs a serving: one food item eaten.
  - Units: a dish holds 8 servings; a table counts at most 8 lengths (16 dishes) from where you eat; diners count for 2,400 ticks after they last ate; the blessing reaches 16 blocks.
  - Tiers by score: 3, 5, 8 and 11. Regeneration I for 200 ticks; Absorption I for 2,400; Haste I, Luck and Health Boost I for 6,000. A higher tier gives the lower tiers' effects too, and another serving refreshes them; they don't stack in strength.
  - Unlike a beacon, the feast uses food for every blessing, and its effects end. Nothing is made: a serving is eaten, and its container (a bowl, a bottle) comes back as it would. No loop.
- **The corn maze:**
  - Costs: the gate from a hay bale and four sticks; a kernel a stalk of wall (30, 70, 126 or 198 on clear ground). The finish post drops nothing.
  - Units: mazes of 3, 5, 7 and 9 cells across (7, 11, 15 and 19 blocks); planting 32 stalks a tick; a run at most 12,000 ticks; a shortcut is a run that walked less than 0.8 of the shortest way through, in blocks across the ground.
  - Return: each stalk gives back its one kernel from its bottom, and nothing else. A ribbon comes once for each runner on each gate (the gate remembers the last 64), so running a maze again only betters a time. No loop.
- **Ghost hunting:**
  - Costs: the lantern from three gold nuggets, two glass panes, an amethyst shard and a candle; a glass bottle a catch (it comes back when the Ectoplasm is stirred into wax).
  - Units: a grave stirs on 1 random tick in 4, at night only; at most 3 spirits within 16 blocks; a spirit's haunt is 6 blocks across and 3 up; it drifts at 0.04 blocks a tick and shies at 0.12 (a walking player covers about 0.22); a lantern reveals within 12 blocks, looking every 10 ticks, each look lasting 40.
  - Yield: one Ectoplasm a spirit; one Ectoplasm scents a pot of wax, as any scent item does. A graveyard of a dozen graves gives about two or three spirits a minute at night while it has room for them; a single grave about two a night.
  - Nothing is made from nothing: a spirit needs a grave and a night, a catch needs a bottle, and Ectoplasm only scents wax. No loop.
- **Face paint:**
  - Costs: a kit from a bowl and four dyes, good for 16 faces.
  - Units: painting your own face takes 32 ticks; the server looks for painted faces under water every 20 ticks.
  - Face paint makes nothing and gives no effect: it is a costume, and the costume rules' rewards (one treat a home a night, a contest ribbon by vote) are unchanged. No loop.

## Multiplayer and persistence
- **Server authority:**
  - Putting things in the pot, dipping, pouring, lighting and snuffing all go through vanilla's block use path (reach, spawn protection, adventure mode) and are decided on the server.
  - The server checks the wax kind and the pot's room, that the wax is molten, the two-scent limit, and whether brightener or extender is already in.
  - The cooldown ("still warm") is the server's own record of the player's item cooldowns. A client can't dip faster, or keep a warm layer, by claiming otherwise.
- **The aura is worked out on the server** at each pulse, from the candle's saved mixture, over a bounded box. Clients are sent the candle's mixture (to draw its colour, height and flame) and a block event for the ring; they decide nothing.
- **Concurrent use:** two players at one pot each take their own measure; the pot's room and molten count are checked on every use. The cooldown is shared by all of a player's Aura Candles (it is per item), so a player dips one candle at a time.
- **Persistence:**
  - The pot saves its wax, set and molten measures, melting progress, dye sums, scents, brightener and extender.
  - A candle saves its mixture (`mix`: wax, layers, colour, scents, bright, lasting, burn and burned) and keeps it as the item component `jugcraft:candle` when it is broken, along with its colour and name.
  - A pot's wax is lost when the pot is broken.
- **Chunk unload:** a candle in an unloaded chunk doesn't tick, so it neither burns nor pulses until the chunk loads again. No chunk loading.
- **Bounded work:**
  - An unlit candle doesn't tick.
  - A lit one counts down each tick and pulses every 80 ticks: one entity query over at most a 33-block box, plus at most 128 block lookups for a bright harvest candle.
  - A pot ticks only while it holds wax.
- **IDs:**
  - Blocks with items: `jugcraft:wax_melting_pot`, `jugcraft:aura_candle`.
  - Block entities of the same names.
  - The data component `jugcraft:candle`.
  - Item tags: `jugcraft:candle_wax/beeswax`, `jugcraft:candle_wax/tallow`, `jugcraft:candle_scents/<scent>` (eleven), `jugcraft:candle_brighteners`, `jugcraft:candle_extenders`.
  - All are new; nothing earlier is renamed.
- **Disable behaviour:** with the agriculture feature disabled the pot's recipe doesn't load; the blocks, items, component and block entities stay registered, so placed pots and candles stay in the world.

- **Cider mill, server authority:**
  - Putting apples in, cranking, turning the screw, bottling, filling and drawing the barrel go through vanilla's block use path (reach, spawn protection, adventure mode) and are decided on the server.
  - The press checks room, that it isn't mid-pressing, that the trough has room for the turn, and its own pacing. The pacing is per press, not per player: two players can't grind faster than one.
  - Picking apples goes through the same path, as the chestnut's does.
- **Cider mill, persistence:**
  - The press saves its apples, pulp, turns, cheese and juice, and sends them to clients for the renderer (they decide nothing).
  - The barrel saves its servings and the game time its batch started; a broken barrel keeps both as the item component `jugcraft:barrel_cider`.
  - Ageing is worked out from the world's game time, which every dimension shares. So a barrel ages while unloaded or carried, and needs no ticking; it only looks every 20 ticks to change its chalk mark (one comparison).
- **Cider mill, IDs:**
  - blocks with items `apple_leaves`, `cider_press` and `cider_barrel`; the block `apple_sapling` (planted from `apple_seeds`);
  - items `apple_seeds`, `apple_pomace`, `sweet_cider`, `sparkling_cider`, `aged_cider`, `mulled_cider`, `mulling_spices` and `apple_cider_donut`;
  - block entities `cider_press` and `cider_barrel`; the data component `jugcraft:barrel_cider`;
  - item tags `jugcraft:cider_apples` and `c:seeds/apple`;
  - worldgen feature `jugcraft:apple_tree` and placed feature `jugcraft:patch_apple_tree`;
  - the Cooking Pot recipe `jugcraft:pot_cooking/mulled_cider`.
  - All are new. The chestnut leaves' fruiting moved into the shared `FruitingLeavesBlock` with no change of ID, state or behaviour (the festival tests still check it).
- **Cider mill, disable behaviour:** with the agriculture feature off, the recipes don't load and wild apple trees don't generate; the blocks, items, component and block entities stay registered.

- **Pantry, server authority:**
  - Filling, adding jars, lifting them out, draining, and using the shelf go through vanilla's block use path and are decided on the server.
  - The kettle checks for water, room, and that a jar is full and unsealed.
  - Spoiling is checked on the server when a serving is eaten, from the jar's stored time and the world's game time; a client can't freshen a jar.
- **Pantry, persistence:**
  - The kettle saves its water, heat, jars and how long each has been processed. The shelf saves its jars. Both send them to clients to draw, and both spill their jars when broken.
  - A jar's state is two components: `jugcraft:sealed` (a sealed jar) and `jugcraft:jar_contents` (servings left and when it was cooked or opened).
  - The Cooking Pot stamps a cooked jar with the game time. That is the only change to the pot: it builds the meal before using the ingredients, so a stamped jar that can't stack in the result slots waits rather than being lost.
- **Pantry, IDs:**
  - blocks with items `canning_kettle` and `pantry_shelf`, with block entities of the same names;
  - items `mason_jar`, `cider_vinegar`, `sweet_berry_jam`, `apple_butter`, `pumpkin_butter`, `cranberry_preserves`, `glow_berry_jelly`, `pickled_beets`, `pickled_peppers` and `corn_relish`;
  - data components `jugcraft:sealed` and `jugcraft:jar_contents`;
  - Cooking Pot recipes `pot_cooking/<preserve>` and `pot_cooking/cider_vinegar`.
  - All are new.
- **Pantry, disable behaviour:** with the agriculture feature off, the recipes don't load; blocks, items and components stay registered.

- **Crows, server authority:** crows are server-side mobs; where they fly, which crop they pick, their pecking, fleeing and leaving, and the scarecrow checks are all decided on the server. Clients are sent the crow's position and whether it is pecking, to draw it; they decide nothing.
- **Crows, persistence:**
  - Crows are ambient mobs, as bats are: they despawn when no player is within 128 blocks, and in time when none is within 32. A crow's crop, rest and fleeing aren't saved; a crow loaded again just looks again.
  - A crop changes only when a crow standing on it has pecked for two seconds, in a loaded chunk.
  - Scarecrows save nothing new; a scarecrow's guard comes from its head, which it already saved.
- **Crows, bounded work:**
  - The spawner runs every 200 ticks: for each player, one entity count and at most 16 heightmap lookups.
  - A crow looks for a crop every 20 ticks while not resting: 24 heightmap lookups within 12 blocks.
  - Each crop it considers, and the crop it is after every 10 ticks, is checked against scarecrows: the block entities of the loaded chunks within 12 blocks (at most nine chunks).
  - Nothing loads a chunk (`isLoaded`, `getChunkNow`).
- **Crows, IDs:** the entity `jugcraft:crow`, its loot table `jugcraft:entities/crow` and texture `jugcraft:textures/entity/crow.png`. All new; the scarecrow keeps its ID and states.
- **Crows, disable behaviour:** with the agriculture feature off, no crows come, and crows already about fly off as at nightfall; the entity stays registered, so a saved crow loads.

- **Fireworks, server authority:** the server spawns, moves and bursts every rocket, and runs every show (loading, starting and stopping go through vanilla's block use path or redstone). A burst is one small message to the players who can see the rocket (`jugcraft:spooky_burst`: where, which picture, whether it twinkles); each client draws it. Clients decide nothing.
- **Fireworks, persistence:** a rocket saves its flight and how long it has flown; the launcher saves its tubes, whether a show is running and where it is in it.
- **Fireworks, bounded work:** a launcher ticks only while a show runs; a rocket ticks as any projectile; a burst is drawn on the client from one message.
- **Fireworks, IDs:** items `bat_firework`, `pumpkin_firework`, `ghost_firework` and `skull_firework`; block with item `show_launcher` and its block entity; entity `spooky_rocket`; particle `jugcraft:spooky_spark`; data component `jugcraft:twinkle`; the network message `jugcraft:spooky_burst`; 24 rocket recipes and the launcher's. All new.
- **Fireworks, disable behaviour:** with the agriculture feature off, the recipes don't load; everything stays registered.
- **Lanterns, server authority:** the server lets every lantern go (through vanilla's item use path, using one up), moves it and burns it out. The festival is counted on the server from its own record of releases, and the Luck, message and advancement are given there. A mooncake's Luck is decided on the server from the Overworld clock and whether the sky is open above the eater. Clients are sent a lantern's position, colour and when it burns out, to draw it; they decide nothing.
- **Lanterns, persistence:** a lantern saves its colour and when it burns out; its wish is its name. The festival's count is kept in memory only (at most 256 recent releases, and the last day's festivals): a restart forgets it, so a festival under way starts its count again, and a place could hold a second festival the same day after a restart.
- **Lanterns, bounded work:** a lantern ticks like any entity, one fixed step a tick, and is tracked by clients within 10 chunks. Each release looks once through at most 256 remembered releases. Nothing loads a chunk.
- **Lanterns, IDs:** item `sky_lantern` and entity `sky_lantern`; items `red_bean_mooncake`, `chestnut_mooncake` and `pumpkin_mooncake`; advancement `lantern_festival`; the lantern's recipe and the three Cooking Pot recipes. All new.
- **Lanterns, disable behaviour:** with the agriculture feature off, the recipes don't load; lanterns already made can still be let go, and everything stays registered.
- **Feast, server authority:** serving, eating and taking a dish back go through vanilla's block use path (reach, spawn protection, adventure mode) and are decided on the server. The server checks that the item is food, that the dish is empty or holds the same food and has room, and that a dish eaten from isn't empty. The score, the diners and the blessing are worked out on the server; a diner is blessed only if alive and within 16 blocks of the table. Clients are sent the dishes, to draw them; they decide nothing.
- **Feast, concurrent use:** two players eating from one dish each take their own serving, one after the other on the server; the last serving goes to whoever's use arrives first.
- **Feast, persistence:** each length saves its two dishes. Who has eaten there is kept in memory only: a restart forgets the diners, never the dishes.
- **Feast, bounded work:** a table never ticks. A meal walks at most 8 lengths of table and looks through their recent diners once.
- **Feast, IDs:** block with item `feast_table` and its block entity; advancement `harvest_home`; the table's recipe. All new.
- **Feast, disable behaviour:** with the agriculture feature off, the recipe doesn't load; tables already placed still serve and feast, and everything stays registered.
- **Maze, server authority:** choosing the size and planting go through vanilla's block use path and are decided on the server, which takes the kernels, carves the maze and plants it. The server starts, follows and finishes every run from the positions it already has for its players; clients claim nothing. A run is void for flying, gliding, spectating, climbing over the corn, leaving the maze, taking too long or a shortcut. Times are counted in the server's game ticks.
- **Maze, concurrent use:** up to sixteen runners at once, each timed alone; each runner's best counts once on the board.
- **Maze, persistence:** the gate saves its size, seed, whether it is planted, its maze's shortest way, any stalks still to plant, its board and who has had a ribbon (the last 64). The maze itself is blocks in the world. Runs in progress are kept in memory only, so a restart ends them.
- **Maze, bounded work:** a gate plants at most 32 stalks a tick. A planted gate looks for players in its own square each tick and, while anyone runs, in the finish post's square and at its runners (at most sixteen). The maze's plan is carved once and kept. Nothing loads a chunk.
- **Maze, IDs:** blocks `corn_maze_gate` (with its item and block entity), `corn_maze_finish` and `maze_corn` (no items); advancement `amazing`; the gate's recipe. All new.
- **Maze, disable behaviour:** with the agriculture feature off, the recipe doesn't load; mazes already planted still run, and everything stays registered.
- **Ghosts, server authority:** spirits rise, drift, shy, fade and are caught on the server. Whether a spirit is revealed is the server's own reckoning (a player it can see holding a lantern within 12 blocks, or the Glowing effect); clients are sent when it was revealed and until when, and draw it accordingly. A catch goes through vanilla's entity interaction (reach checked by the server) and needs the spirit revealed on the server; a client can't catch, or see, a spirit the server hasn't revealed by claiming to.
- **Ghosts, persistence:** a spirit saves its grave; its being revealed is not saved and is looked for again. Spirits are ambient creatures: they despawn when no player is near, like bats.
- **Ghosts, bounded work:** graves stir only on vanilla's random ticks, which reach only chunks near players; each stir counts the spirits within 16 blocks once. A spirit looks for a lantern every 10 ticks (one search among the level's players). Nothing loads a chunk.
- **Ghosts, IDs:** entity `restless_spirit`; items `spirit_lantern` and `ectoplasm`; the item tag `jugcraft:candle_scents/ghostly`; advancement `ghost_hunter`; the lantern's recipe. All new. The gravestones and the grave mound keep their IDs; they only gain random ticks.
- **Ghosts, disable behaviour:** with the agriculture feature off, graves raise no spirits, spirits about fade, and the lantern's recipe doesn't load; everything stays registered.
- **Face paint, server authority:** painting goes through vanilla's item use and entity interaction (reach checked by the server); turning the dial changes the kit on the server. The paint is a Fabric data attachment the server sets and clears; the server sends it to every client that can see the player, and clients only draw it. Whether a player is in costume is the server's own check.
- **Face paint, persistence:** the paint is saved with the player and kept across a restart; it washes off under water, and is not kept at death. The kit saves its design as a data component.
- **Face paint, bounded work:** every 20 ticks the server looks at each online player once (a water check for the painted ones). Each client finds a painted player among the level's players when drawing them.
- **Face paint, IDs:** item `face_paint_kit`; data component `jugcraft:face_paint_design`; attachment `jugcraft:face_paint`; advancement `face_painter`; the kit's recipe. All new.
- **Face paint, disable behaviour:** with the agriculture feature off, the kit's recipe doesn't load; kits already made still paint, paint already on stays, and everything stays registered.

## Dependencies and assets
No new dependencies. Every texture is drawn by code (`tools/chandlery_textures.py`): the pot's hammered copper and dark inside, the brass dish, the wax (pale, tinted by its colour as it is drawn), the wax's surface in the pot, the flame (white at its heart, tinted by its scent) and the candle's item in two layers (its body, tinted by its dyed colour; its wick and dish, not). The models, blockstates, item model, names, tooltip, messages, loot and tags come from `tools/chandlery_data.py`; the numbers from `CHANDLERY` in `tools/agriculture.py`. The client's `WaxPotRenderer` draws the wax in the pot at its level and colour; `AuraCandleRenderer` draws the candle at its height, layers and colour and its flame; both share `TintedBoxes`. The item's colour is the vanilla `dyed_color` component, read by the item model's dye tint. Sounds are vanilla's (honeycomb waxing, dye use, brewing, a bottle filling, a honey slide, a bucket emptying, a candle going out).

The cider mill:
- Textures are drawn by code in `tools/cider_textures.py`: the leaves at each stage, the sapling, the press's oak, slats and iron, the barrel's staves along and across, its head with each chalk mark, its brass, the pulp, juice and apple skin for the renderer, and the items. The bottles share the treats' bottle shape.
- The models, blockstates, loot, tags, worldgen and words come from `tools/cider_data.py`; the numbers from `CIDER` in `tools/agriculture.py`.
- The client's `CiderPressRenderer` draws the press's apples, pulp, plate, screw, capstan bar and juice (with `TintedBoxes`). The barrel is a block model with a variant for each chalk mark.
- Sounds are vanilla's (an item frame's click, a grindstone, a trapdoor's creak, a bucket, bottles).

The pantry:
- Textures are drawn by code in `tools/pantry_textures.py`: the kettle's speckled enamel, inside and rack, the shelf's oak and beadboard, the water, a jar's glass, lid and gingham, and each preserve's jar, plain and sealed.
- Models, the items' sealed-or-not models (a `minecraft:condition` on `jugcraft:sealed`), loot, tags and words come from `tools/pantry_data.py`; the numbers from `PANTRY` in `tools/agriculture.py`.
- The client's `CanningKettleRenderer` and `PantryShelfRenderer` draw the water and jars (with `PreserveJars` and `TintedBoxes`).
- Sounds are vanilla's (buckets, bottles, a chiseled bookshelf's slot, eating).

Crows:
- The texture is drawn by code in `tools/crow_textures.py`: glossy blue-black feathers with a lighter sheen along the wings' edges, a dark grey beak and feet, and pale yellow eyes.
- Its name and loot come from `tools/crow_data.py`; the numbers from `CROWS` in `tools/agriculture.py`.
- The client's `CrowModel` (head and beak, body, two wings that spread and beat in flight and fold while it pecks, a fanned tail, legs) and `CrowRenderer` draw it.
- Sounds are vanilla's: a parrot's call, pitched down, for its caw; crops breaking when it has pecked.

Fireworks:
- Textures are drawn by code in `tools/firework_textures.py`: each rocket's item, the launcher's stained crate with iron corners, its painted tubes and their mouths, the brass dial at each mode, a rocket's paper, and the soft star of a spark.
- The pictures are pixel pictures in `FireworkShape.java`, which the client reads to draw a burst (`SpookyBursts`, `SpookySparkParticle`). The launcher's loaded rockets are drawn by `ShowLauncherRenderer`; a rocket in flight by vanilla's thrown-item renderer.
- Models, blockstates, recipes, loot, tags and words come from `tools/firework_data.py`; the numbers from `FIREWORKS` in `tools/agriculture.py`.
- Sounds are vanilla's fireworks' (launch, blast near and far, twinkle).

Sky lanterns:
- Textures are drawn by code in `tools/lantern_textures.py`: the lantern's item in two layers (its paper, pale so the dye tints it, and, untinted, its bamboo ring and flame), the rice paper the client wraps a lantern in, and each mooncake (a scalloped golden cake pressed with a flower, a wedge cut away to show its filling).
- A lantern in flight is drawn by `SkyLanternRenderer`: its paper body on its ring, glowing at full brightness and flickering, dimming as it burns out.
- Models, the recipe, the dyeing recipe (Minecraft 26.3's `minecraft:crafting_dye`, as leather's), the `minecraft:cauldron_can_remove_dye` tag and words come from `tools/lantern_data.py`; the mooncakes' Cooking Pot recipes from `POT_RECIPES`; the numbers from `LANTERNS` in `tools/agriculture.py`.
- Sounds are vanilla's (flint and steel to light one, a fire going out when one is torn).

The feast table:
- Textures are drawn by code in `tools/feast_textures.py`: the table's honey-oak top, its darker trestle wood, its orange runner with a border of little leaves, and the cream plate each dish sits on.
- The dishes are drawn by `FeastTableRenderer`: each on its plate on the runner, the food lying on it, one, two or three pieces high as the servings run down.
- Models (a length on its own, at the start, in the middle and at the end, along either axis), blockstates, loot, the recipe, the advancement and words come from `tools/feast_data.py`; the numbers from `FEAST` in `tools/agriculture.py`.
- Sounds are vanilla's (a decorated pot's when a dish is served).

The corn maze:
- Textures are drawn by code in `tools/maze_textures.py`: the gate's weathered posts, its green pennant with a white arrow, and the finish post's chequered flag. The posts' hay wraps use vanilla's hay bale texture by reference (not copied), and maze corn uses ripe corn's own models.
- Models, blockstates, the gate's item, loot (maze corn gives back its kernel from the bottom; the finish post gives nothing), the recipe, tags and words come from `tools/maze_data.py`; the numbers from `MAZE` in `tools/agriculture.py`.
- Sounds are vanilla's (crops planted, a level-up chime at the finish).

Ghost hunting:
- Textures are drawn by code in `tools/ghost_textures.py`: the Spirit Lantern, Ectoplasm, and the pale stuff a spirit is shaped from.
- A spirit is drawn by `RestlessSpiritRenderer`: see-through boxes for its hood, robe, reaching arms and swaying tail, with hollow eyes and mouth, glowing faintly, bobbing, and fading in and out as it is revealed and hidden.
- Item models, the lantern's recipe and words come from `tools/ghost_data.py`; the Ghostly scent's tag from `CHANDLERY`; the numbers from `GHOSTS` in `tools/agriculture.py`.
- Sounds are vanilla's (an amethyst chime when one is revealed, a ghast's moan pitched up, a bottle filling).

Face paint:
- Textures are drawn by code in `tools/face_paint_textures.py`: the kit, and each design as a 16x16 laid over the face's 8x8, see-through where the skin shows.
- The paint is drawn by `FacePaintLayer`, added to the player renderers: one quad a hair in front of the head's front, posed with the head.
- The kit's item model, recipe and words come from `tools/face_paint_data.py`; the numbers from `FACE_PAINT` in `tools/agriculture.py`.
- Sounds are vanilla's (dye being used, a bucket emptying when paint washes off).

## Verification
### Chandlery verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-15` stacked on batch 14:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the chandlery's numbers, waxes and scents with Java: capacity, timings, scent limit, burn factors, layers, pulse and effect time, harvest divisor, radii and light by layers, each wax's measures, burn and colour, each scent's effect and colour; checks every wax and scent tag, and the brightener and extender tags, hold `tools/agriculture.py`'s items; that the candle's blockstate has a variant for every state; and that every pot message, candle tooltip, candle name, wax and scent has its words) | Pass, 581 IDs |
| `./gradlew build` on `1cdcf09` (Build workflow run 37021364812) | Pass on its second attempt (see the next row) |
| Game tests on the headless server, same run: 336 in total, 8 of them new here (`ChandleryGameTests`) | **All 336 pass** on the second attempt. The first attempt failed one test from `main`, `PetroGameTests.heliostatsHeatASolarReceiver` ("The receiver made 48 JE/t, expected 36 on tick 25"), which this branch doesn't touch; the same tests had all passed on `f56071b` (run 37020041265), and the failed job was re-run once |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `1cdcf09`, with the screenshots above. No model or texture errors for the chandlery in the log |

The 8 new game tests (`ChandleryGameTests`):
1. honeycomb melts over a campfire and stays set on stone; a pot of beeswax takes no tallow;
2. molten wax takes red, then blue dye (and mixes them), two scents but not a third, glowstone dust and redstone once each;
3. string starts a scented one-layer candle and uses a measure; dipped again while warm, the layer slides off and its wax is lost; a cool candle gains a layer and its burn time adds up; layering a third scent muddles a candle;
4. a pulse gives players within five blocks both effects and a player six blocks off none; a bright candle gives Speed II; a muddled candle gives nothing;
5. warding slows and weakens a zombie; revealing makes the zombie glow, but not a player;
6. a bright harvest candle's pulses grow the wheat round it;
7. flint and steel lights a candle (light 8 for one layer); a lit candle burns down and goes out for good, its block entity with it; broken part-burned, a candle drops itself with its mixture and burn;
8. the pot's recipe and both loot tables load.

Found by CI and fixed before this record:
- 26.3's `LivingEntity.drop` takes a third argument; a candle that doesn't fit in the inventory pops out of the pot instead (`e61fa32`).
- 26.3 has no `RenderTypes.entityCutoutNoCull`; the renderers use `entityCutout`, wind each face to face along its normal, and draw a flame's plane from both sides (`7ee4afb`).
- 26.3's entity types are constants of `EntityTypes`, not `EntityType` (`5935bc2`).
- A test swapped the glowstone dust in hand for redstone before counting it (a test bug, not the pot's); each is now checked while it is held (`f56071b`).
- The first screenshots showed the flames as thin slivers in the scent's colour, lost against wax of the same colour; they are now about twice the size with a warm-white heart (`1cdcf09`).

**Not run:**
- a person playing it in a client: dipping by hand, watching the cooldown, walking in and out of an aura;
- a dedicated server with two players at one pot, or in one aura;
- how long a harvest candle takes to grow a real farm (the test drives 400 pulses at once);
- the sounds (the CI client has no sound device).

### Cider mill verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-16` stacked on the chandlery:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the cider mill with Java: the apple leaves' fruiting and picking, the press's capacity, trough, turns, pacing and pomace, the barrel's capacity, ageing times and stages; checks the apple tag, the press's, barrel's and leaves' block states, the barrel's chalk-mark states, every message, the barrel's loot keeping its cider, the apple tree's biomes and that apple seeds plant a known sapling; and refuses a drink that gives its bottle back but cooks into another bottled drink) | Pass, 593 IDs |
| `./gradlew build` on `c01a4a2` (Build workflow run 37025266440) | Pass |
| Game tests on the headless server, same run: 345 in total, 9 of them new here (`CiderGameTests`) | **All 345 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `c01a4a2`, with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-cider-mill). No model or texture errors for the cider mill in the log. Like every Jugcraft machine and Cooking Pot recipe, the mulled cider recipe is logged as one the vanilla recipe book can't place (the recipe book only knows vanilla recipe types) |

The 9 new game tests (`CiderGameTests`):
1. an apple sapling grows a tree: an oak trunk and a crown of apple leaves;
2. tree-grown leaves blossom and ripen; placed leaves don't; picking drops apples and leaves bare leaves;
3. eight apples go in and not a ninth; a crank turn grinds one, a second at once does nothing, and once free the crank grinds another;
4. four screw turns press a full cheese: a quarter of the juice each, no apples in while pressing, four pomace knocked out, the screw free again; a bottle draws sweet cider; comparators read the juice;
5. a cheese of three apples gives exactly three servings and two pomace;
6. sweet cider goes into a barrel and the bottles come back; after a day it is sparkling (the chalk mark follows) and takes no fresh juice; a bottle draws sparkling cider; after three days it is aged; broken, it keeps its servings and start time;
7. topping up a sweet batch starts its day again;
8. every cider is drunk on a full stomach, leaves its bottle and gives its effect; sweet cider gives its bottle back in a recipe and sparkling cider doesn't;
9. the recipes (mulled cider in the Cooking Pot among them), loot tables and the apple tree's placed feature load, and sparkling cider with spices finds the mulled cider recipe.

Found by CI and fixed before this record:
- The press started with its last work at `Long.MIN_VALUE`, so "now minus then" overflowed and a new press never turned its crank. The first run's `pressGrindsApplesIntoPulp` caught it (344 of 345 passed); it now starts a work interval in the past (`3d1751a`).
- The first screenshots showed the barrels as square crates; three stepped boxes now round their profile (`c01a4a2`).

**Not run (cider mill):**
- a person playing it in a client: grinding and pressing by hand, drinking;
- a dedicated server with two players at one press;
- a real day of ageing (the tests set the barrel's start time back);
- wild apple trees in a new world (the test grows one from a sapling, and checks the placed feature loads);
- the sounds.

### Pantry verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-17` stacked on the cider mill:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the pantry with Java: servings, spoiling time, the kettle's jars and timings, the shelf's slots, every preserve's food, effect and colour; checks that every preserve is cooked into one Mason Jar in the Cooking Pot and that its item model switches on `jugcraft:sealed`, that cider vinegar is cooked and gives its bottle back, and every message and tooltip; and reads condition item models) | Pass, 605 IDs |
| `./gradlew build` on `b801521` (Build workflow run 37030026432) | Pass |
| Game tests on the headless server, same run: 353 in total, 8 of them new here (`PantryGameTests`) | **All 353 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `b801521`, with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-preserves-pantry). No model or texture errors for the pantry in the log; its Cooking Pot recipes are logged as ones the vanilla recipe book can't place, like every Cooking Pot recipe |

Found by CI and fixed before this record:
- Two blocks in one registration method shared a variable name (a compile error) (`6bcec72`).
- Holding anything but a jar, using the shelf fell through to the empty-hand action, so a stick took a jar down; the shelf and kettle now answer only an empty hand. `pantryShelfHoldsSixJars` caught it (352 of 353 passed) (`55ff342`).
- The first screenshots showed the kettle under a cloud of steam; the steam is now a few small wisps (`b801521`).

The 8 new game tests (`PantryGameTests`):
1. jam cooks into a jar in the Cooking Pot, full, unsealed and stamped with when it was cooked; beets pickle in cider vinegar, and the vinegar's bottle goes to a result slot, not back among the ingredients;
2. a jar is eaten a serving at a time (2 food each for the jelly), the last leaving the empty jar; the jelly gives Night Vision;
3. an unsealed jar over three days old has spoiled (1 food, Hunger and Nausea); a sealed jar of the same age is fine, and opening it starts its days;
4. sealed jars of a preserve stack, unsealed jars from different batches don't;
5. the kettle takes no jar without water, fills from a water bucket, refuses an opened jar, takes fresh ones; at the boil both seal after twenty seconds; comparators read them; an empty hand lifts them out sealed;
6. off the heat the kettle stops boiling and nothing seals; sneaking lifts the jar out unsealed;
7. a shelf holds six jars and refuses a seventh and a stick; a full shelf reads 15; an empty hand takes one down; broken, it spills the rest;
8. the recipes (every preserve and the vinegar in the Cooking Pot) and loot tables load.

**Not run (pantry):**
- a person playing it in a client: cooking, canning and eating;
- a dedicated server with two players at one kettle;
- a real three days of spoiling (the tests set the time back);
- the sounds.

### Crows verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-18` stacked on the preserves pantry:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the crows and scarecrows with Java: the crow's flight, fleeing, raiding, pecking, setback, rest, nightfall and look intervals, the spawner's timing, distances, flock and caps, the scarecrows' guard radii and height, the crow's health; checks the crow is registered with its attributes, named, and drops feathers) | Pass, 605 IDs (crows add an entity, not an item or block) |
| `./gradlew build` on `71d42ef` (Build workflow run 37037813700) | Pass |
| Game tests on the headless server, same run: 361 in total, 8 of them new here (`CrowGameTests`) | **All 361 pass**. They also all passed on `c2d093b` (run 37033625474) and `85e6c36` (run 37036340295). Run 37034790416, on a commit that changed only the client test, failed once on `main`'s `PetroGameTests.heliostatsHeatASolarReceiver` ("The receiver made 48 JE/t, expected 36 on tick 25"), which this branch doesn't touch; its re-run passed |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `71d42ef` (run 37037813700), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#crows-and-working-scarecrows): five crows on the client, one of them pecking for real. No model or texture errors for the crow in the log |

The 8 new game tests (`CrowGameTests`):
1. a bare scarecrow guards 4 blocks across and not 5, and 6 blocks up or down but not 7; wearing a carved pumpkin, 8 and not 9; wearing a jack o'lantern, 12;
2. sent after ripe wheat, a crow lands and pecks; halfway through, the wheat is still ripe; after two seconds it is three stages back and the crow is done, and won't go for the unripe wheat;
3. with its own AI (by day), a crow three blocks above flies down to the wheat it was sent after and pecks it back;
4. a scarecrow going up beside the wheat a crow is pecking sends it off within half a second, the wheat spared; no other crow goes after a guarded crop;
5. a sneaking player four blocks off doesn't scare a pecking crow; standing up, the player does, and it flies away from them; with no one near another crow pecks, and a blow sends it off;
6. at nightfall a crow leaves its crop and climbs, and is gone once high over the ground;
7. the spawner finds ripe wheat open to the sky, not under a glass roof, and brings a flock of two or three crows in the sky above it;
8. a crow killed by a player drops at most two feathers; its loot table loads.

Found by CI and fixed before this record:
- The spawner test's field was under the game test's barrier ceiling, which crows rightly took for a roof; the test now lifts it, and checks that a glass roof keeps crows off (`c2d093b`). The other tests passed first time.
- In the client test the scarecrow broke itself on farmland, which has no solid top, and the pecking crow was hidden in tall wheat; the scarecrow now stands on grass and the crow pecks carrots at the front of the patch (`0c18542`, `85e6c36`, `71d42ef`).

**Not run (crows):**
- a person playing it in a client: crows coming to a real farm, scaring them off, building scarecrows round a field;
- a dedicated server with two players;
- how often crows come to a real farm over a day (the tests call the spawner directly), or how much they set a farm back;
- the sounds.

### Fireworks verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-19` stacked on the crows:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the fireworks with Java: the rocket's flight base, spread and climb, the launcher's tubes, tube size, sequence and volley steps and both leans; checks every picture has its firework item, named and drawn, with a recipe for each flight, plain and twinkling, making three of that flight; the launcher's models for every facing and mode, its words, loot and recipe; the spark particle, the burst payload and the dispensing are registered) | Pass, 610 IDs |
| `./gradlew build` on `d035ac4` (Build workflow run 37038322733) | Pass |
| Game tests on the headless server, same run | **All pass**. On `f869aa1` (run 37037699738), the last commit to change the game tests, the count was 369, 8 of them new here (`FireworkGameTests`); `d035ac4` changes only the client test |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `d035ac4` (run 37038322733), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#spooky-fireworks): four pictures bursting at night, a finale, and a loaded launcher with its rockets' tips showing. No model, texture or particle errors in the log |

The 8 new game tests (`FireworkGameTests`):
1. every picture is well formed, at least 40 sparks, and has a firework that bursts into it with flight 1 when crafted plain; the jack o'lantern has its green stem and holes round it;
2. used on the ground, a ghost firework is used up and goes up alone, flies for its flight's time, is still climbing after half a second and bursts, breaking and hurting nothing;
3. flight 3 flies longer, a twinkling firework twinkles; all 24 firework recipes load, and paper, two gunpowder, a carved pumpkin and glowstone dust make three twinkling flight-2 jack o'lanterns; the launcher's recipe and loot table load;
4. the Show Launcher loads sixteen rockets to a tube, matching tubes first and vanilla rockets in their own; sticks don't load; hoppers load rockets from any side and unload only from below; comparators read the loaded tubes; a full launcher takes no more; broken, it spills every rocket;
5. in sequence a show fires one rocket every half second round the loaded tubes in turn and stops when they are empty;
6. in volleys it fires a row of three every second, front row first, a vanilla rocket in a row going up with it; a finale fires one from every tube at once, each leaning out from the middle, and stops;
7. a rising redstone signal starts a show and the next stops it, and so does an empty hand; sneaking turns the dial; a stick does nothing and an empty launcher won't start;
8. a dispenser facing up sets off a spooky firework.

Found by CI and fixed before this record:
- The spark particle overrode a light method 26.3 doesn't have; it now lights itself at full brightness through `getLightCoords` (`f2e6c83`).
- The recipe test assembled with the older two-argument `assemble`; 26.3's takes only the input (`6af3169`).
- Two mistakes in the tests themselves: the jack o'lantern's stem pixel was looked for in the wrong column, and the "not a rocket" check used dirt, which the use test placed as a block; it now uses a stick (`f869aa1`).
- The first screenshots were too far off to make out the pictures; the camera now stands closer to the launcher and the bursts (`d035ac4`).

**Not run (fireworks):**
- a person playing it in a client: crafting the fireworks, setting them off by hand, loading and running a show;
- a dedicated server with two players, each seeing the pictures face them;
- how a long show looks and runs with many launchers going at once;
- the sounds.

### Lanterns verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-20` stacked on spooky fireworks:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the lanterns with Java: the lantern's rise, wind, wind period, lifetime, spread and fade; the festival's count, radius, window, cooldown, Luck and memory; the mooncake's Luck and the night's start and end; checks the lantern is registered, named, dyeable and crafted two at a time, every mooncake is registered with its food and bakes four at a time in the Cooking Pot, and the festival has its message and advancement) | Pass, 614 IDs |
| `./gradlew build` on `53cbeef` (Build workflow run 37038755618) | Pass |
| Game tests on the headless server, same run: 373 in total, 4 of them new here (`LanternGameTests`) | **All 4 lantern tests pass.** One test failed, twice (the run and its one re-run): `main`'s `PetroGameTests.heliostatsHeatASolarReceiver` ("The receiver made 48 JE/t, expected 36 on tick 25"), which this branch doesn't touch and which fails the same way now and then on other branches. On `c3edfea` (run 37037176622) the lantern tests passed too; the only failures there were the two firework test mistakes since fixed on the fireworks branch (`f869aa1`) and merged here |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `53cbeef` (run 37038755618) and `c3edfea` (run 37037176622), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-sky-lantern-festival): eighteen lanterns in seven colours let go together at night, one carrying a wish, and the three mooncakes and a lantern in item frames |

The 4 new game tests (`LanternGameTests`):
1. used, a dyed, named lantern is used up and one lantern goes up, blue and carrying its wish, to burn for two minutes or so; a second later it has risen and drifted with the wind;
2. an undyed lantern is warm red with no wish; it burns bright and dims over its last five seconds; two let go apart drift exactly the same way, rising; one above the world is gone; a blow tears one and puts it out;
3. seven lanterns make no festival, nor an eighth let go too far off; the eighth near makes one, with Luck and A Sky Full of Wishes for the player near; the same place holds no second festival that day;
4. the first and ninth nights are full-moon nights, and noon, the second night and dawn aren't; the three mooncake recipes load; wheat, sugar, an egg and roasted chestnuts bake four chestnut mooncakes; a mooncake is three food.

Found by CI and fixed before this record: nothing in the lanterns; their tests passed on their first run.

Fixed later (3 October 2026): the lantern was dyed by listing it in `minecraft:dyeable`, which Minecraft 26.3 doesn't dye by (knitting found that leather isn't in it). It now has its own dyeing recipe, `sky_lantern_dyed` (`minecraft:crafting_dye`: the lantern and any dye give it back in that colour, as leather is dyed), and joins `minecraft:cauldron_can_remove_dye`, so a water cauldron washes it. The checker now looks for the recipe and the tag. A fifth test, `aLanternTakesDye`, shows a lantern and red dye make the same kind of recipe as a leather helmet and come out dyed, and a water cauldron washes the dye out. It passed in Build workflow run 37129429165 on `edceeff` (its second attempt; 571 server game tests in all, and the client tests). The first attempt failed only on `PixelHollowsGameTests.everyVillageHasOneShop`, a village test this fix doesn't touch, in which a taiga village got no shop.

**Not run (lanterns):**
- a person playing it in a client: letting lanterns go, watching a festival, eating a mooncake under a real full moon (the test checks the full-moon reckoning and the food, not the Luck from eating one at night);
- a dedicated server with two or more players letting lanterns go together;
- a server restart in the middle of a festival's count;
- the sounds.

### Feast verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-21` stacked on the sky lantern festival:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the feast with Java: the dishes, servings, diners' window and longest table; the four tiers, the effects' lengths and the reach; checks the table is registered and named, has a model for every axis and part, words for every tier, loot, a recipe making two, and its advancement) | Pass, 615 IDs |
| `./gradlew build` on `c892a9d` (Build workflow run 37038153230) | Pass |
| Game tests on the headless server, same run: 377 in total, 4 of them new here (`FeastGameTests`) | **All 377 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `c892a9d` (run 37038153230), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-harvest-feast-table): a four-length table set with eight foods between hay bale seats |

The 4 new game tests (`FeastGameTests`):
1. lengths end to end along one line join into a table with legs at its ends; a length across them stands alone;
2. food is served eight to a dish, one food to a dish, and a stick isn't served; a serving of bread feeds a hungry diner by five, and two dishes and one diner make a good meal (Regeneration, not yet Absorption); sneaking takes a dish back; a stew leaves its bowl; broken, the table spills its dishes;
3. at a table of six foods, the first diner has a feast (Absorption), a second makes it a harvest feast for both (Haste and Luck), and the fifth a grand feast for all five (Health Boost and Harvest Home);
4. the table's recipe and loot table load.

Found by CI and fixed before this record: nothing; the feast's tests passed on their first run.

**Not run (feast):**
- a person playing it in a client: setting a table, eating at it, a feast with friends;
- a dedicated server with two or more players eating together (the grand feast test uses five mock players on the headless server);
- a restart with diners counted;
- how it balances in play against a beacon or a potion.

### Maze verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-22` stacked on the Harvest Feast Table:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the maze with Java: the sizes in cells and their names, the planting rate, the longest run, the shortcut fraction and the most runners; checks the gate, finish post and maze corn are registered, with a model for every facing or section, words for every message and size, maze corn's loot giving back its kernel from the bottom only, and the gate's recipe and advancement) | Pass, 618 IDs |
| `./gradlew build` on `b0035e6` (Build workflow run 37041890514) | Pass |
| Game tests on the headless server, same run: 381 in total, 4 of them new here (`MazeGameTests`) | **All 381 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `b0035e6` (run 37041890514) and `62445da` (run 37040197910), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#the-corn-maze) from the latter: a medium maze planted by its gate, seen from above, and the gate between its walls |

The 4 new game tests (`MazeGameTests`), each planting a tiny maze in its own test area:
1. every size of maze has one way through and every open square reachable, and the same seed carves the same maze;
2. too few kernels plant nothing; enough plant the maze, a kernel a stalk: three-tall maze corn on every wall square, the way through left open, and the finish post at the exit; a planted maze isn't planted again;
3. a runner walked along the way through is timed (in about the time it took) and boarded in first place, with a first prize ribbon and A-maze-ing; one sent straight from the gate to the finish took a shortcut and isn't boarded; flying or leaving the maze voids a run;
4. maze corn stands three tall on solid ground and blocks the way; breaking the bottom brings the stalk down and gives back a kernel.

Found by CI and fixed before this record:
- Two calls 26.3 doesn't have: taking the kernels now goes through the inventory by hand (`Inventory.clearOrCountMatchingItems` takes other arguments in 26.3), and maze corn pops when pushed with `PushReaction.POPPED`, as 26.3 calls what was `DESTROY` (`62445da`).
- The run test looked for a "blue ribbon"; a winner gets the Harvest Scale's first prize ribbon (`b0035e6`).

**Not run (maze):**
- a person playing it in a client: planting a maze, finding the way through, racing friends;
- a dedicated server with two or more players running at once;
- a large maze planted over uneven or cluttered ground;
- a restart during a run (runs are in memory, so a restart ends them).

### Ghost hunting verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-23` stacked on the corn maze:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares ghost hunting with Java: the stir chance, the cap and its range, the lantern's reach, the haunt's size, the shy radii, both speeds and the reveal, look and fade times; checks the spirit is registered with its attributes and named, the lantern and Ectoplasm are registered, named and drawn, Ectoplasm gives its bottle back and is the Ghostly scent, the lantern has its recipe and tooltip, every grave takes random ticks and stirs, and Ghost Hunter exists; the chandlery check now includes the Ghostly scent) | Pass, 620 IDs |
| `./gradlew build` on `7a52c69` (Build workflow run 37043843402) | Pass |
| Game tests on the headless server, same run: 386 in total, 5 of them new here (`GhostGameTests`) | **All 386 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `7a52c69` (run 37043843402), with the screenshots in [AGRICULTURE.md](../branches/AGRICULTURE.md#ghost-hunting): four spirits revealed over a row of graves at night, and the hunter's kit with a lit Ghostly candle by day |

The 5 new game tests (`GhostGameTests`):
1. every grave takes random ticks; by day no spirit rises; at night spirits rise over a gravestone, each haunting it, unseen and out of any aim, until three are near; a grave mound stirs too;
2. hidden, a spirit can't be caught by a bottle; a Spirit Lantern held in the off hand fifteen blocks above doesn't reveal it, but held near it does, so it fades in (and fades out when no longer revealed) and can be aimed at; a glass bottle then catches it as Ectoplasm, one bottle of two, earning Ghost Hunter;
3. revealed, a spirit shies away from a player two blocks off but not from a sneaking one; hidden, it heeds no one; its haunt holds it within 6 blocks across and 3 up and never down into the ground; herded with its own AI, it shies a good way along, but no farther than its haunt's edge;
4. a blow passes through a spirit; at dawn it fades, leaving nothing; a Revealing candle's aura makes one glow, which reveals it;
5. Ectoplasm is the Ghostly scent, of invisibility; stirred into molten wax it scents it and gives its bottle back; a lit Ghostly candle turns the players near invisible; the Spirit Lantern's recipe loads.

Found by CI and fixed before this record: nothing; ghost hunting's tests and client test passed on their first run.

**Not run (ghost hunting):**
- a person playing it in a client: a night in a graveyard with a lantern, chasing and catching spirits, a Ghostly candle at a party;
- a dedicated server with two or more players, one holding the lantern;
- graves stirring by their own random ticks over a real night (the tests raise spirits directly);
- the sounds.

### Face paint verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-24` stacked on ghost hunting:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares face paint with Java: the designs in order, the kit's uses, the time to paint your own face and how often paint washes off; checks the kit is registered and worn by use, its design component and the paint attachment are registered and the attachment is sent to clients, every design has its name and texture, the kit has its texture, words, recipe and advancement, and trick-or-treating and the costume contest take a painted face as a costume) | Pass, 621 IDs |
| `./gradlew build` on `5cb4d12` (Build workflow run 37045137269) | Pass |
| Game tests on the headless server, same run: 389 in total, 3 of them new here (`FacePaintGameTests`) | **All 389 pass**, the existing trick-or-treat and costume contest tests among them |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `5cb4d12` (run 37045137269): the player, painted on the server with each design in turn, seen from the front, with the paint drawn on their face. The screenshots are in [AGRICULTURE.md](../branches/AGRICULTURE.md#face-paint) |

The 3 new game tests (`FacePaintGameTests`):
1. a bare face is no costume; used on a friend, the kit paints a skull at once, wears by one use and earns Face Painter; used sneaking it turns its dial to a jack o'lantern and paints nothing; that then paints over the skull; painting your own face takes 32 ticks, brushing, and at the end paints it; the kit paints no zombie; after its last face it is used up; its recipe loads;
2. paint stays on a dry face; standing in water two deep, the head is under and the paint washes off; the paint attachment is saved, and a design saved reads back the same;
3. bare-headed and unpainted, a player knocking at a door on a Halloween night gets "no costume"; painted, the costume rule passes and only the missing porch light turns them away.

Found by CI and fixed before this record: the player renderers are found by 26.3's `EntityTypes.PLAYER` (`EntityType.PLAYER` is gone there) (`5cb4d12`). The common code, the attachment included, compiled first time.

**Not run (face paint):**
- a person playing it in a client: painting a friend, painting your own face, washing it off in a lake;
- a dedicated server with two players, each seeing the other's paint (the client test paints its own player, on its own server);
- the paint after a restart or on rejoining (the tests check that the attachment is saved and its design reads back, not a real restart);
- trick-or-treating and the costume contest in play with a painted face.

## World and event applicability
- Candles and pots work anywhere, in every dimension, all year. Nothing is seasonal. The aura doesn't depend on biome, time or weather; harvest helps only plants that would grow there anyway.
- Revealing shows creatures through walls (Glowing), which can help find hostile mobs in caves; it gives no other information.

- Wild apple trees grow in plains and flower-rich biomes (`c:is_plains`, `c:is_floral`), in new chunks only, about one patch in twelve chunks there.
- A barrel ages by game time, so sleeping through the night doesn't age it (sleeping skips the time of day, not game time).

- Crows come to the Overworld only, by day (on the Overworld clock), wherever there are ripe crops open to the sky; they don't depend on the biome or the season.

- Sky lanterns go up in any dimension, all year. A festival counts lanterns in one dimension at a time.
- A feast table works anywhere, in every dimension, all year.
- A corn maze can be planted on any solid ground, in any dimension, all year. It needs no farmland or light.
- Spirits rise at night on the Overworld clock, from graves in any dimension, all year; they aren't tied to the Halloween event.
- Face paint works anywhere, all year.

## Rollout and open questions
- The aura's area is a box reaching the radius in every direction, up and down too (a beacon's reaches the whole height of the world).
- Warding works on monsters (zombies, skeletons, creepers, spiders, endermen, witches and the like). Hostile mobs that aren't monsters to the game, such as slimes, magma cubes, phantoms, ghasts, shulkers and hoglins, aren't warded.
- Effects are given to players only, not to tamed animals or villagers.
- Mending gives Regeneration II when bright, to everyone in up to a 33-block box, for as long as the candle burns. That is stronger than a beacon's Regeneration I for a short while and is open to balance review in play.
- The "still warm" cooldown is the item cooldown, so it shows on every Aura Candle in the hotbar, not only the one just dipped.
- A dipped candle goes to the hand that held it if that hand is empty, else into the inventory, else it pops out of the top of the pot.
- A pot's wax is lost when the pot is broken; the pot drops itself.
- Apple leaves take random ticks but not bone meal, so a harvest candle's aura doesn't speed their fruiting.
- The press can't be fed or emptied by hoppers or pipes yet; grinding, pressing and bottling are by hand.
- The press's pacing is per press: a second player at the same press waits as the first does.
- A barrel holds one batch; a fermenting batch takes no fresh juice, so a cider maker uses several barrels.
- Grapes, juices and the seed oil of the planned Fruit and Seed Press are not built.
- Preserves only spoil when unsealed; nothing else in Jugcraft spoils. A jar's three days count game time (sleeping doesn't hasten them).
- The kettle and shelf can't be filled or emptied by hoppers yet.
- A jar can be sealed only full and fresh: an opened jar can't be put up again.
- A spooky firework can't be used in a crossbow (crossbows only take vanilla rockets) or to boost elytra flight.
- A burst is drawn facing each player when it bursts; a player who moves round it while it fades sees it turn edge-on.
- Rockets climb about 15 blocks at flight 1 and 30 to 40 at flight 3, much as vanilla's do.
- Crows peck only crops of the vanilla crop kind (`CropBlock`); tall crops, gourds, bushes and fruit trees are safe.
- A scarecrow's guard is a circle measured across the ground from its head; walls don't block it.
- Crows don't eat crops from storage, trample farmland, or attack. A crop set back keeps its farmland.
- The spawn chances, flock size and caps are first guesses, open to balance review in play.
- Crows use vanilla sounds (a parrot's call pitched down); a crow's own caw would need a new sound file.
- A lantern lights nothing below it: entities can't give off block light, so its glow is drawn, not cast.
- The wind is the same everywhere in a world and turns with game time; weather doesn't change it.
- The festival's memory is the server's alone and is lost on a restart (see Multiplayer and persistence).
- Lanterns can't be let go from a dispenser.
- A serving can be eaten at the table whether or not the diner is hungry, so a blessing's only cost is the food. That is open to balance review in play.
- Variety counts kinds of food: two dishes of bread count once. A table longer than eight lengths is counted eight lengths at a time, from where you eat.
- Only players are diners; pets and villagers at the table don't count.
- Dishes can't be filled or emptied by hoppers.
- A gate plants one maze. For a new maze, clear the old corn first (each stalk gives back its kernel), then place a gate again: a new gate plants round whatever still stands.
- The finish post drops nothing. The clock stops at its square whether or not the post still stands.
- The gate doesn't check that the paths are clear: on uneven or cluttered ground a wall may have gaps, or a path may be blocked by whatever was already there.
- Runs count blocks walked across the ground, so a runner who wanders the dead ends is never mistaken for a shortcut; one who breaks through a wall usually is.
- A maze's walls are corn, which anyone can break: the board's times are only as fair as the players keeping the maze.
- Spirits rise only from the graveyard decorations' graves (gravestones and grave mounds), not from crypts or mourning angels, nor from vanilla blocks.
- A revealed spirit is revealed to everyone who can see it, by design: one lantern serves a whole party.
- Spirits pass no walls: they float, but bump into blocks like other creatures.
- A Ghostly candle turns every player in its aura invisible, friend or foe, as a potion of Invisibility would; armour and held items still show.
- Anyone holding a kit can paint another player's face without asking; the paint is harmless and washes off in water.
- Face paint shows on players only; armor stands, mannequins and mobs can't be painted.
- Rain doesn't wash paint off; only putting your head under water does.
- Jars of preserves can't be served: a jar has no food component (it is eaten a serving at a time from the hand, with its own sealing and spoiling), and the table serves only items that do. Serving preserves at the table would be a follow-up.
- A mooncake's Luck goes by the Overworld clock's moon wherever it is eaten, and needs only the sky open above the eater; it isn't limited to the Overworld.

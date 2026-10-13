# More Halloween

Status: implemented in source for batches 7 to 14, not yet played. The Build workflow compiles it, and CI's game tests pass (recorded below).
Proposal issue: none; requested directly by the owner on 2 October 2026 ("I want to make more halloween content more decorations and fun festive content", then "Lets do those 45 by each category starting with Haunted House inside then Mad Scientist and Mosnters then Yard and Porch then Lighting and Glow then Party Games then Night Events then Treats then Costumes"). The 45 ideas ship one category per pull request, each stacked on the one before:
- batch 7, the haunted house inside: the Haunted Chandelier, the Phantom Pipe Organ, the Suit of Armor, the Dust Sheet, the Spirit Mirror, Tattered Curtains and the Creepy Doll;
- batch 8, the mad scientist and monsters: the Tesla Coil, the Lab Table, the Specimen Jar, the Mummy Sarcophagus, the Raven on a Perch and the Black Cat Figure;
- batch 9, the yard and porch: the Yard Inflatables (a ghost, a black cat, a pumpkin stack and a spider), the Animatronic Porch Witch, Grasping Hands, the Poseable Skeleton, Bone Wind Chimes, the Bat and Witch Weathervanes, the Spooky Sign, the Haunted Archway and the Dead Hollow Tree;
- batch 10, lighting and glow: the Black Light and Glow Paint, the Witch Fire Brazier, the Shadow Puppet Lamp, the Mini Pumpkin Stack and the Floating Witch Hat;
- batch 11, party games: the Jump-Scare Trap, the Costume Contest (the Costume Runway, the Judges' Table and the Best Costume Ribbon), Pumpkin Bowling (Skeleton Pins, the Bowling Pumpkin and the Bowling Scoreboard), the Candy Cache, the Monster Mash Dance Floor, Ghost Tag (the Ghost Bell) and the Fortune Teller's Table;
- batch 12, night events: trick-or-treaters at your door, Toilet Paper Rolls, the Haunted Hayride and the Halloween Bonfire (with marshmallows to toast);
- batch 13, treats: the Witch's Brew Punch Bowl, Soul Cakes and the Barmbrack (with a ring that tells your fortune), pumpkin spice lattes, pumpkin bread, spiderweb cupcakes and bat-wing cookies, and Giant Candy props;
- batch 14, costumes: the Vampire Cape, Mummy Wraps, the Skeleton Suit, the Werewolf Mask, Cat Ears and Tail and Bat Wings, and the Costume Trunk.

Owner: @jimbozoomer-byte
Target milestone and tier: batch 7 is Discovery tier (iron, candles, a note block, bones, planks, an armor stand, white carpet, glass, gold nuggets, soul sand, string, clay and wool). Batch 8 is Discovery tier too (iron, glass, a slime ball, sandstone, paper, rotten flesh, feathers, sticks, black terracotta, glowstone dust), apart from the Tesla Coil, which needs copper and copper cable (the tin and bronze tier) and a generator to run. Batch 9 is Discovery tier (wool, dyes, iron, redstone, a cauldron, bones, rotten flesh, dirt, string, slabs, planks, sticks, mossy cobblestone, lanterns, logs and a jack o'lantern). Batch 10 is Discovery tier too (iron, purple stained glass, glowstone dust, a glow ink sac, bone meal, a campfire, paper, a candle, slabs, carved pumpkins, torches and black wool). So is batch 11 (planks, wool, iron, red and purple carpet, gold, bones, a pumpkin, black dye, sticks, logs, a chest, black stained glass, glowstone dust, a redstone lamp and a book), and batch 12 (paper, sticks, sugar, an egg, a minecart, hay bales, a jack o'lantern, logs, coal and cobblestone), and batch 13 (glass, glow and sweet berries, wheat, sugar, eggs, a pumpkin, cocoa beans, a milk bucket, a glass bottle, a gold nugget and red dye), and batch 14 (wool, dyes, string, paper, bones, glowstone dust, leather, sticks, planks and a chest).
Primary specialty and supported player role: building and play; builders (haunted houses and Halloween yards), groups (a shared haunted house to explore, a street of decorated yards, a Halloween party with games to play together), and anyone who likes a prop that does something when you are not looking.

Event-only activities in later batches follow the rule the earlier Halloween work set: they run only in the Halloween window, and anything crafted or placed stays all year. Nothing in batches 7 to 11 depends on the event: the party games can be played at any party, all year. In batch 12 only the trick-or-treaters are an event activity (they come only while the event runs); the toilet paper, the hayride, the bonfire and marshmallows work all year. Nothing in batch 13 depends on the event: the treats can be baked, eaten and served all year. The batch 14 outfits can be worn all year; while the event runs they count for trick-or-treating, and costumed mobs may wear them.

## Player experience
### Batch 7: the Haunted Chandelier
1. A wrought-iron ring of eight candles that hangs under a block or from a chain (it falls if that goes). Flint and steel or a fire charge lights every candle (3 light for every two that burn: 12 with all eight); an empty hand snuffs them and they stay out.
2. It sways gently on its chain, and each burning candle has a flickering flame.
3. **At night a draft nobody can find** now and then blows every candle out at once with a hiss and a wisp of smoke. Then they relight by themselves, one every three quarters of a second, until all eight burn again.

### Batch 7: the Phantom Pipe Organ
4. A carved organ three blocks wide and two tall: a keyboard over a pedalboard, a music desk with a sheet of old music, carved towers and thirteen pewter pipes. It is one prop, like a large machine: place it with its keyboard toward you, and breaking any block of it picks up the whole organ.
5. **Use any block of it, or give it a redstone signal,** and it plays the opening of Bach's Toccata and Fugue in D minor: the famous falling phrase three times, each an octave lower, then the rolled chord over a low D. The keys go down by themselves as it plays. Use it again to stop it.
6. **At night it sometimes starts playing on its own.**

### Batch 7: the Suit of Armor
7. A full suit of plate on a wooden stand, two blocks tall, holding a halberd. Its helmet slowly turns to follow the nearest player within ten blocks (up to 75 degrees either way), and looks ahead again when nobody is near. At night a red glow shows in its visor. Use it and it clanks.

### Batch 7: the Dust Sheet
8. An old white sheet (three white carpets). Use it on a chair, a stair or slab, a chest, a barrel, a bookshelf, a crafting table or another block of the `jugcraft:dust_sheet_coverable` tag, and it drapes over it, sagging a little in the middle and falling to a ragged hem on the floor. It goes over a rocking chair rather than sitting you down, and over a chest rather than opening it.
9. The sheet keeps what is under it: its shape (it still blocks the way and you can still stand on it), how long it takes to break, and a chest's contents.
10. **Use it with an empty hand to pull the sheet off:** the furniture is back as it was, contents and all, and the sheet goes back to you. Breaking a covered block drops the sheet and whatever the block would drop, and spills a chest's contents, as breaking the chest would.
11. **At night one sheet in three seems to breathe,** rising and falling as if something under it were asleep.

### Batch 7: the Spirit Mirror
12. An old silvered mirror in an arched gilt frame, hung on a wall. By day it is only a mirror.
13. **At night a pale face shows in the glass** now and then: for four seconds in every thirty, fading in and out, to anyone standing in front of it within eight blocks. Each mirror keeps its own time.
14. Look into it (use it): by day "Just your reflection. Probably."; at night "For a moment, someone stands behind you in the glass."

### Batch 7: Tattered Curtains
15. Ragged, moth-eaten cheesecloth on an iron rod, for windows and doorways (three from three iron nuggets and six string). They hang at the back of their block, before a wall or window or under a lintel. Placed under one another they make one drape, up to eight blocks long, with the rod at the top and a ragged hem at the bottom.
16. **Use any of them to draw the whole drape open or shut.**
17. Closed, they sway in a draft that nobody can find, more toward the hem, and twice as much at night. Anyone walks through them.

### Batch 7: the Creepy Doll
18. A porcelain doll in a faded velvet dress, with ringlets, a red bow, big black eyes and a crack across its face. It sits on a floor, a shelf, a slab or a fence post.
19. **Its head never moves while you watch it.** Look away and look back, and it has turned: usually toward you, now and then (one time in three) far off to one side. Each player sees it turn for themselves.
20. Wind it (use it) and its music box plays a note.

### Batch 8: the Tesla Coil
21. A mad scientist's coil two blocks tall: a riveted iron base, a copper primary, a tall copper winding and a polished toroid on top (a lightning rod, two copper ingots, copper cable and three iron ingots). Cables connect to its base.
22. **Use it to switch it on.** With power (20 JE a tick) it hums, its winding glows (light 8), sparks spit from the toroid, and every second or two it throws a crackling violet arc to another running coil within eight blocks, or into the air if it stands alone. The arcs are harmless: nothing is struck.

### Batch 8: the Lab Table
23. A riveted steel operating table two blocks long with leather straps, and on it a patient under a stained sheet, one grey hand slipped out from under it. Place it and it lies away from you; breaking either block picks up the whole table.
24. **Give it a redstone signal and the patient sits bolt upright**, with a crackle of sparks and a groan; it lies back down when the power goes. At night it twitches now and then.

### Batch 8: the Specimen Jar
25. A tall glass jar of glowing green fluid (light 7) under an iron lid, with an eye, a tentacle, a tiny pumpkin or a brain floating in it, bobbing and turning slowly while bubbles rise past. Sneak-use it to put in the next; broken, it keeps its specimen.

### Batch 8: the Mummy Sarcophagus
26. A painted sarcophagus two blocks tall, gold and lapis on sandstone, standing up. **Use it, or give it a redstone signal**, and its lid grinds open, the mummy lurches out at you with its arms coming up and a groan, and four seconds later it shuffles back and the lid shuts.

### Batch 8: the Raven on a Perch
27. A raven on a turned wooden perch. Its head turns to watch the nearest player within eight blocks; now and then it ruffles its feathers and croaks. Use it and it caws and beats its wings at you.

### Batch 8: the Black Cat Figure
28. A glazed black cat sitting tall, its tail swishing slowly. At night its eyes glow green.
29. **Run past it** (sprinting within three blocks) and it arches its back and hisses, for a second and a half; then it won't again for three seconds. Use it and it purrs.

### Batch 9: Yard Inflatables
30. Four big vinyl figures, two blocks tall, each on a little blower: a **ghost** with its arms out, a **black cat** with a bow, a **pumpkin stack** in a witch's hat and a **spider** on eight legs (each its own block).
31. **Use it, or give it a redstone signal**, and the blower fills it over two seconds: it rises off the lawn, flopping forward as it fills, then stands up straight, wobbling in the breeze (more in the rain) and glowing from inside (light 7). Switched off, it sags and folds flat over three seconds, a low pile you can walk over.

### Batch 9: the Animatronic Porch Witch
32. A green-faced witch two blocks tall in a tall pointed hat, stirring a pot of bubbling green brew with a long wooden spoon, slowly, round and round. Her head turns to follow the nearest player.
33. **Walk up to her** (within four blocks, when nobody was there) and she throws her head back and cackles, stirring hard, her eyes glowing green, for two seconds. Someone who stays doesn't set her off again: they have to go and come back, and she rests ten seconds between cackles. At night her eyes glow anyway.

### Batch 9: Grasping Hands
34. Two rotting hands clawing up out of a mound of dirt. **Step on them** without sneaking and they snatch at your ankles: a groan, and Slowness II for a second and a half. They stay up, clenched, for under a second, then sink back and rest two seconds before they can grab again. Sneak past and they leave you alone; mobs that blunder over them are grabbed too.

### Batch 9: the Poseable Skeleton
35. A life-sized skeleton. **Use it to pose it:** sitting on the ground with its legs out, standing and waving, lounging back with its hands behind its head and a knee up, or hanging by its hands from whatever is above it (a beam, a branch, a porch roof).

### Batch 9: Bone Wind Chimes
36. Five bones and a little skull striker hung on string from a wooden disc, under a block. They swing in the wind, a little on a still day, more in the rain and wildly in a thunderstorm, and clack together now and then: more often and louder as the weather worsens.

### Batch 9: the Weathervanes
37. A black iron vane on a pole over the four compass points (which always point north, east, south and west), with a **bat** or a **witch on a broomstick** on its tail. It stands on a roof, a post or a fence, and turns to point into the wind: the same wind everywhere in the world, slowly coming round over the days, so every vane in a town points the same way. In a storm it swings about.

### Batch 9: the Spooky Sign
38. A weathered board on a stake with a warning slapped on in dripping red paint. **Use it** to paint the next: BEWARE, KEEP OUT, TURN BACK, GO AWAY, NO TRESPASSING, ABANDON HOPE.
39. **Write your own**, as on a gravestone: use a Name Tag named in an anvil on it (the tag isn't used up), or rename the sign in an anvil before placing it. Your words cover the painted ones; sneak-use with an empty hand to wipe them off. Broken, it keeps both.

### Batch 9: the Haunted Archway
40. A gateway three blocks wide and three tall for the front of a yard or a graveyard: two mossy stone pillars, each with an iron lantern on its front (light 14), and a wrought-iron arch with a skull and crossbones at its crown and cobwebs in its corners. You walk through the middle. Placed from the block you aim at to your right; broken as one. Use it to put the lanterns out or light them.

### Batch 9: the Dead Hollow Tree
41. A dead, gnarled tree four blocks tall: a dark hollow at its foot, a face in its bark with glowing yellow eyes, and bare branches reaching out over the blocks round it, two of them hung with lanterns (light 13). Only the trunk is solid. Placed and broken as one; use it to put the lanterns out or light them.

### Batch 10: the Black Light and Glow Paint
42. **The Black Light:** a violet fluorescent tube in a black fixture, hung on a wall. Use it to switch it on and off, or give it a redstone signal; it glows violet (light 6).
43. **Glow Paint:** a jar of glowing paint (a glow ink sac and bone meal make four). Paint it on any face of a block, a wall, the floor or a ceiling: a skull, a bat, a spider, a web, a handprint or an eye; use it to paint the next design over it. In ordinary light it is a faint pale smear; **under a black light** within six blocks it blazes out green-white, fully within three blocks and fading by six.

### Batch 10: the Witch Fire Brazier
44. A black iron bowl of glowing coals on four legs, burning with a tall flame (light 15). **Use a dye on it** to turn the flame orange, green, purple or blue (the dye is used up), with sparks to match. A shovel puts it out; flint and steel or a fire charge lights it again. It is witch fire: it burns nothing and nobody.

### Batch 10: the Shadow Puppet Lamp
45. A candle on a turned wooden base under a three-sided paper shade with a bat, a cat and a witch cut out of it. Lit like a candle (light 12), the warm shade turns slowly, once every twelve seconds, and the three shapes **slide round the walls of the room**, bigger the further away the wall, darker at night.

### Batch 10: the Mini Pumpkin Stack
46. Three little jack o'lanterns, two side by side and one on top, grinning out at whoever placed them, a candle in each (light 12). Placed lit; an empty hand snuffs them and flint and steel or a fire charge lights them again.

### Batch 10: the Floating Witch Hat
47. A pointed black hat with an orange band and a candle hanging inside it, floating where it is placed, bobbing and turning slowly, each hat out of step with its neighbours. Lit like a candle (light 10); nothing walks into it.

### Batch 11: the Jump-Scare Trap
48. A battered plank crate with iron corners and a red question mark on its front. **Walk up to its front** (within two and a half blocks, not sneaking) or trip a redstone signal into it (hook it to a tripwire) and the lid bursts open with a shriek: a sheet ghost on a spring shoots up out of it, overshooting and wobbling. Two seconds later the ghost sinks back and the lid drops shut; three seconds after that it is ready again. It frightens; it does no harm. Sneak up on it to get past.

### Batch 11: the Costume Contest
49. **The Costume Runway** is red carpet with gold braid along its edges and little footlights, laid like carpet along the way you face (three from three red carpets and two gold nuggets). **The Judges' Table** is draped in red to the floor, with a brass bell, a ballot box and three score cards.
50. **Ring the bell** (use the table) to open a round of one minute. Anyone wearing a costume on their head (a carved pumpkin, a witch hat, a ghost sheet or a scarecrow hat, the trick-or-treat costumes) who walks the runway within 16 blocks is entered (at most 16 contestants); the score cards go up.
51. **Everyone else votes** by using the contestant they like best with an empty hand: one vote each, which they can move; nobody votes for themselves. Use the table during a round to hear the standings.
52. When the minute is up, the contestants with the most votes (at least one) each win a **Best Costume Ribbon**, and the table closes.

### Batch 11: Pumpkin Bowling
53. **Skeleton Pins:** little skeletons standing to attention, round as bowling pins, red ribbons at their necks (two from two bones). Nothing walks into them. Set them up in a triangle, or however you like.
54. **The Bowling Pumpkin:** a small, heavy pumpkin drilled with three finger holes (a pumpkin and an iron nugget). **Use it to roll it** along the ground the way you face; it rolls on, slowing, falling where the ground falls, and knocks down every pin it rolls through, the pins behind them sometimes going down too. When it stops or hits a wall it comes to rest as an item to pick up.
55. **The Bowling Scoreboard:** a slate in a wooden frame on legs. A pumpkin rolled within six blocks of it scores each roll on it for the pins within four blocks of it: **ten frames of two balls, strikes and spares as in ten-pin**, for a lane of any number of pins. The slate shows the frame, the last three frames' marks (X, /, -) and the score. It stands the pins up again two seconds after a strike, a spare or the end of a frame; use it to start a new game. A fallen pin also stands up when used.

### Batch 11: the Candy Cache
56. A hollow, mossy stump with roots and a knot-hole, for a candy hunt. It is a Candy Bowl in disguise: fill it with treats, and each finder takes one a night (whoever hid it, any time). Nothing shows from outside how full it is, apart from a faint sparkle at the knot-hole now and then while there are treats in it.

### Batch 11: the Monster Mash Dance Floor
57. Black glass tiles over a grid of coloured lamps (eight from glowstone dust, black stained glass and a redstone lamp). A tile beside **a jukebox playing a disc**, or with a redstone signal, lights up and passes it on to the tiles beside it, up to eight tiles away (light 8). Lit tiles pulse in orange, purple, green and magenta, the colours stepping along the floor in waves on the beat; tiles further from the music glow a little less. **Villagers on lit tiles hop and spin.**

### Batch 11: Ghost Tag
58. **The Ghost Bell:** a tarnished bronze bell with a little ghost for a clapper, hung from a crooked post. **Ring it** to start a two-minute round of Ghost Tag with everyone within 16 blocks (at least two players, at most 32). One of them, at random, is the ghost: "it", glowing so everyone can see.
59. The ghost tags someone by hitting them: **no harm is done** (hits between players in the round are cancelled), and now they are the ghost. They can't tag straight back whoever tagged them for two seconds. Players who leave, or wander more than 48 blocks away, drop out. When time is up, whoever is the ghost loses. The bell swings the whole round; use it to hear who is the ghost.

### Batch 11: the Fortune Teller's Table
60. A round table under a purple, star-sprinkled cloth with a gold fringe, a spirit board (YES, NO and GOODBYE) and three tarot cards on it. **Use it:** the middle card flips over (the Moon, the Bat, the Pumpkin, the Ghost, the Cat or Death), the planchette slides across the board to YES, NO or GOODBYE and circles there, and you are told one of twenty silly fortunes, for you alone. Everyone near sees the card and the planchette. A table reads at most once every two seconds.

### Batch 12: Trick-or-treaters at your door
61. **While the Halloween event runs, between dusk and midnight,** village children come trick-or-treating to players' homes. A home is a **Candy Bowl** within four blocks of a wooden door that has a porch light (a jack o'lantern, a turnip lantern, or a lit hand-carved or giant pumpkin, as for knocking on villagers' doors). Every ten seconds, with a player within 48 blocks, a bowl has one chance in four of visitors, at most six groups a night.
62. A group of one to three children in costume (a carved pumpkin, a witch hat, a scarecrow hat or a ghost sheet) comes walking from 12 to 20 blocks away, up to the bowl, knocks ("Trick or treat!") and waits three seconds. **Each takes a treat from the bowl and leaves a thank-you gift** at it: a flower, pumpkin seeds, a sheet of paper (a drawing), a feather or, rarely, a candle (the gift table is data, `gameplay/trick_or_treater_thanks`).
63. **If the bowl is empty**, or runs out before every child has had one, they run off giggling and **toilet-paper the trees** round the door: up to six streamers from the leaves and over the fences within eight blocks.
64. Then they walk back the way they came and are gone. They give up and go home after a minute if they can't reach the bowl. They can't be hurt and drop nothing. Candy Caches are hidden, so nobody knocks at them.

### Batch 12: Toilet Paper Rolls
65. **Throw a Toilet Paper Roll** (four from three paper and a stick) over a tree or a fence. Where it lands it unrolls into up to four **Toilet Paper Streamers** within two blocks: strands of paper hanging one to three blocks down from leaves and logs, or draped over fences and walls. Streamers break at a touch and drop nothing, fall when what holds them goes, and **rain washes off** the ones it reaches.

### Batch 12: the Haunted Hayride
66. A hay wagon on rails: a minecart (a minecart, two hay bales and a jack o'lantern) with **four seats** on the hay bales along its sides and a lit jack o'lantern on a post. Use it to climb on, up to four riders; it rolls like a minecart, powered rails and all.
67. **At night, while it rolls with riders,** something spooky is heard from the dark beside it every five to twelve seconds: a ghast's shriek, a witch's cackle, a groan, a rattle of bones or bats, with a swirl of soul sparks.

### Batch 12: the Halloween Bonfire and marshmallows
68. **The Halloween Bonfire:** a cone of logs over glowing embers in a ring of stones, burning with flames a block and a half high (light 15) (from logs, coal and cobblestone). It cooks whatever a campfire cooks (corn, chestnuts, pumpkin seeds, potatoes, meat), on **four skewers, twice as fast**: use the food on it, and when it is done it pops off. It burns whatever stands on it. A shovel puts it out; flint and steel or a fire charge lights it again.
69. **Marshmallows** (four from two sugar and an egg) go on a stick. **Hold a Marshmallow on a Stick out** (keep using it) over a lit bonfire within three and a half blocks, or a campfire within two: let go after three seconds for a golden **Toasted Marshmallow**, but after seven it is a **Burnt Marshmallow** (it starts to smoke a second before). Eating either leaves the stick. Walk away from the fire and it stops toasting.

### Batch 13: the Witch's Brew Punch Bowl
70. A wide glass bowl on a black iron tripod (five glass round a glow berry). **Use a glow berry or a sweet berry on it** to brew two servings of glowing green punch, up to twelve; when it is full it takes no more. While there is punch in it, it glows (light 6), an iron ladle leans in it, a full bowl has eyeballs floating in it, and **dry-ice fog** spills over its rim and down its sides with a bubble now and then.
71. **Use a glass bottle on it** to ladle out a **Witch's Brew Punch** (one serving). Drinking it (2 hunger, even when full) makes you glow for ten seconds and gives the bottle back. The punch in a bowl is lost if the bowl is broken.

### Batch 13: Soul Cakes and the Barmbrack
72. **Soul Cakes** (three from wheat, sugar, an egg and sweet berries for currants): small spiced cakes, 4 hunger, given out on All Hallows' Eve, so they count as treats for a Candy Bowl or Candy Bag.
73. **The Barmbrack** (two wheat, sugar, two sweet berries and a gold nugget): an Irish fruit loaf, placed like a cake and eaten a slice at a time by a hungry player (six slices, 2 hunger each). **When it is placed, one slice is picked to hide the ring** (the gold nugget baked into it). Whoever eats that slice finds the **Barmbrack Ring** and is told they will be married within the year; every other slice tells a smaller fortune: a coin (good fortune), a pea (no wedding this year), a stick (quarrels), a rag (hard times) or just crumbs. Each loaf has exactly one ring. A whole loaf picks up as itself; a cut one breaks and is lost, as a cake does.

### Batch 13: pumpkin spice lattes, pumpkin bread, spiderweb cupcakes and bat-wing cookies
74. **Pumpkin Spice Latte** (a milk bucket, a pumpkin, sugar, cocoa beans and a glass bottle; the bucket comes back): 3 hunger and Speed for thirty seconds, even when full; it leaves its bottle.
75. **Pumpkin Bread** (two from two wheat, a pumpkin, sugar and an egg): 6 hunger, a good meal; it counts as bread.
76. **Spiderweb Cupcakes** (four from wheat, sugar, an egg and cocoa beans): chocolate, iced with a white web, in an orange and black case; 3 hunger. **Bat-Wing Cookies** (eight from two wheat, cocoa beans and sugar): chocolate bats with orange eyes; 2 hunger. Both count as candy, for a Candy Bowl, a Candy Bag or the trick-or-treaters.

### Batch 13: Giant Candy
77. **Giant Candy** (two from eight sugar round a red dye): a sweet as big as a block, to stand in a yard or on a party table. **Use it with an empty hand to change it** between a candy corn kernel, a swirled orange, white and purple lollipop on a stick (taller than a block), a sweet in a twisted purple wrapper, and a sugared green gumdrop. It is only a prop: nothing to eat.

### Batch 14: the outfits
78. Six full-body outfits, each one item **worn on the head** (put it on like a helmet, or use it), as the Ghost Sheet is. Each counts as a **costume for trick-or-treating** and for the costume contest, and earns the costume-hat bonus at villagers' doors. While the Halloween event runs, zombies, husks, skeletons, strays and zombie villagers may wear them too.
79. **Vampire Cape** (black wool round red): a tall red-lined collar and a long black cape that **flares out behind as you walk**, the faster the further, and **wraps round you when you sneak**: its side panels swing round your sides and its front panels close over your front.
80. **Mummy Wraps** (paper and string round white wool): linen bandages over your head, body, arms and legs, with a slit for the eyes and two loose ends trailing.
81. **Skeleton Suit** (bones and black wool round glowstone dust): a black suit painted with a skull, ribs, a spine and arm and leg bones, which **glow in the dark**.
82. **Werewolf Mask** (brown wool, leather and a bone): a wolf's head with a snout, fangs, yellow eyes and pricked ears, shaggy fur over your body, arms and legs, claws, and a bushy tail that lifts as you walk.
83. **Cat Ears and Tail** (two black wool, string and pink dye): ears on a headband, a black catsuit with a gold bell at the neck, and a long tail that **sways and curls**.
84. **Bat Wings** (sticks, leather and black dye): a hood with tall bat ears (your face shows), a dark furry suit, and leathery wings on your back that stay folded while you stand on something and **spread and flap when you jump or fall**.

### Batch 14: the Costume Trunk
85. A steamer trunk of dark wood bound in brass, with a purple label (planks round purple wool and a chest). It keeps up to **nine costumes** (any trick-or-treat costume: the outfits, the costume hats, the Ghost Sheet, carved pumpkins), one of each item.
86. **Use it holding a costume** to pack it away. **Use it with an empty hand to change**: you put on the costume at the front, and the one you were wearing goes in at the back, so using it again and again goes through your wardrobe. A helmet that isn't a costume has to come off first. **Sneak and use it with an empty hand** to take out the costume packed last.
87. The lid opens when it is used and falls shut a second later; comparators read how full it is (15 with nine). Broken, it drops itself and spills its costumes.

## Connections
- Batch 14 inputs: black, red, white, brown, purple and black wool, pink and black dye, string, paper, bones, glowstone dust, leather, sticks, planks and a chest.
- Batch 14 outputs: costumes for trick-or-treating (the existing `jugcraft:trick_or_treat_costumes` and `jugcraft:costume_hats` tags), the costume contest and costumed mobs (`CostumedMobs.COSTUMES`); a costume store that holds the earlier costumes too. No new costume system: an outfit is a head item, as the earlier costumes are.
- Batch 13 inputs: glass and glow berries (the bowl), and glow or sweet berries to brew; glass bottles to ladle; wheat, sugar, eggs, sweet berries, a pumpkin, cocoa beans, a milk bucket and a glass bottle (the treats); a gold nugget (the barmbrack's ring); sugar and red dye (giant candy).
- Batch 13 outputs: food (soul cakes, pumpkin bread, cupcakes, cookies, the latte, the punch, the barmbrack's slices), treats for the Candy Bowl, Candy Cache, Candy Bag and the trick-or-treaters (soul cakes, cupcakes and cookies, through the existing `jugcraft:candy_bag_treats` and `c:foods/candy` tags), and the Barmbrack Ring (a keepsake; nothing consumes it).
- Batch 12 inputs: paper and sticks (toilet paper); a minecart, hay bales and a jack o'lantern (the hayride), and rails; logs, coal and cobblestone (the bonfire), and whatever a campfire cooks; sugar, an egg and a stick (marshmallows). The trick-or-treaters need a Candy Bowl, a wooden door and a porch light, and the treats a bowl holds (any treat: candy, cookies, caramel apples, marshmallows).
- Batch 12 outputs: thank-you gifts (flowers, pumpkin seeds, paper, feathers, candles) for treats given out; cooked food (by the campfire recipes); toasted and burnt marshmallows (food, and treats for a bowl or Candy Bag).
- Batch 11 inputs: planks, white wool and iron (the trap), and redstone or a tripwire to spring it; red carpet and gold nuggets (the runway); red wool, a gold ingot and planks (the judges' table), and a costume on the head (the trick-or-treat costumes) to enter; bones (the pins); a pumpkin and an iron nugget (the bowling pumpkin); planks, black dye and sticks (the scoreboard); logs and a chest (the cache), and treats to fill it; black stained glass, glowstone dust and a redstone lamp (the dance floor), and a jukebox playing a disc or redstone to light it; sticks, gold ingots and white wool (the bell); purple carpet, a book and planks (the fortune table).
- Batch 11 outputs: the Best Costume Ribbon (a trophy; nothing consumes it), and treats moved from one player to another through the Candy Cache.
- Batch 10 inputs: iron nuggets, purple stained glass, glowstone dust, and redstone (the black light); a glow ink sac and bone meal (the paint); iron and a campfire, and dyes (the brazier); paper, a candle and a wooden slab (the lamp); carved pumpkins and a torch (the pumpkins); black wool and a candle (the hat); flint and steel or a fire charge to light them.
- Batch 9 inputs: wool, a dye or a spider eye, iron nuggets and redstone (the inflatables), and redstone to run them; purple and black wool, redstone and a cauldron (the witch); bones, rotten flesh and dirt (the hands); bones (the skeleton); an iron nugget, string, a wooden slab and bones (the chimes); iron and black or purple dye (the vanes); planks, red dye and a stick (the sign), and a Name Tag to write on it; lanterns, iron and mossy cobblestone (the archway); lanterns, a stick, logs and a jack o'lantern (the tree).
- Batch 8 inputs: a lightning rod, copper, copper cable and iron, and the electric network's power (the coil); iron, white wool and rotten flesh, and redstone (the table); glass, an iron nugget and a slime ball (the jar); sandstone, gold nuggets, paper and rotten flesh, and redstone (the sarcophagus); feathers, black dye and sticks (the raven); black terracotta and glowstone dust (the cat).
- Existing input producer: iron, candles, flint and steel or fire charges (the chandelier); iron, bone, a note block and planks, and redstone (the organ); iron and an armor stand (the suit); white carpet (the sheet); a glass pane, gold nuggets and soul sand (the mirror); iron nuggets and string (the curtains); clay, wool and string (the doll). The sheet covers the earlier batches' Rocking Chair, Hay Bale Seat, Crystal Ball and Grimoire Stand, this batch's doll and mirror, and vanilla furniture.
- Existing output consumer: decoration, light (the chandelier), music (the organ), storage kept under a sheet. Batch 11's Candy Cache is the Candy Bowl's own block entity under another block, so treats go in and out of it exactly as a bowl's do (no second candy store), and the costume contest uses the trick-or-treat costume tag.
- Technology connection: none needed for batches 7 and 9; the organ and the inflatables answer redstone. Batch 8's Tesla Coil runs on the electric network through the shared energy interface (`EnergyStorage.SIDED`), so any Jugcraft generator powers it, and its recipe needs copper cable; the lab table and sarcophagus answer redstone.
- Magic connection: none yet.
- Reachable entry path: everything is crafted from vanilla materials an early player has.
- Required vs optional: all optional decoration; nothing in progression needs them.
- How this stays useful without other branches: builders get a furnished haunted house and a Halloween yard that do things by themselves.

## Balance and automation
- No energy. A chandelier costs two iron nuggets, an iron ingot and three candles; an organ three iron ingots, two bones, a note block and three planks; a suit of armor four iron ingots and an armor stand; a sheet three white carpets (two wool); a mirror seven gold nuggets, a glass pane and a soul sand; three curtains three iron nuggets and six string; a doll a clay ball, two wool and a string.
- Light: the chandelier 3 for every two burning candles (12 with all eight).
- Batch 8: the Tesla Coil uses 20 JE a tick while it runs (buffer 4,000 JE, up to 64 JE a tick in): a coal generator runs it easily, a solar panel (8 JE a tick) can't alone. Nothing else in batch 8 uses energy. A coil costs a lightning rod, two copper ingots, a copper cable and three iron ingots; a table two white wool, a rotten flesh and five iron ingots; a jar an iron nugget, three glass and a slime ball; a sarcophagus five sandstone, two gold nuggets, a paper and a rotten flesh; a raven two feathers, a black dye and four sticks; a cat five black terracotta and a glowstone dust. Light: a running coil 8, a jar 7.
- Batch 9 uses no energy. Inflatables cost five wool, a dye (a spider eye for the spider), two iron nuggets and a redstone; the witch a purple wool, two black wool, a redstone and a cauldron; the hands two bones, a rotten flesh and a dirt; the skeleton six bones; the chimes an iron nugget, two string, a wooden slab and three bones; a vane two iron nuggets, a dye and two iron ingots; two signs five planks, a red dye and a stick; the archway two lanterns, two iron ingots and four mossy cobblestone; the tree two lanterns, a stick, three logs and a jack o'lantern. Light: a blown-up inflatable 7, the witch's brew 6, the archway's lanterns 14, the tree's 13.
- The hands' Slowness is harmless (no damage) and can't be farmed into anything.
- Batch 10 uses no energy. A black light costs three iron nuggets, two purple stained glass and a glowstone dust; four glow paints a glow ink sac and a bone meal; a brazier five iron ingots and a campfire, and a dye each time its flame changes colour; a lamp five paper, a candle and a wooden slab; two pumpkin stacks three carved pumpkins and a torch; two hats six black wool and a candle. Light: a black light 6, glow paint 1, a brazier 15, a lamp 12, a pumpkin stack 12, a hat 10.
- Batch 11 uses no energy. A trap costs seven planks, a white wool and an iron ingot; three runways three red carpets and two gold nuggets; a judges' table two red wool, a gold ingot and two planks; two pins two bones; a bowling pumpkin a pumpkin and an iron nugget; a scoreboard five planks, a black dye and two sticks; a cache four logs and a chest; eight dance floor tiles four black stained glass, four glowstone dust and a redstone lamp; a ghost bell a stick, three gold ingots and a white wool; a fortune table two purple carpets, a book and five planks. Light: a lit dance floor tile 8.
- Batch 12 uses no energy. Four toilet paper rolls cost three paper and a stick; a hayride a minecart, two hay bales and a jack o'lantern; a bonfire four logs, a coal (or charcoal) and three cobblestone; four marshmallows two sugar and an egg, and one more stick each to toast. Light: a lit bonfire 15.
- Batch 13 uses no energy. A punch bowl costs five glass and a glow berry, and a berry for every two servings; three soul cakes a wheat, a sugar, an egg and a sweet berry; two pumpkin breads two wheat, a pumpkin, a sugar and an egg; four cupcakes a wheat, a sugar, an egg and a cocoa bean; eight cookies two wheat, a cocoa bean and a sugar; a latte a pumpkin, a sugar, a cocoa bean and a glass bottle (the milk bucket comes back empty); a barmbrack two wheat, a sugar, two sweet berries and a gold nugget; two giant candies eight sugar and a red dye. Food: a soul cake 4 hunger, pumpkin bread 6, a cupcake 3, a cookie 2, a latte 3 (with 30 s of Speed), a punch 2 (with 10 s of Glowing), a barmbrack 12 in six slices. These sit with vanilla's own baking: pumpkin pie gives 8 from a pumpkin, a sugar and an egg; cookies 16 from two wheat and a cocoa bean. Light: a punch bowl with punch in it 6.
- Batch 14 uses no energy. A cape costs seven black wool and a red wool; wraps four paper, four string and a white wool; a skeleton suit four bones, four black wool and a glowstone dust; a werewolf mask six brown wool, a leather and a bone; cat ears and tail two black wool, a string and a pink dye; bat wings four leather, two sticks and a black dye; a trunk seven planks, a purple wool and a chest.
- **Batch 14 makes nothing from nothing.** Outfits give no armor or other protection, and nothing consumes them; the trunk only moves costumes between its store and your head or hand, one of each. Costumed mobs drop their outfit only as any mob drops its equipment (now and then).
- **Batch 13 makes nothing from nothing.** A serving of punch is half a berry; a bottle goes in and comes back with every drink. The barmbrack's ring is the gold nugget baked into it, one a loaf, found once; a cut loaf doesn't drop (so it can't be broken and placed again to hide a second ring), and the ring turns into nothing.
- **Batch 12 makes nothing from nothing.** A trick-or-treater's gift comes only for a treat taken from a player's bowl (one treat, one small gift: a flower, seeds, a sheet of paper, a feather or, rarely, a candle), at most six groups of three a bowl a night, during the event. The bonfire cooks by the campfire's own recipes, no more than a campfire would. Streamers drop nothing. A marshmallow toasts into one marshmallow.
- **Batch 11 makes nothing from nothing.** The Best Costume Ribbon is the only item it gives, one to each winner of a round that had at least one vote, and a vote needs another player. The bowling pumpkin comes back as itself; the cache only hands out treats someone put in. Ghost Tag and the trap do no harm and drop nothing.
- **The Dust Sheet makes nothing.** It only moves a block (and that block's saved data) under itself and back. Breaking a sheeted block gives the covered block's own drops, with the tool the player is using: a sheet over stone stairs broken by hand gives only the sheet, as breaking stone stairs by hand gives nothing. A sheeted chest's contents spill once, from the sheet, never also from the chest.
- No conversion loops; nothing here makes items or energy.

## Multiplayer and persistence
- **Server authority.**
  - Lighting and snuffing the chandelier, playing and stopping the organ, covering and uncovering, opening and closing curtains, and using the mirror, suit and doll go through vanilla's block and item use paths (reach, spawn protection, adventure mode) and are decided on the server.
  - The chandelier's gusts and relighting, the organ's night playing and its notes run on the server, for everyone near.
  - **Covering a container closes it first** for anyone looking into it, and its contents move under the sheet before the chest is replaced, so nothing can be taken out of a covered chest or spilled twice. Clients are only told which block is under a sheet, never a chest's contents.
- **Batch 8, server authority.** Switching a coil, sitting the patient up, changing a specimen, opening the sarcophagus and using the raven and cat go through vanilla's block use path (reach, spawn protection) or redstone, and are decided on the server. A coil's power, running and arcs (and which coil it arcs to) are worked out on the server and sent as block events; the cat watches for runners on the server, from where players really are, every 5 ticks, at the level's player list. The raven's flap is a block event.
- **Batch 9, server authority.** Switching an inflatable, posing the skeleton, changing or writing on a sign, and lighting the archway and tree go through vanilla's block use path (reach, spawn protection; writing and wiping the sign also need build permission) or redstone, and are decided on the server. The witch watches the level's player list on the server every 5 ticks; the hands grab on the server when something steps on them, and their phases run on scheduled ticks. A sign's own words are cleaned (control characters dropped, at most 50 characters) before they are kept or sent.
- **Batch 10, server authority.** Switching a black light, painting, dyeing, lighting and putting out go through vanilla's block use path (reach, spawn protection; painting over glow paint needs build permission) or redstone, and are decided on the server; a dye or fire charge is used up and flint and steel worn there.
- **Batch 11, server authority.** Everything the games decide happens on the server.
  - **The trap** watches the level's player list every 5 ticks while it is ready, and springs only for a player near its front who isn't sneaking or spectating. Its phases run on scheduled ticks.
  - **The contest:** the table enters contestants from where players really are and what they really wear. A vote is a use of another player (Fabric's `UseEntityCallback`), checked on the server: an empty hand, within 16 blocks of an open table, for a contestant, not yourself, one vote a player. Ribbons go only to winners still on the server, into their inventory or at their feet.
  - **Bowling:** the pumpkin rolls, knocks pins and finishes on the server; the scoreboard counts the pins within four blocks itself, once a roll, and never takes a score from a client.
  - **Ghost Tag:** a tag is a hit between two players in the same round (Fabric's `AttackEntityCallback`), checked on the server and cancelled, so no damage or knockback reaches the player hit. Players who leave or go more than 48 blocks away drop out every second. Glowing is a server effect, so every client sees who is the ghost.
  - **The fortune table** picks the fortune, card and answer on the server. The fortune goes to the reader alone; the card and answer reach everyone near as a block event. A table reads at most once every two seconds.
  - All of them are used through vanilla's block use path (reach, spawn protection).
- **Batch 12, server authority.** The trick-or-treaters are run entirely by the server: which bowls they visit (from the event, the clock, the door, the porch light and the players near), where they come from, when they knock, what they take and give and where they paper. Treats come out of the bowl's own store on the server. Throwing a roll, draping streamers, boarding the hayride (Fabric's `UseEntityCallback`, checked on the server: not sneaking, not riding, a seat free), cooking on the bonfire (by the server's own recipes) and toasting (the server times how long the stick was held, and checks for a fire near every half second) are all decided there; the riders' spooks are server sounds.
- **Batch 14, server authority.** Putting an outfit on is vanilla equipping. Packing, changing and taking out go through vanilla's block use path (reach, spawn protection) and are decided on the server, which checks the item is a costume (the tag), that there is room, and that the head holds a costume or nothing.
- **Batch 14, client only.** How an outfit looks on its wearer (the cape's flare and wrap, the tails, the wings, the glowing bones) is drawn by each client's `CostumeLayer` from what it already has: the render state (walking speed, sneaking, time) and the one block under the wearer's feet (whether they are off the ground). Nothing about it is sent or saved.
- **Batch 13, server authority.** Brewing and ladling punch, eating a slice of barmbrack and changing a giant candy go through vanilla's block use path (reach, spawn protection) and are decided on the server: the server checks the player can eat, picks the ring's slice (when the loaf is placed, or, for a loaf that never was, the first time it is eaten), gives the ring and tells the fortune to the eater alone. Drinks are vanilla consumables, applied by the server.
- **Batch 13, client only.** The bowl's fog and bubbles are each client's own particles; everything else is block models.
- **Client only.** The chandelier's sway and flames, the organ's keys, the suit's helmet and visor glow, the sheet's drape and breathing, the mirror's face, the curtains' sway and the doll's head are drawn by each client from what it already has (the time, the block states, where players are, its own camera), so nothing about them is sent or trusted. Every client works out the same nearest player for the suit. The doll turns for each player alone, by when that player last saw it.
- **Batch 8, client only.** The arcs (from the block event), the patient sitting up (from the block state), the specimens bobbing, the lid and mummy, the raven's head and wings, and the cat's tail and eyes are drawn by each client from what it already has. The coil keeps a set of running coils per level, so finding a partner searches no blocks.
- **Batch 9, client only.** The inflatables filling, flopping and wobbling, the witch's arm and head, the chimes swinging and clacking, the vanes turning and the sign's lettering are drawn by each client from what it already has (the block states, the time, the weather, where players are). The wind is the same function of the time on every client, so every vane points the same way; the chimes' clacking is each client's own sound.
- **Batch 12, client only.** The bonfire's flames and the food on its skewers (from the synced skewers), and the hayride's wagon, wheels and lantern, are drawn by each client.
- **Batch 11, client only.** The trap's lid, ghost and spring, the dance floor's colours, the scoreboard's chalk, the bell's swing and the fortune table's card and planchette are drawn by each client from what it already has (the block states, the scoreboard's synced rolls, the reading's block event, the time). The rolling pumpkin's turning is worked out by each client from how far it has moved. The trap keeps when each trap last changed phase, as that client saw it, in a weak map.
- **Batch 10, client only.** The paint's glow, the brazier's flames, the lamp's shade and shadows and the hats' bobbing are drawn by each client from what it already has. Each client knows which black lights shine from the block states it has: a shining light notes where it is in a small map each tick, and paint looks through that map, never searching blocks. The lamp looks along at most six blocks for each of its three shadows' walls each frame.
- **Saved state.**
  - Block states: the chandelier's `lit` and `burning`; the organ's `part`, `facing`, `playing` and `powered`; the suit's `facing` and `half`; the mirror's `facing`; the curtains' `facing`, `open` and `part`; the doll's `facing`.
  - The organ's master block entity saves where it is in its tune (`tick`) and when it started (`start_time`, for the keys).
  - A sheet's block entity saves the covered block (`covered`) and, if that block had a block entity, its full saved data with its type (`covered_data`).
  - Batch 8 block states: the coil's `facing`, `half`, `enabled` and `active`; the table's `facing`, `part` (`foot`, `head`) and `powered`; the jar's `specimen`; the sarcophagus's `facing`, `half`, `open` and `powered`; the raven's `facing`; the cat's `facing` and `hissing`. The coil's lower half saves its energy (`energy`); the cat saves when it calms and when it may hiss again (`calm_at`, `ready_at`). A sarcophagus shuts by a scheduled tick.
- Batch 9 saved state: block states (the inflatables' `facing`, `half`, `on`, `powered`; the witch's `facing`, `half`, `cackling`; the hands' `facing` and `phase`: rest, grab, recover; the skeleton's `facing`, `half`, `pose`; the sign's `facing` and `words`; the archway's and tree's `facing`, `lit` and `part`). The witch saves when she calms, when she may cackle again and whether anyone is near (`calm_at`, `ready_at`, `someone_near`); a sign saves its own words (`words`), which go with its item as its name. The hands' phases run on scheduled ticks.
- **Bounded work.**
  - Only a playing organ ticks, on its master block, for its 200 ticks. A lit chandelier and the organ's master take random ticks; a relighting chandelier schedules one tick per candle.
  - The renderers do a little arithmetic a frame. The suit looks through the level's player list; the curtains look up at most eight blocks above them; the suit and doll keep one small entry per block entity in a weak map.
  - Batch 8: a running coil ticks (its energy, and an arc every 15 to 40 ticks); a cat looks every 5 ticks; nothing else ticks. Arcs, the patient, the specimens, the mummy, the raven and the cat's tail are a little arithmetic a frame on each client; the raven looks through the level's player list.
- Batch 14 saved state: an outfit is an ordinary item. The trunk's block states (`facing`, `open`); its block entity saves its costumes in order (`outfits`).
- Batch 14 bounded work: nothing ticks; a used trunk schedules one tick to close its lid. Each worn outfit costs a few boxes a frame and one block lookup on each client.
- Batch 13 saved state: block states (the bowl's `servings` 0–12; the barmbrack's `facing` and `bites` 0–5; the giant candy's `facing` and `design`). A barmbrack's block entity saves which slice hides the ring (`ring`, -1 before it is picked) and whether it has been found (`found`).
- Batch 13 bounded work: nothing ticks. The bowl's fog is a client's `animateTick` (a puff half the time, now and then a bubble).
- Batch 12 saved state: block states (the streamer's `draped`; the bonfire's `lit`). A bonfire saves what is on its skewers and how far each has cooked (`skewers`); a Candy Bowl now also saves how many groups of trick-or-treaters came on which night (`groups_night`, `groups`); a hayride is saved as a minecart. The trick-or-treaters are never saved: a visit in progress ends with a restart, and any children found when the world loads go home at once.
- Batch 12 bounded work: a Candy Bowl (not a cache) looks every ten seconds; outside the event and the hours it stops there. In them it reads the porch (at most 405 blocks for the door and 486 for the light) and the level's player list. A visit moves every half second through its children (at most three), and a prank looks at a 17 by 17 by 17 box once. A roll looks at a 5 by 5 by 5 box once where it lands. A lit bonfire ticks its four skewers. A toasting stick looks at a 9 by 5 by 9 box every half second. Streamers take random ticks only to wash off in rain.
- Batch 11 saved state: block states (the trap's `facing`, `phase`: ready, popped, resetting, and `powered`; the runway's `axis`; the judges' table's `facing` and `open`; the pins' `facing` and `down`; the scoreboard's `facing`; the cache's `facing` and `fill`; the dance floor's `distance` 0–8; the bell's `facing` and `ringing`; the fortune table's `facing`). The judges' table saves its round (`ends_at`, `contestants` in the order they walked the runway, `votes`); the scoreboard its game (`rolls`, `pins`, `standing`); the bell its round (`ends_at`, `players`, `it`, `tagged_by`, `tagged_at`); the cache its treats, owner and visits exactly as a Candy Bowl; a rolling pumpkin its scoreboard (`scoreboard`). A round in progress goes on after a restart.
- Batch 11 bounded work: a ready trap looks every 5 ticks; a judges' table with a round open every 10, a ringing bell every 20 (both through the level's player list), and they stop ticking when the round ends; a scoreboard counts a 9 by 3 by 9 box of blocks once a roll; a bowling pumpkin looks at two blocks a tick, rolls at most 300 ticks, and its domino run goes at most two pins deep; a pumpkin's use looks for a scoreboard in a 13 by 5 by 13 box once; the dance floor works out its distance in scheduled ticks only as its neighbours change, and lit tiles take random ticks for villagers in the block above them. The contest is limited to 16 contestants and Ghost Tag to 32 players.
- Batch 10 saved state: block states only (the black light's `facing`, `lit`, `powered`; the paint's `facing` and `design`; the brazier's `lit` and `flame`; the lamp's `lit`; the pumpkins' `facing` and `lit`; the hat's `lit`).
- Batch 10 bounded work: nothing ticks on the server; a shining black light ticks on each client to note where it is.
- Batch 9 bounded work: the witch looks every 5 ticks; the hands only act when stepped on, and then schedule two ticks; nothing else ticks. The renderers do a little arithmetic a frame; the witch's head reads the level's player list.
- New IDs only:
  - blocks with items: `haunted_chandelier`, `phantom_pipe_organ`, `suit_of_armor`, `spirit_mirror`, `tattered_curtains`, `creepy_doll`;
  - the `dust_sheet` block and the `dust_sheet` item (an item, not a block item: it covers blocks rather than being placed);
  - block entities: `haunted_chandelier`, `suit_of_armor`, `spirit_mirror`, `tattered_curtains`, `creepy_doll` (empty, for the client's drawing), `phantom_pipe_organ` (the tune) and `dust_sheet` (what it covers);
  - block tag `jugcraft:dust_sheet_coverable`, so packs can let sheets cover more or fewer blocks.
- Batch 8 new IDs: blocks with items `tesla_coil`, `lab_table`, `specimen_jar`, `mummy_sarcophagus`, `raven_perch`, `black_cat_figure`; block entities of the same names (`tesla_coil` its energy, `black_cat_figure` its timers, the rest empty, for drawing).
- Batch 9 new IDs: blocks with items `inflatable_ghost`, `inflatable_cat`, `inflatable_pumpkin`, `inflatable_spider`, `porch_witch`, `grasping_hands`, `poseable_skeleton`, `bone_wind_chimes`, `bat_weathervane`, `witch_weathervane`, `spooky_sign`, `haunted_archway`, `dead_hollow_tree`; block entities `inflatable` (all four inflatables), `porch_witch` (her timers), `bone_wind_chimes`, `weathervane` (both vanes) and `spooky_sign` (its words); the inflatables, chimes and vanes' are empty, for drawing.
- Batch 10 new IDs: blocks with items `black_light`, `glow_paint`, `witch_fire_brazier`, `shadow_puppet_lamp`, `mini_pumpkin_stack`, `floating_witch_hat`; block entities `black_light`, `glow_paint`, `witch_fire_brazier`, `shadow_puppet_lamp` and `floating_witch_hat` (all empty, for drawing).
- Batch 11 new IDs: blocks with items `jump_scare_trap`, `costume_runway`, `judges_table`, `skeleton_pin`, `bowling_scoreboard`, `candy_cache`, `dance_floor`, `ghost_bell`, `fortune_teller_table`; items `best_costume_ribbon` and `bowling_pumpkin`; the entity `bowling_pumpkin`; block entities `jump_scare_trap`, `judges_table`, `bowling_scoreboard`, `dance_floor` (empty, for drawing), `ghost_bell` and `fortune_teller_table` (the cache uses `candy_bowl`'s).
- Batch 12 new IDs: blocks with items `toilet_paper_streamer`, `halloween_bonfire`; items `toilet_paper_roll`, `haunted_hayride`, `marshmallow`, `marshmallow_on_a_stick`, `toasted_marshmallow`, `burnt_marshmallow`; entities `toilet_paper_roll`, `haunted_hayride`; block entity `halloween_bonfire`; block tags `jugcraft:toilet_paper_hangs_from` and `jugcraft:toilet_paper_drapes_over`; loot table `jugcraft:gameplay/trick_or_treater_thanks`. The trick-or-treaters are vanilla villagers with the entity tag `jugcraft.trick_or_treater`.
- Batch 13 new IDs: blocks with items `witchs_brew_punch_bowl`, `barmbrack`, `giant_candy`; items `soul_cake`, `pumpkin_bread`, `spiderweb_cupcake`, `bat_wing_cookie`, `pumpkin_spice_latte`, `witchs_brew_punch`, `barmbrack_ring`; block entity `barmbrack`; item tag `jugcraft:witchs_brew_ingredients` (what brews punch: glow berries and sweet berries), so packs can add more.
- Batch 14 new IDs: items `vampire_cape`, `mummy_wraps`, `skeleton_suit`, `werewolf_mask`, `cat_ears_and_tail`, `bat_wings` (each with an equipment asset of the same name, without layers); block with item `costume_trunk`; block entity `costume_trunk`. The outfits join the existing tags `jugcraft:trick_or_treat_costumes` and `jugcraft:costume_hats`.
- **The `agriculture` switch** turns off their recipes; placed blocks stay and work, and sheets can still be pulled off.

## Dependencies and assets
No new dependencies. Every texture is drawn by code (`tools/decor7_textures.py`); the models, loot, tags and recipes come from `tools/decor7_data.py`. The organ and the suit are modelled whole and cut into one model per block. The chandelier, the suit's helmet and the doll's head are written out as quads to `assets/jugcraft/decor7_quads.json` for their renderers (`HauntedChandelierRenderer`, `SuitOfArmorRenderer`, `CreepyDollRenderer`, through `DecorQuads`); the organ's keys, the sheet, the mirror's face and the curtains are drawn by `PipeOrganRenderer`, `DustSheetRenderer`, `SpiritMirrorRenderer` and `TatteredCurtainsRenderer` (with `DecorDraw`).

Batch 8's textures are drawn by code in `tools/decor8_textures.py` and its models, loot, tags and quads come from `tools/decor8_data.py` (the quads in `assets/jugcraft/decor8_quads.json`); the renderers are `TeslaCoilRenderer`, `LabTableRenderer`, `SpecimenJarRenderer`, `MummySarcophagusRenderer`, `RavenRenderer` and `BlackCatRenderer`. 26.3 has no plain cat sounds (cats use sound variants), so the cat hisses with a creeper's fuse, pitched up, and purrs with a low fox sniff; the raven croaks and caws with a parrot's call pitched far down.

Batch 9's textures are drawn by code in `tools/decor9_textures.py` and its models, loot, tags and quads come from `tools/decor9_data.py` (the quads in `assets/jugcraft/decor9_quads.json`; the weathervanes' silhouettes are drawn cut out, a new `cutout` flag on a quad). The archway is cut into one model per block; the tree's boxes go whole to the block their middle is in, so its branches reach out over the air beside it. The renderers are `InflatableRenderer`, `PorchWitchRenderer`, `BoneWindChimesRenderer`, `WeathervaneRenderer` and `SpookySignRenderer`. The witch cackles with the vanilla witch's celebration sound; the chimes clack on the xylophone note-block sound.

Batch 10's textures are drawn by code in `tools/decor10_textures.py` (the paint's designs share the bat and witch grids with batch 9) and its models, loot and tags come from `tools/decor10_data.py` (the hat's quads in `assets/jugcraft/decor10_quads.json`). The renderers are `GlowPaintRenderer`, `WitchFireBrazierRenderer`, `ShadowPuppetLampRenderer` and `FloatingWitchHatRenderer`; lighting and snuffing the candle-lit ones share `CandleLighting`. 26.3 has no item constant for each dye, so the brazier knows dyes by their colour, as the scarecrow does.

Batch 11's textures are drawn by code in `tools/decor11_textures.py` (the pins wear the Poseable Skeleton's bones; the spirit board is drawn at 32 by 16 and cut into two block textures) and its models, loot, tags and quads come from `tools/decor11_data.py` (the quads in `assets/jugcraft/decor11_quads.json`). The renderers are `JumpScareTrapRenderer`, `BowlingScoreboardRenderer`, `DanceFloorRenderer`, `GhostBellRenderer`, `FortuneTellerTableRenderer` and, for the rolling pumpkin, `BowlingPumpkinRenderer`. The trap shrieks with the ghast's scream; the pins rattle with the skeleton's hurt sound, pitched up.

Batch 14's textures are drawn by code in `tools/decor14_textures.py` and its models, loot, tags and the outfits' boxes come from `tools/decor14_data.py`. Each outfit is a list of pieces hung on the wearer's body parts, with joints for what moves; `decor14_data.layout` packs each box's faces onto a 128-texel-wide texture the way a vanilla model box is laid out, the painter paints the same layout, and `assets/jugcraft/costumes.json` carries the boxes and their places to the client's `CostumeLayer`. The Skeleton Suit's glowing bones are a second texture of the bones alone, drawn at full brightness like a spider's eyes. The outfits' boxes stand clear of a player skin's outer layer.

Batch 13's textures are drawn by code in `tools/decor13_textures.py` and its models, loot and tags come from `tools/decor13_data.py`; there are no renderers (everything is a block model, and the fog is particles). The treats are foods in `ITEMS` (`tools/agriculture.py`); the two drinks are a new kind there, `drink` (an effect and its seconds), which the checker compares with `JugcraftAgriculture.drink`. The barmbrack is eaten with the vanilla eating sound, and the ring is found with an amethyst chime.

Batch 12's textures are drawn by code in `tools/decor12_textures.py` and its models, loot, tags and quads come from `tools/decor12_data.py` (the hayride's quads in `assets/jugcraft/decor12_quads.json`). The renderers are `HalloweenBonfireRenderer` and `HauntedHayrideRenderer`; the thrown roll is drawn as its item. The hayride is a vanilla minecart underneath (`Minecart`, placed with vanilla's `MinecartItem`); the trick-or-treaters are vanilla baby villagers in costume.

The organ's tune is the opening of J. S. Bach's Toccata and Fugue in D minor, BWV 565, which is in the public domain; the arrangement for note-block sounds is written here.

## Verification
### Batch 7 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-7` stacked on decorations batch 6:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the chandelier's candles, gusts, relighting, sway and ring, the organ's size, tune length and night chance, the suit's watching, the sheet's breathing and tag, the mirror's face, the curtains' drape and sway and the doll's glances with Java; checks every note of the organ's tune is in range, every state has a blockstate entry, and that the quads and textures the client draws exist) | Pass, 511 IDs |
| `./gradlew build` on `1d94dda` (later commits only change docs and screenshots) | Pass |
| Game tests on the headless server, same commit: 278 in total, 9 of them new here (`Decor7GameTests`) | **All 278 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `1d94dda`; no model, texture or quad errors in the log |

The 9 new game tests (`Decor7GameTests`):
1. a chandelier hangs under a stone ceiling but not from the open air, and is placed unlit; flint and steel lights all eight candles (light 12); a gust blows them all out; three relighting ticks later some but not all burn, and after nine all eight; an empty hand snuffs them and they stay out; it never sways more than its sway; it falls with its ceiling, dropping once;
2. an organ won't go where a block is in its way; placed, its six blocks are where they should be, facing the player, each knowing its master, which holds the tune; breaking a corner breaks all six and drops one organ;
3. using any block of the organ plays the tune from the top (its start time is the game time), using it again stops it; every note is in the tune and a note block's range and on the keyboard, the flute above the harp above the bass; a redstone block at a corner plays it; half way through it is still playing, and at the end it falls quiet;
4. a suit of armor stands two blocks tall facing the player, its helmet's block entity on the upper half; its helmet looks straight at a player ahead, 45 degrees toward one ahead and to its left, no more than 75 degrees, straight ahead at a player in front of an east-facing suit, and ahead with nobody near; it turns 4 degrees a tick and stops where it looks; broken, it drops once;
5. a dust sheet won't cover plain stone; used on a chest holding diamonds and apples it covers it instead of opening it, takes one sheet, keeps the chest and its data, spills nothing and takes the chest's shape; an empty hand pulls it off, giving back the chest with five diamonds and three apples and the sheet to the player; covered again and broken, it drops the sheet, the chest, five diamonds and three apples;
6. a dust sheet covers a rocking chair instead of sitting the player in it, and pulled off, the chair is back with its block entity;
7. a spirit mirror hangs on a wall facing out; by day it never shows a face; at night it shows one for 79 or 80 ticks of each 600, fully in the middle; it falls with its wall, dropping once;
8. three curtains placed down a wall make one drape (top, middle, bottom, facing out); using the middle one opens all three, and using the bottom one closes them; the cloth doesn't move at the rod, and sways at the hem no more than its sway, more at night;
9. a creepy doll sits on a fence post; of 300 glances about one in three find it looking elsewhere and the rest straight at the viewer, never turning more than it can; it falls with its post, dropping once; the seven recipes and loot tables load, and the sheet's tag holds chests and slabs but not stone.

The client game test (`Decor7ClientGameTests`) builds a dark-oak room open at the front: a lit Haunted Chandelier under the ceiling, the Phantom Pipe Organ playing against the back wall, Tattered Curtains at two windows (one drape drawn open), a Suit of Armor by the left wall, Dust Sheets over a rocking chair, a chest and a stair, a Spirit Mirror on the right wall and the Creepy Doll on a bookshelf. It photographs them by day and at midnight, then waits in front of the mirror until its face shows: the sheets drape over their furniture, the organ's keys and a note show as it plays, the chandelier's flames burn, the suit's visor glows red, and the face looks out of the mirror. The doll had looked away from the camera when it was photographed.

Found by CI and fixed before this record:
- 26.3 names `PushReaction.POPPED` and `BLOCK` `POPPED` and `IMMOVEABLE` (a compile error).

**Not run (batch 7):**
- a person playing it in a client;
- a dedicated server with two players (each watched by the suit of armor in turn; both seeing the doll turn differently; one covering a chest the other has open);
- the chandelier's gust and the organ's night playing from real random ticks at night (the tests call the gust directly; the organ's night start is one random-tick check);
- the organ's sound (the CI client has no sound device);
- the doll turning as a real player looks away and back (its rule is tested; the screenshot shows it once).

### Batch 8 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-8` stacked on batch 7:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the coil's power, range and arcs, the table's sitting and twitching, the jar's specimens, light and bob, the sarcophagus's timing and swing, the raven's watching and ruffling and the cat's reach, hiss and swish with Java; checks every block state has a blockstate entry, and that the quads and textures the client draws and the coil's and jar's messages exist) | Pass, 524 IDs |
| `./gradlew build` on `86cb7a5` and on `f413e66` (which only moves the client test's camera and coils; later commits only change docs and screenshots) | Pass |
| Game tests on the headless server, same commits: 288 in total, 7 of them new here (`Decor8GameTests`) | **All 288 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `86cb7a5` and `f413e66`; no model, texture or quad errors in the log |

The 7 new game tests (`Decor8GameTests`):
1. arc offsets pack and unpack; a coil stands two blocks tall, its power found on the lower half; using either half switches it on, but without power it doesn't run; with power it runs, glowing (light 8), using 20 JE a tick; two running coils three blocks apart arc to each other, not into the air; switched off, it stops;
2. a lab table lies away from the player, foot where aimed and head beyond, the patient drawn from the foot; a redstone block by the head sits the patient up and taking it away lays it down; it sits up to 70 degrees and lies back 6 degrees a tick; by day it lies still and at night twitches 4 ticks in each 97; breaking the head breaks the table, dropping it once;
3. a specimen jar glows (light 7) with an eye in it; sneak-use puts in the tentacle, pumpkin, brain, then the eye again; it never bobs more than its bob; broken, it drops once and keeps its tentacle;
4. a sarcophagus stands two tall facing the player; used, both halves open; the lid swings a little a tick; after its time it shuts; a redstone block opens it and it shuts again though the power stays;
5. a raven faces the player who placed it; it ruffles for its ticks of each period; used, it flaps;
6. a walking player beside the black cat doesn't upset it; a sprinting one makes it hiss, its tail still; it settles after its time and, with the runner still there, rests until its cooldown is over, then hisses again;
7. the six recipes and loot tables load.

The client game test (`Decor8ClientGameTests`) builds a stone-brick lab open at the front: two running Tesla Coils, a Lab Table over a block of redstone (the patient sitting up), a counter of Specimen Jars (one of each specimen), a Mummy Sarcophagus, a Raven on a Perch and two Black Cat Figures. It photographs them by day and at midnight, waiting for a coil to arc before photographing the coils, opening the sarcophagus and setting one cat hissing: the arcs show between the coils, the patient sits up under its sheet, the specimens float in their green fluid, the lid stands open with the mummy stepping out, one cat arches its back with its tail up, and at night the cats' eyes glow green.

Found by CI and fixed before this record:
- 26.3 has no `SoundEvents.CAT_PURR` or `CAT_HISS` (cats use sound variants), a compile error; the cat purrs with a fox sniff and hisses with a creeper's fuse instead.
- The first screenshots had a coil in front of the jars and the sarcophagus cut off at the bottom; the coils and camera were moved (`f413e66`).

**Not run (batch 8):**
- a person playing it in a client;
- a dedicated server with two players (one running past the cat while the other walks; both seeing the same arcs; the raven turning from one player to the other);
- a coil on a real generator and cable (the tests fill its buffer directly; it uses the same `EnergyStorage.SIDED` lookup as the other machines);
- the sounds (the CI client has no sound device);
- the patient's night twitching and the jars' bubbles in motion (the rules are tested; screenshots are still).

### Batch 9 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-9` stacked on batch 8:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the inflatables' filling, light and wobble, the witch's reach, cackle, rest and stirring, the hands' grab and rest, the chimes' swing and clacking, the vanes' turning and the archway's and tree's sizes and light with Java; checks the designs, poses, phases and words agree, every block state has a blockstate entry, block models turn only by 22.5 or 45 degrees, and the quads, textures and messages the client uses exist) | Pass, 537 IDs |
| `./gradlew build` on `57adfe9` and on `b819e48` (which only thickens the archway's arch and enlarges the sign's board and words) | Pass |
| Game tests on the headless server, same commits: 298 in total, 10 of them new here (`Decor9GameTests`) | **All 298 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `57adfe9` and `b819e48`; no model, texture or quad errors in the log |

The 10 new game tests (`Decor9GameTests`):
1. each inflatable design is its own block; one stands two blocks tall, flat and dark, nothing of it up high; used, its blower runs on both halves and it glows (light 7); used again it goes flat; a redstone block runs it and taking it away lets it go flat; it fills in 40 ticks and empties in 60; it never wobbles more than its wobble; breaking the upper half drops it once;
2. the porch witch only stirs with nobody near; a visitor walking up sets her cackling on both halves, and she settles after her time; one who stays doesn't set her off again even after her rest; one who goes and comes back does; her spoon keeps to its round, slow and fast;
3. grasping hands need a solid floor; something sneaking over them isn't grabbed; something walking over them is (Slowness II, at most 30 ticks); they sink back after their time and grab nothing while they do, then rest; without their floor they drop once;
4. a skeleton is placed sitting, nothing of it in the upper half; use poses it waving, lounging, hanging and sitting again on both halves, filling the upper half only standing or hanging; broken, it drops once;
5. wind chimes won't go on the floor, only under a block; they swing more, and clack more often and louder, in rain and more again in a storm; without the block above they drop once;
6. both weathervanes are blocks of their own; one stands on a fence post, not on air; the wind it points into is always 0 to 360 degrees, turns less than a fifth of a degree a tick, and comes round by more than 30 degrees in a day and a half; still weather adds no gusts and a storm's are bounded; without its post it drops once;
7. a spooky sign faces the player saying BEWARE; use paints the next warnings; an unnamed Name Tag paints nothing; a named one paints its name and is kept; then use changes nothing, and sneak-use wipes the words; broken, it drops once, with its own words as its name and its painted warning kept;
8. an archway won't go where a block is in its way; placed, its seven blocks go up from the block aimed at to the player's right, facing them, with an opening in the middle; its pillar lanterns light 14; using its crown puts them all out and using a pillar lights them again; breaking a corner breaks it all and drops one archway;
9. a dead tree goes up four blocks; its lanterns light 13 at its third block; use puts them out; breaking its top breaks it all and drops one;
10. the thirteen recipes and loot tables load.

The client game test (`Decor9ClientGameTests`) builds a house front with a porch and a lawn: the Porch Witch at her pot, Bone Wind Chimes under the porch roof, a Bat and a Witch Weathervane on the roof, the four inflatables blown up, Grasping Hands, three Spooky Signs (one with its own words), the skeleton sitting on the porch, waving, lounging and hanging from a gallows beam, the Haunted Archway at the gate and the Dead Hollow Tree. It photographs them by day and at midnight, setting the witch cackling before photographing her.

Found by CI and fixed before this record:
- 26.3 has `SoundEvents.AXE_STRIP` as a sound holder, not a sound (a compile error); the sign plays its value.
- In the first screenshots the arch was thin and the sign's words were small; both were made larger (`b819e48`).

**Not run (batch 9):**
- a person playing it in a client;
- a dedicated server with two players (one walking up to the witch while the other stays; both seeing every vane point the same way; one writing on a sign the other reads);
- something really walking onto the grasping hands (the test calls `stepOn`, as the game does when something walks on them);
- the chimes and vanes in real rain and thunder (their rules are tested; the screenshots are in clear weather);
- the sounds (the CI client has no sound device).

### Batch 10 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-10` stacked on batch 9:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the black light's light and range, the brazier's light, flames and dyes, the lamp's light, turning and reach, the pumpkins' and hat's light and the hat's bob and turning with Java; checks the paint's designs agree, every block state has a blockstate entry, and the textures and quads the client draws exist) | Pass, 543 IDs |
| `./gradlew build` on `34fa3b4` (later commits only merge batch 9's docs and change docs and screenshots) | Pass |
| Game tests on the headless server, same commit: 303 in total, 5 of them new here (`Decor10GameTests`) | **All 303 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `34fa3b4`; no model, texture or quad errors in the log |

The 5 new game tests (`Decor10GameTests`):
1. a black light hangs on the wall it was placed against, facing out, off; used, it shines (light 6), used again it goes off; a redstone block turns it on and taking it away off; its glow is full within half its range, half at three quarters and gone at its edge; without its wall it drops once;
2. glow paint goes on a wall, the floor and a ceiling, facing out from each, a skull first, with a faint light of its own (1); used, it is painted over with a bat, a spider, a web, a hand, an eye and a skull again; it needs a face behind it; without its wall it drops once;
3. a brazier is placed burning orange (light 15); a green dye turns it green and is used up, the same dye again does nothing; an iron shovel puts it out; flint and steel lights it again, still green, and is worn by one; broken, it drops once;
4. the lamp and the hat are placed out and the pumpkin stack lit, facing the player; flint and steel lights the lamp (12) and a fire charge the hat (10), and is used up; an empty hand snuffs all three; the lamp's shade goes round once in its time, and the hat bobs no more than its bob;
5. the six recipes and loot tables load.

The client game test (`Decor10ClientGameTests`) builds a dark stone room with two Black Lights over a back wall of Glow Paint (and paint on the floor, the ceiling and a side wall) and a lit Shadow Puppet Lamp on a table in the middle; outside, four Witch Fire Braziers (orange, green, purple, blue) with Floating Witch Hats over them and Mini Pumpkin Stacks in front. It photographs them by day and at midnight, then turns the black lights off and photographs the paint again: the paint blazes green-white under the lights and is a faint smear without them, the braziers burn in their four colours, and the lamp's bat and cat fall on the walls.

Found by CI and fixed before this record:
- 26.3 has no item constant for each dye (`Items.ORANGE_DYE` and the rest), a compile error; the brazier knows dyes by their colour, as the scarecrow does.

**Not run (batch 10):**
- a person playing it in a client;
- a dedicated server with two players (one switching a black light on while the other watches the paint; both seeing the lamp's shadows);
- the brazier's flames, the lamp's turning and the hats' bobbing in motion (their rules are tested; the screenshots are still);
- the sounds (the CI client has no sound device).

### Batch 11 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-11` stacked on batch 10:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the trap's reach and timing, the contest's round, range and entries, the bowling pumpkin's speed, friction and dominoes, the lane's reach and frames, the dance floor's reach and light, Ghost Tag's round, range, players and tag-backs and the fortune table's fortunes, cards and cooldown with Java; checks the Candy Cache is a Candy Bowl with the bowl's block entity, every block state has a blockstate entry, every message the games show has its words (each fortune, each vote, each cache answer), and the textures and quads the client draws exist) | Pass, 554 IDs |
| `./gradlew build` on `0df3dfe` (later commits only change docs and screenshots) | Pass |
| Game tests on the headless server, same commit: 312 in total, 9 of them new here (`Decor11GameTests`) | **All 312 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `0df3dfe`; no model, texture or quad errors in the log |

The 9 new game tests (`Decor11GameTests`):
1. a player sneaking up to a jump-scare trap's front doesn't spring it; walking up does (popped); it shuts and rests (resetting) after its time, then is ready again; a redstone block (a tripwire's signal) springs it;
2. ringing the judges' table opens a round; a player in a witch hat on the runway is entered and one without a costume isn't, until they put on a ghost sheet; using a contestant with an empty hand votes for them, with a stick in hand doesn't; nobody votes for themselves; voting again moves the one vote; only contestants take votes; the round, its contestants (in order) and votes survive a save and load; at the end the most votes win the Best Costume Ribbon and the table closes;
3. ten-pin scoring: twelve strikes are 300 and the game is over, eleven leave a roll; all spares of five 150; a gutter game 0 in twenty rolls; nine and a miss every frame 90; strike and spare bonuses; a three-pin lane's perfect game; the next ball after a strike and a three; marks for a strike, a spare after a gutter and an open frame; when the pins go back up;
4. a bowling pumpkin used by a player rolls down a lane through three skeleton pins, knocks each down the way it rolled, stops at the wall and comes to rest as an item; the scoreboard scores a strike on the three-pin lane and stands the pins up again; a knocked pin stands up when used;
5. treats used on a candy cache go in and show how full it is; a finder takes one a night and has it;
6. a line of dance floor tiles from a jukebox playing a disc lights one step further each tile and is dark past eight; lit tiles shine (light 8); a villager on a lit tile spins on its random tick; without the music the floor goes dark; a redstone block under a tile lights it and those beside it;
7. ringing the ghost bell starts a round with both players near, one of them the ghost, glowing; the ghost hitting the other tags them harmlessly and they glow instead; they can't tag straight back; a player outside the round is left alone; the round and the ghost survive a save and load; ending it stops the bell and the glowing;
8. using a fortune table reads a fortune (a card and an answer); using it again five ticks later doesn't, after its cooldown it does;
9. the nine recipes and the bowling pumpkin's, and the nine loot tables, load; a turned runway turns its edging.

The client game test (`Decor11ClientGameTests`) builds a party on a lawn:
- a Costume Runway up to a Judges' Table with its round open and a witch-hatted armor stand on the runway;
- a bowling lane of ten Skeleton Pins, seven knocked down, and its Scoreboard;
- a five-by-five Monster Mash Dance Floor lit by a jukebox, with two villagers on it;
- two Jump-Scare Traps, one gone off;
- the Ghost Bell ringing, the Fortune Teller's Table and a Candy Cache.

It photographs them by day and at midnight, having the fortune table read before photographing it. In the screenshots:
- the ghost stands out of its crate on its spring;
- the pins stand and lie as placed, and the slate chalks FRAME 1 and SCORE 7;
- the dance floor's tiles glow in four colours;
- the turned card and the planchette at YES show on the spirit board.

Found by CI and fixed before this record:
- 26.3's `BlockPos` has no `getCenter()`, a compile error; the bell and table measure from `Vec3.atCenterOf` instead.
- The contest saved its contestants as a map, which came back in another order after a save and load; they are saved as a list in the order they walked the runway.
- The fortune table's own block event set its reading time again on the server, a tick late; only clients note readings from it now.

**Not run (batch 11):**
- a person playing it in a client;
- a dedicated server with two or more players: a real costume contest (walking the runway, voting by clicking), a real game of Ghost Tag (hits from a real client), two players bowling on one lane, both seeing the same fortune;
- a real tripwire on the trap (the test uses a redstone block, the same signal);
- the bowling pumpkin bouncing over uneven ground, or many pumpkins rolling at once;
- a jukebox with a real disc in it (the test sets its "has a disc" state, which is what the floor reads);
- the sounds (the CI client has no sound device).

### Batch 12 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-12` stacked on batch 11:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the trick-or-treaters' timing, chances, group sizes, distances, prank, costumes and gift table, the toilet paper's reach and strands, the hayride's seats and spooks, the bonfire's light, skewers and speed and toasting's reach and times with Java; checks the gift table holds the gifts, every block state has a blockstate entry, every message has its words, and the textures and quads the client draws exist; `#minecraft:walls` and `#minecraft:coals` join its list of vanilla tags) | Pass, 562 IDs |
| `./gradlew build` on `299a1cc` (later commits only change docs and screenshots) | Pass |
| Game tests on the headless server, same commit: 318 in total, 6 of them new here (`Decor12GameTests`) | **All 318 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `299a1cc` on its second attempt; no model, texture or quad errors in the log. The first attempt hung in `JugcraftClientGameTests` (an earlier test this branch doesn't touch) and was cancelled at the job's 30-minute limit; the same tests passed on the commit before, and again when the job was re-run once |

The 6 new game tests (`Decor12GameTests`):
1. draping round a tree and a fence hangs a streamer under the leaves and drapes one over the fence, and nothing in the open air; without its leaves the streamer goes; a roll thrown down onto a fence drapes it and is used up; streamers drop nothing;
2. a bowl finds its door, and the door its porch light; trick-or-treating time is the event between dusk and midnight; two children come, each a baby villager in costume that a hit doesn't hurt; at a bowl with two treats they knock, take both, leave gifts and play no prank; at the empty bowl the next child papers the leaves over the door;
3. a hayride is placed on a rail from its item; a sneaking player doesn't board; four do; the fifth finds it full; it picks as its own item; night for its spooks is dusk to dawn;
4. a bonfire is placed lit (light 15); four chestnuts go on its skewers and no fifth, nor a stick; it burns a pig standing on it; the chestnuts roast in half a campfire's time and pop off; a shovel puts it out and flint and steel lights it; broken, it drops itself and the chestnut on its skewer;
5. a marshmallow is raw before three seconds, toasted after, burnt after seven; a lit bonfire three blocks off is near enough, an unlit one isn't, a lit campfire beside you is; let go after four seconds it is a toasted marshmallow in the pocket; after one, still raw;
6. the five recipes, the two loot tables and the gift table load.

The client game test (`Decor12ClientGameTests`) builds:
- a house front with a jack o'lantern porch light, a Candy Bowl of candy corn, and three village children in costume posed at its door;
- two trees and a fence toilet-papered with `drape`;
- a lit Halloween Bonfire roasting chestnuts, corn and a potato;
- a Haunted Hayride on a stretch of rail with three costumed children aboard.

It photographs them by day and at night. In the screenshots:
- the streamers hang from the trees;
- the bonfire's flames and smoke rise over its skewers;
- the hayride sits along its rails with its wheels, lantern and riders.

Found by CI and fixed before this record:
- 26.3's entities have no `setInvulnerable` (a compile error); the children are kept from harm through Fabric's `ALLOW_DAMAGE` instead.
- The bonfire test's first victim was a player who had just joined, and so couldn't be hurt yet; it burns a pig now.
- The first screenshots had a thin spike of flame and the hayride turned across its rails (the test placed it at 90 degrees; a minecart along x faces 0). The flames were widened and lowered, and the test's hayride turned (`299a1cc`).

**Not run (batch 12):**
- a person playing it in a client;
- a real Halloween night: trick-or-treaters arriving by themselves at a player's bowl (the test sends a group and brings it to the bowl; the chance, the hour and the porch rules are tested in parts), and walking there over real ground;
- a dedicated server with two or more players: four riding the hayride round a track at night, one throwing toilet paper while another watches;
- rain washing streamers off (the rule is a random tick in rain);
- the sounds (the CI client has no sound device).

### Batch 13 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-13` stacked on batch 12:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the punch bowl's servings, brewing and light, the barmbrack's slices, food and fortunes, and the giant candy's designs with Java; checks every block state has a blockstate entry, every fortune its words and the punch's ingredient tag; audits the new `drink` foods against `JugcraftAgriculture.drink`; and refuses the `block_state_property` loot condition, which 26.3 doesn't have) | Pass, 572 IDs |
| `./gradlew build` on `e4076ef` (Build workflow run 36976493848) | Pass |
| Game tests on the headless server, same run: 324 in total, 6 of them new here (`Decor13GameTests`) | **All 324 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `e4076ef` and on `4a1da35` (the bowl's glass and fog, pull request run 36977717598; the screenshots are from it); no model or texture errors in the log |

The 6 new game tests (`Decor13GameTests`):
1. an empty bowl doesn't glow; a glow berry brews two servings and glows (light 6); six fill it; a full bowl takes no more berries; a bottle ladles one Witch's Brew Punch; the last serving empties it and then a bottle gets nothing;
2. a barmbrack placed from its item hides the ring in one of its six slices; a full player can't eat; a hungry one eats a slice a use and finds the ring in exactly that slice; the last slice finishes the loaf, and the six slices fed 12;
3. broken, a whole loaf drops itself and a cut one nothing; a loaf with nothing under it goes;
4. an empty hand goes through the giant candy's four designs and back;
5. the latte and the punch can be drunk on a full stomach, give Speed and Glowing, and leave their bottles; soul cakes, cupcakes and cookies go in a candy bag and bread doesn't; cupcakes and cookies count as candy;
6. the eight recipes and three loot tables load.

The client game test (`Decor13ClientGameTests`) sets a party table:
- a full and a half-full punch bowl, a whole and a cut barmbrack, and a Candy Bowl of soul cakes and cupcakes;
- the four giant candies in front of it;
- every treat in an item frame on the wall behind.

It photographs them by day and at night. In the screenshots:
- the punch glows green in its glass bowl, with the ladle, the floating eyes of the full bowl and the fog;
- the cut loaf is shorter than the whole one;
- the lollipop stands taller than a block;
- every item's picture shows in its frame.

Found by CI and fixed before this record:
- 26.3 has no `block_state_property` loot condition: the server failed to load the barmbrack's loot table and every game test stopped. Whole loaves are matched with `match_block` instead, as the coffin's head is (`e4076ef`), and the checker now refuses the old condition.
- The first screenshots showed the bowl's glass almost clear (the punch read as a bare green cube) and two fog puffs a display tick hiding the bowl. The glass is half see-through now and a puff comes half the time (`4a1da35`).
- The pull request's own `mod` job on `4a1da35` failed once in `PetroGameTests` (the heliostat test from `main`, which also failed on batch 12's, batch 9's and an earlier pull request's first runs); this batch doesn't touch it, and the job passed when re-run once.

**Not run (batch 13):**
- a person playing it in a client;
- a dedicated server with two or more players sharing one loaf and one bowl;
- a barmbrack saved and loaded with its ring still hidden (the block entity saves it; not tested across a restart);
- the sounds (the CI client has no sound device).

### Batch 14 verification

Actual results (2 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions), on `claude/agriculture-halloween-decor-14` stacked on batch 13:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the outfits, the trunk's slots and lid time with Java; checks every outfit has an equipment asset without layers, boxes on known body parts with known axes and motions (those `CostumeLayer` knows), faces inside its texture, textures that exist, and its place in the costume tags; the trunk's block states and messages; and accepts the outfits as costumed mobs' costumes) | Pass, 579 IDs |
| `./gradlew build` on `efc98a9` (Build workflow run 36979434685) | Pass |
| Game tests on the headless server, same run: 328 in total, 4 of them new here (`Decor14GameTests`) | **All 328 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `efc98a9` on its second attempt; the first hung in `JugcraftClientGameTests` (the first client test, from `main`, before any of this batch's) and was cancelled at the 30-minute limit, as batch 12's first attempt was. No model or texture errors for this batch in the log |

The 4 new game tests (`Decor14GameTests`):
1. every outfit is worn on the head with an equipment asset, is a trick-or-treat costume and a costume hat, and is on costumed mobs' list;
2. a stick doesn't go in a trunk, two outfits do and the lid opens; an empty hand puts on the first, then changes into the second and packs the first; sneaking takes the last one out; a helmet that isn't a costume stays on; nine fit and a tenth is refused; a full trunk gives comparators 15; the lid falls shut;
3. one of a stack goes in; broken, a trunk drops itself and its costumes;
4. the seven recipes and the trunk's loot table load.

The client game test (`Decor14ClientGameTests`) sets up:
- the six outfits on armor stands;
- a crouching zombie in the cape, a skeleton in the werewolf mask and a husk in mummy wraps;
- an armor stand floating in the air in bat wings;
- an open trunk.

It photographs them from the front and the back by day and at night. In the screenshots:
- the cape flares behind its stand and wraps the crouching zombie;
- the cat's tail curls up and the wolf's hangs;
- the floating stand's wings are spread while the standing one's are folded;
- the skeleton suit's bones glow at night.

Found by CI and fixed before this record:
- 26.3's `PoseStack.mulPose` takes no quaternion (a compile error); the joints turn with `rotateDegrees`, as the other renderers do (`efc98a9`).

**Not run (batch 14):**
- a person playing it in a client: a player walking in the cape, sneaking and jumping in the wings (the test uses a crouching zombie and a floating armor stand, which the layer reads the same way);
- a dedicated server with two or more players changing at one trunk;
- knocking on a villager's door in an outfit during the event (the knock checks the costume tag, which the test checks);
- the sounds (the CI client has no sound device).

### Look fixes, 5 October 2026: the Specimen Jar's eye and the Shadow Puppet Lamp
The owner: "the texture of the eye in the specimen jar is weird but the rest of the jar is really cool" and "shadow puppet lamp textures and just bizzare constantly flashing and glitching". Fixed on 5 October 2026 (art fixes); tiers, inputs, outputs, costs, IDs, light levels and behaviour are unchanged. The jar's glass, fluid, iron and motion are untouched (the owner likes them); bigger jars are a separate change.

**The Specimen Jar's eye.**
- **Cause:** a 3-pixel cube with the eye picture pinned to its front only; its other five faces took position UVs, the middle 3 × 3 texels of the picture, so as it turned the jar mostly showed a black-topped blue cube. The picture itself was random speckle with a checkered iris and a square pupil.
- **Fix:** `tools/decor16_data.py` `specimen_eye()` builds it on the Flying Eyeball's painters: a 4-pixel body with a bulge through each side (rounder, 4.5 pixels across), every face mapped to its own piece at four texels to a pixel, the warm white with a few placed veins, a round blue iris (it stands out against the green fluid) with a dark ring, a round pupil and one glint, and the optic nerve hanging straight down on the axis it turns about. It stays inside the fluid as it turns and bobs (at most 2.83 pixels from the axis; the fluid is 3). `specimen_eye.png` is now its 64 × 64 piece texture (one writer); the old painter in `tools/decor8_textures.py` is gone, and `tools/decor8_data.py` takes the eye's elements from it, for the client's quads and the item.
- **Checked offline:** renders of the jar turned 0, 45, 90, 180 and 270 degrees, and of the eye alone from four sides and above: the eye reads from every side and nothing pokes out of the fluid. `Decor8ClientGameTests` adds the screenshot `jugcraft_specimen_eye_close` for CI (not run yet).

**The Shadow Puppet Lamp.**
- **Causes:** each paper panel was drawn twice in one plane, facing out and facing in. 26.3's cut-out entity type draws both sides of a quad, so the two copies fought at the same depth from every side (the witch and the cat fighting their mirror images), and as the shade turns it never settled. The shadows chose their wall by the ray's main direction, not the face it entered, so at many angles a shadow stood across the wall, half inside it, snapping between two ways; a step of a quarter block could also miss corners. A shadow was never cut to its wall, so it hung in the air over doorways and outside corners. The panels fell 0.003 pixel short of meeting at the corners. The paper and shadows were 16 × 16 random speckle, and the brass and candle noise.
- **Fix (`client/ShadowPuppetLampRenderer.java`):** one quad per panel (seen from inside, the picture is mirrored, as real paper's is), wide enough to close the corners. Each shadow is looked for by five rays across its panel's spread (to 32 degrees either side), each walking the blocks at the flame's height to the face it enters, and on the walls round the ends of those walls' runs (across an inside corner, or round an outside one such as a doorway's side or a pillar), up to twelve walls. On each wall a card is placed where the panel's middle ray meets the wall's plane and cut, its picture cropped to match: to the run of open wall (full blocks with nothing full in front, along the wall either side); to what the flame can see of it past the full blocks in between; and, block by block, to how high the open wall goes. Two rays striking one run give the same pieces, drawn once. A card fades out over the last block of the lamp's six-block range. So a shadow crossing a doorway, a corner or a pillar's edge shows on each side of it and slides smoothly across, instead of vanishing (the first fix dropped it when less than 35% was left) or jumping to the next wall in one frame. The textures (`tools/decor10_textures.py`) are repainted clean: 32 × 32 paper in a lit and shaded wooden frame with regular fibre lines, the bat, the cat and the witch redrawn from smooth shapes with nothing under two texels, 32 × 32 shadows of the same shapes, a turned wooden base with rings, flat brass lit along its top and a cream candle with a melted pool. The Floating Witch Hat's candle keeps its own texture.
- **Checked offline:** renders of the lamp from three sides and in a room with a doorway and a pillar at night, the shadows placed by a Python port of the new code; the old double panels gave about 20,000 fighting pixels a view, one panel gives none beyond its edges. The renderer's shadow methods, compiled against stand-ins for `Vec3`, `BlockPos` and the level, matched the port at 720 angles in eight rooms (5,760 frames, no difference above 0.0001 block). With the shade stepped an eighth of a degree through a full turn in those rooms (a doorway, a side passage, pillars, a window above the wall, a half-open room, CI's corridor), every drawn point lay on open wall and in the flame's sight, and the most the shadows' drawn area changed in one step fell from 1.7 to 3.9 whole shadows (the first fix: shadows popping out or jumping walls) to at most 0.19 of one, or 0.65 where a shadow slides at a grazing angle along a one-block passage (a smooth slide: it shrinks fourfold with a step four times finer). `Decor10ClientGameTests` adds, at the end of its run, the lamp close up twice 30 ticks apart (`jugcraft_shadow_puppet_lamp_close_night`, `_later`) and a second lamp at the end of a narrow corridor with a doorway in its side, twice 40 ticks apart (`jugcraft_shadow_puppet_lamp_corridor_night`, `_later`). CI has not run them yet.

## World and event applicability
- Batches 7 to 14 work anywhere, all year, except batch 12's trick-or-treaters, who come only while the Halloween event runs, between dusk and midnight on the Overworld's clock, wherever the bowl is. When the event ends they stop coming; the bowls, gifts and anything papered stay. The chandelier's gusts, the organ's night playing, the suit's visor glow, the sheets' breathing, the mirror's face and the curtains' night draft follow the Overworld's clock (as the earlier decorations' night effects do); so do batch 8's twitching patient and glowing cat eyes and batch 9's witch's eyes. Batch 9's chimes and vanes follow the weather where the player is; batch 10's lamp's shadows are darker at night. Batch 11's games don't depend on the time, the weather or the event; the Candy Cache's "night" is the Candy Bowl's. Batch 12's hayride spooks follow the Overworld's clock; streamers wash off in the rain where they are.

## Rollout and open questions
- Tesla Coils only arc to coils that are running; a coil in an unloaded chunk drops out of the set until it runs again.
- The Lab Table's patient and the sarcophagus's mummy are props: they can't be fought, and nothing comes out of them.
- The Dust Sheet covers only blocks in its tag, and only those that hold nothing a sheet could lose: no block entity, an empty one (the Jugcraft decorations'), or a container (chests, barrels, chiseled bookshelves). Shulker boxes, lecterns, jukeboxes and two-block things (doors, beds, the suit of armor) are left out on purpose.
- A sheeted chest that is half of a double chest becomes a single chest beside its other half; pulled off, the two join again.
- A sheet over a block keeps that block's shape but not its light, redstone or comparator output.
- The organ, like the giant pumpkin, is mirrored by structure mirroring as a turn, not a flip.
- The archway and tree, like the organ, are mirrored as a turn, and stop being placed where any block they need isn't free.
- The wind is one for the whole world (all dimensions); it doesn't depend on biome or height.
- An inflatable glows from inside at full brightness while it is blown up, day or night.
- Glow Paint only glows under a Jugcraft Black Light, not under other light; it has a faint light of its own (1).
- The lamp's shadows fall on full blocks only, along each panel's direction; they don't bend round corners or fall on furniture.
- A bowling pumpkin knocks down pins it rolls through and, by chance, those behind; it doesn't bounce off them. Lanes can be any size and shape: the scoreboard counts whatever pins are within four blocks of it, as the lane's pins at the first roll of a game.
- A Ghost Tag round, or a costume contest, needs players near when it starts; whoever arrives later isn't in it (latecomers can still walk the runway while a contest is open).
- The costume contest gives a ribbon to every contestant tied for the most votes.
- Villagers dance on lit tiles only on random ticks, so some dance more often than others.
- Trick-or-treaters walk with the villagers' own pathfinding: a bowl they can't reach (behind a wall, up a cliff) gets nobody, and they give up after a minute. They come only where a player is near, so empty homes stay quiet.
- The prank papers only leaves, logs, fences and walls near the door, and only spots that are open air; it never breaks or replaces a block.
- The hayride seats its riders by order of boarding; it has no driver and is pushed or powered like any minecart.
- The punch bowl holds only one punch, witch's brew; a berry from the item tag brews it.
- A barmbrack picks its ring's slice when it is placed; one placed by a command or a structure picks it the first time it is eaten, from the slices left, so it always holds exactly one ring.
- An outfit is worn on the head, so it can't be worn with a helmet; a chestplate, leggings and boots are drawn over it.
- The bat wings flap whenever there is nothing solid just under their wearer's feet: when jumping and falling, but also in water, on a ladder and standing at the very edge of a block.
- Outfits are drawn on players, armor stands and the humanoid mobs (zombies, skeletons and their kin); villagers don't wear them.
- An outfit retains its equippable asset ID, but has no flat equipment JSON file (also true of the Ghost Sheet). The missing-asset fallback draws no layers; `CostumeLayer` draws the outfit. The 10 October integration follow-up removed invalid empty layer-map files, preserving those IDs and the custom renderer while eliminating their resource-load errors.

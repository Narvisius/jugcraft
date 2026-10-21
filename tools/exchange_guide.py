"""The Exchange's own guide book (finance/, docs/features/finance-exchange.md).

The Exchange Guide item (finance/ExchangeGuide.java) and the Guide button on the Exchange screen open
client/ExchangeGuideScreen, which reads assets/jugcraft/exchange_guide/en_us.json written here. It covers only the
Exchange; the Engineer's Handbook stays about the rest of the mod.
"""
import pathlib

MOD = "jugcraft"
ITEM = "exchange_guide"
TITLE = "The Exchange: A Trader's Guide"


def pages():
    return [
        {"title": "Welcome to the Exchange", "icon": "minecraft:emerald", "text": [
            "The Exchange is the server's bank and market. Press semicolon (change it under Controls > Jugcraft) or type "
            "/jugcraft exchange to open it.",
            "Its tabs: Banking (your account), Economy (the central bank and the money supply), Equities "
            "(companies and their shares), Commodities (buy and sell items), Requests (ask for items), Contracts "
            "(pay other players) and Land (buy and protect chunks).",
            "Every deal is kept in your history on the Exchange. Only big casino wins and lottery jackpots are called out in chat.",
            "Lost this book? Type /jugcraft exchange guide for another, or press Guide on the Exchange screen."]},
        {"title": "Money", "icon": "minecraft:gold_nugget", "text": [
            "The Exchange deals in Jugs, the town's credit: the same Jugs you earn at the town's shops and send at its Jug "
            "Tellers. Your Jugs show on the Banking tab.",
            "Jugs are not an item: the server keeps them for you, so they cannot be lost, dropped or stolen from a chest.",
            "The Exchange's central bank keeps a reserve of Jugs. They reach players as loans and grants, and move "
            "between them through sales, contracts, requests, land and shares."]},
        {"title": "Banking", "icon": "minecraft:book", "text": [
            "The Banking tab shows your cash in hand, what you owe the central bank, your net worth (cash less debt) "
            "and how much more you could borrow.",
            "Recent Entries lists the latest ledger lines that involve you."]},
        {"title": "Your Starter Loan", "icon": "minecraft:gold_ingot", "text": [
            "Every new player can take one starter loan (100 Jugs unless the admins change it) from the Banking tab. Use "
            "it to buy your first goods from more advanced players.",
            "A loan costs a flat fee (10%): borrow 100 Jugs, repay 110. It is due 10 Minecraft days later (a day is 20 "
            "minutes of play). Banking shows the due day."]},
        {"title": "Paying It Back", "icon": "minecraft:emerald", "text": [
            "While you owe the bank, a quarter of everything you earn on the market (sales, auctions, filled "
            "requests) goes straight to your loan. Selling is how you pay it off.",
            "You can also repay any amount yourself on the Economy tab.",
            "Pay it off by the due day and your credit limit triples (up to three times), so you can borrow more "
            "for bigger projects."]},
        {"title": "Overdue Loans", "icon": "minecraft:clock", "text": [
            "Miss the due day and a one-time late fee of 10% is added. Until the loan is repaid you can still sell, "
            "but you cannot buy, bid, post paid requests, pay other players or borrow, and half of your sales go to "
            "the bank. Repeating contracts you pay for lapse.",
            "Nothing is taken from your inventory or base: sell your way out."]},
        {"title": "Borrowing More", "icon": "minecraft:gold_block", "text": [
            "On the Economy tab, drag the slider to choose an amount and press Borrow; the button shows what you "
            "will repay. You may borrow up to your credit limit.",
            "Borrowing more adds to the same loan and keeps its due day.",
            "Repaid money and fees return to the central bank's reserve."]},
        {"title": "Contracts", "icon": "minecraft:writable_book", "text": [
            "The Contracts tab pays another player: once, every day or every week. Type their name, the amount and "
            "what they give you in return, then press Sign & Pay.",
            "The first payment goes at once. Daily and weekly payments follow each Minecraft day or week.",
            "Either side can end a repeating contract with its End button. It also ends by itself if the payer runs "
            "out of money. The promise written on it is up to the players to keep."]},
        {"title": "Selling Goods", "icon": "minecraft:chest", "text": [
            "Hold what you want to sell in your main hand and open the Commodities tab. Choose how many to sell (they "
            "can come from anywhere in your inventory) and a price.",
            "Fixed price: buyers pay that much for each item and can buy part of the lot.",
            "Auction: set an opening bid for the whole lot and how long it runs (1, 3 or 7 days). The top bidder "
            "when it closes wins it.",
            "Listed items are held by the Exchange, so buyers get them even while you are offline. Take a listing "
            "down to get the items back (an auction that has a bid must run to its close)."]},
        {"title": "Buying Goods", "icon": "minecraft:gold_block", "text": [
            "Pick a listing on the Commodities tab. For a fixed price, type how many you want and press Buy. For an "
            "auction, type your bid (or leave it empty to bid the least allowed) and press Bid.",
            "A bid sets your money aside. If someone outbids you, it comes straight back.",
            "Items you buy go to your inventory. Anything that arrives while you are offline, or that does not fit, "
            "waits in your collection box: press Collect."]},
        {"title": "Requests", "icon": "minecraft:paper", "text": [
            "The Requests tab asks the server for items. Search for the item, pick it, say how many and the price "
            "you will pay for each, then press Post request. The money is set aside until someone delivers.",
            "Leave the price empty to post an advert instead: it just tells everyone what you want.",
            "To fill someone's request, pick it, type how many you will deliver and press Fill. The items leave "
            "your inventory and you are paid at once."]},
        {"title": "Buying Land", "icon": "minecraft:filled_map", "text": [
            "The Land tab shows a map of the Overworld chunks around you (each square is one chunk, 16 by 16 blocks). "
            "It is drawn like a vanilla map from the ground your game has already seen; places you have not visited "
            "stay blank paper. You are the white arrow. Click a chunk to select it.",
            "Gold chunks are yours, green ones are for sale or up for auction, red ones belong to others, blue ones "
            "belong to someone who trusts you.",
            "An unclaimed chunk costs the base price (25 Jugs unless the admins change it), and each chunk you already "
            "own makes the next dearer (10% unless the admins change it). That money goes to the central bank. The "
            "admins may also limit how many chunks one owner holds."]},
        {"title": "Protecting Land", "icon": "minecraft:iron_door", "text": [
            "Nobody else can break or place blocks, open doors and chests, use machines, or hurt animals and item "
            "frames on your land.",
            "To let a friend build, type their name under Trusted Builders and press Trust. They may then build on "
            "all your land. Press Remove to take it back.",
            "Explosions, fire, flowing fluids and pistons are not stopped yet."]},
        {"title": "Selling Land", "icon": "minecraft:oak_sign", "text": [
            "Select one of your chunks, type an asking price and press Sell. Anyone can then buy it from the Land tab; "
            "the money comes to you (and, if you owe the bank, part of it repays your loan).",
            "Press Unlist with the price box empty to stop selling. Give up hands the chunk back to the land office, "
            "with no refund."]},
        {"title": "Land Auctions", "icon": "minecraft:bell", "text": [
            "To auction a chunk, select it, type the opening bid in the price box, pick how long it runs (1, 3 or 7 "
            "days, the small button) and press Auction.",
            "To bid, select a chunk on auction (shown green), type your bid (or leave it empty to bid the least "
            "allowed) and press Bid. Your money is held until you are outbid, when it comes straight back.",
            "When the auction closes, the top bidder pays the owner and the chunk is theirs. With no bids, the owner "
            "keeps it. An owner can call off an auction that has no bids yet."]},
        {"title": "Chartering a Company", "icon": "minecraft:writable_book", "text": [
            "On the Equities tab, press Charter a company. Choose a name, a ticker (2 to 5 letters, like JUG) and "
            "how many shares the company has. The charter fee (250 Jugs unless the admins change it) goes to the bank.",
            "Use the slider (1% steps) or type an exact percentage, like 12.5, to offer part of the company to the public and set a price per share. Those shares go on "
            "the market as the company's own sell order; what they sell for goes into the company's treasury. You "
            "keep the rest of the shares.",
            "Draw a 16x16 logo by clicking the grid with a colour from the palette, or load a PNG (type its path and "
            "press Load, or drop the file on the window). Uploaded logos show once an admin approves them (unless "
            "the admins turned approval off)."]},
        {"title": "Issuing More Shares", "icon": "minecraft:paper", "text": [
            "A company's controller can raise money later: in Manage, type how many new shares and the price for each, "
            "then press Issue. The new shares go on the market as the company's own offer, and what they sell for goes "
            "into the treasury.",
            "New shares make everyone's part of the company smaller, the controller's too: issuing too many can cost "
            "you control. The admins may charge a fee to issue."]},
        {"title": "Trading Shares", "icon": "minecraft:paper", "text": [
            "Pick a company on the left and open Trade. Selling shows offers from the cheapest; Buying shows bids "
            "from the highest.",
            "Type how many shares and a price each, then press Buy or Sell. If someone is already selling at or "
            "below your price (or buying at or above it), you trade at once at their price. Otherwise your order "
            "waits on the book: a buy order holds its money, a sell order holds its shares. Cancel it any time.",
            "The price shown for a company is the price of its last trade."]},
        {"title": "Control and Dividends", "icon": "minecraft:gold_ingot", "text": [
            "Whoever holds more than half of a company's shares controls it. If nobody does, nobody does: buy shares "
            "to take it over.",
            "The controller's Manage view pays dividends from the treasury, shared out by shares held, and changes "
            "the logo. The Overview shows the ownership chart, the treasury and the last dividend and its yield.",
            "On the Land tab, the controller can zone their own chunks into the company (and back). Company land is "
            "run by whoever controls the company, and the players they trust may build on it."]},
        {"title": "Selling for a Company", "icon": "minecraft:gold_ingot", "text": [
            "If you control a company, the Commodities tab has an As: button beside List for sale. Press it to choose "
            "who sells: you, or one of your companies.",
            "A company's listing shows the company as the seller, and the money from every sale goes into its "
            "treasury. Take it down and the goods come back to you."]},
        {"title": "The Market Chest", "icon": "jugcraft:market_chest", "text": [
            "A clerk's counter that sells for your company. A company's controller buys one on the Equities tab: pick "
            "the company, open Manage and press Buy a Market Chest (200 Jugs unless the admins change it; the money "
            "goes to the central bank). Only a player who controls a company can set one up, and only that company's "
            "controller can open or break it.",
            "Put goods in it (by hand or with a hopper), then on its screen pick each kind of item and set a price. "
            "Every two seconds it lists what it holds on the Commodities market for your company; the goods move into "
            "the Exchange's keeping, so buyers get them even when you are far away.",
            "Pause stops new listings. Take back brings unsold goods back into the chest. If you control several "
            "companies, Switch changes which one it sells for. Hoppers can fill it but cannot empty it.",
            "Breaking it takes its goods off the market and drops them with everything inside."]},
        {"title": "Poker Tables", "icon": "jugcraft:poker_table_oval", "text": [
            "Texas hold'em for Jugs, played at a table 4 blocks wide and 2 deep. Place it from where you want the near "
            "long side; it reaches 4 blocks to your right and 2 away. Right-click any part to sit down or watch.",
            "Three tables, all crafted from vanilla things. The Gilded Oval: green wool on top, gold ingot, leather, "
            "gold ingot in the middle, dark oak planks along the bottom. The Saloon: green wool, then dark oak planks, "
            "then two sticks for legs at the corners. The Clockwork Dealer: green wool, then gold block, clock, gold "
            "block, then dark oak planks, redstone block, dark oak planks.",
            "Pick an empty seat, type your buy-in and press Sit here. Your chips are Jugs set aside from your account; "
            "Stand up turns what is left back into Jugs. Up to six players can sit at one table.",
            "Whoever places a table owns it. Its Settings button sets the small and big blinds, the least and most "
            "buy-in (within the admins' range) and the seconds each player has to act; leave a box blank for the "
            "server's setting. New blinds start with the next hand. Tables placed before this had no owner: break "
            "one and place it again to set it up."]},
        {"title": "Playing a Hand", "icon": "minecraft:clock", "text": [
            "With two or more players seated, the table deals by itself a few seconds later. The dealer button moves "
            "one seat each hand; the two players after it post the small and big blinds.",
            "Each player gets two cards. Betting goes round after the deal, after the flop (three shared cards), the "
            "turn and the river. On your turn: Fold, Check or Call, Raise to the amount in the box (Min, half pot and "
            "Pot fill it for you), or go All in.",
            "The best five cards from your two and the five shared ones win the pot. Players who went all in can only "
            "win what they matched; the rest goes into side pots for the others.",
            "If you take too long the table checks for you, or folds if there is a bet. Players who walk away or log "
            "off are stood up between hands with their chips. Breaking the table, or the world closing mid-hand, "
            "calls off the hand and gives every chip back."]},
        {"title": "The Casino", "icon": "jugcraft:roulette_table", "text": [
            "Blackjack, craps, roulette and baccarat tables, eight slot machines, an arcade cabinet, a Plinko board, a "
            "prize wheel, a coin pusher and a lottery kiosk, all crafted from vanilla things. The machines light up "
            "like the real thing: screens, bulbs and signs glow in the dark. Whoever "
            "places one owns it and is the house: lost bets go to the owner's Jugs and wins are paid from them. If the "
            "owner cannot pay, the central bank pays the winner and lends the owner what was short, on the usual "
            "loan terms, so winners are always paid. A game takes no bet bigger than its owner could cover.",
            "The owner's Settings button sets the least and most bet (within the admins' limits) and who is the house: "
            "the owner, or the server's central bank (then the bank pays wins and keeps losses, and the owner neither "
            "wins nor loses). Tables with a clock (blackjack, roulette, craps, baccarat, the prize wheel) can set "
            "their own betting seconds; a Plinko board can close Low, Medium or High risk; a lottery kiosk sets its "
            "days between draws and the house's cut of each ticket. The Takings column shows what the game has taken "
            "in, paid out and kept, its biggest win and how many bets, with a button to reset them. The owner is the "
            "only one besides an operator who can take the game down. An owner who plays their own game plays against "
            "the central bank. Breaking a game, or the world "
            "closing, gives back every bet still in play.",
            "Recipes (top, middle, bottom rows). Blackjack: green wool x3; gold ingot, paper, gold ingot; dark oak "
            "planks x3. Craps: green wool x3; bone, quartz block, bone; dark oak planks x3. Roulette: red, green and "
            "black wool; gold ingot, compass, gold ingot; dark oak planks x3. Fruit slots: iron ingot, glass pane, iron "
            "ingot; apple, redstone, apple; iron ingot x3. Clockwork slots: gold ingot, glass pane, gold ingot; clock, "
            "redstone, clock; gold ingot, lever, gold ingot. Gem Mine slots: deepslate bricks, glass pane, deepslate "
            "bricks; diamond, redstone, emerald; deepslate bricks, minecart, deepslate bricks. Progressive slots: "
            "glowstone, glass pane, glowstone; gold block, redstone block, gold block; obsidian x3.",
            "More recipes (top, middle, bottom). Nether Furnace slots: blackstone, glass pane, blackstone; magma block, "
            "redstone, magma block; nether bricks, blaze rod, nether bricks. Sunken Treasure slots: prismarine bricks, "
            "glass pane, prismarine bricks; heart of the sea, redstone, nautilus shell; prismarine bricks, sea lantern, "
            "prismarine bricks. Creeper Blast slots: green concrete, glass pane, green concrete; gunpowder, redstone, "
            "gunpowder; black concrete, TNT, black concrete. Pub Fruit Machine: oak planks, glass pane, oak planks; "
            "apple, redstone, sweet berries; iron ingot, stone button, iron ingot.",
            "Casino Arcade: black concrete, glass pane, black concrete; lapis lazuli, redstone, lapis lazuli; black "
            "concrete, stone button, black concrete. Plinko Board: iron nugget, glass pane, iron nugget; iron nugget x3; "
            "dark oak planks, redstone, dark oak planks. Prize Wheel: red, yellow and blue dye; gold ingot, gold block, "
            "gold ingot; dark oak planks x3. Coin Pusher: glass pane x3; iron ingot, piston, iron ingot; iron ingot, "
            "redstone, iron ingot. Baccarat: green wool x3; emerald, paper, emerald; dark oak planks x3. Lottery Kiosk: "
            "yellow concrete, glass pane, yellow concrete; paper, redstone, paper; iron ingot, hopper, iron ingot."]},
        {"title": "Blackjack", "icon": "jugcraft:blackjack_table", "text": [
            "Up to five players against the dealer. Pick a chip, then Sit & bet; press Deal when ready, or the cards "
            "come when the betting clock runs out. Get closer to 21 than the dealer without going over.",
            "Six decks, reshuffled once three quarters are dealt. The dealer stands on all 17s. Blackjack pays 3 to 2; "
            "other wins pay even money; ties push. Double on any two cards, split pairs up to four hands (split aces "
            "get one card each). When the dealer shows an ace you may insure for half your bet; it pays 2 to 1. "
            "Played well, the house keeps about half a Jug in every hundred bet."]},
        {"title": "Roulette", "icon": "jugcraft:roulette_table", "text": [
            "An American wheel: 0, 00 and 1 to 36. Click the layout to bet the chip you picked: a number (pays 35 to 1), "
            "the line between two (17), the strip under a column for its street (11), a corner (8), between two "
            "streets for a six line (5), the strip under the zeros for 0-00-1-2-3 (6), dozens and columns (2), and "
            "red, black, odd, even, 1-18 and 19-36 (even money).",
            "Bets close when the clock runs out (or press Spin). The house edge is 5.26%, or 7.89% on the five-number "
            "bet: the same as a casino's double-zero wheel."]},
        {"title": "Craps", "icon": "jugcraft:craps_table", "text": [
            "The shooter's first roll is the come-out: 7 or 11 wins the Pass Line, 2, 3 or 12 loses it. Any other "
            "number is the point; the line wins if the point rolls again before a 7. A 7 first is a seven-out, and the "
            "dice pass to the next player. Don't Pass is the reverse (12 is a push).",
            "Once a point is on: Come and Don't Come work like the line from the next roll; take Odds behind a line or "
            "come bet (up to 3, 4 and 5 times on 4/10, 5/9 and 6/8), which pay true odds with no house edge. Place "
            "bets pay 9:5 on 4 and 10, 7:5 on 5 and 9, 7:6 on 6 and 8. One-roll bets: the Field (2 pays double, 12 "
            "triple), any seven 4:1, any craps 7:1, eleven 15:1, two or twelve 30:1. Hardways 7:1 and 9:1.",
            "The shooter presses Roll; if nobody rolls, the dice go when the clock runs out. Place bets, odds and "
            "line bets with no point yet can be taken down; a Pass or Come bet stays until it is decided."]},
        {"title": "Slot Machines", "icon": "jugcraft:slot_machine_classic", "text": [
            "Pick a bet and pull the lever. Every spin is decided by the server from real reel strips, so each "
            "machine pays back a fixed share of what is bet over time: Fruit 95%, Clockwork 94%, Gem Mine 92% and "
            "the Progressive 88% plus its jackpot. The paytable is on each machine.",
            "Fruit: three reels, one line; cherries pay anywhere, any three bars pay 5, three sevens 200. Clockwork: "
            "the clock is wild and doubles a win for each one in it; three clocks pay 1,000. Gem Mine: five reels and "
            "nine lines, paying three, four or five ores in a row from the left, per line bet; five diamonds pay "
            "2,000. Progressive: two Jugs in every hundred bet build the jackpot shown on top; three jackpot symbols "
            "(about one spin in 33,000) win it all at the machine's top bet, or a share at a smaller bet.",
            "One player uses a machine at a time; it frees itself when they walk away."]},
        {"title": "More Slot Machines", "icon": "jugcraft:slot_machine_treasure", "text": [
            "Nether Furnace (93%): three reels, one line. Three netherite pay 250, blazes 60, ghasts 30, magma 15, "
            "nuggets 8; two nuggets pay 3 and one pays 1. A blaze rod on the third reel spins the bonus wheel on top "
            "for 2x to 50x the bet.",
            "Sunken Treasure (94%): five reels, twenty lines; the heart of the sea pays 50, 200 or 1,000 per line bet "
            "for three, four or five. Three or more chests anywhere pay 2, 10 or 50 times the bet and give ten free "
            "spins where every win is tripled.",
            "Creeper Blast (92%): five reels, ten lines; five swords pay 1,500 per line bet. A creeper head on reel "
            "2, 3 or 4 blows up and turns that whole reel wild.",
            "Pub Fruit Machine (95%): three reels, one line, 7 7 7 pays 100. Now and then it offers HOLD (pick reels to "
            "keep where they are for the next spin) or NUDGES (step a reel down one symbol, free); Collect gives up "
            "any nudges left.",
            "Shark Frenzy (95%): five reels, twenty lines; five divers pay 500 per line bet. A shark on reel 2, 3 or 4 "
            "turns its reel wild and swims one reel left, turning that one wild too. Recipe: blue concrete, glass "
            "pane, blue concrete; cod, redstone, cod; blue concrete, iron ingot, blue concrete.",
            "Golden Dragon (93%): five reels and 243 ways: matching symbols pay on reels side by side from the left, "
            "in any row, per coin (a spin is 25 coins); the dragon is wild on reels 2 to 4. A pearl on every reel "
            "starts HOLD & SPIN: the pearls stay with their prizes (1x to 50x the bet), each new pearl gives three "
            "more respins, and filling all fifteen spots wins the GRAND, 500x the bet. Recipe: red concrete, glass "
            "pane, red concrete; lantern, redstone, lantern; red concrete, gold block, red concrete."]},
        {"title": "The Arcade", "icon": "jugcraft:casino_arcade", "text": [
            "One cabinet, five games picked from its menu. Nothing pays more than 1,000 times the bet.",
            "Video Poker: 9/6 Jacks or Better. You get five cards, hold any, draw once. A royal flush pays 800, a "
            "straight flush 50, four of a kind 25, a full house 9, a flush 6, a straight 4, three of a kind 3, two "
            "pair 2, jacks or better 1. Played well it pays back about 99.5%.",
            "Keno: pick 1 to 10 of 80 numbers; twenty are drawn and you are paid by how many you caught (the table "
            "is on screen). Pays back 75% to 92% depending on how many you pick.",
            "Mines: a 5x5 grid with the mines you choose hidden in it. Every safe tile raises the multiplier; cash out "
            "any time, or lose the bet on a mine (99% of a fair price). Crash: the multiplier climbs until it bursts; "
            "cash out before it does, or set an automatic cash-out (99%). Hi-Lo: guess if the next card is higher or "
            "lower; each right guess multiplies the win by the odds against it (98%). Cashing out before any step "
            "gives the bet back."]},
        {"title": "Plinko and the Wheel", "icon": "jugcraft:plinko_board", "text": [
            "Plinko: drop a ball through twelve rows of pegs into one of thirteen slots. Low risk pays 0.5x to 10x, "
            "medium 0.3x to 33x, high 0.2x to 170x; the middle slots are the likeliest. All three pay back about 99%.",
            "Prize Wheel (Big Six): a 54-segment wheel. Bet on 1, 2, 5, 10 or 20 (they pay that many to 1), or the "
            "Joker or the Jug (40 to 1). The house keeps 11% on the 1 and up to 24% on the Joker and Jug. The wheel "
            "spins when the clock runs out or someone presses Spin."]},
        {"title": "Baccarat", "icon": "jugcraft:baccarat_table", "text": [
            "Bet on Player, Banker or Tie; the cards are dealt by the fixed rules of punto banco from eight decks. "
            "Hands count their last digit (tens and face cards are 0); closest to 9 wins.",
            "Player pays 1 to 1 (house edge 1.24%), Banker 0.95 to 1 (1.06%), Tie 8 to 1 (14.4%); on a tie, Player "
            "and Banker bets push. The road shows the last results."]},
        {"title": "Coin Pusher", "icon": "jugcraft:coin_pusher", "text": [
            "Drop a coin of the chip size you pick onto the shelf. The pusher slides back and forth and now and then "
            "shoves part of the heap over the edge: nine tenths of what falls lands in your tray, the rest goes off "
            "the sides to the house. The heap stays for the next player. Over time it pays back about 88%.",
            "When a coin pusher is broken, whatever is on its shelf goes to its owner (the house)."]},
        {"title": "The Derby Table", "icon": "jugcraft:derby_table", "text": [
            "A horse-racing table 4 blocks wide, 3 deep and 2 high. Six miniature horses race once round the oval "
            "under its dome, and everyone nearby can watch them run. Recipe: glass pane x3; saddle, clock, saddle; "
            "green wool, hay bale, green wool.",
            "Each race has a card: six horses with their silks, form (their last five finishes) and live prices. Click "
            "a price to bet the chip you picked on that horse to Win (first), Place (first or second) or Show (top "
            "three). A price is what one Jug pays back if it comes in, your stake included; prices move as people bet.",
            "Betting pools work as at a real track: the house takes its cut of each pool (15% unless the owner changes "
            "it) and the rest goes to the winning tickets. The house backs every pool with a large stake spread by "
            "each horse's real chances, so the prices stay fair less the cut however few people bet.",
            "The race starts when the betting clock runs out or a bettor presses Start race. The winner is decided by "
            "each horse's chances when the gates open; the results are paid when the last horse is home."]},
        {"title": "Lottery and Scratch Cards", "icon": "jugcraft:lottery_kiosk", "text": [
            "Lottery: buy tickets at the kiosk (the price is the kiosk's least bet). Most of every ticket goes into "
            "the pot and the rest to the house (one tenth unless the owner changes it). Every few days (seven unless "
            "the owner or the admins change it) one ticket "
            "is drawn and wins the whole pot; the more tickets you hold, the better your chance. Breaking the kiosk "
            "refunds every ticket.",
            "Scratch cards: Copper (1 Jug, up to 100), Iron (5, up to 1,000) and Gold (25, up to 5,000). Buy one at a "
            "kiosk, then use it to scratch: rub the nine panels with the mouse, or Reveal all. Three of the same "
            "amount win it. The prize is decided and paid by the central bank the moment you use the card; scratch "
            "cards pay back about 65%, like real ones. Only paid tickets issued by a kiosk can redeem a prize; "
            "alchemy-created blanks and copies do not create another paid ticket. "
            "If the bank cannot pay a prize immediately, the unpaid winnings are saved and paid automatically when funded."]},
        {"title": "The Economy", "icon": "minecraft:clock", "text": [
            "Held by players: all the cash in players' accounts. Central reserve: what the central bank holds.",
            "Total money: cash, money set aside for bids and requests, the reserve, and the goods on the market at "
            "their listed prices.",
            "Inflation: how much the price of traded goods changed since the last Minecraft day, on average.",
            "Wealthiest Holders: the ten players with the most cash (goods on the market are not counted)."]},
        {"title": "For Admins", "icon": "minecraft:command_block", "text": [
            "Operators run the central bank. In singleplayer, turn on cheats (Open to LAN, Allow Cheats) to be one.",
            "On the Economy tab, the Admin button opens Server Settings: every number the Exchange uses, in one list. "
            "Type a new value beside a setting and press Set. Percentages are typed like 12.5, toggles as on or off.",
            "The settings: the loan fee, starter loan, days to repay, share of sales that repays a loan, late fee and "
            "how credit grows; a market sales tax (paid to the reserve), how much each bid must beat the last, and how "
            "many listings and requests each player may have; the base land price, how much dearer each chunk owned "
            "makes the next, and the most chunks anyone may own; the charter fee, the fee to issue shares, how many "
            "companies a player may found and the Exchange lists, open share orders per player, whether uploaded logos "
            "need approval, and how many Market Chests a company may run; the poker blinds, the smallest and largest "
            "buy-in and the seconds each player has to act; the casino's least and most bet, betting and turn "
            "clocks, the Progressive jackpot's starting meter, and the days between lottery draws.",
            "The same from chat: /jugcraft exchange admin settings lists them, and /jugcraft exchange admin set "
            "<setting> <value> changes one (for example set land_growth 5).",
            "Loan commands: /jugcraft exchange admin fee <percent>, limit <amount>, term <days>, salesshare <percent>, debts "
            "(who owes what), forgive <player> [amount], resetstarter <player>.",
            "Money commands: print <amount> (add to the reserve), retire <amount>, grant <player> <amount>, nextday "
            "(close the day early) and chatlog everyone|parties (whether big casino wins are told to everyone or only the winner).",
            "Admins can take down any listing or request; the items and money go back to their owners.",
            "Land commands: /jugcraft exchange admin land price <amount> (base chunk price), land unclaim (the chunk you stand "
            "in) and land give <player>. Admins may build anywhere and can remove any claim from the Land tab.",
            "Company commands: /jugcraft exchange admin corp fee <amount>, corp approvelogo <ticker> and corp removelogo <ticker>; "
            "admins also approve logos and can cancel any order from the Equities tab."]},
        {"title": "Coming Soon", "icon": "minecraft:filled_map", "text": [
            "Protection from explosions, fire, fluids and pistons on claimed land."]},
    ]


def write_assets(write, rid, assets, data, lang):
    """The guide's pages, its item model and its name. There is no recipe: every player is given one on first joining,
    and /jugcraft exchange guide hands out another."""
    assets = pathlib.Path(assets)
    write(assets / "exchange_guide" / "en_us.json", {"title": TITLE, "pages": pages()})
    write(assets / "models" / "item" / f"{ITEM}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{ITEM}")}})
    write(assets / "items" / f"{ITEM}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{ITEM}")}})
    lang[f"item.{MOD}.{ITEM}"] = TITLE
    lang[f"message.{MOD}.exchange_guide.given"] = "You got the Exchange guide. Right-click it to read how the bank and markets work."

"""The walled town's English text (tools/town_assets.py writes it into the lang file): names, screens, messages, and
what townsfolk say in each theme. Every line is original."""

THEME_NAMES = {
    "spring": "Spring", "summer": "Summer", "autumn": "Autumn", "winter": "Winter",
    "halloween": "Halloween", "harvest": "the Harvest Feast", "december": "December",
}

# What each kind of townsperson says, three lines per theme (the line shown changes with time and person).
LINES = {
    "townsfolk": {
        "spring": ["The tulips came up early this year. Lovely, aren't they?", "Fresh air and new leaves. The whole town smells of blossom.",
                   "Mind the puddles by the fountain, the spring rains have been generous."],
        "summer": ["Warm days and long evenings. Best time to sit by the fountain.", "The market's busy in summer. Have you seen the flower stall?",
                   "Keep to the shade at midday, friend."],
        "autumn": ["The leaves are turning gold outside the walls. Worth a walk.", "Pumpkins in every window box! Autumn's my favourite.",
                   "Bring your harvest to the General Store; they pay fair Jugs."],
        "winter": ["Cold enough to freeze the fountain! Well, nearly.", "Wrap up warm. The wind comes straight down the main street.",
                   "The snowman in the square has better posture than me."],
        "halloween": ["Did you see the soul lanterns? Gives me the shivers.", "I'm going as a pumpkin this year. Again.",
                      "The pile of jack o'lanterns in the square glows all night!"],
        "harvest": ["The Harvest Feast! The table in the square is groaning with food.", "Give thanks and share the pie, that's what my gran said.",
                    "Hay bales everywhere. Mind you don't sneeze."],
        "december": ["Have you seen the great tree in the square? The baubles glow!", "Red and green banners on every house. Merry December!",
                     "I've wrapped my presents in wool. Is that normal?"],
    },
    "guard": {
        "spring": ["All quiet at the gate. Just bees.", "Keep your blade sheathed in town, traveller.", "Nothing gets past these walls."],
        "summer": ["Long watch in the sun. Still, nothing gets past me.", "Monsters don't like the lamps. Neither do I, in this heat.",
                   "Move along, the square's that way."],
        "autumn": ["Dark comes early now. I'll be watching.", "Seen a creeper skulking by the east gate. Not anymore.",
                   "The town's safe. You have my word."],
        "winter": ["Frozen solid on the wall walk. Still standing.", "Snow hides footprints. Not from me.", "Stay inside the walls after dark."],
        "halloween": ["Skeletons in costume are still skeletons. I check.", "Boo yourself.", "The decorations are fake. The zombies aren't."],
        "harvest": ["Even guards get a slice of pie at the Feast.", "Guarding the pie table is a serious duty.", "All quiet, and well fed."],
        "december": ["A guard's December: lanterns, snow and no surprises.", "Somebody keeps putting tinsel on my helmet.",
                     "The tree in the square is under my protection."],
    },
    "decorator": {
        "spring": ["Out with the old, in with the tulips!", "Pink and white for the stalls this spring. What do you think?",
                   "Every window box gets fresh flowers. Every single one."],
        "summer": ["Yellow banners for summer. Sunny, like me.", "The flower bed in the square was my idea.", "Lanterns stay, everything else goes bright."],
        "autumn": ["Pumpkins, hay and orange banners. Perfect.", "I've carried more pumpkins this week than a farmer.",
                   "Autumn's easy: if it's orange, it goes up."],
        "winter": ["Snow blocks for the planters, spruce for the windows.", "The snowman's coming along nicely.", "White and pale blue. Very wintry."],
        "halloween": ["Soul lanterns on every post. Spooky!", "Black and orange stripes for the stalls. Mwahaha.", "Cobwebs? Those are deliberate."],
        "harvest": ["The feast table's set, hay's stacked, pumpkins polished.", "Brown and gold for the Harvest Feast.",
                    "Carrying a whole cake across the square is harder than it looks."],
        "december": ["That tree took me all morning. Worth it.", "Red and white awnings, like peppermint.", "Presents under the tree are just wool. Shh."],
    },
    "priest": {
        "spring": ["Spring brings new life. Welcome to the church.", "The bells ring for every season.", "Peace be with you, traveller."],
        "summer": ["The rose window shines brightest in summer.", "Rest awhile in the cool of the nave.", "Peace be with you."],
        "autumn": ["We give thanks for the harvest.", "The days grow short; the candles burn longer.", "Peace be with you."],
        "winter": ["Even in winter, the doors are open.", "Warm yourself by the candles.", "Peace be with you."],
        "halloween": ["Fear not the lanterns. They are only pumpkins.", "All souls are welcome here.", "Peace be with you, and with your costume."],
        "harvest": ["Share what you have; that is the feast.", "We give thanks together.", "Peace be with you."],
        "december": ["The longest nights bring the brightest lights.", "Goodwill to all who pass the gates.", "Peace be with you."],
    },
    "mayor": {
        "spring": ["Welcome to our town! Mind the flower boxes.", "The town council approves of spring.", "Our walls keep the town as it ever was."],
        "summer": ["A fine summer for trade. Visit the market!", "The Jug Teller in the bank keeps your Jugs safe.",
                   "Our shops buy fair and sell fair. I insist."],
        "autumn": ["Harvest time! The General Store is buying.", "The council has approved more pumpkins.", "Enjoy our town, and spend your Jugs."],
        "winter": ["The town stands firm, winter or no.", "The snowman has been elected to the council.", "Stay warm, visitor."],
        "halloween": ["The council regrets nothing about the decorations.", "Trick or treat? The mayor has neither.", "Happy Halloween!"],
        "harvest": ["Welcome to the Harvest Feast!", "Eat, drink and buy something at the stalls.", "A bountiful year for the town."],
        "december": ["Season's greetings from the town council!", "The great tree was approved unanimously.", "Merry December, visitor!"],
    },
}

SHOP_NAMES = {"general": "General Store", "seasonal": "Seasonal Stall", "curios": "Curiosities", "florist": "Florist"}


def lang():
    out = {
        "block.jugcraft.atm": "Jug Teller",
        "entity.jugcraft.townsfolk": "Townsperson",
        "message.jugcraft.town.protected": "The town is protected: you can't change it here",
        "message.jugcraft.town.decorating": "The townsfolk are decorating for %s",
        "message.jugcraft.town.welcome": "Welcome to the town! Here are %s Jugs to start you off. Spend them at the market.",
        "message.jugcraft.townsfolk.says": "%s: %s",
        "message.jugcraft.townsfolk.banker": "You hold %s Jugs. Use a Jug Teller to send Jugs to another player.",
        "message.jugcraft.shop.out_of_season": "That isn't on sale this time of year",
        "message.jugcraft.shop.too_few_jugs": "That costs %s Jugs",
        "message.jugcraft.shop.too_few_items": "You need %s %s to sell",
        "message.jugcraft.shop.purse_full": "Your Jugs can't hold any more",
        "message.jugcraft.shop.bought": "Bought %s %s for %s Jugs",
        "message.jugcraft.shop.sold": "Sold %s %s for %s Jugs",
        "message.jugcraft.atm.choose": "Choose who to send to and how many Jugs",
        "message.jugcraft.atm.offline": "%s is not online",
        "message.jugcraft.atm.refused": "The Jug Teller can't send that",
        "message.jugcraft.atm.sent": "Sent %s Jugs to %s",
        "message.jugcraft.atm.received": "You received %s Jugs from %s",
        "screen.jugcraft.jugs": "Jugs: %s",
        "screen.jugcraft.price": "%s Jugs",
        "screen.jugcraft.shop.buy": "Buy",
        "screen.jugcraft.shop.sell": "Sell",
        "screen.jugcraft.shop.buy_one": "Buy",
        "screen.jugcraft.shop.sell_one": "Sell",
        "screen.jugcraft.shop.empty": "Nothing here today. Come back another season!",
        "screen.jugcraft.atm.to": "Send to:",
        "screen.jugcraft.atm.nobody": "No one else is online",
        "screen.jugcraft.atm.recipient": "To: %s",
        "screen.jugcraft.atm.amount": "Amount: %s",
        "screen.jugcraft.atm.clear": "Clear",
        "screen.jugcraft.atm.all": "All",
        "screen.jugcraft.atm.send": "Send",
        "command.jugcraft.town.none": "There is no town in this world",
        "command.jugcraft.town.info": "The town's middle is at %s %s %s; it is decorated for %s",
        "command.jugcraft.town.overworld": "The town can only be placed in the Overworld",
        "command.jugcraft.town.exists": "This world already has a town",
        "command.jugcraft.town.placed": "The town will be built round you as its chunks load",
        "command.jugcraft.town.no_theme": "No theme called %s",
        "command.jugcraft.town.theme": "The town is now decorated for %s",
        "command.jugcraft.jugs.balance": "You hold %s Jugs",
        "command.jugcraft.jugs.too_few": "They don't have that many Jugs",
        "command.jugcraft.jugs.changed": "%s now holds %s Jugs",
    }
    for theme, name in THEME_NAMES.items():
        out[f"theme.jugcraft.{theme}"] = name
    for shop, name in SHOP_NAMES.items():
        out[f"shop.jugcraft.{shop}"] = name
    for role, themes in LINES.items():
        for theme, lines in themes.items():
            for i, line in enumerate(lines):
                out[f"message.jugcraft.townsfolk.{role}.{theme}.{i}"] = line
    return out

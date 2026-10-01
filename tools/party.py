"""Chat messages for the /party commands (party/PartyCommands.java).

generate_material_data.py adds these to the English language file, and check_mod_data.py
verifies that every PartyManager.Result has an error message.
"""

MOD = "jugcraft"

PARTY_LANG = {
    "none": "You are not in a party. Pending invites: %s. Use /party invite <player> to start one.",
    "roster": "Party (%s/%s): %s",
    "created": "Party created. Invite players with /party invite <player>.",
    "invite_sent": "Invited %s. The invite expires in 5 minutes.",
    "invite_received": "%s invited you to their party. Type /party accept or /party decline.",
    "joined": "%s joined the party.",
    "declined": "Invite declined.",
    "left": "You left the party.",
    "member_left": "%s left the party.",
    "kicked": "%s was removed from the party.",
    "you_were_kicked": "You were removed from the party.",
    "new_leader": "%s is now the party leader.",
    "disbanded": "The party was disbanded.",
    "error.ok": "Done.",
    "error.disabled": "Parties are turned off on this server.",
    "error.self": "You can't do that to yourself.",
    "error.already_in_party": "You are already in a party. Leave it first with /party leave.",
    "error.not_in_party": "You are not in a party.",
    "error.not_leader": "Only the party leader can do that.",
    "error.not_member": "That player is not in your party.",
    "error.target_in_party": "That player is already in a party.",
    "error.party_full": "The party is full.",
    "error.already_invited": "That player already has an invite from your party.",
    "error.no_invite": "You have no pending party invite.",
    "error.rate_limited": "You are sending invites too quickly. Wait a minute and try again.",
}


def party_lang(lang):
    for key, text in PARTY_LANG.items():
        lang[f"message.{MOD}.party.{key}"] = text

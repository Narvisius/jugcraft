"""Presentation text for the constellation composer and illustrated encyclopedia (OpenAI Codex)."""
TEXT = {
    'close': 'Close', 'contents': 'Contents', 'refresh': 'Refresh', 'details': 'Details', 'less': 'Less',
    'undo': 'Undo', 'reset': 'Reset', 'load': 'Load', 'wipe': 'Erase', 'more': 'More',
    'paths': 'Path %s', 'compose.enhance': 'Enhance: %s',
    'branch': 'Path %s/%s', 'branch.add': '+ Branch', 'branch.remove': '- Branch',
    'stage.0': 'Reach', 'stage.1': 'Target', 'stage.2': 'Effect', 'stage.3': 'Refine',
    'stage.hint.0': 'Choose how the spell reaches the world.',
    'stage.hint.1': 'Choose what your spell affects.',
    'stage.hint.2': 'Choose up to three effects. Select an effect again to remove it.',
    'stage.hint.3': 'Optional: select a part, then a compatible enhancement. Select again to remove it.',
    'compose.subtitle': 'Weave a spell among the stars', 'compose.preview': 'Your spell',
    'compose.ready': 'Ready to inscribe', 'compose.inscribed': 'Inscribed on your wand', 'compose.cost_compact': '%s Focus  ·  %ss', 'compose.cost': '%s Focus  /  %ss cooldown',
    'compose.capacity': 'Capacity  %s / %s',
    'compose.start': 'Begin with Reach. Choose a star, then a target and an effect. Inscribe when your spell is ready.',
    'compose.locked': '%s stars still to discover. Continue your research to learn more.',
    'compose.no_words': 'No stars learned here yet. Continue your research in the Journal.',
    'compose.need_wand': 'Hold a composing wand in your main hand to begin.',
    'word.here': 'At your feet', 'word.touch': 'Touch', 'word.ray': 'Light ray',
    'word.struck': 'One target', 'word.creatures': 'Creatures', 'word.spread': 'Nearby blocks', 'word.allies': 'Your party',
    'word.light': 'Create light', 'word.reveal': 'Reveal', 'word.ward': 'Protect', 'word.sear': 'Sear', 'word.dazzle': 'Dazzle',
    'word.intensify': 'Stronger', 'word.prolong': 'Longer', 'word.extend': 'Further', 'word.widen': 'Wider', 'word.pulse': 'Repeat',
    'book.volume': 'The Jugcraft Field Guide', 'book.intro': 'A world to discover. A craft to make your own.',
    'book.technology': 'Technology', 'book.technology.hint': 'Materials, machines & factories',
    'book.magic': 'Magic', 'book.magic.hint': 'Schools, workshops & rituals',
    'book.discover': 'First steps', 'book.discover.hint': 'Your next discovery',
    'book.spellcraft': 'Spellcraft', 'book.spellcraft.hint': 'Your wand, spells & composition',
    'book.expedition': 'Bestiary & expeditions', 'book.expedition.hint': 'The keepers you have encountered',
    'book.keys': 'Controls', 'book.keys.hint': 'Keys, tools & useful shortcuts',
    'book.inspect': 'Field inspection', 'book.inspect.hint': 'Read the workshop in front of you',
    'book.empty': 'No field notes here yet. Discover this subject in the world to add it to your guide.',
    'book.loading': 'Opening your field notes...', 'book.topics': 'Select a topic', 'book.topic': 'Field notes  %s / %s',
    'book.chapters': 'Topics', 'book.progression': 'Milestones', 'book.choose_topic': 'Choose a topic on the left. Open its chapter for recipes, uses and detailed guidance.',
    'book.read': 'Read chapter  >', 'book.guides': 'Workshop guides', 'book.field_notes': 'Field notes',
}


def lang_entries(lang):
    lang.update({'ui.jugcraft.' + key: value for key, value in TEXT.items()})

"""The plasma rifle's item and sounds (Justin approved the plasma rifle sheet on 6 November 2026; the sounds are his picks
from a library he subscribes to, 6 November 2026; the item remade as the NT-7, take 3 of rifle A, 9 November 2026).
Security carry it (tower/crew/TowerCrew: the item drawn in their hands, client/TowerCrewRenderer); players can have one
from the creative menu (tower/crew/PlasmaRifle).

  write_item()     the item: tools/plasma_rifle_nt7.py (its definition, its models by charge, its texture, the reticle)
  write_sounds(SRC) the sounds from two library files in the folder SRC (not kept in this repository, only what's made
                   from them): the shot, "Future Weapons 3 - Plasma Hand Gun - Shot 1", cut where Justin wanted it to
                   stop (0.915 s, at the quiet after the shot, a 25 ms fade) with its gun clinks taken out (from the
                   shot itself; above 1.2 kHz gone from the metal clink after it), and the hit, "Robotic Lifeforms 2 - Impact
                   - Energy Cyborg Impact 02", whole; OGG Vorbis, mono, into sounds/crew/ and sounds.json
                   (plasma_rifle.fire, plasma_bolt.hit)
"""
from __future__ import annotations

import json
import subprocess
import tempfile
from pathlib import Path

import numpy as np
from scipy.io import wavfile

import plasma_rifle_nt7

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"


def write_item(assets=ASSETS):
    plasma_rifle_nt7.write(assets)


# ------------------------------------------------------------------ the sounds

SR = 48000
FIRE_WAV = "Future_Weapons_3_-Plasma_Hand_Gun_-Shot_1.wav"
HIT_WAV = "Robotic_Lifeforms_2_-Impact_-Energy_Cyborg_Impact_02.wav"
FIRE_CUT, FIRE_FADE = 0.915, 0.025       # Justin's cut ("I kind of want the sound to stop there"): at the quiet after the shot
# the gun's mechanical clinks taken out (Justin: "it's like a shotgun. There's that clink sound. Get rid of that"): the two
# clicks before the shot (it starts at the shot itself, 0.169 s in) and the metal clink after it (0.61-0.71 s, ringing at 9
# and 10.5 kHz): from there on only what's under 1.2 kHz (the plasma's low tail) is kept
FIRE_START, CLINK_AT, CLINK_CUT = 0.165, 0.605, 1200.0


def load(path):
    """A library file as mono at 48 kHz."""
    out = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", str(path), "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(out, np.float32).astype(float)


def fire(src):
    from scipy import signal
    x = load(Path(src) / FIRE_WAV)[int(FIRE_START * SR):int(FIRE_CUT * SR)]
    t = np.arange(len(x)) / SR + FIRE_START
    low = signal.sosfiltfilt(signal.butter(4, CLINK_CUT, btype="low", fs=SR, output="sos"), x)
    w = np.clip((t - (CLINK_AT - 0.0075)) / 0.015, 0, 1)
    w = w * w * (3 - 2 * w)
    x = x * (1 - w) + low * w
    x *= np.minimum((t - FIRE_START) / 0.003, 1)
    n = int(FIRE_FADE * SR)
    x[-n:] *= np.cos(np.linspace(0, np.pi / 2, n)) ** 2
    return x / np.abs(x).max() * 0.89


def hit(src):
    x = load(Path(src) / HIT_WAV)
    return x / np.abs(x).max() * 0.89


def ogg(x, path):
    with tempfile.TemporaryDirectory() as d:
        wav = Path(d) / "x.wav"
        wavfile.write(wav, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-ac", "1", "-c:a", "libvorbis", "-q:a", "7", str(path)], check=True)


def write_sounds(src, assets=ASSETS):
    """The shot and the hit from the two library files in `src` (Justin's picks, from a library he subscribes to; they
    aren't kept in this repository, only what's made from them)."""
    assets = Path(assets)
    out = assets / "sounds/crew"
    out.mkdir(parents=True, exist_ok=True)
    ogg(fire(src), out / "plasma_rifle_fire.ogg")
    ogg(hit(src), out / "plasma_bolt_hit.ogg")
    write_sound_events(assets)


def write_sound_events(assets=ASSETS):
    assets = Path(assets)
    sounds = json.loads((assets / "sounds.json").read_text())
    sounds["plasma_rifle.fire"] = {"subtitle": "subtitles.jugcraft.plasma_rifle.fire",
                                   "sounds": [{"name": "jugcraft:crew/plasma_rifle_fire", "attenuation_distance": 32}]}
    sounds["plasma_bolt.hit"] = {"subtitle": "subtitles.jugcraft.plasma_bolt.hit", "sounds": [{"name": "jugcraft:crew/plasma_bolt_hit"}]}
    (assets / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n")


if __name__ == "__main__":
    import sys
    write_item()
    if len(sys.argv) > 1:
        write_sounds(sys.argv[1])
        print("plasma rifle item and sounds written")
    else:
        write_sound_events()
        print("plasma rifle item written (sounds kept: give the folder with the library files to rebuild them)")

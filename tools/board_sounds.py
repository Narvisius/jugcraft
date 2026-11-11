"""The control room's annunciator horn (round 27, nuclear/ControlBoard.java), synthesised here (no recorded or
third-party audio): an electromechanical alarm horn, two blasts of its buzzing tone (the vibrating diaphragm at 470 Hz,
its odd harmonics, the 120 Hz rattle of its coil), played every two seconds while a window is unacknowledged.

Writes assets/jugcraft/sounds/control_room/annunciator.ogg (needs numpy and ffmpeg with libvorbis); its sounds.json
entry is SOUNDS, written with every other sound by tools/generate_material_data.py.
"""
import os
import subprocess

import numpy as np

RATE = 44100
HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "src/main/resources/assets/jugcraft/sounds/control_room")
SOUNDS = {"control_room.annunciator": {"subtitle": "subtitles.jugcraft.control_room.annunciator",
                                       "sounds": [{"name": "jugcraft:control_room/annunciator", "attenuation_distance": 32}]}}
LANG = {"subtitles.jugcraft.control_room.annunciator": "Annunciator horn sounds"}


def horn():
    dur = 1.35
    n = int(RATE * dur)
    t = np.arange(n) / RATE
    f0 = 470
    tone = sum(np.sin(2 * np.pi * f0 * h * t) / h ** 0.9 for h in (1, 3, 5, 7, 9))
    tone = np.tanh(tone * 1.6)
    rattle = 0.75 + 0.25 * np.sign(np.sin(2 * np.pi * 120 * t))
    gate = np.zeros(n)
    for a, b in ((0.0, 0.55), (0.72, 1.27)):
        m = (t >= a) & (t < b)
        tt = t[m] - a
        gate[m] = np.minimum(1, tt / 0.015) * np.minimum(1, (b - a - tt) / 0.03)
    return tone * rattle * gate


def write(name, signal):
    os.makedirs(OUT, exist_ok=True)
    signal = np.tanh(signal / np.max(np.abs(signal)) * 1.2) / np.tanh(1.2) * 0.8
    pcm = (signal * 32767).astype("<i2").tobytes()
    path = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", path], input=pcm, check=True)
    print("wrote", path)


if __name__ == "__main__":
    write("annunciator", horn())

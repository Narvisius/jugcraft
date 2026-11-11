"""The crew drop ship's sounds, cut from two NASA launch recordings (NASA audio is not copyrighted; credit: NASA), from
NASA's historical sounds page (https://www.nasa.gov/historical-sounds/):

  STS-131: Sound of Launch     590189main_ringtone_131_launchNats.mp3 (the Shuttle's roar and crackle, no commentary)
  Atlas V: Launch              590329main_ringtone_SDO_launchNats.mp3 (a deep, distant rumble, no commentary)

  crew_ship/entry.ogg     the roar as it comes in from out of sight: the rumble under the crackle, swelling in (13 s)
  crew_ship/thrust.ogg    the thrusters burning: a seamless loop of the Shuttle's steady roar with the rumble under it
  crew_ship/idle.ogg      idling in a hover or on the ground: a low, soft loop of the rumble (kept quiet: it plays on)
  crew_ship/liftoff.ogg   lifting off: the roar building and carrying away (16 s)

Levels are set by loudness (EBU R128), well under the recordings' own peaks, so nothing blares; the game falls them off
with distance (sounds.json: attenuation_distance). Usage: python3 tools/crew_ship_sounds.py <folder with the two mp3s>
"""
import json, os, subprocess, sys, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
OUT = "src/main/resources/assets/jugcraft/sounds/crew_ship"
SOUNDS_JSON = "src/main/resources/assets/jugcraft/sounds.json"
ROAR = "590189main_ringtone_131_launchNats.mp3"
RUMBLE = "590329main_ringtone_SDO_launchNats.mp3"
# the loudness each is cut to (LUFS): the roar loud enough to feel, the idle low
TARGET = {"entry": -19.0, "thrust": -19.0, "idle": -27.0, "liftoff": -19.0}


def load(path):
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "x.wav")
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", path, "-ac", "1", "-ar", str(SR), w], check=True)
        sr, x = wavfile.read(w)
    return x.astype(np.float64) / 32768.0


def lowpass(x, hz, order=4):
    return sosfilt(butter(order, hz, "low", fs=SR, output="sos"), x)


def highpass(x, hz, order=2):
    return sosfilt(butter(order, hz, "high", fs=SR, output="sos"), x)


def loudness(x):
    """An EBU R128-style integrated loudness (K-weighted, gated), in LUFS."""
    shelf = sosfilt(butter(2, 1500, "high", fs=SR, output="sos"), x) * 1.58 + x
    k = highpass(shelf, 38)
    block, hop = int(0.4 * SR), int(0.1 * SR)
    ms = np.array([np.mean(k[i:i + block] ** 2) for i in range(0, max(1, len(k) - block), hop)]) + 1e-12
    lufs = -0.691 + 10 * np.log10(ms)
    gated = ms[lufs > -70]
    rel = -0.691 + 10 * np.log10(np.mean(gated)) - 10
    gated = ms[(lufs > -70) & (lufs > rel)]
    return -0.691 + 10 * np.log10(np.mean(gated))


def to_level(x, target):
    y = x * 10 ** ((target - loudness(x)) / 20)
    peak = np.max(np.abs(y))
    return y * (0.89 / peak) if peak > 0.89 else y      # never past -1 dBFS


def fade(x, a, b):
    y = x.copy()
    na, nb = int(a * SR), int(b * SR)
    if na:
        y[:na] *= np.linspace(0, 1, na) ** 2
    if nb:
        y[-nb:] *= np.linspace(1, 0, nb) ** 1.5
    return y


def loop(x, xf=1.0):
    """A seamless loop: the end crossfaded (equal power) into the start."""
    n = int(xf * SR)
    head, body, tail = x[:n], x[n:-n], x[-n:]
    t = np.linspace(0, 1, n)
    return np.concatenate([tail * np.cos(t * np.pi / 2) + head * np.sin(t * np.pi / 2), body])


def seg(x, a, b):
    return x[int(a * SR):int(b * SR)]


def write(name, x):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "x.wav")
        wavfile.write(w, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", w, "-c:a", "libvorbis", "-q:a", "5", f"{OUT}/{name}.ogg"], check=True)
    print(f"{name}.ogg {len(x) / SR:.1f} s, {loudness(x):.1f} LUFS, peak {20 * np.log10(np.max(np.abs(x)) + 1e-9):.1f} dBFS")


def sounds_json():
    js = json.load(open(SOUNDS_JSON))
    for key, dist, sub in (("entry", 128, "comes in"), ("thrust", 96, "thrusters roar"), ("idle", 32, "thrusters idle"),
                           ("liftoff", 128, "lifts off")):
        js[f"crew_ship.{key}"] = {"subtitle": f"subtitles.jugcraft.crew_ship.{key}",
                                  "sounds": [{"name": f"jugcraft:crew_ship/{key}", "attenuation_distance": dist, "stream": True}]}
    js["crew_ship.ramp"] = {"subtitle": "subtitles.jugcraft.crew_ship.ramp",
                            "sounds": [{"name": "minecraft:tile/piston/out", "pitch": 0.55, "volume": 0.9, "attenuation_distance": 24},
                                       {"name": "minecraft:tile/piston/in", "pitch": 0.55, "volume": 0.9, "attenuation_distance": 24}]}
    with open(SOUNDS_JSON, "w") as f:
        json.dump(js, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    src = sys.argv[1] if len(sys.argv) > 1 else "."
    roar = load(os.path.join(src, ROAR))
    rumble = load(os.path.join(src, RUMBLE))
    weight = lowpass(rumble, 220) * 2.2                  # the Atlas's low rumble, for weight under the Shuttle's crackle
    # entry: the rumble first, far off, the crackle swelling over it
    n = int(13 * SR)
    r = np.resize(weight, n)
    c = np.resize(roar[: int(18.5 * SR)], n)
    swell = np.linspace(0, 1, n) ** 1.6
    entry = fade(r * (0.6 + 0.4 * swell) + lowpass(c, 2600) * swell, 2.5, 2.0)
    write("entry", to_level(entry, TARGET["entry"]))
    # the burn: the Shuttle's steady middle, the rumble under it, looped
    body = seg(roar, 3.0, 12.0)
    under = np.resize(seg(weight, 1.0, 19.0), len(body))
    write("thrust", to_level(loop(lowpass(body, 3200) + 0.8 * under, 1.0), TARGET["thrust"]))
    # idle: the rumble alone, soft, looped
    idle = lowpass(seg(rumble, 1.0, 19.0), 380, 6)
    write("idle", to_level(loop(idle, 1.5), TARGET["idle"]))
    # lift-off: the roar from its start, building, carrying away
    lift = seg(roar, 0.0, 16.0)
    lift = lowpass(lift, 3000) + 0.7 * np.resize(weight, len(lift))
    write("liftoff", to_level(fade(lift * np.linspace(0.7, 1.0, len(lift)), 0.6, 4.0), TARGET["liftoff"]))
    sounds_json()
    print("sounds.json: crew_ship.entry, thrust, idle, liftoff, ramp")

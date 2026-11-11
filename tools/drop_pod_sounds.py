"""The drop pods' and the designator's sounds: sounds/drop_pod/*.ogg, their sounds.json entries and subtitles.

Real recordings (Justin, 10 November 2026: "take sounds from like real life re entry of stuff"; "make sure to add real
rocket sounds for the boosters"; "i want the ground impacts to be off real sounds as well"):

  roar_far.ogg   a pod coming down, far off: the Atlas V's distant rumble with the Shuttle's under it (a loop)
  roar_near.ogg  coming down close: the Shuttle's roar and crackle, the Atlas under it (a loop). The client plays both
                 from the pod as it falls, the near one taking over as it closes, pitched by its speed toward you
                 (client/DropPodSounds)
  boom.ogg       its sonic boom as it starts down: a Falcon Heavy's two side boosters coming home, heard from the NASA
                 Causeway (their triple boom and the rumble of their landing burns after); played at three pitches
  retro.ogg      its retro rockets kicking in: a Falcon 9's engines lighting, from a pad camera's microphone (the
                 ignition and the roar building), cut off as it hits, the roar's tail under the cut
  impact_0..2.ogg  it hitting the ground: NASA's gantry drop of an air taxi body onto the impact pad, a heavy metal
                 thud on dirt and a heavy bag dropped on dirt (a little slowed: more weight), then the dirt and stones
                 thrown up falling back
  hatch.ogg      the hatch's explosive bolts going (three firecracker cracks, each a little different) and the hatch
                 slamming down and clattering on the ground (a heavy metal door's slam, sheet metal dropped on concrete)
  selfdestruct.ogg  a pod blowing itself apart once its soldier is clear: a demolition charge (4 kg of TNT, a land mine
                 cleared in Cambodia) and its debris raining back down, brought closer in time

  Sources (in the folder given):
    590189main_ringtone_131_launchNats.mp3, 590329main_ringtone_SDO_launchNats.mp3
        NASA historical sounds (https://www.nasa.gov/historical-sounds/): STS-131 and Atlas V (SDO) launches
    KSC-20260830-AU-RTD01-0001-Roman_Space_Telescope_Live_Launch_Coverage_CH1_Causeway_CH2_Landing_Zone_2-M26246~128k.mp3
        NASA Image and Video Library (images.nasa.gov): Roman Space Telescope launch, 30 August 2026, CH1 NASA Causeway
    KSC-20261001-AU-LMM01-0001-SpaceX_Crew_13_Live_Launch_Coverage_Pad_UCS3_Landing-M26153~128k.mp3
        NASA Image and Video Library: SpaceX Crew-13 launch, 1 October 2026, CH2 pad camera UCS-3
    LRC-2025-V-06242025-RVLT-Gantry-Drop-Test-With-Slate-V0002050~orig.mp4
        NASA Image and Video Library: Langley gantry drop test of an air taxi body, June 2025
    NASA audio and video are not copyrighted in the US (credit: NASA); NASA endorses nothing here.
    fs_640204.mp3, fs_392008.mp3, fs_569745.mp3
        Freesound, CC0: "Heavy Metal Thud on Ground" (7of9Designs), "HEAVY BAG DROP DIRT.wav" (burbujafilms),
        "Gravel Stone Dirt Debris Falling Small" (Sheyvan)
    fs_609588.mp3, fs_406197.mp3, fs_587442.mp3, fs_165808.mp3
        Freesound, CC0: "Firecracker Explosion" (unfa), "door metal big heavy close kinda slam thud echo offmic.wav"
        (kyles), "Dropping sheet metal" (SamsterBirdies), "Explosion with debris - authentic. 4kg TNT" (sidohzen)

Made here (no recordings): mark.ogg (the designator's two-tone chirp: it is electronic).

Usage: python3 tools/drop_pod_sounds.py <folder with the sources>
"""
import json, os, subprocess, sys, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
OUT = "src/main/resources/assets/jugcraft/sounds/drop_pod"
SOUNDS_JSON = "src/main/resources/assets/jugcraft/sounds.json"
LANG = "src/main/resources/assets/jugcraft/lang/en_us.json"
SRC = "."
STS, ATLAS = "590189main_ringtone_131_launchNats.mp3", "590329main_ringtone_SDO_launchNats.mp3"
ROMAN = "KSC-20260830-AU-RTD01-0001-Roman_Space_Telescope_Live_Launch_Coverage_CH1_Causeway_CH2_Landing_Zone_2-M26246~128k.mp3"
CREW13 = "KSC-20261001-AU-LMM01-0001-SpaceX_Crew_13_Live_Launch_Coverage_Pad_UCS3_Landing-M26153~128k.mp3"
GANTRY = "LRC-2025-V-06242025-RVLT-Gantry-Drop-Test-With-Slate-V0002050~orig.mp4"
THUD, BAG, DEBRIS = "fs_640204.mp3", "fs_392008.mp3", "fs_569745.mp3"
CRACK, DOOR, SHEET, TNT = "fs_609588.mp3", "fs_406197.mp3", "fs_587442.mp3", "fs_165808.mp3"
rng = np.random.default_rng(11)


# ---------------------------------------------------------------- the recordings

def decode(name, start=None, length=None, channel=None):
    """A stretch of a recording, mono (one channel of it, or both mixed), as floats."""
    args = ["ffmpeg", "-loglevel", "error", "-y"]
    if start is not None:
        args += ["-ss", str(start)]
    if length is not None:
        args += ["-t", str(length)]
    args += ["-i", os.path.join(SRC, name)]
    args += ["-af", f"pan=mono|c0=c{channel}"] if channel is not None else ["-ac", "1"]
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "x.wav")
        subprocess.run(args + ["-ar", str(SR), w], check=True)
        _, x = wavfile.read(w)
    return x.astype(np.float64) / 32768


def lowpass(x, hz, order=4):
    return sosfilt(butter(order, hz, "low", fs=SR, output="sos"), x)


def highpass(x, hz, order=2):
    return sosfilt(butter(order, hz, "high", fs=SR, output="sos"), x)


def shelf_up(x, hz, db):
    """A high shelf: what's above hz raised by db (some of the air's loss of the highs over distance given back)."""
    return x + (10 ** (db / 20) - 1) * highpass(x, hz, 2)


def peak_norm(x, p=0.9):
    return x * (p / (np.max(np.abs(x)) + 1e-12))


def loudness(x):
    """EBU R128-style integrated loudness (K-weighted, gated), LUFS."""
    k = highpass(x + 0.58 * highpass(x, 1500, 2), 38)
    block, hop = int(0.4 * SR), int(0.1 * SR)
    ms = np.array([np.mean(k[i:i + block] ** 2) for i in range(0, max(1, len(k) - block), hop)]) + 1e-12
    lufs = -0.691 + 10 * np.log10(ms)
    g = ms[lufs > -70]
    rel = -0.691 + 10 * np.log10(np.mean(g)) - 10
    g = ms[(lufs > -70) & (lufs > rel)]
    return -0.691 + 10 * np.log10(np.mean(g))


def to_lufs(x, target):
    y = x * 10 ** ((target - loudness(x)) / 20)
    pk = np.max(np.abs(y))
    return y * (0.89 / pk) if pk > 0.89 else y


def loop_xfade(x, xf=1.5):
    """A seamless loop: the end crossfaded (equal power) into the start."""
    n = int(xf * SR)
    head, body, tail = x[:n], x[n:-n], x[-n:]
    t = np.linspace(0, 1, n)
    return np.concatenate([tail * np.cos(t * np.pi / 2) + head * np.sin(t * np.pi / 2), body])


def crossfade(a, b, xf):
    n = int(xf * SR)
    t = np.linspace(0, 1, n)
    return np.concatenate([a[:-n], a[-n:] * np.cos(t * np.pi / 2) + b[:n] * np.sin(t * np.pi / 2), b[n:]])


def declip(x, thr=0.97):
    """Where the microphone ran out (flat tops at full scale), the wave carried on by a cubic through the samples
    either side: the boom's and the ignition's peaks given back, not squared off."""
    from scipy.interpolate import CubicSpline
    y = x.copy()
    bad = np.abs(x) >= thr
    i, n = 0, len(x)
    while i < n:
        if bad[i]:
            j = i
            while j < n and bad[j]:
                j += 1
            xs = [k for k in range(max(0, i - 4), min(n, j + 4)) if not bad[k]]
            if len(xs) >= 4:
                y[i:j] = CubicSpline(xs, x[xs])(np.arange(i, j))
            i = j
        else:
            i += 1
    return y


def slow(x, f):
    """Played slower by f (and so deeper): more weight behind it."""
    return np.interp(np.arange(0, len(x) - 1, f), np.arange(len(x)), x)


LOOP = 16.0


def roars():
    sts, atl = decode(STS), decode(ATLAS)
    a = atl[int(0.5 * SR):int((0.5 + LOOP + 1.5) * SR)]
    s = sts[int(0.6 * SR):int((0.6 + LOOP + 1.5) * SR)]
    far = loop_xfade(1.6 * highpass(a, 18) + 0.45 * lowpass(s, 320, 4), 1.5)
    near = loop_xfade(shelf_up(highpass(s, 22), 1800, 5) + 0.9 * highpass(a, 18), 1.5)
    return to_lufs(far, -20), to_lufs(near, -16)


def boom():
    bang = declip(decode(ROMAN, 3535.40, 3.3, channel=0))     # (58:55 into the recording)
    roll = decode(ROMAN, 3544.4, 3.6, channel=0)
    k = np.sqrt(np.mean(bang[-int(0.4 * SR):] ** 2)) / (np.sqrt(np.mean(roll[:int(0.4 * SR)] ** 2)) + 1e-12)
    bang[-int(1.2 * SR):] *= np.linspace(1, 0.45, int(1.2 * SR))
    y = highpass(crossfade(bang, roll * k * 0.45, 0.6), 18, 2)
    y[-int(2.2 * SR):] *= np.linspace(1, 0, int(2.2 * SR)) ** 2
    return peak_norm(y, 0.92)


def retro():
    x = declip(decode(CREW13, 245.55, 1.4, channel=1))         # (the ignition 0.08 s in)
    t = np.arange(len(x)) / SR
    burn = 0.08 + 0.78
    y = x * np.minimum(1, t / 0.004) * np.where(t < burn, 1.0, np.exp(-(t - burn) / 0.06))
    tail = decode(CREW13, 266.0, 1.4, channel=1)
    tail = lowpass(tail, 1800, 2) * np.exp(-np.arange(len(tail)) / SR / 0.35)
    k = np.sqrt(np.mean(x[int((burn - 0.2) * SR):int(burn * SR)] ** 2)) / (np.sqrt(np.mean(tail[:int(0.2 * SR)] ** 2)) + 1e-12)
    out = np.zeros(int(burn * SR) + len(tail))
    out[:len(y)] += y
    out[int(burn * SR):int(burn * SR) + len(tail)] += tail * k * 0.35
    return to_lufs(highpass(out, 20, 2), -12)


def impact(variant):
    crash = decode(GANTRY, 100.47, 1.6)
    thud = slow(decode(THUD), (0.82, 0.76, 0.88)[variant])
    bag = slow(decode(BAG, (12.78, 17.83, 21.18)[variant], 0.9), (0.8, 0.86, 0.74)[variant])
    debris = decode(DEBRIS)
    n = int(3.6 * SR)
    out = np.zeros(n)

    def put(x, at, g):
        i = int(round(at * SR))
        if i < 0:
            x, i = x[-i:], 0
        m = min(len(x), n - i)
        out[i:i + m] += x[:m] / (np.max(np.abs(x)) + 1e-12) * g

    def hit(x):                                                 # its first strong low-end peak
        e = np.abs(lowpass(x, 400, 2))
        return int(np.argmax(e > 0.6 * e.max())) / SR

    HIT = 0.06                                                  # each one's hit lined up here
    put(crash, HIT - hit(crash), 0.85)
    put(thud, HIT - hit(thud), 0.9)
    put(bag, HIT - hit(bag), 0.7)
    put(debris, HIT + 0.12, 0.35)
    out = highpass(out, 22, 2)
    out[-int(0.5 * SR):] *= np.linspace(1, 0, int(0.5 * SR)) ** 2
    return peak_norm(out, 0.92)


def hatch_real():
    """The bolts: the same crack three times (0, 60 and 110 ms), each a little slower or faster and quieter than the
    last; the hatch slamming down (a heavy metal door) as its first edge hits at 160 ms, and clattering (a sheet of
    metal dropped on concrete)."""
    crack = decode(CRACK)
    crack = crack[int(np.argmax(np.abs(crack))) - int(0.002 * SR):][:int(0.45 * SR)]
    door = decode(DOOR, 0.29, 1.4)
    sheet = decode(SHEET, 5.04, 2.4)
    n = int(2.2 * SR)
    out = np.zeros(n)

    def put(x, at, g):
        i = int(round(at * SR))
        m = min(len(x), n - i)
        out[i:i + m] += x[:m] / (np.max(np.abs(x)) + 1e-12) * g

    for k, (at, f, g) in enumerate(((0.0, 1.0, 1.0), (0.06, 1.07, 0.85), (0.11, 0.94, 0.75))):
        put(slow(crack, f), 0.01 + at, g)
    put(slow(door, 0.9), 0.17, 0.55)
    put(sheet, 0.2, 0.38)
    out = highpass(out, 30, 2)
    out[-int(0.4 * SR):] *= np.linspace(1, 0, int(0.4 * SR)) ** 2
    return peak_norm(out, 0.9)


def selfdestruct_real():
    """The charge going off, its roll over the land, and the debris it threw coming down (from later in the same
    recording, brought in 1.8 seconds after the blast and a little quieter)."""
    blast = decode(TNT, 2.08, 5.2)
    debris = decode(TNT, 9.0, 3.6)
    n = int(5.6 * SR)
    out = np.zeros(n)
    out[:len(blast)] += blast / np.max(np.abs(blast))
    i = int(1.8 * SR)
    m = min(len(debris), n - i)
    out[i:i + m] += debris[:m] / np.max(np.abs(blast)) * 0.85
    out = highpass(out, 22, 2)
    out[-int(1.2 * SR):] *= np.linspace(1, 0, int(1.2 * SR)) ** 2
    return peak_norm(out, 0.92)


# ---------------------------------------------------------------- made here: the designator's chirp
# (the old made-up scream, retros, impact, hatch and self-destruct are drawn still, so the chirp comes out as it was)

def t_(sec):
    return np.arange(int(SR * sec)) / SR


def band(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def low(x, hz, order=3):
    return sosfilt(butter(order, hz, "low", fs=SR, output="sos"), x)


def env(t, attack, decay):
    return np.minimum(1, t / max(attack, 1e-4)) * np.exp(-np.maximum(0, t - attack) / decay)


def norm(x, peak=0.7):
    return peak * x / (np.abs(x).max() + 1e-9)


def _scream():
    t = t_(7.0)
    f = 1700 * np.exp(-t / 3.2) + 380                       # the air's shriek, falling as it slows
    ph = 2 * np.pi * np.cumsum(f) / SR
    shriek = band(rng.normal(0, 1, len(t)), 300, 4000) * 0.25 + np.sin(ph) * 0.35 + 0.15 * np.sin(2.01 * ph)
    shriek *= np.minimum(1, t / 1.5) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * t) ** 2)
    roar = low(rng.normal(0, 1, len(t)), 500) * np.minimum(1, (t / 6.0) ** 1.6) * 2.2
    crackle = (rng.random(len(t)) > 0.9985) * rng.normal(0, 3, len(t))
    crackle = band(crackle, 600, 5000) * np.minimum(1, t / 4.0)
    x = shriek + roar + crackle
    return norm(x * np.minimum(1, (7.0 - t) / 0.08), 0.8)


def _retro():
    t = t_(1.1)
    x = low(rng.normal(0, 1, len(t)), 1400) * 1.4 + band(rng.normal(0, 1, len(t)), 1500, 6000) * 0.4
    x += 0.6 * np.sin(2 * np.pi * 55 * t) * np.exp(-t / 0.5)
    return norm(x * env(t, 0.02, 0.45), 0.85)


def _impact():
    t = t_(1.6)
    f = 70 * np.exp(-t / 0.3) + 28
    thump = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.35) * 1.5
    debris = band(rng.normal(0, 1, len(t)), 400, 5000) * env(t, 0.01, 0.3) * 0.7
    rattle = (rng.random(len(t)) > 0.996) * rng.normal(0, 1.5, len(t))
    rattle = band(rattle, 1500, 7000) * np.exp(-t / 0.6)
    return norm(thump + debris + rattle, 0.9)


def hatch():
    t = t_(0.7)
    x = np.zeros(len(t))
    for k, at in enumerate((0.0, 0.06, 0.11)):               # the bolts
        i = int(at * SR)
        n = len(t) - i
        crack = band(rng.normal(0, 1, n), 800, 8000) * np.exp(-np.arange(n) / (SR * 0.012)) * (1.2 - 0.15 * k)
        x[i:] += crack
    i = int(0.16 * SR)
    tt = np.arange(len(t) - i) / SR                         # the hatch clanging off
    clang = sum(np.sin(2 * np.pi * f_ * tt) * a for f_, a in ((523, 0.5), (1187, 0.3), (2210, 0.2))) * np.exp(-tt / 0.18)
    x[i:] += clang * 0.6
    return norm(x, 0.8)


def selfdestruct():
    t = t_(2.2)
    boom = low(rng.normal(0, 1, len(t)), 300) * env(t, 0.005, 0.5) * 2.5
    blast = band(rng.normal(0, 1, len(t)), 300, 6000) * env(t, 0.003, 0.18)
    crack = (rng.random(len(t)) > 0.997) * rng.normal(0, 1.5, len(t))
    crack = band(crack, 1200, 7000) * np.exp(-t / 0.9)
    return norm(boom + blast + crack, 0.9)


def mark():
    t = t_(0.35)
    f = np.where(t < 0.15, 1600, 2400)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * (np.minimum(1, t / 0.005)) * np.minimum(1, (0.35 - t) / 0.03)
    x *= np.where((t > 0.14) & (t < 0.17), 0.2, 1.0)
    return norm(x, 0.5)



def write(name, x, q=6):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "x.wav")
        wavfile.write(w, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", w, "-c:a", "libvorbis", "-q:a", str(q), f"{OUT}/{name}.ogg"], check=True)
    print(f"{name}.ogg {len(x) / SR:.1f} s, peak {20 * np.log10(np.max(np.abs(x)) + 1e-9):.1f} dBFS")


# sounds.json: the event, its files (name, pitch), how far it carries, whether it streams, its subtitle. The far
# roar, the near roar and the boom carry over the whole area (the client sets their loudness by distance itself).
EVENTS = {
    "roar_far": ([("roar_far", 1.0)], 2000, True, "Drop pod roars in"),
    "roar_near": ([("roar_near", 1.0)], 2000, True, "Drop pod roars overhead"),
    "boom": ([("boom", 0.92), ("boom", 1.0), ("boom", 1.08)], 2000, True, "Sonic boom"),
    "retro": ([("retro", 1.0)], 96, False, "Drop pod fires its retros"),
    "impact": ([("impact_0", 1.0), ("impact_1", 1.0), ("impact_2", 1.0)], 96, False, "Drop pod hits"),
    "hatch": ([("hatch", 1.0)], 32, False, "Drop pod hatch blows"),
    "selfdestruct": ([("selfdestruct", 1.0)], 64, False, "Drop pod blows itself apart"),
    "mark": ([("mark", 1.0)], 16, False, "Designator marks"),
}
RETIRED = ["scream"]                                            # (the old made-up shriek: its event and file go)


def main():
    global SRC
    SRC = sys.argv[1] if len(sys.argv) > 1 else "."
    far, near = roars()
    write("roar_far", far)
    write("roar_near", near)
    write("boom", boom())
    write("retro", retro())
    for v in range(3):
        write(f"impact_{v}", impact(v))
    write("hatch", hatch_real())
    write("selfdestruct", selfdestruct_real())
    _scream(), _retro(), _impact(), hatch(), selfdestruct()
    write("mark", mark(), 5)
    with open(SOUNDS_JSON) as f:
        sounds = json.load(f)
    with open(LANG, encoding="utf-8") as f:
        lang = json.load(f)
    for name in RETIRED:
        sounds.pop(f"drop_pod.{name}", None)
        lang.pop(f"subtitles.jugcraft.drop_pod.{name}", None)
        if os.path.exists(f"{OUT}/{name}.ogg"):
            os.remove(f"{OUT}/{name}.ogg")
    for key, (files, reach, stream, subtitle) in EVENTS.items():
        entry = []
        for name, pitch in files:
            e = {"name": f"jugcraft:drop_pod/{name}", "attenuation_distance": reach}
            if pitch != 1.0:
                e["pitch"] = pitch
            if stream:
                e["stream"] = True
            entry.append(e)
        sounds[f"drop_pod.{key}"] = {"subtitle": f"subtitles.jugcraft.drop_pod.{key}", "sounds": entry}
        lang[f"subtitles.jugcraft.drop_pod.{key}"] = subtitle
    with open(SOUNDS_JSON, "w") as f:
        json.dump(sounds, f, indent=2)
        f.write("\n")
    with open(LANG, "w", encoding="utf-8") as f:
        f.write(json.dumps(lang, indent=2, ensure_ascii=False) + "\n")
    print("drop pod sounds:", ", ".join(EVENTS))


if __name__ == "__main__":
    main()

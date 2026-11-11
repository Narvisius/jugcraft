"""The gate guards' plasma baton: its item and its sounds (Justin approved take 12 of the sounds on 6 November 2026).
The gate guards carry it holstered on the back of the right thigh (tower/crew/TowerCrew, the baton and holster on the
crew's props layer: tools/crew_skins.py HOLSTER; the baton itself is the item, drawn there); players can have one from the creative menu
(tower/crew/PlasmaBaton).

  write_item()     the item: its definition (items/plasma_baton.json), its two models (models/item/plasma_baton.json lit,
                   plasma_baton_off.json: round moulded parts from tools/item_kit.py) and its texture
                   (textures/item/plasma_baton.png); the plasma is drawn by the client (client/BatonPlasma)
  write_sounds(SRC) the sounds, built from two sounds out of a sound library Justin subscribes to and is licensed to use
                   (the "Energy Swords" set: Scifi_Whoosh_Short ...-24.wav and ...-15.wav, in the folder SRC; they are not
                   kept in this repository, only what is made from them):
                     ignite   -24 itself, mono, as the plasma grows (its 0.2 s swell matches the growth)
                     hum      a seamless 2.4 s loop stretched out of -24's held part (160 ms grains, each placed where its
                              waveform best matches what's there: WSOLA), its air above 2.5 kHz eased down for an idle
                     off      the hum failing: slowing, sputtering, a few arcs building, then a hard stop on a discharge
                              snap and a pop, a few dying sparks, the holster's catch
                     swing    the held part Doppler-swept as it passes, a whoosh of air
                     hit      -15 (its slow swell trimmed, its start lifted to land at full strength) with superheated
                              plasma going into flesh: a wet fibrous tearing crunch, a sputtering flash of steam, the
                              "popcorn" bursts of steam pockets rupturing the tissue, a few char crackles
                     catch    the holster's catch (drawn)
                   OGG Vorbis, mono, into sounds/crew/ and sounds.json (plasma_baton.*)
"""
from __future__ import annotations

import json
import math
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import signal
from scipy.io import wavfile


ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
IGNITE_WAV = "Scifi_Whoosh_Short_EMI-RogueWaves-EnergySwords-24.wav"
HIT_WAV = "Scifi_Whoosh_Short_EMI-RogueWaves-EnergySwords-15.wav"

# ---------------------------------------------------------------------------------------------------------------- sounds

SR = 48000
SR44 = 44100
IDLE = 10 ** (-16 / 20)                          # the hum's idle level against the ignition
T_SNAP = 0.20                                    # the power-down's snap: the plasma gone (4 ticks into the holstering)


def bp(x, lo, hi, order=2, sr=SR):
    return signal.sosfilt(signal.butter(order, [lo, min(hi, sr / 2.1)], btype="band", fs=sr, output="sos"), x)


def hp(x, lo, order=2):
    return signal.sosfilt(signal.butter(order, lo, btype="high", fs=SR, output="sos"), x)


def lp(x, hi, order=2):
    return signal.sosfilt(signal.butter(order, hi, btype="low", fs=SR, output="sos"), x)


def at(t, t0):
    return np.clip(t - t0, 0, None), t >= t0


def thump(t, t0, f0, f1, decay, k, drive=1.6, sweep=0.04):
    tz, on = at(t, t0)
    f = f1 + (f0 - f1) * np.exp(-tz / sweep)
    e = np.where(tz < 0.002, tz / 0.002, np.exp(-(tz - 0.002) / decay))
    return k * np.tanh(drive * np.sin(2 * np.pi * np.cumsum(f * on) / SR)) * e * on


def flicker(t, rng, depth, rate):
    g = lp(rng.normal(0, 1, len(t)), rate)
    g = (g - g.min()) / (g.max() - g.min() + 1e-9)
    return 1 - depth * (1 - g) ** 2


def crackle(t, dens, rng, k, lo, hi, ring=0.75):
    ticks = (rng.random(len(t)) < dens / SR) * rng.choice([-1, 1], len(t)) * rng.uniform(0.3, 1.0, len(t))
    gate = lp(np.abs(rng.normal(0, 1, len(t))), 18) ** 2
    gate /= gate.max() + 1e-9
    return k * bp(signal.lfilter([1], [1, -ring], ticks) * (0.25 + 0.75 * gate), lo, hi)


def to48(x):
    return signal.resample_poly(x, 160, 147)


def click44(t, t0, k, lo, hi, dur, seed):
    tz, on = at(t, t0)
    n = np.random.default_rng(seed).normal(0, 1, len(t))
    return k * bp(n, lo, hi, sr=SR44) * np.exp(-tz / dur) * on


class Baton:
    """The sounds, from the two library files in `src`."""

    def __init__(self, src):
        src = Path(src)
        self._ig = self._load(src / IGNITE_WAV)
        self._ig = self._ig / np.abs(self._ig).max() * 10 ** (-1 / 20)
        h = self._load(src / HIT_WAV)
        self._hit = h / np.abs(h).max()
        self._hum = None

    @staticmethod
    def _load(path):
        sr, x = wavfile.read(path)
        if x.dtype == np.int32:                  # 24-bit PCM: scipy left-justifies it in 32 bits
            x = x.astype(np.float32) / 2 ** 31
        m = x.astype(float).mean(1) if x.ndim > 1 else x.astype(float)
        return signal.resample_poly(m, SR, sr)

    def ignite(self):
        return self._ig

    def hum_raw(self, dur=2.4, seed=0, grain=0.16, a=0.42, b=0.86):
        """-24's held part stretched into a seamless loop (WSOLA), at the ignition's level."""
        if self._hum is not None and (dur, seed) == (2.4, 0):
            return self._hum
        rng = np.random.default_rng(seed)
        src = self._ig[int(a * SR):int(b * SR)]
        G = int(grain * SR); H = G // 2
        L = int(dur * SR) // H * H
        win = np.hanning(G + 1)[:G]
        out = np.zeros(L)
        picks = []
        n = L // H
        for k in range(n):
            t0 = k * H
            idx = (t0 + np.arange(G)) % L
            if k == 0:
                s = int(rng.integers(0, len(src) - G))
            else:
                have = out[idx].copy()
                w = win.copy()
                if k < n - 1:
                    w[H:] = 0; have[H:] = 0
                cand = np.arange(0, len(src) - G, 8)
                best, s = -9.0, cand[0]
                for c in cand:
                    if any(abs(c - p) < int(0.05 * SR) for p in picks[-2:]):
                        continue
                    g = src[c:c + G] * w
                    r = np.dot(g, have) / (np.linalg.norm(g) * np.linalg.norm(have) + 1e-12)
                    if r > best:
                        best, s = r, c
                for c in range(max(0, s - 8), min(len(src) - G, s + 9)):
                    g = src[c:c + G] * w
                    r = np.dot(g, have) / (np.linalg.norm(g) * np.linalg.norm(have) + 1e-12)
                    if r > best:
                        best, s = r, c
            picks.append(s)
            out[idx] += src[s:s + G] * win
        if (dur, seed) == (2.4, 0):
            self._hum = out
        return out

    def hum(self):
        x = self.hum_raw()
        F = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR)
        g = 10 ** (-4.0 / 20 * (1 / (1 + (2500.0 / np.maximum(f, 1)) ** 2)))   # a circular high shelf: still seamless
        return np.fft.irfft(F * g, len(x))

    def catch(self, seed=0):
        t = np.arange(int(0.1 * SR44)) / SR44
        return to48(click44(t, 0.0, 0.12, 300, 1500, 0.005, seed))

    def body(self, seed=0, a=0.30, b=0.86):
        rng = np.random.default_rng(100 + seed)
        src = self._ig[int(a * SR):int(b * SR)]
        s = np.tile(src, 8)
        return s[int(rng.integers(0, len(src))):]

    def swing(self, seed=0):
        rng = np.random.default_rng(10 + seed)
        dur = 0.55
        n = int(dur * SR)
        t = np.arange(n) / SR
        s = np.sin(np.pi * np.clip(t / 0.36, 0, 1))
        h = self.body(seed + 7)
        x = np.interp(np.cumsum(1 + 0.22 * s), np.arange(len(h)), h)
        x = x / np.abs(x).max() * (0.25 + 0.75 * s ** 1.5)
        x += bp(rng.normal(0, 1, n), 300, 6000) * s ** 3 * 0.18
        x *= np.clip((dur - t) / 0.08, 0, 1)
        return x * IDLE * 2.2

    def off(self, seed=0):
        rng = np.random.default_rng(30 + seed)
        dur = 0.6
        n = int(dur * SR)
        t = np.arange(n) / SR
        h = np.tile(self.hum_raw(), 2)
        k = np.clip(t / T_SNAP, 0, 1)
        x = np.interp(np.cumsum(1 - 0.5 * k ** 1.5), np.arange(len(h)), h) * IDLE * 1.15
        x *= flicker(t, rng, 0.75 * k, 35) * (1 + 0.6 * k)
        x *= np.clip((T_SNAP - t) / 0.004, 0, 1)
        dens = np.where(t < T_SNAP, 150 + 2600 * k ** 2, 0)
        x += crackle(t, dens, rng, 0.35, 700, 10000)
        tz, on = at(t, T_SNAP)
        burst = crackle(t, 30000 * np.exp(-tz / 0.012) * on, rng, 0.35, 500, 11000, 0.6)
        zap = hp(rng.normal(0, 1, n), 1800) * np.exp(-tz / 0.006) * on * 0.15
        pop = thump(t, T_SNAP, 150, 60, 0.06, 0.3, 2.2, 0.012)
        sparks = np.zeros(n)
        for ts, a in ((T_SNAP + 0.045, 0.35), (T_SNAP + 0.09, 0.22), (T_SNAP + 0.15, 0.12)):
            sparks[int(ts * SR)] = a * rng.choice([-1, 1])
        sparks = bp(signal.lfilter([1], [1, -0.7], sparks), 900, 9000) * 1.2
        catch = np.zeros(n)
        c = self.catch(9)
        i = int(0.42 * SR)
        catch[i:i + len(c)] = c[: n - i]
        return hp(x + burst + zap + pop + sparks + catch, 25)

    @staticmethod
    def _grains(n, t0, t1, rng, rate, lo, hi, glen=(0.001, 0.004), decay=None):
        out = np.zeros(n)
        tt = t0
        while tt < t1:
            L = int(rng.uniform(*glen) * SR)
            i = int(tt * SR)
            if i + L >= n:
                break
            c = np.exp(rng.uniform(np.log(lo), np.log(hi)))
            g = bp(rng.normal(0, 1, L + 256), c / 1.6, c * 1.6)[256:] * np.hanning(L)
            a = rng.uniform(0.3, 1.0) * (np.exp(-(tt - t0) / decay) if decay else 1.0)
            out[i:i + L] += g * a
            tt += rng.exponential(1.0 / rate)
        return out

    def flesh(self, n, rng):
        """Superheated plasma into flesh: the tissue's water flashing to steam, its pockets bursting the tissue apart (the
        "popcorn effect"), then drying and charring; torn and cracked wet, as foley fakes flesh."""
        t = np.arange(n) / SR
        tear = self._grains(n, 0.0, 0.12, rng, 900, 350, 3500, (0.0008, 0.003), 0.05)
        wet = bp(rng.normal(0, 1, n), 140, 650) * flicker(t, rng, 0.8, 45) * np.exp(-t / 0.05) * np.minimum(t / 0.002, 1) * 0.6
        steam = bp(rng.normal(0, 1, n), 800, 4500) * flicker(t, rng, 0.75, 28) * np.minimum(t / 0.008, 1) * np.exp(-t / 0.11) * 0.17
        pop = np.zeros(n)
        tt, k = 0.015, 0
        while tt < 0.38 and k < 16:
            i = int(tt * SR)
            L = int(0.012 * SR)
            tl = np.arange(L) / SR
            crack = hp(rng.normal(0, 1, L + 128), 900)[128:] * np.exp(-tl / 0.0012)
            puff = bp(rng.normal(0, 1, L + 256), 180, 900)[256:] * np.exp(-tl / 0.004) * 0.6
            pop[i:i + L] += (crack + puff) * rng.uniform(0.4, 1.0) * np.exp(-tt / 0.18)
            tt += rng.uniform(0.012, 0.05)
            k += 1
        char = self._grains(n, 0.15, 0.5, rng, 22, 1500, 7000, (0.0005, 0.0015)) * 0.35
        return [tear * 1.1, wet, steam, pop * 0.9, char]

    def hit(self, seed=0):
        rng = np.random.default_rng(20 + seed)
        s = self._hit[int(0.12 * SR):]
        dur = 0.70
        n = int(dur * SR)
        t = np.arange(n) / SR
        s = np.pad(s, (0, max(0, n - len(s))))[:n]
        lift = 10 ** (8 * np.clip(1 - t / 0.10, 0, 1) / 20)
        env = np.minimum(t / 0.002, 1) * np.where(t < 0.40, 1.0, np.exp(-(t - 0.40) / 0.09))
        layers = [v * 0.55 for v in self.flesh(n, rng)] + [s * lift * env]
        x = hp(sum(layers), 25)
        return x * (0.89 / np.abs(x).max())


def norm(x, peak=0.89):
    return x * (peak / np.abs(x).max())


def ogg(x, path):
    with tempfile.TemporaryDirectory() as d:
        wav = Path(d) / "x.wav"
        wavfile.write(wav, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-ac", "1", "-c:a", "libvorbis", "-q:a", "7", str(path)], check=True)


def sounds(src):
    """Each sound file's samples by name (what goes into sounds/crew/)."""
    b = Baton(src)
    out = {"plasma_baton_ignite": b.ignite(), "plasma_baton_hum": norm(b.hum()), "plasma_baton_off": norm(b.off()),
           "plasma_baton_catch": norm(b.catch(0))}
    for i in range(2):
        out[f"plasma_baton_hit{i + 1}"] = b.hit(i)
        out[f"plasma_baton_swing{i + 1}"] = norm(b.swing(i))
    return out


def write_sounds(src, assets=ASSETS):
    assets = Path(assets)
    out = assets / "sounds/crew"
    out.mkdir(parents=True, exist_ok=True)
    for name, x in sounds(src).items():
        ogg(x, out / f"{name}.ogg")
    write_sound_events(assets)


def write_sound_events(assets=ASSETS):
    assets = Path(assets)
    sj = json.loads((assets / "sounds.json").read_text())
    ev = lambda name, files, dist: {"subtitle": f"subtitles.jugcraft.{name}",
                                    "sounds": [{"name": f"jugcraft:crew/{f}", "attenuation_distance": dist} for f in files]}
    sj["plasma_baton.ignite"] = ev("plasma_baton.ignite", ["plasma_baton_ignite"], 24)
    sj["plasma_baton.hum"] = ev("plasma_baton.hum", ["plasma_baton_hum"], 12)
    sj["plasma_baton.swing"] = ev("plasma_baton.swing", ["plasma_baton_swing1", "plasma_baton_swing2"], 16)
    sj["plasma_baton.hit"] = ev("plasma_baton.hit", ["plasma_baton_hit1", "plasma_baton_hit2"], 24)
    sj["plasma_baton.off"] = ev("plasma_baton.off", ["plasma_baton_off"], 24)
    sj["plasma_baton.catch"] = ev("plasma_baton.catch", ["plasma_baton_catch"], 8)
    (assets / "sounds.json").write_text(json.dumps(sj, indent=2) + "\n")


# ------------------------------------------------------------------------------------------------------------------ item
# The baton as real 3D gear (Justin, 10 November 2026: "make the plasama baton look better"; he picked A, "Patrol", from the
# rebuild sheets): round moulded parts from tools/item_kit.py, one description that makes the item and so the guards'
# baton (the item drawn in their hand and in the holster: client/TowerCrewRenderer.BatonLayer).
#
# Its frame: x across, y up (the safety switch and the charge lamps on top), z along it from the middle of the grip (the
# fist); px, a sixteenth of a block. From the butt: a steel stud, the pommel, a lit ring, the knurled rubber grip with
# five moulded rings, the guard, the emitter housing (its lit band, the switch, three lamps), a bezel, a ceramic
# insulator of two sheds, the rod through three focusing rings, the slotted head and its end cap.
#
# The plasma is not in the model: the client draws it as glows round the rod and head, as the rifle's bolt is drawn
# (client/BatonPlasma; ROD_FROM and TIP are its ends). Three models: lit (the rod hot, the band and lamps lit, full
# bright), off (the rod out, cold: a guard's between the flick that throws the rod out and the plasma lighting) and
# stowed (the rod telescoped back into the handle, its three rings stacked on the insulator under the head: how a guard
# carries it in the holster. At its full length it is longer than a leg, and worn on the thigh its grip would swing
# through the guard's back at every stride).

TPX = 8                                  # texels to the baton's pixel
TEX = (128, 64)                          # the item's texture
GX = 4.2                                 # the grip's middle in the model, along its length (the baton's middle at 8.4)
ROD_FROM, TIP = 4.96, 11.95              # the insulator's face (where the plasma starts) and the end cap's
RINGS, HEAD, CAP = 0.26, 1.11, 0.24      # a focusing ring's length, the head's, the end cap's
STOWED_TIP = ROD_FROM + 3 * RINGS + HEAD + CAP           # the end cap's face with the rod in
GUARD = 2.42                             # the guard's front face: what rests on the holster's mouth

RUBBER = (0.075, 0.075, 0.085)
RIB = (0.115, 0.115, 0.13)
GUNM = (0.36, 0.37, 0.40)
TUNG = (0.235, 0.24, 0.265)              # the housing: tungsten grey (the NT-7's)
STEEL = (0.62, 0.63, 0.66)
DARK = (0.12, 0.12, 0.135)
CERAMIC = (0.86, 0.84, 0.78)
GRAPHITE = (0.17, 0.17, 0.19)
VIOLET = (0.72, 0.46, 1.0)
VIOLET_OFF = (0.20, 0.13, 0.30)
HOT = (0.82, 0.68, 1.0)                  # the rod inside the plasma
LAMP_OFF = (0.10, 0.09, 0.12)


def shade(rgb, k):
    return tuple(c * k for c in rgb)


def lamp_row(lamps):
    """Three small lamps in a dark window, one texel each with a texel between."""
    def f(a):
        a[..., :3] = (0.05, 0.05, 0.06)
        H, W = a.shape[:2]
        for i, c in enumerate(lamps):
            x = 2 * i if W < 7 else 1 + 2 * i
            if x < W:
                a[max(0, H // 2 - 1):H // 2 + 1, x, :3] = c
    return f


def baton(lit, stowed=False):
    """The baton's parts (item_kit.Kit): lit, or off; stowed: the rod in the handle, its rings stacked under the head."""
    import item_kit as IK
    k = IK.Kit(TPX)
    s = '1' if lit else '0'
    band = VIOLET if lit else VIOLET_OFF
    F = lambda key, fn: (key, fn)
    flat = lambda key, rgb: (key, IK.flat(rgb))
    gun_cap = flat('gunc', shade(GUNM, 0.8))
    handle = [
        (-3.55, -3.25, 0.62, F('stud', IK.turned(STEEL, ends=0.1)), flat('studc', STEEL), False, 'stud'),
        (-3.25, -2.64, 1.76, F('pommel', IK.turned(GUNM, lines=(0.5,), lip=0.22)), gun_cap, False, 'pommel'),
        (-2.64, -2.40, 1.50, flat('lit' + s, band), flat('litc' + s, shade(band, 0.8)), lit, 'pommel_ring'),
        (-2.40, 2.10, 1.30, F('grip', IK.knurl(RUBBER)), flat('gripc', RUBBER), False, 'grip'),
        (2.10, GUARD, 2.06, F('guard', IK.turned(GUNM, ends=0.12, lip=0.25)), gun_cap, False, 'guard'),
        (GUARD, 3.02, 1.62, F('housing_a', IK.turned(TUNG, ends=0.14, lines=(0.5,), speck=0.03, seed=3)), flat('tungc', TUNG), False, 'housing'),
        (3.02, 3.28, 1.84, flat('lit' + s, band), flat('litc' + s, shade(band, 0.8)), lit, 'band'),
        (3.28, 3.78, 1.62, F('housing_b', IK.turned(TUNG, ends=0.14, speck=0.03, seed=4)), flat('tungc', TUNG), False, 'housing'),
        (3.78, 4.04, 1.84, F('bezel', IK.turned(GUNM, ends=0.1, lip=0.2)), gun_cap, False, 'bezel'),
        (4.04, 4.28, 1.32, F('shed', IK.turned(CERAMIC, ends=0.10)), flat('shedc', shade(CERAMIC, 0.9)), False, 'shed'),
        (4.28, 4.46, 0.94, F('waist', IK.turned(shade(CERAMIC, 0.86), ends=0.2)), flat('waistc', shade(CERAMIC, 0.8)), False, 'waist'),
        (4.46, 4.72, 1.32, F('shed', IK.turned(CERAMIC, ends=0.10)), flat('shedc', shade(CERAMIC, 0.9)), False, 'shed'),
        (4.72, ROD_FROM, 0.94, F('waist', IK.turned(shade(CERAMIC, 0.86), ends=0.2)), flat('waistc', shade(CERAMIC, 0.8)), False, 'waist'),
    ]
    head = lambda z: [(z, z + HEAD, 1.24, F('head' + s, IK.slotted(GUNM, DARK, lit=VIOLET if lit else None)), gun_cap, False, 'head'),
                      (z + HEAD, z + HEAD + CAP, 0.86, F('cap', IK.turned(STEEL, ends=0.1)), flat('studc', STEEL), False, 'cap')]
    if stowed:
        # the three rings stacked on the insulator's face (one sleeve, scribed where they meet), the head on them
        k.lathe(handle + [(ROD_FROM, ROD_FROM + 3 * RINGS, 1.02, F('rings', IK.turned(GUNM, ends=0.2, lip=0.3, lines=(1 / 3, 2 / 3))),
                           flat('ringc', shade(GUNM, 0.78)), False, 'rings')] + head(ROD_FROM + 3 * RINGS))
    else:
        k.lathe(handle + [(ROD_FROM, TIP - CAP - HEAD, 0.62, F('rod' + s, IK.turned(HOT if lit else GRAPHITE, ends=0.0, speck=0.0 if lit else 0.04, seed=5)),
                           flat('rodc' + s, HOT if lit else GRAPHITE), lit, 'rod')] + head(TIP - CAP - HEAD))
        # the rod's focusing rings: sleeves standing proud of it
        for z in (6.2, 7.75, 9.3):
            k.prism(z, z + RINGS, 1.02, F('ring', IK.turned(GUNM, ends=0.2, lip=0.3)), (flat('ringc', shade(GUNM, 0.78)),) * 2, name='ring')
    # the grip's moulded rings: sleeves standing proud of it
    for z in (-1.95, -1.1, -0.25, 0.6, 1.45):
        k.prism(z, z + 0.36, 1.54, F('rib', IK.turned(RIB, ends=0.25, lip=0.3)), (flat('ribc', shade(RIB, 0.75)),) * 2, name='rib')
    # on the housing's top flat: the safety switch behind the band, three charge lamps ahead of it
    k.box((-0.17, 0.81, 2.56), (0.17, 0.93, 2.92), {'*': flat('switch', DARK), '+y': F('switch_top', IK.turned(DARK, bands=((0.55, 0.85, (0.75, 0.16, 0.14)),), ends=0.0))},
          only=('+y', '+x', '-x', '+z', '-z'), name='switch')
    lamps = [VIOLET] * 3 if lit else [LAMP_OFF] * 3
    k.box((-0.19, 0.81, 3.34), (0.19, 0.92, 3.72), {'+y': F('lamps' + s, lamp_row(lamps)), '*': flat('switch', DARK)},
          only=('+y', '+x', '-x', '+z', '-z'), glow=lit, name='lamps')
    return k


def display():
    """Held: in third person ahead of the fist as a sword is, the grip's middle (GX) in the fist where the guards hold
    theirs (crew_skins.BATON_AT: 0.4 px short of the arm's end, on its middle), its top up the arm; in first person gripped
    upright in the lower right, the rod up and a little in, the handle running down toward the waist (Justin, 11 November
    2026, take 1's A: "angled more toward the waist like its being held in the characters hand"); in the menus on the
    slant.

    The game puts a held item's middle 10 px down the arm and 2 px ahead of its middle (ItemInHandLayer: the arm's
    (x, y, z) is the item's (-x, -z, -y) there), so the grip's middle goes 2 px back and 0.4 px up from that."""
    R3 = rot_xyz(0, 90, 90)
    t3 = -(R3 @ np.array([GX - 8, 0.0, 0.0])) + np.array([0.0, -2.0, 0.4])
    mid = (-3.55 + TIP) / 2 + GX - 8                     # the baton's middle from the model's
    a = math.radians(45)
    gui = [round(float(-mid * math.cos(a)), 3), round(float(-mid * math.sin(a)), 3), 0]
    return {"thirdperson_righthand": {"rotation": [0, 90, 90], "translation": [round(float(v), 3) for v in t3], "scale": [1.0] * 3},
            "firstperson_righthand": {"rotation": [0, 100, 55], "translation": [-0.5, 2.5, 0.6], "scale": [0.95] * 3},
            "gui": {"rotation": [0, 0, 45], "translation": gui, "scale": [0.95] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5] * 3},
            "fixed": {"rotation": [0, 180, 45], "translation": [-gui[0], gui[1], 0], "scale": [0.95] * 3}}


def item_models():
    """The lit, the off and the stowed model, their shared texture; none of their faces lies on another (item_kit.check)."""
    import item_kit as IK
    atlas = IK.Atlas(TEX[0])
    models = {}
    for name, lit, stowed in (("plasma_baton", True, False), ("plasma_baton_off", False, False), ("plasma_baton_stowed", False, True)):
        elements = IK.elements(baton(lit, stowed).parts, atlas, TEX, GX)
        IK.check(elements)
        models[name] = {"textures": {"0": "jugcraft:item/plasma_baton", "particle": "jugcraft:item/plasma_baton"}, "elements": elements,
                        "display": display()}
    assert atlas.used <= TEX[1], "the baton's texture is full (%d rows)" % atlas.used
    tex = (np.clip(atlas.img[:TEX[1]], 0, 1) * 255).round().astype(np.uint8)
    return models, Image.fromarray(tex, "RGBA")


def definition():
    """The item: a guard's baton says which model it is (custom model data "stowed" in the holster, "off" with the rod
    thrown out, "lit": the renderer draws their plasma as it grows, client/TowerCrewRenderer); anyone else's is lit with
    its plasma drawn by the item (client/BatonPlasmaItem), flat on in the menus."""
    model = lambda name: {"type": "minecraft:model", "model": "jugcraft:item/" + name}
    plasma = lambda flat: {"type": "minecraft:composite", "models": [model("plasma_baton"), {
        "type": "minecraft:special", "base": "jugcraft:item/plasma_baton", "model": {"type": "jugcraft:baton_plasma", "flat": flat}}]}
    return {"model": {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
                      "cases": [{"when": "stowed", "model": model("plasma_baton_stowed")}, {"when": "off", "model": model("plasma_baton_off")},
                                {"when": "lit", "model": model("plasma_baton")}],
                      "fallback": {"type": "minecraft:select", "property": "minecraft:display_context",
                                   "cases": [{"when": "gui", "model": plasma(True)}], "fallback": plasma(False)}}}


def rot_xyz(ax, ay, az):
    """ItemTransform's rotation (degrees, JOML rotationXYZ: Rx * Ry * Rz)."""
    a, b, c = (math.radians(v) for v in (ax, ay, az))
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    Rz = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return Rx @ Ry @ Rz


def write_item(assets=ASSETS):
    import keep_same
    assets = Path(assets)
    models, tex = item_models()
    (assets / "items").mkdir(parents=True, exist_ok=True)
    (assets / "items/plasma_baton.json").write_text(json.dumps(definition(), indent=2) + "\n")
    for name, m in models.items():
        (assets / ("models/item/" + name + ".json")).write_text(json.dumps(m, indent=1) + "\n")
    keep_same.png(tex, assets / "textures/item/plasma_baton.png", optimize=True)


if __name__ == "__main__":
    write_item()
    if len(sys.argv) > 1:
        write_sounds(sys.argv[1])
        print("plasma baton item and sounds written")
    else:
        write_sound_events()
        print("plasma baton item written (sounds kept: give the folder with the Energy Swords files to rebuild them)")

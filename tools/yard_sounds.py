"""The transformer yard's and the switchyard's sounds (transformer yard and switchyard build, nuclear/YardBlockEntity.java),
synthesised here (no recorded or third-party audio):

- breaker: a 345 kV SF6 dead-tank breaker operating: the mechanism's spring releasing (a sharp metallic crack and its
  ring in the steel), the puffer's gas blast and the poles' clunk; loud, as the real thing is (Justin, 18 October: loud
  bangs for events are fine);
- arc: a disconnect opened under load: the arc's tearing crackle and roar as it stretches, a bang when it breaks;
- hum: a transformer's steady hum at twice the line frequency (120 Hz) and its harmonics, quiet, two seconds, played
  again and again close to a working transformer;
- gong: a deluge valve's water-motor alarm gong: its clapper beating the bell as the water turns the motor.

Writes assets/jugcraft/sounds/yard/*.ogg (needs numpy and ffmpeg with libvorbis); the sounds.json entries are SOUNDS,
written with every other sound by tools/generate_material_data.py.
"""
import os
import subprocess

import numpy as np

RATE = 44100
HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "src/main/resources/assets/jugcraft/sounds/yard")
NAMES = {"breaker": ("A breaker operates", 64), "arc": ("An electric arc", 96), "hum": ("A transformer hums", 18),
         "gong": ("A deluge alarm gong rings", 40)}
SOUNDS = {f"yard.{n}": {"subtitle": f"subtitles.jugcraft.yard.{n}", "sounds": [{"name": f"jugcraft:yard/{n}", "attenuation_distance": d}]}
          for n, (_, d) in NAMES.items()}
LANG = {f"subtitles.jugcraft.yard.{n}": t for n, (t, _) in NAMES.items()}


def _t(dur):
    return np.arange(int(RATE * dur)) / RATE


def _noise(n, seed):
    return np.random.default_rng(seed).normal(0, 1, n)


def _lowpass(x, a):
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += a * (v - acc)
        y[i] = acc
    return y


def breaker():
    t = _t(1.6)
    n = len(t)
    out = np.zeros(n)
    # the spring's crack: a click of noise with a fast decay, then the steel ringing (inharmonic partials)
    crack = _noise(n, 1) * np.exp(-t * 90)
    ring = sum(np.sin(2 * np.pi * f * t) * a for f, a in ((310, 1.0), (743, 0.6), (1270, 0.35), (2210, 0.2))) * np.exp(-t * 7)
    out += crack * 1.0 + ring * 0.35
    # the puffer's blast: a short burst of low noise 30 ms later
    d = int(0.03 * RATE)
    blast = _lowpass(_noise(n, 2), 0.08) * np.exp(-t * 14) * 4.0
    out[d:] += blast[:n - d]
    # the poles' clunk and the linkage settling
    for k, (at, f) in enumerate(((0.06, 95), (0.11, 120), (0.16, 80))):
        s = int(at * RATE)
        tt = t[:n - s]
        out[s:] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * 30) * (0.9 - 0.2 * k)
    return out


def arc():
    t = _t(1.8)
    n = len(t)
    rng = np.random.default_rng(7)
    # crackle: sparse sharp impulses, denser as the arc draws out
    crackle = np.zeros(n)
    for i in range(1400):
        at = int(rng.beta(1.2, 2.0) * (n - 400))
        crackle[at:at + 60] += rng.normal(0, 1, 60) * np.exp(-np.arange(60) / 9) * rng.uniform(0.3, 1.0)
    # the roar: low noise buzzing at 120 Hz (the arc re-strikes each half cycle)
    roar = _lowpass(_noise(n, 8), 0.05) * (0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 120 * t))) * 3.0
    env = np.minimum(1, t / 0.05) * np.where(t < 1.3, 1.0, np.exp(-(t - 1.3) * 12))
    out = (crackle * 0.8 + roar) * env
    # the bang as it breaks
    s = int(1.3 * RATE)
    tt = t[:n - s]
    out[s:] += (_noise(n - s, 9) * np.exp(-tt * 25) * 2.5 + np.sin(2 * np.pi * 60 * tt) * np.exp(-tt * 9) * 2.0)
    return out


def hum():
    t = _t(2.0)
    # 120 Hz and its even harmonics (magnetostriction), a little 360 Hz; whole cycles so it loops without a click
    out = sum(np.sin(2 * np.pi * f * t + p) * a for f, a, p in ((120, 1.0, 0), (240, 0.55, 0.4), (360, 0.3, 1.1), (480, 0.18, 2.0),
                                                              (720, 0.08, 0.3)))
    out *= 1 + 0.03 * np.sin(2 * np.pi * 0.5 * t)
    return out * np.minimum(1, np.minimum(t, 2.0 - t) / 0.08)


def gong():
    t = _t(1.0)
    n = len(t)
    out = np.zeros(n)
    # the clapper beats about eight times a second; each strike rings the bell (partials of a bell)
    for k in range(8):
        s = int(k * 0.125 * RATE)
        tt = t[:n - s]
        strike = sum(np.sin(2 * np.pi * f * tt) * a for f, a in ((880, 1.0), (1760 * 1.19, 0.5), (2637, 0.3), (523, 0.4)))
        out[s:] += strike * np.exp(-tt * 6) * 0.6 + _noise(n - s, 20 + k) * np.exp(-tt * 300) * 0.4
    return out


def write(name, signal):
    os.makedirs(OUT, exist_ok=True)
    signal = np.tanh(signal / np.max(np.abs(signal)) * 1.2) / np.tanh(1.2) * 0.8
    pcm = (signal * 32767).astype("<i2").tobytes()
    path = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", path], input=pcm, check=True)
    print("wrote", path)


if __name__ == "__main__":
    for name, make in (("breaker", breaker), ("arc", arc), ("hum", hum), ("gong", gong)):
        write(name, make())

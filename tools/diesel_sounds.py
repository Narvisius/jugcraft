"""The emergency diesel generator's sounds, synthesised here (no recorded or third-party audio).

- edg_start: the air start: the start valve's clunk, compressed air hissing into the air start motors, their whine,
  the engine cranking over on compression, then the first cylinders firing and catching.
- edg_run: a seamless 2-second loop of a big, slow medium-speed V16 at its rated 480 rpm (four-stroke: 64 firings a
  second), deep (Justin's round 13: the first version, at 900 rpm with turbo whine and a sharp knock, sounded like a
  high whine): each firing's low exhaust thump through the stack, the cylinders a little uneven with the beat of each
  revolution and each cycle, a low intake roar, the generator's hum; nothing much above a kilohertz. The client plays
  it while the engine turns, its pitch and loudness following the engine's speed (so it also runs down when stopped):
  client/DieselSounds.java.
- edg_stop: the fuel racks pulled to stop: the shutdown solenoid's clunk and a sigh of exhaust.
Writes assets/jugcraft/sounds/diesel/*.ogg and their sounds.json entries (needs numpy and ffmpeg with libvorbis).
"""
import json
import os
import subprocess
import numpy as np

RATE = 44100
HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "src/main/resources/assets/jugcraft/sounds/diesel")
SOUNDS_JSON = os.path.join(HERE, "..", "src/main/resources/assets/jugcraft/sounds.json")
LANG = {"subtitles.jugcraft.diesel.start": "Diesel generator starts",
        "subtitles.jugcraft.diesel.run": "Diesel generator runs",
        "subtitles.jugcraft.diesel.stop": "Diesel generator stops"}


def band_noise(n, low, high, rng, periodic=False):
    """Noise between low and high Hz (shaped in the frequency domain; periodic over n samples if asked)."""
    m = n if periodic else n * 2
    spectrum = rng.normal(size=m // 2 + 1) + 1j * rng.normal(size=m // 2 + 1)
    f = np.fft.rfftfreq(m, 1 / RATE)
    shape = ((f >= low) & (f <= high)).astype(float)
    # Soft edges.
    shape *= np.clip((f - low * 0.7) / (low * 0.3 + 1e-9), 0, 1) * np.clip((high * 1.3 - f) / (high * 0.3), 0, 1)
    out = np.fft.irfft(spectrum * shape, n=m)[:n]
    return out / (np.max(np.abs(out)) + 1e-12)


def ring(t, freq, decay):
    """A damped resonance (a pipe or the block ringing)."""
    return np.sin(2 * np.pi * freq * t) * np.exp(-t / decay)


def firing_response(rng):
    """One cylinder firing as heard: a deep exhaust thump (the stack's low resonances) and a soft knock."""
    n = int(RATE * 0.12)
    t = np.arange(n) / RATE
    bark = 1.0 * ring(t, 46, 0.045) + 0.8 * ring(t, 92, 0.03) + 0.45 * ring(t, 150, 0.016) + 0.2 * ring(t, 240, 0.008)
    bark *= np.minimum(1, t / 0.003)
    thud = band_noise(n, 60, 400, rng) * np.exp(-t / 0.012) * 0.45
    knock = band_noise(n, 600, 1500, rng) * np.exp(-t / 0.0015) * 0.06
    return bark + thud + knock


def low_pass_periodic(signal, cutoff, low=25.0):
    """Keeps a loop between low and cutoff Hz (a gentle roll-off above), in the frequency domain so it still loops."""
    n = len(signal)
    spectrum = np.fft.rfft(signal)
    f = np.fft.rfftfreq(n, 1 / RATE)
    spectrum *= 1 / np.sqrt(1 + (f / cutoff) ** 4)
    spectrum[f < low] = 0
    return np.fft.irfft(spectrum, n=n)


def periodic_convolve(events, response):
    """Circular convolution: the loop's last firings ring on into its start, so it loops without a click."""
    n = len(events)
    h = np.zeros(n)
    h[:len(response)] = response
    return np.fft.irfft(np.fft.rfft(events) * np.fft.rfft(h), n=n)


def run_loop():
    rng = np.random.default_rng(7)
    seconds = 2.0
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    firing = 64  # 16 cylinders, four-stroke, 480 rpm: 8 firings a revolution, 8 revolutions a second
    events = np.zeros(n)
    # Each cylinder a little different, and every firing varies a touch (the pattern repeats each cycle of 16).
    cylinders = 1 + 0.22 * rng.uniform(-1, 1, 16)
    for i in range(int(seconds * firing)):
        at = int(round(i * RATE / firing + rng.normal(0, 6))) % n
        events[at] += cylinders[i % 16] * (1 + rng.normal(0, 0.06))
    engine = periodic_convolve(events, firing_response(rng))
    engine /= np.max(np.abs(engine))
    # The crank's once-a-revolution and once-a-cycle beat in the loudness (8 and 4 Hz, whole cycles in the loop).
    engine *= 1 + 0.16 * np.sin(2 * np.pi * 8 * t) + 0.1 * np.sin(2 * np.pi * 4 * t + 1.1)
    intake = band_noise(n, 50, 450, rng, periodic=True) * 0.2
    hum = 0.04 * np.sin(2 * np.pi * 120 * t) + 0.012 * np.sin(2 * np.pi * 240 * t)
    return low_pass_periodic(engine + intake + hum, 700)


def high_pass(signal, cutoff=28.0):
    """A first-order high-pass (for the one-shots)."""
    rc = 1 / (2 * np.pi * cutoff)
    k = rc / (rc + 1 / RATE)
    out = np.zeros_like(signal)
    for i in range(1, len(signal)):
        out[i] = k * (out[i - 1] + signal[i] - signal[i - 1])
    return out


def start():
    rng = np.random.default_rng(11)
    seconds = 2.8
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    # The air start valve opening: a clunk.
    clunk = ring(t, 72, 0.05) * 0.8 + band_noise(n, 800, 4000, rng) * np.exp(-t / 0.006) * 0.5
    out += clunk
    # Air hissing into the start motors.
    hiss_env = np.clip((t - 0.06) / 0.05, 0, 1) * np.exp(-np.clip(t - 0.1, 0, None) / 1.4) * (t < 1.55)
    hiss_env *= np.clip((1.55 - t) / 0.12, 0, 1)
    out += band_noise(n, 900, 5000, rng) * hiss_env * 0.22
    # The start motors' growl running up.
    whine_f = 140 + 160 * np.clip(t / 1.2, 0, 1)
    phase = 2 * np.pi * np.cumsum(whine_f) / RATE
    out += (np.sin(phase) * 0.1 + np.sin(2 * phase) * 0.04) * hiss_env
    # Cranking: compression strokes thumping, faster as it turns over (about 100 to 170 rpm).
    crank_rate = 9 + 7 * np.clip(t / 1.4, 0, 1)
    crank_phase = np.cumsum(crank_rate) / RATE
    events = np.zeros(n)
    last = -1
    for i in range(n):
        k = int(crank_phase[i])
        if k != last and 0.12 < t[i] < 1.5:
            events[i] = 0.55 + 0.1 * rng.uniform(-1, 1)
            last = k
    chuff_t = np.arange(int(RATE * 0.08)) / RATE
    chuff = ring(chuff_t, 44, 0.04) + band_noise(len(chuff_t), 80, 600, rng) * np.exp(-chuff_t / 0.015) * 0.4
    out += np.convolve(events, chuff)[:n]
    # It fires: a few ragged first firings, then catching and running up (the running loop fades in under it).
    events = np.zeros(n)
    when = 1.42
    rate = 18.0
    while when < seconds - 0.05:
        i = int(when * RATE)
        strength = min(1.0, 0.5 + (when - 1.42) * 0.9) * (1 + 0.25 * rng.uniform(-1, 1))
        events[i] += strength
        rate = min(40.0, rate * 1.06)
        when += 1 / rate * (1 + 0.15 * rng.uniform(-1, 1))
    out += np.convolve(events, firing_response(rng))[:n] * 0.9
    # Fade the tail (the loop carries on).
    out *= np.clip((seconds - t) / 0.5, 0, 1)
    return out


def stop():
    rng = np.random.default_rng(5)
    seconds = 1.4
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    # The shutdown solenoid pulling the fuel racks: a hard clunk, then a sigh out of the exhaust.
    out = ring(t, 70, 0.05) * 0.9 + band_noise(n, 600, 2500, rng) * np.exp(-t / 0.004) * 0.4
    sigh = band_noise(n, 80, 700, rng) * np.clip((t - 0.05) / 0.1, 0, 1) * np.exp(-np.clip(t - 0.15, 0, None) / 0.35)
    return out + sigh * 0.35


def write(name, signal):
    os.makedirs(OUT, exist_ok=True)
    signal = np.tanh(signal / np.max(np.abs(signal)) * 1.2) / np.tanh(1.2) * 0.85
    pcm = (signal * 32767).astype("<i2").tobytes()
    path = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "5", path], input=pcm, check=True)
    print("wrote", path)


def write_sounds_json():
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        data = json.load(f)
    data["diesel.start"] = {"subtitle": "subtitles.jugcraft.diesel.start",
                            "sounds": [{"name": "jugcraft:diesel/edg_start", "attenuation_distance": 40}]}
    data["diesel.run"] = {"subtitle": "subtitles.jugcraft.diesel.run",
                          "sounds": [{"name": "jugcraft:diesel/edg_run", "attenuation_distance": 40}]}
    data["diesel.stop"] = {"subtitle": "subtitles.jugcraft.diesel.stop",
                           "sounds": [{"name": "jugcraft:diesel/edg_stop", "attenuation_distance": 32}]}
    with open(SOUNDS_JSON, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2) + "\n")


if __name__ == "__main__":
    write("edg_run", run_loop())
    write("edg_start", high_pass(start()))
    write("edg_stop", high_pass(stop()))
    write_sounds_json()

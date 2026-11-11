"""The drop ship's door guns' drive motor (Justin, 9 November 2026: "make sure there is animation for rotating barrels
on the minigun one. Make sure sounds and animations are accurately lined up"): crew_ship/door_gun_motor.ogg, a seamless
two-second loop of an electric rotary-gun drive at full speed, made here (no recording): the motor's whine (its
fundamental and the harmonics of its poles), the gear train's higher whine, and the rattle of the bolts in the rotor,
six to a turn at the barrels' full speed (a turn per six shots, CrewShip.SPIN_STEP), under a little air. The game plays
it while the barrels turn, its pitch and loudness with their speed (client/CrewShipSounds), so it winds up as they spin
up and down as they stop. Every part repeats a whole number of times in the two seconds, so the loop has no seam.
Usage: python3 tools/crew_ship_motor.py"""
import json, os, subprocess, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
LEN = 2.0
OUT = "src/main/resources/assets/jugcraft/sounds/crew_ship"
SOUNDS_JSON = "src/main/resources/assets/jugcraft/sounds.json"
SHOTS_A_SECOND = 20 / 3                                   # CrewShip: a door gun's shot every GUN_COOLDOWN + 1 = 3 ticks


def tone(f, amp, t, wobble=0.0, seed=0):
    """A partial at f (rounded to repeat whole in the loop), a slow shimmer in its loudness."""
    f = round(f * LEN) / LEN
    rng = np.random.default_rng(seed)
    w = 1 + wobble * np.sin(2 * np.pi * (round(rng.uniform(1, 3) * LEN) / LEN) * t + rng.uniform(0, 6.28))
    return amp * w * np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28))


def looped_noise(n, seed, lo, hi):
    """Noise band-passed lo-hi, made to loop (its two ends cross-faded over a tenth of a second)."""
    rng = np.random.default_rng(seed)
    x = rng.normal(0, 1, n + SR // 10)
    x = sosfilt(butter(2, [lo, hi], "band", fs=SR, output="sos"), x)
    k = SR // 10
    head, tail = x[:k], x[n:n + k]
    a = np.linspace(0, 1, k)
    x = x[k:n + k].copy()
    x[-k:] = x[-k:] * (1 - a) + head * a                    # the end eases into what the start was before it
    return x / (np.abs(x).max() + 1e-9)


def main():
    n = int(SR * LEN)
    t = np.arange(n) / SR
    motor = 196.0                                          # the motor's fundamental at full speed
    x = tone(motor, 0.42, t, 0.04, 1) + tone(2 * motor, 0.3, t, 0.05, 2) + tone(3 * motor, 0.16, t, 0.06, 3) \
        + tone(6 * motor, 0.12, t, 0.08, 4) + tone(8 * motor, 0.05, t, 0.1, 5)
    gear = 1470.0                                          # the gear train's mesh
    x += tone(gear, 0.10, t, 0.12, 6) + tone(2 * gear, 0.035, t, 0.15, 7)
    # the bolts' rattle: a click of filtered noise each time a barrel comes round (a shot's worth of turn)
    clicks = np.zeros(n)
    per = int(round(SR / SHOTS_A_SECOND))
    reps = n // per
    per = n // reps                                        # (a whole number of them in the loop)
    env = np.exp(-np.arange(per) / (SR * 0.012))
    rattle = looped_noise(n, 8, 900, 4200)
    for k in range(reps):
        clicks[k * per:(k + 1) * per] = env
    x += 0.28 * clicks * rattle
    x += 0.05 * looped_noise(n, 9, 300, 2500)              # air through the rotor
    # a slow lope in the motor's load, twice in the loop
    x *= 1 + 0.06 * np.sin(2 * np.pi * (2 / LEN) * t)
    x = 0.5 * x / np.abs(x).max()
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "m.wav")
        wavfile.write(w, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", w, "-c:a", "libvorbis", "-q:a", "5", f"{OUT}/door_gun_motor.ogg"], check=True)
    with open(SOUNDS_JSON) as f:
        sounds = json.load(f)
    sounds["crew_ship.door_gun_motor"] = {"subtitle": "subtitles.jugcraft.crew_ship.door_gun_motor",
                                          "sounds": [{"name": "jugcraft:crew_ship/door_gun_motor", "attenuation_distance": 32}]}
    with open(SOUNDS_JSON, "w") as f:
        json.dump(sounds, f, indent=2)
        f.write("\n")
    print("door_gun_motor.ogg", round(LEN, 2), "s;", reps, "rattles a loop")


if __name__ == "__main__":
    main()

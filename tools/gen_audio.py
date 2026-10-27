"""Generate the game's original, royalty-free audio.

Everything here is synthesized from sine waves and deterministic noise, so the
repository needs no third-party samples. Output is 22.05 kHz mono 16-bit WAV,
which keeps the APK small while sounding clean on phone speakers.

Run from the repository root:
    python tools/gen_audio.py
"""

from __future__ import annotations

import math
import os
import random
import struct
import wave

SR = 22050
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")


def write_wav(name: str, samples: list[float], peak: float = 0.55) -> None:
    """Normalize, apply a 2 ms de-click fade and write a mono 16-bit WAV."""
    top = max((abs(v) for v in samples), default=0.0)
    gain = (peak / top) if top > 0 else 0.0
    n = len(samples)
    fade = min(int(SR * 0.002), n // 2)
    data = bytearray()
    for i, value in enumerate(samples):
        v = value * gain
        if i < fade:
            v *= i / fade
        if i >= n - fade:
            v *= (n - 1 - i) / fade
        data += struct.pack("<h", int(max(-1.0, min(1.0, v)) * 32767))
    with wave.open(os.path.join(RAW, name), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(SR)
        out.writeframes(bytes(data))
    print(f"{name}: {n / SR:.2f}s peak={peak:.2f}")


def sine(freq: float, t: float, phase: float = 0.0) -> float:
    return math.sin(2.0 * math.pi * freq * t + phase)


def swept_sine(f0: float, f1: float, t: float, duration: float) -> float:
    """Constant-amplitude frequency sweep, phase-continuous."""
    k = (f1 - f0) / max(duration, 1e-6)
    return math.sin(2.0 * math.pi * (f0 * t + 0.5 * k * t * t))


def bell(buf: list[float], freq: float, start: int, duration: float, amp: float) -> None:
    """A soft struck tone. Tails wrap so a loop stays seamless."""
    n = len(buf)
    length = min(int(duration * SR), n)
    for k in range(length):
        t = k / SR
        env = math.exp(-t * 3.4) * min(1.0, t / 0.006)
        value = (
            sine(freq, t)
            + 0.28 * sine(freq * 2.0, t, 0.4)
            + 0.08 * sine(freq * 3.0, t, 1.1)
        )
        buf[(start + k) % n] += amp * env * value


def music(duration: float = 12.0) -> list[float]:
    n = int(SR * duration)
    buf = [0.0] * n
    # Am - F - C - G, one bar each. Frequencies are snapped to the loop period
    # so every oscillator is phase-continuous at the seam.
    chords = [
        (220.00, 261.63, 329.63),
        (174.61, 220.00, 261.63),
        (261.63, 329.63, 392.00),
        (196.00, 246.94, 293.66),
    ]
    roots = [55.00, 43.65, 65.41, 49.00]

    def loop_freq(f: float) -> float:
        return round(f * duration) / duration

    for ci, chord in enumerate(chords):
        for ni, f in enumerate(chord):
            fq = loop_freq(f)
            for harmonic, amp in ((1, 0.052), (2, 0.014), (3, 0.005)):
                freq = fq * harmonic
                phase = (ci * 1.7 + ni * 2.3 + harmonic * 0.9) % (2 * math.pi)
                for i in range(n):
                    t = i / SR
                    weight = 0.5 + 0.5 * math.cos(2.0 * math.pi * (4.0 * t / duration - ci))
                    tremolo = 1.0 + 0.07 * math.sin(2.0 * math.pi * 3.0 * t / duration + ci)
                    buf[i] += amp * weight * tremolo * sine(freq, t, phase)

        # Quiet sub root anchors each bar.
        fq = loop_freq(roots[ci])
        for i in range(n):
            t = i / SR
            weight = 0.5 + 0.5 * math.cos(2.0 * math.pi * (4.0 * t / duration - ci))
            buf[i] += 0.022 * weight * sine(fq, t, 0.2)

    # Gentle pentatonic sparkle, one note every 0.75 s.
    scale = [440.00, 523.25, 587.33, 659.25, 783.99, 880.00]
    pattern = [0, 2, 4, 3, 1, 3, 5, 2, 4, 1, 3, 0, 2, 5, 1, 3]
    for i, note in enumerate(pattern):
        bell(buf, scale[note], int(i * 0.75 * SR), 1.6, 0.026)

    return buf


def click() -> list[float]:
    n = int(0.045 * SR)
    rng = random.Random(1)
    out = []
    for i in range(n):
        t = i / SR
        env = math.exp(-t * 150.0)
        out.append(env * (0.55 * swept_sine(1500, 900, t, 0.045) + 0.08 * rng.uniform(-1, 1)))
    return out


def tap() -> list[float]:
    n = int(0.075 * SR)
    rng = random.Random(2)
    out = []
    for i in range(n):
        t = i / SR
        env = math.exp(-t * 55.0)
        out.append(env * (0.5 * swept_sine(760, 420, t, 0.075) + 0.06 * rng.uniform(-1, 1)))
    return out


def exit_whoosh() -> list[float]:
    n = int(0.28 * SR)
    rng = random.Random(3)
    out = []
    low = 0.0
    for i in range(n):
        t = i / SR
        env = math.sin(math.pi * min(1.0, t / 0.28)) ** 1.4
        cutoff = 0.035 + 0.22 * math.exp(-((t - 0.12) ** 2) / (2 * 0.065 ** 2))
        low += cutoff * (rng.uniform(-1, 1) - low)
        tone = 0.22 * swept_sine(620, 170, t, 0.28)
        out.append(env * (low * 1.4 + tone))
    return out


def invalid() -> list[float]:
    n = int(0.22 * SR)
    out = []
    for i in range(n):
        t = i / SR
        env = math.exp(-t * 13.0)
        out.append(env * (0.34 * sine(152, t) + 0.24 * sine(228, t, 0.7) + 0.10 * sine(76, t)))
    return out


def heart() -> list[float]:
    n = int(0.46 * SR)
    out = [0.0] * n
    for start, amp in ((0.0, 0.5), (0.17, 0.34)):
        begin = int(start * SR)
        length = min(int(0.16 * SR), n - begin)
        for k in range(length):
            t = k / SR
            env = math.exp(-t * 22.0) * min(1.0, t / 0.004)
            out[begin + k] += amp * env * (sine(72, t) + 0.35 * sine(144, t))
    return out


def hint() -> list[float]:
    n = int(0.40 * SR)
    out = [0.0] * n
    for start, freq, amp in ((0.0, 880.0, 0.34), (0.09, 1318.5, 0.30)):
        begin = int(start * SR)
        length = min(int(0.30 * SR), n - begin)
        for k in range(length):
            t = k / SR
            env = math.exp(-t * 7.5) * min(1.0, t / 0.006)
            out[begin + k] += amp * env * (
                sine(freq, t) + 0.22 * sine(freq * 2.0, t, 0.5)
            )
    return out


def undo() -> list[float]:
    n = int(0.18 * SR)
    out = []
    for i in range(n):
        t = i / SR
        env = math.exp(-t * 17.0)
        out.append(env * (0.42 * swept_sine(420, 900, t, 0.18) + 0.10 * sine(210, t)))
    return out


def win() -> list[float]:
    n = int(0.86 * SR)
    out = [0.0] * n
    notes = [(0.00, 523.25, 0.30), (0.11, 659.25, 0.28), (0.22, 783.99, 0.28), (0.33, 1046.50, 0.32)]
    for start, freq, amp in notes:
        bell(out, freq, int(start * SR), 0.60, amp)
    # Airy final chord.
    for freq in (523.25, 659.25, 783.99):
        begin = int(0.33 * SR)
        for k in range(n - begin):
            t = k / SR
            env = math.exp(-t * 2.4) * min(1.0, t / 0.01)
            out[begin + k] += 0.055 * env * sine(freq, t, 0.3)
    return out


def main() -> None:
    os.makedirs(RAW, exist_ok=True)
    write_wav("music_loop.wav", music(12.0), peak=0.30)
    write_wav("sfx_click.wav", click(), peak=0.42)
    write_wav("sfx_tap.wav", tap(), peak=0.52)
    write_wav("sfx_exit.wav", exit_whoosh(), peak=0.48)
    write_wav("sfx_invalid.wav", invalid(), peak=0.55)
    write_wav("sfx_heart.wav", heart(), peak=0.50)
    write_wav("sfx_hint.wav", hint(), peak=0.48)
    write_wav("sfx_undo.wav", undo(), peak=0.48)
    write_wav("sfx_win.wav", win(), peak=0.52)


if __name__ == "__main__":
    main()

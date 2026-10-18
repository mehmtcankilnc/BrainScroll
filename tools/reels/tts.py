#!/usr/bin/env python3
"""Voice lines -> wav files, with kokoro-onnx (local model, no network).
usage: tts.py voice.json outdir   (writes outdir/<id>.wav and outdir/durations.json)
voice.json: {"voice": "af_heart", "speed": 1.08, "lines": [{"id": "intro", "text": "..."}, ...]}
"""
import json
import os
import sys

import numpy as np
import soundfile as sf
from kokoro_onnx import Kokoro

TTS_DIR = os.environ.get("KOKORO_DIR", os.path.join(os.path.dirname(os.path.abspath(__file__)), "models"))

spec = json.load(open(sys.argv[1]))
out = sys.argv[2]
kokoro = Kokoro(f"{TTS_DIR}/kokoro-v1.0.onnx", f"{TTS_DIR}/voices-v1.0.bin")
durations = {}
for line in spec["lines"]:
    samples, sr = kokoro.create(
        line["text"], voice=spec.get("voice", "af_heart"), speed=spec.get("speed", 1.08), lang="en-us"
    )
    samples = np.asarray(samples, dtype=np.float32)
    # trim silence at both ends, keep 40 ms
    loud = np.where(np.abs(samples) > 0.01)[0]
    if len(loud):
        pad = int(0.04 * sr)
        samples = samples[max(0, loud[0] - pad): loud[-1] + pad]
    sf.write(f"{out}/{line['id']}.wav", samples, sr)
    durations[line["id"]] = round(len(samples) / sr, 3)
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    print(f"{line['id']}: {durations[line['id']]} s, peak {peak:.2f}, '{line['text']}'")
json.dump(durations, open(f"{out}/durations.json", "w"), indent=1)

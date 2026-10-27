#!/usr/bin/env python3
"""Generate video-like levels 5 and 6 for Tap Arrows (original layouts).

The reference video's early levels are: L1 3, L2 6, L3 8, L4 10,
L5 ~36 on a large board, L6 a heart-shaped board with ~40 arrows.
Tap Arrows already matched L1-L4 exactly (3, 6, 8, 10, original
layouts), but its L5 was 12 and its L6 was a 17-arrow rectangle,
with the heart at level 101 instead. This script rebuilds ONLY
levels 5 and 6 to video size, with random original layouts — it does
NOT copy the video's level layouts:

    L5: 13x12 rectangle, 36 arrows, EASY
    L6: 13x15 heart mask (125 cells), ~40 arrows, MEDIUM, shapeName Heart

Method: the same reverse construction as tools/gen_levels.js
(pieces placed last-removed-first, each accepted only if it can
already slide out against the pieces placed so far, so reversing the
placement order is a guaranteed-valid removal order). Prints a JSON
array [L5, L6] to stdout; it does NOT rewrite levels.json itself.
Levels 7-101 are left unchanged, so after the heart the normal EASY
boards resume — the heart is a special early challenge, as in the
video, not a permanent difficulty step.
"""
import json
import random
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
from gen_early_levels import can_exit, greedy_solve, try_piece

DIRS = [(-1, 0), (0, 1), (1, 0), (0, -1)]

# Heart mask, 13 rows x 15 cols, left-right symmetric ('#' = playable).
HEART_ROWS = [
    [(2, 4), (10, 12)],
    [(1, 5), (9, 13)],
    [(0, 14)],
    [(0, 14)],
    [(0, 14)],
    [(0, 14)],
    [(1, 13)],
    [(2, 12)],
    [(3, 11)],
    [(4, 10)],
    [(5, 9)],
    [(6, 8)],
    [(7, 7)],
]
H_ROWS = len(HEART_ROWS)
H_COLS = 15
H_SHAPE = [(r, c) for r, ivs in enumerate(HEART_ROWS)
           for (a, b) in ivs for c in range(a, b + 1)]
H_SET = set(H_SHAPE)


def try_piece_mask(occupied, rng):
    for _ in range(300):
        # Short pieces: 40 arrows must fit in 125 mask cells, so the
        # average piece can only be ~3 cells long. Longer pieces make
        # 40 pieces arithmetically impossible (40 x 3.4 > 125).
        want = rng.choice([2, 2, 3, 3, 3, 4])
        sr, sc = rng.choice(H_SHAPE)
        if (sr, sc) in occupied:
            continue
        path = [(sr, sc)]
        in_path = {(sr, sc)}
        pdr = pdc = 0
        bends = 0
        max_bends = 3 if want >= 5 else 2
        for _step in range(1, want):
            r, c = path[-1]
            opts = []
            for dr, dc in DIRS:
                if (pdr, pdc) != (0, 0) and (dr, dc) == (-pdr, -pdc):
                    continue
                nr, nc = r + dr, c + dc
                if (nr, nc) not in H_SET or (nr, nc) in occupied or (nr, nc) in in_path:
                    continue
                is_turn = (pdr, pdc) != (0, 0) and (dr, dc) != (pdr, pdc)
                if is_turn and bends >= max_bends:
                    continue
                opts += [(dr, dc, nr, nc)] * (1 if is_turn else 6)
            if not opts:
                break
            dr, dc, nr, nc = rng.choice(opts)
            if (pdr, pdc) != (0, 0) and (dr, dc) != (pdr, pdc):
                bends += 1
            pdr, pdc = dr, dc
            path.append((nr, nc))
            in_path.add((nr, nc))
        if len(path) < 2:
            continue
        for cand in (path, list(reversed(path))):
            hd = (cand[-1][0] - cand[-2][0], cand[-1][1] - cand[-2][1])
            if can_exit(cand, hd, occupied, H_ROWS, H_COLS):
                return cand
    return None


def gen_l5():
    rows, cols, target = 13, 12, 36
    for retry in range(200):
        rng = random.Random(5000 + retry * 100003)
        occupied = set()
        pieces = []
        stagnant = 0
        while len(pieces) < target and stagnant < 900:
            p = try_piece(occupied, rows, cols, 2, 6, rng)
            if not p:
                stagnant += 1
                continue
            stagnant = 0
            pid = "p%d" % len(pieces)
            occupied.update(p["cells"])
            pieces.append({"id": pid, "cells": [list(c) for c in p["cells"]]})
        if len(pieces) != target:
            continue
        if greedy_solve(pieces, rows, cols) is None:
            continue
        print(f"L5 {rows}x{cols} pieces=36 filled={len(occupied)}/{rows*cols}",
              file=sys.stderr)
        return {"id": 5, "rows": rows, "cols": cols, "difficulty": "EASY",
                "allowedMistakes": 3, "pieces": pieces,
                "solution": [p["id"] for p in reversed(pieces)]}
    raise SystemExit("failed to generate level 5")


def gen_l6():
    best = None
    for retry in range(120):
        rng = random.Random(7000 + retry * 100003)
        occupied = set()
        pieces = []
        stagnant = 0
        while len(pieces) < 42 and stagnant < 1200:
            path = try_piece_mask(occupied, rng)
            if not path:
                stagnant += 1
                continue
            stagnant = 0
            pid = "p%d" % len(pieces)
            occupied.update(path)
            pieces.append({"id": pid, "cells": [list(c) for c in path]})
        filled = len(occupied)
        if len(pieces) >= 36 and filled >= 100:
            cand = {"id": 6, "rows": H_ROWS, "cols": H_COLS,
                    "difficulty": "MEDIUM", "allowedMistakes": 3,
                    "shapeName": "Heart",
                    "shape": [list(c) for c in H_SHAPE],
                    "pieces": pieces,
                    "solution": [p["id"] for p in reversed(pieces)]}
            if greedy_solve(pieces, H_ROWS, H_COLS) is None:
                continue
            print(f"L6 heart {H_ROWS}x{H_COLS} pieces={len(pieces)} "
                  f"filled={filled}/{len(H_SHAPE)}", file=sys.stderr)
            if len(pieces) == 40:
                return cand
            if best is None or len(pieces) > len(best["pieces"]):
                best = cand
    if best is not None:
        return best
    raise SystemExit("failed to generate level 6 heart")


def main():
    print(json.dumps([gen_l5(), gen_l6()], indent=1))


if __name__ == "__main__":
    main()

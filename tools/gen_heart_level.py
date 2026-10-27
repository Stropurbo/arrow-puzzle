#!/usr/bin/env python3
"""Generate the Heart shaped level (level 101) for Tap Arrows.

Method: the same reverse construction as tools/gen_levels.js, but piece
paths are restricted to the heart mask below. Pieces are placed
last-removed-first; each new piece is accepted only if it can already
slide out against the pieces placed so far, so reversing the placement
order is a guaranteed-valid removal order. The script is deterministic
(it searches seeds in order and stops at the first board filling at
least 58 of the 64 heart cells) and prints one level JSON object to
stdout. It does NOT rewrite levels.json itself: paste the output as a
new entry, then run the unit tests (LevelsValidationTest covers the
shape, its symmetry, solvability, and session round-trip).

Heart mask, 9 rows x 11 cols ('#' = playable):
    .###...###.
    ###########
    ###########
    ###########
    .#########.
    ..#######..
    ...#####...
    ....###....
    .....#.....
"""
import json
import random
import sys

SHAPE_ROWS = [
    [(1, 3), (7, 9)],
    [(0, 10)],
    [(0, 10)],
    [(0, 10)],
    [(1, 9)],
    [(2, 8)],
    [(3, 7)],
    [(4, 6)],
    [(5, 5)],
]
ROWS = len(SHAPE_ROWS)
COLS = 11
SHAPE = [(r, c) for r, ivs in enumerate(SHAPE_ROWS) for (a, b) in ivs for c in range(a, b + 1)]
SHAPE_SET = set(SHAPE)
DIRS = [(-1, 0), (0, 1), (1, 0), (0, -1)]


def can_exit(cells, hd, occupied):
    pose = list(cells)
    for _ in range(ROWS + COLS + len(cells) + 2):
        if all(not (0 <= r < ROWS and 0 <= c < COLS) for r, c in pose):
            return True
        last = pose[-1]
        nxt = pose[1:] + [(last[0] + hd[0], last[1] + hd[1])]
        inb = [x for x in nxt if 0 <= x[0] < ROWS and 0 <= x[1] < COLS]
        if len(inb) != len(set(inb)) or any(x in occupied for x in inb):
            return False
        pose = nxt
    return False


def try_piece(rng, occupied):
    for _ in range(300):
        want = rng.choice([2, 3, 3, 4, 4, 5, 5, 6])
        sr, sc = rng.choice(SHAPE)
        if (sr, sc) in occupied:
            continue
        path = [(sr, sc)]
        inpath = {(sr, sc)}
        dr = dc = 0
        bends = 0
        maxb = 3 if want >= 6 else 2
        for _step in range(1, want):
            r, c = path[-1]
            opts = []
            for ddr, ddc in DIRS:
                if (dr, dc) != (0, 0) and (ddr, ddc) == (-dr, -dc):
                    continue
                nr, nc = r + ddr, c + ddc
                if (nr, nc) not in SHAPE_SET or (nr, nc) in occupied or (nr, nc) in inpath:
                    continue
                is_turn = (dr, dc) != (0, 0) and (ddr, ddc) != (dr, dc)
                if is_turn and bends >= maxb:
                    continue
                opts += [(ddr, ddc, nr, nc)] * (1 if is_turn else 6)
            if not opts:
                break
            ddr, ddc, nr, nc = rng.choice(opts)
            if (dr, dc) != (0, 0) and (ddr, ddc) != (dr, dc):
                bends += 1
            dr, dc = ddr, ddc
            path.append((nr, nc))
            inpath.add((nr, nc))
        if len(path) < 2:
            continue
        a, b = path[-2], path[-1]
        hd = (b[0] - a[0], b[1] - a[1])
        if can_exit(path, hd, occupied):
            return path
        rev = list(reversed(path))
        a, b = rev[-2], rev[-1]
        hd = (b[0] - a[0], b[1] - a[1])
        if can_exit(rev, hd, occupied):
            return rev
    return None


def main():
    for seed in range(200):
        rng = random.Random(1000 + seed * 83)
        occupied = set()
        pieces = []
        stagnant = 0
        while len(pieces) < 24 and stagnant < 700:
            path = try_piece(rng, occupied)
            if path is None:
                stagnant += 1
                continue
            stagnant = 0
            pieces.append({"id": f"p{len(pieces)}", "cells": [list(x) for x in path]})
            occupied.update(path)
        filled = sum(len(p["cells"]) for p in pieces)
        if len(pieces) >= 18 and filled >= 58:
            level = {
                "id": 101, "rows": ROWS, "cols": COLS,
                "difficulty": "MEDIUM", "allowedMistakes": 3,
                "shapeName": "Heart",
                "shape": [list(x) for x in SHAPE],
                "pieces": pieces,
                "solution": [p["id"] for p in reversed(pieces)],
            }
            print(json.dumps(level))
            print(f"# seed={seed} pieces={len(pieces)} filled={filled}/{len(SHAPE)}", file=sys.stderr)
            return
    raise SystemExit("no heart board reached the fill target")


if __name__ == "__main__":
    main()

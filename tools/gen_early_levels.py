#!/usr/bin/env python3
"""Generate video-like ramp levels 1-5 for Tap Arrows (original layouts).

The reference video's difficulty ramps with level: very few arrows first,
then more as levels increase. Tap Arrows' original levels 1-5 had 12-17
arrows, so a first-time player faced a hard board immediately. These
replacements use the same reverse-construction method as
tools/gen_levels.js (pieces placed last-removed-first, each accepted only
if it can already slide out against the pieces placed so far, so the
reverse placement order is a guaranteed-valid removal order), with
smaller boards and fewer pieces:

    L1 5x5,  3 pieces
    L2 6x6,  6 pieces
    L3 6x7,  8 pieces
    L4 7x7, 10 pieces
    L5 7x8, 12 pieces

NOTE (2026-10-27): levels 1-4 are still as above, but level 5 was
later rebuilt to video size (36 arrows, 13x12) by
tools/gen_video_levels.py, and level 6 became a 38-arrow heart.
Do not re-run this script for level 5 without also re-applying
the video levels, or level 5 will revert to 12 arrows.
Layouts are generated randomly (deterministic seeds), not copied from
any other game's levels. Prints a JSON array of the 5 new level objects
to stdout; it does NOT rewrite levels.json itself.
"""
import json
import random
import sys

DIRS = [(-1, 0), (0, 1), (1, 0), (0, -1)]  # UP RIGHT DOWN LEFT

SPECS = {
    1: (5, 5, 3, 2, 3),
    2: (6, 6, 6, 2, 4),
    3: (6, 7, 8, 2, 4),
    4: (7, 7, 10, 2, 5),
    5: (7, 8, 12, 3, 5),
}


def can_exit(cells, hd, occupied, rows, cols):
    pose = list(cells)
    for _ in range(rows + cols + len(cells) + 2):
        if all(not (0 <= r < rows and 0 <= c < cols) for r, c in pose):
            return True
        last = pose[-1]
        nxt = pose[1:] + [(last[0] + hd[0], last[1] + hd[1])]
        inb = [x for x in nxt if 0 <= x[0] < rows and 0 <= x[1] < cols]
        if len(inb) != len(set(inb)) or any(x in occupied for x in inb):
            return False
        pose = nxt
    return False


def greedy_solve(pieces, rows, cols):
    remaining = {p["id"]: p for p in pieces}
    order = []
    while remaining:
        occ = set()
        for p in remaining.values():
            occ.update(tuple(c) for c in p["cells"])
        found = None
        for p in remaining.values():
            cells = [tuple(c) for c in p["cells"]]
            others = occ - set(cells)
            hd = (cells[-1][0] - cells[-2][0],
                  cells[-1][1] - cells[-2][1])
            if can_exit(cells, hd, others, rows, cols):
                found = p["id"]
                break
        if found is None:
            return None
        order.append(found)
        del remaining[found]
    return order


def try_piece(occupied, rows, cols, len_min, len_max, rng):
    for _ in range(250):
        if len_max < 4:
            want_len = rng.randint(len_min, len_max)
        else:
            want_len = rng.randint(2, min(4, len_max)) if rng.random() < 0.35 \
                else rng.randint(max(4, len_min), len_max)
        if rng.random() < 0.65:
            edge = rng.randrange(4)
            if edge == 0:
                sr, sc = rng.randint(0, min(2, rows - 1)), rng.randint(0, cols - 1)
            elif edge == 1:
                sr, sc = rng.randint(max(0, rows - 3), rows - 1), rng.randint(0, cols - 1)
            elif edge == 2:
                sr, sc = rng.randint(0, rows - 1), rng.randint(0, min(2, cols - 1))
            else:
                sr, sc = rng.randint(0, rows - 1), rng.randint(max(0, cols - 3), cols - 1)
        else:
            sr, sc = rng.randint(0, rows - 1), rng.randint(0, cols - 1)
        if (sr, sc) in occupied:
            continue
        path = [(sr, sc)]
        in_path = {(sr, sc)}
        pdr = pdc = 0
        bends = 0
        max_bends = 3 if want_len >= 6 else 2
        for _step in range(1, want_len):
            r, c = path[-1]
            opts = []
            for dr, dc in DIRS:
                if (pdr, pdc) != (0, 0) and (dr, dc) == (-pdr, -pdc):
                    continue
                nr, nc = r + dr, c + dc
                if not (0 <= nr < rows and 0 <= nc < cols):
                    continue
                if (nr, nc) in occupied or (nr, nc) in in_path:
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
        if len(path) < max(2, len_min - 1):
            continue
        for cand in (path, list(reversed(path))):
            hd = (cand[-1][0] - cand[-2][0], cand[-1][1] - cand[-2][1])
            if can_exit(cand, hd, occupied, rows, cols):
                return {"cells": cand}
    return None


def gen_level(level_id):
    rows, cols, target, len_min, len_max = SPECS[level_id]
    for retry in range(40):
        rng = random.Random(1000 + level_id * 83 + retry * 100003)
        occupied = set()
        pieces = []
        stagnant = 0
        while len(pieces) < target and stagnant < 700:
            p = try_piece(occupied, rows, cols, len_min, len_max, rng)
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
        solution = [p["id"] for p in reversed(pieces)]
        return {
            "id": level_id, "rows": rows, "cols": cols,
            "difficulty": "TUTORIAL", "allowedMistakes": 3,
            "pieces": pieces, "solution": solution,
        }
    raise SystemExit("failed to generate level %d" % level_id)


def main():
    levels = [gen_level(i) for i in range(1, 6)]
    for lv in levels:
        print("L%d %dx%d pieces=%d" % (lv["id"], lv["rows"], lv["cols"], len(lv["pieces"])),
              file=sys.stderr)
    print(json.dumps(levels, indent=1))


if __name__ == "__main__":
    main()

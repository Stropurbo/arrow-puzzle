// Generates 100 increasingly dense, interwoven Arrow Puzzle levels in the
// style of the reference game: flat thin polylines, mostly straight runs with
// a bend or two, packed tightly enough that only a few dot-grid corridors show.
//
// Method: reverse construction. Pieces are placed LAST-removed-first on an empty
// board; each new piece is accepted only if canExit() holds against the pieces
// already placed (an exact port of the Kotlin ExitSimulator). The reverse
// placement order is therefore a guaranteed-valid removal order, and greedy
// solving can never get stuck: at every step the last-placed remaining piece is
// safe. Deterministic: mulberry32(seed = 1000 + levelId * 83 + retry * 100003).
const fs = require('fs');

function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const DIRS = [[-1, 0], [0, 1], [1, 0], [0, -1]]; // UP RIGHT DOWN LEFT

function canExit(cells, hd, occupied, rows, cols) {
  let pose = cells.map(c => [c[0], c[1]]);
  const limit = rows + cols + cells.length + 2;
  for (let s = 0; s < limit; s++) {
    let allOut = true;
    for (const c of pose) {
      if (c[0] >= 0 && c[0] < rows && c[1] >= 0 && c[1] < cols) { allOut = false; break; }
    }
    if (allOut) return true;
    const last = pose[pose.length - 1];
    const next = pose.slice(1);
    next.push([last[0] + hd[0], last[1] + hd[1]]);
    const inBoard = next.filter(c => c[0] >= 0 && c[0] < rows && c[1] >= 0 && c[1] < cols);
    const seen = new Set();
    for (const c of inBoard) {
      const k = c[0] * cols + c[1];
      if (seen.has(k)) return false;
      seen.add(k);
      if (occupied.has(k)) return false;
    }
    pose = next;
  }
  return false;
}

function greedySolve(pieces, rows, cols) {
  const remaining = new Set(pieces.map(p => p.id));
  const byId = new Map(pieces.map(p => [p.id, p]));
  const order = [];
  while (remaining.size > 0) {
    const list = [...remaining].map(id => byId.get(id));
    const occ = new Set();
    for (const p of list) for (const c of p.cells) occ.add(c[0] * cols + c[1]);
    let found = null;
    for (const p of list) {
      const others = new Set(occ);
      for (const c of p.cells) others.delete(c[0] * cols + c[1]);
      if (canExit(p.cells, p.heading, others, rows, cols)) { found = p.id; break; }
    }
    if (found === null) return null;
    order.push(found);
    remaining.delete(found);
  }
  return order;
}

// Board size, arrow count and arrow length per difficulty band.
// Counts are interpolated inside a band so density ramps smoothly.
function tierFor(id) {
  // Video-like ramp: very few arrows first, then more as levels increase.
  // Regenerated 2026-10-27 (tools/gen_early_levels.py) as 3, 6, 8, 10, 12.
  if (id <= 5)   return { rows: [5, 7],   cols: [5, 8],   pieces: [3, 12],  len: [2, 5],  diff: 'TUTORIAL',     from: 1,  to: 5 };
  if (id <= 15)  return { rows: [8, 9],   cols: [8, 9],   pieces: [14, 18], len: [3, 7],  diff: 'EASY',         from: 6,  to: 15 };
  if (id <= 35)  return { rows: [9, 11],  cols: [9, 11],  pieces: [20, 26], len: [4, 8],  diff: 'MEDIUM',       from: 16, to: 35 };
  if (id <= 60)  return { rows: [11, 13], cols: [11, 13], pieces: [24, 30], len: [4, 10], diff: 'CHALLENGING',  from: 36, to: 60 };
  if (id <= 85)  return { rows: [13, 15], cols: [13, 15], pieces: [28, 34], len: [5, 10], diff: 'HARD',         from: 61, to: 85 };
  return         { rows: [16, 18], cols: [16, 18], pieces: [36, 42], len: [5, 11], diff: 'EXPERT',       from: 86, to: 100 };
}

function randInt(rng, lo, hi) { return lo + Math.floor(rng() * (hi - lo + 1)); }

function tryPiece(occupied, rows, cols, lenMin, lenMax, rng) {
  for (let att = 0; att < 250; att++) {
    // Mixed lengths: mostly long runs, with a sprinkling of short 2-4 cell
    // arrows so the board reads like the reference's varied line work.
    const wantLen = rng() < 0.35
      ? randInt(rng, 2, Math.min(4, lenMax))
      : randInt(rng, Math.max(4, lenMin), lenMax);
    let sr, sc;
    // Bias start cells toward the border: border-headed arrows can always leave,
    // which is what lets the board fill up from the outside in.
    if (rng() < 0.65) {
      const edge = Math.floor(rng() * 4);
      if (edge === 0) { sr = randInt(rng, 0, Math.min(2, rows - 1)); sc = randInt(rng, 0, cols - 1); }
      else if (edge === 1) { sr = randInt(rng, Math.max(0, rows - 3), rows - 1); sc = randInt(rng, 0, cols - 1); }
      else if (edge === 2) { sr = randInt(rng, 0, rows - 1); sc = randInt(rng, 0, Math.min(2, cols - 1)); }
      else { sr = randInt(rng, 0, rows - 1); sc = randInt(rng, Math.max(0, cols - 3), cols - 1); }
    } else {
      sr = randInt(rng, 0, rows - 1); sc = randInt(rng, 0, cols - 1);
    }
    if (occupied.has(sr * cols + sc)) continue;

    // Winding walk with a strong straight bias: long parallel runs like the
    // reference art, with at most a couple of rounded 90-degree bends.
    const path = [[sr, sc]];
    const inPath = new Set([sr * cols + sc]);
    let pdr = 0, pdc = 0;
    let bends = 0;
    const maxBends = wantLen >= 6 ? 3 : 2;

    for (let step = 1; step < wantLen; step++) {
      const r = path[path.length - 1][0], c = path[path.length - 1][1];
      const opts = [];
      for (const [dr, dc] of DIRS) {
        if (pdr !== 0 || pdc !== 0) {
          if (dr === -pdr && dc === -pdc) continue; // no U-turn
        }
        const nr = r + dr, nc = c + dc;
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
        if (occupied.has(nr * cols + nc) || inPath.has(nr * cols + nc)) continue;
        const isTurn = pdr !== 0 && (dr !== pdr || dc !== pdc);
        if (isTurn && bends >= maxBends) continue;
        const weight = isTurn ? 1 : 6;
        for (let k = 0; k < weight; k++) opts.push([dr, dc, nr, nc]);
      }
      if (opts.length === 0) break;
      const pick = opts[Math.floor(rng() * opts.length)];
      if (pdr !== 0 && (pick[0] !== pdr || pick[1] !== pdc)) bends++;
      pdr = pick[0]; pdc = pick[1];
      path.push([pick[2], pick[3]]);
      inPath.add(pick[2] * cols + pick[3]);
    }

    if (path.length < Math.max(2, lenMin - 1)) continue;
    const a = path[path.length - 2], b = path[path.length - 1];
    const hd = [b[0] - a[0], b[1] - a[1]];
    if (canExit(path, hd, occupied, rows, cols)) return { cells: path, heading: hd };

    const rev = path.slice().reverse();
    const ra = rev[rev.length - 2], rb = rev[rev.length - 1];
    const rhd = [rb[0] - ra[0], rb[1] - ra[1]];
    if (canExit(rev, rhd, occupied, rows, cols)) return { cells: rev, heading: rhd };
  }
  return null;
}

// Final packing pass: short straight darts fitted into any remaining corridor.
function exhaustiveDart(occupied, rows, cols, rng) {
  const starts = [];
  for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) {
    if (!occupied.has(r * cols + c)) starts.push([r, c]);
  }
  for (let i = starts.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [starts[i], starts[j]] = [starts[j], starts[i]];
  }
  for (const [r, c] of starts) {
    const dirOrder = [0, 1, 2, 3].sort(() => rng() - 0.5);
    for (const d of dirOrder) {
      const [dr, dc] = DIRS[d];
      for (const len of [4, 3, 2]) {
        const cells = [];
        let ok = true;
        for (let k = len - 1; k >= 0; k--) {
          const nr = r - dr * k, nc = c - dc * k;
          if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || occupied.has(nr * cols + nc)) { ok = false; break; }
          cells.push([nr, nc]);
        }
        if (!ok) continue;
        if (canExit(cells, [dr, dc], occupied, rows, cols)) {
          return { cells, heading: [dr, dc] };
        }
      }
    }
  }
  return null;
}

function genLevel(id, verbose) {
  const t = tierFor(id);
  const frac = (id - t.from) / Math.max(1, t.to - t.from);
  const wanted = Math.round(t.pieces[0] + frac * (t.pieces[1] - t.pieces[0]));
  const hardMin = Math.max(6, Math.floor(t.pieces[0] * 0.7));
  let best = null;

  for (let retry = 0; retry < 6; retry++) {
    const rng = mulberry32(1000 + id * 83 + retry * 100003);
    const rows = randInt(rng, t.rows[0], t.rows[1]);
    const cols = randInt(rng, t.cols[0], t.cols[1]);
    const target = wanted + (rng() < 0.35 ? 1 : 0);
    const occupied = new Set();
    const pieces = [];
    let stagnant = 0;

    while (pieces.length < target && stagnant < 700) {
      const fillFrac = pieces.length / target;
      const stuck = stagnant > 120;
      const hi = stuck ? Math.max(4, Math.round(t.len[1] - fillFrac * 2)) : t.len[1];
      const lo = stuck ? 2 : Math.min(t.len[0], hi - 1);
      const p = stagnant > 280
        ? exhaustiveDart(occupied, rows, cols, rng)
        : tryPiece(occupied, rows, cols, lo, hi, rng);
      if (!p) {
        if (stagnant > 420) break;
        stagnant++;
        continue;
      }
      stagnant = 0;
      const pid = 'p' + pieces.length;
      for (const c of p.cells) occupied.add(c[0] * cols + c[1]);
      pieces.push({ id: pid, cells: p.cells, heading: p.heading });
    }

    // Densify: fit a few extra short darts into leftover corridors. They are
    // appended last, so they are removed first and cannot break solvability.
    let extra = 0;
    while (extra < 14) {
      const d = exhaustiveDart(occupied, rows, cols, rng);
      if (!d) break;
      const pid = 'p' + pieces.length;
      for (const c of d.cells) occupied.add(c[0] * cols + c[1]);
      pieces.push({ id: pid, cells: d.cells, heading: d.heading });
      extra++;
    }

    if (pieces.length < 6) continue;

    const solution = pieces.map(p => p.id).reverse();
    if (greedySolve(pieces, rows, cols) === null) continue;

    const out = pieces.map(p => ({ id: p.id, cells: p.cells }));
    const cells = pieces.reduce((s, p) => s + p.cells.length, 0);
    const candidate = {
      id, rows, cols, difficulty: t.diff, allowedMistakes: 3,
      pieces: out, solution
    };
    if (!best || pieces.length > best.pieces.length) best = candidate;
    if (verbose) {
      console.log(`L${id} ${rows}x${cols} pieces=${pieces.length} wanted=${wanted} ` +
        `fill=${Math.round(cells / (rows * cols) * 100)}% avgLen=${(cells / pieces.length).toFixed(1)} retries=${retry}`);
    }
    if (pieces.length >= wanted) return candidate;
  }
  if (best) { if (verbose) console.log(`L${id} using best=${best.pieces.length}/${wanted}`); return best; }
  throw new Error('failed level ' + id);
}

const levels = [];
for (let id = 1; id <= 100; id++) {
  levels.push(genLevel(id, id % 10 === 0));
  if (id % 10 === 0) console.log('built', id);
}

const tiers = [[1, 5], [6, 15], [16, 35], [36, 60], [61, 85], [86, 100]];
for (const [a, b] of tiers) {
  const slice = levels.filter(l => l.id >= a && l.id <= b);
  const avgP = (slice.reduce((s, l) => s + l.pieces.length, 0) / slice.length).toFixed(1);
  const avgLen = (slice.reduce((s, l) => s + l.pieces.reduce((x, p) => x + p.cells.length, 0) / l.pieces.length, 0) / slice.length).toFixed(1);
  const avgFill = (slice.reduce((s, l) => {
    const cells = l.pieces.reduce((x, p) => x + p.cells.length, 0);
    return s + cells / (l.rows * l.cols);
  }, 0) / slice.length * 100).toFixed(0);
  console.log(`L${a}-${b}: pieces~${avgP} len~${avgLen} fill~${avgFill}% diff=${slice[0].difficulty}`);
}

fs.writeFileSync('app/src/main/assets/levels.json', JSON.stringify(levels));
console.log('wrote levels.json bytes=', fs.statSync('app/src/main/assets/levels.json').size);


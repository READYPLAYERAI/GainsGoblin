# Git Worktrees

This repo uses [git worktrees](https://git-scm.com/docs/git-worktree) to develop
multiple features in parallel: each feature gets its own checkout (working
directory) with its own branch, so you never need to stash or switch to change
context.

## Layout

- **Main checkout** `gainsgoblin/` — stays on `master` (the stable branch).
- **Feature checkouts** live as siblings under `../gainsgoblin-worktrees/<feature>/`.

```
projects/
├── gainsgoblin/                      # main checkout, on master
└── gainsgoblin-worktrees/
    ├── room-backup/                  # feature/room-backup
    └── my-feature/                   # feature/my-feature
```

## Quick start

Create a new feature worktree (branch `feature/<name>` from `master`):

```sh
git wt new my-feature
cd ../gainsgoblin-worktrees/my-feature
```

List all worktrees:

```sh
git wt list
```

Remove a worktree once its branch is merged:

```sh
git wt remove my-feature
```

Plain git equivalents (no helper):

```sh
git worktree add -b feature/my-feature ../gainsgoblin-worktrees/my-feature master
git worktree remove ../gainsgoblin-worktrees/my-feature
git branch -d feature/my-feature
```

## Rules & tips

- **Keep `master` clean** — only finished, tested work gets merged onto it.
  Branch names: `feature/<name>`.
- **One worktree per active feature**; remove it as soon as the branch is merged
  so stale copies don't pile up.
- **Android Studio / CLI specifics:**
  - Each worktree is its own project — open it as a separate Studio window.
  - `.idea/` and `local.properties` are gitignored, so each new worktree needs
    them regenerated / copied. Studio recreates `.idea/` on first open; for CLI
    builds copy `local.properties` once: `cp local.properties ../gainsgoblin-worktrees/<feature>/`
  - Gradle caches (`.gradle/`, `build/`) are per-checkout; first build in a new
    worktree is slower.
- **Sharing:** push a feature branch when it needs review/CI:
  `git push -u origin feature/my-feature`.
- **Cleanup:** never delete a worktree directory by hand while it's registered —
  use `git worktree remove`, or run `git worktree prune` after manual deletes.

## Current worktrees

```sh
git wt list
```

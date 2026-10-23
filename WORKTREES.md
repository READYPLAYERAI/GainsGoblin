# Git Worktrees

This project uses [git worktrees](https://git-scm.com/docs/git-worktree) so multiple
features can be checked out and built side by side. Each feature branch gets its own
directory under `.worktrees/` (which is gitignored).

## Layout

```
gainsgoblin/                  <- primary worktree, always on master
├── .worktrees/
│   ├── room-backup/          <- feature/room-backup
│   └── <feature-name>/       <- one directory per in-flight feature
```

## Day-to-day

Start a new feature:

```bash
git wt-new my-new-feature     # creates branch + worktree from master
cd .worktrees/my-new-feature
```

See what's checked out where:

```bash
git wt                        # alias for `git worktree list`
```

Remove a finished feature (worktree plus its branch):

```bash
git wt-rm my-new-feature      # refuses to run if the worktree has uncommitted changes
```

## Notes

- **Base (primary) worktree stays on `master`** — it's your main line. Do all
  feature work in `.worktrees/<feature>`.
- Builds are independent per worktree (each has its own `build/` and `.gradle/`).
  Gradle caches are still shared via `~/.gradle`, so dependency downloads don't repeat.
- `local.properties` was copied into existing worktrees. If you create a new worktree
  with `git wt-new` and Android Studio can't find the SDK, copy it again:
  `cp local.properties .worktrees/<feature>/` (or just open the folder in Studio,
  which regenerates it).
- You can open each worktree in its own Android Studio window if you want to work on
  multiple features side-by-side.

## Raw commands (no aliases)

```bash
git worktree list
git worktree add .worktrees/<feature> -b <feature> master
git worktree remove .worktrees/<feature> && git branch -D <feature>
```

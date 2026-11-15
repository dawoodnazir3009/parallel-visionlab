# Team workflow

## Set up each computer

Clone the repository and configure the actual contributor's identity locally:

```powershell
git clone https://github.com/dawoodnazir3009/parallel-visionlab.git
cd parallel-visionlab
git config --local user.name "Your name"
git config --local user.email "Your GitHub commit email"
```

Install JDK 21 and Maven, then run `mvn verify` and `mvn javafx:run`.
The ignored `.tools` directory is local to the original computer and is not
included in a clone. Java-only work does not require CUDA.

## Start a task

Read [the roadmap](docs/roadmap.md) and agree on a task owner. Then:

```powershell
git switch main
git pull --ff-only origin main
git switch -c feature/sequential-grayscale
```

Use a distinct branch for each task. Check `git status` before switching branches;
preserve uncommitted work. Agree on shared interfaces before editing them on
multiple computers.

## Commit and push

- Make one coherent, reviewable change per commit. Include its relevant tests.
- Run `mvn verify` for Java changes and the appropriate native checks for CUDA.
  For documentation-only changes, inspect links, content, and `git diff --check`.
- Stage specific files, inspect `git diff --cached`, then commit with a meaningful
  message such as `Add grayscale alpha preservation tests`.
- Push completed small changes to the task branch regularly. Do not generate empty
  or cosmetic churn to reach a commit quota. Do not commit credentials, local
  dependencies, build output, or large image datasets.

```powershell
git add path/to/changed-file
git diff --cached
git commit -m "Describe the completed change"
git push -u origin feature/sequential-grayscale
```

Open a pull request into `main`, describe behavior and verification, and have a
teammate review it. Keep `main` buildable; avoid force pushes to shared branches.
To retain small commits, use GitHub's merge-commit option rather than squashing
when the team wants that history preserved. Repository branch protection and
required reviews are recommended; they are not configured by this document.

## Handoff between people or Codex sessions

The Git repository is the shared source of truth. A shared Codex sign-in does not
replace pulling code or reading the project documentation. Before ending a task,
record the completed phase, checks, limitations, and next task in the roadmap or
pull request. Start the next session by inspecting current files and Git status.
Use the contributor's own Git identity; do not attribute work to another teammate.

Documentation-maintenance commits may be pushed directly to `main` by the project
owner; implementation tasks should follow the feature-branch review workflow.

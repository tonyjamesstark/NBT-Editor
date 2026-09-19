# Adopt upstream's NBTManager to SubjectIO rename, or decide not to

Status: `ready-for-human`
Created: 2026-09-18
Source: [`../spec.md`](../spec.md), upstream group C item `290a846`

`290a846` renames `NBTManager` to `SubjectIO` across upstream's tree. The sync pass skipped it
because nothing misbehaves at 26.2, so it is a naming decision and not part of a defect pass. It
is recorded here rather than in the spec's result line, where it would be the tail of a 64-item
list nobody rereads.

The name appears in 28 files under `src/main/java`, including the whole of
`multiversion/nbt/manager/` and its `components/` subpackage. `CONTEXT.md` names neither
`NBTManager` nor `SubjectIO` today, so taking the rename means adding the new name to the
glossary, which `docs/agents/domain.md` requires before anything is named after it.

## The argument against, which is not obvious from upstream's diff

The package being renamed is `multiversion/nbt/manager/`, and `CLAUDE.md` says the multi-version
layer is being dismantled rather than maintained. Renaming a tree scheduled for deletion spends
the churn twice. The counter-argument is the one the spec gives: the rename lowers friction on
every future cherry-pick that touches these files, and there will be more of those than there
will be deletions in the near term.

Whoever decides should answer which of those two happens first. If the manager tree is going away
inside the next few phases, this is `wontfix`.

## Comments

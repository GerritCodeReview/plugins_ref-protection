Ref protection plugin.

Protects against commits being lost by creating backups of deleted refs 
(or non-fast-forward commits) under the `refs/backups/` namespace.

Branch deletion protection can be disabled by setting 
`plugin.ref-protection.protectDeleted false` in `gerrit.config`.
Similarly, non-fast-forward update protection can be disabled with
`plugin.ref-protection.protectFastForward false`.

Branches under `refs/heads/` that are deleted or rewritten are backed up
as `refs/backups/heads/branch-name-YYYYMMDD-HHmmss` by default, or as
sequentially increasing numbers under `refs/backups/heads/branch-name/#`
by setting `plugin.ref-protection.useTimestamp false`.

Tags under `refs/tags/` that are deleted are backed up (as branches) as
`refs/backups/tags/tag-name-YYYYMMDD-HHmmss` or as sequentially
increasing numbers under `refs/backups/tags/branch-name/#` using the same
`plugin.ref-protection.useTimestamp` setting.

By default, the backups are created as branches.  Optionally, they may
be created as tags, containing information about the original ref that
was changed, as well as the user that performed the change.  This can
be enabled by setting `plugin.ref-protection.createTag true`.

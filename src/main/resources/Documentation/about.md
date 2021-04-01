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

The `Create Reference` and `Push` permissions need to be granted to the
`Registered Users` group on `refs/backups/*`, to allow the user, whose identity
is inherited by the plugin, creating the backup refs, otherwise backups will fail.

Since the `refs/backups/*` branch is created in the same User scope as the
delete, `Create Reference` and `Push` permission need to be granted to any user
that is allowed to delete or force-push a ref or backups will fail.
This is most conveniently achieved by granting the permission to `Registered
Users` (all logged in users).

Furthermore, to avoid the backup refs to be exposed to the users, a block on the
`Read` permission on `refs/backups/*` is necessary.
This will avoid a possible security issue in the following case:
* user A has exclusive access to `refs/super-secret-branch`
* user A create a change in `refs/super-secret-branch`
* user A deletes `refs/super-secret-branch`
* `ref-protection` creates a backup in `refs/backups`
* user B can access `super-secret-branch` backup

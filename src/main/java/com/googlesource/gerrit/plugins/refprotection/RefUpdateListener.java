/*
 *  The MIT License
 *
 *  Copyright 2014 Sony Mobile Communications AB. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.googlesource.gerrit.plugins.refprotection;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RefUpdateListener implements EventListener {

  private static final Logger log = LoggerFactory.getLogger(RefUpdateListener.class);
  private final ProjectCache projectCache;
  private final CurrentUser user;
  private final GitRepositoryManager repoManager;
  private final BackupRef backupRef;
  private final boolean protectDeleted;
  private final boolean protectFastForward;

  @Inject
  RefUpdateListener(
      ProjectCache projectCache,
      CurrentUser user,
      GitRepositoryManager repoManager,
      BackupRef backupRef,
      PluginConfigFactory cfg,
      @PluginName String pluginName) {
    this.projectCache = projectCache;
    this.user = user;
    this.repoManager = repoManager;
    this.backupRef = backupRef;
    this.protectDeleted = cfg.getFromGerritConfig(pluginName).getBoolean("protectDeleted", true);
    this.protectFastForward =
        cfg.getFromGerritConfig(pluginName).getBoolean("protectFastForward", true);
  }

  @Override
  public void onEvent(Event event) {
    if (event instanceof RefUpdatedEvent) {
      RefUpdatedEvent refUpdate = (RefUpdatedEvent) event;
      if ((protectDeleted || protectFastForward) && isRelevantRef(refUpdate)) {
        Project.NameKey nameKey = refUpdate.getProjectNameKey();
        try {
          ProjectState state = projectCache.checkedGet(nameKey);
          if (state != null) {
            ProjectResource project = new ProjectResource(state, user);
            if ((protectDeleted && isRefDeleted(refUpdate))
                || (protectFastForward && isNonFastForwardUpdate(refUpdate, project))) {
              backupRef.createBackup(refUpdate, project);
            }
          }
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Is the event on a relevant ref?
   *
   * @param event the Event
   * @return True if relevant, otherwise False.
   */
  private boolean isRelevantRef(RefUpdatedEvent event) {
    return (!isNewRef(event))
        && (event.getRefName().startsWith(R_HEADS) || event.getRefName().startsWith(R_TAGS));
  }

  /**
   * Is the event a new ref?
   *
   * @param event the Event
   * @return True if a new ref, otherwise False.
   */
  private boolean isNewRef(RefUpdatedEvent event) {
    return event.refUpdate.get().oldRev.equals(ObjectId.zeroId().getName());
  }

  /**
   * Is the event a ref deletion?
   *
   * @param event the Event
   * @return True if a ref deletion, otherwise False.
   */
  private boolean isRefDeleted(RefUpdatedEvent event) {
    RefUpdateAttribute refUpdate = event.refUpdate.get();
    if (refUpdate.newRev.equals(ObjectId.zeroId().getName())) {
      log.info(
          String.format(
              "Ref Deleted: project [%s] refname [%s] old object id [%s]",
              event.getProjectNameKey().toString(), event.getRefName(), refUpdate.oldRev));
      return true;
    }
    return false;
  }

  /**
   * Is the event a non-fast-forward update?
   *
   * @param event the Event
   * @return True if a non-fast-forward update, otherwise False.
   */
  private boolean isNonFastForwardUpdate(RefUpdatedEvent event, ProjectResource project)
      throws RepositoryNotFoundException, IOException {
    if (isRefDeleted(event)) {
      // Can't be non-fast-forward if the ref was deleted, and
      // attempting a check would cause a MissingObjectException.
      return false;
    }
    RefUpdateAttribute refUpdate = event.refUpdate.get();
    try (Repository repo = repoManager.openRepository(project.getNameKey())) {
      try (RevWalk walk = new RevWalk(repo)) {
        RevCommit oldCommit = walk.parseCommit(repo.resolve(refUpdate.oldRev));
        RevCommit newCommit = walk.parseCommit(repo.resolve(refUpdate.newRev));
        return !walk.isMergedInto(oldCommit, newCommit);
      }
    }
  }
}

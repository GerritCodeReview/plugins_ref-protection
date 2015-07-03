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

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class RefUpdateListener implements GitReferenceUpdatedListener {

  private static final Logger log =
      LoggerFactory.getLogger(RefUpdateListener.class);
  private final ProjectControl.GenericFactory projectControl;
  private final CurrentUser user;
  private final GitRepositoryManager repoManager;
  private final BackupBranch backupBranch;

  @Inject
  RefUpdateListener(ProjectControl.GenericFactory p,
      CurrentUser user,
      GitRepositoryManager repoManager,
      BackupBranch backupBranch) {
    this.projectControl = p;
    this.user = user;
    this.repoManager = repoManager;
    this.backupBranch = backupBranch;
  }

  @Override
  public void onGitReferenceUpdated(final Event event) {
    if (isRelevantRef(event)) {
      Project.NameKey nameKey = new Project.NameKey(event.getProjectName());
      try {
        ProjectResource project =
            new ProjectResource(projectControl.controlFor(nameKey, user));
        if (isRefDeleted(event) || isNonFastForwardUpdate(event, project)) {
          backupBranch.createBackup(event, project);
        }
      } catch (NoSuchProjectException | IOException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Is the event on a relevant ref?
   *
   * @param event the Event
   * @return True if relevant, otherwise False.
   */
  private boolean isRelevantRef(Event event) {
    return (!isNewRef(event)) &&
           (event.getRefName().startsWith(R_HEADS)
            || event.getRefName().startsWith(R_TAGS));
  }

  /**
   * Is the event a new ref?
   *
   * @param event the Event
   * @return True if a new ref, otherwise False.
   */
  private boolean isNewRef(Event event) {
    return event.getOldObjectId().equals(ObjectId.zeroId().getName());
  }

  /**
   * Is the event a ref deletion?
   *
   * @param event the Event
   * @return True if a ref deletion, otherwise False.
   */
  private boolean isRefDeleted(Event event) {
    if (event.getNewObjectId().equals(ObjectId.zeroId().getName())) {
      log.info(String.format(
          "Ref Deleted: project [%s] refname [%s] old object id [%s]",
          event.getProjectName(), event.getRefName(), event.getOldObjectId()));
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
  private boolean isNonFastForwardUpdate(Event event, ProjectResource project)
      throws RepositoryNotFoundException, IOException {
    try (Repository repo = repoManager.openRepository(project.getNameKey())) {
      try (RevWalk walk = new RevWalk(repo)) {
        RevCommit oldCommit =
            walk.parseCommit(repo.resolve(event.getOldObjectId()));
        RevCommit newCommit =
            walk.parseCommit(repo.resolve(event.getNewObjectId()));
        return !walk.isMergedInto(oldCommit, newCommit);
      }
    }
  }
}

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
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.CreateBranch;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class RefUpdateListener implements GitReferenceUpdatedListener {

  private static final Logger log =
      LoggerFactory.getLogger(RefUpdateListener.class);
  private final CreateBranch.Factory createBranchFactory;
  private final ProjectControl.GenericFactory projectControl;
  private final CurrentUser user;

  @Inject
  RefUpdateListener(CreateBranch.Factory createBranchFactory,
      ProjectControl.GenericFactory p,
      CurrentUser user) {
    this.createBranchFactory = createBranchFactory;
    this.projectControl = p;
    this.user = user;
  }

  @Override
  public void onGitReferenceUpdated(final Event event) {
    if (isRelevantEvent(event)) {
      Project.NameKey nameKey = new Project.NameKey(event.getProjectName());
      try {
        createBackupBranch(event,
            new ProjectResource(projectControl.controlFor(nameKey, user)));
      } catch (NoSuchProjectException | IOException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Create a backup branch for the given ref.
   *
   * @param event the Event
   */
  private void createBackupBranch(Event event, ProjectResource project) {
    String branchName = event.getRefName();
    String backupRef = BackupBranch.get(branchName);

    // No-op if the backup branch name is same as the original
    if (backupRef.equals(branchName)) {
      return;
    }

    CreateBranch.Input input = new CreateBranch.Input();
    input.ref = backupRef;
    input.revision = event.getOldObjectId();

    try {
      createBranchFactory.create(backupRef).apply(project, input);
    } catch (BadRequestException | AuthException | ResourceConflictException
        | IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Is the event relevant?
   *
   * @param event the Event
   * @return True if relevant, otherwise False.
   */
  private boolean isRelevantEvent(Event event) {
    return ((event.isDelete() || event.isNonFastForward()) &&
            !event.isCreate() &&
            (event.getRefName().startsWith(R_HEADS) ||
             event.getRefName().startsWith(R_TAGS)));
  }
}

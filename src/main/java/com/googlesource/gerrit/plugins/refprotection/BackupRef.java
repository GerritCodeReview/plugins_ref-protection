/*
 *  The MIT License
 *
 *  Copyright 2015 Sony Mobile Communications AB. All rights reserved.
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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener.Event;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.CreateBranch;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REFS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupRef {
  public static final String R_BACKUPS = R_REFS + "backups/";
  private static final Logger log =
      LoggerFactory.getLogger(BackupRef.class);
  private final CreateBranch.Factory createBranchFactory;
  @Inject private static PluginConfigFactory cfg;
  @Inject private static GitRepositoryManager repoManager;
  @Inject private static String pluginName;


  @Inject
  BackupRef(CreateBranch.Factory createBranchFactory) {
    this.createBranchFactory = createBranchFactory;
  }

  public void createBackup(Event event, ProjectResource project) {
    String refName = event.getRefName();
    String backupRef = get(project, refName);

    // No-op if the backup branch name is same as the original
    if (backupRef.equals(refName)) {
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

  static String get(ProjectResource project, String refName) {
    if (cfg.getFromGerritConfig(pluginName).getBoolean("useTimestamp", true)) {
      return getTimestampBranch(refName);
    }
    else {
      return getSequentialBranch(project, refName);
    }
  }

  private static String getTimestampBranch(String refName) {
    if (refName.startsWith(R_HEADS) || refName.startsWith(R_TAGS)) {
      return String.format("%s-%s",
          R_BACKUPS + refName.replaceFirst(R_REFS, ""),
          new SimpleDateFormat("YYYYMMdd-HHmmss").format(new Date()));
    }

    return refName;
  }

  private static String getSequentialBranch(ProjectResource project, String branchName) {
    Integer rev = 1;
    String deletedName = branchName.replaceFirst(R_REFS, "");
    try (Repository git = repoManager.openRepository(project.getNameKey())) {
      for (Ref ref : git.getAllRefs().values()) {
        String name = ref.getName();
        if (name.startsWith(R_BACKUPS + deletedName + "/")) {
          Integer thisNum =
              Integer.parseInt(name.substring(name.lastIndexOf('/') + 1));
          if (thisNum >= rev) {
            rev = thisNum + 1;
          }
        }
      }
    } catch (RepositoryNotFoundException e) {
      log.error("Repository does not exist", e);
    } catch (IOException e) {
      log.error("Could not determine latest revision of deleted branch", e);
    }

    return R_BACKUPS + deletedName + "/" + rev;
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.gradle.plugins.gitgrep;

import java.util.List;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;

/**
 * Uses {@code git-grep(1)} to find text files matching a list of patterns.
 * <p>
 * It will use all cores, respect .gitignore, as expected
 */
public class GitGrepPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    if (project != project.getRootProject()) {
      throw new GradleException("This plugin can be applied to the root project only.");
    }

    var tasks = project.getTasks();

    var invalidPatterns = List.of(
      "[\u062F-\u073B]",
      "\u202A-\u202E\u2066-\u2069" // misuse of RTL/LTR (https://trojansource.codes)
    );

    var applyGitGrepRulesTask =
        tasks.register(
            "applyGitGrepRules",
            Exec.class,
            (task) -> {
              task.setExecutable(Os.isFamily(Os.FAMILY_WINDOWS) ? "git.exe" : "git");
              task.setWorkingDir(project.getLayout().getProjectDirectory());
              task.setIgnoreExitValue(true);
              task.setArgs(
                  List.of(
                      "--no-pager",
                      "grep",
                      "--untracked",       // also check untracked files
                      "--line-number",     // add line numbers to output
                      "--column",          // add col numbers to output
                      "-I",                // don't search binary files
                      "中"));


                      //"-f",                // read patterns from file
                      //"gradle/validation/git-grep/patterns.txt",
                      //"--",                // match all files except patterns file itself
                      //":^gradle/validation/git-grep/patterns.txt"));
              task.doLast(_ -> {
                // only exit status of 1 (no matches) is acceptable
                int exitStatus = task.getExecutionResult().get().getExitValue();
                if (exitStatus != 1) {
                  throw new GradleException("git-grep failed");
                }
              });
            });

    tasks
        .matching(task -> task.getName().equals("check"))
        .configureEach(
            task -> {
              task.dependsOn(applyGitGrepRulesTask);
            });
  }
}

/**
 * Copyright 2015 Bryan Kelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.btkelly.gnag.tasks;

import com.btkelly.gnag.models.ViolationComment;
import com.btkelly.gnag.utils.Logger;
import com.github.rjeschke.txtmark.Processor;
import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bobbake4 on 12/1/15.
 */
public class CheckLocalTask extends BaseCheckTask {

    private static final String TASK_NAME = "checkLocal";
    private static final String REPORT_FILE_NAME = "gnag.html";

    public static void addTask(Project project) {
        Map<String, Object> taskOptions = new HashMap<>();

        taskOptions.put(Task.TASK_NAME, TASK_NAME);
        taskOptions.put(Task.TASK_TYPE, CheckLocalTask.class);
        taskOptions.put(Task.TASK_GROUP, "Verification");
        taskOptions.put(Task.TASK_DEPENDS_ON, "check");
        taskOptions.put(Task.TASK_DESCRIPTION, "Runs Gnag locally and generates a report without attempting to publish to Github");

        project.task(taskOptions, TASK_NAME);
    }

    @TaskAction
    public void taskAction() {

        ViolationComment violationComment = buildViolationComment();

        File gnagReportDirectory = new File(getProject().getProjectDir(), "/build/outputs/gnag/");
        File gnagReportFile = new File(gnagReportDirectory, REPORT_FILE_NAME);

        try {
            gnagReportDirectory.mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(gnagReportFile);

            String htmlViolationReport = Processor.process(violationComment.getCommentMessage());

            IOUtils.write(htmlViolationReport, fileOutputStream);

            fileOutputStream.close();

        } catch (IOException ignored) {
            Logger.logE("Error saving Gnag report");
        }

        if (failBuildOnError() && violationComment.isFailBuild()) {
            String errorMessage = "Gnag check found failures, report at " + gnagReportFile.toURI();
            Logger.logE(errorMessage);
            throw new GradleException(errorMessage);
        } else if (violationComment.isFailBuild()) {
            Logger.logE("Gnag check failed but configuration allows build success");
        }
    }
}

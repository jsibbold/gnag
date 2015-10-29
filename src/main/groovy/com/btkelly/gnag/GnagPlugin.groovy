package com.btkelly.gnag

import com.btkelly.gnag.reporters.CommentReporter
import com.btkelly.gnag.reporters.PMDReporter
import org.gradle.StartParameter
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

/**
 * Created by bobbake4 on 10/23/15.
 */
class GnagPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        Gradle gradle = project.getGradle();

        StartParameter startParameter = gradle.getStartParameter();

        if (startParameter.isOffline()) {
            println "Build is running offline. Reports will not be collected";
        } else {
            addCheckAndReportTask(project);
        }
    }

    private static void addCheckAndReportTask(Project project) {

        println "Loading reporters..."

        List<CommentReporter> reporters = new ArrayList<>();

        //TODO load all reporters from config
        PMDReporter pmdReporter = new PMDReporter();
        reporters.add(pmdReporter);
        println "Loaded " + pmdReporter.reporterName();

        println "Finished loading reporters..."

        project.task("checkAndReport").dependsOn('check') << {

            println "Collecting violation reports";

            boolean failBuild = false;

            StringBuilder commentBuilder = new StringBuilder();

            for (CommentReporter githubCommentReporter : reporters) {
                commentBuilder.append(githubCommentReporter.textToAppendComment(project));

                if (githubCommentReporter.shouldFailBuild(project)) {
                    failBuild = true;
                }
            }

            String commentBody = "{ \"body\" : \"" + commentBuilder.toString() + "\" }";

            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://api.github.com/repos/stkent/amplify/issues/11/comments").openConnection();
            httpURLConnection.setRequestProperty("Authorization", "token 77d3c796ad3f612b188bb4bbbfe08390ba0e28b6");
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            println "Sending violation reports";

            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(commentBody);
            dataOutputStream.flush();
            dataOutputStream.close();

            int statusCode = httpURLConnection.getResponseCode();

            if (statusCode >= 200 && statusCode < 300) {
                println "Violation reports sent";
            } else {
                println "Error sending violation reports, status code: " + statusCode + " " + httpURLConnection.getResponseMessage();
            }

            if (failBuild) {
                throw new GradleException("One or more comment reporters has forced the build to fail");
            }
        }
    }
}
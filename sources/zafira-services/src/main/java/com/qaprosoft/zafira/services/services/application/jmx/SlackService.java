/*******************************************************************************
 * Copyright 2013-2018 QaProSoft (http://www.qaprosoft.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.qaprosoft.zafira.services.services.application.jmx;

import static com.qaprosoft.zafira.models.db.Setting.SettingType.SLACK_WEB_HOOK_URL;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import com.qaprosoft.zafira.services.util.URLResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.qaprosoft.zafira.models.db.Setting;
import com.qaprosoft.zafira.models.db.TestRun;
import com.qaprosoft.zafira.services.exceptions.ServiceException;
import com.qaprosoft.zafira.services.services.application.SettingsService;
import com.qaprosoft.zafira.services.services.application.emails.TestRunResultsEmail;
import com.qaprosoft.zafira.services.services.application.jmx.context.SlackContext;

import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackAttachment;
import in.ashwanthkumar.slack.webhook.SlackAttachment.Field;
import in.ashwanthkumar.slack.webhook.SlackMessage;

@ManagedResource(objectName = "bean:name=slackService", description = "Slack init Managed Bean", currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200, persistLocation = "foo", persistName = "bar")
public class SlackService implements IJMXService<SlackContext> {

    private final static String RESULTS_PATTERN = "Passed: %d, Failed: %d, Known Issues: %d, Skipped: %d";
    private final static String INFO_PATTERN = "%1$s\n<%2$s|Open in Zafira>  |  <%3$s|Open in Jenkins>";
    private final static String ON_FINISH_PATTERN = "Test run #%1$d has been completed after %2$s with status %3$s\n";
    private final static String REVIEWED_PATTERN = "Test run #%1$d has been reviewed. Status: %2$s\n";

    private static final Logger LOGGER = Logger.getLogger(SlackService.class);

    @Value("${zafira.slack.image}")
    private String image;

    @Value("${zafira.slack.author}")
    private String author;

    @Autowired
    private URLResolver urlResolver;

    @Autowired
    private JenkinsService jenkinsService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private CryptoService cryptoService;

    @Override
    public void init() {
        try {
            init(author, image);
        } catch (Exception e) {
            LOGGER.error("Setting does not exist", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            if (getSlack() != null) {
                getSlack().push(new SlackMessage(StringUtils.EMPTY));
            } else {
                return false;
            }
        } catch (IOException e) {
            if (((HttpResponseException) e).getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                return false;
            }
        }
        return true;
    }

    @ManagedOperation(description = "Change Slack initialization")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "author", description = "Slack author"),
            @ManagedOperationParameter(name = "picPath", description = "Slack pi path") })
    public void init(String author, String picPath) throws ServiceException {
        String wH = getWebhook();
        if (wH != null) {
            try {
                putContext(Setting.Tool.SLACK, new SlackContext(wH, author, picPath));
            } catch (IllegalArgumentException e) {
                LOGGER.info("Webhook url is not provided");
            }
        }
    }

    public void sendStatusOnFinish(TestRun testRun) {
        String onFinishMessage = String.format(ON_FINISH_PATTERN, testRun.getId(), countElapsedInSMH(testRun.getElapsed()), TestRunResultsEmail.buildStatusText(testRun));
        sendNotification(testRun, onFinishMessage);
    }

    public void sendStatusReviewed(TestRun testRun) {
        String reviewedMessage = String.format(REVIEWED_PATTERN, testRun.getId(), TestRunResultsEmail.buildStatusText(testRun));
        sendNotification(testRun, reviewedMessage);
    }

    private void sendNotification(TestRun tr, String customizedMessage) {
        String channels = tr.getSlackChannels();
        if (StringUtils.isNotEmpty(channels)) {
            String zafiraUrl = urlResolver.buildWebURL() + "/#!/tests/runs/" + tr.getId();
            String jenkinsUrl = tr.getJob().getJobURL() + "/" + tr.getBuildNumber();
            String attachmentColor = determineColor(tr);
            String mainMessage = customizedMessage + String.format(INFO_PATTERN, buildRunInfo(tr), zafiraUrl, jenkinsUrl);
            String resultsMessage = String.format(RESULTS_PATTERN, tr.getPassed(), tr.getFailed(), tr.getFailedAsKnown(), tr.getSkipped());
            SlackAttachment attachment = generateSlackAttachment(mainMessage, resultsMessage, attachmentColor, tr.getComments());
            Arrays.stream(channels.split(",")).forEach(channel -> {
                try {
                    getContext(Setting.Tool.SLACK).setSlack(getSlack().sendToChannel(channel));
                    getSlack().push(attachment);
                } catch (IOException e) {
                    LOGGER.error("Unable to push Slack notification");
                }
            });
        }
    }

    private SlackAttachment generateSlackAttachment(String mainMessage, String messageResults, String attachmentColor, String comments) {
        SlackAttachment slackAttachment = new SlackAttachment("");
        slackAttachment
                .preText(mainMessage)
                .color(attachmentColor)
                .addField(new Field("Test Results", messageResults, false))
                .fallback(mainMessage + "\n" + messageResults);
        if (comments != null) {
            slackAttachment.addField(new Field("Comments", comments, false));
        }
        return slackAttachment;
    }


    public String getWebhook() {
        String wH = null;
        Setting slackWebHookURL = settingsService.getSettingByType(SLACK_WEB_HOOK_URL);
        if (slackWebHookURL != null) {
            if (slackWebHookURL.isEncrypted()) {
                try {
                    slackWebHookURL.setValue(cryptoService.decrypt(slackWebHookURL.getValue()));
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
            wH = slackWebHookURL.getValue();
        }
        if (wH != null && !StringUtils.isEmpty(wH)) {
            return wH;
        }
        return wH;
    }

    private String buildRunInfo(TestRun tr) {
        StringBuilder sbInfo = new StringBuilder();
        sbInfo.append(tr.getProject().getName());
        Map<String, String> jenkinsParams = jenkinsService.getBuildParametersMap(tr.getJob(), tr.getBuildNumber());
        if (jenkinsParams != null && jenkinsParams.get("groups") != null) {
            sbInfo.append("(");
            sbInfo.append(jenkinsParams.get("groups"));
            sbInfo.append(")");
        }
        sbInfo.append(" | ");
        sbInfo.append(tr.getTestSuite().getName());
        sbInfo.append(" | ");
        sbInfo.append(tr.getEnv());
        sbInfo.append(" | ");
        sbInfo.append(tr.getPlatform() == null ? "no_platform" : tr.getPlatform());
        if (tr.getAppVersion() != null) {
            sbInfo.append(" | ");
            sbInfo.append(tr.getAppVersion());
        }
        return sbInfo.toString();
    }

    private String countElapsedInSMH(Integer elapsed) {
        if (elapsed != null) {
            int s = elapsed % 60;
            int m = (elapsed / 60) % 60;
            int h = (elapsed / (60 * 60)) % 24;
            StringBuilder sb = new StringBuilder(String.format("%02d sec", s));
            if (m > 0)
                sb.insert(0, String.format("%02d min ", m));
            if (h > 0)
                sb.insert(0, String.format("%02d h ", h));
            return sb.toString();
        }
        return null;
    }

    private String determineColor(TestRun tr) {
        if (tr.getPassed() > 0 && tr.getFailed() == 0 && tr.getSkipped() == 0) {
            return "good";
        }
        if (tr.getPassed() == 0 && tr.getFailed() == 0 && tr.getFailedAsKnown() == 0
                && tr.getSkipped() == 0) {
            return "danger";
        }
        return "warning";
    }

    @ManagedAttribute(description = "Get Slack current instance")
    public Slack getSlack() {
        return getContext(Setting.Tool.SLACK) != null ? getContext(Setting.Tool.SLACK).getSlack() : null;
    }
}

package uk.gov.hmcts.reform.fact.data.api.clients;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.data.api.config.SlackProperties;

import java.io.IOException;

@Slf4j
@Component
public class SlackClient {

    private final SlackProperties properties;

    public SlackClient(SlackProperties properties) {
        this.properties = properties;
    }

    public void sendSlackMessage(String message) {
        try {
            ChatPostMessageResponse resp = Slack.getInstance()
                .methods(properties.getToken())
                .chatPostMessage(r -> r
                    .channel(properties.getChannelId())
                    .text(message)
                    .linkNames(true)
                );
            if (!resp.isOk()) {
                throw new IllegalStateException(
                    "Slack API error: " + resp.getError()
                );
            }
        } catch (IOException | SlackApiException ex) {
            log.error("Exception occurred while calling Slack API: {}", ex.getMessage());
        }
    }
}

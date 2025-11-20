package uk.gov.hmcts.reform.fact.data.api.clients;

import com.slack.api.RequestConfigurator;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import uk.gov.hmcts.reform.fact.data.api.config.SlackProperties;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SlackClientTest {

    @Test
    void sendSlackMessageShouldEnableLinkNames() throws Exception {
        SlackProperties properties = new SlackProperties();
        properties.setToken("slack-token");
        properties.setChannelId("channel-id");

        Slack slack = mock(Slack.class);
        MethodsClient methodsClient = mock(MethodsClient.class);

        AtomicReference<ChatPostMessageRequest> capturedRequest = new AtomicReference<>();

        when(slack.methods("slack-token")).thenReturn(methodsClient);
        doAnswer(invocation -> {
            ChatPostMessageRequest.ChatPostMessageRequestBuilder builder = ChatPostMessageRequest.builder();
            @SuppressWarnings("unchecked")
            RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> configurator =
                invocation.getArgument(0);
            configurator.configure(builder);
            capturedRequest.set(builder.build());
            ChatPostMessageResponse response = new ChatPostMessageResponse();
            response.setOk(true);
            return response;
        }).when(methodsClient).chatPostMessage(
            ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()
        );

        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);

            SlackClient slackClient = new SlackClient(properties);
            slackClient.sendSlackMessage("Test message");
        }

        ChatPostMessageRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.getChannel()).isEqualTo("channel-id");
        assertThat(request.getText()).isEqualTo("Test message");
        java.lang.reflect.Field linkNamesField = ChatPostMessageRequest.class.getDeclaredField("linkNames");
        linkNamesField.setAccessible(true);
        Object linkNamesValue = linkNamesField.get(request);
        assertThat(linkNamesValue).isEqualTo(Boolean.TRUE);
    }
}

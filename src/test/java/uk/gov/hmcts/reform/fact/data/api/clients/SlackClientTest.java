package uk.gov.hmcts.reform.fact.data.api.clients;

import com.slack.api.RequestConfigurator;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import uk.gov.hmcts.reform.fact.data.api.config.SlackProperties;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SlackClientTest {

    @Test
    void sendSlackMessageShouldEnableLinkNames() throws Exception {
        SlackProperties properties = slackProperties();
        Slack slack = mock(Slack.class);
        MethodsClient methodsClient = mock(MethodsClient.class);

        AtomicReference<ChatPostMessageRequest> capturedRequest = new AtomicReference<>();

        when(slack.methods(properties.getToken())).thenReturn(methodsClient);
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
        assertThat(request.getChannel()).isEqualTo(properties.getChannelId());
        assertThat(request.getText()).isEqualTo("Test message");
        java.lang.reflect.Field linkNamesField = ChatPostMessageRequest.class.getDeclaredField("linkNames");
        linkNamesField.setAccessible(true);
        Object linkNamesValue = linkNamesField.get(request);
        assertThat(linkNamesValue).isEqualTo(Boolean.TRUE);
    }

    @Test
    void sendSlackMessageShouldThrowWhenSlackResponseNotOk() throws Exception {
        SlackProperties properties = slackProperties();
        Slack slack = mock(Slack.class);
        MethodsClient methodsClient = mock(MethodsClient.class);
        when(slack.methods(properties.getToken())).thenReturn(methodsClient);

        ChatPostMessageResponse response = new ChatPostMessageResponse();
        response.setOk(false);
        response.setError("invalid_auth");

        when(methodsClient.chatPostMessage(
            ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()
        )).thenReturn(response);

        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);

            SlackClient slackClient = new SlackClient(properties);

            assertThatThrownBy(() -> slackClient.sendSlackMessage("message"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid_auth");
        }
    }

    @Test
    void sendSlackMessageShouldHandleSlackApiException() throws Exception {
        SlackProperties properties = slackProperties();
        Slack slack = mock(Slack.class);
        MethodsClient methodsClient = mock(MethodsClient.class);
        when(slack.methods(properties.getToken())).thenReturn(methodsClient);

        SlackApiException exception = mock(SlackApiException.class);
        when(methodsClient.chatPostMessage(
            ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()
        )).thenThrow(exception);

        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);

            SlackClient slackClient = new SlackClient(properties);

            assertThatCode(() -> slackClient.sendSlackMessage("message"))
                .doesNotThrowAnyException();
        }
    }

    private SlackProperties slackProperties() {
        SlackProperties properties = new SlackProperties();
        properties.setToken("slack-token");
        properties.setChannelId("channel-id");
        return properties;
    }
}

package io.finstream.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class McpServerStartupTest {
    @Autowired ToolCallbackProvider tools;

    @Test
    void applicationContextRegistersAllReadOnlyMcpTools() {
        assertThat(tools.getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder(
                        "get_market_state",
                        "get_funding_rate_state",
                        "get_open_interest_state",
                        "get_recent_events",
                        "get_event_detail",
                        "get_abnormal_events");
    }
}

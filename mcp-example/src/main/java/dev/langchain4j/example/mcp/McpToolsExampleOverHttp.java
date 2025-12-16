package dev.langchain4j.example.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

import java.time.Duration;
import java.util.List;

/**
 * 通过HTTP的MCP-TOOLS示例
 */
public class McpToolsExampleOverHttp {

    /**
     * 此示例使用 `server-everything` MCP 服务器，展示了 MCP 协议的某些方面。
     * 特别是，我们使用其“add”工具来将两个数字相加。
     * This example uses the `server-everything` MCP server that showcases some aspects of the MCP protocol.
     * In particular, we use its 'add' tool that adds two numbers.
     * <p>
     * 在运行此示例之前，您需要在本地主机的 3001 端口以 SSE 模式启动 `everything` 服务器。
     * 请查看 https://github.com/modelcontextprotocol/servers/tree/main/src/everything 并运行 `npm install` 和 `node dist/sse.js`。
     * Before running this example, you need to start the `everything` server in SSE mode on localhost:3001.
     * Check out https://github.com/modelcontextprotocol/servers/tree/main/src/everything
     * and run `npm install` and `node dist/sse.js`.
     * <p>
     * 当然，可以随意将服务器换成任何其他MCP服务器。
     * Of course, feel free to swap out the server with any other MCP server.
     * <p>
     * 运行示例并检查日志以验证模型是否使用了该工具。
     * Run the example and check the logs to verify that the model used the tool.
     */
    public static void main(String[] args) throws Exception {

        // 聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();

        // MCP传输
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        // MCP客户端
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        // 工具提供者
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // 构建机器人
        // AI服务
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();
        try {
            // 聊天响应
            String response = bot.chat("What is 5+12? Use the provided tool to answer " +
                    "and always assume that the tool is correct.");
            System.out.println(response);
        } finally {
            mcpClient.close();
        }
    }
}

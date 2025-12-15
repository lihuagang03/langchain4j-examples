import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 带工具的AI服务示例
 */
public class _10_ServiceWithToolsExample {

    // Please also check CustomerSupportApplication and CustomerSupportApplicationTest
    // from spring-boot-example module

    /**
     * 计算器
     * 函数调用
     */
    static class Calculator {

        @Tool("Calculates the length of a string")
        int stringLength(String s) {
            System.out.println("Called stringLength() with s='" + s + "'");
            return s.length();
        }

        @Tool("Calculates the sum of two numbers")
        int add(int a, int b) {
            System.out.println("Called add() with a=" + a + ", b=" + b);
            return a + b;
        }

        @Tool("Calculates the square root of a number")
        double sqrt(int x) {
            System.out.println("Called sqrt() with x=" + x);
            return Math.sqrt(x);
        }
    }

    /**
     * 助手
     */
    interface Assistant {

        /**
         * 聊天
         * @param userMessage 用户消息
         */
        String chat(String userMessage);
    }

    public static void main(String[] args) {

        // 聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY) // WARNING! Tools are not supported with "demo" API key
                .modelName(GPT_4_O_MINI)
                // Structured Outputs for Tools
                .strictTools(true) // https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-tools
                .build();

        // 创建助手
        // AI服务类
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                // 配置大型语言模型可以使用的工具
                .tools(new Calculator())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 问题
        String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";

        // 聊天-回答
        String answer = assistant.chat(question);

        System.out.println(answer);
        // The square root of the sum of the number of letters in the words "hello" and "world" is approximately 3.162.
    }
}

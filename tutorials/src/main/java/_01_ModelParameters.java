import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;

/**
 * 模型参数
 */
public class _01_ModelParameters {

    public static void main(String[] args) {

        // OpenAI parameters are explained here: https://platform.openai.com/docs/api-reference/chat/create

        // 聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .temperature(0.3)
                .timeout(ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        // 提示词，用户指令
        String prompt = "Explain in three lines how to make a beautiful painting";

        // 聊天
        String response = model.chat(prompt);

        System.out.println(response);
    }
}

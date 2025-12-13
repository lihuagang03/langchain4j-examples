import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class _00_HelloWorld {

    public static void main(String[] args) {

        // 聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 聊天
        String answer = model.chat("Say Hello World");

        System.out.println(answer);
    }
}

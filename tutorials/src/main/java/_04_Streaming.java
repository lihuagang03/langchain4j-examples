import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class _04_Streaming {

    public static void main(String[] args) {

        // 流式聊天模型
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 提示词
        String prompt = "Write a short funny poem about developers and null-pointers, 10 lines maximum";

        System.out.println("Nr of chars: " + prompt.length());
        // 词元计数估算器
        System.out.println("Nr of tokens: " + new OpenAiTokenCountEstimator(GPT_4_O_MINI).estimateTokenCountInText(prompt));

        // 异步的聊天响应
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 流式聊天
        // 流式聊天响应处理器
        model.chat(prompt, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n\nDone streaming");
                // 聊天响应
                futureChatResponse.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                // 错误发生
                futureChatResponse.completeExceptionally(error);
            }
        });

        // 完成时返回结果值，如果异常完成则抛出（未检查的）异常。
        futureChatResponse.join();
    }
}

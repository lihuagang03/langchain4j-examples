import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 聊天记忆
 */
public class _05_Memory {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 流式聊天模型
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 聊天记忆
        // 词元窗口聊天记忆
        ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(1000,
                new OpenAiTokenCountEstimator(GPT_4_O_MINI));

        // 系统消息
        SystemMessage systemMessage = SystemMessage.from(
                "You are a senior developer explaining to another senior developer, "
                        + "the project you are working on is an e-commerce platform with Java back-end, " +
                        "Oracle database, and Spring Data JPA");
        chatMemory.add(systemMessage);

        // 用户消息
        UserMessage userMessage1 = userMessage(
                "How do I optimize database queries for a large-scale e-commerce platform? "
                        + "Answer short in three to five lines maximum.");
        chatMemory.add(userMessage1);

        System.out.println("[User]: " + userMessage1.singleText());
        System.out.print("[LLM]: ");

        // 流式聊天
        // AI消息
        AiMessage aiMessage1 = streamChat(model, chatMemory);
        chatMemory.add(aiMessage1);

        // 用户消息
        UserMessage userMessage2 = userMessage(
                "Give a concrete example implementation of the first point? " +
                        "Be short, 10 lines of code maximum.");
        chatMemory.add(userMessage2);

        System.out.println("\n\n[User]: " + userMessage2.singleText());
        System.out.print("[LLM]: ");

        // 流式聊天
        // AI消息
        AiMessage aiMessage2 = streamChat(model, chatMemory);
        chatMemory.add(aiMessage2);
    }

    private static AiMessage streamChat(StreamingChatModel model, ChatMemory chatMemory)
            throws ExecutionException, InterruptedException {

        // 异步的AI消息
        CompletableFuture<AiMessage> futureAiMessage = new CompletableFuture<>();

        // 流式聊天响应处理器
        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 聊天响应的AI消息
                futureAiMessage.complete(completeResponse.aiMessage());
            }

            @Override
            public void onError(Throwable error) {
            }
        };

        // 流式聊天
        // 聊天记忆中的聊天消息列表
        model.chat(chatMemory.messages(), handler);
        return futureAiMessage.get();
    }
}

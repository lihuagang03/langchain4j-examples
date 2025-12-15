import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;

/**
 * 少量示例
 */
public class _06_FewShot {

    public static void main(String[] args) {

        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .timeout(ofSeconds(100))
                .build();

        // 少量示例历史
        List<ChatMessage> fewShotHistory = new ArrayList<>();

        // 添加正面反馈示例到历史记录中
        // Adding positive feedback example to history
        fewShotHistory.add(UserMessage.from(
                "I love the new update! The interface is very user-friendly and the new features are amazing!"));
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: Thank you very much for this great feedback! We have transmitted your message to our product development team who will surely be very happy to hear this. We hope you continue enjoying using our product."));

        // 添加负面反馈示例到历史记录中
        // Adding negative feedback example to history
        fewShotHistory.add(UserMessage
                .from("I am facing frequent crashes after the new update on my Android device."));
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - crash after update Android\nReply: We are so sorry to hear about the issues you are facing. We have reported the problem to our development team and will make sure this issue is addressed as fast as possible. We will send you an email when the fix is done, and we are always at your service for any further assistance you may need."));

        // 添加其他正面反馈示例到历史记录中
        // Adding another positive feedback example to history
        fewShotHistory.add(UserMessage
                .from("Your app has made my daily tasks so much easier! Kudos to the team!"));
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: Thank you so much for your kind words! We are thrilled to hear that our app is making your daily tasks easier. Your feedback has been shared with our team. We hope you continue to enjoy using our app!"));

        // 添加其他负面反馈示例到历史记录中
        // Adding another negative feedback example to history
        fewShotHistory.add(UserMessage
                .from("The new feature is not working as expected. It’s causing data loss."));
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - data loss by new feature\nReply:We apologize for the inconvenience caused. Your feedback is crucial to us, and we have reported this issue to our technical team. They are working on it on priority. We will keep you updated on the progress and notify you once the issue is resolved. Thank you for your patience and support."));

        // 真实的用户消息
        // Adding real user's message
        UserMessage customerComplaint = UserMessage
                .from("How can your app be so slow? Please do something about it!");
        fewShotHistory.add(customerComplaint);

        System.out.println("[User]: " + customerComplaint.singleText());
        System.out.print("[LLM]: ");

        // 异步的聊天响应
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 流式聊天
        // 流式聊天响应处理器
        model.chat(fewShotHistory, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
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

        // 提取回复并发送给客户
        // 在后端执行必要的操作
        // Extract reply and send to customer
        // Perform necessary action in back-end
    }
}

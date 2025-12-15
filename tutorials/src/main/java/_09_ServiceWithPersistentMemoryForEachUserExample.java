import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static org.mapdb.Serializer.INTEGER;
import static org.mapdb.Serializer.STRING;

/**
 * 为每个用户提供持久内存的AI服务示例
 */
public class _09_ServiceWithPersistentMemoryForEachUserExample {

    /**
     * 助手
     */
    interface Assistant {

        /**
         * 聊天
         * @param memoryId 聊天记忆ID
         * @param userMessage 用户消息
         */
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    public static void main(String[] args) {

        // 聊天记忆存储
        // 持久聊天记忆存储
        PersistentChatMemoryStore store = new PersistentChatMemoryStore();
        // InMemoryChatMemoryStore
        // RedisChatMemoryStore

        // 聊天记忆提供者
        // 消息窗口聊天记录
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();

        // 聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建助手
        // AI服务类
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(chatMemoryProvider)
                .build();

        // 聊天
        System.out.println(assistant.chat(1, "Hello, my name is Klaus"));
        System.out.println(assistant.chat(2, "Hi, my name is Francine"));

        // 现在，把上面的两行注释掉，取消下面两行的注释，然后再次运行。
        // Now, comment out the two lines above, uncomment the two lines below, and run again.

        // 聊天
        // System.out.println(assistant.chat(1, "What is my name?"));
        // System.out.println(assistant.chat(2, "What is my name?"));
    }

    /**
     * 持久聊天记忆存储
     */
    // 您可以创建自己的 ChatMemoryStore 实现，并在任何您想要的时候存储聊天记录
    // You can create your own implementation of ChatMemoryStore and store chat memory whenever you'd like
    static class PersistentChatMemoryStore implements ChatMemoryStore {

        // 多个用户的聊天记忆的数据库
        private final DB db = DBMaker.fileDB("multi-user-chat-memory.db").transactionEnable().make();
        // 映射表
        private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            String json = map.get((int) memoryId);
            // 聊天消息反序列化器
            // ChatMessageDeserializer
            return messagesFromJson(json);
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // 聊天消息序列化器
            // ChatMessageSerializer
            String json = messagesToJson(messages);
            map.put((int) memoryId, json);
            db.commit();
        }

        @Override
        public void deleteMessages(Object memoryId) {
            map.remove((int) memoryId);
            db.commit();
        }
    }
}

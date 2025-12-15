package _1_easy;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 简单的RAG示例
 */
public class Easy_RAG_Example {

    /**
     * 聊天模型
     */
    private static final ChatModel CHAT_MODEL = OpenAiChatModel.builder()
            .apiKey(OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .build();

    /**
     * 这个例子演示了如何实现一个“Easy RAG”（检索增强生成）应用程序。
     * 所谓“Easy”，是指我们不会深入探讨解析、拆分、嵌入等所有细节。
     * 所有的“魔法”都隐藏在“langchain4j-easy-rag”模块中。
     * This example demonstrates how to implement an "Easy RAG" (Retrieval-Augmented Generation) application.
     * By "easy" we mean that we won't dive into all the details about parsing, splitting, embedding, etc.
     * All the "magic" is hidden inside the "langchain4j-easy-rag" module.
     * <p>
     * 如果你想学习如何在没有“Easy RAG”这种“魔法”的情况下进行 RAG，请参见 Naive_RAG_Example。
     * If you want to learn how to do RAG without the "magic" of an "Easy RAG", see {@link Naive_RAG_Example}.
     */
    public static void main(String[] args) {

        // 首先，让我们加载我们想要用于 RAG 的文档
        // First, let's load documents that we want to use for RAG
        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));

        // 其次，让我们创建一个可以访问我们文档的助手
        // Second, let's create an assistant that will have access to our documents
        // 构建助手
        // AI 服务类
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(CHAT_MODEL) // it should use OpenAI LLM
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // it should remember 10 latest messages
                // 内容检索器
                .contentRetriever(createContentRetriever(documents)) // it should have access to our documents
                .build();

        // 最后，让我们开始与助手的对话。我们可以提出如下问题：
        // Lastly, let's start the conversation with the assistant. We can ask questions like:
        // - Can I cancel my reservation?
        // - I had an accident, should I pay extra?
        startConversationWith(assistant);
    }

    /**
     * 创建内容检索器
     * @param documents 文档列表
     */
    private static ContentRetriever createContentRetriever(List<Document> documents) {

        // 在这里，我们为文档及其嵌入创建一个空的内存存储。
        // Here, we create an empty in-memory store for our documents and their embeddings.
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 在这里，我们正在将我们的文档导入存储。
        // 在底层，发生了很多“魔法”，但我们现在可以忽略它。
        // Here, we are ingesting our documents into the store.
        // Under the hood, a lot of "magic" is happening, but we can ignore it for now.
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // 最后，让我们从嵌入存储中创建一个内容检索器。
        // Lastly, let's create a content retriever from an embedding store.
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}

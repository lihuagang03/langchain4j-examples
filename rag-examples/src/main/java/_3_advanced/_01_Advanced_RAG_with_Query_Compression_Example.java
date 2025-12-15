package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 具有查询压缩的高级RAG示例
 */
public class _01_Advanced_RAG_with_Query_Compression_Example {

    /**
     * 请参考 Naive_RAG_Example 以获取基础上下文。
     * Please refer to {@link Naive_RAG_Example} for a basic context.
     * <p>
     * Advanced RAG in LangChain4j is described here: https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例展示了如何使用一种被称为“查询压缩”的技术来实现更复杂的RAG应用程序。
     * 用户的查询往往是跟进问题，会回溯之前对话的内容，但缺乏进行有效检索所需的全部细节。
     * 例如，考虑以下对话：
     * 用户：约翰·多的遗产是什么？
     * AI：约翰·多是……
     * 用户：他什么时候出生？
     * This example illustrates the implementation of a more sophisticated RAG application
     * using a technique known as "query compression".
     * Often, a query from a user is a follow-up question that refers back to earlier parts of the conversation
     * and lacks all the necessary details for effective retrieval.
     * For example, consider this conversation:
     * User: What is the legacy of John Doe?
     * AI: John Doe was a...
     * User: When was he born?
     * <p>
     * 在这种情况下，使用一个基本的RAG方法，查询像“他什么时候出生的？”可能无法找到关于约翰·多伊的文章，因为查询中没有包含“约翰·多伊”。
     * 查询压缩涉及将用户的查询和前面的对话内容结合起来，然后让大语言模型（LLM）将其“压缩”成一个独立、完整的查询。
     * LLM应该生成类似“约翰·多伊什么时候出生的？”这样的查询。
     * 该方法会增加一些延迟和成本，但会显著提升RAG过程的质量。
     * 值得注意的是，用于压缩的LLM不必与用于对话的LLM相同。
     * 例如，你可以使用一个较小的本地模型，该模型经过摘要训练。
     * In such scenarios, using a basic RAG approach with a query like "When was he born?"
     * would likely fail to find articles about John Doe, as it doesn't contain "John Doe" in the query.
     * Query compression involves taking the user's query and the preceding conversation, then asking the LLM
     * to "compress" this into a single, self-contained query.
     * The LLM should generate a query like "When was John Doe born?".
     * This method adds a bit of latency and cost but significantly enhances the quality of the RAG process.
     * It's worth noting that the LLM used for compression doesn't have to be the same as the one
     * used for conversation. For instance, you might use a smaller local model trained for summarization.
     */

    public static void main(String[] args) {

        // 创建助手
        Assistant assistant = createAssistant("documents/biography-of-john-doe.txt");

        // 首先，问“约翰·多伊的遗产是什么？”
        // 然后，问“他什么时候出生的？”
        // 现在，查看日志：
        // 第一个查询未被压缩，因为没有前置上下文可以压缩。
        // 然而，第二个查询被压缩成类似“约翰·多伊是什么时候出生？”的问题
        // First, ask "What is the legacy of John Doe?"
        // Then, ask "When was he born?"
        // Now, review the logs:
        // The first query was not compressed as there was no preceding context to compress.
        // The second query, however, was compressed into something like "When was John Doe born?"
        startConversationWith(assistant);
    }

    /**
     * 创建助手
     */
    private static Assistant createAssistant(String documentPath) {

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // 聊天模型
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 我们将创建一个压缩查询转换器，负责将用户的查询以及之前的对话压缩成一个独立的查询。
        // 这应该会显著提高检索过程的质量。
        // We will create a CompressingQueryTransformer, which is responsible for compressing
        // the user's query and the preceding conversation into a single, stand-alone query.
        // This should significantly improve the quality of the retrieval process.
        // 压缩查询转换器
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);

        // 嵌入存储的内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // RetrievalAugmentor 作为进入 LangChain4j 中 RAG 流程的入口。
        // 它可以配置以根据您的需求定制 RAG 的行为。
        // 在后续的例子中，我们将探索更多自定义内容。
        // The RetrievalAugmentor serves as the entry point into the RAG flow in LangChain4j.
        // It can be configured to customize the RAG behavior according to your requirements.
        // In subsequent examples, we will explore more customizations.
        // 检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer) // 查询转换器
                .contentRetriever(contentRetriever) // 内容检索器
                .build();

        // 构建助手
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor) // 检索增强器
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}

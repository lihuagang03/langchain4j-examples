package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 具有重新排序的高级RAG示例
 */
public class _03_Advanced_RAG_with_ReRanking_Example {

    /**
     * 请参考 Naive_RAG_Example 以获取基础上下文。
     * Please refer to {@link Naive_RAG_Example} for a basic context.
     * <p>
     * Advanced RAG in LangChain4j is described here: https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个例子展示了如何使用一种称为“重新排序”的技术来实现更高级的RAG应用。
     * This example illustrates the implementation of a more advanced RAG application
     * using a technique known as "re-ranking".
     * <p>
     * 通常，ContentRetriever 检索到的所有结果并不都与用户查询真正相关。
     * 这是因为在初始检索阶段，通常更倾向于使用更快速且成本更低的模型，特别是在处理大量数据时。
     * 这样做的权衡是检索的质量可能较低。向大语言模型提供不相关的信息可能代价高昂，最糟糕的情况下还可能导致幻觉。
     * 因此，在第二阶段，我们可以对第一阶段获得的结果进行重新排序，并使用更高级的模型（例如 Cohere Rerank）剔除不相关的结果。
     * Frequently, not all results retrieved by {@link ContentRetriever} are truly relevant to the user query.
     * This is because, during the initial retrieval stage, it is often preferable to use faster
     * and more cost-effective models, particularly when dealing with a large volume of data.
     * The trade-off is that the retrieval quality may be lower.
     * Providing irrelevant information to the LLM can be costly and, in the worst case, lead to hallucinations.
     * Therefore, in the second stage, we can perform re-ranking of the results obtained in the first stage
     * and eliminate irrelevant results using a more advanced model (e.g., Cohere Rerank).
     * <p>
     * 此示例需要 "langchain4j-cohere" 依赖项。
     * This example requires "langchain4j-cohere" dependency.
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // First, say "Hi". Observe how all segments retrieved in the first stage were filtered out.
        // Then, ask "Can I cancel my reservation?" and observe how all but one segment were filtered out.
        startConversationWith(assistant);
    }

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

        // 嵌入存储的内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5) // let's get more results
                .build();

        // 要注册并获取 Cohere 的免费 API 密钥，请访问以下链接：
        // To register and get a free API key for Cohere, please visit the following link:
        // https://dashboard.cohere.com/welcome/register
        // Cohere评分模型
        ScoringModel scoringModel = CohereScoringModel.builder()
                .apiKey(System.getenv("COHERE_API_KEY"))
                .modelName("rerank-multilingual-v3.0")
                .build();

        // 重新排序的内容聚合器
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel) // 评分模型
                .minScore(0.8) // we want to present the LLM with only the truly relevant segments for the user's query
                .build();

        // 检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever) // 内容检索器
                .contentAggregator(contentAggregator) // 内容聚合器
                .build();

        // 聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .retrievalAugmentor(retrievalAugmentor) // // 检索增强器
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}

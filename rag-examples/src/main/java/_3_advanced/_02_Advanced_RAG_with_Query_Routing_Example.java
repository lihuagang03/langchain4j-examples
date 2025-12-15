package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
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
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 具有查询路由的高级RAG示例
 */
public class _02_Advanced_RAG_with_Query_Routing_Example {

    /**
     * 请参考 Naive_RAG_Example 以获取基础上下文。
     * Please refer to {@link Naive_RAG_Example} for a basic context.
     * <p>
     * Advanced RAG in LangChain4j is described here: https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个例子展示了使用一种称为“查询路由”的技术来实现更高级的RAG应用。
     * This example showcases the implementation of a more advanced RAG application
     * using a technique known as "query routing".
     * <p>
     * 通常，私有数据分布在多个来源和格式中。
     * 这可能包括 Confluence 上的公司内部文档、你项目的 Git 仓库代码、包含用户数据的关系型数据库，或者包含你销售产品的搜索引擎等。
     * 在使用来自多个来源的数据的 RAG 流程中，你可能会拥有多个 EmbeddingStores 或 ContentRetrievers。
     * 虽然你可以将每个用户查询路由到所有可用的 ContentRetrievers，但这种方法可能效率低下，反而适得其反。
     * Often, private data is spread across multiple sources and formats.
     * This might include internal company documentation on Confluence, your project's code in a Git repository,
     * a relational database with user data, or a search engine with the products you sell, among others.
     * In a RAG flow that utilizes data from multiple sources, you will likely have multiple
     * {@link EmbeddingStore}s or {@link ContentRetriever}s.
     * While you could route each user query to all available {@link ContentRetriever}s,
     * this approach might be inefficient and counterproductive.
     * <p>
     * “查询路由”是应对这一挑战的解决方案。它涉及将查询引导到最合适的内容检索器（或多个内容检索器）。
     * 路由可以通过多种方式实现：
     * - 使用规则（例如，根据用户的权限、位置等）。
     * - 使用关键词（例如，如果查询包含词 X1、X2、X3，则路由到内容检索器 X 等）。
     * - 使用语义相似性（参见本仓库中的 EmbeddingModelTextClassifierExample）。
     * - 使用大型语言模型（LLM）来做出路由决策。
     * "Query routing" is the solution to this challenge. It involves directing a query to the most appropriate
     * {@link ContentRetriever} (or several). Routing can be implemented in various ways:
     * - Using rules (e.g., depending on the user's privileges, location, etc.).
     * - Using keywords (e.g., if a query contains words X1, X2, X3, route it to {@link ContentRetriever} X, etc.).
     * - Using semantic similarity (see EmbeddingModelTextClassifierExample in this repository).
     * - Using an LLM to make a routing decision.
     * <p>
     * 对于场景 1、2 和 3，你可以实现一个自定义的QueryRouter。
     * 对于场景 4，这个示例将演示如何使用 LanguageModelQueryRouter。
     * For scenarios 1, 2, and 3, you can implement a custom {@link QueryRouter}.
     * For scenario 4, this example will demonstrate how to use a {@link LanguageModelQueryRouter}.
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // First, ask "What is the legacy of John Doe?"
        // Then, ask "Can I cancel my reservation?"
        // Now, see the logs to observe how the queries are routed to different retrievers.
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 嵌入模型
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 让我们专门为传记创建一个独立的嵌入存储
        // Let's create a separate embedding store specifically for biographies.
        EmbeddingStore<TextSegment> biographyEmbeddingStore =
                embed(toPath("documents/biography-of-john-doe.txt"), embeddingModel);
        ContentRetriever biographyContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(biographyEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 此外，我们来创建一个专门用于使用条款的独立嵌入存储
        // Additionally, let's create a separate embedding store dedicated to terms of use.
        EmbeddingStore<TextSegment> termsOfUseEmbeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);
        ContentRetriever termsOfUseContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(termsOfUseEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 让我们创建一个查询路由器
        // Let's create a query router.
        // 内容检索器到描述的映射表
        Map<ContentRetriever, String> retrieverToDescription = new HashMap<>();
        retrieverToDescription.put(biographyContentRetriever, "biography of John Doe");
        retrieverToDescription.put(termsOfUseContentRetriever, "terms of use of car rental company");
        // 大语言模型的查询路由器
        QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);

        // 检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter) // 查询路由器
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor) // 检索增强器
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        // 文本文档解析器
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(documentPath, documentParser);

        // 递归地文档拆分器
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // 内存中的嵌入存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }
}
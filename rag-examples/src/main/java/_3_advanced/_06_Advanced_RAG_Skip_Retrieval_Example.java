package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static shared.Utils.*;

/**
 * 跳过检索的高级RAG示例
 */
public class _06_Advanced_RAG_Skip_Retrieval_Example {


    /**
     * 请参考 Naive_RAG_Example 以获取基础上下文。
     * Please refer to {@link Naive_RAG_Example} for a basic context.
     * <p>
     * Advanced RAG in LangChain4j is described here: https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例演示了如何有条件地跳过检索。有时，检索是不必要的，例如，当用户只是说“你好”时。
     * This example demonstrates how to conditionally skip retrieval.
     * Sometimes, retrieval is unnecessary, for instance, when a user simply says "Hi".
     * <p>
     * 实现这一点有多种方法，但最简单的方法是使用自定义的 QueryRouter。
     * 当检索应该被跳过时，QueryRouter 会返回一个空列表，这意味着查询不会被路由到任何 ContentRetriever。
     * There are multiple ways to implement this, but the simplest one is to use a custom {@link QueryRouter}.
     * When retrieval should be skipped, QueryRouter will return an empty list,
     * meaning that the query will not be routed to any {@link ContentRetriever}.
     * <p>
     * 决策可以通过多种方式实现：
     * - 使用规则（例如，取决于用户的权限、位置等）。
     * - 使用关键词（例如，如果查询包含特定词汇）。
     * - 使用语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
     * - 使用大型语言模型（LLM）来做出决策。
     * Decision-making can be implemented in various ways:
     * - Using rules (e.g., depending on the user's privileges, location, etc.).
     * - Using keywords (e.g., if a query contains specific words).
     * - Using semantic similarity (see EmbeddingModelTextClassifierExample in this repository).
     * - Using an LLM to make a decision.
     * <p>
     * 在这个例子中，我们将使用大型语言模型来决定用户查询是否需要进行检索。
     * In this example, we will use an LLM to decide whether a user query should do retrieval or not.
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // First, say "Hi"
        // Notice how this query is not routed to any retrievers.

        // Now, ask "Can I cancel my reservation?"
        // This query has been routed to our retriever.
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        // 内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 聊天模型
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 让我们创建一个查询路由器。
        // Let's create a query router.
        QueryRouter queryRouter = new QueryRouter() {

            /**
             * 以下查询是否与汽车租赁公司的业务相关？
             * 仅回答“是”、“否”或“可能”。
             * 查询：{{it}}
             */
            private final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
                    "Is the following query related to the business of the car rental company? " +
                            "Answer only 'yes', 'no' or 'maybe'. " +
                            "Query: {{it}}"
            );

            @Override
            public Collection<ContentRetriever> route(Query query) {

                // 提示-文本
                Prompt prompt = PROMPT_TEMPLATE.apply(query.text());
                UserMessage userMessage = prompt.toUserMessage();

                // 聊天-AI消息
                AiMessage aiMessage = chatModel.chat(userMessage).aiMessage();
                // 大型语言模型决定：
                System.out.println("LLM decided: " + aiMessage.text());

                if (aiMessage.text().toLowerCase().contains("no")) {
                    return emptyList();
                }

                return singletonList(contentRetriever);
            }
        };

        // 检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter) // 查询路由器
                .build();

        // 构建助手
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor) // 检索增强器
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(documentPath, documentParser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }
}
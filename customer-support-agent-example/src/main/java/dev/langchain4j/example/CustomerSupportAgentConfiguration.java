package dev.langchain4j.example;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 客户支持代理配置
 */
@Configuration
public class CustomerSupportAgentConfiguration {

    /**
     * 聊天记忆提供者
     * @param tokenizer 词元计数估算器
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider(
            TokenCountEstimator tokenizer
    ) {
        // memoryId = userId
        // 词元窗口聊天记忆
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(5000, tokenizer)
                .build();
    }

    /**
     * 嵌入模型
     */
    @Bean
    EmbeddingModel embeddingModel() {
        // Not the best embedding model, but good enough for this demo
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 文本片段的嵌入存储
     * @param embeddingModel 嵌入模型
     * @param resourceLoader 资源加载器
     * @param tokenizer 词元计数估算器
     */
    @Bean
    EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel,
            ResourceLoader resourceLoader,
            TokenCountEstimator tokenizer
    ) throws IOException {

        // 通常，你的数据嵌入存储已经填充了你的数据。
        // 然而，为了本次演示的目的，我们将：
        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        // 1. 创建内存中的嵌入存储
        // 1. Create an in-memory embedding store
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 2. 加载示例文档（《Miles of Smiles》使用条款）
        // 2. Load an example document ("Miles of Smiles" terms of use)
        Resource resource = resourceLoader.getResource("classpath:miles-of-smiles-terms-of-use.txt");
        Document document = loadDocument(resource.getFile().toPath(), new TextDocumentParser());

        // 3. 将文档分成每段 100 个词元
        // 4. 将片段转换为嵌入
        // 5. 将嵌入存储到嵌入存储中
        // 所有这些都可以手动完成，但我们将使用 EmbeddingStoreIngestor 来自动化处理：
        // 3. Split the document into segments 100 tokens each
        // 4. Convert segments into embeddings
        // 5. Store embeddings into embedding store
        // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
        // 文档拆分器
        // 这是一个推荐用于通用文本的 DocumentSplitter。它首先尝试将文档拆分为段落，并尽可能多地将段落放入单个 TextSegment 中。
        // 如果某些段落过长，它们会递归地拆分为行，然后是句子，再然后是单词，最后是字符，直到它们适合放入一个段落片段中。
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, tokenizer);
        // 嵌入存储摄取器
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        // 将指定的文档导入到在创建此 EmbeddingStoreIngestor 时指定的 EmbeddingStore 中。
        ingestor.ingest(document);

        return embeddingStore;
    }

    /**
     * 内容检索器
     * @param embeddingStore 文本片段的嵌入存储
     * @param embeddingModel 嵌入模型
     */
    @Bean
    ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {

        // 您需要调整这些参数以找到最佳设置，这将取决于多个因素，例如：
        // - 你的数据的性质
        // - 你正在使用的嵌入模型
        // You will need to adjust these parameters to find the optimal setting,
        // which will depend on multiple factors, for example:
        // - The nature of your data
        // - The embedding model you are using
        int maxResults = 1;
        double minScore = 0.6;

        // 嵌入存储的内容检索器
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    /**
     * 词元计数估算器
     */
    @Bean
    TokenCountEstimator tokenCountEstimator() {
        // OpenAI 词元计数估算器
        return new OpenAiTokenCountEstimator(GPT_4_O_MINI);
    }
}

package _2_naive;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 简单的RAG示例
 */
public class Naive_RAG_Example {

    /**
     * 这个示例演示了如何实现一个简单的检索增强生成（RAG）应用程序。
     * 所谓“简单”，意味着我们不会使用任何高级的RAG技术。
     * 在与大型语言模型（LLM）的每次交互中，我们将：
     * 1. 直接使用用户的查询。
     * 2. 使用嵌入模型对查询进行嵌入。
     * 3. 使用查询的嵌入在嵌入存储（包含文档的小片段）中搜索最相关的X个片段。
     * 4. 将找到的片段附加到用户的查询中。
     * 5. 将组合后的输入（用户查询 + 片段）发送给LLM。
     * 6. 希望：
     *    - 用户的查询已制定得很好，并包含检索所需的所有必要细节。
     *    - 找到的片段与用户的查询相关。
     * This example demonstrates how to implement a naive Retrieval-Augmented Generation (RAG) application.
     * By "naive", we mean that we won't use any advanced RAG techniques.
     * In each interaction with the Large Language Model (LLM), we will:
     * 1. Take the user's query as-is.
     * 2. Embed it using an embedding model.
     * 3. Use the query's embedding to search an embedding store (containing small segments of your documents)
     * for the X most relevant segments.
     * 4. Append the found segments to the user's query.
     * 5. Send the combined input (user query + segments) to the LLM.
     * 6. Hope that:
     * - The user's query is well-formulated and contains all necessary details for retrieval.
     * - The found segments are relevant to the user's query.
     */
    public static void main(String[] args) {

        // 让我们创建一个可以了解我们文档的助手
        // Let's create an assistant that will know about our document
        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // 现在，让我们开始与助手的对话。我们可以提出如下问题：
        // Now, let's start the conversation with the assistant. We can ask questions like:
        // - Can I cancel my reservation?
        // - I had an accident, should I pay extra?
        startConversationWith(assistant);
    }

    /**
     * 创建助手
     */
    private static Assistant createAssistant(String documentPath) {

        // 首先，让我们创建一个聊天模型，也称为大语言模型（LLM），它将回答我们的查询。
        // 在这个例子中，我们将使用 OpenAI 的 gpt-4o-mini，但你可以选择任何受支持的模型。
        // LangChain4j 目前支持超过 10 个流行的 LLM 提供商。
        // First, let's create a chat model, also known as a LLM, which will answer our queries.
        // In this example, we will use OpenAI's gpt-4o-mini, but you can choose any supported model.
        // LangChain4j currently supports more than 10 popular LLM providers.
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();


        // 现在，让我们加载一个我们想要用于 RAG 的文档。
        // 我们将使用一个虚构的汽车租赁公司“微笑里程”的使用条款。
        // 在这个例子中，我们只导入一个文档，但你可以根据需要导入任意数量的文档。
        // LangChain4j 提供了从各种来源加载文档的内置支持：
        // 此外，LangChain4j 支持解析多种文档类型：
        // Now, let's load a document that we want to use for RAG.
        // We will use the terms of use from an imaginary car rental company, "Miles of Smiles".
        // For this example, we'll import only a single document, but you can load as many as you need.
        // LangChain4j offers built-in support for loading documents from various sources:
        // File System, URL, Amazon S3, Azure Blob Storage, GitHub, Tencent COS.
        // Additionally, LangChain4j supports parsing multiple document types:
        // text, pdf, doc, xls, ppt.
        // However, you can also manually import your data from other sources.
        // 文本文档解析器
        DocumentParser documentParser = new TextDocumentParser();
        // 从指定的文件路径加载文档。使用指定的 DocumentParser 解析文件。
        Document document = loadDocument(toPath(documentPath), documentParser);


        // 现在，我们需要将这份文档拆分成更小的部分，也称为“块/分片/片段”。
        // 这种方法允许我们仅将与用户查询相关的部分发送给大语言模型，而不是整个文档。
        // 例如，如果用户询问取消政策，我们将识别并只发送与取消相关的内容片段。
        // 一个好的起点是使用递归文档拆分器，最初尝试按段落进行拆分。
        // 如果一个段落太大而无法放入单个片段，拆分器会递归地按换行符拆分，然后按句子拆分，最后如有必要再按单词拆分，以确保每一段文本都能适合单个片段。
        // Now, we need to split this document into smaller segments, also known as "chunks."
        // This approach allows us to send only relevant segments to the LLM in response to a user query,
        // rather than the entire document. For instance, if a user asks about cancellation policies,
        // we will identify and send only those segments related to cancellation.
        // A good starting point is to use a recursive document splitter that initially attempts
        // to split by paragraphs. If a paragraph is too large to fit into a single segment,
        // the splitter will recursively divide it by newlines, then by sentences, and finally by words,
        // if necessary, to ensure each piece of text fits into a single segment.
        // 递归的文档拆分器
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        // 将单个文档拆分成一组 TextSegment 对象。
        // 元数据通常会从文档中复制，并添加与段落相关的信息，如在文档中的位置、页码等。
        List<TextSegment> segments = splitter.split(document);


        // 现在，我们需要对这些片段进行嵌入（也称为“向量化”）。
        // 执行相似性搜索需要嵌入。
        // 在这个例子中，我们将使用本地进程内的嵌入模型，但你可以选择任何支持的模型。
        // Now, we need to embed (also known as "vectorize") these segments.
        // Embedding is needed for performing similarity searches.
        // For this example, we'll use a local in-process embedding model, but you can choose any supported model.
        // LangChain4j currently supports more than 10 popular embedding model providers.
        // 嵌入模型
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        // 嵌入一系列文本片段的文本内容。
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();


        // 接下来，我们将把这些嵌入存储在嵌入存储中（也称为“向量数据库”）。
        // 此存储将用于在每次与大型语言模型互动时搜索相关的片段。
        // 为了简便起见，这个示例使用了内存中的嵌入存储，但你可以选择任何支持的存储方式。
        // Next, we will store these embeddings in an embedding store (also known as a "vector database").
        // This store will be used to search for relevant segments during each interaction with the LLM.
        // For simplicity, this example uses an in-memory embedding store, but you can choose from any supported store.
        // Langchain4j currently supports more than 15 popular embedding stores.
        // 嵌入存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        // 将多个嵌入及其对应的已嵌入内容添加到存储中。
        embeddingStore.addAll(embeddings, segments);

        // 我们也可以使用 EmbeddingStoreIngestor 将上述手动步骤隐藏在一个更简单的 API 后面。
        // 请参阅在 _01_Advanced_RAG_with_Query_Compression_Example 中使用 EmbeddingStoreIngestor 的示例。
        // We could also use EmbeddingStoreIngestor to hide manual steps above behind a simpler API.
        // See an example of using EmbeddingStoreIngestor in _01_Advanced_RAG_with_Query_Compression_Example.


        // 内容检索器负责根据用户查询检索相关内容。
        // 目前，它能够检索文本片段，但未来的改进将包括对图像、音频等其他模态的支持。
        // The content retriever is responsible for retrieving relevant content based on a user query.
        // Currently, it is capable of retrieving text segments, but future enhancements will include support for
        // additional modalities like images, audio, and more.
        // 嵌入存储的内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
                .build();


        // 我们可以选择使用聊天记忆，使得可以与大型语言模型进行来回对话，并让它记住之前的互动内容。
        // 目前，LangChain4j 提供了两种聊天记忆实现：MessageWindowChatMemory 和 TokenWindowChatMemory。
        // Optionally, we can use a chat memory, enabling back-and-forth conversation with the LLM
        // and allowing it to remember previous interactions.
        // Currently, LangChain4j offers two chat memory implementations:
        // MessageWindowChatMemory and TokenWindowChatMemory.
        // 消息窗口的聊天记忆（聊天记录）
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);


        // 最后一步是构建我们的AI服务，并配置它以使用我们上面创建的组件。
        // The final step is to build our AI Service,
        // configuring it to use the components we've created above.
        // 构建助手
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }
}

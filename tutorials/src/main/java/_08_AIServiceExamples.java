import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

public class _08_AIServiceExamples {

    /**
     * 聊天模型
     */
    static ChatModel model = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .timeout(ofSeconds(60))
            .build();

    ////////////////// SIMPLE EXAMPLE //////////////////////
    /// 简单示例

    /**
     * 简单的AI服务示例
     */
    static class Simple_AI_Service_Example {

        /**
         * 助手
         */
        interface Assistant {

            /**
             * 聊天
             * @param message 消息
             */
            String chat(String message);
        }

        public static void main(String[] args) {

            // 创建助手
            // AI服务类
            Assistant assistant = AiServices.create(Assistant.class, model);

            // 用户消息
            String userMessage = "Translate 'Plus-Values des cessions de valeurs mobilières, de droits sociaux et gains assimilés'";

            // 聊天
            String answer = assistant.chat(userMessage);

            System.out.println(answer);
        }
    }

    ////////////////// WITH MESSAGE AND VARIABLES //////////////////////
    /// 带消息和变量

    /**
     * 带有系统消息的AI服务示例
     */
    static class AI_Service_with_System_Message_Example {

        /**
         * 厨师
         */
        interface Chef {

            /**
             * 问答
             * <pre>
             * 你是一名专业厨师。你友好、礼貌并且言简意赅。
             * </pre>
             * @param question 问题
             */
            // 系统消息
            @SystemMessage("You are a professional chef. You are friendly, polite and concise.")
            String answer(String question);
        }

        public static void main(String[] args) {

            // 创建厨师
            // AI服务类
            Chef chef = AiServices.create(Chef.class, model);

            // 问答
            String answer = chef.answer("How long should I grill chicken?");

            System.out.println(answer); // Grilling chicken usually takes around 10-15 minutes per side ...
        }
    }

    /**
     * 带系统和用户消息的AI服务示例
     */
    static class AI_Service_with_System_and_User_Messages_Example {

        /**
         * 文本工具
         */
        interface TextUtils {

            /**
             * 文本翻译，根据语言
             * <pre>
             * 你是一名专业的{{language}}翻译
             * 翻译以下文本：{{text}}
             * </pre>
             */
            // 系统消息
            // 用户消息
            @SystemMessage("You are a professional translator into {{language}}")
            @UserMessage("Translate the following text: {{text}}")
            String translate(@V("text") String text, @V("language") String language);

            /**
             * 文本总结
             * <pre>
             * 将用户的每条消息总结为{{n}}个要点。
             * 仅提供要点。
             * </pre>
             */
            // 系统消息
            @SystemMessage("Summarize every message from user in {{n}} bullet points. Provide only bullet points.")
            List<String> summarize(@UserMessage String text, @V("n") int n);
        }

        public static void main(String[] args) {

            // 创建文本工具
            // AI服务类
            TextUtils utils = AiServices.create(TextUtils.class, model);

            // 翻译文本
            String translation = utils.translate("Hello, how are you?", "italian");
            System.out.println(translation); // Ciao, come stai?

            // 文本总结
            String text = "AI, or artificial intelligence, is a branch of computer science that aims to create "
                    + "machines that mimic human intelligence. This can range from simple tasks such as recognizing "
                    + "patterns or speech to more complex tasks like making decisions or predictions.";

            // 总结文本
            List<String> bulletPoints = utils.summarize(text, 3);
            bulletPoints.forEach(System.out::println);
            // [
            // "- AI is a branch of computer science",
            // "- It aims to create machines that mimic human intelligence",
            // "- It can perform simple or complex tasks"
            // ]
        }
    }

    //////////////////// EXTRACTING DIFFERENT DATA TYPES ////////////////////
    /// 提取不同的数据类型

    /**
     * 情感提取
     */
    static class Sentiment_Extracting_AI_Service_Example {

        /**
         * 情感
         */
        enum Sentiment {
            POSITIVE, NEUTRAL, NEGATIVE
        }

        /**
         * 情感分析器
         */
        interface SentimentAnalyzer {

            /**
             * 分析情绪
             * <pre>
             * 分析{{it}}的情感
             * </pre>
             */
            // 用户消息
            @UserMessage("Analyze sentiment of {{it}}")
            Sentiment analyzeSentimentOf(String text);

            /**
             * {{it}} 有积极的情绪吗？
             */
            // 用户消息
            @UserMessage("Does {{it}} have a positive sentiment?")
            boolean isPositive(String text);
        }

        public static void main(String[] args) {

            // 创建情感分析器
            // AI服务类
            SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

            // 分析情绪
            Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("It is good!");
            System.out.println(sentiment); // POSITIVE

            boolean positive = sentimentAnalyzer.isPositive("It is bad!");
            System.out.println(positive); // false
        }
    }

    /**
     * 酒店点评
     */
    static class Hotel_Review_AI_Service_Example {

        /**
         * 问题类别
         * <pre>
         * 维护问题，服务问题，舒适度问题，
         * 设施问题，清洁问题，网络连接问题，
         * 入住问题，整体体验问题
         * </pre>
         */
        public enum IssueCategory {
            MAINTENANCE_ISSUE,
            SERVICE_ISSUE,
            COMFORT_ISSUE,
            FACILITY_ISSUE,
            CLEANLINESS_ISSUE,
            CONNECTIVITY_ISSUE,
            CHECK_IN_ISSUE,
            OVERALL_EXPERIENCE_ISSUE
        }

        /**
         * 酒店评论问题分析器
         */
        interface HotelReviewIssueAnalyzer {

            /**
             * 分析评论
             * <pre>
             * 请分析以下评论：|||{{it}}|||
             * </pre>
             */
            @UserMessage("Please analyse the following review: |||{{it}}|||")
            List<IssueCategory> analyzeReview(String review);
        }

        public static void main(String[] args) {

            // 创建酒店评论问题分析器
            // AI服务类
            HotelReviewIssueAnalyzer hotelReviewIssueAnalyzer = AiServices.create(HotelReviewIssueAnalyzer.class, model);

            // 评论
            String review = "Our stay at hotel was a mixed experience. The location was perfect, just a stone's throw away " +
                    "from the beach, which made our daily outings very convenient. The rooms were spacious and well-decorated, " +
                    "providing a comfortable and pleasant environment. However, we encountered several issues during our " +
                    "stay. The air conditioning in our room was not functioning properly, making the nights quite uncomfortable. " +
                    "Additionally, the room service was slow, and we had to call multiple times to get extra towels. Despite the " +
                    "friendly staff and enjoyable breakfast buffet, these issues significantly impacted our stay.";

            // 分析评论
            List<IssueCategory> issueCategories = hotelReviewIssueAnalyzer.analyzeReview(review);

            // Should output [MAINTENANCE_ISSUE, SERVICE_ISSUE, COMFORT_ISSUE, OVERALL_EXPERIENCE_ISSUE]
            System.out.println(issueCategories);
        }
    }

    /**
     * 数字提取
     */
    static class Number_Extracting_AI_Service_Example {

        /**
         * 数字提取器
         */
        interface NumberExtractor {

            @UserMessage("Extract number from {{it}}")
            int extractInt(String text);

            @UserMessage("Extract number from {{it}}")
            long extractLong(String text);

            @UserMessage("Extract number from {{it}}")
            BigInteger extractBigInteger(String text);

            @UserMessage("Extract number from {{it}}")
            float extractFloat(String text);

            @UserMessage("Extract number from {{it}}")
            double extractDouble(String text);

            @UserMessage("Extract number from {{it}}")
            BigDecimal extractBigDecimal(String text);
        }

        public static void main(String[] args) {

            // 创建数字提取器
            // AI服务类
            NumberExtractor extractor = AiServices.create(NumberExtractor.class, model);

            // 文本
            String text = "After countless millennia of computation, the supercomputer Deep Thought finally announced "
                    + "that the answer to the ultimate question of life, the universe, and everything was forty two.";

            int intNumber = extractor.extractInt(text);
            System.out.println(intNumber); // 42

            long longNumber = extractor.extractLong(text);
            System.out.println(longNumber); // 42

            BigInteger bigIntegerNumber = extractor.extractBigInteger(text);
            System.out.println(bigIntegerNumber); // 42

            float floatNumber = extractor.extractFloat(text);
            System.out.println(floatNumber); // 42.0

            double doubleNumber = extractor.extractDouble(text);
            System.out.println(doubleNumber); // 42.0

            BigDecimal bigDecimalNumber = extractor.extractBigDecimal(text);
            System.out.println(bigDecimalNumber); // 42.0
        }
    }

    /**
     * 日期和时间提取
     */
    static class Date_and_Time_Extracting_AI_Service_Example {

        /**
         * 日期时间提取器
         */
        interface DateTimeExtractor {

            /**
             * 提取日期
             */
            @UserMessage("Extract date from {{it}}")
            LocalDate extractDateFrom(String text);

            /**
             * 提取时间
             */
            @UserMessage("Extract time from {{it}}")
            LocalTime extractTimeFrom(String text);

            /**
             * 提取日期时间
             */
            @UserMessage("Extract date and time from {{it}}")
            LocalDateTime extractDateTimeFrom(String text);
        }

        public static void main(String[] args) {

            // 创建日期时间提取器
            // AI服务类
            DateTimeExtractor extractor = AiServices.create(DateTimeExtractor.class, model);

            // 文本
            String text = "The tranquility pervaded the evening of 1968, just fifteen minutes shy of midnight,"
                    + " following the celebrations of Independence Day.";

            LocalDate date = extractor.extractDateFrom(text);
            System.out.println(date); // 1968-07-04

            LocalTime time = extractor.extractTimeFrom(text);
            System.out.println(time); // 23:45

            LocalDateTime dateTime = extractor.extractDateTimeFrom(text);
            System.out.println(dateTime); // 1968-07-04T23:45
        }
    }

    /**
     * POJO对象提取
     */
    static class POJO_Extracting_AI_Service_Example {

        /**
         * 人物信息
         */
        static class Person {

            /**
             * 人的名字
             */
            // 类字段的描述
            @Description("first name of a person") // you can add an optional description to help an LLM have a better understanding
            private String firstName;
            private String lastName;
            private LocalDate birthDate;

            @Override
            public String toString() {
                return "Person {" +
                        " firstName = \"" + firstName + "\"" +
                        ", lastName = \"" + lastName + "\"" +
                        ", birthDate = " + birthDate +
                        " }";
            }
        }

        /**
         * 人物提取器
         */
        interface PersonExtractor {

            /**
             * 提取人物信息
             * <pre>
             * 从以下文本中提取一个人：{{it}}
             * </pre>
             */
            @UserMessage("Extract a person from the following text: {{it}}")
            Person extractPersonFrom(String text);
        }

        public static void main(String[] args) {

            // 聊天模型
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    // When extracting POJOs with the LLM that supports the "json mode" feature
                    // (e.g., OpenAI, Azure OpenAI, Vertex AI Gemini, Ollama, etc.),
                    // it is advisable to enable it (json mode) to get more reliable results.
                    // When using this feature, LLM will be forced to output a valid JSON.
                    .responseFormat("json_schema") // ResponseFormatType.JSON_SCHEMA
//                    .responseFormat(ResponseFormat.JSON)
//                    .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                    .strictJsonSchema(true) // https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-json-mode
                    .timeout(ofSeconds(60))
                    .build();

            // 创建人物提取器
            // AI服务类
            PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

            // 文本
            String text = "In 1968, amidst the fading echoes of Independence Day, "
                    + "a child named John arrived under the calm evening sky. "
                    + "This newborn, bearing the surname Doe, marked the start of a new journey.";

            // 从以下文本中提取一个人
            Person person = extractor.extractPersonFrom(text);

            System.out.println(person); // Person { firstName = "John", lastName = " ", birthDate = 1968-07-04 }
        }
    }

    ////////////////////// DESCRIPTIONS ////////////////////////
    /// 描述

    /**
     * 带描述的POJO提取
     */
    static class POJO_With_Descriptions_Extracting_AI_Service_Example {

        /**
         * 食谱
         */
        static class Recipe {

            /**
             * 短标题，最多三个字
             */
            @Description("short title, 3 words maximum")
            private String title;

            /**
             * 简短描述，最多两句
             */
            @Description("short description, 2 sentences maximum")
            private String description;

            /**
             * 每一步应描述为六到八个词，每一步应押韵
             */
            @Description("each step should be described in 6 to 8 words, steps should rhyme with each other")
            private List<String> steps;

            private Integer preparationTimeMinutes;

            @Override
            public String toString() {
                return "Recipe {" +
                        " title = \"" + title + "\"" +
                        ", description = \"" + description + "\"" +
                        ", steps = " + steps +
                        ", preparationTimeMinutes = " + preparationTimeMinutes +
                        " }";
            }
        }

        /**
         * 创建食谱提示词
         * <pre>
         * 制作一道只用食材就能准备的菜肴的食谱
         * 制作一道只用 {{ingredients}} 就能做出的 {{dish}} 食谱
         * </pre>
         */
        @StructuredPrompt("Create a recipe of a {{dish}} that can be prepared using only {{ingredients}}")
        static class CreateRecipePrompt {

            private String dish;
            private List<String> ingredients;
        }

        /**
         * 厨师
         */
        interface Chef {

            /**
             * 从食材创建食谱
             */
            Recipe createRecipeFrom(String... ingredients);

            /**
             * 创建食谱
             * @param prompt 创建食谱提示词
             */
            Recipe createRecipe(CreateRecipePrompt prompt);
        }

        public static void main(String[] args) {

            // 聊天模型
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    // When extracting POJOs with the LLM that supports the "json mode" feature
                    // (e.g., OpenAI, Azure OpenAI, Vertex AI Gemini, Ollama, etc.),
                    // it is advisable to enable it (json mode) to get more reliable results.
                    // When using this feature, LLM will be forced to output a valid JSON.
                    .responseFormat("json_schema")
                    .strictJsonSchema(true) // https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-json-mode
                    .timeout(ofSeconds(60))
                    .build();

            // 创建厨师
            // AI服务类
            Chef chef = AiServices.create(Chef.class, model);

            // 从食材创建食谱
            Recipe recipe = chef.createRecipeFrom("cucumber", "tomato", "feta", "onion", "olives", "lemon");

            System.out.println(recipe);
            // Recipe {
            // title = "Greek Salad",
            // description = "A refreshing mix of veggies and feta cheese in a zesty dressing.",
            // steps = [
            // "Chop cucumber and tomato",
            // "Add onion and olives",
            // "Crumble feta on top",
            // "Drizzle with dressing and enjoy!"
            // ],
            // preparationTimeMinutes = 10
            // }

            // 创建食谱提示词
            CreateRecipePrompt prompt = new CreateRecipePrompt();
            prompt.dish = "oven dish";
            prompt.ingredients = asList("cucumber", "tomato", "feta", "onion", "olives", "potatoes");

            // 创建食谱
            Recipe anotherRecipe = chef.createRecipe(prompt);
            System.out.println(anotherRecipe);
            // Recipe ...
        }
    }


    ////////////////////////// WITH MEMORY /////////////////////////
    /// 带聊天记忆

    /**
     * 带聊天记忆功能的服务示例
     */
    static class ServiceWithMemoryExample {

        /**
         * 助手
         */
        interface Assistant {

            /**
             * 聊天
             * @param message 消息
             */
            String chat(String message);
        }

        public static void main(String[] args) {

            // 聊天记忆
            // 消息窗口的聊天记录
            // memoryId = "default"
            // SingleSlotChatMemoryStore
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

//            ChatMemory chatMemory1 = MessageWindowChatMemory.builder()
//                    .maxMessages(10)
//                    .chatMemoryStore(
//                            RedisChatMemoryStore.builder()
//                                    .host("localhost") // 127.0.0.1
//                                    .port(6379)
//                                    .build()
//                    )
//                    .build();
            // InMemoryChatMemoryStore
            // RedisChatMemoryStore

            // 创建助手
            // AI服务类
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemory(chatMemory)
                    .build();

            // 聊天
            String answer = assistant.chat("Hello! My name is Klaus.");
            // 你好，Klaus！我今天能帮你做什么？
            System.out.println(answer); // Hello Klaus! How can I assist you today?

            String answerWithName = assistant.chat("What is my name?");
            System.out.println(answerWithName); // Your name is Klaus.
        }
    }

    /**
     * 为每个用户提供聊天记忆的服务示例
     */
    static class ServiceWithMemoryForEachUserExample {

        /**
         * 助手
         */
        interface Assistant {

            /**
             * 聊天
             * @param memoryId 聊天记忆ID-用户ID
             * @param userMessage 用户消息
             */
            String chat(@MemoryId int memoryId, @UserMessage String userMessage);
        }

        public static void main(String[] args) {

            // 创建助手
            // AI服务类
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    // 聊天记忆提供者
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();

            // 聊天
            System.out.println(assistant.chat(1, "Hello, my name is Klaus"));
            // Hi Klaus! How can I assist you today?

            // 聊天
            System.out.println(assistant.chat(2, "Hello, my name is Francine"));
            // Hello Francine! How can I assist you today?

            // 聊天
            System.out.println(assistant.chat(1, "What is my name?"));
            // Your name is Klaus.

            // 聊天
            System.out.println(assistant.chat(2, "What is my name?"));
            // Your name is Francine.
        }
    }

}

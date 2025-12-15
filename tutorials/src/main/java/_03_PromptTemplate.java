import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * 提示词模板
 */
public class _03_PromptTemplate {

    static class Simple_Prompt_Template_Example {

        public static void main(String[] args) {

            // 聊天模型
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .timeout(ofSeconds(60))
                    .build();

            // 提示词模版
            String template = "Create a recipe for a {{dishType}} with the following ingredients: {{ingredients}}";
            PromptTemplate promptTemplate = PromptTemplate.from(template);

            // 模版变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("dishType", "oven dish");
            variables.put("ingredients", "potato, tomato, feta, olive oil");

            // 模版应用提示词
            Prompt prompt = promptTemplate.apply(variables);

            // 聊天
            String response = model.chat(prompt.text());

            System.out.println(response);
        }

    }

    static class Structured_Prompt_Template_Example {
        // 结构化的提示词
        @StructuredPrompt({
                "Create a recipe of a {{dish}} that can be prepared using only {{ingredients}}.",
                "Structure your answer in the following way:",

                "Recipe name: ...",
                "Description: ...",
                "Preparation time: ...",

                "Required ingredients:",
                "- ...",
                "- ...",

                "Instructions:",
                "- ...",
                "- ..."
        })
        static class CreateRecipePrompt {

            String dish;
            List<String> ingredients;

            CreateRecipePrompt(String dish, List<String> ingredients) {
                this.dish = dish;
                this.ingredients = ingredients;
            }
        }

        public static void main(String[] args) {

            // 聊天模型
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .timeout(ofSeconds(60))
                    .build();

            // 结构化对象
            Structured_Prompt_Template_Example.CreateRecipePrompt createRecipePrompt = new Structured_Prompt_Template_Example.CreateRecipePrompt(
                    "salad",
                    asList("cucumber", "tomato", "feta", "onion", "olives")
            );

            // 结构化的提示词到提示词
            Prompt prompt = StructuredPromptProcessor.toPrompt(createRecipePrompt);

            // 聊天
            String recipe = model.chat(prompt.text());

            System.out.println(recipe);
        }
    }
}

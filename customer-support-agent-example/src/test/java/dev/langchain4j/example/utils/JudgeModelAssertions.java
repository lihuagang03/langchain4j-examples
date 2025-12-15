package dev.langchain4j.example.utils;

import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.output.JsonSchemas;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 评估模型断言
 */
public class JudgeModelAssertions {

    /**
     * 状况评估结果
     */
    private enum ConditionAssessmentResult {
        /**
         * 满意
         */
        SATISFIED,
        /**
         * 不满意
         */
        NOT_SATISFIED,
        /**
         * 不确定
         */
        NOT_SURE
    }

    /**
     * 状况评估
     * @param conditionIndex 条件索引
     * @param reasoning 推理
     * @param result 状况评估结果
     */
    private record ConditionAssessment(
            int conditionIndex,
            String reasoning,
            ConditionAssessmentResult result) {
    }

    /**
     * 状况评估列表
     */
    private record ConditionAssessments(List<ConditionAssessment> conditionAssessments) {
    }

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    /**
     * JSON响应格式
     */
    private static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
            .type(JSON)
            .jsonSchema(JsonSchemas.jsonSchemaFrom(ConditionAssessments.class).get())
            .build();

    public static ModelAssertion with(ChatModel judgeModel) {
        return new ModelAssertion(judgeModel);
    }

    /**
     * 模型断言
     */
    public static class ModelAssertion {

        /**
         * 评估的聊天模型
         */
        private final ChatModel judgeModel;

        ModelAssertion(ChatModel judgeModel) {
            this.judgeModel = ensureNotNull(judgeModel, "judgeModel");
        }

        public TextAssertion assertThat(String text) {
            return new TextAssertion(judgeModel, text);
        }
    }

    /**
     * 文本断言
     */
    public static class TextAssertion {

        /**
         * 评估的聊天模型
         */
        private final ChatModel judgeModel;
        /**
         * 文本
         */
        private final String text;

        TextAssertion(ChatModel judgeModel, String text) {
            this.judgeModel = ensureNotNull(judgeModel, "judgeModel");
            this.text = ensureNotNull(text, "text");
        }

        public TextAssertion satisfies(String... conditions) {
            return satisfies(asList(conditions));
        }

        /**
         * 满足所有条件
         * @param conditions 条件列表
         */
        public TextAssertion satisfies(List<String> conditions) {

            ensureNotEmpty(conditions, "conditions");

            // 条件格式化
            StringBuilder conditionsFormatted = new StringBuilder();
            int i = 0;
            for (String condition : conditions) {
                conditionsFormatted.append("<condition%s>%s</condition%s>".formatted(i, condition, i++));
                conditionsFormatted.append("\n");
            }

            // 聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(
                            // 系统消息
                            // 以下文本符合以下条件吗？
                            // 为每个条件提供索引、理由和评估结果。
                            SystemMessage.from("""
                                    Does the following text satisfy the following conditions?
                                    %s
                                    Provide index, reasoning and assessment result for each condition.
                                    """.formatted(conditionsFormatted)
                            ),
                            // 用户消息的文本
                            UserMessage.from("<text>%s</text>".formatted(text))
                    )
                    .parameters(ChatRequestParameters.builder()
                            .responseFormat(RESPONSE_FORMAT)
                            .build())
                    .build();

            // 聊天-响应
            ChatResponse chatResponse = judgeModel.chat(chatRequest);

            // AI消息的文本
            String json = chatResponse.aiMessage().text();
            try {
                // 状况评估列表
                ConditionAssessments conditionAssessments = JSON_MAPPER.readValue(json, ConditionAssessments.class);

                List<String> failures = new ArrayList<>();

                for (ConditionAssessment assessment : conditionAssessments.conditionAssessments) {
                    // 状况评估
                    if (assessment.result != ConditionAssessmentResult.SATISFIED) {
                        failures.add("""
                                Condition %s: %s
                                Reasoning: %s
                                """.formatted(
                                assessment.conditionIndex, conditions.get(assessment.conditionIndex),
                                assessment.reasoning
                        ));
                    }
                }

                if (!failures.isEmpty()) {
                    // 文本 '%s' 的某些条件未满足：
                    //
                    //%s
                    fail("Some conditions were not satisfied for the text '%s':\n\n%s"
                            .formatted(text, String.join("\n", failures)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return this;
        }
    }
}

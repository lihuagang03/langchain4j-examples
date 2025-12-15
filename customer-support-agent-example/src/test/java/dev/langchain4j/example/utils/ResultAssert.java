package dev.langchain4j.example.utils;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import org.assertj.core.api.AbstractAssert;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.util.IterableUtil.isNullOrEmpty;

/**
 * AI服务调用的结果的断言
 * Custom AssertJ assertions for {@link Result} class.
 */
public class ResultAssert extends AbstractAssert<ResultAssert, Result<?>> {

    public ResultAssert(Result<?> actual) {
        super(actual, ResultAssert.class);
    }

    public static ResultAssert assertThat(Result<?> actual) {
        return new ResultAssert(actual);
    }

    /**
     * 只有这个工具被执行
     * @param toolName 工具名称
     */
    public ResultAssert onlyToolWasExecuted(String toolName) {

        isNotNull();

        // 工具执行列表
        // 表示工具的执行，包括请求和结果。
        List<ToolExecution> toolExecutions = actual.toolExecutions();
        if (isNullOrEmpty(toolExecutions)) {
            failWithMessage("Expected <%s> tool to be executed, but no tools were executed at all");
        }

        // 执行的工具名称集合
        Set<String> executedToolNames = toolExecutions.stream()
                .map(toolExecution -> toolExecution.request().name())
                .collect(toSet());

        if (!executedToolNames.contains(toolName)) {
            failWithMessage("Expected tool <%s> to be executed, but found different tools: <%s>",
                    toolName, executedToolNames);
        }

        if (executedToolNames.size() > 1) {
            failWithMessage("Expected only tool <%s> to be executed, but additional tools were executed: <%s>",
                    toolName, executedToolNames);
        }

        return this;
    }

    /**
     * 未执行任何工具
     */
    public ResultAssert noToolsWereExecuted() {

        isNotNull();

        // 工具执行列表
        // 表示工具的执行，包括请求和结果。
        List<ToolExecution> toolExecutions = actual.toolExecutions();
        if (!isNullOrEmpty(toolExecutions)) {
            failWithMessage("Expected no tools to be executed, but found: <%s>", toolExecutions);
        }

        return this;
    }

    /**
     * 检索到的来源包含这个文本
     * @param text 文本
     */
    public ResultAssert retrievedSourcesContain(String text) {

        isNotNull();

        // 来源内容列表
        List<Content> sources = actual.sources();
        if (isNullOrEmpty(sources)) {
            failWithMessage("Expected sources to be retrieved, but no sources were found");
        }

        // 来源文本片段的文本列表
        List<String> sourceTexts = sources.stream()
                .map(source -> source.textSegment().text())
                .toList();

        if (sourceTexts.stream().noneMatch(sourceText -> sourceText.contains(text))) {
            failWithMessage("Expected to find text <%s> in sources, but found following texts:\n%s",
                    text,
                    sourceTexts.stream().collect(joining("\n- ", "- ", "")));
        }

        return this;
    }
}

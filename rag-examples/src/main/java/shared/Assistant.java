package shared;

/**
 * 助手
 * 这是一个“AI 服务”。它是一个具有 AI 功能/特性的 Java 服务。
 * 它可以像其他服务一样集成到你的代码中，作为一个 Bean，并且可以在测试中进行模拟。
 * 其目标是在你的（现有）代码库中无缝集成 AI 功能，尽量减少摩擦。
 * 概念上，它类似于 Spring Data JPA 或 Retrofit。
 * 你定义一个接口，并可选择使用注解进行自定义。
 * LangChain4j 然后会通过代理和反射为该接口提供实现。这种方法抽象掉了所有复杂性和样板代码。
 * 因此，你无需处理模型、消息、聊天记忆、RAG 组件、工具调用、输出解析器等。
 * 但是，不用担心。它非常灵活且可配置，因此你可以根据具体的使用场景进行定制。
 * This is an "AI Service". It is a Java service with AI capabilities/features.
 * It can be integrated into your code like any other service, acting as a bean, and can be mocked for testing.
 * The goal is to seamlessly integrate AI functionality into your (existing) codebase with minimal friction.
 * It's conceptually similar to Spring Data JPA or Retrofit.
 * You define an interface and optionally customize it with annotations.
 * LangChain4j then provides an implementation for this interface using proxy and reflection.
 * This approach abstracts away all the complexity and boilerplate.
 * So you won't need to juggle the model, messages, memory, RAG components, tools, output parsers, etc.
 * However, don't worry. It's quite flexible and configurable, so you'll be able to tailor it
 * to your specific use case.
 * <br>
 * More info here: https://docs.langchain4j.dev/tutorials/ai-services
 */
public interface Assistant {

    /**
     * 问答
     * @param query 查询
     */
    String answer(String query);
}
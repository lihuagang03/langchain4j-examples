package dev.langchain4j.example.mcp;

/**
 * 机器人
 */
public interface Bot {

    /**
     * 聊天
     * @param prompt 提示
     */
    String chat(String prompt);
}

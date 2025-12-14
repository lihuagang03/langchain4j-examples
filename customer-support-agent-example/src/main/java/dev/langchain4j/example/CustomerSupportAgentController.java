package dev.langchain4j.example;

import dev.langchain4j.service.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户支持代理的控制器
 */
@RestController
public class CustomerSupportAgentController {

    /**
     * 客户支持代理
     */
    private final CustomerSupportAgent customerSupportAgent;

    public CustomerSupportAgentController(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    /**
     * 客户支持代理
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return 问答内容
     */
    @GetMapping("/customerSupportAgent")
    public String customerSupportAgent(
            @RequestParam String sessionId,
            @RequestParam String userMessage
    ) {
        // 问答内容的结果
        Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
        return result.content();
    }
}

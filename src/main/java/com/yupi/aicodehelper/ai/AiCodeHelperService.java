package com.yupi.aicodehelper.ai;

import com.yupi.aicodehelper.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

@InputGuardrails({SafeInputGuardrail.class})
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "hot100-wrong-analysis-prompt.txt")
    String analyzeHot100WrongAnswer(String userMessage);

    @SystemMessage(fromResource = "hot100-wrong-analysis-json-repair-prompt.txt")
    String repairHot100WrongAnalysisJson(String userMessage);

    @SystemMessage(fromResource = "hot100-agent-loop-prompt.txt")
    String runHot100AgentLoopTurn(String userMessage);

    @SystemMessage(fromResource = "system-prompt-role.txt")
    Flux<String> chatStream(@MemoryId int memoryId,
                            @UserMessage String userMessage,
                            @V("roleCard") String roleCard,
                            @V("coachingStrategy") String coachingStrategy,
                            @V("solvingModeStrategy") String solvingModeStrategy,
                            @V("problemContext") String problemContext,
                            @V("userLearningProfile") String userLearningProfile,
                            @V("mcpCapabilityNotice") String mcpCapabilityNotice);
}

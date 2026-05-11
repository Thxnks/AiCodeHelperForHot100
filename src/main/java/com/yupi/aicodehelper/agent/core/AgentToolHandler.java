package com.yupi.aicodehelper.agent.core;

import java.util.Map;

@FunctionalInterface
public interface AgentToolHandler {

    Object handle(Map<String, Object> input);
}

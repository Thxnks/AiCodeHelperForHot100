package com.yupi.aicodehelper.agent.core;

import java.util.Map;

public record ToolUseBlock(String id, String name, Map<String, Object> input) {
}

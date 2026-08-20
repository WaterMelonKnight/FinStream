package io.finstream.mcp;

public record McpToolResult(boolean success, Object data, McpToolError error) {
    public static McpToolResult found(Object data) { return new McpToolResult(true, data, null); }
    public static McpToolResult error(String code, String message) {
        return new McpToolResult(false, null, new McpToolError(code, message));
    }

    public record McpToolError(String code, String message) {}
}

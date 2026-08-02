import { useCallback, useState } from "react";
import { streamChat } from "../api/chatApi";

const initialState = {
  assistantText: "",
  activeTool: null,
  isStreaming: false,
  completed: false,
  error: null,
};

function dataString(data, key) {
  const value = data?.[key];
  return typeof value === "string" ? value : null;
}

export function useChatStream() {
  const [state, setState] = useState(initialState);

  const startStream = useCallback(async (conversationId, message) => {
    setState({ ...initialState, isStreaming: true });
    try {
      await streamChat(conversationId, message, (event) => {
        setState((current) => {
          switch (event.eventType) {
            case "text-delta":
              return {
                ...current,
                assistantText:
                  current.assistantText +
                  (dataString(event.data, "delta") ?? ""),
              };
            case "tool-start":
              return {
                ...current,
                activeTool:
                  dataString(event.data, "toolName") ??
                  dataString(event.data, "name"),
              };
            case "tool-result":
              return { ...current, activeTool: null };
            case "completion":
              return {
                ...current,
                isStreaming: false,
                completed: true,
                activeTool: null,
              };
            case "error":
              return {
                ...current,
                isStreaming: false,
                activeTool: null,
                error:
                  dataString(event.data, "message") ??
                  "The assistant stream failed.",
              };
            default:
              return current;
          }
        });
      });
      setState((current) =>
        current.isStreaming
          ? { ...current, isStreaming: false, completed: true }
          : current,
      );
    } catch (error) {
      setState((current) => ({
        ...current,
        isStreaming: false,
        activeTool: null,
        error:
          error instanceof Error
            ? error.message
            : "The assistant stream failed.",
      }));
    }
  }, []);

  const reset = useCallback(() => setState(initialState), []);
  return { ...state, startStream, reset };
}

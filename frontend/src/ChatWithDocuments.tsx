
import { useState } from "react";
import { ApiError, api } from "./api";
import "./ChatWithDocuments.css";

interface ChatSource {
  documentId: string;
  documentName: string;
  chunkIndex: number;
  content: string;
  citationNumber: number;
}

interface ChatResponse {
  answer: string;
  sources: ChatSource[];
}

interface ChatMessage {
  id: number;
  role: "user" | "assistant";
  text: string;
  sources?: ChatSource[];
}

const suggestions = [
  "What documents are available?",
  "Explain the main topics in my documents.",
  "Find information about a specific topic.",
];

export default function ChatWithDocuments() {
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const submitQuestion = async (questionText: string) => {
    const query = questionText.trim();

    if (!query || isLoading) return;

    setQuestion("");
    setIsLoading(true);

    setMessages((previous) => [
      ...previous,
      {
        id: Date.now(),
        role: "user",
        text: query,
      },
    ]);

    try {
      const response = await api.postJson<ChatResponse>(
        "/api/chat",
        {
          message: query,
        }
      );

      setMessages((previous) => [
        ...previous,
        {
          id: Date.now() + 1,
          role: "assistant",
          text: response.answer,
          sources: response.sources ?? [],
        },
      ]);
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        window.location.reload();
        return;
      }

      const errorMessage =
        error instanceof Error ? error.message : "Chat request failed.";

      setMessages((previous) => [
        ...previous,
        {
          id: Date.now() + 1,
          role: "assistant",
          text: `Something went wrong: ${errorMessage}`,
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void submitQuestion(question);
  };

  const handleKeyDown = (
    event: React.KeyboardEvent<HTMLTextAreaElement>
  ) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void submitQuestion(question);
    }
  };

  const clearChat = () => {
    if (!isLoading) {
      setMessages([]);
      setQuestion("");
    }
  };

  return (
    <div className="chat-page">
      <div className="chat-notice">
        <strong>AI document assistant</strong>
        <span>
          Ask questions about your uploaded documents.
          DocMind uses document retrieval and a local AI model
          to generate answers with supporting sources.
        </span>
      </div>

      <section className="chat-panel">
        <div className="chat-toolbar">
          <div>
            <strong>Chat with Documents</strong>
            <p>Ask questions and get AI-generated answers.</p>
          </div>

          <button
            type="button"
            className="chat-clear-button"
            onClick={clearChat}
            disabled={messages.length === 0 || isLoading}
          >
            Clear chat
          </button>
        </div>

        <div className="chat-messages" aria-live="polite">
          {messages.length === 0 && (
            <div className="chat-welcome">
              <div className="chat-welcome-icon">✦</div>

              <h2>Explore your documents</h2>

              <p>
                Ask a question and let DocMind find
                relevant information and generate an answer.
              </p>

              {suggestions.map((suggestion) => (
                <button
                  type="button"
                  className="chat-suggestion"
                  key={suggestion}
                  onClick={() => void submitQuestion(suggestion)}
                  disabled={isLoading}
                >
                  {suggestion}
                </button>
              ))}
            </div>
          )}

          {messages.map((message) => (
            <div
              className={`chat-message ${message.role}`}
              key={message.id}
            >
              <div className="chat-role">
                {message.role === "user" ? "You" : "DocMind"}
              </div>

              <p className="chat-message-text">{message.text}</p>

              {message.sources && message.sources.length > 0 && (
                <div className="chat-sources">
                  <h3>Sources and references</h3>

                  {message.sources.map((source) => (
                    <article
                      className="chat-source"
                      key={`${source.documentId}-${source.chunkIndex}`}
                    >
                      <div className="chat-source-heading">
                        <span className="chat-source-number">
                          {source.citationNumber}
                        </span>

                        <div>
                          <strong>{source.documentName}</strong>
                          <span>Chunk {source.chunkIndex}</span>
                        </div>
                      </div>

                      <p>{source.content}</p>
                    </article>
                  ))}
                </div>
              )}
            </div>
          ))}

          {isLoading && (
            <div className="chat-loading">
              <span>✦</span>
              <span>DocMind is thinking...</span>
            </div>
          )}
        </div>

        <form className="chat-composer" onSubmit={handleSubmit}>
          <textarea
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask something about your documents..."
            rows={2}
            disabled={isLoading}
            aria-label="Your question"
          />

          <button
            type="submit"
            className="search-button"
            disabled={isLoading || !question.trim()}
          >
            {isLoading ? "Thinking..." : "Send"}
          </button>
        </form>

        <p className="chat-footer-hint">
          Press Enter to send. Use Shift + Enter for a new line.
        </p>
      </section>
    </div>
  );
}

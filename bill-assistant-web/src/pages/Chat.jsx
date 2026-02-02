import { useEffect, useRef, useState } from "react";
import { sendChatMessage, getChatContext } from "../api/chatApi";

export default function Chat() {
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([]);
  const [typing, setTyping] = useState(false);
  const [contextId, setContextId] = useState(null);

  const bottomRef = useRef(null);
  const inputRef = useRef(null);
  const containerRef = useRef(null);
  const userScrolledUp = useRef(false);

  /* =========================
   * 1️⃣ Load contextId from backend
   * ========================= */
  useEffect(() => {
    getChatContext()
      .then(setContextId)
      .catch(() => setContextId("unknown"));
  }, []);

  const storageKey = contextId
    ? `bill-assistant-chat-${contextId}`
    : null;

  /* =========================
   * 2️⃣ Load chat for context
   * ========================= */
  useEffect(() => {
    if (!storageKey) return;

    const saved = sessionStorage.getItem(storageKey);
    if (saved) {
      try {
        setMessages(JSON.parse(saved));
      } catch {
        sessionStorage.removeItem(storageKey);
        setMessages(getWelcomeMessage());
      }
    } else {
      setMessages(getWelcomeMessage());
    }
  }, [storageKey]);

  /* =========================
   * 3️⃣ Persist chat per context
   * ========================= */
  useEffect(() => {
    if (!storageKey || messages.length === 0) return;
    sessionStorage.setItem(storageKey, JSON.stringify(messages));
  }, [messages, storageKey]);

  /* =========================
   * 4️⃣ Auto-scroll (safe)
   * ========================= */
  useEffect(() => {
    if (userScrolledUp.current) return;

    bottomRef.current?.scrollIntoView({
      behavior: "auto",
    });
  }, [messages]);

  /* =========================
   * 5️⃣ Keep cursor in input
   * ========================= */
  useEffect(() => {
    if (!typing) {
      inputRef.current?.focus();
    }
  }, [typing]);

  const send = async (text) => {
    if (!text.trim() || typing) return;

    setMessages((m) => [...m, { from: "user", text }]);
    setTyping(true);

    try {
      const reply = await sendChatMessage(text);
      setMessages((m) => [...m, { from: "bot", text: reply }]);
    } catch {
      setMessages((m) => [
        ...m,
        {
          from: "bot",
          text:
            "Sorry, something went wrong while processing your request. Please try again.",
        },
      ]);
    } finally {
      setTyping(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send(input);
      setInput("");
    }
  };

  return (
    <div className="bg-white rounded-xl shadow h-[70vh] flex flex-col">
      {/* Header */}
      <div className="flex items-center gap-2 px-4 py-3 border-b">
        <span className="text-2xl">🤖</span>
        <h2 className="text-lg font-semibold">Bill Assistant</h2>
      </div>

      {/* Messages */}
      <div
        ref={containerRef}
        onScroll={() => {
          const el = containerRef.current;
          if (!el) return;

          const nearBottom =
            el.scrollHeight - el.scrollTop - el.clientHeight < 50;

          userScrolledUp.current = !nearBottom;
        }}
        className="flex-1 overflow-auto space-y-3 p-4"
      >
        {messages.map((m, i) => (
          <div
            key={i}
            className={`flex items-start gap-2 ${
              m.from === "user" ? "justify-end" : "justify-start"
            }`}
          >
            {m.from === "bot" && <span className="text-xl">🤖</span>}

            <div
              className={`max-w-[70%] px-3 py-2 rounded-lg text-sm whitespace-pre-wrap ${
                m.from === "user"
                  ? "bg-blue-600 text-white"
                  : "bg-slate-200 text-slate-800"
              }`}
            >
              {m.text}
            </div>
          </div>
        ))}

        {typing && (
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <span className="text-lg">🤖</span>
            <span>is typing…</span>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="flex gap-2 p-4 border-t">
        <textarea
          ref={inputRef}
          className="flex-1 border rounded-lg px-3 py-2 resize-none
            focus:outline-none focus:ring-2 focus:ring-blue-500"
          rows={2}
          value={input}
          disabled={typing}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask me about bills, payments… (Enter to send, Shift+Enter for new line)"
        />
        <button
          type="button"
          disabled={typing || !input.trim()}
          className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50
            text-white px-4 rounded-lg"
          onClick={() => {
            send(input);
            setInput("");
          }}
        >
          {typing ? "Waiting…" : "Send"}
        </button>
      </div>
    </div>
  );
}

/* =========================
 * Helpers
 * ========================= */
function getWelcomeMessage() {
  return [
    {
      from: "bot",
      text:
        "Hi! I’m your Bill Assistant 🤖. I can help you pay bills, check payments, or answer questions.",
    },
  ];
}

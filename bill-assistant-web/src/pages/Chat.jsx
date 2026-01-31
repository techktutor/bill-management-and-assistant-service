import { useEffect, useRef, useState } from "react";
import { sendChatMessage } from "../api/chatApi";

const STORAGE_KEY = "bill-assistant-chat";

export default function Chat() {
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([]);
  const [typing, setTyping] = useState(false);

  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  /* =========================
   * 1️⃣ Load chat from storage
   * ========================= */
  useEffect(() => {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        setMessages(JSON.parse(saved));
      } catch {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } else {
      setMessages([
        {
          from: "bot",
          text: "Hi! I’m your Bill Assistant. I can help you pay bills, check payments, or answer questions.",
        },
      ]);
    }
  }, []);

  /* =========================
   * 2️⃣ Persist chat on change
   * ========================= */
  useEffect(() => {
    if (messages.length > 0) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
    }
  }, [messages]);

  /* =========================
   * 3️⃣ Auto-scroll to bottom
   * ========================= */
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, typing]);

  /* =========================
   * 4️⃣ Keep cursor inside input
   * ========================= */
  useEffect(() => {
    if (!typing) {
      inputRef.current?.focus();
    }
  }, [typing, messages]);

  const send = async (text) => {
    if (!text.trim() || typing) return;

    setMessages((m) => [...m, { from: "user", text }]);
    setTyping(true);

    try {
      const reply = await sendChatMessage(text);
      setMessages((m) => [...m, { from: "bot", text: reply }]);
    } finally {
      setTyping(false);
    }
  };

  /* =========================
   * 5️⃣ Enter to send
   * ========================= */
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
      <div className="flex-1 overflow-auto space-y-3 p-4">
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

        {/* Typing indicator */}
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
          className="flex-1 border rounded-lg px-3 py-2 resize-none"
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
          className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white px-4 rounded-lg"
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

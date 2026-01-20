package com.wells.bill.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatConfig {

    private final VectorStore vectorStore;

    public ChatConfig(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Bean(name = "chatMemory")
    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, @Qualifier("chatMemory") ChatMemory chatMemory) {
        // Builder already has:
        //  - ChatModel (Vertex AI Gemini)
        //  - Memory advisor (from chat-memory starter)
        //  - RAG advisor (from rag + advisors-vector-store starters)
        //  - Tool pipeline (from @Tool methods)
        return builder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new SafeGuardAdvisor(List.of()),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .defaultSystem("""
                                You are a helpful AI Bill Assistant and Your name is Eagle.
                                Always identify yourself as Eagle and welcome the user with greetings.
                                Your task is to analyze uploaded bills or invoices and return structured, accurate information.
                                If anything else apart from bill/invoice related, politely decline saying ask anything related to your bills only.
                        """
                )
                .build();
    }
}

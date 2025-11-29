# AI-Powered Bill Management and Payment Assistant

## Project Vision

Your project originally aimed to build:

### ✔ AI Assistant

That can:
- Read/understand bills
- Extract due dates, amounts, biller details
- Interact with the user conversationally
- Automate payments
- Schedule reminders
- Schedule future payments
- Manage bills end-to-end using natural language

### ✔ Bill Management System

With:
- Bill ingestion
- Bill storage
- Bill PDF/text understanding
- Classification/metadata extraction
- Reminders
- Due date tracking

### ✔ Payment Processor Integration

Initially Stripe → later replaced with your own custom Stripe-like processor.

### ✔ Tool System

Backend functions exposed to AI agents as "tools" for:
- Scheduling payments
- Making instant payments
- Cancelling scheduled payments
- Retrieving bill info

### ✔ Microservice-ready Architecture

The codebase uses:
- Spring Boot microservice patterns
- Repository + service + controller layers
- Domain-driven objects
- Outbox pattern
- Scheduling
- Tokenization
- Ledger entries
- Idempotency
- Async job processing

All of which align beautifully with the project's target architecture.

### ✔ AI Agent Orchestration

You started implementing agent tools such as:
- `schedulePayment`
- `makeInstantPayment`
- `cancelScheduledPayment`

This is exactly what an AI agent needs to automate bills in a safe, controlled and deterministic way.

### ✔ Custom Payment Rail (Stripe-like)

You went beyond the original plan by:
- Building your own payment-intent + authorize + capture flows
- Implementing a ledger
- Tokenization service
- Outbox dispatcher
- Scheduled payment engine

This enhances the original vision and makes the system independent of Stripe.

User -> ChatController (POST /api/chat) -> AssistantOrchestratorService.processMessage
-> MemoryAdvisor (ChatMemory) -> RetrievalAugmentationAdvisor -> VectorStoreDocumentRetriever
-> VectorStore (pgvector) -> returns Documents -> Context assembled
-> ChatClient prompt (system + user + context + tools)
-> LLM returns text OR structured tool-invocation
-> If tool-invocation: Orchestrator ensures tool confirmation policy -> execute tool (PaymentToolAdapter/BillToolAdapter)
-> Tool may call MakePaymentService or ScheduledPaymentService
-> Persist operations in DB (payments, scheduled payments)
-> Tool returns structured output -> ChatClient consumes tool output -> final answer returned to user


Separate ingestion path:
User uploads file -> IngestController -> ETLPipelineService
-> Tika extraction -> TokenTextSplitter -> VectorStore.add(documents with metadata)
-> Ingest audit log written (requestId) -> Documents available for retrieval


Scheduled execution path:
ScheduledJob PaymentExecutionService (cron) -> finds due scheduled payments -> executes via MakePaymentService -> updates ScheduledPaymentEntity

## RAG Integration Plan
com.wells.bill.assistant
├── controller
│    ├── ChatController
│    └── RagQueryController (optional - dev only)
├── service
│    ├── AssistantOrchestratorService   <-- integrates RAG
│    ├── BillService
│    ├── MakePaymentService
│    ├── PaymentExecutionService
│    └── ScheduledPaymentService
├── rag                                  <-- your RAG engine layer
│    ├── VectorRetrievalService
│    ├── RagQueryService   <-- place it here
│    ├── ChunkRankerService (future)
│    ├── BillContextStitcher (future)
├── tools
│    ├── BillRagTools           <-- use hybrid retrieval here
│    ├── OverdueExplainerTool   <-- uses retrieval + stitching
│    └── BillRagQueryTool (optional)

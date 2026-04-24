package com.quizgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizgen.model.Question;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AIQuizGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(AIQuizGenerationService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.base.url:https://api.openai.com/v1/}")
    private String baseUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.max.tokens:2000}")
    private int maxTokens;

    @Value("${app.quiz.ai.temperature:0.5}")
    private double temperature;

    @Value("${app.quiz.chunk.max.chars:3000}")
    private int maxChunkSize;

    @Value("${app.quiz.chunk.overlap.chars:250}")
    private int chunkOverlapChars;

    @Value("${app.quiz.ai.max.retries:3}")
    private int maxRetries;

    @Value("${app.quiz.ai.retry.delay.ms:1200}")
    private long retryDelayMs;

    @Value("${app.quiz.ai.chunk.parallelism:2}")
    private int chunkParallelism;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OpenAiService openAiService;
    private ExecutorService chunkExecutor;

    @PostConstruct
    public void initializeClient() {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenAI/Groq API key is not configured; AI generation will fail until configured.");
            return;
        }

        ObjectMapper mapper = OpenAiService.defaultObjectMapper();
        OkHttpClient baseClient = OpenAiService.defaultClient(apiKey, Duration.ofSeconds(60));
        OkHttpClient client = baseClient.newBuilder()
            .addInterceptor(chain -> {
                okhttp3.Request request = chain.request();
                okhttp3.HttpUrl url = request.url();

                if (url.host().equalsIgnoreCase("api.groq.com") && url.encodedPath().startsWith("/v1/")) {
                okhttp3.HttpUrl rewritten = url.newBuilder()
                    .encodedPath("/openai" + url.encodedPath())
                    .build();
                request = request.newBuilder().url(rewritten).build();
                }

                return chain.proceed(request);
            })
            .build();
        Retrofit retrofit = OpenAiService.defaultRetrofit(client, mapper)
                .newBuilder()
                .baseUrl(baseUrl)
                .build();

        OpenAiApi api = retrofit.create(OpenAiApi.class);
        ExecutorService apiExecutor = client.dispatcher().executorService();

        openAiService = new OpenAiService(api, apiExecutor);

        int parallelism = Math.max(1, Math.min(chunkParallelism, 4));
        chunkExecutor = Executors.newFixedThreadPool(parallelism);

        logger.info("AI quiz generation initialized with model={} baseUrl={} chunkParallelism={}",
                model, baseUrl, parallelism);
    }

    @PreDestroy
    public void shutdown() {
        if (chunkExecutor != null) {
            chunkExecutor.shutdown();
        }
        if (openAiService != null) {
            openAiService.shutdownExecutor();
        }
    }

    
    public static String buildCacheKey(String text, int questionCount, String modelName) {
        return modelName + ":" + questionCount + ":" + Objects.hash(modelName, questionCount, text);
    }

    
    @Cacheable(value = "quizzes", key = "T(com.quizgen.service.AIQuizGenerationService).buildCacheKey(#root.args[0], #root.args[1], #root.target.modelName)")
    public List<Question> generateQuestions(String text, int questionCount) throws Exception {
        ensureClientReady();

        if (text == null || text.isBlank()) {
            return List.of();
        }

        int requestedQuestionCount = Math.max(1, questionCount);
        logger.info("Generating {} questions using AI from text of length {}", requestedQuestionCount, text.length());

        List<String> chunks = buildSlidingWindowChunks(text);
        int[] questionDistribution = distributeQuestionCounts(requestedQuestionCount, chunks.size());

        if (chunks.size() == 1) {
            List<Question> singleChunkQuestions = trimQuestions(
                    generateQuestionsWithRetry(chunks.get(0), questionDistribution[0]),
                    requestedQuestionCount);

            if (singleChunkQuestions.isEmpty()) {
                throw new Exception("AI returned no valid questions");
            }

            return singleChunkQuestions;
        }

        logger.info("Large text detected. Processing {} chunks with overlap {} chars.",
                chunks.size(), Math.max(0, chunkOverlapChars));

        List<CompletableFuture<List<Question>>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i + 1;
            final String chunkText = chunks.get(i);
            final int chunkQuestionCount = questionDistribution[i];

            if (chunkQuestionCount <= 0) {
                continue;
            }

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    logger.info("Processing chunk {}/{} ({} chars) for {} questions",
                            chunkIndex, chunks.size(), chunkText.length(), chunkQuestionCount);
                    return generateQuestionsWithRetry(chunkText, chunkQuestionCount);
                } catch (Exception e) {
                    logger.warn("Chunk {}/{} failed: {}", chunkIndex, chunks.size(), e.getMessage());
                    return List.of();
                }
            }, chunkExecutor));
        }

        List<Question> mergedQuestions = new ArrayList<>();
        for (CompletableFuture<List<Question>> future : futures) {
            mergedQuestions.addAll(future.join());
        }

        List<Question> deduplicatedQuestions = deduplicateQuestions(mergedQuestions);

        if (deduplicatedQuestions.size() < requestedQuestionCount) {
            logger.warn("Generated {} questions after chunking, less than requested {}",
                    deduplicatedQuestions.size(), requestedQuestionCount);
        }

        if (deduplicatedQuestions.isEmpty()) {
            throw new Exception("AI returned no valid questions after chunk processing");
        }

        return trimQuestions(deduplicatedQuestions, requestedQuestionCount);
    }

    private void ensureClientReady() {
        if (openAiService == null) {
            initializeClient();
        }
        if (openAiService == null) {
            throw new IllegalStateException("OpenAI-compatible client could not be initialized. Check API key and base URL.");
        }
    }

    private List<String> buildSlidingWindowChunks(String text) {
        List<String> chunks = new ArrayList<>();

        int chunkSize = Math.max(800, maxChunkSize);
        int overlap = Math.max(0, Math.min(chunkOverlapChars, chunkSize / 3));

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(text.substring(start, end));

            if (end >= text.length()) {
                break;
            }

            start = Math.max(0, end - overlap);
        }

        return chunks;
    }

    private int[] distributeQuestionCounts(int totalQuestions, int chunkCount) {
        int[] distribution = new int[chunkCount];

        int base = totalQuestions / chunkCount;
        int remainder = totalQuestions % chunkCount;

        for (int i = 0; i < chunkCount; i++) {
            distribution[i] = base + (i < remainder ? 1 : 0);
        }

        return distribution;
    }

    private List<Question> deduplicateQuestions(List<Question> questions) {
        Map<String, Question> deduplicated = new LinkedHashMap<>();

        for (Question question : questions) {
            if (question == null || question.getQuestionText() == null) {
                continue;
            }
            String key = question.getQuestionText().trim().toLowerCase(Locale.ROOT);
            deduplicated.putIfAbsent(key, question);
        }

        return new ArrayList<>(deduplicated.values());
    }

    private List<Question> trimQuestions(List<Question> questions, int maxQuestions) {
        if (questions.size() <= maxQuestions) {
            return questions;
        }
        return new ArrayList<>(questions.subList(0, maxQuestions));
    }

    private List<Question> generateQuestionsWithRetry(String chunkText, int questionCount) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= Math.max(1, maxRetries); attempt++) {
            try {
                return generateQuestionsInternal(chunkText, questionCount);
            } catch (Exception e) {
                lastException = e;

                boolean retryable = isRetryableError(e);
                if (!retryable || attempt >= maxRetries) {
                    break;
                }

                long delay = retryDelayMs * (1L << (attempt - 1));
                logger.warn("AI call attempt {}/{} failed: {}. Retrying after {} ms",
                        attempt, maxRetries, e.getMessage(), delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Retry interrupted", interruptedException);
                }
            }
        }

        throw new Exception("AI quiz generation failed after retries", lastException);
    }

    
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("429")
                || lower.contains("rate limit")
                || lower.contains("timeout")
                || lower.contains("temporarily")
                || lower.contains("503")
                || lower.contains("502")
                || lower.contains("connection reset");
    }

    
    private List<Question> generateQuestionsInternal(String chunkText, int questionCount) throws Exception {
        try {
            String prompt = createPrompt(chunkText, questionCount);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                    "You are an expert educator who creates high-quality multiple choice questions."),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)))
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            logger.info("Received AI response: {}", response.substring(0, Math.min(200, response.length())));

            List<Question> parsedQuestions = parseAIResponse(response);
            return trimQuestions(parsedQuestions, questionCount);

        } catch (Exception e) {
            logger.error("Error generating questions with AI: {}", e.getMessage());
            throw new Exception("AI quiz generation failed: " + e.getMessage(), e);
        }
    }

    
    private String createPrompt(String text, int questionCount) {
        return String.format("""
                Based on the following study material, generate exactly %d multiple choice questions (MCQs).

                Study Material:
                %s

                Requirements:
                1. Each question must have exactly 4 options
                2. Only one option should be correct
                3. Questions should test understanding, not just memorization
                4. Options should be plausible and not obviously wrong
                5. Return the response in the following JSON format:

                {
                  "questions": [
                    {
                      "question": "Question text here?",
                      "option1": "First option",
                      "option2": "Second option",
                      "option3": "Third option",
                      "option4": "Fourth option",
                      "correctAnswer": 1
                    }
                  ]
                }

                The correctAnswer should be a number from 1 to 4 indicating which option is correct.
                Return ONLY the JSON, no additional text.
                """, questionCount, text);
    }

    
    private List<Question> parseAIResponse(String response) throws Exception {
        List<Question> questions = new ArrayList<>();

        try {
            String jsonString = extractJSON(response);

            JsonNode root = objectMapper.readTree(jsonString);
            JsonNode questionsNode = root.path("questions");

            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode questionNode : questionsNode) {
                    String questionText = questionNode.path("question").asText("").trim();
                    String option1 = questionNode.path("option1").asText("").trim();
                    String option2 = questionNode.path("option2").asText("").trim();
                    String option3 = questionNode.path("option3").asText("").trim();
                    String option4 = questionNode.path("option4").asText("").trim();
                    int correctAnswer = questionNode.path("correctAnswer").asInt(1);

                    if (questionText.isEmpty() || option1.isEmpty() || option2.isEmpty() || option3.isEmpty() || option4.isEmpty()) {
                        continue;
                    }

                    if (correctAnswer < 1 || correctAnswer > 4) {
                        correctAnswer = 1;
                    }

                    Question question = new Question();
                    question.setQuestionText(questionText);
                    question.setOption1(option1);
                    question.setOption2(option2);
                    question.setOption3(option3);
                    question.setOption4(option4);
                    question.setCorrectAnswer(correctAnswer);

                    questions.add(question);
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage());
            throw new Exception("Failed to parse AI response", e);
        }

        return questions;
    }

    
    private String extractJSON(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        int startIndex = trimmed.indexOf('{');
        int endIndex = trimmed.lastIndexOf('}');

        if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
            return trimmed.substring(startIndex, endIndex + 1);
        }

        return trimmed;
    }

    public String getModelName() {
        return model;
    }
}

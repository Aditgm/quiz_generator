package com.quizgen.service;

import com.quizgen.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RuleBasedQuizGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleBasedQuizGenerationService.class);

    private static final List<String> QUESTION_TEMPLATES = List.of(
            "What is %s?",
            "Define %s.",
            "Which of the following best describes %s?",
            "What does %s refer to?",
            "According to the text, what is %s?");

    
    public List<Question> generateQuestions(String text, int questionCount) {
        logger.info("Generating {} questions using rule-based approach from text of length {}",
                questionCount, text.length());

        List<Question> questions = new ArrayList<>();

        List<String> sentences = extractSentences(text);
        List<String> keyConcepts = extractKeyConcepts(text);

        int questionsGenerated = 0;
        Random random = new Random();

        for (int i = 0; i < sentences.size() && questionsGenerated < questionCount; i++) {
            String sentence = sentences.get(i);

            if (sentence.length() < 20 || sentence.length() > 200) {
                continue;
            }

            try {
                Question question = createQuestionFromSentence(sentence, keyConcepts, random);
                if (question != null) {
                    questions.add(question);
                    questionsGenerated++;
                }
            } catch (Exception e) {
                logger.warn("Failed to create question from sentence: {}", e.getMessage());
            }
        }

        while (questionsGenerated < questionCount && !keyConcepts.isEmpty()) {
            String concept = keyConcepts.get(random.nextInt(keyConcepts.size()));
            Question question = createGenericQuestion(concept, keyConcepts, random);
            questions.add(question);
            questionsGenerated++;
        }

        logger.info("Successfully generated {} questions using rule-based approach", questions.size());
        return questions;
    }

    
    private List<String> extractSentences(String text) {
        String[] rawSentences = text.split("[.!?]+");
        List<String> sentences = new ArrayList<>();

        for (String sentence : rawSentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        return sentences;
    }

    
    private List<String> extractKeyConcepts(String text) {
        Set<String> concepts = new HashSet<>();

        Pattern pattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String concept = matcher.group();
            if (!isCommonWord(concept)) {
                concepts.add(concept);
            }
        }

        Pattern quotedPattern = Pattern.compile("\"([^\"]+)\"");
        Matcher quotedMatcher = quotedPattern.matcher(text);

        while (quotedMatcher.find()) {
            concepts.add(quotedMatcher.group(1));
        }

        return new ArrayList<>(concepts);
    }

    
    private Question createQuestionFromSentence(String sentence, List<String> keyConcepts, Random random) {
        Question question = new Question();

        String keyTerm = findKeyTermInSentence(sentence, keyConcepts);

        if (keyTerm == null || keyTerm.isEmpty()) {
            return null;
        }

        String template = QUESTION_TEMPLATES.get(random.nextInt(QUESTION_TEMPLATES.size()));
        question.setQuestionText(String.format(template, keyTerm));

        String correctOption = sentence.trim();
        if (correctOption.length() > 200) {
            correctOption = correctOption.substring(0, 197) + "...";
        }

        List<String> options = new ArrayList<>();
        options.add(correctOption);
        options.addAll(generateDistractors(correctOption, keyConcepts, 3));

        Collections.shuffle(options, random);

        question.setOption1(options.get(0));
        question.setOption2(options.get(1));
        question.setOption3(options.get(2));
        question.setOption4(options.get(3));

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(correctOption)) {
                question.setCorrectAnswer(i + 1);
                break;
            }
        }

        return question;
    }

    
    private Question createGenericQuestion(String concept, List<String> allConcepts, Random random) {
        Question question = new Question();

        String template = QUESTION_TEMPLATES.get(random.nextInt(QUESTION_TEMPLATES.size()));
        question.setQuestionText(String.format(template, concept));

        List<String> options = new ArrayList<>();
        options.add("A key concept related to " + concept);
        options.add("An important term in the study material");
        options.add("A fundamental principle discussed in the text");
        options.add("A topic covered in the material");

        Collections.shuffle(options, random);

        question.setOption1(options.get(0));
        question.setOption2(options.get(1));
        question.setOption3(options.get(2));
        question.setOption4(options.get(3));
        question.setCorrectAnswer(1);

        return question;
    }

    
    private String findKeyTermInSentence(String sentence, List<String> keyConcepts) {
        for (String concept : keyConcepts) {
            if (sentence.contains(concept)) {
                return concept;
            }
        }

        String[] words = sentence.split("\\s+");
        for (String word : words) {
            if (word.length() > 4 && !isCommonWord(word)) {
                return word;
            }
        }

        return null;
    }

    
    private List<String> generateDistractors(String correctAnswer, List<String> keyConcepts, int count) {
        List<String> distractors = new ArrayList<>();

        List<String> templates = List.of(
                "This is not the correct definition",
                "An alternative but incorrect explanation",
                "A common misconception about this topic",
                "This does not accurately describe the concept");

        for (int i = 0; i < count; i++) {
            if (i < templates.size()) {
                distractors.add(templates.get(i));
            } else {
                distractors.add("Incorrect option " + (i + 1));
            }
        }

        return distractors;
    }

    
    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of(
                "The", "This", "That", "These", "Those", "What", "When", "Where",
                "Which", "Who", "Why", "How", "Can", "Could", "Should", "Would",
                "May", "Might", "Must", "Will", "Shall", "Have", "Has", "Had",
                "Do", "Does", "Did", "Is", "Are", "Was", "Were", "Been", "Being");
        return commonWords.contains(word);
    }
}

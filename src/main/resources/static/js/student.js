
const API_BASE_URL = 'http://localhost:8080/api';
let currentQuiz = null;
let questions = [];
let answers = {};

document.addEventListener('DOMContentLoaded', () => {
    loadAvailableQuizzes();
});

async function loadAvailableQuizzes() {
    const selectEl = document.getElementById('quizSelect');

    try {
        const response = await fetch(`${API_BASE_URL}/quiz`);
        const quizzes = await response.json();

        if (quizzes.length === 0) {
            selectEl.innerHTML = '<option value="">No quizzes available</option>';
        } else {
            selectEl.innerHTML = '<option value="">-- Select a quiz --</option>' +
                quizzes.map(quiz =>
                    `<option value="${quiz.id}">${quiz.title}</option>`
                ).join('');
        }
    } catch (error) {
        console.error('Error loading quizzes:', error);
        selectEl.innerHTML = '<option value="">Error loading quizzes</option>';
    }
}

async function startQuiz() {
    const studentName = document.getElementById('studentName').value.trim();
    const studentUniqueId = document.getElementById('studentUniqueId').value.trim();
    const quizId = document.getElementById('quizSelect').value;

    if (!studentName) {
        alert('Please enter your name');
        return;
    }

    if (!studentUniqueId) {
        alert('Please enter your Unique ID');
        return;
    }

    if (!quizId) {
        alert('Please select a quiz');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${quizId}`);
        const data = await response.json();

        currentQuiz = data.quiz;
        questions = data.questions;
        answers = {};

        document.getElementById('quizSelection').classList.add('hidden');
        document.getElementById('quizContainer').classList.remove('hidden');
        document.getElementById('quizTitle').textContent = currentQuiz.title;
        document.getElementById('displayStudentName').textContent = studentName;
        document.getElementById('totalQuestions').textContent = questions.length;

        renderQuestions();

    } catch (error) {
        console.error('Error loading quiz:', error);
        alert('Failed to load quiz. Please try again.');
    }
}

function renderQuestions() {
    const container = document.getElementById('questionsContainer');

    container.innerHTML = questions.map((question, index) => `
        <div class="question-card fade-in">
            <span class="question-number">Question ${index + 1}</span>
            <h3 style="margin: 1rem 0;">${question.questionText}</h3>
            
            <div class="options">
                <label class="option" onclick="selectAnswer(${question.id}, 1, this)">
                    <input type="radio" name="question-${question.id}" value="1">
                    <span>${question.option1}</span>
                </label>
                <label class="option" onclick="selectAnswer(${question.id}, 2, this)">
                    <input type="radio" name="question-${question.id}" value="2">
                    <span>${question.option2}</span>
                </label>
                <label class="option" onclick="selectAnswer(${question.id}, 3, this)">
                    <input type="radio" name="question-${question.id}" value="3">
                    <span>${question.option3}</span>
                </label>
                <label class="option" onclick="selectAnswer(${question.id}, 4, this)">
                    <input type="radio" name="question-${question.id}" value="4">
                    <span>${question.option4}</span>
                </label>
            </div>
        </div>
    `).join('');

    updateProgress();
}

function selectAnswer(questionId, answerValue, element) {
    answers[questionId] = answerValue;

    const parentCard = element.closest('.question-card');
    const allOptions = parentCard.querySelectorAll('.option');
    allOptions.forEach(opt => opt.classList.remove('selected'));
    element.classList.add('selected');

    updateProgress();
}

function updateProgress() {
    const answeredCount = Object.keys(answers).length;
    document.getElementById('currentQuestion').textContent = answeredCount;
}

async function submitQuiz() {
    const studentName = document.getElementById('studentName').value.trim();
    const studentUniqueId = document.getElementById('studentUniqueId').value.trim();

    if (Object.keys(answers).length < questions.length) {
        if (!confirm('You have not answered all questions. Submit anyway?')) {
            return;
        }
    }

    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${currentQuiz.id}/attempt`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                studentName: studentName,
                studentUniqueId: studentUniqueId,
                answers: answers
            })
        });

        if (!response.ok) {
            throw new Error('Failed to submit quiz');
        }

        const result = await response.json();
        showResults(result);

    } catch (error) {
        console.error('Error submitting quiz:', error);
        alert('Failed to submit quiz. Please try again.');
    }
}

function showResults(attempt) {
    document.getElementById('quizContainer').classList.add('hidden');
    document.getElementById('resultsContainer').classList.remove('hidden');

    document.getElementById('scoreDisplay').textContent = attempt.score;
    document.getElementById('totalScore').textContent = attempt.totalQuestions;
    document.getElementById('percentageDisplay').textContent = attempt.percentage.toFixed(1);
    document.getElementById('resultStudentName').textContent = attempt.studentName;
}

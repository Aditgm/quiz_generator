
const API_BASE_URL = 'http://localhost:8080/api';
let quizzes = [];

document.addEventListener('DOMContentLoaded', () => {
    loadQuizzes();
    loadLeaderboard();
});

async function loadQuizzes() {
    try {
        const response = await fetch(`${API_BASE_URL}/quiz`);
        quizzes = await response.json();

        const filterEl = document.getElementById('quizFilter');
        filterEl.innerHTML = '<option value="">All Quizzes (Global)</option>' +
            quizzes.map(quiz =>
                `<option value="${quiz.id}">${quiz.title}</option>`
            ).join('');
    } catch (error) {
        console.error('Error loading quizzes:', error);
    }
}

async function loadLeaderboard() {
    const loadingEl = document.getElementById('leaderboardLoading');
    const containerEl = document.getElementById('leaderboardContainer');
    const noResultsEl = document.getElementById('noResults');
    const bodyEl = document.getElementById('leaderboardBody');

    const quizId = document.getElementById('quizFilter').value;

    loadingEl.classList.remove('hidden');
    containerEl.classList.add('hidden');
    noResultsEl.classList.add('hidden');

    try {
        let url = `${API_BASE_URL}/leaderboard`;
        if (quizId) {
            url = `${API_BASE_URL}/leaderboard/quiz/${quizId}`;
        }

        const response = await fetch(url);
        const attempts = await response.json();

        loadingEl.classList.add('hidden');

        if (attempts.length === 0) {
            noResultsEl.classList.remove('hidden');
        } else {
            containerEl.classList.remove('hidden');
            bodyEl.innerHTML = attempts.map((attempt, index) => {
                const rank = index + 1;
                const quiz = quizzes.find(q => q.id === attempt.quizId);
                const quizTitle = quiz ? quiz.title : `Quiz #${attempt.quizId}`;

                return `
                    <tr class="fade-in">
                        <td>
                            ${rank <= 3 ? getTrophy(rank) : ''}
                            <span class="rank rank-${rank <= 3 ? rank : ''}">${rank}</span>
                        </td>
                        <td><strong>${attempt.studentName}</strong></td>
                        <td style="color: var(--text-secondary);">${quizTitle}</td>
                        <td><strong>${attempt.score}/${attempt.totalQuestions}</strong></td>
                        <td>
                            <div style="display: flex; align-items: center; gap: 0.5rem;">
                                <div style="flex: 1; background: rgba(255,255,255,0.1); border-radius: 10px; height: 8px; overflow: hidden;">
                                    <div style="width: ${attempt.percentage}%; height: 100%; background: var(--primary-gradient);"></div>
                                </div>
                                <span style="min-width: 50px;"><strong>${attempt.percentage.toFixed(1)}%</strong></span>
                            </div>
                        </td>
                        <td style="color: var(--text-secondary);">
                            ${new Date(attempt.attemptDate).toLocaleDateString()}
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (error) {
        console.error('Error loading leaderboard:', error);
        loadingEl.classList.add('hidden');
        noResultsEl.classList.remove('hidden');
    }
}

function getTrophy(rank) {
    const trophies = {
        1: '🥇',
        2: '🥈',
        3: '🥉'
    };
    return `<span class="trophy">${trophies[rank] || ''}</span>`;
}

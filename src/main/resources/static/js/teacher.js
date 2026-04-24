
const API_BASE_URL = 'http://localhost:8080/api';
let currentMaterialId = null;

function formatDateTime(dateValue) {
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
        return 'Unknown time';
    }
    return date.toLocaleString([], {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function normalizeQuizTitleForDisplay(title) {
    const rawTitle = (title || '').trim();
    if (!rawTitle) {
        return 'Untitled Quiz';
    }

    if (rawTitle.toLowerCase().startsWith('quiz:')) {
        const legacyTitle = rawTitle.substring(5).trim();
        const extensionIndex = legacyTitle.lastIndexOf('.');
        if (extensionIndex > 0) {
            return legacyTitle.substring(0, extensionIndex);
        }
        return legacyTitle || 'Untitled Quiz';
    }

    return rawTitle;
}

document.addEventListener('DOMContentLoaded', () => {
    loadMaterials();
    loadQuizzes();
    setupFileUpload();
});

function setupFileUpload() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');

    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFileUpload(files[0]);
        }
    });

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFileUpload(e.target.files[0]);
        }
    });
}

async function handleFileUpload(file) {
    const uploadStatus = document.getElementById('uploadStatus');
    
    const validExtensions = ['txt', 'pdf', 'docx'];
    const fileExtension = file.name.split('.').pop().toLowerCase();
    
    if (!validExtensions.includes(fileExtension)) {
        showMessage(uploadStatus, 'error', 'Invalid file type. Please upload TXT, PDF, or DOCX files.');
        return;
    }

    if (file.size > 10 * 1024 * 1024) {
        showMessage(uploadStatus, 'error', 'File size exceeds 10MB limit.');
        return;
    }

    showMessage(uploadStatus, 'info', 'Uploading and processing file...');

    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${API_BASE_URL}/materials/upload`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Upload failed');
        }

        const material = await response.json();
        const extractedLength = (material.extractedText || '').length;
        showMessage(uploadStatus, 'success', `File uploaded successfully! Extracted ${extractedLength} characters.`);
        
        loadMaterials();
        
        document.getElementById('fileInput').value = '';

    } catch (error) {
        showMessage(uploadStatus, 'error', `Upload failed: ${error.message}`);
    }
}

async function loadMaterials() {
    const loadingEl = document.getElementById('materialsLoading');
    const listEl = document.getElementById('materialsList');
    const noMaterialsEl = document.getElementById('noMaterials');

    try {
        const response = await fetch(`${API_BASE_URL}/materials`);
        const materials = await response.json();

        loadingEl.classList.add('hidden');

        if (materials.length === 0) {
            listEl.classList.add('hidden');
            noMaterialsEl.classList.remove('hidden');
        } else {
            noMaterialsEl.classList.add('hidden');
            listEl.classList.remove('hidden');
            listEl.innerHTML = materials.map(material => `
                <li class="list-item">
                    <div>
                        <strong>${material.fileName}</strong>
                        <p style="color: var(--text-secondary); font-size: 0.9rem; margin-top: 0.25rem;">
                            Uploaded: ${new Date(material.uploadDate).toLocaleDateString()} | 
                            ${(material.extractedText || '').length} characters
                        </p>
                    </div>
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn btn-primary" onclick="openQuizModal(${material.id})">
                            Generate Quiz
                        </button>
                        <button class="btn" style="background: var(--secondary-gradient);" onclick="deleteMaterial(${material.id})">
                            Delete
                        </button>
                    </div>
                </li>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading materials:', error);
        loadingEl.classList.add('hidden');
    }
}

async function loadQuizzes() {
    const loadingEl = document.getElementById('quizzesLoading');
    const listEl = document.getElementById('quizzesList');
    const noQuizzesEl = document.getElementById('noQuizzes');

    try {
        const response = await fetch(`${API_BASE_URL}/quiz`);
        const quizzes = await response.json();

        loadingEl.classList.add('hidden');

        if (quizzes.length === 0) {
            listEl.classList.add('hidden');
            noQuizzesEl.classList.remove('hidden');
        } else {
            noQuizzesEl.classList.add('hidden');
            listEl.classList.remove('hidden');
            listEl.innerHTML = quizzes.map(quiz => `
                <li class="list-item">
                    <div>
                        <strong>${normalizeQuizTitleForDisplay(quiz.title)}</strong>
                        <p style="color: var(--text-secondary); font-size: 0.9rem; margin-top: 0.25rem;">
                            Created: ${formatDateTime(quiz.createdDate)} | 
                            Method: <span class="method-chip">${quiz.generationMethod}</span>
                        </p>
                    </div>
                    <div>
                        <a href="/student.html" class="btn btn-success">
                            View Quiz
                        </a>
                    </div>
                </li>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading quizzes:', error);
        loadingEl.classList.add('hidden');
    }
}

function openQuizModal(materialId) {
    currentMaterialId = materialId;
    const modal = document.getElementById('quizModal');
    const titleInput = document.getElementById('quizTitle');
    const countInput = document.getElementById('questionCount');

    if (titleInput) {
        titleInput.value = '';
    }
    if (countInput) {
        countInput.value = '10';
    }

    modal.classList.remove('hidden');
    modal.style.display = 'flex';

    if (titleInput) {
        titleInput.focus();
    }
}

function closeQuizModal(resetSelection = true) {
    const modal = document.getElementById('quizModal');
    modal.classList.add('hidden');
    modal.style.display = 'none';
    if (resetSelection) {
        currentMaterialId = null;
    }
}

async function confirmGenerateQuiz() {
    const questionCount = document.getElementById('questionCount').value;
    const quizTitle = (document.getElementById('quizTitle')?.value || '').trim();
    const selectedMaterialId = currentMaterialId;

    if (!selectedMaterialId) {
        const uploadStatus = document.getElementById('uploadStatus');
        showMessage(uploadStatus, 'error', 'Please select a study material before generating a quiz.');
        return;
    }

    if (quizTitle.length > 100) {
        const uploadStatus = document.getElementById('uploadStatus');
        showMessage(uploadStatus, 'error', 'Quiz title must be 100 characters or fewer.');
        return;
    }

    closeQuizModal(false);

    const uploadStatus = document.getElementById('uploadStatus');
    showMessage(uploadStatus, 'info', 'Generating quiz... This may take a moment.');

    try {
        const query = new URLSearchParams({ questionCount });
        if (quizTitle) {
            query.append('quizTitle', quizTitle);
        }

        const response = await fetch(`${API_BASE_URL}/quiz/generate/${selectedMaterialId}?${query.toString()}`, {
            method: 'POST'
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ error: 'Quiz generation failed' }));
            throw new Error(error.error || 'Quiz generation failed');
        }

        const result = await response.json();
        const generatedTime = formatDateTime(result.quiz?.createdDate);
        showMessage(uploadStatus, 'success', 
            `Quiz "${result.quiz.title}" generated successfully with ${result.questions.length} questions at ${generatedTime} using ${result.quiz.generationMethod} method.`);
        
        loadQuizzes();

    } catch (error) {
        showMessage(uploadStatus, 'error', `Quiz generation failed: ${error.message}`);
    } finally {
        currentMaterialId = null;
    }
}

async function deleteMaterial(materialId) {
    if (!confirm('Are you sure you want to delete this material?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/materials/${materialId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadMaterials();
        }
    } catch (error) {
        console.error('Error deleting material:', error);
    }
}

function showMessage(element, type, message) {
    const alertClass = type === 'error'
        ? 'alert-error'
        : type === 'info'
            ? 'alert-info'
            : 'alert-success';
    element.innerHTML = `<div class="alert ${alertClass}">${message}</div>`;
    element.classList.remove('hidden');
}

# AI Quiz Generator

An intelligent quiz generation system that automatically creates Multiple Choice Questions (MCQs) from study materials using Generative AI with a rule-based fallback mechanism.

## Features

- 🤖 **AI-Powered Quiz Generation** - Uses OpenAI GPT to create intelligent MCQs
- 🔄 **Fallback Mechanism** - Rule-based generation when AI is unavailable
- 📄 **Multiple File Formats** - Supports TXT, PDF, and DOCX files
- ✏️ **Online Quiz Attempts** - Students can take quizzes and get instant results
- 🏆 **Leaderboard System** - Track top performers and competitive learning
- 💎 **Modern UI** - Premium dark theme with glassmorphism effects

## Technology Stack

### Backend
- Java 17
- Spring Boot 3.2.0
- MySQL / H2 Database
- Apache POI (Word documents)
- Apache PDFBox (PDF parsing)
- OpenAI Java Client

### Frontend
- HTML5, CSS3, JavaScript
- Modern responsive design
- Glassmorphism UI effects

## Prerequisites

Before running the application, ensure you have:

1. **Java 17 or higher** - [Download here](https://www.oracle.com/java/technologies/downloads/)
2. **Maven 3.6+** - [Download here](https://maven.apache.org/download.cgi)
3. **MySQL** (optional) - H2 in-memory database is used by default

## Installation

### 1. Install Maven

**On macOS:**
```bash
# Install Homebrew if not installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Maven
brew install maven
```

**On Windows:**
- Download Maven from https://maven.apache.org/download.cgi
- Extract and add to PATH

**Verify installation:**
```bash
mvn --version
```

### 2. Clone/Navigate to Project
```bash
cd "/Users/raunaksingh/Documents/AI quiz generator"
```

### 3. Configure OpenAI API (Optional)

For AI-based quiz generation, set your OpenAI API key:

**Option 1: Environment Variable**
```bash
export OPENAI_API_KEY="your-api-key-here"
```

**Option 2: Edit application.properties**
Edit `src/main/resources/application.properties` and add:
```properties
openai.api.key=your-api-key-here
```

> **Note:** If no API key is provided, the system will automatically use the rule-based fallback mechanism.

### 4. Build the Application
```bash
mvn clean package
```

### 5. Run the Application
```bash
mvn spring-boot:run
```

Or run the JAR file:
```bash
java -jar target/ai-quiz-generator-1.0.0.jar
```

The application will start on **http://localhost:8080**

## Usage

### For Teachers

1. Navigate to **http://localhost:8080/teacher.html**
2. Upload study materials (TXT, PDF, or DOCX files)
3. Click "Generate Quiz" on any uploaded material
4. Choose the number of questions (5-20)
5. The system will generate MCQs using AI or rule-based logic

### For Students

1. Navigate to **http://localhost:8080/student.html**
2. Enter your name
3. Select a quiz from the dropdown
4. Answer all questions
5. Submit to see your score instantly

### View Leaderboard

1. Navigate to **http://localhost:8080/leaderboard.html**
2. View top performers globally or filter by specific quiz
3. See rankings with scores and percentages

## Database Configuration

### Using H2 (Default - No Setup Required)

The application uses H2 in-memory database by default. Access the H2 console at:
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:quizdb`
- Username: `sa`
- Password: (leave empty)

### Using MySQL (Production)

1. Create a MySQL database:
```sql
CREATE DATABASE quiz_generator_db;
```

2. Edit `src/main/resources/application.properties`:
```properties
# Comment out H2 configuration
#spring.datasource.url=jdbc:h2:mem:quizdb

# Uncomment MySQL configuration
spring.datasource.url=jdbc:mysql://localhost:3306/quiz_generator_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

## API Endpoints

### Study Materials
- `POST /api/materials/upload` - Upload study material
- `GET /api/materials` - List all materials
- `GET /api/materials/{id}` - Get material by ID
- `DELETE /api/materials/{id}` - Delete material

### Quizzes
- `POST /api/quiz/generate/{materialId}` - Generate quiz
- `GET /api/quiz` - List all quizzes
- `GET /api/quiz/{id}` - Get quiz with questions

### Quiz Attempts
- `POST /api/quiz/{id}/attempt` - Submit quiz attempt
- `GET /api/quiz/{id}/attempts` - Get attempts for quiz

### Leaderboard
- `GET /api/leaderboard` - Global leaderboard
- `GET /api/leaderboard/quiz/{id}` - Quiz-specific leaderboard

## Testing

A sample study material file is included: `sample-material.txt`

### Test the System:

1. Start the application
2. Upload `sample-material.txt` via the teacher dashboard
3. Generate a quiz (will use rule-based method if no API key)
4. Take the quiz as a student
5. View results on the leaderboard

## Troubleshooting

### Maven not found
Install Maven using the instructions in the Installation section.

### Port 8080 already in use
Change the port in `application.properties`:
```properties
server.port=8081
```

### AI generation fails
- Check your OpenAI API key is valid
- Ensure you have API credits
- The system will automatically fall back to rule-based generation

### File upload fails
- Check file size is under 10MB
- Ensure file format is TXT, PDF, or DOCX
- Check application logs for detailed error messages

## Project Structure

```
ai-quiz-generator/
├── src/
│   ├── main/
│   │   ├── java/com/quizgen/
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── QuizGeneratorApplication.java
│   │   └── resources/
│   │       ├── static/          # Frontend files
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── *.html
│   │       ├── application.properties
│   │       └── schema.sql
│   └── test/                    # Test files
├── pom.xml                      # Maven configuration
└── README.md
```

## Contributing

This is an educational project. Feel free to extend it with:
- Additional file format support (images with OCR)
- More quiz types (True/False, Fill in the blanks)
- User authentication
- Quiz scheduling
- Analytics dashboard

## License

This project is for educational purposes.

## Support

For issues or questions, please check the application logs in the console where the application is running.

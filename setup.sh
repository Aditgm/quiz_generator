#!/bin/bash

# AI Quiz Generator - Quick Setup Script

echo "🎓 AI Quiz Generator - Setup Script"
echo "===================================="
echo ""

# Check Java
echo "Checking Java installation..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "✅ Java found: $JAVA_VERSION"
else
    echo "❌ Java not found. Please install Java 17 or higher."
    exit 1
fi

# Check Maven
echo ""
echo "Checking Maven installation..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo "✅ Maven found: $MVN_VERSION"
else
    echo "❌ Maven not found."
    echo ""
    echo "Installing Maven using Homebrew..."
    
    # Check if Homebrew is installed
    if ! command -v brew &> /dev/null; then
        echo "Installing Homebrew first..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    
    echo "Installing Maven..."
    brew install maven
    
    if command -v mvn &> /dev/null; then
        echo "✅ Maven installed successfully!"
    else
        echo "❌ Maven installation failed. Please install manually from https://maven.apache.org/download.cgi"
        exit 1
    fi
fi

# Build the project
echo ""
echo "Building the project..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "🚀 Starting the application..."
    echo ""
    mvn spring-boot:run
else
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

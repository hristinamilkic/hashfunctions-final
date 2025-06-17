#!/bin/bash

# proveri da li je java instalirana
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed. Please install Java 17 or later."
    exit 1
fi

# proveri da li je maven instaliran
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven 3.6 or later."
    exit 1
fi

echo "Building the project..."
mvn clean package

if [ $? -eq 0 ]; then
    echo "Starting the application..."
    mvn javafx:run
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi 
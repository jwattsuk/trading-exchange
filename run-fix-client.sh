#!/bin/bash

# FIX Test Client Runner Script
# This script compiles and runs the FIX test client

echo "Building and running FIX Test Client..."

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Check if we need to compile
if [ ! -f "target/classes/com/jwattsuk/tradingexchange/FixTestClient.class" ]; then
    echo "Compiling project..."
    mvn compile
else
    echo "Using existing compiled classes..."
fi

if [ $? -ne 0 ]; then
    echo "Error: Compilation failed"
    exit 1
fi

# Run the test client
echo "Starting FIX test client..."
echo "Usage: $0 [host] [port]"
echo "Default: localhost:5001"
echo ""

# Parse command line arguments
HOST=${1:-localhost}
PORT=${2:-5001}

echo "Connecting to $HOST:$PORT"
echo "Press Ctrl+C to stop the client"
echo ""

# Run the client
mvn exec:java -Dexec.mainClass="com.jwattsuk.tradingexchange.FixTestClient"

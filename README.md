# Hash Functions Analysis Tool

A comprehensive Java application for analyzing and comparing various cryptographic hash functions. This tool provides detailed insights into hash function performance, security characteristics, and statistical properties.

## Features

### Hash Functions

- MD5 (Message Digest 5)
- SHA-256 (Secure Hash Algorithm 256-bit)
- SHA-3 (Keccak)
- Blake2b
- Blake3
- RIPEMD-160

### Analysis Capabilities

1. **Performance Analysis**

   - Speed benchmarking
   - Processing time measurement
   - Comparative performance charts

2. **Security Analysis**

   - Collision resistance testing
   - Avalanche effect analysis
   - Bit distribution analysis

3. **Visualization**
   - Interactive charts and graphs
   - Comparative data tables
   - Real-time results display

### User Interface

- Modern JavaFX-based GUI
- Tabbed interface for organized results
- Progress tracking during analysis
- Export functionality for results

## Application Workflow

1. **Launch the Application**

   ```bash
   ./run.sh
   ```

2. **Select Files for Analysis**

   - Click "Select Files" to choose input files
   - Multiple files can be selected for batch analysis
   - Files are processed in parallel for efficiency

3. **View Results**

   - **Results Tab**: Shows overall performance metrics

     - Speed comparison chart
     - Avalanche effect visualization
     - Collision rate analysis

   - **Detailed Analysis Tab**: Provides in-depth information

     - Collision test results with detailed statistics
     - Avalanche effect measurements and standard deviation
     - Bit distribution patterns for each hash function

   - **Settings Tab**: Configure analysis parameters
     - Number of test iterations
     - Thread count for parallel processing
     - Test data generation options

4. **Export Results**
   - Click "Export" to save analysis results
   - Results are saved in CSV format
   - Includes all metrics and test results

## Requirements

- Java 17 or later
- Maven 3.6 or later

## Building and Running

1. **Build the Project**

   ```bash
   mvn clean package
   ```

2. **Run the Application**
   ```bash
   ./run.sh
   ```

## Implementation Details

### Core Components

- `HashFunction`: Base interface for all hash implementations
- `BaseHashFunction`: Abstract class providing common functionality
- `HashImplementations`: Consolidated implementation of all hash functions
- `HashBenchmark`: Performance testing and benchmarking
- `CollisionTest`: Collision resistance analysis
- `AvalancheTest`: Avalanche effect measurement
- `TestDataGenerator`: Generates various test data patterns

### Testing Features

- Random data testing
- Sequential data analysis
- Pattern-based testing
- Bit-flip analysis
- Similarity testing

### GUI Components

- Main window with tabbed interface
- Interactive charts using JavaFX Charts
- Data tables with sortable columns
- Progress tracking and status updates
- File selection dialog
- Export functionality


# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD (Test-Driven Development) practice project for a point management system. The project is built with Spring Boot and focuses on implementing user point charging, usage, and history tracking features using TDD methodology.

## Build and Test Commands

### Running the Application
```bash
./gradlew bootRun
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "PointServiceTest"

# Run a specific test method
./gradlew test --tests "PointServiceTest.사용자_포인트_조회_테스트"
```

### Building the Project
```bash
# Build the project
./gradlew build

# Build without running tests
./gradlew build -x test

# Clean and build
./gradlew clean build
```

### Code Coverage
```bash
# Generate JaCoCo coverage report
./gradlew test jacocoTestReport

# Coverage report location: build/reports/jacoco/test/html/index.html
```

## Architecture Overview

### Layer Structure
The project follows a standard Spring Boot 3-layer architecture:
- **Controller Layer** (`io.hhplus.tdd.point.PointController`): REST endpoints for point operations
- **Service Layer** (`io.hhplus.tdd.point.PointService`): Business logic for point management
- **Data Layer** (`io.hhplus.tdd.database`): In-memory table implementations

### Key Architectural Points

**In-Memory Database Tables**
The project uses in-memory HashMap/List-based table implementations instead of a real database:
- `UserPointTable`: Stores user point balances (HashMap-based)
- `PointHistoryTable`: Stores point transaction history (List-based)
- Both tables include artificial throttling (200-300ms random delays) to simulate database latency
- **Important**: These table classes should NOT be modified; only use their public APIs

**Data Initialization**
`DataInitializer` (implements `CommandLineRunner`) populates initial test data on application startup. Note that there's a bug in the current implementation where user2 uses ID 1 instead of 2 (line 29 in DataInitializer.java).

**Domain Models**
- `UserPoint`: Java record representing user point balance (id, point, updateMillis)
- `PointHistory`: Java record representing transaction history (id, userId, amount, type, updateMillis)
- `TransactionType`: Enum for CHARGE/USE operations

**Global Exception Handling**
`ApiControllerAdvice` provides centralized exception handling using `@RestControllerAdvice`, returning 500 status with generic error messages.

### Current Implementation Status

The controller has TODOs for implementing:
1. User point lookup (partially implemented in service)
2. Point history lookup (not implemented in service)
3. Point charging (not implemented in service)
4. Point usage (not implemented in service)

The service layer currently only has `getPointById()` implemented. The `charge()` and `use()` methods exist but have no implementation and return null.

### Testing Approach

The project uses:
- JUnit 5 (`@ExtendWith(MockitoExtension.class)`)
- Mockito for mocking dependencies
- AssertJ for fluent assertions

Test location: `src/test/java/io/hhplus/tdd/PointServiceTest.java`

When writing tests, follow the given-when-then pattern as shown in existing tests.

## Technology Stack

- Java 17
- Spring Boot 3.x
- Gradle (Kotlin DSL)
- Lombok for boilerplate reduction
- JaCoCo for code coverage
- JUnit 5 + Mockito + AssertJ for testing

## Development Notes

### Windows Environment
This project is being developed on Windows (note the gradlew.bat script). Use `gradlew.bat` instead of `./gradlew` when running commands in Windows Command Prompt or PowerShell (though Git Bash supports `./gradlew`).

### Branch Strategy
Current working branch is `feat/week1-step1` which appears to be part of a weekly step-by-step learning approach.

### Test Configuration
The test task is configured with `ignoreFailures = true` in build.gradle.kts, allowing the build to continue even if tests fail. This is useful during TDD practice but should be reconsidered for production code.

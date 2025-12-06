# IV1351 Project Task 3:

A Java command-line application for managing University Course logic, implementing strict Layered Architecture and ACID transaction handling.

## Overview

This project implements **Task 3 (Higher Grade)** requirements for the IV1351 course. It focuses on:
-   **ACID Transactions**: Ensuring data integrity with `commit`/`rollback` and `SELECT ... FOR UPDATE` locking.
-   **Layered Architecture**: Strict separation of concerns (View, Controller, Model, Integration).
-   **Data Access**: Pure CRUD operations in the DAO layer (no business logic).

## Architecture

The project has been consolidated into a minimal 3-file structure under `se.kth.iv1351.school`:

1.  **`SchoolApp.java`**:
    *   **Startup**: `main` method.
    *   **View**: `SchoolInterpreter` class (User Interface).
    *   **Controller**: `Controller` class (Transaction boundaries, Use cases).
    *   **Service**: `TeacherAllocationService` class (Business rules like Max 4 Instances).
2.  **`SchoolModel.java`**:
    *   **DTOs**: Weak entities for data transfer (`ActivityDTO`, `CostReportDTO`, etc.).
    *   **Exceptions**: `SchoolDBException`.
    *   **Logic**: Domain logic inside DTOs (e.g., calculating effective hours).
3.  **`SchoolDAO.java`**:
    *   **Integration**: Direct database interactions using JDBC.
    *   **CRUD Only**: No business rules.

## Database

The application requires a PostgreSQL database with the schema defined in `schema.sql`.
Sample data is provided in `seed.sql`.

## Prerequisites

-   Java JDK 8+
-   **PostgreSQL JDBC Driver**:
    1.  Download the PostgreSQL JDBC driver (e.g., `postgresql-42.2.20.jar`) from the [official website](https://jdbc.postgresql.org/download/).
    2.  Create a folder named `lib` in the root directory of this project.
    3.  Place the downloaded `.jar` file inside the `lib` folder.
    *   *Note: The commands below assume the file is named `postgresql-42.2.20.jar`. If you download a newer version, update the commands accordingly.*

## How to Run

1.  **Compile**:
    ```powershell
    javac -d . se/kth/iv1351/school/*.java
    ```

2.  **Run**:
    ```powershell
    # Windows (PowerShell)
    java -cp ".;lib/postgresql-42.2.20.jar" se.kth.iv1351.school.SchoolApp

    # Linux/Mac
    java -cp ".:lib/postgresql-42.2.20.jar" se.kth.iv1351.school.SchoolApp
    ```

## Commands

Once the application is running, you can use the following commands:

*   **`cost <courseCode> <year> <period>`**:
    *   Shows the planned vs actual cost for a course instance.
    *   *Example*: `cost IV1351 2022 1`

*   **`update <courseCode> <year> <period>`**:
    *   Adds 100 students to the course instance.
    *   Recalculates derived hours and scales allocations.
    *   Shows cost Before and After.
    *   *Transaction*: Uses Locking Read to prevent lost updates.

*   **`allocate <teacherName> <courseCode> <activityId> <period> <year>`**:
    *   Allocates a teacher to a specific activity.
    *   *Rule*: Fails if the teacher is already in 4 instances in that period.

*   **`deallocate <allocationId>`**:
    *   Removes a teacher allocation (Soft delete).

*   **`exercise <courseCode> <year> <period> <hours>`**:
    *   Adds a new "Exercise" activity to the course instance.

*   **`quit`**: Exits the application.

## Design Decisions

-   **Consolidation**: Files were grouped to minimize file count while maintaining logical separation.
-   **Logic Placement**:
    -   *Derivation Logic*: Moved to `ActivityDTO` (Model).
    -   *Scaling Logic*: Moved to `Controller`.
    -   *Rules*: Enforced in `TeacherAllocationService` called by `Controller`.
-   **Security**: PreparedStatement used everywhere to prevent SQL Injection.

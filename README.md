# IV1351 Task 3 (Dajue Qiu Only)

A command-line application for managing course layout and teaching allocation.

## To run, you need:

- Java 14 or higher
- Maven 3.6+
- PostgreSQL 12+

## Database Setup

1. Connect to PostgreSQL and run the database creation script:
   ```sql
   \i src/main/resources/Create_database.sql
   ```

2. Connect to the `dsp_university` database and run the schema:
   ```sql
   \c dsp_university
   \i src/main/resources/Schema.sql
   ```

3. Seed the database with initial data:
   ```sql
   \i src/main/resources/Seed.sql
   ```

## Compiling and Running

```bash
mvn clean compile
mvn exec:java
```

## Available Commands

| Command | Description |
|---------|-------------|
| `help` | Show all available commands |
| `quit` | Exit the application |
| `list instances` | List all course instances |
| `list activities` | List all teaching activities |
| `list allocations <employee_id>` | List allocations for a teacher |
| `cost <instance_id>` | Compute planned vs actual teaching cost |
| `increase <instance_id> <count>` | Increase student count |
| `allocate <emp_id> <instance_id> <activity_id> <hours>` | Allocate teaching |
| `deallocate <emp_id> <instance_id> <activity_id>` | Remove allocation |
| `newactivity <name> <factor>` | Create new teaching activity |
| `associate <instance_id> <activity_id> <hours>` | Add planned activity |

# Commands I used to generate the sample printout in my report:
list instances 2025
cost 2025-50001
increase 2025-50001 100
cost 2025-50001
list activities
newactivity Exercise 2.0
list activities
associate 2025-50001 8 30
allocate 6 2025-50001 8 60
showactivity Exercise
allocate 1 2025-50413 1 10
quit

## Architecture

This project follows the MVC + Layer pattern:

- **startup**: Application entry point
- **controller**: Business logic layer
- **integration**: Data access layer (DAO)
- **model**: Domain entities and DTOs
- **view**: Command-line interface

## Author

Dajue Qiu

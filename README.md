# Local Development Setup

## Prerequisites

To run locally, you need the following:

- Java 25
- Maven (a version compatible with java 25)
- PostgreSQL 17 or newer

You can download PostgreSQL from [PostgreSQL Downloads](https://www.postgresql.org/download/).

pgAdmin 4 is also recommended. It provides an interface to put in queries for troubleshooting.
If you just do the default installation, it should come with it (but make sure the box is checked)

postgresql will prompt you to enter a password to access the application. There is no recovery method,
so you need to have it saved somewhere.

## 1. Create a Local PostgreSQL Database

Everyone who is working locally will have to set up their own db to work with for their testing
purposes, a shared db will also be deployed on azure. no need to add your own data to the db,
as Flyway takes care of that.

After installing PostgreSQL, open pgAdmin4 and connect to your local PostgreSQL server.

1. Expand **Servers** in the left sidebar.
2. Expand your PostgreSQL server.
3. Right-click **Databases**.
4. Select **Create → Database**.
5. Set the database name to:

   `perkmanager`

6. Set the owner to `postgres`.
7. Click **Save**.

Each developer will have to create their own local `perkmanager` database.

## 3. Configure Database Connection

There is an application.properties file, you will need to make sure it is configured properly.
If you used the defaults on your postgresql installation, then the existing file should work.
NOTE: you do need to change placeholder password to the password you set during installation

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/perkmanager}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:WHATEVER_YOU_SET_YOUR_PW_TO}
```

ATTENTION: DO NOT COMMIT YOU application.properties OR ELSE WE ALL SEE YOUR PASSWORD

## 4. Run the Application

From the project directory, run:

```bash
mvn spring-boot:run
```

When the application starts, Flyway automatically checks and applies any 
database migrations that have not already been executed. So you don't have to
create any tables or data yourself

## 5. Verify the Application

Once Spring Boot has started, open:

http://localhost:8080

This is the url where the application will be run on.

## 6. Database Migrations (Flyway)

Database schema changes are managed through Flyway.

Migration files are located in:

```text
src/main/resources/db/migration/
```

Migration filenames must follow this convention:

```text
V1__create_perks_and_memberships.sql
V2__insert_initial_data.sql
V3__add_perk_category.sql
```

or just generally:
```text
Vn__description.sql
```

When new database changes are required:

1. Create a new migration file with the next version number.
2. Write the SQL statements for the required changes.
3. Commit the migration file to GitHub.
4. When other developers pull the changes and run the application, Flyway automatically applies the new migration to their local databases.

**Important:** Do not modify migrations that have already been applied. Always create a new migration for additional database changes.
If you do, Flyway will give an error, and you will have to undo your changes and move them to a new migration file.

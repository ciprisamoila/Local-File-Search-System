# Local File Search System

Local File Search System is a Java desktop application for indexing and searching files on a local machine.  
It supports text and image-oriented search use cases, including file-name/content search for text files and color-based lookup for images.

## Purpose

The project provides a local search experience backed by crawling, indexing, and ranked query results, with support for complex query expressions.

## Usage

- Run the app (JavaFX): `./mvnw javafx:run`
- Run tests: `./mvnw test`

## Technology

- Java 23 + JavaFX
- Maven build
- PostgreSQL (index/query persistence)
- Apache Tika (file content extraction)
- org.json (JSON handling)

## Architecture

See the architecture documentation in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

# Crypto Portfolio API

A RESTful API server built with Kotlin and Ktor for tracking cryptocurrency prices and managing user portfolios.

## Features

- User authentication with JWT
- Real-time cryptocurrency data from external API
- Historical price data tracking for coins
- Automatic periodic updates of coin price data
- Portfolio management with profit/loss tracking
- RESTful API endpoints
- PostgreSQL database integration

## Prerequisites

- JDK 17 or higher
- Gradle (or use the included Gradle wrapper)
- PostgreSQL database

## Getting Started

### Database Setup

1. Copy the sample database properties file:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```

2. Edit the `db.properties` file with your PostgreSQL connection details:
   ```properties
   db.url=jdbc:postgresql://hostname:5432/database_name
   db.user=username
   db.password=password
   db.driver=org.postgresql.Driver
   db.pool.max=10
   ```

### Running the Application

```bash
# Using Gradle wrapper
./gradlew run
```

The server will start on port 8080.

## API Endpoints

### Authentication

- `POST /auth/register` - Register a new user
  - Request body: `{ "username": "user", "email": "user@example.com", "password": "password" }`
  - Response: `{ "token": "jwt-token", "userId": 1, "username": "user" }`

- `POST /auth/login` - Login and get JWT token
  - Request body: `{ "username": "user", "password": "password" }`
  - Response: `{ "token": "jwt-token", "userId": 1, "username": "user" }`

### Coin Data

- `GET /api/coins` - Get all coins with their latest stats
- `GET /api/coins/date/{date}` - Get all coins with stats for a specific date (format: yyyy-MM-dd)
- `GET /api/coins/{id}` - Get coin by ID with its latest stats
- `GET /api/coins/{id}/date/{date}` - Get coin by ID with stats for a specific date (format: yyyy-MM-dd)
- `GET /api/coins/{id}/history?from={fromDate}&to={toDate}` - Get historical data for a coin within a date range
- `GET /api/coins/ticker/{ticker}` - Get coin by ticker with its latest stats

### Portfolio Management (Requires Authentication)

- `GET /api/portfolio` - Get portfolio summary with total investment, current value, and profit/loss
- `GET /api/portfolio/entries` - Get all portfolio entries
- `GET /api/portfolio/entries/{id}` - Get portfolio entry by ID
- `POST /api/portfolio/entries` - Create a new portfolio entry
  - Request body: `{ "cryptoId": 1, "quantity": "1.5", "entryPrice": "50000", "notes": "Initial investment" }`
- `PUT /api/portfolio/entries/{id}` - Update a portfolio entry
- `DELETE /api/portfolio/entries/{id}` - Delete a portfolio entry

## Authentication

All portfolio endpoints require authentication. To authenticate, include the JWT token in the Authorization header:

```
Authorization: Bearer your-jwt-token
```

## Database Schema

### Users Table
- `id` - Integer, Primary Key, Auto Increment
- `username` - Varchar(50), Unique
- `email` - Varchar(255), Unique
- `password_hash` - Varchar(255)
- `created_at` - Timestamp
- `updated_at` - Timestamp

### Coins Table
- `id` - Integer, Primary Key, Auto Increment
- `ticker` - Varchar(20), Unique
- `name` - Varchar(100)

### CoinStats Table
- `id` - Integer, Primary Key, Auto Increment
- `coin_id` - Integer, Foreign Key to Coins.id
- `current_price` - Decimal(20,8)
- `market_cap` - Decimal(30,2), Nullable
- `volume_24h` - Decimal(30,2), Nullable
- `last_updated` - Timestamp
- `date` - Date, Default: current date
- Unique Index on (coin_id, date) to ensure one record per coin per day

### Portfolio Entries Table
- `id` - Integer, Primary Key, Auto Increment
- `user_id` - Integer, Foreign Key to Users.id
- `crypto_id` - Integer, Foreign Key to Coins.id
- `quantity` - Decimal(20,8)
- `entry_price` - Decimal(20,8)
- `entry_date` - Timestamp
- `notes` - Text, Nullable
- `created_at` - Timestamp
- `updated_at` - Timestamp

## Development

This project uses Gradle for dependency management and building. The main dependencies are:

- Ktor: Framework for building asynchronous servers
- kotlinx.serialization: JSON serialization/deserialization
- Exposed: Kotlin SQL framework
- PostgreSQL JDBC Driver: For database connectivity
- HikariCP: High-performance JDBC connection pool
- Auth0 JWT: For JWT authentication
- BCrypt: For password hashing
- Logback: Logging framework

## Background Tasks

The application includes a background task system that periodically fetches cryptocurrency data from an external API and updates the CoinStats table. The main components are:

### CoinDataFetchTask

This task runs at a configurable interval (default: 5 minutes) and performs the following actions:
1. Retrieves all coins from the database
2. For each coin, fetches the latest price data from the external API
3. Updates the CoinStats table with the new data for the current date
4. Optionally cleans up old historical data (configurable retention period)

### TaskManager

Manages the lifecycle of all background tasks in the application:
- Starts tasks when the application starts
- Stops tasks when the application is shutting down
- Provides methods to manually start/stop individual tasks

### Customizing the API Integration

To integrate with your specific cryptocurrency data API:
1. Modify the `fetchCoinDataFromApi` method in `CoinDataFetchTask.kt`
2. Update the `CoinApiResponse` data class to match your API's response structure
3. Adjust the fetch interval as needed by modifying the `fetchInterval` parameter

## Historical Data

The application stores historical price data for each coin on a daily basis. This allows for:
- Tracking price changes over time
- Analyzing trends and patterns
- Calculating performance metrics
- Generating charts and reports

You can access historical data through the API endpoints:
- `/api/coins/date/{date}` - Get all coins with stats for a specific date
- `/api/coins/{id}/date/{date}` - Get a specific coin with stats for a specific date
- `/api/coins/{id}/history?from={fromDate}&to={toDate}` - Get historical data for a coin within a date range

The system automatically manages historical data storage and provides options for data retention policies.

## Data Source

Cryptocurrency data is fetched from an external API by the periodic task. The task updates the CoinStats table with the latest price data for the current date. Historical data is preserved, allowing for time-series analysis and charting. 
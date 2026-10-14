# Freight Fox Toll Plaza Finder

An intelligent Spring Boot microservice that calculates driving routes between Indian postal codes (pincodes), identifies all toll plazas positioned along the route, computes their exact distance from the origin along the driving path, and delivers cached responses.

---

## Table of Contents

1. [Project Title and Overview](#1-project-title-and-overview)
2. [Technology Stack](#2-technology-stack)
3. [Prerequisites](#3-prerequisites)
4. [Project Structure](#4-project-structure)
5. [Environment Configuration](#5-environment-configuration)
6. [Local Setup](#6-local-setup)
7. [API Documentation](#7-api-documentation)
8. [Validation and Error Handling](#8-validation-and-error-handling)
9. [Toll Matching Algorithm](#9-toll-matching-algorithm)
10. [Caching](#10-caching)
11. [Testing](#11-testing)
12. [Postman Testing](#12-postman-testing)
13. [Deployment](#13-deployment)
14. [Assumptions and Limitations](#14-assumptions-and-limitations)

---

## 1. Project Title and Overview

**Freight Fox Toll Plaza Finder** is a specialized logistics routing service built with Java and Spring Boot.

### Core Functionality
- **Pincode Geocoding:** Translates 6-digit Indian postal codes into precise geographic coordinates (latitude and longitude) using OpenStreetMap Nominatim with an automatic fallback to the Zippopotam API.
- **Driving Route Calculation:** Integrates with OpenRouteService Directions API (`driving-car`) to obtain actual road geometry and total route distance in kilometers.
- **Toll Plaza Matching:** Reads a comprehensive dataset of Indian National Highway toll plazas from PostgreSQL and matches plazas within a 500-meter perpendicular proximity threshold along the decoded polyline route segments.
- **Distance from Origin Along Route:** Computes the exact driving distance from the starting pincode to each identified toll plaza along the road trajectory.
- **In-Memory Caching:** Leverages a 30-minute Caffeine in-memory cache to instantly replay route and toll calculations for identical pincode pairs.
- **Initial Data Seeding:** Automatically seeds the toll plaza repository from an embedded CSV file (`data/toll_plaza_india.csv`) on startup when the database table is empty.

---

## 2. Technology Stack

- **Java:** Version `21`
- **Spring Boot:** Version `3.4.2`
  - `spring-boot-starter-web`: RESTful web services and HTTP client handling (`RestClient`)
  - `spring-boot-starter-data-jpa`: Object-relational mapping and database access via Spring Data JPA and Hibernate 6.6.5
  - `spring-boot-starter-validation`: Declarative validation framework
  - `spring-boot-starter-cache`: Spring caching abstraction
  - `spring-boot-starter-test`: Unit and integration testing with JUnit 5, Mockito, and Spring MockMvc
- **Build & Dependency Management:** Apache Maven
- **Database:** PostgreSQL (supports cloud providers like Neon serverless PostgreSQL)
  - `org.postgresql:postgresql` runtime JDBC driver
  - HikariCP connection pooling
- **Cache Provider:** Caffeine Cache (`com.github.ben-manes.caffeine:caffeine`)
- **CSV Ingestion:** Apache Commons CSV (`org.apache.commons:commons-csv:1.14.1`)
- **Code Generation:** Project Lombok (`org.projectlombok:lombok`)
- **External APIs:**
  - OpenRouteService Directions API (`https://api.heigit.org/openrouteservice/v2/directions/driving-car`)
  - OpenStreetMap Nominatim Geocoding API (`https://nominatim.openstreetmap.org/search`)
  - Zippopotam India Geocoding API (`https://api.zippopotam.us/in/`)

---

## 3. Prerequisites

Before running the application, ensure the following are installed and configured:

1. **Java Development Kit (JDK):** JDK 21 or higher installed (`java -version`).
2. **Apache Maven:** Maven 3.8+ installed (`mvn -version`).
3. **PostgreSQL Database:** A running PostgreSQL instance (local or hosted on Neon, Supabase, AWS RDS, etc.).
4. **OpenRouteService API Key:** An active API token obtained by creating an account at [openrouteservice.org](https://openrouteservice.org/).

---

## 4. Project Structure

```text
FrieghtFox/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/freightfox/tollplazafinder/
│   │   │   ├── TollPlazaFinderApplication.java        # Spring Boot entry point (@EnableCaching)
│   │   │   ├── controller/
│   │   │   │   └── TollPlazaController.java           # POST /api/v1/toll-plazas endpoint
│   │   │   ├── dto/
│   │   │   │   ├── TollPlazaRequest.java              # Request payload (sourcePincode, destinationPincode)
│   │   │   │   ├── TollPlazaFinderResponse.java       # Response record (RouteInfo, TollPlazaItem)
│   │   │   │   ├── RouteResponse.java                 # OpenRouteService JSON response binding
│   │   │   │   └── RouteTollResponse.java             # Entity mapping DTO
│   │   │   ├── entity/
│   │   │   │   └── TollPlaza.java                     # JPA Entity mapped to toll_plazas table
│   │   │   ├── repository/
│   │   │   │   └── TollPlazaRepository.java           # Spring Data JPA repository for TollPlaza
│   │   │   ├── service/
│   │   │   │   ├── RoutingService.java                # Nominatim/Zippo geocoding and ORS routing client
│   │   │   │   ├── RouteTollService.java              # Route decoding, toll matching, and @Cacheable logic
│   │   │   │   ├── TollPlazaInsertService.java        # CommandLineRunner for initial CSV import
│   │   │   │   └── TollPlazaService.java              # Toll plaza retrieval helper service
│   │   │   ├── util/
│   │   │   │   ├── DistanceCalculator.java            # Haversine distance & segment closest-point projection
│   │   │   │   └── PolyLineDecoder.java               # Decodes Google Encoded Polyline algorithm
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java        # Centralized REST exception handler (@RestControllerAdvice)
│   │   └── resources/
│   │       ├── application.yaml                       # Application configuration and cache definitions
│   │       └── data/
│   │           └── toll_plaza_india.csv               # Toll plaza master dataset (name, coordinates, state)
│   └── test/
│       └── java/com/freightfox/tollplazafinder/
│           └── TollPlazaFinderApplicationTests.java   # Comprehensive integration test suite (9 scenarios)
```

---

## 5. Environment Configuration

The application configures its data sources and external routing keys via environment variables referenced in `src/main/resources/application.yaml`.

| Environment Variable | Description | Example Placeholder |
| :--- | :--- | :--- |
| `DB_URL` | JDBC connection URL for PostgreSQL database | `jdbc:postgresql://<host>:<port>/<database>?sslmode=require` |
| `DB_USERNAME` | Database role username | `<your_db_username>` |
| `DB_PASSWORD` | Database role password | `<your_db_password>` |
| `ORS_API_KEY` | OpenRouteService API key | `<your_openrouteservice_api_key>` |

> **Security Note:** Never commit database credentials, tokens, or API keys directly to source control. Always inject them via environment variables or secret managers.

---

## 6. Local Setup

### Step 1: Clone the Repository
```bash
git clone https://github.com/Manishi-nigam/Freight-Fox.git
cd Freight-Fox
```

### Step 2: Configure Environment Variables

#### On Windows (PowerShell):
```powershell
$env:DB_URL="jdbc:postgresql://<host>:<port>/<database>?sslmode=require"
$env:DB_USERNAME="<your_db_username>"
$env:DB_PASSWORD="<your_db_password>"
$env:ORS_API_KEY="<your_ors_api_key>"
```

#### On Linux / macOS (Bash):
```bash
export DB_URL="jdbc:postgresql://<host>:<port>/<database>?sslmode=require"
export DB_USERNAME="<your_db_username>"
export DB_PASSWORD="<your_db_password>"
export ORS_API_KEY="<your_ors_api_key>"
```

### Step 3: Build the Project
```bash
mvn clean package -DskipTests
```

### Step 4: Run the Application
```bash
mvn spring-boot:run
```
Or execute the packaged JAR:
```bash
java -jar target/toll-plaza-finder-0.0.1-SNAPSHOT.jar
```

The server starts by default on port `8080`. On first run, `TollPlazaInsertService` checks the `toll_plazas` table; if empty, it imports records from `data/toll_plaza_india.csv`.

---

## 7. API Documentation

### Endpoint
`POST /api/v1/toll-plazas`

### Request Headers
- `Content-Type: application/json`

### Request Body
```json
{
  "sourcePincode": "110001",
  "destinationPincode": "560001"
}
```

*Note: The fields `@JsonAlias("source")` and `@JsonAlias("destination")` are also supported.*

### Response Schema

| Field Path | Type | Description |
| :--- | :--- | :--- |
| `route.sourcePincode` | `String` | Source Indian postal code |
| `route.destinationPincode` | `String` | Destination Indian postal code |
| `route.distanceInKm` | `Double` | Actual calculated driving route distance in kilometers |
| `tollPlazas` | `Array` | List of toll plazas on route, sorted ascending by `distanceFromSource` |
| `tollPlazas[].name` | `String` | Name of the toll plaza |
| `tollPlazas[].latitude` | `Double` | Latitude of the toll plaza |
| `tollPlazas[].longitude` | `Double` | Longitude of the toll plaza |
| `tollPlazas[].distanceFromSource` | `Double` | Cumulative driving distance from source to this toll plaza in km |

### Realistic Example Response (Delhi `110001` to Bengaluru `560001`)
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "560001",
    "distanceInKm": 2174.52
  },
  "tollPlazas": [
    {
      "name": "Chhayan Toll Plaza",
      "latitude": 27.818817,
      "longitude": 77.56847,
      "distanceFromSource": 92.15
    },
    {
      "name": "Mahavan Toll Plaza",
      "latitude": 27.420833,
      "longitude": 77.771944,
      "distanceFromSource": 143.68
    },
    {
      "name": "Semra Toll Plaza",
      "latitude": 26.6575,
      "longitude": 78.113056,
      "distanceFromSource": 263.84
    }
  ]
}
```

### Empty List Case (No Toll Plazas Found Along Route)
When no toll plazas are within the 500-meter proximity threshold (e.g., intra-city queries such as `110001` to `110001`):
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "110001",
    "distanceInKm": 0.0
  },
  "tollPlazas": []
}
```

---

## 8. Validation and Error Handling

All error responses return a standardized JSON structure produced by `GlobalExceptionHandler`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "<descriptive error message>"
}
```

### Supported Validation & Error Scenarios:

1. **Missing Request Body:**
   - Status: `400 Bad Request`
   - Message: `Request body is required.`
2. **Missing / Blank Pincode:**
   - Status: `400 Bad Request`
   - Message: `Field 'sourcePincode' is required and cannot be blank.`
3. **Invalid Pincode Format:**
   - Pincodes must strictly match the regex `^[1-9][0-9]{5}$` (6 digits, starting with 1–9).
   - Status: `400 Bad Request`
   - Message: `Invalid sourcePincode: '012345'. Pincode must be a 6-digit Indian postal code starting with digits 1-9.`
4. **Identical Source and Destination:**
   - Gracefully handled. Geocodes both locations, resolves the route, and returns `distanceInKm` with an empty `"tollPlazas": []` list.
5. **Unresolved Pincodes:**
   - When coordinates cannot be resolved by either Nominatim or Zippopotam.
   - Status: `404 Not Found`
   - Message: `Could not resolve geographic coordinates for pincode: 999999`
6. **External Service Downtime:**
   - When OpenRouteService or geocoders are unreachable.
   - Status: `502 Bad Gateway`
   - Message: `External service communication failure.`

---

## 9. Toll Matching Algorithm

The toll matching process in `RouteTollService` executes in sequential stages:

```
[Source & Destination Pincodes]
              │
              ▼
    Geocoding (Nominatim / Zippopotam)
              │
              ▼
   OpenRouteService Directions API (driving-car)
              │
              ▼
   Encoded Polyline Decoded into Coordinates [(lat, lon), ...]
              │
              ▼
   Iterate Segments [c_i, c_{i+1}] & Compute Segment Lengths via Haversine
              │
              ▼
   Project each Toll Plaza onto nearest segment (equirectangular scalar t in [0, 1])
              │
              ▼
   Is perpendicular distance <= 500.0 meters?
              │
              ├── Yes: Calculate cumulative distance = segmentOffset + t * segmentLength
              └── No: Discard
              │
              ▼
   Deduplicate plazas & sort ascending by distanceFromSource
              │
              ▼
   Construct TollPlazaFinderResponse
```

### Key Technical Details:
- **Polyline Decoding:** `PolyLineDecoder` decodes the compressed Google Polyline string with 5 decimal places of precision into ordered `(lat, lon)` points.
- **Segment Closest-Point Math:** `DistanceCalculator.findClosestPointOnSegment` calculates the closest point $C$ on the segment $[A, B]$ to point $P$ using equirectangular projection adjusted for the mean segment latitude:
  $$t = \frac{\vec{u} \cdot \vec{v}}{\|\vec{v}\|^2}, \quad t \in [0, 1]$$
- **Perpendicular Distance:** The Haversine formula calculates spherical distance between toll plaza coordinates and closest point $C$.
- **Matching Threshold:** **`500.0` meters** (`thresholdMeters = 500.0`). Only toll plazas whose closest point is within 500 meters of the road segment are considered on the route.
- **Distance from Origin:** The cumulative route distance up to the segment start point plus the fractional distance along the segment ($t \times \text{segmentLength}$).

---

## 10. Caching

The application integrates an in-memory **Caffeine Cache** managed via Spring's `@Cacheable` annotation on `RouteTollService.findTollPlazasAlongRoute`.

### Configuration in `application.yaml`:
```yaml
spring:
  cache:
    type: caffeine
    cache-names:
      - route-toll-plazas
    caffeine:
      spec: expireAfterWrite=30m
```

### Caching Behavior:
- **Cache Name:** `route-toll-plazas`
- **Cache Key:** `#sourcePincode + '-' + #destinationPincode` (e.g., `110001-560001`).
- **Asymmetric Routing:** Distinct keys are maintained for opposite directions (`110001-560001` vs `560001-110001`), ensuring directional accuracy.
- **TTL (Time-To-Live):** Entries automatically expire 30 minutes after write (`expireAfterWrite=30m`).
- **Error Protection:** Unsuccessful responses (e.g. 400 Bad Request, 404 Not Found, 502 Bad Gateway) throw exceptions and are never stored in cache.

### In-Memory Cache Limitations:
- **Node-Local:** Cache is stored entirely in JVM heap memory. In a multi-instance or horizontal scale-out deployment, instances do not share cache state (unlike distributed caches such as Redis).
- **Process Lifetime:** Cache contents are lost when the application process restarts.
- **Memory Footprint:** Cache memory grows with high volumes of distinct pincode combinations.

---

## 11. Testing

The project includes an end-to-end integration test suite in `TollPlazaFinderApplicationTests.java`.

### Running Tests
Ensure required environment variables are set in your shell before running Maven:

```powershell
# Windows PowerShell
$env:DB_URL="jdbc:postgresql://<host>:<port>/<database>?sslmode=require"
$env:DB_USERNAME="<username>"
$env:DB_PASSWORD="<password>"
$env:ORS_API_KEY="<ors_key>"
mvn clean test
```

```bash
# Linux / macOS
export DB_URL="jdbc:postgresql://<host>:<port>/<database>?sslmode=require"
export DB_USERNAME="<username>"
export DB_PASSWORD="<password>"
export ORS_API_KEY="<ors_key>"
mvn clean test
```

### Verified Test Scenarios (9 tests):
1. `testExistingDatabaseFunctionality`: Verifies database connection and confirms toll plaza records exist.
2. `testInputValidationMalformedPincodes`: Tests validation against null bodies, blanks, letters, out-of-range lengths, and leading zeros.
3. `testGeocodingCoordinatesAndUnresolvedPincode`: Asserts correct coordinates for Delhi (`110001`) and Lucknow (`226001`), and verifies 404 response for invalid pincodes (`999999`).
4. `testSegmentProjectionGeometry`: Verifies closest point projection on segment bounds (start, midpoint, end, clamped extensions).
5. `testTollPlazasAlongRouteWithValidPincodes`: Verifies end-to-end routing between Delhi and Lucknow, checking distance (> 400 km) and ascending toll distance ordering.
6. `testRouteWithNoMatchingTollsReturnsEmptyList`: Validates that routes without toll plazas return `"tollPlazas": []`.
7. `testHttpEndpointPostTollPlazasValidationAndSuccess`: Executes `MockMvc` POST requests verifying 400 on bad format, 404 on unresolvable pincode, and 200 OK with the exact JSON schema.
8. `testCachingSuccessReplayAndDistinctKeys`: Verifies cache hit replay, directional key separation, empty result caching, and non-caching of failed executions.
9. `testCacheTtlExpirationConfiguration`: Inspects Caffeine cache policy to verify 30-minute expiration configuration.

### Actual Execution Result:
```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 12. Postman Testing

### General Configuration
- **Method:** `POST`
- **URL:** `http://localhost:8080/api/v1/toll-plazas`
- **Headers:**
  - `Content-Type: application/json`

### Test Cases

#### Case 1: Standard Route Query (Success - 200 OK)
- **Body:**
  ```json
  {
    "sourcePincode": "110001",
    "destinationPincode": "560001"
  }
  ```
- **Expected Status:** `200 OK`
- **Expected Result:** JSON object with `route` details and populated `tollPlazas` list sorted by `distanceFromSource`.

#### Case 2: Intra-city Route (Empty List - 200 OK)
- **Body:**
  ```json
  {
    "sourcePincode": "110001",
    "destinationPincode": "110001"
  }
  ```
- **Expected Status:** `200 OK`
- **Expected Result:** JSON object with `distanceInKm` and `"tollPlazas": []`.

#### Case 3: Invalid Pincode Format (400 Bad Request)
- **Body:**
  ```json
  {
    "sourcePincode": "012345",
    "destinationPincode": "560001"
  }
  ```
- **Expected Status:** `400 Bad Request`
- **Expected Result:** `Invalid sourcePincode: '012345'. Pincode must be a 6-digit Indian postal code starting with digits 1-9.`

#### Case 4: Non-existent Pincode (404 Not Found)
- **Body:**
  ```json
  {
    "sourcePincode": "999999",
    "destinationPincode": "560001"
  }
  ```
- **Expected Status:** `404 Not Found`
- **Expected Result:** `Could not resolve geographic coordinates for pincode: 999999`

#### Case 5: Empty / Missing Request Body (400 Bad Request)
- **Body:** (empty)
- **Expected Status:** `400 Bad Request`
- **Expected Result:** `Request body is required.`

---

## 13. Deployment

The application is packaged as an executable Spring Boot fat JAR, making it compatible with cloud container and PaaS environments such as **Railway**, Render, AWS Elastic Beanstalk, or Docker.

### Railway Deployment Workflow:
1. Connect the GitHub repository `https://github.com/Manishi-nigam/Freight-Fox` to Railway.
2. Railway's buildpack automatically identifies Maven through `pom.xml`, compiles the code (`mvn clean package`), and executes the resulting JAR.
3. In the Railway dashboard under **Variables**, define the following environment variables:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `ORS_API_KEY`
   - `PORT` (Railway provides this automatically; Spring Boot binds to `8080` by default).

---

## 14. Assumptions and Limitations

- **Indian Postal Codes Only:** Geocoding is configured specifically for India (`country=India` query param on Nominatim and `/in/{pincode}` on Zippopotam).
- **Road Driving Mode:** Route computations use OpenRouteService's `driving-car` profile. Public transit, pedestrian, or ferry paths are not modeled.
- **500-Meter Proximity Threshold:** Toll plazas located beyond 500 meters perpendicular distance from polyline route segments are not matched.
- **Static Toll Plaza Catalog:** Toll plaza records are populated from `toll_plaza_india.csv` upon initial application startup. Real-time updates or dynamic plaza additions require updating the database.
- **Single-Node In-Memory Cache:** Caffeine cache entries reside in memory on the running JVM instance and do not synchronize across distributed cluster nodes.
- **Third-Party Rate Limits:** Geocoding and routing depend on external services (Nominatim, Zippopotam, OpenRouteService) and are subject to their respective usage limits, network latency, and availability.

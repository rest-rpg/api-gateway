# Plan Ulepszeń Spring Cloud Gateway

## 📊 Status Implementacji - 2025-03-29

**Ukończone:** Sprint 1 - Foundation & Resilience ✅
**Następny:** Sprint 2 - Observability 🎯
**Postęp ogólny:** 3/14 głównych funkcji (21%)

### ✅ Co zostało zaimplementowane:
1. ✅ Natywna konfiguracja YAML (zamiast programowej)
2. ✅ Circuit Breaker dla wszystkich serwisów
3. ✅ Per-route timeouts (connect + response)
4. ✅ Generyczny FallbackController (reusable dla dowolnych projektów)
5. ✅ Reaktywny stack (ServerWebExchange + Mono)

### 🎯 Co jest dalej (Sprint 2):
- Correlation ID Global Filter
- Prometheus Metrics
- Request/Response Logging
- Distributed Tracing (Zipkin)

---

## Obecny Stan Projektu

**Wersja:** Spring Boot 3.3.4, Spring Cloud 2023.0.3
**Architektura:** Microservices (game, user, auth)
**Gateway:** Spring Cloud Gateway (WebFlux-based, Netty)

### Obecna Konfiguracja (po Sprint 1)
- 3 mikroserwisy: game (8080), user (8081), auth (8082)
- ✅ **Routing natywny w YAML** (application.yml)
- ✅ **Circuit Breaker aktywny** (Resilience4j dla wszystkich route'ów)
- ✅ **Timeouts skonfigurowane** (per-route metadata)
- ✅ **Generyczny FallbackController** (obsługuje dowolne serwisy)
- ✅ CORS skonfigurowany globalnie
- ✅ Actuator dla monitoringu
- ✅ Docker networking (game-service:8080, user-service:8081, auth-service:8082)

## Zidentyfikowane Problemy i Możliwości Ulepszenia

### Krytyczne:
1. ✅ **~~Brak Service Discovery~~** - **ROZWIĄZANE**: już używasz Docker container names (`game-service:8080`, etc.) przez zmienne środowiskowe
2. **Circuit Breaker wyłączony** - zakomentowany w kodzie, brak fallback endpoints
3. **Brak timeout configuration** - ryzyko cascading failures
4. **Brak correlation ID** - trudne śledzenie requestów przez system
5. **Brak monitoringu produkcyjnego** - brak Prometheus metrics

### Ważne:
6. **Brak rate limiting** - brak ochrony przed nadmiernym ruchem/DoS
7. **Brak request/response logging** - trudne debugowanie
8. **Brak security headers** - nie spełnia standardów OWASP
9. **Brak distributed tracing** - brak visibility w przepływ requestów
10. **Nieoptymalna konfiguracja Netty** - domyślne wartości connection pool

### Nice to have:
11. **Programowe route'y zamiast YAML** - można to ulepszyć na natywną konfigurację YAML
12. **Brak response caching** - możliwość optymalizacji performance
13. **Brak OAuth2/JWT integration** - centralized authentication
14. **Brak weighted routing** - przydatne dla canary deployments

## Plan Ulepszeń - Roadmap

### 🎯 PRIORITY MATRIX

**P0 (Critical) - Must Have:**
- Circuit Breaker Fallbacks
- Timeout Configuration
- Correlation ID Tracking
- Prometheus Metrics
- Request/Response Logging

**P1 (High) - Should Have:**
- Security Headers
- Rate Limiting (Redis)
- Distributed Tracing (Zipkin)
- Netty Performance Tuning

**P2 (Medium) - Nice to Have:**
- Service Discovery (Eureka) - **obecnie używasz Docker networking, wystarczające**
- Response Caching
- OAuth2/JWT Integration
- Weighted Routing
- Custom Filters (Auth, RBAC)

## Plan Ulepszeń - Detailed

### Faza 1: Migracja do Natywnej Konfiguracji Spring Cloud Gateway

**Cel:** Użycie wbudowanej konfiguracji route'ów Spring Gateway w YAML

#### 1.1 Struktura Katalogów
```
src/main/java/com/microservices/apigateway/
├── filters/
│   ├── LoggingGatewayFilterFactory.java
│   ├── AuthenticationFilter.java
│   └── RequestTrackingFilter.java
└── fallback/
    └── FallbackController.java
```

#### 1.2 Natywna Konfiguracja w YAML (application.yml)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: game-route
          uri: http://localhost:8080
          predicates:
            - Path=/api/game/**
          filters:
            - StripPrefix=1
        - id: user-route
          uri: http://localhost:8081
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
        - id: auth-route
          uri: http://localhost:8082
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
```

**Korzyści:**
- Brak potrzeby tworzenia custom klas `Routes.java` i `RoutesProperties.java`
- Natywne wsparcie Spring Cloud Gateway
- Prostsze dodawanie nowych route'ów
- Hot-reload z Spring Cloud Config
- Pełne wsparcie IDE (autocomplete, validation)

### Faza 2: Zaawansowana Konfiguracja Route'ów

#### 2.1 Dodanie CircuitBreaker, Retry, i innych filtrów
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: game-route
          uri: ${GAME_SERVICE_URL:http://localhost:8080}
          predicates:
            - Path=/api/game/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: gameServiceCB
                fallbackUri: forward:/fallback/game
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
                methods: GET,POST
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

#### 2.2 Zmienne środowiskowe i properties
- Użycie `${VAR:default}` dla URI serwisów
- Konfiguracja per-environment (dev, prod)
- Externalized configuration

### Faza 3: ✅ Service Discovery - Status

**Obecny stan:**
- ✅ Używasz Docker Compose networking
- ✅ Nazwy kontenerów jako hostnames: `game-service:8080`, `user-service:8081`, `auth-service:8082`
- ✅ Zmienne środowiskowe w `.env` file
- ✅ Brak hardcoded URLs

**Docker Compose setup (już działa):**
```yaml
# env/docker-compose.yml
services:
  api-gateway:
    environment:
      ROUTES_GAMEURL: ${GAME_URL}  # = http://game-service:8080
      ROUTES_USERURL: ${USER_URL}  # = http://user-service:8081
      ROUTES_AUTHURL: ${AUTH_URL}  # = http://auth-service:8082
    networks:
      - rest-rpg-network

# env/.env
GAME_URL=http://game-service:8080
USER_URL=http://user-service:8081
AUTH_URL=http://auth-service:8082
```

**Kiedy dodać Eureka?**
- Gdy będziesz skalować (2+ instancje serwisu)
- Gdy potrzebujesz auto-scaling
- Gdy przejdziesz na Kubernetes
- **Na ten moment: Docker networking wystarczy!**

**Opcjonalnie - Eureka (P2 - Nice to Have):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### Faza 4: Resilience Patterns

**Cel:** Circuit Breaking, Timeouts, Retries, Fallbacks

#### 4.1 Circuit Breaker z Fallbacks
```yaml
routes:
  - id: game-route
    uri: lb://GAME-SERVICE
    predicates:
      - Path=/api/game/**
    filters:
      - StripPrefix=1
      - name: CircuitBreaker
        args:
          name: gameServiceCB
          fallbackUri: forward:/fallback/game
      - name: FallbackHeaders
        args:
          executionExceptionTypeHeaderName: Exception-Type
          executionExceptionMessageHeaderName: Exception-Message
    metadata:
      response-timeout: 3000
      connect-timeout: 1000
```

#### 4.2 Fallback Controller
```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/game")
    public ResponseEntity<Map<String, String>> gameFallback(
            @RequestHeader(value = "Exception-Type", required = false) String exType) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "error", "Game service temporarily unavailable",
                    "exceptionType", exType != null ? exType : "Unknown"
                ));
    }
}
```

#### 4.3 Retry Configuration
```yaml
routes:
  - id: game-route
    filters:
      - name: Retry
        args:
          retries: 3
          statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
          methods: GET,POST
          backoff:
            firstBackoff: 50ms
            maxBackoff: 500ms
            factor: 2
            basedOnPreviousValue: true
```

#### 4.4 Per-Route Timeouts
- Connect timeout: 1s (szybko fail na connection issues)
- Response timeout: 3-5s per service (prevent slow responses)
- Global timeout w metadata każdego route

### Faza 5: Observability & Monitoring

**Cel:** Full visibility, metrics, tracing, logging

#### 5.1 Prometheus Metrics
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: gateway,health,metrics,prometheus,info
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

**Dostępne metryki:**
- `gateway.requests` - timer per route (latency, throughput)
- Tags: route_id, status, outcome, http_method
- Connection pool metrics
- Circuit breaker states (już masz w Resilience4J!)

**Grafana Dashboard:**
- Request rate per service
- p50/p95/p99 latencies
- Error rates (4xx, 5xx)
- Circuit breaker states
- Connection pool utilization

#### 5.2 Distributed Tracing (Zipkin/Jaeger)
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (dev), 0.1 (prod)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

**Korzyści:**
- Request flow visualization
- Latency breakdown per service
- Error root cause analysis

#### 5.3 Correlation ID Global Filter
```java
@Component
@Order(-1)
public class CorrelationIdFilter implements GlobalFilter {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID);

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        // Add to request downstream
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID, correlationId)
                .build();

        // Add to response
        exchange.getResponse().getHeaders().add(CORRELATION_ID, correlationId);

        // MDC for logging
        return chain.filter(exchange.mutate().request(request).build())
                .contextWrite(Context.of("correlationId", correlationId));
    }
}
```

#### 5.4 Request/Response Logging Filter
```java
@Component
@Order(0)
@Slf4j
public class RequestLoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        log.info("Request: {} {} from {}",
                request.getMethod(),
                request.getURI().getPath(),
                request.getRemoteAddress());

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();

            log.info("Response: {} {} - status={} duration={}ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    response.getStatusCode(),
                    duration);
        });
    }
}
```

**Structured Logging (JSON):**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### Faza 6: Security & Rate Limiting

**Cel:** Security headers, rate limiting, authentication

#### 6.1 Security Headers Filter
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - SecureHeaders
```

**Dodaje automatycznie:**
- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security`

#### 6.2 Rate Limiting (Redis-based)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    gateway:
      routes:
        - id: game-route
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 100  # tokens/second
                  burstCapacity: 200  # max burst
                  requestedTokens: 1  # tokens per request
                key-resolver: "#{@userKeyResolver}"
```

**Custom KeyResolver (per user/IP):**
```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        // Rate limit per user (from JWT)
        String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id");

        if (userId != null) {
            return Mono.just(userId);
        }

        // Fallback: rate limit per IP
        return Mono.just(exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress());
    };
}
```

**Response na rate limit:**
- HTTP 429 Too Many Requests
- Headers: `X-RateLimit-Remaining`, `X-RateLimit-Burst-Capacity`, `X-RateLimit-Replenish-Rate`

#### 6.3 OAuth2/JWT Authentication (Optional)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-security</artifactId>
</dependency>
```

**TokenRelay Filter:**
```yaml
routes:
  - id: secured-route
    filters:
      - TokenRelay  # automatically forwards OAuth2 access token
```

**Custom JWT Validation Filter:**
```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            // Validate JWT
            Claims claims = validateToken(token);

            // Add user context to headers
            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(request).build());

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100; // high priority
    }
}
```

### Faza 7: Performance Optimization

**Cel:** Optymalizacja Netty, connection pooling, caching

#### 7.1 Netty HTTP Client Configuration
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          type: FIXED
          max-connections: 1000  # default: 500
          acquire-timeout: 2000ms
          max-idle-time: 20000ms
        connect-timeout: 1000
        response-timeout: 5s
        compression: true
        wiretap: false  # true dla debugging
```

**JVM Parameters:**
```bash
-Dreactor.netty.ioWorkerCount=32  # 3-4x CPU cores
-Dreactor.netty.pool.leasingStrategy=lifo  # hot connections
```

#### 7.2 Response Caching (LocalResponseCache)
```yaml
spring:
  cloud:
    gateway:
      filter:
        local-response-cache:
          enabled: true
          time-to-live: 30m
          size: 500MB
      routes:
        - id: game-static-route
          uri: lb://GAME-SERVICE
          predicates:
            - Path=/api/game/static/**
          filters:
            - LocalResponseCache
```

**Cachuje:**
- GET requests only
- Status 200, 206, 301
- Respektuje Cache-Control headers

#### 7.3 GZIP Compression
- Automatycznie enabled w httpclient
- Kompresja response > 1KB
- Content-Type: text/*, application/json, application/xml

### Faza 8: Advanced Routing Patterns

**Cel:** Weighted routing, canary deployments, A/B testing

#### 8.1 Weighted Routing (Canary)
```yaml
routes:
  - id: game-v1
    uri: lb://GAME-SERVICE-V1
    predicates:
      - Path=/api/game/**
      - Weight=game-group, 80  # 80% traffic
  - id: game-v2-canary
    uri: lb://GAME-SERVICE-V2
    predicates:
      - Path=/api/game/**
      - Weight=game-group, 20  # 20% traffic (canary)
```

**Use cases:**
- Gradual rollouts (10% → 50% → 100%)
- A/B testing
- Blue-green deployments

#### 8.2 Header-based Routing (Beta Users)
```yaml
routes:
  - id: game-beta
    uri: lb://GAME-SERVICE-BETA
    predicates:
      - Path=/api/game/**
      - Header=X-Beta-User, true
    order: 1  # higher priority

  - id: game-prod
    uri: lb://GAME-SERVICE
    predicates:
      - Path=/api/game/**
    order: 2
```

#### 8.3 Time-based Routing (Maintenance Windows)
```yaml
routes:
  - id: maintenance-window
    uri: http://maintenance-page
    predicates:
      - Path=/api/**
      - Between=2024-12-25T00:00:00Z, 2024-12-25T06:00:00Z
    order: 1

  - id: normal-routes
    uri: lb://SERVICES
    predicates:
      - Path=/api/**
    order: 2
```

### Faza 9: Additional Useful Filters

#### 9.1 Request Modification
- **AddRequestHeader**: `- AddRequestHeader=X-Request-Source, api-gateway`
- **AddRequestParameter**: `- AddRequestParameter=source, gateway`
- **PrefixPath**: `- PrefixPath=/api` (dodaje prefix)
- **RewritePath**: `- RewritePath=/old/(?<segment>.*), /new/${segment}` (regex rewrite)
- **SetPath**: `- SetPath=/api/{segment}` (templating)

#### 9.2 Response Modification
- **AddResponseHeader**: `- AddResponseHeader=X-Gateway-Version, 1.0`
- **SetStatus**: `- SetStatus=200` (override status code)
- **RedirectTo**: `- RedirectTo=302, https://new-url.com` (redirect)

#### 9.3 Host & Session
- **PreserveHostHeader**: zachowuje oryginalny Host header
- **SaveSession**: wymusza save sesji (Spring Session)

#### 9.4 Request Size Limit
```yaml
routes:
  - id: upload-route
    filters:
      - name: RequestSize
        args:
          maxSize: 5000000  # 5MB
```

### Faza 10: Actuator Gateway Endpoints

**Dostępne endpointy** (już masz actuator!):

```bash
# Lista wszystkich route'ów
GET /actuator/gateway/routes

# Szczegóły konkretnego route
GET /actuator/gateway/routes/{id}

# Lista global filters (z kolejnością)
GET /actuator/gateway/globalfilters

# Lista dostępnych filter factories
GET /actuator/gateway/routefilters

# Lista dostępnych predicate factories
GET /actuator/gateway/routepredicates

# Odświeżenie cache routes (hot-reload)
POST /actuator/gateway/refresh

# Dynamiczne dodanie route
POST /actuator/gateway/routes/{id}
Content-Type: application/json
{
  "id": "dynamic-route",
  "uri": "lb://NEW-SERVICE",
  "predicates": ["Path=/api/new/**"]
}

# Usunięcie route
DELETE /actuator/gateway/routes/{id}
```

**Use cases:**
- Debugging routing issues
- Dynamic route management (bez restartu!)
- Monitoring filter order
- CI/CD integration

## Dodatkowe Możliwości Spring Cloud Gateway

### Predicate Factories (Routing Criteria)
Oprócz `Path`, Spring Gateway oferuje:
- **After/Before/Between** - routing czasowy (maintenance windows, timed features)
- **Header** - routing po headerach (API versioning, feature flags, A/B testing)
- **Host** - routing po hostname (multi-tenant, subdomain routing)
- **Cookie** - routing po cookies (user segmentation, beta features)
- **Method** - routing po HTTP method (read/write separation)
- **Query** - routing po query params (API versioning: ?v=2)
- **RemoteAddr/XForwardedRemoteAddr** - routing po IP (geo-routing, whitelisting)
- **Weight** - weighted routing (canary deployments, A/B testing)

### Useful Filters
- **ModifyRequestBody/ModifyResponseBody** - transformacja body (enrichment, masking)
- **PrefixPath/RewritePath/SetPath** - zaawansowana manipulacja ścieżkami
- **AddRequest/ResponseHeader** - dodawanie custom headers
- **SetStatus** - override status codes
- **RedirectTo** - HTTP redirects (301/302)
- **PreserveHostHeader** - zachowanie oryginalnego Host
- **SaveSession** - Spring Session integration
- **RequestSize** - limit rozmiaru requestu (DoS protection)

## Struktura Po Refaktoryzacji

### application.yml - Pełna Konfiguracja
```yaml
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: game-route
          uri: ${GAME_SERVICE_URL:http://localhost:8080}
          predicates:
            - Path=/api/game/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: gameServiceCB
                fallbackUri: forward:/fallback/game
        - id: user-route
          uri: ${USER_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
        - id: auth-route
          uri: ${AUTH_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_UNIQUE
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:5173,http://localhost:3000"
            allowedHeaders: "*"
            allowedMethods: "*"
            allow-credentials: true
```

**Uwaga:** Klasy `Routes.java` i `RoutesProperties.java` można usunąć - nie są potrzebne przy natywnej konfiguracji Spring Gateway!

## Harmonogram Implementacji

1. **Faza 1:** Migracja do natywnej konfiguracji Spring Gateway w YAML, usunięcie niepotrzebnych klas
2. **Faza 2:** Dodanie CircuitBreaker, Retry, RateLimit do konfiguracji YAML
3. **Faza 3:** Resilience & Security (JWT filter, fallback endpoints)
4. **Faza 4:** Observability (custom filters: logging, metrics, tracing)
5. **Faza 5:** Optymalizacja (caching filters, transformacje)
6. **Faza 6:** Developer Experience (dokumentacja, testy, DevOps)

## Zależności do Dodania

```xml
<!-- Rate Limiting -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>

<!-- Distributed Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Service Discovery (opcjonalnie) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

## Harmonogram Implementacji - Zaktualizowany (Docker Networking)

### ✅ Sprint 1 - Foundation & Resilience (UKOŃCZONY - 2025-03-29)
**P0 - Critical:**
1. ✅ Faza 1: Migracja do natywnej konfiguracji YAML
2. ✅ Faza 3: Service Discovery (Docker networking - już działa!)
3. ✅ Faza 4: Circuit Breaker z fallbacks + timeouts

**Szczegóły implementacji Sprint 1:**

**Konfiguracja & Routing:**
- ✅ Usunięto programową konfigurację route'ów (`Routes.java`, `RoutesProperties.java`)
- ✅ Wszystkie route'y skonfigurowane natywnie w `application.yml`
- ✅ **Upgrade do Spring Cloud 2025.x** - zaktualizowano structure: `spring.cloud.gateway.server.webflux.routes`
- ✅ Zmieniono zależność na `spring-cloud-starter-circuitbreaker-reactor-resilience4j` (reaktywny CB)

**Circuit Breaker & Resilience:**
- ✅ Circuit Breaker dodany do wszystkich 3 route'ów (game, user, auth)
- ✅ Konfiguracja Resilience4J: sliding window 10, failure rate 50%, wait 5s
- ✅ Per-route timeouts: game (3s response, 1s connect), user/auth (2s response, 1s connect)
- ✅ FallbackHeaders filter - propagacja exception type i message

**Fallback Controller:**
- ✅ **Generyczny FallbackController** - obsługuje dowolne serwisy przez `@PathVariable`
- ✅ **Reaktywny stack** - użycie `ServerWebExchange` + `Mono` zamiast servlet API
- ✅ Standardizowana odpowiedź fallback (timestamp, status, service, method, exception details)
- ✅ Działa dla wszystkich HTTP methods (GET, POST, PUT, DELETE, PATCH)

**Testy (Spock Framework + Groovy):**
- ✅ `FallbackControllerSpec.groovy` - testy jednostkowe fallback endpoint
- ✅ `GatewayRoutingSpec.groovy` - testy integracyjne routing configuration
- ✅ `CircuitBreakerSpec.groovy` - testy Circuit Breaker configuration
- ✅ `application-test.yml` - dedykowana konfiguracja testowa
- ✅ 20+ test cases pokrywających wszystkie scenariusze

**Zmiany w plikach:**
- `pom.xml` - dodano Spock, Groovy, gmavenplus-plugin, reactor-resilience4j
- `src/main/resources/application.yml` - Spring Cloud 2025.x structure
- `src/main/java/.../fallback/FallbackController.java` - generyczny, reaktywny controller
- `src/test/groovy/.../FallbackControllerSpec.groovy` - testy Spock
- `src/test/groovy/.../GatewayRoutingSpec.groovy` - testy routing
- `src/test/groovy/.../CircuitBreakerSpec.groovy` - testy CB
- `src/test/resources/application-test.yml` - test configuration
- Usunięto: `src/main/java/.../routes/Routes.java`
- Usunięto: `src/main/java/.../config/RoutesProperties.java`

**Gateway jest teraz w pełni reusable** - można go użyć w dowolnym projekcie bez modyfikacji kodu Java!
**Testy zapewniają** - stabilność i regresję przy dalszych zmianach!

### 🎯 Sprint 2 - Observability (Tydzień 3-4) ← **NASTĘPNY**
**P0 - Critical:**
4. Faza 5: Correlation ID Global Filter
5. Faza 5: Prometheus Metrics + Grafana
6. Faza 5: Request/Response Logging Filter

**P1 - High:**
7. Faza 5: Distributed Tracing (Zipkin/Jaeger)

**Cel:** Pełna observability - tracking requestów, metryki, logi

### Sprint 3 - Security & Performance (Tydzień 5-6)
**P1 - High:**
8. Faza 6: Security Headers Filter (SecureHeaders)
9. Faza 6: Rate Limiting (Redis-based)
10. Faza 7: Netty Performance Tuning

### Sprint 4 - Advanced Features (Tydzień 7-8)
**P2 - Nice to Have:**
11. Faza 7: Response Caching (LocalResponseCache)
12. Faza 8: Weighted Routing (canary deployments)
13. Faza 6: JWT Authentication Filter
14. Faza 3: Eureka Service Discovery (gdy będziesz skalować)

### Continuous
- Testing (integration, load tests z Gatling)
- Documentation (API docs, runbooks)
- Monitoring dashboards (Grafana)
- Performance profiling

## Metryki Sukcesu

**Performance:**
- Response time p50: < 50ms
- Response time p95: < 200ms
- Response time p99: < 500ms
- Throughput: > 10,000 req/s

**Reliability:**
- Dostępność: > 99.9%
- Circuit breaker activation: < 1% requests
- Successful retries: > 80%

**Development:**
- Czas dodania nowego serwisu: < 5 minut (dodanie w YAML)
- Zero hardcoded URLs (wszystko przez Eureka)
- Hot-reload support (Actuator refresh)

**Security:**
- All OWASP headers present
- Rate limiting active: < 0.1% 429 errors
- No exposed sensitive headers

**Observability:**
- 100% requests tracked (correlation ID)
- Prometheus metrics exposed
- Distributed tracing enabled (sampling rate configurable)
- Structured JSON logs

## Podsumowanie - Co Warto Zaimplementować

### 🔴 MUST HAVE (Sprint 1-2):
1. ✅ **Service Discovery** - używasz Docker networking (game-service:8080)
2. 🎯 **Circuit Breaker Fallbacks** - graceful degradation ← **NASTĘPNY KROK**
3. 🎯 **Timeout Configuration** - prevent cascading failures
4. **Correlation ID** - request tracking
5. **Prometheus Metrics** - production monitoring
6. **Request Logging** - audit trail

### 🟡 SHOULD HAVE (Sprint 3):
7. **Security Headers** - OWASP compliance (SecureHeaders filter)
8. **Rate Limiting** - DoS protection (Redis-based)
9. **Distributed Tracing** - request flow visualization (Zipkin)
10. **Netty Tuning** - performance optimization

### 🟢 NICE TO HAVE (Sprint 4+):
11. **Response Caching** - reduce backend load (LocalResponseCache)
12. **Weighted Routing** - canary deployments (Weight predicate)
13. **JWT Authentication** - centralized auth (custom GlobalFilter)
14. **Advanced Predicates** - Header/Cookie/Time-based routing
15. **Eureka Service Discovery** - gdy będziesz skalować na multiple instances

## Notatki Techniczne

**Spring Cloud Gateway:**
- Już używa WebFlux (Netty) - nie trzeba zmieniać zależności
- Obecna wersja Spring Cloud (2023.0.3) jest aktualna i stabilna
- Natywna konfiguracja YAML jest preferowana nad programowe route'y
- Wszystkie funkcje z planu są dostępne out-of-the-box

**Best Practices:**
- ✅ Używaj nazw kontenerów Docker (już robisz: `game-service:8080`)
- ✅ Zmienne środowiskowe zamiast hardcoded URLs (już masz w `.env`)
- 🎯 Zawsze konfiguruj timeouts (connect + response) ← **NASTĘPNY KROK**
- 🎯 Dodaj circuit breakers do wszystkich backend services
- Monitoruj metryki gateway przez Prometheus/Grafana
- Loguj correlation ID w każdym requestie
- Używaj SecureHeaders filter globalnie
- Rate limiting per user/IP, nie globalnie
- Testuj z realistic load (Gatling/JMeter)

**Production Readiness (Docker Compose):**
- ✅ Docker networking dla service discovery
- Redis dla rate limiting (dodać kontener)
- Zipkin dla distributed tracing (dodać kontener)
- Prometheus + Grafana dla metrics (dodać kontenery)
- ELK/Loki dla centralized logging (opcjonalnie)
- ✅ Health checks (Actuator - już masz)
- Graceful shutdown configuration

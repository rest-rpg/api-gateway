package com.ms.apigateway.fallback;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Generic Fallback Controller for Circuit Breaker fallbacks. Handles any service dynamically based
 * on the URL path.
 *
 * <p>Usage in application.yml: fallbackUri: forward:/fallback/{serviceName}
 *
 * <p>Example: - forward:/fallback/game - forward:/fallback/user - forward:/fallback/payment -
 * forward:/fallback/inventory
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

  /**
   * Generic fallback endpoint that handles any service and any HTTP method.
   *
   * @param serviceName the name of the service (extracted from path)
   * @param exceptionType the type of exception (from FallbackHeaders filter)
   * @param exceptionMessage the exception message (from FallbackHeaders filter)
   * @param exchange the ServerWebExchange (reactive request/response)
   * @return Mono with HTTP 503 response and error details
   */
  @RequestMapping("/{serviceName}")
  public Mono<Map<String, Object>> handleFallback(
      @PathVariable String serviceName,
      @RequestHeader(value = "Exception-Type", required = false) String exceptionType,
      @RequestHeader(value = "Exception-Message", required = false) String exceptionMessage,
      ServerWebExchange exchange) {

    String method = exchange.getRequest().getMethod().name();
    String path = exchange.getRequest().getPath().value();

    log.warn(
        "Fallback triggered for service: {} | Method: {} | Path: {} | Exception: {} - {}",
        serviceName,
        method,
        path,
        exceptionType,
        exceptionMessage);

    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

    return Mono.just(createFallbackResponse(serviceName, method, exceptionType, exceptionMessage));
  }

  /** Creates a standardized fallback response. */
  private Map<String, Object> createFallbackResponse(
      String serviceName, String method, String exceptionType, String exceptionMessage) {

    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", Instant.now().toString());
    response.put("status", 503);
    response.put("error", "Service Unavailable");
    response.put(
        "message",
        String.format(
            "The %s service is temporarily unavailable. Please try again later.", serviceName));
    response.put("service", serviceName);
    response.put("method", method);

    // Add exception details if available
    if (exceptionType != null) {
      response.put("exceptionType", exceptionType);
    }
    if (exceptionMessage != null) {
      response.put("exceptionMessage", exceptionMessage);
    }

    return response;
  }
}

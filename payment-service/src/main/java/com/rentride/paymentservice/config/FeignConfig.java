package com.rentride.paymentservice.config;

import com.rentride.paymentservice.client.CustomFeignErrorDecoder;
import feign.Client;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenFeign client configuration defining resilience timeouts, custom error decoding,
 * and a modern JDK HttpClient supporting standard HTTP methods including PATCH with
 * Spring Cloud LoadBalancer resolution.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomFeignErrorDecoder();
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                3000, TimeUnit.MILLISECONDS, // connectTimeout
                5000, TimeUnit.MILLISECONDS, // readTimeout
                true                         // followRedirects
        );
    }

    /**
     * Modern JDK HTTP client supporting all standard HTTP verbs including PATCH,
     * wrapped with {@link FeignBlockingLoadBalancerClient} when load balancing is active.
     */
    @Bean
    public Client feignClient(
            ObjectProvider<LoadBalancerClient> loadBalancerClientProvider,
            ObjectProvider<LoadBalancerClientFactory> loadBalancerClientFactoryProvider) {

        Client rawClient = new Client() {
            private final HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(3000))
                    .build();

            @Override
            public Response execute(Request request, Request.Options options) throws IOException {
                try {
                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(request.url()))
                            .timeout(Duration.ofMillis(options.readTimeoutMillis()));

                    // Copy request headers (excluding restricted headers)
                    for (Map.Entry<String, Collection<String>> entry : request.headers().entrySet()) {
                        String headerName = entry.getKey();
                        if (!"content-length".equalsIgnoreCase(headerName) && !"host".equalsIgnoreCase(headerName)) {
                            for (String headerVal : entry.getValue()) {
                                reqBuilder.header(headerName, headerVal);
                            }
                        }
                    }

                    String method = request.httpMethod().name();
                    HttpRequest.BodyPublisher bodyPublisher;
                    if (request.body() != null && request.body().length > 0) {
                        bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(request.body());
                    } else if ("PATCH".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                        bodyPublisher = HttpRequest.BodyPublishers.noBody();
                    } else {
                        bodyPublisher = null;
                    }

                    if (bodyPublisher != null) {
                        reqBuilder.method(method, bodyPublisher);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        reqBuilder.GET();
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        reqBuilder.DELETE();
                    } else {
                        reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                    }

                    HttpResponse<byte[]> httpResponse = httpClient.send(
                            reqBuilder.build(),
                            HttpResponse.BodyHandlers.ofByteArray()
                    );

                    Map<String, Collection<String>> respHeaders = new HashMap<>();
                    httpResponse.headers().map().forEach(respHeaders::put);

                    return Response.builder()
                            .status(httpResponse.statusCode())
                            .reason(httpResponse.statusCode() == 200 ? "OK" : "Status " + httpResponse.statusCode())
                            .headers(respHeaders)
                            .body(httpResponse.body() != null ? httpResponse.body() : new byte[0])
                            .request(request)
                            .build();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("HTTP request execution interrupted", e);
                }
            }
        };

        LoadBalancerClient loadBalancerClient = loadBalancerClientProvider.getIfAvailable();
        LoadBalancerClientFactory loadBalancerClientFactory = loadBalancerClientFactoryProvider.getIfAvailable();

        if (loadBalancerClient != null && loadBalancerClientFactory != null) {
            return new FeignBlockingLoadBalancerClient(rawClient, loadBalancerClient, loadBalancerClientFactory);
        }

        return rawClient;
    }
}

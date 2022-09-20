package de.transline.labs.translation.tlc.facade.client;

import de.transline.labs.translation.tlc.facade.TLCRestClient;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by aga on 26.01.2022.
 */
public class DefaultTLCRestClient implements TLCRestClient<Order>{

    private static final String API_BASE_PATH = "/api/external/v2/orders";

    private RestTemplate restTemplate = new RestTemplate();

    private final String apiUrl;
    private final String apiKey;

    public DefaultTLCRestClient(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public Order getOrder(String orderId) {
        try {
            String url = API_BASE_PATH + "/" + orderId;
            return execute(url, HttpMethod.GET, null, Order.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    public String createOrder(String title, String description, String sourceLanguage, Set<String> targetLanguages) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);
        body.put("source_language", sourceLanguage);
        body.put("target_languages", targetLanguages);

        Map response = execute(API_BASE_PATH, HttpMethod.POST, body, Map.class);

        return response.get("order_id").toString();
    }

    public void uploadFile(String orderId, Resource resource) {
        String url = API_BASE_PATH + "/" + orderId + "/files/upload";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        executeUpload(url, HttpMethod.PUT, body, String.class);
    }

    public void finishUpload(String orderId) {
        String url = API_BASE_PATH + "/" + orderId + "/order";

        Map<String, Object> body = new HashMap<>();
        body.put("order_type", "direct_order");

        execute(url, HttpMethod.PUT, body, String.class);
    }

    public InputStream downLoadFile(String downloadUrl) throws IOException {
      // Usage of executeDownload was removed because it results in an 404 error.
      //Resource resource = executeDownload(downloadUrl, HttpMethod.GET, null, Resource.class);
      URI uri = toUri(downloadUrl);
      HttpHeaders headers = createHttpHeaders(new HttpHeaders());
      Resource resource = restTemplate.exchange(
              new RequestEntity(null, headers, HttpMethod.GET, uri),
              Resource.class
      ).getBody();
      return resource.getInputStream();
    }

    private <ReturnType, RequestType> ReturnType execute(String apiPath, HttpMethod method, RequestType requestBody, Class<ReturnType> clazz)
    {
        HttpHeaders headers = createHttpHeaders(new HttpHeaders(), MediaType.APPLICATION_JSON);
        return execute(apiPath, method, requestBody, clazz, headers);
    }

    private <ReturnType, RequestType> ReturnType executeUpload(String apiPath, HttpMethod method, RequestType requestBody, Class<ReturnType> clazz)
    {
        HttpHeaders headers = createHttpHeaders(new HttpHeaders(), MediaType.MULTIPART_FORM_DATA);
        return execute(apiPath, method, requestBody, clazz, headers);
    }

    private <ReturnType, RequestType> ReturnType executeDownload(String apiPath, HttpMethod method, RequestType requestBody, Class<ReturnType> clazz)
    {
        HttpHeaders headers = createHttpHeaders(new HttpHeaders());
        return execute(apiPath, method, requestBody, clazz, headers);
    }

    private <ReturnType, RequestType> ReturnType execute(String apiPath, HttpMethod method, RequestType requestBody, Class<ReturnType> clazz,
        HttpHeaders headers)
    {
        URI uri = this.toUri(apiPath);
        return restTemplate.exchange(
            new RequestEntity(requestBody, headers, method, uri),
            clazz
        ).getBody();
    }

    private HttpHeaders createHttpHeaders(HttpHeaders headers, MediaType mediaType) {
        HttpHeaders httpHeaders = createHttpHeaders(headers);
        httpHeaders.setContentType(mediaType);
        return httpHeaders;
    }

    private HttpHeaders createHttpHeaders(HttpHeaders headers) {
        HttpHeaders httpHeaders = HttpHeaders.writableHttpHeaders(headers);
        httpHeaders.set("Authorization", "API-Key " + apiKey);
        return httpHeaders;
    }

    private URI toUri(String apiPath) {
        try {
            if (apiPath.startsWith("/api")) {
              return new URI(apiUrl + apiPath);
            } else {
              return new URI(apiPath);
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error creating uri. " + e.getMessage(), e);
        }
    }
}

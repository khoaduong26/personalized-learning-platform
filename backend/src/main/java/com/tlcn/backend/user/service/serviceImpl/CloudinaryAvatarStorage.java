package com.tlcn.backend.user.service.serviceImpl;

import com.tlcn.backend.user.exception.ProfileException;
import com.tlcn.backend.user.service.AvatarStorage;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CloudinaryAvatarStorage implements AvatarStorage {
    private final RestClient client;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    @Autowired
    public CloudinaryAvatarStorage(@Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret) {
        this(cloudName, apiKey, apiSecret, createClient());
    }

    CloudinaryAvatarStorage(String cloudName, String apiKey, String apiSecret, RestClient client) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.client = client;
    }

    private static RestClient createClient() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String upload(Long userId, byte[] image, String contentType) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ProfileException(HttpStatus.SERVICE_UNAVAILABLE, "AVATAR_STORAGE_UNAVAILABLE",
                    "Chưa cấu hình dịch vụ ảnh đại diện.");
        }
        if (!cloudName.matches("[a-zA-Z0-9_-]+")) throw unavailable();
        var fields = new LinkedMultiValueMap<String, Object>();
        var imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.parseMediaType(contentType));
        var resource = new ByteArrayResource(image) {
            @Override public String getFilename() { return "avatar" + ("image/png".equals(contentType) ? ".png" : ".jpg"); }
        };
        fields.add("file", new HttpEntity<>(resource, imageHeaders));
        // Unique assets ensure a failed DB update cannot overwrite the currently saved avatar.
        fields.add("public_id", "tlcn/avatars/" + userId + "/" + UUID.randomUUID());
        fields.add("overwrite", "false");
        try {
            Map<?, ?> response = client.post()
                    .uri("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .headers(headers -> headers.setBasicAuth(apiKey, apiSecret))
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(fields).retrieve().body(Map.class);
            if (response == null || !(response.get("secure_url") instanceof String url)) throw unavailable();
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"res.cloudinary.com".equals(uri.getHost())
                    || uri.getUserInfo() != null || !uri.getPath().startsWith("/" + cloudName + "/")) throw unavailable();
            return url;
        } catch (RestClientException | IllegalArgumentException ex) {
            // Do not expose provider responses or credentials in errors/logs.
            throw unavailable();
        }
    }

    private ProfileException unavailable() {
        return new ProfileException(HttpStatus.BAD_GATEWAY, "AVATAR_UPLOAD_FAILED",
                "Chưa tải được ảnh đại diện. Vui lòng thử lại.");
    }
}

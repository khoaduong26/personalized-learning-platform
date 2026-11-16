package com.tlcn.backend.user.service.serviceImpl;

import com.tlcn.backend.user.exception.ProfileException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryAvatarStorageTests {
    private MockRestServiceServer server;
    private RestClient client;

    @BeforeEach void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
    }

    @Test void authenticatedMultipartUploadReturnsSecureUrl() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Basic a2V5OnNlY3JldA=="))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"file\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tlcn/avatars/42/")))
                .andRespond(withSuccess("{\"secure_url\":\"https://res.cloudinary.com/demo/image/upload/avatar.png\"}", MediaType.APPLICATION_JSON));
        var storage = new CloudinaryAvatarStorage("demo", "key", "secret", client);
        assertEquals("https://res.cloudinary.com/demo/image/upload/avatar.png", storage.upload(42L, new byte[]{1, 2}, "image/png"));
        server.verify();
    }

    @Test void missingConfigurationFailsWithoutNetwork() {
        var storage = new CloudinaryAvatarStorage("", "", "", client);
        var error = assertThrows(ProfileException.class, () -> storage.upload(42L, new byte[]{1}, "image/png"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        assertEquals("AVATAR_STORAGE_UNAVAILABLE", error.getCode());
        server.verify();
    }

    @Test void providerFailureDoesNotExposeResponseSecrets() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private provider detail secret"));
        var storage = new CloudinaryAvatarStorage("demo", "key", "secret", client);
        var error = assertThrows(ProfileException.class, () -> storage.upload(42L, new byte[]{1}, "image/png"));
        assertEquals("AVATAR_UPLOAD_FAILED", error.getCode());
        assertFalse(error.getMessage().contains("secret"));
        server.verify();
    }

    @Test void invalidProviderUrlIsNotAccepted() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andRespond(withSuccess("{\"secure_url\":\"https://evil.invalid/a.png\"}", MediaType.APPLICATION_JSON));
        var storage = new CloudinaryAvatarStorage("demo", "key", "secret", client);
        assertThrows(ProfileException.class, () -> storage.upload(42L, new byte[]{1}, "image/png"));
        server.verify();
    }
}

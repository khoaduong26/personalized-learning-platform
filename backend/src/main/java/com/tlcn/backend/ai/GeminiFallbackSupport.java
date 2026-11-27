package com.tlcn.backend.ai;

import java.util.Locale;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Shared policy for deciding when a Gemini provider failure can use DeepSeek. */
public final class GeminiFallbackSupport {
    private GeminiFallbackSupport() {
    }

    public static boolean shouldUseDeepSeek(RuntimeException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        if (exception instanceof IllegalStateException) {
            return true;
        }
        if (!(exception instanceof RestClientResponseException responseException)) {
            return false;
        }

        int status = responseException.getStatusCode().value();
        if (status == 400 || status == 401 || status == 404 || status == 429
                || (status >= 500 && status <= 599)) {
            return true;
        }
        if (status != 402 && status != 403) {
            return false;
        }

        String body = responseException.getResponseBodyAsString().toLowerCase(Locale.ROOT);
        return body.contains("quota")
                || body.contains("resource_exhausted")
                || body.contains("rate limit")
                || body.contains("rate_limit")
                || body.contains("credit")
                || body.contains("billing");
    }
}

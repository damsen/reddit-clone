package com.redditclone.redditservice.utils;

import io.rsocket.metadata.WellKnownMimeType;
import lombok.experimental.UtilityClass;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.BearerTokenMetadata;
import org.springframework.util.MimeTypeUtils;

import java.util.function.Consumer;

@UtilityClass
public class RSocketUtils {

    public Consumer<RSocketRequester.MetadataSpec<?>> addTokenToMetadata(String token) {
        var mimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());
        var bearerTokenMetadata = new BearerTokenMetadata(token);
        return spec -> spec.metadata(bearerTokenMetadata, mimeType);
    }
}

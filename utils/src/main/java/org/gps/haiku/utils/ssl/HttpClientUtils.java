package org.gps.haiku.utils.ssl;


import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by leogps on 4/1/18.
 */
public class HttpClientUtils {

    private static final Logger LOGGER = LogManager.getLogger(HttpClientUtils.class);

    @Getter
    private static final Client client;
    static {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        client = clientBuilder.build();
    }


    public static String tryParse(String url) {
        try {
            final WebTarget target = client.target(url);
            return target.getUri().toString();
        } catch (Exception e) {
            LOGGER.warn("Failed to parse url: " + url, e);
        }
        return url;
    }
}

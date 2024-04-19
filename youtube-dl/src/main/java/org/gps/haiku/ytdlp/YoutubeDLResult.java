package org.gps.haiku.ytdlp;

import lombok.Data;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.util.Date;

/**
 * Created by leogps on 12/17/15.
 */
@Data
public class YoutubeDLResult {
    private final String title;
    private final String url;
    private final String filename;
    private final String id;
    private final String error;
    private final Date timestamp = new Date();

    public YoutubeDLResult(String id, String title, String url, String filename, String error) {
        this.title = title;
        this.url = url;
        this.filename = filename;
        this.id = id;
        this.error = error;
    }

    public String buildUrlFromId() {
        return String.format("https://www.youtube.com/watch?v=%s", id);
    }

    public Date parseExpiryTime() {
        try {
            String valueStr = new URIBuilder(url).getQueryParams()
                    .stream()
                    .filter(param -> param.getName().equals("expire"))
                    .findFirst()
                    .map(NameValuePair::getValue)
                    .orElse(null);
            if (valueStr == null) {
                return null;
            }
            try {
                long unixTime = Long.parseLong(valueStr);
                return new Date(unixTime * 1000);
            } catch (NumberFormatException e) {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "YoutubeDLResult{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", filename='" + filename + '\'' +
                ", id='" + id + '\'' +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

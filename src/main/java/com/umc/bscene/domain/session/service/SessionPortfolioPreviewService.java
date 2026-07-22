package com.umc.bscene.domain.session.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.session.entity.SessionApplicationLink;
import com.umc.bscene.domain.session.enums.PortfolioMediaType;
import com.umc.bscene.domain.session.event.SessionPortfolioPreviewRequestedEvent;
import com.umc.bscene.domain.session.repository.SessionApplicationLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionPortfolioPreviewService {
    private static final int MAX_RESPONSE_BYTES = 512 * 1024;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final Pattern META_TAG = Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROPERTY = Pattern.compile(
            "(?:property|name)\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT = Pattern.compile(
            "content\\s*=\\s*['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_TAG = Pattern.compile(
            "<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final SessionApplicationLinkRepository linkRepository;
    private final TransactionTemplate transactionTemplate;
    private final SessionPortfolioVideoThumbnailService videoThumbnailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Async("sessionPortfolioPreviewExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SessionPortfolioPreviewRequestedEvent event) {
        SessionApplicationLink link = linkRepository.findById(event.sessionApplicationLinkId())
                .orElse(null);
        if (link == null || link.getDeletedAt() != null) return;

        try {
            Preview preview = extract(link.getUrl());
            savePreview(event.sessionApplicationLinkId(), preview);
            if (preview.mediaType() == PortfolioMediaType.VIDEO) {
                videoThumbnailService.generateAsync(
                        event.sessionApplicationLinkId(), link.getUrl());
            }
        } catch (Exception exception) {
            log.warn("세션 포트폴리오 미리보기 추출 실패: linkId={}, url={}",
                    event.sessionApplicationLinkId(), link.getUrl(), exception);
            savePreview(event.sessionApplicationLinkId(), fallback(link.getUrl()));
        }
    }

    private void savePreview(Long linkId, Preview preview) {
        transactionTemplate.executeWithoutResult(status -> linkRepository
                .findById(linkId)
                .filter(current -> current.getDeletedAt() == null)
                .ifPresent(current -> current.applyPreview(
                        truncate(preview.title()), preview.thumbnailUrl(), preview.mediaType())));
    }

    private Preview extract(String value) throws Exception {
        URI uri = validateExternalUri(value);
        String host = uri.getHost().toLowerCase(Locale.ROOT);

        if (host.equals("youtu.be") || host.endsWith(".youtube.com")) {
            String videoId = youtubeVideoId(uri);
            String thumbnail = videoId == null ? null
                    : "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            String title;
            try {
                title = fetchOEmbedTitle(
                        "https://www.youtube.com/oembed?format=json&url=" + encode(uri.toString()));
            } catch (Exception ignored) {
                title = "YouTube";
            }
            return new Preview(title, thumbnail, PortfolioMediaType.YOUTUBE);
        }
        if (host.equals("soundcloud.com") || host.endsWith(".soundcloud.com")) {
            JsonNode data = fetchJson(
                    URI.create("https://soundcloud.com/oembed?format=json&url="
                            + encode(uri.toString())));
            return new Preview(text(data, "title"), text(data, "thumbnail_url"),
                    PortfolioMediaType.SOUNDCLOUD);
        }

        PortfolioMediaType directType = directMediaType(uri.getPath());
        if (directType == PortfolioMediaType.IMAGE) {
            return new Preview(fileName(uri), uri.toString(), directType);
        }
        if (directType == PortfolioMediaType.VIDEO) {
            return new Preview(fileName(uri), null, directType);
        }

        HttpResponse<InputStream> response = send(uri, "text/html,application/xhtml+xml");
        String html = readLimited(response);
        String title = firstMeta(html, "og:title", "twitter:title");
        if (title == null) title = titleTag(html);
        if (title == null) title = uri.getHost();
        String thumbnail = firstMeta(html, "og:image", "twitter:image");
        return new Preview(title, resolveUrl(uri, thumbnail), PortfolioMediaType.LINK);
    }

    private Preview fallback(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
            if (host != null && (host.equals("youtu.be") || host.endsWith(".youtube.com"))) {
                String videoId = youtubeVideoId(uri);
                return new Preview("YouTube", videoId == null ? null
                        : "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg",
                        PortfolioMediaType.YOUTUBE);
            }
            if (host != null && (host.equals("soundcloud.com")
                    || host.endsWith(".soundcloud.com"))) {
                return new Preview("SoundCloud", null, PortfolioMediaType.SOUNDCLOUD);
            }
            PortfolioMediaType type = directMediaType(uri.getPath());
            String title = type == PortfolioMediaType.LINK ? host : fileName(uri);
            String thumbnail = type == PortfolioMediaType.IMAGE ? value : null;
            return new Preview(title, thumbnail, type);
        } catch (RuntimeException ignored) {
            return new Preview(null, null, PortfolioMediaType.LINK);
        }
    }

    private String fetchOEmbedTitle(String url) throws Exception {
        return text(fetchJson(URI.create(url)), "title");
    }

    private JsonNode fetchJson(URI uri) throws Exception {
        HttpResponse<InputStream> response = send(uri, "application/json");
        return objectMapper.readTree(readLimited(response));
    }

    private HttpResponse<InputStream> send(URI uri, String accept) throws Exception {
        validateExternalUri(uri.toString());
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Accept", accept)
                .header("User-Agent", "BScene-LinkPreview/1.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalStateException("미리보기 서버 응답 코드: " + response.statusCode());
        }
        return response;
    }

    private String readLimited(HttpResponse<InputStream> response) throws Exception {
        try (InputStream input = response.body()) {
            byte[] bytes = input.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new IllegalStateException("미리보기 응답 크기 초과");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private URI validateExternalUri(String value) throws Exception {
        URI uri = URI.create(value);
        if (uri.getScheme() == null
                || !(uri.getScheme().equalsIgnoreCase("http")
                || uri.getScheme().equalsIgnoreCase("https"))
                || uri.getHost() == null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("외부 HTTP URL만 허용됩니다.");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            byte[] raw = address.getAddress();
            boolean uniqueLocalV6 = address instanceof Inet6Address
                    && raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress() || uniqueLocalV6) {
                throw new IllegalArgumentException("내부 네트워크 URL은 허용되지 않습니다.");
            }
        }
        return uri;
    }

    private String firstMeta(String html, String... keys) {
        Matcher tags = META_TAG.matcher(html);
        while (tags.find()) {
            String tag = tags.group();
            Matcher property = PROPERTY.matcher(tag);
            Matcher content = CONTENT.matcher(tag);
            if (!property.find() || !content.find()) continue;
            for (String key : keys) {
                if (property.group(1).equalsIgnoreCase(key)) return decode(content.group(1));
            }
        }
        return null;
    }

    private String titleTag(String html) {
        Matcher matcher = TITLE_TAG.matcher(html);
        return matcher.find() ? decode(matcher.group(1).replaceAll("<[^>]+>", "")) : null;
    }

    private String decode(String value) {
        return value == null ? null : value.replace("&amp;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").strip();
    }

    private String youtubeVideoId(URI uri) {
        if (uri.getHost().equalsIgnoreCase("youtu.be")) {
            String path = uri.getPath();
            return path.length() > 1 ? validYoutubeId(path.substring(1).split("/")[0]) : null;
        }
        if (uri.getPath().equals("/watch") && uri.getRawQuery() != null) {
            for (String pair : uri.getRawQuery().split("&")) {
                String[] entry = pair.split("=", 2);
                if (entry.length == 2 && entry[0].equals("v")) return validYoutubeId(entry[1]);
            }
        }
        String[] parts = uri.getPath().split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("shorts") || parts[i].equals("embed")) {
                return validYoutubeId(parts[i + 1]);
            }
        }
        return null;
    }

    private String validYoutubeId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{6,20}") ? value : null;
    }

    private PortfolioMediaType directMediaType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$")) return PortfolioMediaType.IMAGE;
        if (lower.matches(".*\\.(mp4|mov|webm|m4v|avi)$")) return PortfolioMediaType.VIDEO;
        return PortfolioMediaType.LINK;
    }

    private String fileName(URI uri) {
        String path = uri.getPath();
        int slash = path.lastIndexOf('/');
        return decode(slash >= 0 ? path.substring(slash + 1) : path);
    }

    private String resolveUrl(URI base, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI resolved = base.resolve(value);
            validateExternalUri(resolved.toString());
            return resolved.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_TITLE_LENGTH ? value : value.substring(0, MAX_TITLE_LENGTH);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Preview(String title, String thumbnailUrl, PortfolioMediaType mediaType) {
    }
}

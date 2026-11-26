package com.umc.bscene.global.media;

import java.net.URI;
import java.util.Locale;

public final class MediaFileType {

    private MediaFileType() {
    }

    public static boolean isGifPath(String path) {
        return path != null && path.toLowerCase(Locale.ROOT).endsWith(".gif");
    }

    public static boolean isGifUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            return isGifPath(URI.create(url).getPath());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}

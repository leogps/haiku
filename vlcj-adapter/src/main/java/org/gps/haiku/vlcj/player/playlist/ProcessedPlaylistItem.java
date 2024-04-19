package org.gps.haiku.vlcj.player.playlist;

import lombok.Data;

import java.util.Objects;
import java.util.UUID;

/**
 * @author leogps
 * Created on 4/19/24
 */
@Data
public class ProcessedPlaylistItem<T> implements PlaylistItem<T> {
    private final String id;
    private final long trackId;
    private final String name;
    private final String artist;
    private final String album;
    private String location;
    private final boolean isMovie;
    private T model;

    public ProcessedPlaylistItem(final long trackId, final String name, final String artist,
                                 final String album, final String location, final boolean isMovie,
                                 final T model) {
        this.id = UUID.randomUUID().toString();
        this.trackId = trackId;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.location = location;
        this.isMovie = isMovie;
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaylistItem)) return false;

        PlaylistItem that = (PlaylistItem) o;

        return Objects.equals(id, that.getId());

    }

    @Override
    public int hashCode() {
        return 31 * (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean needsProcessing() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}

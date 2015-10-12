/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gps.itunes.media.player.ui.tasks;

import com.gps.ilp.utils.Constants;
import com.gps.itunes.lib.items.playlists.Playlist;
import com.gps.itunes.lib.items.tracks.Track;
import com.gps.itunes.media.player.ui.handlers.ProgressHandler;
import com.gps.itunes.media.player.ui.holders.PlaylistHolder;
import com.gps.itunes.media.player.ui.holders.TrackHolder;
import com.gps.itunes.media.player.ui.tablehelpers.models.TracksTableModel;
import com.gps.itunes.lib.tasks.LibraryParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 *
 * Loads the tracks for any selected playlists.
 * 
 * @author leogps
 */
public class TracksLoader extends ProgressHandler {

    private static org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(TracksLoader.class);
    
    private final JTable playlistTable;
    private final JTable tracksTable;
    private final JMenuItem copyPlaylistsMenuItem;
    private final LibraryParser parser;
    private final String searchQuery;
    private final JLabel tracksTableHeadingLabel;

    private static final int TRACKS_TABLE_HEADING_LENGTH_LIMIT = 100;

    public TracksLoader(final LibraryParser parser, final JProgressBar progressBar,
            final JTable tracksTable, final JTable playlistTable,
            final JMenuItem copyPlaylistsMenuItem,
            final JLabel tracksTableHeadingLabel) {
        super(progressBar, TaskType.SUB_TASK);
        this.parser = parser;
        this.tracksTable = tracksTable;
        this.playlistTable = playlistTable;
        this.copyPlaylistsMenuItem = copyPlaylistsMenuItem;
        this.searchQuery = null;
        this.tracksTableHeadingLabel = tracksTableHeadingLabel;
    }
    
    public TracksLoader(final LibraryParser parser, final JProgressBar progressBar,
            final JTable tracksTable, final JTable playlistTable,
            final JMenuItem copyPlaylistsMenuItem,
            final JLabel tracksTableHeadingLabel,
            final String searchQuery) {
        super(progressBar, TaskType.SUB_TASK);
        this.parser = parser;
        this.tracksTable = tracksTable;
        this.playlistTable = playlistTable;
        this.copyPlaylistsMenuItem = copyPlaylistsMenuItem;
        this.searchQuery = searchQuery;
        this.tracksTableHeadingLabel = tracksTableHeadingLabel;
    }

    @Override
    public void runTask(final TaskParams params) {
        try{

            final TracksTableModel model =
                    (TracksTableModel) tracksTable.getModel();

            model.clearTable(tracksTable);

            final Map<Long, Track> trackMap = new HashMap<Long, Track>();

            List<Playlist> playlistList = new ArrayList<Playlist>();
            for (final int selectedRow : playlistTable.getSelectedRows()) {

                final Playlist playlist =
                        ((PlaylistHolder) playlistTable.getValueAt(selectedRow, 0)).getPlaylist();
                playlistList.add(playlist);

                final Track[] tracks =
                        parser.getPlaylistTracks(playlist.getPlaylistId());


                for(final Track track : tracks){
                    if(searchQuery == null || Constants.EMPTY.equalsIgnoreCase(searchQuery)){
                        trackMap.put(track.getTrackId(), track);
                    } else if(match(track)){
                        trackMap.put(track.getTrackId(), track);
                    }
                }
            }

            addTracks(model, trackMap.values().toArray(new Track[trackMap.size()]));

            if(playlistTable.getSelectedRows().length > 0
                    && tracksTable.getRowCount() > 0){
                enableCopyPlaylists(true);
            } else {
                enableCopyPlaylists(false);
            }

            String tracksTableHeadingText;
            if(searchQuery == null || Constants.EMPTY.equals(searchQuery)){

                log.info(trackMap.size() + " track(s) in " + playlistTable.getSelectedRows().length + " selected playlist(s).");
                setProgressMsg("Tracks loaded.");
                tracksTableHeadingText = getTracksTableHeading(playlistList);

            } else {
                log.info(trackMap.size() + " track(s) found.");
                setProgressMsg("Tracks loaded.");
                tracksTableHeadingText = getTracksTableHeading(playlistList, searchQuery);
            }

            // Setting Track table heading. Truncating the heading based on the limit.
            if(tracksTableHeadingText.length() > TRACKS_TABLE_HEADING_LENGTH_LIMIT) {
                tracksTableHeadingLabel.setText(tracksTableHeadingText.substring(0, TRACKS_TABLE_HEADING_LENGTH_LIMIT) + "...");
                tracksTableHeadingLabel.setToolTipText(tracksTableHeadingText);
            } else {
                tracksTableHeadingLabel.setText(tracksTableHeadingText);
                tracksTableHeadingLabel.setToolTipText(tracksTableHeadingText);
            }
        
        } catch(Exception ex){
            log.error(ex);
        }

    }

    private String getTracksTableHeading(List<Playlist> playlistList, String searchQuery) {
        if(Constants.EMPTY.equalsIgnoreCase(searchQuery)) {
            getTracksTableHeading(playlistList);
        }
        return String.format("Matches for '%s' in %s", searchQuery, getTracksTableHeading(playlistList));
    }

    private String getTracksTableHeading(List<Playlist> playlistList) {
        if(playlistList.size() == 1) {
            return playlistList.get(0).getName();
        }
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < playlistList.size(); i++) {
            Playlist playlist = playlistList.get(i);
            if(i != 0) {
                buffer.append(", ");
            }
            buffer.append(playlist.getName());
        }

        return buffer.toString();
    }

    private void addTracks(final TracksTableModel model, final Track[] tracks) {


        for (final Track track : tracks) {

            final TrackHolder holder = new TrackHolder(track);

            model.addRow(new Object[]{
                model.getRowCount() + 1, //S.No.
                holder, //Name
                TrackDataParser.parseTime(
                    track.getAdditionalTrackInfo().getAdditionalInfo(TIME) //Time
                    ), 
                track.getAdditionalTrackInfo().getAdditionalInfo(ARTIST), //Artist
                track.getAdditionalTrackInfo().getAdditionalInfo(ALBUM), //Album 
                track.getAdditionalTrackInfo().getAdditionalInfo(GENRE), //Genre 
                track.getAdditionalTrackInfo().getAdditionalInfo(RATING), //Rating
                track.getAdditionalTrackInfo().getAdditionalInfo(PLAYS), //Plays
            });

        }
    }
    
    private void enableCopyPlaylists(final boolean isEnable) {
        copyPlaylistsMenuItem.setEnabled(isEnable);
    }
    
    private static final String ARTIST = "Artist";
    private static final String ALBUM = "Album";
    private static final String TIME = "Total Time";
    private static final String GENRE = "Genre";
    private static final String RATING = "Rating";
    private static final String PLAYS = "Play Count";

    private boolean match(Track track) {
        
        return (doMatch(track.getTrackName()))
            || (doMatch(track.getAdditionalTrackInfo().getAdditionalInfo(ARTIST))) 
            || (doMatch(track.getAdditionalTrackInfo().getAdditionalInfo(ALBUM)));
    }
    
    private boolean doMatch(final String str){
        return str != null && str.toUpperCase().contains(searchQuery);
    }
    
    
}

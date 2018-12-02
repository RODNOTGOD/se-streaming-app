package com.dotify.music.dotify;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class UserLibrary extends Fragment {

    private JSONArray userLibrary;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.user_library, container, false);
        JSONArray userLibrary = getSongs();
        if (userLibrary == null){
            rootView = ErrorDisplay.displayError("Failed to connect to server", this, R.id.main_feed_fragment);
        } else {
            this.userLibrary = userLibrary;
            GridView albumView = rootView.findViewById(R.id.library_albums);

            HashMap<String, Artist> builtLibrary = buildLibrary(userLibrary);
            ArrayList<HashMap<String, String>> adaptedArtists = new ArrayList<>();

            Set<String> keys = builtLibrary.keySet();
            for (String key : keys) {
                Artist artist = builtLibrary.get(key);
                HashMap<String, String> item = new HashMap<>();
                item.put("artist", artist.getName());
                adaptedArtists.add(item);
            }

            String[] from = {"artist"};
            int[] to = {R.id.artist_display_title};

            System.out.println(adaptedArtists);
            View finalRootView = rootView;
            SimpleAdapter adapter = new SimpleAdapter(this.getContext(), adaptedArtists, R.layout.artist_display, from, to) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view =  super.getView(position, convertView, parent);
                    TextView textView = view.findViewById(R.id.artist_display_title);
                    textView.setOnClickListener((v) -> {
                        updateViewToAlbums(finalRootView, builtLibrary.get(textView.getText()));
                    });
                    return view;
                }
            };
            albumView.setAdapter(adapter);
        }
        return rootView;
    }

    private JSONArray getSongs() {
        DatabaseRetrieve music = new DatabaseRetrieve();
        try {
            music.execute("http://192.168.1.160:8080/getAllSongs.php");
            return music.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<String, Artist> buildLibrary(@NonNull JSONArray data){
        HashMap<String, Artist> discography = new HashMap<>();
        String artistName, albumName, songName;

        for (int i = 0; i < data.length(); i++) {
            try {
                artistName = data.getJSONObject(i).getString("artist");
                albumName = data.getJSONObject(i).getString("album");
                songName = data.getJSONObject(i).getString("song");
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
            Artist artist = discography.get(artistName);
            if (artist == null) artist = new Artist(artistName);
            Album album = artist.addAlbumIfEmpty(albumName);
            album.addSong(songName);
            artist.updateAlbum(album);
            discography.put(artistName, artist);
        }

        return discography;
    }

    private void updateViewToAlbums(View rootView, Artist artist) {
        GridView gridView = rootView.findViewById(R.id.library_albums);
        HashMap<String, Album> discography = artist.getDiscography();
        ArrayList<HashMap<String, String>> adaptedAlbums = new ArrayList<>();

        for (String albumTitle : discography.keySet()) {
            HashMap<String, String> albums = new HashMap<>();
            Album album = discography.get(albumTitle);
            albums.put("_album", album.getTitle());
            adaptedAlbums.add(albums);
        }

        String[] from = {"_album"};
        int[] to = {R.id.artist_display_title};
        SimpleAdapter adapter = new SimpleAdapter(getContext(), adaptedAlbums, R.layout.artist_display, from, to) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view =  super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.artist_display_title);
                textView.setOnClickListener((v) -> {
                    updateViewToSongs(rootView, discography.get(textView.getText()));
                });
                return view;
            }
        };
        gridView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void updateViewToSongs(View rootView, Album album) {
        GridView gridView = rootView.findViewById(R.id.library_albums);
        HashMap<String, Song> songList = album.getSongs();
        ArrayList<HashMap<String, String>> adaptedSongs = new ArrayList<>();

        for (String songTitle : songList.keySet()) {
            HashMap<String, String> songs = new HashMap<>();
            Song song = songList.get(songTitle);
            songs.put("_song", songTitle);
            adaptedSongs.add(songs);
        }

        String[] from = {"_song"};
        int[] to = {R.id.artist_display_title};
        SimpleAdapter adapter = new SimpleAdapter(getContext(), adaptedSongs, R.layout.artist_display, from, to);
        gridView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}

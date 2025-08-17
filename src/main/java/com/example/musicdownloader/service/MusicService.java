package com.example.musicdownloader.service;

import java.util.List;
import java.util.Map;

public interface MusicService {
    List<Map<String, Object>> searchSongs(String keyword) throws Exception;
    List<Map<String, Object>> searchArtistSongs(String artist, String command) throws Exception;
    String downloadSong(Map<String, Object> song) throws Exception;
}
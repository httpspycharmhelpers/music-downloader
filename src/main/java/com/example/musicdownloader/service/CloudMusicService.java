package com.example.musicdownloader.service;

import com.example.musicdownloader.utils.HttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudMusicService implements MusicService {

    @Value("${download.dir}")
    private String downloadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SEARCH_URL = "https://music.163.com/api/cloudsearch/pc";
    private static final String DOWNLOAD_URL = "https://music.163.com/api/song/enhance/player/url";

    @Override
    public List<Map<String, Object>> searchSongs(String keyword) throws Exception {
        return search(keyword, false);
    }

    @Override
    public List<Map<String, Object>> searchArtistSongs(String artist, String command) throws Exception {
        return search(artist, true);
    }

    private List<Map<String, Object>> search(String keyword, boolean artistSearch) throws Exception {
        String url = SEARCH_URL + "?s=" + HttpUtils.encodeUrl(keyword) + "&type=1&offset=0&limit=100";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://music.163.com/");
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        List<Map<String, Object>> results = new ArrayList<>();
        if (data.containsKey("result") && data.get("result") instanceof Map) {
            Map<?, ?> result = (Map<?, ?>) data.get("result");
            if (result.containsKey("songs") && result.get("songs") instanceof List) {
                List<?> songs = (List<?>) result.get("songs");
                for (int i = 0; i < songs.size(); i++) {
                    Map<?, ?> song = (Map<?, ?>) songs.get(i);
                    
                    // 如果是歌手搜索，检查歌手名称
                    if (artistSearch) {
                        List<?> artists = (List<?>) song.get("ar");
                        boolean artistMatch = false;
                        for (Object artistObj : artists) {
                            Map<?, ?> art = (Map<?, ?>) artistObj;
                            String artistName = (String) art.get("name");
                            if (artistName != null && artistName.toLowerCase().contains(keyword.toLowerCase())) {
                                artistMatch = true;
                                break;
                            }
                        }
                        if (!artistMatch) continue;
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", i + 1);
                    item.put("name", song.getOrDefault("name", "未知歌曲"));
                    
                    if (song.containsKey("ar") && song.get("ar") instanceof List) {
                        List<?> artists = (List<?>) song.get("ar");
                        StringBuilder artist = new StringBuilder();
                        for (Object art : artists) {
                            Map<?, ?> artistMap = (Map<?, ?>) art;
                            if (artist.length() > 0) artist.append("、");
                            artist.append(artistMap.getOrDefault("name", ""));
                        }
                        item.put("artist", artist.toString());
                    } else {
                        item.put("artist", "未知歌手");
                    }
                    
                    item.put("songId", song.getOrDefault("id", "").toString());
                    results.add(item);
                }
            }
        }
        
        return results;
    }

    @Override
    public String downloadSong(Map<String, Object> song) throws Exception {
        String songId = (String) song.get("songId");
        String songName = (String) song.get("name");
        String artist = (String) song.get("artist");
        
        String url = DOWNLOAD_URL + "?id=" + songId + "&ids=%5B" + songId + "%5D&br=320000";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://music.163.com/song?id=" + songId);
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        if (!data.containsKey("data") || !(data.get("data") instanceof List)) {
            throw new Exception("获取下载链接失败");
        }
        
        List<?> dataList = (List<?>) data.get("data");
        if (dataList.isEmpty()) {
            throw new Exception("获取下载链接失败");
        }
        
        Map<?, ?> songInfo = (Map<?, ?>) dataList.get(0);
        String downloadUrl = (String) songInfo.get("url");
        if (StringUtils.isBlank(downloadUrl)) {
            throw new Exception("下载链接为空");
        }
        
        return downloadFile(downloadUrl, songName, artist, "mp3");
    }
    
    private String downloadFile(String url, String title, String creator, String ext) throws Exception {
        // 清理文件名中的非法字符
        String filename = title + "-" + creator + "." + ext;
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // 缩短过长的文件名
        if (filename.length() > 100) {
            filename = filename.substring(0, 50) + filename.substring(filename.length() - 50);
        }
        
        // 确保下载目录存在
        java.nio.file.Path downloadPath = java.nio.file.Path.of(downloadDir);
        if (!java.nio.file.Files.exists(downloadPath)) {
            java.nio.file.Files.createDirectories(downloadPath);
        }
        
        String filePath = downloadDir + "/" + filename;
        HttpUtils.downloadFile(url, filePath);
        
        return filePath;
    }
}
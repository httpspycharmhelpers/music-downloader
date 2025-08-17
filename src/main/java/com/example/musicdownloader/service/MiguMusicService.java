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
public class MiguMusicService implements MusicService {

    @Value("${download.dir}")
    private String downloadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "https://c.musicapp.migu.cn";
    private static final String SEARCH_URL = BASE_URL + "/v1.0/content/search_all.do";
    private static final String DOWNLOAD_URL = BASE_URL + "/MIGUM3.0/strategy/listen-url/v2.3";

    @Override
    public List<Map<String, Object>> searchSongs(String keyword) throws Exception {
        return search(keyword, false);
    }

    @Override
    public List<Map<String, Object>> searchArtistSongs(String artist, String command) throws Exception {
        return search(artist, true);
    }

    private List<Map<String, Object>> search(String keyword, boolean artistSearch) throws Exception {
        String url = SEARCH_URL + "?text=" + HttpUtils.encodeUrl(keyword) + "&pageNo=1&pageSize=100";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("channel", "0140210");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        List<Map<String, Object>> results = new ArrayList<>();
        if (data.containsKey("songResultData") && data.get("songResultData") instanceof Map) {
            Map<?, ?> songResult = (Map<?, ?>) data.get("songResultData");
            if (songResult.containsKey("result") && songResult.get("result") instanceof List) {
                List<?> songs = (List<?>) songResult.get("result");
                for (int i = 0; i < songs.size(); i++) {
                    Map<?, ?> song = (Map<?, ?>) songs.get(i);
                    
                    // 如果是歌手搜索，检查歌手名称
                    if (artistSearch) {
                        List<?> singers = (List<?>) song.get("singers");
                        boolean artistMatch = false;
                        for (Object singerObj : singers) {
                            Map<?, ?> singer = (Map<?, ?>) singerObj;
                            String singerName = (String) singer.get("name");
                            if (singerName != null && singerName.toLowerCase().contains(keyword.toLowerCase())) {
                                artistMatch = true;
                                break;
                            }
                        }
                        if (!artistMatch) continue;
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", i + 1);
                    item.put("name", song.getOrDefault("name", "未知歌曲"));
                    
                    if (song.containsKey("singers") && song.get("singers") instanceof List) {
                        List<?> singers = (List<?>) song.get("singers");
                        StringBuilder artist = new StringBuilder();
                        for (Object singerObj : singers) {
                            Map<?, ?> singer = (Map<?, ?>) singerObj;
                            if (artist.length() > 0) artist.append("、");
                            artist.append(singer.getOrDefault("name", ""));
                        }
                        item.put("artist", artist.toString());
                    } else {
                        item.put("artist", "未知歌手");
                    }
                    
                    item.put("contentId", song.getOrDefault("contentId", ""));
                    item.put("copyrightId", song.getOrDefault("copyrightId", ""));
                    
                    if (song.containsKey("albums") && song.get("albums") instanceof List) {
                        List<?> albums = (List<?>) song.get("albums");
                        if (!albums.isEmpty()) {
                            Map<?, ?> album = (Map<?, ?>) albums.get(0);
                            item.put("albumId", album.getOrDefault("id", "0"));
                        } else {
                            item.put("albumId", "0");
                        }
                    } else {
                        item.put("albumId", "0");
                    }
                    
                    results.add(item);
                }
            }
        }
        
        return results;
    }

    @Override
    public String downloadSong(Map<String, Object> song) throws Exception {
        String contentId = (String) song.get("contentId");
        String copyrightId = (String) song.get("copyrightId");
        String albumId = (String) song.get("albumId");
        String songName = (String) song.get("name");
        String artist = (String) song.get("artist");
        
        String url = DOWNLOAD_URL + "?copyrightId=" + copyrightId + "&contentId=" + contentId + 
                     "&resourceType=2&albumId=" + albumId + "&netType=01&toneFlag=PQ";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("channel", "0140210");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        if (!data.containsKey("data") || !(data.get("data") instanceof Map)) {
            throw new Exception("获取下载链接失败");
        }
        
        Map<?, ?> dataMap = (Map<?, ?>) data.get("data");
        String downloadUrl = (String) dataMap.get("url");
        
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

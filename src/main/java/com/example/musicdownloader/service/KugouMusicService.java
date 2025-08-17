package com.example.musicdownloader.service;

import com.example.musicdownloader.utils.HttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class KugouMusicService implements MusicService {

    @Value("${download.dir}")
    private String downloadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SEARCH_URL = "https://songsearch.kugou.com/song_search_v2";
    private static final String DOWNLOAD_URL = "https://wwwapi.kugou.com/yy/index.php";

    @Override
    public List<Map<String, Object>> searchSongs(String keyword) throws Exception {
        return search(keyword, false);
    }

    @Override
    public List<Map<String, Object>> searchArtistSongs(String artist, String command) throws Exception {
        return search(artist, true);
    }

    private List<Map<String, Object>> search(String keyword, boolean artistSearch) throws Exception {
        String url = SEARCH_URL + "?keyword=" + HttpUtils.encodeUrl(keyword) + "&page=1&pagesize=100";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://www.kugou.com/");
        headers.put("Cookie", "kg_mid=" + generateRandomString(32));
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        List<Map<String, Object>> results = new ArrayList<>();
        if (data.containsKey("data") && data.get("data") instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) data.get("data");
            if (dataMap.containsKey("lists") && dataMap.get("lists") instanceof List) {
                List<?> songs = (List<?>) dataMap.get("lists");
                for (int i = 0; i < songs.size(); i++) {
                    Map<?, ?> song = (Map<?, ?>) songs.get(i);
                    
                    // 如果是歌手搜索，检查歌手名称
                    if (artistSearch) {
                        String singerName = (String) song.get("SingerName");
                        if (singerName == null || !singerName.toLowerCase().contains(keyword.toLowerCase())) {
                            continue;
                        }
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", i + 1);
                    item.put("name", song.getOrDefault("SongName", "未知歌曲"));
                    item.put("artist", song.getOrDefault("SingerName", "未知歌手"));
                    item.put("hash", song.getOrDefault("FileHash", ""));
                    item.put("albumId", song.getOrDefault("AlbumID", ""));
                    results.add(item);
                }
            }
        }
        
        return results;
    }

    @Override
    public String downloadSong(Map<String, Object> song) throws Exception {
        String hash = (String) song.get("hash");
        String albumId = (String) song.get("albumId");
        String songName = (String) song.get("name");
        String artist = (String) song.get("artist");
        
        String url = DOWNLOAD_URL + "?r=play/getdata&hash=" + hash + "&album_id=" + albumId;
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://www.kugou.com/");
        headers.put("Cookie", "kg_mid=" + generateRandomString(32));
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        if (!data.containsKey("data") || !(data.get("data") instanceof Map)) {
            throw new Exception("获取下载链接失败");
        }
        
        Map<?, ?> dataMap = (Map<?, ?>) data.get("data");
        String downloadUrl = (String) dataMap.get("play_url");
        
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
    
    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}
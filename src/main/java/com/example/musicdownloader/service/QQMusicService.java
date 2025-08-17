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
public class QQMusicService implements MusicService {

    @Value("${download.dir}")
    private String downloadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SEARCH_URL = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp";
    private static final String DOWNLOAD_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg";

    @Override
    public List<Map<String, Object>> searchSongs(String keyword) throws Exception {
        return search(keyword, false);
    }

    @Override
    public List<Map<String, Object>> searchArtistSongs(String artist, String command) throws Exception {
        return search(artist, true);
    }

    private List<Map<String, Object>> search(String keyword, boolean artistSearch) throws Exception {
        String url = SEARCH_URL + "?p=1&n=100&w=" + HttpUtils.encodeUrl(keyword) + "&format=json";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://y.qq.com/");
        
        String response = HttpUtils.get(url, headers);
        
        // 处理JSONP响应
        if (response.startsWith("callback(") && response.endsWith(")")) {
            response = response.substring(9, response.length() - 1);
        }
        
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        List<Map<String, Object>> results = new ArrayList<>();
        if (data.containsKey("data") && data.get("data") instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) data.get("data");
            if (dataMap.containsKey("song") && dataMap.get("song") instanceof Map) {
                Map<?, ?> songMap = (Map<?, ?>) dataMap.get("song");
                if (songMap.containsKey("list") && songMap.get("list") instanceof List) {
                    List<?> songs = (List<?>) songMap.get("list");
                    for (int i = 0; i < songs.size(); i++) {
                        Map<?, ?> song = (Map<?, ?>) songs.get(i);
                        
                        // 如果是歌手搜索，检查歌手名称
                        if (artistSearch) {
                            List<?> singers = (List<?>) song.get("singer");
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
                        item.put("name", song.getOrDefault("songname", "未知歌曲"));
                        
                        if (song.containsKey("singer") && song.get("singer") instanceof List) {
                            List<?> singers = (List<?>) song.get("singer");
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
                        
                        item.put("songmid", song.getOrDefault("songmid", ""));
                        results.add(item);
                    }
                }
            }
        }
        
        return results;
    }

    @Override
    public String downloadSong(Map<String, Object> song) throws Exception {
        String songmid = (String) song.get("songmid");
        String songName = (String) song.get("name");
        String artist = (String) song.get("artist");
        
        String url = DOWNLOAD_URL + "?format=json&data=%7B%22req_0%22%3A%7B%22module%22%3A%22vkey.GetVkeyServer%22%2C%22method%22%3A%22CgiGetVkey%22%2C%22param%22%3A%7B%22guid%22%3A%22358840384%22%2C%22songmid%22%3A%5B%22" + 
                     songmid + "%22%5D%2C%22songtype%22%3A%5B0%5D%2C%22uin%22%3A%221443481947%22%2C%22loginflag%22%3A1%2C%22platform%22%3A%2220%22%7D%7D%2C%22comm%22%3A%7B%22uin%22%3A%2218585073516%22%2C%22format%22%3A%22json%22%2C%22ct%22%3A24%2C%22cv%22%3A0%7D%7D";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://y.qq.com/");
        
        String response = HttpUtils.get(url, headers);
        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        
        if (!data.containsKey("req_0") || !(data.get("req_0") instanceof Map)) {
            throw new Exception("获取下载链接失败");
        }
        
        Map<?, ?> req0 = (Map<?, ?>) data.get("req_0");
        if (!req0.containsKey("data") || !(req0.get("data") instanceof Map)) {
            throw new Exception("获取下载链接失败");
        }
        
        Map<?, ?> reqData = (Map<?, ?>) req0.get("data");
        if (!reqData.containsKey("midurlinfo") || !(reqData.get("midurlinfo") instanceof List)) {
            throw new Exception("获取下载链接失败");
        }
        
        List<?> midurlinfo = (List<?>) reqData.get("midurlinfo");
        if (midurlinfo.isEmpty()) {
            throw new Exception("获取下载链接失败");
        }
        
        Map<?, ?> urlInfo = (Map<?, ?>) midurlinfo.get(0);
        String purl = (String) urlInfo.get("purl");
        if (StringUtils.isBlank(purl)) {
            throw new Exception("下载链接为空");
        }
        
        String downloadUrl = "https://dl.stream.qqmusic.qq.com/" + purl;
        return downloadFile(downloadUrl, songName, artist, "m4a");
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
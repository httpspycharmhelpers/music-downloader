package com.example.musicdownloader.controller;

import com.example.musicdownloader.service.MusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MusicController {

    @Autowired
    private MusicService musicService;

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> request) {
        String keyword = request.get("keyword");
        String platform = request.get("platform");
        String command = request.get("command");
        
        try {
            List<Map<String, Object>> results;
            
            if (command != null && command.startsWith("/Singer")) {
                // 处理歌手搜索
                results = musicService.searchArtistSongs(keyword, platform, command);
            } else {
                // 普通搜索
                results = musicService.searchSongs(keyword, platform);
            }
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "results", results,
                "platform", platform
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/download")
    public ResponseEntity<?> download(@RequestBody Map<String, Object> request) {
        String platform = (String) request.get("platform");
        Map<String, Object> song = (Map<String, Object>) request.get("song");
        
        try {
            String filePath = musicService.downloadSong(song, platform);
            String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "filePath", filePath,
                "filename", filename
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
              }

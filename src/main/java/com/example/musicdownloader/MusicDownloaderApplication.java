package com.example.musicdownloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class MusicDownloaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicDownloaderApplication.class, args);
        System.out.println("音乐搜索与下载工具已启动！访问 http://localhost:8080");
        System.out.println("信念：好用、耐用、能用！");
    }
    }

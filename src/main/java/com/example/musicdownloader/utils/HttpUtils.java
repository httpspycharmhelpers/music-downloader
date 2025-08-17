package com.example.musicdownloader.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {

    private static final int TIMEOUT = 15000; // 15秒超时

    public static String get(String urlString, Map<String, String> headers) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        
        // 设置请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            return response.toString();
        } else {
            throw new IOException("HTTP请求失败，状态码: " + responseCode);
        }
    }

    public static void downloadFile(String fileUrl, String savePath) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT * 2); // 下载超时时间加倍
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setRequestProperty("Referer", "https://www.google.com/");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "keep-alive");
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream fos = new FileOutputStream(savePath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }
    }

    public static String encodeUrl(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
          }

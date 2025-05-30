package com.xwrl.mvvm.demo.custom;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiguMusicScraper {
    private static final String BASE_URL = "https://music.migu.cn/v3";
    private static final String API_URL = "https://api.leafone.cn/api/mgmusic?id={}";
    private static final String HOT_PATTERN = "\"rankData\">(.*?)</";
    private static final String HOT_SONG_PATTERN = "\"songName\":\"(.*?)\",.*?\"copyrightId\":\"(.*?)\",.*?\"image\":\"(.*?)\",.*?\"singers\":\\[\\{\"singerId\":\"\\d+\",\"singerName\":\"(.*?)\"";
    private static final String RECOMMEND_PATTERN = "\"songData\">(.*?)</";
    private static final String RECOMMEND_SONG_PATTERN = "\"songName\":\"(.*?)\",.*?\"copyrightId\":\"(.*?)\",.*?\"image\":\"(.*?)\",.*?\"singers\":\\[\\{\"singerId\":\"\\d+\",\"singerName\":\"(.*?)\"";
    private OkHttpClient client;

    public MiguMusicScraper() {
        client = new OkHttpClient();
    }

    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public List<String[]> getNewSongs() {
        List<String[]> songList = new ArrayList<>();
        try {
            String html = fetchData(BASE_URL);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("a[href^=\"https://music.migu.cn/v3/music/song/\"][data-title*=\"【首发】\"]");

            for (Element element : elements) {
                String songId = element.attr("href").split("/")[5];
                String title = element.attr("data-title").replace("【首发】", "").trim();
                String imageUrl = element.selectFirst("img[src]").attr("src");

                String songName = "";
                String singerName = "";
                if (title.contains("\t")) {
                    String[] parts = title.split("\t");
                    songName = parts[0].trim();
                    singerName = parts[1].trim();
                } else if (title.contains("》")) {
                    String[] parts = title.split("》");
                    songName = parts[0].trim() + "》";
                    singerName = parts[1].trim();
                } else {
                    String[] parts = title.split(" ");
                    songName = parts[0].trim();
                    singerName = parts[1].trim();
                }

                songName = songName.replace("&amp;", " ");

                imageUrl = "https:" + imageUrl;
                songList.add(new String[]{songName, songId, imageUrl, singerName});
            }
            System.out.println("New Songs: " + songList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songList;
    }

    public List<String[]> getHotSongs(int limit) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL)
                .build();

        Response response = client.newCall(request).execute();
        String responseData = response.body().string();

        Pattern pattern = Pattern.compile(HOT_PATTERN, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseData);
        String hotSongsData = matcher.find() ? matcher.group(1) : "";

        List<String[]> songsList = new ArrayList<>();
        Pattern songPattern = Pattern.compile(HOT_SONG_PATTERN, Pattern.DOTALL);
        Matcher songMatcher = songPattern.matcher(hotSongsData);

        while (songMatcher.find() && songsList.size() < limit) {
            String[] songInfo = {
                    songMatcher.group(1),
                    songMatcher.group(2),
                    "https:" + songMatcher.group(3),
                    songMatcher.group(4)
            };
            songsList.add(songInfo);
        }

        return songsList;
    }

    public List<String[]> getRecommendSongs(int limit) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL)
                .build();

        Response response = client.newCall(request).execute();
        String responseData = response.body().string();

        Pattern pattern = Pattern.compile(RECOMMEND_PATTERN, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseData);
        String recommendSongsData = matcher.find() ? matcher.group(1) : "";

        List<String[]> songsList = new ArrayList<>();
        Pattern songPattern = Pattern.compile(RECOMMEND_SONG_PATTERN, Pattern.DOTALL);
        Matcher songMatcher = songPattern.matcher(recommendSongsData);

        while (songMatcher.find() && songsList.size() < limit) {
            String[] songInfo = {
                    songMatcher.group(1),
                    songMatcher.group(2),
                    "https:" + songMatcher.group(3),
                    songMatcher.group(4)
            };
            songsList.add(songInfo);
        }

        return songsList;
    }

    public String getMiguMusic(String id) {
        try {
            String url = API_URL.replace("{}", id);
            return fetchData(url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
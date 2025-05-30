package com.xwrl.mvvm.demo.custom;

import android.os.AsyncTask;

import com.xwrl.mvvm.demo.model.CrawerActivity;
import com.xwrl.mvvm.demo.model.CrawlerSong;

import java.util.ArrayList;
import java.util.List;

public class FetchNewSongsTask extends AsyncTask<Void, Void, List<String[]>> {
    private CrawerActivity crawerActivity;
    private Exception exception;

    public FetchNewSongsTask(CrawerActivity mainActivity) {
        this.crawerActivity = mainActivity;
    }

    @Override
    protected List<String[]> doInBackground(Void... voids) {
        List<String[]> newSongs = new ArrayList<>();
        MiguMusicScraper miguMusicScraper = new MiguMusicScraper();

        try{
            newSongs.addAll(miguMusicScraper.getNewSongs());
            newSongs.addAll(miguMusicScraper.getHotSongs(10));
            newSongs.addAll(miguMusicScraper.getRecommendSongs(10));
        } catch (Exception e){
            exception = e;
        }
        return newSongs;
    }

    @Override
    protected void onPostExecute(List<String[]> newSongs) {
        if (exception != null) {
            exception.printStackTrace();
            crawerActivity.showErrorToast("Failed to fetch new songs");
        } else if (newSongs != null && !newSongs.isEmpty()) {
            List<CrawlerSong> updatedSongsList = new ArrayList<>();
            for (String[] songData : newSongs) {
                updatedSongsList.add(new CrawlerSong(songData[0], songData[3], songData[2], songData[1]));
            }
            crawerActivity.updateSongs(updatedSongsList);
        } else {
            crawerActivity.showErrorToast("No songs found");
        }
    }
}
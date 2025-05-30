package com.xwrl.mvvm.demo.model;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.adapter.SongAdapter;
import com.xwrl.mvvm.demo.custom.FetchNewSongsTask;

import java.util.ArrayList;
import java.util.List;

public class CrawerActivity extends AppCompatActivity {
    private SongAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<CrawlerSong> songsList = new ArrayList<>();

        RecyclerView recyclerView = findViewById(R.id.songsRecyclerView);
        adapter = new SongAdapter(songsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        new FetchNewSongsTask(this).execute();
    }

    public void updateSongs(List<CrawlerSong> newSongs) {
        adapter.updateSongs(newSongs);
        adapter.notifyDataSetChanged();
    }

    public void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

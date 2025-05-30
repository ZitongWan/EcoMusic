package com.xwrl.mvvm.demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.model.CrawlerSong;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private static List<CrawlerSong> songs;

    public SongAdapter(List<CrawlerSong> songs) {
        this.songs = songs;
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {
        CrawlerSong song = songs.get(position);
        holder.indexTextView.setText(String.valueOf(position + 1));
        holder.titleTextView.setText(song.getTitle());
        holder.artistTextView.setText(song.getArtist());
//        holder.linkTextView.setText(song.getLink());
//        holder.idTextView.setText(song.getId());
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        Button titleTextView;
        TextView artistTextView;
        TextView linkTextView;
        TextView idTextView;
        TextView indexTextView;

        public SongViewHolder(View itemView) {
            super(itemView);
            indexTextView = itemView.findViewById(R.id.songIndex);
            titleTextView = itemView.findViewById(R.id.songTitle);
            artistTextView = itemView.findViewById(R.id.songArtist);
            linkTextView = itemView.findViewById(R.id.songLink);
            idTextView = itemView.findViewById(R.id.songId);

            titleTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        CrawlerSong song = songs.get(position);
                        String message = "Downloading : " + song.getTitle() + " - " + song.getArtist();
                        Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public void updateSongs(List<CrawlerSong> newSongs) {
        songs.clear();
        songs.addAll(newSongs);
        notifyDataSetChanged();
    }
}
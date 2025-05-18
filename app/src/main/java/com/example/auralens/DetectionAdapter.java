package com.example.auralens;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DetectionAdapter extends RecyclerView.Adapter<DetectionAdapter.ViewHolder> {

    private final List<DetectionEntry> detectionEntries;

    public DetectionAdapter(List<DetectionEntry> detectionEntries) {
        this.detectionEntries = detectionEntries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.detection_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetectionEntry entry = detectionEntries.get(position);
        holder.labelText.setText(entry.getLabel());
        holder.timeText.setText(entry.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return detectionEntries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView labelText;
        public TextView timeText;

        public ViewHolder(View view) {
            super(view);
            labelText = view.findViewById(R.id.text_label);
            timeText = view.findViewById(R.id.text_time);
        }
    }
}
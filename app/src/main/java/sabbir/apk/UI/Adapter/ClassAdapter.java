package sabbir.apk.UI.Adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import sabbir.apk.InterNet.Deta.ScheduleItem;
import sabbir.apk.R;


public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private final List<ScheduleItem> items;
    private final int layoutId;

    public ClassAdapter(List<ScheduleItem> items, int layoutId) {
        this.items = items;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem item = items.get(position);

        holder.subject.setText(item.subject);
        String timeLabel;
        if (item.start != null && item.end != null) {
            timeLabel = item.start + " – " + item.end;
        } else {
            timeLabel = item.period == null ? "Scheduled" : item.period;
        }

        if (item.room != null && !item.room.trim().isEmpty()
                && !item.room.equals("—")) {
            timeLabel = timeLabel + " • Room " + item.room;
        }
        holder.time.setText(timeLabel);

        if (holder.instructor != null) {
            String instructor = item.instructor == null || item.instructor.trim().isEmpty()
                    ? "TBA"
                    : item.instructor;
            holder.instructor.setText(instructor);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView subject, time, instructor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.tv_subject);
            time = itemView.findViewById(R.id.tv_time);
            instructor = itemView.findViewById(R.id.tv_instructor);
        }
    }
}

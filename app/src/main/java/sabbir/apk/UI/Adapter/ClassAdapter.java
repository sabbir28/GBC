package sabbir.apk.UI.Adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.List;

import sabbir.apk.InterNet.Deta.ScheduleItem;
import sabbir.apk.R;


public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private final List<ScheduleItem> items;
    private final int layoutId;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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
        holder.time.setText(formatTimeRange(item));

        if (holder.instructor != null) {
            holder.instructor.setText(item.instructor);
        }
    }

    private String formatTimeRange(ScheduleItem item) {
        return item.start.format(timeFormatter) + " – " + item.end.format(timeFormatter);
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

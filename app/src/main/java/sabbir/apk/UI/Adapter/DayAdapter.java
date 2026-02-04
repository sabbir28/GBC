package sabbir.apk.UI.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import sabbir.apk.R;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.ViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(String day);
    }

    private final List<String> days;
    private final OnDayClickListener listener;
    private String selectedDay;

    public DayAdapter(List<String> days, String selectedDay, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
        this.selectedDay = selectedDay;
    }

    public void setSelectedDay(String selectedDay) {
        this.selectedDay = selectedDay;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String day = days.get(position);
        holder.dayText.setText(day);

        boolean isSelected = day.equals(selectedDay);
        holder.card.setCardBackgroundColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isSelected ? R.color.colorPrimary : R.color.colorSurface
        ));
        holder.dayText.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isSelected ? R.color.colorOnPrimary : R.color.colorOnSurface
        ));

        holder.itemView.setOnClickListener(v -> listener.onDayClick(day));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView dayText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_day);
            dayText = itemView.findViewById(R.id.tv_day);
        }
    }
}

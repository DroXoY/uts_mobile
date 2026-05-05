package com.example.uts;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onStatusChanged(int taskId, boolean isDone);
        void onDeleteClicked(int taskId);
        void onEditClicked(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvCourse.setText(task.getCourse());
        holder.tvPriority.setText(task.getPriority());
        holder.tvDate.setText(task.getDeadline());

        // Hapus listener sementara saat set nilai awal
        holder.cbDone.setOnCheckedChangeListener(null);

        // Logika Coret (Strikethrough)
        if (task.getStatus() == 1) {
            holder.cbDone.setChecked(true);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.GRAY);
        } else {
            holder.cbDone.setChecked(false);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(Color.parseColor("#212121")); // Warna teks default
        }
        // Ganti warna background dan teks berdasarkan level prioritas
        if (task.getPriority().equalsIgnoreCase("TINGGI")) {
            holder.tvPriority.setTextColor(Color.parseColor("#C62828")); // Teks Merah
            holder.tvPriority.setBackgroundColor(Color.parseColor("#FFEBEE")); // BG Merah Muda
        } else if (task.getPriority().equalsIgnoreCase("SEDANG")) {
            holder.tvPriority.setTextColor(Color.parseColor("#F57F17")); // Teks Oranye
            holder.tvPriority.setBackgroundColor(Color.parseColor("#FFFDE7")); // BG Kuning
        } else {
            holder.tvPriority.setTextColor(Color.parseColor("#2E7D32")); // Teks Hijau
            holder.tvPriority.setBackgroundColor(Color.parseColor("#E8F5E9")); // BG Hijau Muda
        }

        // Listener Klik
        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onStatusChanged(task.getId(), isChecked));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(task.getId()));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClicked(task));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCourse, tvPriority, tvDate;
        CheckBox cbDone;
        ImageView btnEdit, btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvCourse = itemView.findViewById(R.id.tv_course);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvDate = itemView.findViewById(R.id.tv_deadline);
            cbDone = itemView.findViewById(R.id.cb_task_status);
            btnEdit = itemView.findViewById(R.id.iv_edit);
            btnDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}
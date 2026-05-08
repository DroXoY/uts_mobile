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

import com.google.android.material.card.MaterialCardView;

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

        // --- LOGIKA WARNA OVERDUE (REVISI) ---
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy HH:mm", java.util.Locale.getDefault());
        java.util.Date now = new java.util.Date();
        boolean isOverdue = false;

        try {
            java.util.Date deadlineDate = sdf.parse(task.getDeadline());
            if (deadlineDate != null && deadlineDate.before(now) && task.getStatus() == 0) {
                isOverdue = true;
            }
        } catch (Exception e) {
            isOverdue = false;
        }

        // Atur Warna Kartu berdasarkan status Overdue
        if (isOverdue) {
            // Jika TERLAMBAT: Background Pink Kemerahan, Border Merah
            holder.cardTask.setCardBackgroundColor(Color.parseColor("#FFF1F0")); // Pink sangat muda
            holder.cardTask.setStrokeColor(Color.parseColor("#FFA39E")); // Border merah muda
            holder.tvTitle.setTextColor(Color.parseColor("#CF1322")); // Teks judul jadi merah gelap
        } else {
            // Jika NORMAL: Background Putih, Border Abu-abu (seperti desain awalmu)
            holder.cardTask.setCardBackgroundColor(Color.WHITE);
            holder.cardTask.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.tvTitle.setTextColor(Color.parseColor("#212121"));
        }
        // ---------------------------------------

        // Logika Coret (Strikethrough) tetap ada
        holder.cbDone.setOnCheckedChangeListener(null);
        if (task.getStatus() == 1) {
            holder.cbDone.setChecked(true);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.GRAY);
            // Jika sudah selesai, kembalikan warna kartu ke netral
            holder.cardTask.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.cardTask.setStrokeColor(Color.parseColor("#E0E0E0"));
        } else if (!isOverdue) { // Jika belum selesai dan TIDAK overdue
            holder.cbDone.setChecked(false);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

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
        MaterialCardView cardTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvCourse = itemView.findViewById(R.id.tv_course);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvDate = itemView.findViewById(R.id.tv_deadline);
            cbDone = itemView.findViewById(R.id.cb_task_status);
            btnEdit = itemView.findViewById(R.id.iv_edit);
            btnDelete = itemView.findViewById(R.id.iv_delete);
            cardTask = itemView.findViewById(R.id.card_task);
        }
    }
}
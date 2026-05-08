package com.example.uts;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Navigasi & Layout
    private MaterialButton btnNavDashboard, btnNavDaftar, btnNavKalender;
    private ConstraintLayout layoutDashboard, layoutDaftar, layoutKalender;

    // Dashboard Komponen
    private TextView tvSelesai, tvTerlambat, tvProgressText;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private RecyclerView rvTugasUtama;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private android.widget.CalendarView calendarView;
    private RecyclerView rvTugasKalender;
    private TaskAdapter adapterKalender;
    private TextView tvLabelKalender;

    private String currentSort = "Deadline"; // Default urutkan dari deadline
    private String currentCalendarDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // 1. Inisialisasi Layout & Tombol Navigasi
        layoutDashboard = findViewById(R.id.layout_halaman_dashboard);
        layoutDaftar = findViewById(R.id.layout_halaman_daftar);
        layoutKalender = findViewById(R.id.layout_halaman_kalender);
        btnNavDashboard = findViewById(R.id.btn_nav_dashboard);
        btnNavDaftar = findViewById(R.id.btn_nav_daftar);
        btnNavKalender = findViewById(R.id.btn_nav_kalender);

        // 2. Inisialisasi Komponen Dashboard
        tvSelesai = findViewById(R.id.tv_jumlah_selesai);
        tvTerlambat = findViewById(R.id.tv_jumlah_terlambat);
        tvProgressText = findViewById(R.id.tv_progress_text);
        progressBar = findViewById(R.id.progress_bar);

        // 3. Inisialisasi Daftar Tugas (Utama)
        rvTugasUtama = findViewById(R.id.rv_tugas_utama);
        rvTugasUtama.setLayoutManager(new LinearLayoutManager(this));

        // 4. Inisialisasi Kalender
        calendarView = findViewById(R.id.calendar_view);
        rvTugasKalender = findViewById(R.id.rv_tugas_kalender);
        rvTugasKalender.setLayoutManager(new LinearLayoutManager(this));
        tvLabelKalender = findViewById(R.id.tv_label_kalender);

        // Panggil setup
        setupNavigation();
        loadData();
        startRealtimeCheck(); // Realtime checker untuk overdue alert

        // Pasang sensor klik pada Kalender
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            currentCalendarDate = selectedDate;
            tvLabelKalender.setText("Tugas pada: " + selectedDate);
            loadDataKalender(selectedDate);
        });

        // Setup Spinner Sorting
        Spinner spinnerSort = findViewById(R.id.spinner_sort);
        String[] opsiSort = {"Deadline", "Nama Tugas", "Prioritas"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, opsiSort);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(spinnerAdapter);
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSort = opsiSort[position];
                loadData();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Tombol Tambah & About Us
        Button btnTambah = findViewById(R.id.btn_tambah);
        btnTambah.setOnClickListener(v -> showAddTaskDialog());
        android.widget.ImageView btnTentangKami = findViewById(R.id.btnTentangKami);
        btnTentangKami.setOnClickListener(v -> showAboutUsDialog());
        btnNavDashboard.performClick();
    }

    // ==========================================
    // LOGIKA DASHBOARD
    // ==========================================
    private void updateDashboardStats() {
        int totalSelesai = 0;
        int totalTerlambat = 0;
        int totalSemua = 0;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy HH:mm", java.util.Locale.getDefault());
        java.util.Date now = new java.util.Date(); // Waktu saat ini

        // Ambil langsung dari SQLite agar paling akurat
        Cursor cursor = dbHelper.getAllTasks();
        totalSemua = cursor.getCount();

        if (cursor.moveToFirst()) {
            do {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS));
                String deadline = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DEADLINE));

                if (status == 1) {
                    totalSelesai++; // Tugas selesai
                } else {
                    // Tugas belum selesai, cek apakah terlambat
                    try {
                        java.util.Date deadlineDate = sdf.parse(deadline);
                        if (deadlineDate != null && deadlineDate.before(now)) {
                            totalTerlambat++; // Terlambat
                        }
                    } catch (Exception e) {
                        // Abaikan jika format gagal
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Update teks di kartu
        tvSelesai.setText(String.valueOf(totalSelesai));
        tvTerlambat.setText(String.valueOf(totalTerlambat));

        // Update Progress Bar
        if (totalSemua > 0) {
            int persentase = (totalSelesai * 100) / totalSemua;
            progressBar.setProgress(persentase);
            tvProgressText.setText(totalSelesai + " dari " + totalSemua + " tugas selesai (" + persentase + "%)");
        } else {
            progressBar.setProgress(0);
            tvProgressText.setText("Belum ada tugas yang ditambahkan");
        }
    }

    // ==========================================
    // LOGIKA NAVIGASI (3 HALAMAN)
    // ==========================================
    private void setupNavigation() {
        btnNavDashboard.setOnClickListener(v -> {
            layoutDashboard.setVisibility(View.VISIBLE);
            layoutDaftar.setVisibility(View.GONE);
            layoutKalender.setVisibility(View.GONE);

            btnNavDashboard.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnNavDashboard.setTextColor(Color.parseColor("#3D5AFE"));
            btnNavDashboard.setIconTintResource(R.color.colorPrimary);

            resetNavButton(btnNavDaftar);
            resetNavButton(btnNavKalender);
        });

        btnNavDaftar.setOnClickListener(v -> {
            layoutDashboard.setVisibility(View.GONE);
            layoutDaftar.setVisibility(View.VISIBLE);
            layoutKalender.setVisibility(View.GONE);

            btnNavDaftar.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnNavDaftar.setTextColor(Color.parseColor("#3D5AFE"));
            btnNavDaftar.setIconTintResource(R.color.colorPrimary);

            resetNavButton(btnNavDashboard);
            resetNavButton(btnNavKalender);
        });

        btnNavKalender.setOnClickListener(v -> {
            layoutDashboard.setVisibility(View.GONE);
            layoutDaftar.setVisibility(View.GONE);
            layoutKalender.setVisibility(View.VISIBLE);

            btnNavKalender.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnNavKalender.setTextColor(Color.parseColor("#3D5AFE"));
            btnNavKalender.setIconTintResource(R.color.colorPrimary);

            resetNavButton(btnNavDashboard);
            resetNavButton(btnNavDaftar);

            Calendar cal = Calendar.getInstance();
            String todayDate = cal.get(Calendar.DAY_OF_MONTH) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
            currentCalendarDate = todayDate;
            tvLabelKalender.setText("Tugas pada: " + todayDate);
            loadDataKalender(todayDate);
        });
    }

    private void resetNavButton(MaterialButton btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5C7CFF")));
        btn.setTextColor(Color.WHITE);
        btn.setIconTintResource(R.color.white);
    }

    // ==========================================
    // ABOUT US
    // ==========================================
    private void showAboutUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("👥 About Us")
                .setMessage("Aplikasi ini dibuat oleh:\n\n1. Farrel Endra A.\n   NIM: 24410100016\n\n2. Michael Christoper\n   NIM: 24410100007\n\n3. Jonathan Kevin\n   NIM: 24410100027")
                .setPositiveButton("OK", null)
                .show();
    }

    // ==========================================
    // LOGIKA LOAD DATA
    // ==========================================
    private void loadData() {
        taskList = new ArrayList<>();
        Cursor cursor = dbHelper.getAllTasks();

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COURSE));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIORITY));
                String deadline = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DEADLINE));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS));
                taskList.add(new Task(id, title, course, priority, deadline, status));
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Logika Sorting
        if (currentSort.equals("Nama Tugas")) {
            java.util.Collections.sort(taskList, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
        } else if (currentSort.equals("Prioritas")) {
            java.util.Collections.sort(taskList, (t1, t2) -> {
                int val1 = t1.getPriority().equalsIgnoreCase("TINGGI") ? 3 : (t1.getPriority().equalsIgnoreCase("SEDANG") ? 2 : 1);
                int val2 = t2.getPriority().equalsIgnoreCase("TINGGI") ? 3 : (t2.getPriority().equalsIgnoreCase("SEDANG") ? 2 : 1);
                return Integer.compare(val2, val1);
            });
        } else if (currentSort.equals("Deadline")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy HH:mm", java.util.Locale.getDefault());
            java.util.Collections.sort(taskList, (t1, t2) -> {
                try {
                    return sdf.parse(t1.getDeadline()).compareTo(sdf.parse(t2.getDeadline()));
                } catch (Exception e) {
                    return 0;
                }
            });
        }

        adapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onStatusChanged(int taskId, boolean isDone) {
                dbHelper.updateTaskStatus(taskId, isDone ? 1 : 0);

                // BARU: Reset ingatan alert karena status tugas berubah!
                lastAlertContent = "";

                loadData();
            }
            @Override
            public void onDeleteClicked(int taskId) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus Tugas")
                        .setMessage("Yakin ingin menghapus tugas ini?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbHelper.deleteTask(taskId);

                            // BARU: Reset ingatan alert karena jumlah tugas berkurang!
                            lastAlertContent = "";

                            loadData();
                            Toast.makeText(MainActivity.this, "Tugas Dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
            @Override
            public void onEditClicked(Task task) {
                showEditTaskDialog(task);
            }
        });
        rvTugasUtama.setAdapter(adapter);

        // SELALU UPDATE DASHBOARD SETELAH LOAD DATA
        updateDashboardStats();
    }

    private void loadDataKalender(String date) {
        List<Task> taskListKalender = new java.util.ArrayList<>();
        Cursor cursor = dbHelper.getTasksByDate(date);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COURSE));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIORITY));
                String deadline = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DEADLINE));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS));
                taskListKalender.add(new Task(id, title, course, priority, deadline, status));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapterKalender = new TaskAdapter(taskListKalender, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onStatusChanged(int taskId, boolean isDone) {
                dbHelper.updateTaskStatus(taskId, isDone ? 1 : 0);
                loadDataKalender(date);
                loadData();
            }
            @Override
            public void onDeleteClicked(int taskId) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus Tugas")
                        .setMessage("Yakin ingin menghapus tugas ini?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbHelper.deleteTask(taskId);
                            loadDataKalender(date);
                            loadData();
                            Toast.makeText(MainActivity.this, "Tugas Dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
            @Override
            public void onEditClicked(Task task) {
                showEditTaskDialog(task);
            }
        });
        rvTugasKalender.setAdapter(adapterKalender);
    }

    // ==========================================
    // POP-UP TAMBAH & EDIT TUGAS
    // ==========================================
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextInputEditText etName = dialogView.findViewById(R.id.et_task_name);
        TextInputEditText etCourse = dialogView.findViewById(R.id.et_course_name);
        AutoCompleteTextView actvPriority = dialogView.findViewById(R.id.actv_priority);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btn_pick_date);
        MaterialButton btnPickTime = dialogView.findViewById(R.id.btn_pick_time);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_task);

        String[] priorities = {"TINGGI", "SEDANG", "RENDAH"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        actvPriority.setAdapter(priorityAdapter);

        btnPickDate.setOnClickListener(v -> showDatePicker(btnPickDate));
        btnPickTime.setOnClickListener(v -> showTimePicker(btnPickDate, btnPickTime));

        btnSave.setOnClickListener(v -> {
            String title = etName.getText().toString();
            String course = etCourse.getText().toString();
            String priority = actvPriority.getText().toString();
            String date = btnPickDate.getText().toString();
            String time = btnPickTime.getText().toString();

            if (title.isEmpty() || course.isEmpty() || priority.isEmpty() || date.equals("Pilih Tanggal Deadline") || time.equals("Pilih Jam")) {
                Toast.makeText(this, "Semua kolom wajib diisi dengan benar!", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullDeadline = date + " " + time;
            long result = dbHelper.insertTask(title, course, priority, fullDeadline);

            if (result != -1) {
                Toast.makeText(this, "Tugas berhasil disimpan!", Toast.LENGTH_SHORT).show();
                loadData();
                if (layoutKalender.getVisibility() == View.VISIBLE && currentCalendarDate != null && !currentCalendarDate.isEmpty()) {
                    loadDataKalender(currentCalendarDate);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void showEditTaskDialog(Task taskToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextInputEditText etName = dialogView.findViewById(R.id.et_task_name);
        TextInputEditText etCourse = dialogView.findViewById(R.id.et_course_name);
        AutoCompleteTextView actvPriority = dialogView.findViewById(R.id.actv_priority);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btn_pick_date);
        MaterialButton btnPickTime = dialogView.findViewById(R.id.btn_pick_time);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_task);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText("Edit Tugas");
        btnSave.setText("Update Tugas");

        etName.setText(taskToEdit.getTitle());
        etCourse.setText(taskToEdit.getCourse());

        String oldDeadline = taskToEdit.getDeadline();
        if (oldDeadline != null && oldDeadline.contains(" ")) {
            String[] parts = oldDeadline.split(" ");
            btnPickDate.setText(parts[0]);
            if (parts.length > 1) {
                btnPickTime.setText(parts[1]);
            }
        }

        String[] priorities = {"TINGGI", "SEDANG", "RENDAH"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        actvPriority.setAdapter(priorityAdapter);
        actvPriority.setText(taskToEdit.getPriority(), false);

        btnPickDate.setOnClickListener(v -> showDatePicker(btnPickDate));
        btnPickTime.setOnClickListener(v -> showTimePicker(btnPickDate, btnPickTime));

        btnSave.setOnClickListener(v -> {
            String newTitle = etName.getText().toString();
            String newCourse = etCourse.getText().toString();
            String newPriority = actvPriority.getText().toString();
            String date = btnPickDate.getText().toString();
            String time = btnPickTime.getText().toString();

            if (newTitle.isEmpty() || date.equals("Pilih Tanggal Deadline") || time.equals("Pilih Jam")) {
                Toast.makeText(this, "Nama, Tanggal, dan Jam wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullDeadline = date + " " + time;
            dbHelper.updateTaskData(taskToEdit.getId(), newTitle, newCourse, newPriority, fullDeadline);
            Toast.makeText(this, "Tugas berhasil diupdate!", Toast.LENGTH_SHORT).show();

            loadData();
            if (layoutKalender.getVisibility() == View.VISIBLE && currentCalendarDate != null && !currentCalendarDate.isEmpty()) {
                loadDataKalender(currentCalendarDate);
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    // ==========================================
    // WAKTU (DATE & TIME PICKER)
    // ==========================================
    private void showDatePicker(MaterialButton btnPickDate) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            btnPickDate.setText(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        Calendar minDate = Calendar.getInstance();
        minDate.set(Calendar.HOUR_OF_DAY, 0);
        minDate.set(Calendar.MINUTE, 0);
        minDate.set(Calendar.SECOND, 0);
        minDate.set(Calendar.MILLISECOND, 0);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private void showTimePicker(MaterialButton btnPickDate, MaterialButton btnPickTime) {
        Calendar now = Calendar.getInstance();
        String selectedDateStr = btnPickDate.getText().toString();
        String todayStr = now.get(Calendar.DAY_OF_MONTH) + "/" + (now.get(Calendar.MONTH) + 1) + "/" + now.get(Calendar.YEAR);
        final boolean isToday = selectedDateStr.equals(todayStr);

        int defaultHour = isToday ? now.get(Calendar.HOUR_OF_DAY) : 0;
        int defaultMinute = isToday ? now.get(Calendar.MINUTE) : 0;

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            if (isToday) {
                int nowHour = now.get(Calendar.HOUR_OF_DAY);
                int nowMinute = now.get(Calendar.MINUTE);
                boolean jamSudahLewat = (hourOfDay < nowHour) || (hourOfDay == nowHour && minuteOfHour <= nowMinute);

                if (jamSudahLewat) {
                    Toast.makeText(this, "Jam tidak boleh sebelum waktu sekarang!", Toast.LENGTH_SHORT).show();
                    showTimePicker(btnPickDate, btnPickTime);
                    return;
                }
            }
            String time = String.format("%02d:%02d", hourOfDay, minuteOfHour);
            btnPickTime.setText(time);
        }, defaultHour, defaultMinute, true);
        timePickerDialog.show();
    }

    // ==========================================
    // LOGIKA PERINGATAN (OVERDUE)
    // ==========================================
    private void checkOverdueTasks(List<Task> tasks) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy HH:mm", java.util.Locale.getDefault());
        java.util.Date now = new java.util.Date();

        StringBuilder overdueTasks = new StringBuilder();

        for (Task task : tasks) {
            if (task.getStatus() == 0) { // Hanya yang belum selesai
                try {
                    java.util.Date deadlineDate = sdf.parse(task.getDeadline());
                    if (deadlineDate != null && deadlineDate.before(now)) {
                        overdueTasks.append("• ").append(task.getTitle())
                                .append(" (").append(task.getDeadline()).append(")\n");
                    }
                } catch (Exception e) { }
            }
        }

        String currentOverdue = overdueTasks.toString();

        // LOGIKA BARU:
        // 1. Jika tidak ada yang terlambat, reset ingatan agar nanti bisa alert lagi kalau ada yang baru
        if (currentOverdue.isEmpty()) {
            lastAlertContent = "";
            return;
        }

        // 2. Jika daftar tugas terlambat SAMA dengan yang sudah di-alert tadi, abaikan (jangan munculin dialog)
        if (currentOverdue.equals(lastAlertContent)) {
            return;
        }

        // 3. Jika isinya beda (ada tugas baru yang telat) dan alert tidak lagi tampil, munculkan!
        if (!isAlertShowing) {
            isAlertShowing = true;

            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Tugas Terlambat dari Deadline!")
                    .setMessage("Tugas berikut sudah melewati deadline:\n\n" + currentOverdue)
                    .setPositiveButton("Saya Mengerti", (dialog, which) -> {
                        isAlertShowing = false;
                        lastAlertContent = currentOverdue; // Simpan daftar ini ke ingatan
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private android.os.Handler realtimeHandler = new android.os.Handler();
    private boolean isAlertShowing = false;
    private String lastAlertContent = "";
    private Runnable realtimeChecker = new Runnable() {
        @Override
        public void run() {
            List<Task> freshTasks = new java.util.ArrayList<>();
            Cursor cursor = dbHelper.getAllTasks();
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                    String course = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COURSE));
                    String priority = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIORITY));
                    String deadline = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DEADLINE));
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS));
                    freshTasks.add(new Task(id, title, course, priority, deadline, status));
                } while (cursor.moveToNext());
            }
            cursor.close();

            checkOverdueTasks(freshTasks);
            updateDashboardStats(); // Refresh dashboard juga!

            realtimeHandler.postDelayed(this, 10 * 1000);
        }
    };

    private void startRealtimeCheck() {
        realtimeHandler.post(realtimeChecker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realtimeHandler.removeCallbacks(realtimeChecker);
    }
}
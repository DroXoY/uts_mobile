package com.example.uts;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MaterialButton btnNavDaftar, btnNavKalender;
    private ConstraintLayout layoutDaftar, layoutKalender;
    private DatabaseHelper dbHelper;
    private RecyclerView rvTugasUtama;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private android.widget.CalendarView calendarView;
    private RecyclerView rvTugasKalender;
    private TaskAdapter adapterKalender;
    private android.widget.TextView tvLabelKalender;
    private String currentSort = "Deadline"; // Default urutkan dari deadline
    private String currentCalendarDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        rvTugasUtama = findViewById(R.id.rv_tugas_utama);
        rvTugasUtama.setLayoutManager(new LinearLayoutManager(this));

        loadData();

        btnNavDaftar = findViewById(R.id.btn_nav_daftar);
        btnNavKalender = findViewById(R.id.btn_nav_kalender);
        layoutDaftar = findViewById(R.id.layout_halaman_daftar);
        layoutKalender = findViewById(R.id.layout_halaman_kalender);

        setupNavigation();
        calendarView = findViewById(R.id.calendar_view);
        rvTugasKalender = findViewById(R.id.rv_tugas_kalender);
        rvTugasKalender.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        tvLabelKalender = findViewById(R.id.tv_label_kalender);

// Pasang sensor klik pada Kalender
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Format tanggal harus SAMA PERSIS dengan format yang kamu simpan dari DatePicker
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            currentCalendarDate = selectedDate;

            // Ubah teks label agar lebih interaktif
            tvLabelKalender.setText("Tugas pada: " + selectedDate);

            // Panggil fungsi untuk memuat data khusus tanggal ini
            loadDataKalender(selectedDate);
        });
        Spinner spinnerSort = findViewById(R.id.spinner_sort);
        String[] opsiSort = {"Deadline", "Nama Tugas", "Prioritas"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, opsiSort);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);
        // Tambahkan sensor ini agar saat user milih, list-nya langsung berubah
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSort = opsiSort[position];
                loadData(); // Panggil ulang data untuk diurutkan
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        Button btnTambah = findViewById(R.id.btn_tambah);
        btnTambah.setOnClickListener(v -> showAddTaskDialog());
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

        // Gunakan TaskAdapter yang sama, tapi datanya beda
        adapterKalender = new TaskAdapter(taskListKalender, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onStatusChanged(int taskId, boolean isDone) {
                dbHelper.updateTaskStatus(taskId, isDone ? 1 : 0);
                loadDataKalender(date); // Refresh kalender
                loadData(); // Refresh list utama juga biar sinkron
            }

            @Override
            public void onDeleteClicked(int taskId) {
                dbHelper.deleteTask(taskId);
                loadDataKalender(date);
                loadData();
                android.widget.Toast.makeText(MainActivity.this, "Tugas Dihapus", android.widget.Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditClicked(Task task) {
                showEditTaskDialog(task);
            }
        });

        rvTugasKalender.setAdapter(adapterKalender);
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Memanggil layout dialog_add_task.xml yang tadi kita buat
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Mencari komponen di dalam dialog
        TextInputEditText etName = dialogView.findViewById(R.id.et_task_name);
        TextInputEditText etCourse = dialogView.findViewById(R.id.et_course_name);
        AutoCompleteTextView actvPriority = dialogView.findViewById(R.id.actv_priority);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btn_pick_date);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_task);

        // Setup Dropdown Prioritas di dalam form
        String[] priorities = {"TINGGI", "SEDANG", "RENDAH"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        actvPriority.setAdapter(priorityAdapter);

        // Setup klik tombol kalender
        btnPickDate.setOnClickListener(v -> showDatePicker(btnPickDate));

        // Setup klik tombol simpan (sementara cuma nge-print Toast dulu)
        btnSave.setOnClickListener(v -> {
            String title = etName.getText().toString();
            String course = etCourse.getText().toString();
            String priority = actvPriority.getText().toString();
            String date = btnPickDate.getText().toString();

            if (title.isEmpty() || date.equals("Pilih Tanggal Deadline") || priority.isEmpty() || course.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show();
            } else {
                // PERINTAH SQLITE: Simpan data
                long result = dbHelper.insertTask(title, course, priority, date);

                if (result != -1) {
                    Toast.makeText(this, "Tugas berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    loadData();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }
    private void showEditTaskDialog(Task taskToEdit) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // Inisialisasi komponen
        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.et_task_name);
        com.google.android.material.textfield.TextInputEditText etCourse = dialogView.findViewById(R.id.et_course_name);
        android.widget.AutoCompleteTextView actvPriority = dialogView.findViewById(R.id.actv_priority);
        com.google.android.material.button.MaterialButton btnPickDate = dialogView.findViewById(R.id.btn_pick_date);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_task);

        // Ubah teks judul dialog dan tombol
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText("Edit Tugas");
        btnSave.setText("Update Tugas");

        // === ISI OTOMATIS DATA LAMA KE DALAM FORM ===
        etName.setText(taskToEdit.getTitle());
        etCourse.setText(taskToEdit.getCourse());
        btnPickDate.setText(taskToEdit.getDeadline());

        String[] priorities = {"TINGGI", "SEDANG", "RENDAH"};
        android.widget.ArrayAdapter<String> priorityAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        actvPriority.setAdapter(priorityAdapter);
        // Set prioritas lama (jangan lupa kasih false biar dropdown tidak langsung kebuka)
        actvPriority.setText(taskToEdit.getPriority(), false);

        // Setup klik kalender
        btnPickDate.setOnClickListener(v -> showDatePicker(btnPickDate));

        // Logika Tombol Simpan (Update)
        btnSave.setOnClickListener(v -> {
            String newTitle = etName.getText().toString();
            String newCourse = etCourse.getText().toString();
            String newPriority = actvPriority.getText().toString();
            String newDate = btnPickDate.getText().toString();

            if (newTitle.isEmpty() || newDate.equals("Pilih Tanggal Deadline")) {
                android.widget.Toast.makeText(this, "Nama Tugas dan Tanggal wajib diisi!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Panggil fungsi UPDATE dari DatabaseHelper
                dbHelper.updateTaskData(taskToEdit.getId(), newTitle, newCourse, newPriority, newDate);

                android.widget.Toast.makeText(this, "Tugas berhasil diupdate!", android.widget.Toast.LENGTH_SHORT).show();

                loadData(); // Refresh list utama
                if (layoutKalender.getVisibility() == View.VISIBLE && !currentCalendarDate.isEmpty()) {
                    loadDataKalender(currentCalendarDate);
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showDatePicker(MaterialButton btnPickDate) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // Jika user milih tanggal, teks di tombol akan berubah jadi tanggalnya
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            btnPickDate.setText(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void setupNavigation() {
        btnNavKalender.setOnClickListener(v -> {
            layoutDaftar.setVisibility(View.GONE);
            layoutKalender.setVisibility(View.VISIBLE);

            btnNavKalender.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnNavKalender.setTextColor(Color.parseColor("#3D5AFE"));
            btnNavKalender.setIconTintResource(R.color.colorPrimary);

            btnNavDaftar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5C7CFF")));
            btnNavDaftar.setTextColor(Color.WHITE);
            btnNavDaftar.setIconTintResource(R.color.white);

            // ====================================================
            // BARU: Paksa kalender memuat data hari ini secara otomatis
            // ====================================================
            java.util.Calendar cal = java.util.Calendar.getInstance();
            // Format tanggal harus disamakan persis dengan format DatePicker
            String todayDate = cal.get(java.util.Calendar.DAY_OF_MONTH) + "/" +
                    (cal.get(java.util.Calendar.MONTH) + 1) + "/" +
                    cal.get(java.util.Calendar.YEAR);
            currentCalendarDate = todayDate;

            // Update label dan panggil data
            tvLabelKalender.setText("Tugas pada: " + todayDate);
            loadDataKalender(todayDate);
        });
        btnNavDaftar.setOnClickListener(v -> {
            // Kebalikannya: Tampilkan Daftar, Sembunyikan Kalender
            layoutDaftar.setVisibility(View.VISIBLE);
            layoutKalender.setVisibility(View.GONE);

            btnNavDaftar.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnNavDaftar.setTextColor(Color.parseColor("#3D5AFE"));
            btnNavDaftar.setIconTintResource(R.color.colorPrimary);

            btnNavKalender.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5C7CFF")));
            btnNavKalender.setTextColor(Color.WHITE);
            btnNavKalender.setIconTintResource(R.color.white);
        });
    }
    private void loadData() {
        taskList = new ArrayList<>();

        // 1. Ambil data dari DatabaseHelper
        Cursor cursor = dbHelper.getAllTasks();

        // 2. Cek apakah database ada isinya
        if (cursor.moveToFirst()) {
            do {
                // Ambil data tiap kolom berdasarkan index-nya
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String course = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COURSE));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIORITY));
                String deadline = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DEADLINE));

                // Masukkan ke dalam list
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS));
                taskList.add(new Task(id, title, course, priority, deadline, status));
            } while (cursor.moveToNext());
        }
        cursor.close(); // Selalu tutup cursor setelah dipakai
        if (currentSort.equals("Nama Tugas")) {
            // Urutkan berdasarkan Abjad (A-Z)
            java.util.Collections.sort(taskList, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));

        } else if (currentSort.equals("Prioritas")) {
            // Urutkan TINGGI lalu SEDANG lalu RENDAH
            java.util.Collections.sort(taskList, (t1, t2) -> {
                // Beri nilai untuk Tugas 1
                int val1 = t1.getPriority().equalsIgnoreCase("TINGGI") ? 3 :
                        (t1.getPriority().equalsIgnoreCase("SEDANG") ? 2 : 1);

                // Beri nilai untuk Tugas 2
                int val2 = t2.getPriority().equalsIgnoreCase("TINGGI") ? 3 :
                        (t2.getPriority().equalsIgnoreCase("SEDANG") ? 2 : 1);

                // Urutkan dari angka terbesar (3) ke terkecil (1)
                return Integer.compare(val2, val1);
            });

        } else if (currentSort.equals("Deadline")) {
            // Urutkan berdasarkan Tanggal paling dekat
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault());
            java.util.Collections.sort(taskList, (t1, t2) -> {
                try {
                    java.util.Date d1 = sdf.parse(t1.getDeadline());
                    java.util.Date d2 = sdf.parse(t2.getDeadline());
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return 0; // Kalau gagal parse, abaikan
                }
            });
        }

        // 3. Pasang Adapter ke RecyclerView
        adapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onStatusChanged(int taskId, boolean isDone) {
                // Update database: jika dicentang jadi 1, jika tidak jadi 0
                dbHelper.updateTaskStatus(taskId, isDone ? 1 : 0);
                loadData(); // Refresh layar biar teksnya kecoret
            }

            @Override
            public void onDeleteClicked(int taskId) {
                // Tampilkan konfirmasi sebelum hapus (opsional tapi bagus)
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus Tugas")
                        .setMessage("Yakin ingin menghapus tugas ini?")
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbHelper.deleteTask(taskId);
                            loadData(); // Refresh layar
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
    }
}
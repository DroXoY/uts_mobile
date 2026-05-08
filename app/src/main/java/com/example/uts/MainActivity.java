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
        startRealtimeCheck();


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
        android.widget.ImageView btnTentangKami = findViewById(R.id.btnTentangKami);
        btnTentangKami.setOnClickListener(v -> showAboutUsDialog());
        MaterialButton btnNavDashboard = findViewById(R.id.btn_nav_dashboard);
        btnNavDashboard.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, DashboardActivity.class);
            startActivity(intent);
        });
    }

    private void showAboutUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("👥 About Us")
                .setMessage(
                        "Aplikasi ini dibuat oleh:\n\n" +
                                "1. Farrel Endra A.\n   NIM: 24410100016\n\n" +
                                "2. Michael Christoper\n   NIM: 24410100007\n\n" +
                                "3. Jonathan Kevin\n   NIM: 24410100027"
                )
                .setPositiveButton("OK", null)
                .show();
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
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus Tugas")
                        .setMessage("Yakin ingin menghapus tugas ini?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbHelper.deleteTask(taskId);
                            loadDataKalender(date); // Refresh list kalender saat ini
                            loadData(); // Refresh list utama agar sinkron
                            Toast.makeText(MainActivity.this, "Tugas Dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null) // 'null' berarti dialog otomatis tertutup saja
                        .show();
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
        MaterialButton btnPickTime = dialogView.findViewById(R.id.btn_pick_time);
        btnPickTime.setOnClickListener(v -> showTimePicker(btnPickDate, btnPickTime));

        // Setup Dropdown Prioritas di dalam form
        String[] priorities = {"TINGGI", "SEDANG", "RENDAH"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        actvPriority.setAdapter(priorityAdapter);

        // Setup klik tombol kalender
        btnPickDate.setOnClickListener(v -> showDatePicker(btnPickDate));

        // Setup klik tombol simpan
        btnSave.setOnClickListener(v -> {
            String title = etName.getText().toString();
            String course = etCourse.getText().toString();
            String priority = actvPriority.getText().toString();
            String date = btnPickDate.getText().toString();
            String time = btnPickTime.getText().toString();

            // 1. Validasi Semua Kolom (Jadikan satu pintu)
            if (title.isEmpty() || course.isEmpty() || priority.isEmpty() || date.equals("Pilih Tanggal Deadline") || time.equals("Pilih Jam")) {
                Toast.makeText(this, "Semua kolom wajib diisi dengan benar!", Toast.LENGTH_SHORT).show();
                return; // Langsung hentikan proses di sini jika ada yang kosong
            }

            // 2. GABUNGKAN TANGGAL DAN JAM
            String fullDeadline = date + " " + time; // Hasilnya: "24/5/2026 14:00"

            // 3. PERINTAH SQLITE: Simpan data (CUKUP PANGGIL 1 KALI SAJA)
            long result = dbHelper.insertTask(title, course, priority, fullDeadline);

            // 4. Cek Hasil
            if (result != -1) {
                Toast.makeText(this, "Tugas berhasil disimpan!", Toast.LENGTH_SHORT).show();

                // Refresh list utama
                loadData();

                // (Opsional) Refresh list kalender jika sedang terbuka
                if (layoutKalender.getVisibility() == View.VISIBLE && currentCalendarDate != null && !currentCalendarDate.isEmpty()) {
                    loadDataKalender(currentCalendarDate);
                }

                dialog.dismiss();
            } else {
                Toast.makeText(this, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show();
            }
        });;

        dialog.show();
    }
    private void showEditTaskDialog(Task taskToEdit) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // 1. Inisialisasi komponen
        TextInputEditText etName = dialogView.findViewById(R.id.et_task_name);
        TextInputEditText etCourse = dialogView.findViewById(R.id.et_course_name);
        AutoCompleteTextView actvPriority = dialogView.findViewById(R.id.actv_priority);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btn_pick_date);
        MaterialButton btnPickTime = dialogView.findViewById(R.id.btn_pick_time); // Tombol Jam
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_task);

        // 2. Ubah teks judul dialog dan tombol
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText("Edit Tugas");
        btnSave.setText("Update Tugas");

        // 3. === ISI OTOMATIS DATA LAMA ===
        etName.setText(taskToEdit.getTitle());
        etCourse.setText(taskToEdit.getCourse());

        // Logika Pemisah (Split) Tanggal dan Jam
        String oldDeadline = taskToEdit.getDeadline();
        if (oldDeadline != null && oldDeadline.contains(" ")) {
            String[] parts = oldDeadline.split(" ");
            btnPickDate.setText(parts[0]); // Isi tombol tanggal
            if (parts.length > 1) {
                btnPickTime.setText(parts[1]); // Isi tombol jam
            }
        } else {
            // Berjaga-jaga kalau data lama sebelum revisi (belum ada jamnya)
            btnPickDate.setText(oldDeadline);
            btnPickTime.setText("00:00");
        }

        // 4. Setup Dropdown Prioritas
        String[] priorities = {"TINGGI", "SEDANG", "RENDAH"};
        android.widget.ArrayAdapter<String> priorityAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        actvPriority.setAdapter(priorityAdapter);
        actvPriority.setText(taskToEdit.getPriority(), false); // false biar tidak mekar otomatis

        // 5. Setup Klik Waktu
        btnPickDate.setOnClickListener(v -> showDatePicker(btnPickDate));
        btnPickTime.setOnClickListener(v -> showTimePicker(btnPickDate, btnPickTime));

        // 6. Logika Tombol Simpan (UPDATE)
        btnSave.setOnClickListener(v -> {
            String newTitle = etName.getText().toString();
            String newCourse = etCourse.getText().toString();
            String newPriority = actvPriority.getText().toString();
            String date = btnPickDate.getText().toString();
            String time = btnPickTime.getText().toString();

            // Validasi: Jangan ada yang kosong atau masih teks default
            if (newTitle.isEmpty() || date.equals("Pilih Tanggal Deadline") || time.equals("Pilih Jam")) {
                Toast.makeText(this, "Nama, Tanggal, dan Jam wajib diisi!", Toast.LENGTH_SHORT).show();
                return; // Hentikan proses kalau masih kosong
            }

            // GABUNGKAN TANGGAL DAN JAM UNTUK DISIMPAN
            String fullDeadline = date + " " + time; // Contoh: "24/5/2026 14:00"

            // PANGGIL FUNGSI UPDATE DATA (Bukan Insert!)
            dbHelper.updateTaskData(taskToEdit.getId(), newTitle, newCourse, newPriority, fullDeadline);

            Toast.makeText(this, "Tugas berhasil diupdate!", Toast.LENGTH_SHORT).show();

            // Refresh List Utama
            loadData();

            // Refresh List Kalender jika sedang dibuka
            if (layoutKalender.getVisibility() == View.VISIBLE && currentCalendarDate != null && !currentCalendarDate.isEmpty()) {
                loadDataKalender(currentCalendarDate);
            }

            dialog.dismiss(); // Tutup Pop-up
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

        // Cek apakah tanggal yang dipilih user adalah HARI INI
        String selectedDateStr = btnPickDate.getText().toString();

        // Format tanggal hari ini sama seperti yang disimpan di tombol: "d/M/yyyy"
        String todayStr = now.get(Calendar.DAY_OF_MONTH) + "/"
                + (now.get(Calendar.MONTH) + 1) + "/"
                + now.get(Calendar.YEAR);
        final boolean isToday = selectedDateStr.equals(todayStr);

        // Jika hari ini: jam awal di picker = jam sekarang, agar user tidak bisa pilih jam lampau
        // Jika hari lain (besok dst): jam awal bebas mulai 00:00
        int defaultHour   = isToday ? now.get(Calendar.HOUR_OF_DAY) : 0;
        int defaultMinute = isToday ? now.get(Calendar.MINUTE)       : 0;



        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {

            // Validasi tambahan: jika tanggalnya hari ini dan jam yang dipilih sudah lewat
            if (isToday) {
                int nowHour   = now.get(Calendar.HOUR_OF_DAY);
                int nowMinute = now.get(Calendar.MINUTE);

                boolean jamSudahLewat = (hourOfDay < nowHour)
                        || (hourOfDay == nowHour && minuteOfHour <= nowMinute);

                if (jamSudahLewat) {
                    // Tolak pilihan dan tampilkan pesan, lalu buka ulang picker
                    Toast.makeText(this,
                            "Jam tidak boleh sebelum atau sama dengan waktu sekarang!",
                            Toast.LENGTH_SHORT).show();
                    showTimePicker(btnPickDate, btnPickTime); // Buka ulang picker
                    return;
                }
            }

            // Format jam agar selalu 2 digit (misal: 08:05, bukan 8:5)
            String time = String.format("%02d:%02d", hourOfDay, minuteOfHour);
            btnPickTime.setText(time);

        }, defaultHour, defaultMinute, true); // 'true' untuk format 24 jam

        timePickerDialog.show();
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
            // Ubah format "d/M/yyyy" menjadi "d/M/yyyy HH:mm"
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy HH:mm", java.util.Locale.getDefault());
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

            public void onDeleteClicked(int taskId) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus Tugas")
                        .setMessage("Yakin ingin menghapus tugas ini?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbHelper.deleteTask(taskId);
                            loadData(); // Refresh list utama agar sinkron
                            Toast.makeText(MainActivity.this, "Tugas Dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null) // 'null' berarti dialog otomatis tertutup saja
                        .show();
            }
            @Override
            public void onEditClicked(Task task) {
                showEditTaskDialog(task);
            }
        });
        rvTugasUtama.setAdapter(adapter);

        checkOverdueTasks(taskList);
    }

    private void checkOverdueTasks(List<Task> tasks) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy HH:mm", java.util.Locale.getDefault());

        // Waktu sekarang persis (termasuk jam dan menit) — BUKAN tengah malam
        java.util.Date now = new java.util.Date();

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        StringBuilder overdueTasks = new StringBuilder();

        for (Task task : tasks) {
            // Cek hanya yang belum selesai (status == 0)
            if (task.getStatus() == 0) {
                try {
                    java.util.Date deadlineDate = sdf.parse(task.getDeadline());
                    // Jika tanggal deadline SEBELUM hari ini → terlambat
                    if (deadlineDate != null && deadlineDate.before(todayCal.getTime())) {
                        overdueTasks.append("• ").append(task.getTitle())
                                .append(" (").append(task.getDeadline()).append(")\n");
                    }
                } catch (Exception e) {
                    // Abaikan jika format tanggal tidak valid
                }
            }
        }

        // Tampilkan alert jika ada tugas yang terlambat
        if (overdueTasks.length() > 0 && !isAlertShowing) {
            isAlertShowing = true;
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Tugas Terlambat dari Deadline!")
                    .setMessage("Tugas berikut belum diselesaikan dan sudah melewati deadline:\n\n" + overdueTasks)
                    .setPositiveButton("OK", (dialog, which) -> isAlertShowing = false)
                    .setCancelable(false)
                    .show();
        }
    }

    private android.os.Handler realtimeHandler = new android.os.Handler();
    private boolean isAlertShowing = false; // Cegah alert menumpuk

    private Runnable realtimeChecker = new Runnable() {
        @Override
        public void run() {
            // Ambil data terbaru dari DB lalu cek
            List<Task> freshTasks = new java.util.ArrayList<>();
            android.database.Cursor cursor = dbHelper.getAllTasks();
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

            checkOverdueTasks(freshTasks); // Cek apakah ada yang baru lewat deadline

            // Jadwalkan ulang 30 detik kemudian
            realtimeHandler.postDelayed(this, 10 * 1000);
        }
    };

    private void startRealtimeCheck() {
        // Mulai timer pertama kali — delay 30 detik setelah app dibuka
        realtimeHandler.post(realtimeChecker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan timer saat Activity ditutup agar tidak memory leak
        realtimeHandler.removeCallbacks(realtimeChecker);
    }
}
package com.example.uts;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvSelesai, tvTerlambat, tvProgressText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);

        // Inisialisasi view
        tvSelesai       = findViewById(R.id.tv_jumlah_selesai);
        tvTerlambat     = findViewById(R.id.tv_jumlah_terlambat);
        tvProgressText  = findViewById(R.id.tv_progress_text);
        progressBar     = findViewById(R.id.progress_bar);

        loadDashboardData();

        // Tombol kembali ke MainActivity
        Button btnBack = findViewById(R.id.btn_back_dashboard);
        btnBack.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadDashboardData() {
        // 1. Hitung total tugas selesai (status = 1)
        int totalSelesai = dbHelper.getTotalSelesai();

        // 2. Hitung tugas terlambat secara manual di Java
        int totalTerlambat = hitungTugasTerlambat();

        // 3. Hitung total semua tugas
        Cursor allCursor = dbHelper.getAllTasks();
        int totalSemua = allCursor.getCount();
        allCursor.close();

        // 4. Tampilkan angka ke TextView
        tvSelesai.setText(String.valueOf(totalSelesai));
        tvTerlambat.setText(String.valueOf(totalTerlambat));

        // 5. Hitung & tampilkan progress bar
        if (totalSemua > 0) {
            int persentase = (totalSelesai * 100) / totalSemua;
            progressBar.setProgress(persentase);
            tvProgressText.setText(totalSelesai + " dari " + totalSemua + " tugas selesai (" + persentase + "%)");
        } else {
            tvProgressText.setText("Belum ada tugas yang ditambahkan");
        }
    }

    private int hitungTugasTerlambat() {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());

        java.util.Date now = new java.util.Date();



        int count = 0;
        // Ambil semua tugas yang belum selesai (status = 0)
        Cursor cursor = dbHelper.getAllUnfinishedTasks();

        if (cursor.moveToFirst()) {
            do {
                String deadline = cursor.getString(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DEADLINE));
                try {
                    java.util.Date deadlineDate = sdf.parse(deadline);
                    // Jika deadline SEBELUM hari ini → terlambat
                    if (deadlineDate != null && deadlineDate.before(now)) {
                        count++;
                    }
                } catch (Exception e) {
                    // Abaikan baris dengan format tanggal tidak valid
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return count;
    }
}
package com.example.uts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // 1. Nama Database & Versi
    private static final String DATABASE_NAME = "tugas_db";
    private static final int DATABASE_VERSION = 2;

    // 2. Nama Tabel & Kolom
    public static final String TABLE_NAME = "tasks";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_COURSE = "course";
    public static final String COL_PRIORITY = "priority";
    public static final String COL_DEADLINE = "deadline";
    public static final String COL_STATUS = "status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // 3. Membuat Tabel saat pertama kali dijalankan
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_COURSE + " TEXT, " +
                COL_PRIORITY + " TEXT, " +
                COL_DEADLINE + " TEXT, " +
                COL_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 4. Perintah Simpan (Insert)
    public long insertTask(String title, String course, String priority, String deadline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_COURSE, course);
        values.put(COL_PRIORITY, priority);
        values.put(COL_DEADLINE, deadline);
        values.put(COL_STATUS, 0);

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    // 5. Perintah Ambil Data (Query)
    public Cursor getAllTasks() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY id DESC", null);
    }

    public void updateTaskStatus(int id, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        db.update(TABLE_NAME, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
    // Fungsi untuk Edit Data Tugas
    public void updateTaskData(int id, String title, String course, String priority, String deadline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_COURSE, course);
        values.put(COL_PRIORITY, priority);
        values.put(COL_DEADLINE, deadline);

        // Update baris yang ID-nya cocok
        db.update(TABLE_NAME, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
    // Fungsi untuk mengambil data berdasarkan tanggal
    public Cursor getTasksByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Gunakan LIKE dan tambahkan % di akhir tanggal
        // % artinya "cari yang depannya sama dengan tanggal ini, sisanya (jam) bebas apa saja"
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_DEADLINE + " LIKE ?", new String[]{date + "%"});
    }

    public int getTotalSelesai() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + COL_STATUS + " = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public Cursor getAllUnfinishedTasks() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_STATUS + " = 0", null);
    }

}
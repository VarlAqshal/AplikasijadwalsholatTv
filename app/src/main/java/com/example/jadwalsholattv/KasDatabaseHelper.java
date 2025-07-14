package com.example.jadwalsholattv;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class KasDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "kas.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_KAS = "kas";
    public static final String COL_ID = "id";
    public static final String COL_JUMLAH = "jumlah";
    public static final String COL_KATEGORI = "kategori";
    public static final String COL_TANGGAL = "tanggal";

    public KasDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_KAS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_JUMLAH + " INTEGER, " +
                COL_KATEGORI + " TEXT, " +
                COL_TANGGAL + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KAS);
        onCreate(db);
    }

    public void tambahKas(int jumlah, String kategori, String tanggal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_JUMLAH, jumlah);
        values.put(COL_KATEGORI, kategori);
        values.put(COL_TANGGAL, tanggal);
        db.insert(TABLE_KAS, null, values);
        db.close();
    }

    public int getTotalMasuk() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_JUMLAH + ") FROM " + TABLE_KAS + " WHERE " + COL_KATEGORI + "='Masuk'", null);
        int total = 0;
        if (cursor.moveToFirst()) total = cursor.getInt(0);
        cursor.close();
        db.close();
        return total;
    }

    public int getTotalKeluar() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_JUMLAH + ") FROM " + TABLE_KAS + " WHERE " + COL_KATEGORI + "='Keluar'", null);
        int total = 0;
        if (cursor.moveToFirst()) total = cursor.getInt(0);
        cursor.close();
        db.close();
        return total;
    }

    public void hapusSemuaDataKas() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_KAS, null, null);
        db.close();
    }
}
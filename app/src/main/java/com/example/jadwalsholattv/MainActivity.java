package com.example.jadwalsholattv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvCurrentTime, tvDate;
    private TextView tvImsak, tvSubuh, tvSyuruq, tvTvDzuhur, tvAshar, tvMaghrib, tvIsya;
    private TextView tvJumlahKas, tvJumlahPengeluaran, tvSaldoAkhir;
    private ImageButton btnKas;
    private KasDatabaseHelper dbHelper;

    private Handler handler;
    private Runnable runnable;

    // Pastikan API_URL ini sudah benar untuk lokasi yang Anda inginkan (1301 untuk Semarang)
    private static final String API_URL = "https://api.myquran.com/v2/sholat/jadwal/1301/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvDate = findViewById(R.id.tvDate);
        tvImsak = findViewById(R.id.tvImsak);
        tvSubuh = findViewById(R.id.tvSubuh);
        tvSyuruq = findViewById(R.id.tvSyuruq);
        tvTvDzuhur = findViewById(R.id.tvDzuhur);
        tvAshar = findViewById(R.id.tvAshar);
        tvMaghrib = findViewById(R.id.tvMaghrib);
        tvIsya = findViewById(R.id.tvIsya);

        dbHelper = new KasDatabaseHelper(this);
        tvJumlahKas = findViewById(R.id.tvTotalKas);
        tvJumlahPengeluaran = findViewById(R.id.tvPengeluaran);
        tvSaldoAkhir = findViewById(R.id.tvSaldoKas);
        btnKas = findViewById(R.id.btnInputKas);

        updateKasDisplay();
        btnKas.setOnClickListener(v -> showKasDialog());

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        updateDate();
        setPrayerTimePlaceholders();
        // Memanggil fetchPrayerTimes() untuk mendapatkan jadwal dan menjadwalkan alarm
        fetchPrayerTimes();
    }

    private void setPrayerTimePlaceholders() {
        String placeholder = "--:--";
        tvImsak.setText(placeholder);
        tvSubuh.setText(placeholder);
        tvSyuruq.setText(placeholder);
        tvTvDzuhur.setText(placeholder);
        tvAshar.setText(placeholder);
        tvMaghrib.setText(placeholder);
        tvIsya.setText(placeholder);
    }

    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH : mm : ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        tvCurrentTime.setText(currentTime);
    }

    private void updateDate() {
        SimpleDateFormat sdfMasehi = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        String dateMasehi = sdfMasehi.format(new Date());
        // Perbarui placeholder tanggal Hijriah jika Anda mendapatkan data yang akurat dari API
        String hijriDatePlaceholder = "17 Muharram 1447 H"; // Contoh, ini akan berubah besok
        tvDate.setText(dateMasehi + " / " + hijriDatePlaceholder);
    }

    private void fetchPrayerTimes() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String currentDate = sdf.format(new Date()); // Mengambil tanggal hari ini
        String url = API_URL + currentDate;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONObject jadwal = data.getJSONObject("jadwal");

                        String imsakTime = jadwal.getString("imsak");
                        String subuhTime = jadwal.getString("subuh");
                        String syuruqTime = jadwal.getString("terbit"); // API menggunakan "terbit" untuk syuruq
                        String dzuhurTime = jadwal.getString("dzuhur");
                        String asharTime = jadwal.getString("ashar");
                        String maghribTime = jadwal.getString("maghrib");
                        String isyaTime = jadwal.getString("isya");

                        tvImsak.setText(imsakTime);
                        tvSubuh.setText(subuhTime);
                        tvSyuruq.setText(syuruqTime);
                        tvTvDzuhur.setText(dzuhurTime);
                        tvAshar.setText(asharTime);
                        tvMaghrib.setText(maghribTime);
                        tvIsya.setText(isyaTime);

                        // PENTING: Panggil metode ini setelah data jadwal diterima dan ditampilkan
                        setAllPrayerAlarms(imsakTime, subuhTime, syuruqTime, dzuhurTime, asharTime, maghribTime, isyaTime);

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Gagal parse data jadwal: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Gagal parse data jadwal.", Toast.LENGTH_SHORT).show();
                        setPrayerTimePlaceholders();
                    }
                }, error -> {
            Log.e("MainActivity", "Tidak dapat terhubung ke server jadwal: " + error.getMessage());
            Toast.makeText(MainActivity.this, "Tidak dapat terhubung ke server jadwal.", Toast.LENGTH_SHORT).show();
            setPrayerTimePlaceholders();
        });
        queue.add(jsonObjectRequest);
    }

    // --- METODE UNTUK ALARM DIMULAI DI SINI ---

    /**
     * Menjadwalkan semua alarm waktu sholat untuk hari ini atau besok.
     * Alarm lama akan dibatalkan terlebih dahulu untuk mencegah duplikasi.
     */
    private void setAllPrayerAlarms(String imsak, String subuh, String syuruq, String dzuhur, String ashar, String maghrib, String isya) {
        // Batalkan semua alarm yang mungkin sudah diatur sebelumnya
        cancelAllPrayerAlarms();

        // Jadwalkan alarm untuk setiap waktu sholat dengan requestCode unik
        schedulePrayerAlarm("Imsak", imsak, 101);
        schedulePrayerAlarm("Subuh", subuh, 102);
        schedulePrayerAlarm("Syuruq", syuruq, 103);
        schedulePrayerAlarm("Dzuhur", dzuhur, 104);
        schedulePrayerAlarm("Ashar", ashar, 105);
        schedulePrayerAlarm("Maghrib", maghrib, 106);
        schedulePrayerAlarm("Isya", isya, 107);

        Log.d("AlarmScheduler", "Semua alarm sholat berhasil dijadwalkan.");
    }

    /**
     * Menjadwalkan alarm spesifik untuk waktu sholat.
     * Alarm akan dijadwalkan untuk hari ini. Jika waktu sholat sudah lewat, akan dijadwalkan untuk besok.
     *
     * @param prayerName Nama sholat (misal: "Subuh", "Dzuhur").
     * @param timeString Waktu sholat dalam format "HH:mm".
     * @param requestCode Kode unik untuk PendingIntent alarm ini.
     */
    private void schedulePrayerAlarm(String prayerName, String timeString, int requestCode) {
        try {
            long currentTimeMillis = System.currentTimeMillis();

            // Format tanggal dan waktu untuk parsing
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

            String todayDateStr = dateFormat.format(new Date()); // Dapatkan tanggal hari ini

            // Gabungkan tanggal hari ini dengan waktu sholat
            String fullPrayerTimeString = todayDateStr + " " + timeString;
            Date prayerDate = fullDateTimeFormat.parse(fullPrayerTimeString);

            if (prayerDate == null) {
                Log.e("AlarmScheduler", "Gagal parse waktu sholat untuk " + prayerName + ": " + timeString);
                Toast.makeText(this, "Gagal menjadwalkan " + prayerName + ": format waktu salah", Toast.LENGTH_SHORT).show();
                return;
            }

            long prayerTimeMillis = prayerDate.getTime();

            // Jika waktu sholat sudah lewat hari ini, jadwalkan untuk besok
            if (prayerTimeMillis <= currentTimeMillis) {
                prayerTimeMillis += (24 * 60 * 60 * 1000L); // Tambahkan 24 jam (dalam milidetik)
                Log.d("AlarmScheduler", prayerName + " (" + timeString + ") sudah lewat hari ini, menjadwalkan untuk besok: " + new Date(prayerTimeMillis));
            }

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            // Action harus sama dengan yang dideklarasikan di AndroidManifest.xml
            intent.setAction("com.example.jadwalsholattv.ACTION_ALARM_PRAYER");
            intent.putExtra("prayerName", prayerName); // Kirim nama sholat ke receiver

            // FLAG_UPDATE_CURRENT: Memperbarui Intent yang ada.
            // FLAG_IMMUTABLE: Penting untuk Android 12 (API 31) ke atas untuk keamanan.
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                // Pilih metode penjadwalan alarm yang paling akurat sesuai versi Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Untuk Android 6.0 (Marshmallow) ke atas, gunakan setExactAndAllowWhileIdle
                    // Ini memastikan alarm tetap akurat bahkan di mode Doze (penghematan daya)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prayerTimeMillis, pendingIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Untuk Android 4.4 (KitKat) hingga 5.1 (Lollipop), gunakan setExact
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, prayerTimeMillis, pendingIntent);
                } else {
                    // Untuk versi Android yang lebih lama
                    alarmManager.set(AlarmManager.RTC_WAKEUP, prayerTimeMillis, pendingIntent);
                }
                Log.d("AlarmScheduler", "Alarm untuk " + prayerName + " berhasil dijadwalkan pada: " + new Date(prayerTimeMillis).toString());
                Toast.makeText(this, "Alarm " + prayerName + " dijadwalkan!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("AlarmScheduler", "AlarmManager tidak tersedia di perangkat ini.");
                Toast.makeText(this, "Sistem alarm tidak tersedia.", Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            Log.e("AlarmScheduler", "Kesalahan parsing waktu untuk " + prayerName + ": " + e.getMessage());
            Toast.makeText(this, "Kesalahan menjadwalkan alarm untuk " + prayerName, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            // Tangani jika izin SCHEDULE_EXACT_ALARM tidak diberikan (khusus Android 12+)
            Log.e("AlarmScheduler", "Izin SCHEDULE_EXACT_ALARM tidak diberikan: " + e.getMessage());
            Toast.makeText(this, "Perlu izin 'Alarm & Pengingat' untuk mengatur alarm secara akurat. Silakan berikan izin di Pengaturan aplikasi.", Toast.LENGTH_LONG).show();
            // Anda bisa menambahkan Intent untuk membuka pengaturan izin di sini jika diperlukan
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //     Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            //     startActivity(intent);
            // }
        }
    }

    /**
     * Membatalkan alarm spesifik berdasarkan requestCode-nya.
     *
     * @param requestCode Kode unik alarm yang akan dibatalkan.
     */
    private void cancelPrayerAlarm(int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.example.jadwalsholattv.ACTION_ALARM_PRAYER"); // Action harus sama
        // FLAG_NO_CREATE: Hanya mendapatkan PendingIntent jika sudah ada, jangan buat yang baru.
        // FLAG_IMMUTABLE: Wajib untuk Android 12 (API 31) ke atas.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent); // Batalkan alarm di AlarmManager
            pendingIntent.cancel(); // Batalkan PendingIntent itu sendiri
            Log.d("AlarmScheduler", "Alarm dengan requestCode " + requestCode + " berhasil dibatalkan.");
        } else {
            Log.d("AlarmScheduler", "Alarm dengan requestCode " + requestCode + " tidak ditemukan atau sudah dibatalkan sebelumnya.");
        }
    }

    /**
     * Membatalkan semua alarm sholat yang telah dijadwalkan.
     */
    private void cancelAllPrayerAlarms() {
        // Panggil cancelPrayerAlarm untuk setiap kode unik sholat
        cancelPrayerAlarm(101); // Imsak
        cancelPrayerAlarm(102); // Subuh
        cancelPrayerAlarm(103); // Syuruq
        cancelPrayerAlarm(104); // Dzuhur
        cancelPrayerAlarm(105); // Ashar
        cancelPrayerAlarm(106); // Maghrib
        cancelPrayerAlarm(107); // Isya
        Log.d("AlarmScheduler", "Semua alarm sholat yang ada telah dibatalkan.");
    }

    // --- METODE UNTUK ALARM BERAKHIR DI SINI ---


    private void updateKasDisplay() {
        int totalMasuk = dbHelper.getTotalMasuk();
        int totalKeluar = dbHelper.getTotalKeluar();
        int saldo = totalMasuk - totalKeluar;

        tvJumlahKas.setText("Kas Masuk: Rp " + totalMasuk);
        tvJumlahPengeluaran.setText("Kas Keluar: Rp " + totalKeluar);
        tvSaldoAkhir.setText("Saldo Akhir: Rp " + saldo);
    }

    private void showKasDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input_kas, null);
        builder.setView(view);

        final EditText etJumlah = view.findViewById(R.id.etJumlah);
        final RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        final Button btnSimpan = view.findViewById(R.id.btn_simpan);
        final Button btnHapusSemua = view.findViewById(R.id.btn_hapus_semua);

        final AlertDialog dialog = builder.create();

        btnSimpan.setOnClickListener(v -> {
            String jumlahStr = etJumlah.getText().toString();
            if (jumlahStr.isEmpty()) {
                Toast.makeText(this, "Jumlah tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Pilih jenis transaksi", Toast.LENGTH_SHORT).show();
                return;
            }

            int jumlah = Integer.parseInt(jumlahStr);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String tanggal = sdf.format(new Date());
            String kategori = (selectedId == R.id.radioKasMasuk) ? "Masuk" : "Keluar";

            dbHelper.tambahKas(jumlah, kategori, tanggal);
            updateKasDisplay();
            Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnHapusSemua.setOnClickListener(v -> {
            dbHelper.hapusSemuaDataKas();
            updateKasDisplay();
            Toast.makeText(this, "Semua data kas telah dihapus", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Pastikan handler untuk waktu realtime dihentikan
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
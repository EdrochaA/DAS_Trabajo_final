package com.example.unigo.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.GenericResponse;
import com.example.unigo.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import java.io.ByteArrayOutputStream;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG       = "ProfileActivity";
    private static final String PREFS     = "SessionPrefs";
    private static final String KEY_PHOTO = "fotoUrl";

    private ImageView imgUserIcon;
    private TextView  tvName, tvPhone, tvEmail;
    private Button    btnBack, btnLogout;
    private View      cardProfileInfo;

    private String username;
    private int    userId;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bitmap thumb = result.getData().getExtras().getParcelable("data");
                            imgUserIcon.setImageBitmap(thumb);
                            uploadImageToServer(thumb);
                        }
                    }
            );

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), granted -> {
                        if (granted) {
                            openCamera();
                        } else {
                            // CORREGIDO: Usando el recurso de strings
                            Toast.makeText(this, getString(R.string.profile_toast_camera_denied), Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.d(TAG, "onCreate");

        imgUserIcon      = findViewById(R.id.imgUserIcon);
        tvName           = findViewById(R.id.tvName);
        tvPhone          = findViewById(R.id.tvPhone);
        tvEmail          = findViewById(R.id.tvEmail);
        btnBack          = findViewById(R.id.btnBack);
        btnLogout        = findViewById(R.id.btnLogout);
        cardProfileInfo  = findViewById(R.id.cardProfileInfo);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // CORREGIDO: Usando el recurso de strings para el nombre de usuario por defecto
        username = prefs.getString("name", getString(R.string.default_username));
        userId   = prefs.getInt("userId", -1);
        String phone    = prefs.getString("phone", "");
        String email    = prefs.getString("email", "");
        String photoUrl = prefs.getString(KEY_PHOTO, "");

        tvName.setText(username);
        // CORREGIDO: Usando las etiquetas correctas de tu strings.xml
        tvPhone.setText(getString(R.string.profile_label_phone) + " " + phone);
        tvEmail.setText(getString(R.string.profile_label_email) + " "   + email);

        if (!photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.usuario)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imgUserIcon);
        }

        imgUserIcon.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        cardProfileInfo.setOnClickListener(v -> showEditDialog());

        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }

    private void showEditDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);

        final TextInputEditText etName = dialogView.findViewById(R.id.etDialogName);
        final TextInputEditText etEmail = dialogView.findViewById(R.id.etDialogEmail);
        final TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogPhone);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        etName.setText(prefs.getString("name", ""));
        etEmail.setText(prefs.getString("email", ""));
        etPhone.setText(prefs.getString("phone", ""));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.profile_dialog_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.profile_dialog_btn_save), (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();

                    if (newName.isEmpty() || newEmail.isEmpty()) {
                        // CORREGIDO: Usando el recurso de strings
                        Toast.makeText(this, getString(R.string.name_and_email_required), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateProfileOnServer(newName, newPhone, newEmail);
                })
                .setNegativeButton(getString(R.string.profile_dialog_btn_cancel), null)
                .show();
    }

    private void updateProfileOnServer(String name, String phone, String email) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        Call<GenericResponse> call = api.updateProfile(userId, name, email, phone);
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                    e.putString("name", name);
                    e.putString("phone", phone);
                    e.putString("email", email);
                    e.apply();

                    tvName.setText(name);
                    tvPhone.setText(getString(R.string.profile_label_phone) + " " + phone);
                    tvEmail.setText(getString(R.string.profile_label_email) + " " + email);

                    Toast.makeText(ProfileActivity.this, getString(R.string.profile_toast_update_success), Toast.LENGTH_SHORT).show();
                } else {
                    String serverError = resp.body() != null ? resp.body().getMessage() : getString(R.string.profile_server_error);
                    String finalMessage = getString(R.string.profile_update_error) + serverError;
                    Toast.makeText(ProfileActivity.this, finalMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // CORREGIDO: Usando recursos de strings
                String finalMessage = getString(R.string.network_failure) + t.getMessage();
                Toast.makeText(ProfileActivity.this, finalMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openCamera() {
        takePictureLauncher.launch(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        );
    }

    private void uploadImageToServer(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        String b64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.uploadProfileImage(userId, b64)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> c, Response<GenericResponse> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().isSuccess()) {
                            String url = r.body().getUrl();
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .putString(KEY_PHOTO, url)
                                    .apply();
                            Glide.with(ProfileActivity.this)
                                    .load(url)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(imgUserIcon);
                            Toast.makeText(ProfileActivity.this, getString(R.string.profile_toast_image_updated), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> c, Throwable t) {
                    }
                });
    }
}
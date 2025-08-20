package si.uni_lj.fe.tnuv.cafflog3;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editDob, editWeight;

    private FirebaseUser user;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        editEmail = findViewById(R.id.editTextEmail);
        editPassword = findViewById(R.id.editTextPassword);
        editDob = findViewById(R.id.editTextDob);
        editWeight = findViewById(R.id.editTextWeight);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);
        user = FirebaseAuth.getInstance().getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        loadUserData();

        buttonSave.setOnClickListener(v -> saveProfile());
        buttonDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());

        Toolbar toolbar = findViewById(R.id.editProfileToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserData() {

        editEmail.setText(user.getEmail());

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    String dob = task.getResult().child("dateOfBirth").getValue(String.class);
                    Double weight = task.getResult().child("weight").getValue(Double.class);

                    if (dob != null) editDob.setText(dob);
                    if (weight != null) editWeight.setText(String.valueOf(weight));
                }
            }
        });
    }

    private void saveProfile() {
        String newEmail = editEmail.getText().toString().trim();
        String newPassword = editPassword.getText().toString();
        String newDob = editDob.getText().toString().trim();
        String newWeightStr = editWeight.getText().toString().trim();

        if (TextUtils.isEmpty(newEmail)) {
            editEmail.setError("Email cannot be empty");
            return;
        }

        if (!newEmail.equals(user.getEmail())) {
            user.updateEmail(newEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Email updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update email", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (!TextUtils.isEmpty(newPassword)) {
            if (newPassword.length() < 6) {
                editPassword.setError("Password must be at least 6 characters");
                return;
            }

            user.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_LONG).show();
                }
            });
        }

        userRef.child("dateOfBirth").setValue(newDob);

        double weight = 0;
        if (!TextUtils.isEmpty(newWeightStr)) {
            try {
                weight = Double.parseDouble(newWeightStr);
                userRef.child("weight").setValue(weight);
            } catch (NumberFormatException e) {
                editWeight.setError("Invalid weight");
                return;
            }
        }

        if (!TextUtils.isEmpty(newDob) && weight > 0) {
            double dailyLimit = calculateDailyLimit(newDob, weight);
            userRef.child("dailyCaffeineLimit").setValue(dailyLimit);
        }

        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private double calculateDailyLimit(String dob, double weight) {
        int age = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            Date birthDate = sdf.parse(dob);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);

            Calendar today = Calendar.getInstance();
            age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        double baseLimit = weight * 5;
        if (age < 18) baseLimit *= 0.5;
        else if (age > 60) baseLimit *= 0.8;

        return baseLimit;
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void deleteAccount() {
        if (user != null) {
            String uid = user.getUid();

            userRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();

                            Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}


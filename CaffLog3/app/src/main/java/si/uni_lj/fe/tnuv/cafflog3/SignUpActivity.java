package si.uni_lj.fe.tnuv.cafflog3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, dobInput, weightInput;
    private Button signupButton;
    private TextView loginLink;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        databaseUsers = FirebaseDatabase.getInstance().getReference("users");

        emailInput = findViewById(R.id.signupEmail);
        passwordInput = findViewById(R.id.signupPassword);
        dobInput = findViewById(R.id.signupDateOfBirth);
        weightInput = findViewById(R.id.signupWeight);
        signupButton = findViewById(R.id.buttonSignup);
        loginLink = findViewById(R.id.textLoginLink);

        signupButton.setOnClickListener(v -> signUpUser());
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void signUpUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String dob = dobInput.getText().toString().trim();
        String weightStr = weightInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || dob.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double dailyLimit = calculateDailyLimit(dob, weight);

        List<Drink> presetDrinks = Arrays.asList(
                new Drink("Coffee", 95),
                new Drink("Espresso", 64),
                new Drink("Black Tea", 47),
                new Drink("Green Tea", 28),
                new Drink("Energy Drink", 80)
        );

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("dateOfBirth", dob);
                        userData.put("weight", weight);
                        userData.put("dailyCaffeineLimit", dailyLimit);
                        databaseUsers.child(uid).setValue(userData)
                                .addOnSuccessListener(aVoid -> {

                                    DatabaseReference drinksRef = databaseUsers.child(uid).child("drinks");
                                    for (Drink drink : presetDrinks) {
                                        drinksRef.push().setValue(drink);
                                    }

                                    Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "Failed to save user data: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
}



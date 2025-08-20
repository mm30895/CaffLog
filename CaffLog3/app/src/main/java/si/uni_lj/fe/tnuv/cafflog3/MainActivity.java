package si.uni_lj.fe.tnuv.cafflog3;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;
    private ProgressBar progressBar;
    private TextView caffeineText;
    private TextView timeSinceText;

    private Button btnAddDrink;
    private static final double HALF_LIFE_HOURS = 5.0;
    private static final double HALF_LIFE_MINS_TEST = 2.0;
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainToolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        progressBar = findViewById(R.id.caffeineProgressBar);
        caffeineText = findViewById(R.id.caffeineProgressText);
        timeSinceText = findViewById(R.id.timeSinceLastDrink);
        btnAddDrink = findViewById(R.id.buttonAddDrink);

        btnAddDrink.setOnClickListener(v -> showAddDrinkDialog());

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            initUI();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_edit_profile) {
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_edit_drinks) {
            Intent intent = new Intent(MainActivity.this, DrinksActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_logout) {
            Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();

            handler.removeCallbacks(updateTask);

            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initUI() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long dailyLimit = snapshot.child("dailyCaffeineLimit").getValue(Long.class);

                    if (dailyLimit != null) {
                        progressBar.setMax(dailyLimit.intValue());
                        progressBar.setProgress(0);

                        caffeineText.setText("0 / " + dailyLimit + " mg");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showAddDrinkDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_drink, null);

        Spinner spinnerDrinks = dialogView.findViewById(R.id.spinner_drinks);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        DatabaseReference drinksRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("drinks");


        Map<String, Long> drinkMap = new HashMap<>();

        drinksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> drinkNames = new ArrayList<>();

                for (DataSnapshot drinkSnap : snapshot.getChildren()) {
                    String name = drinkSnap.child("name").getValue(String.class);
                    Long caffeine = drinkSnap.child("caffeine").getValue(Long.class);
                    if (name != null && caffeine != null) {
                        String desc = name + " (" + caffeine + " mg)";
                        drinkNames.add(desc);
                        drinkMap.put(desc, caffeine);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item, drinkNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDrinks.setAdapter(adapter);

                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load drinks", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String selectedDrink = (String) spinnerDrinks.getSelectedItem();
            if (selectedDrink != null) {
                long caffeineAmount = drinkMap.get(selectedDrink);
                String selectedName = selectedDrink.split(" \\(")[0];
                addDrinkToUser(selectedName, caffeineAmount);
            }
            dialog.dismiss();
        });
    }


    private void addDrinkToUser(String name, long caffeineValue) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userDrinksRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("activeDrinks");

        String key = userDrinksRef.push().getKey();
        Map<String, Object> drinkData = new HashMap<>();
        drinkData.put("name", name);
        drinkData.put("caffeine", caffeineValue);
        drinkData.put("timeTaken", System.currentTimeMillis());

        userDrinksRef.child(key).setValue(drinkData);
    }


    private double getRemainingCaffeine(long originalMg, long timeTakenMillis) {
        long now = System.currentTimeMillis();
//        double elapsedHours = (now - timeTakenMillis) / (1000.0 * 60 * 60);
//        return originalMg * Math.pow(0.5, elapsedHours / HALF_LIFE_HOURS);

        // ----- TESTING -----
        double elapsedHours = (now - timeTakenMillis) / (1000.0 * 60);
        return originalMg * Math.pow(0.5, elapsedHours / HALF_LIFE_MINS_TEST);
    }

    private void updateUIFromFirebase() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference drinksRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("activeDrinks");

        drinksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalCaffeine = 0;
                Long lastDrinkTime = null;
                List<String> drinksToRemove = new ArrayList<>();

                for (DataSnapshot drinkSnap : snapshot.getChildren()) {
                    Long caffeine = drinkSnap.child("caffeine").getValue(Long.class);
                    Long timeTaken = drinkSnap.child("timeTaken").getValue(Long.class);

                    if (caffeine != null && timeTaken != null) {
                        double remaining = getRemainingCaffeine(caffeine, timeTaken);
                        totalCaffeine += remaining;

                        if (remaining < 1) {
                            drinksToRemove.add(drinkSnap.getKey());
                        }

                        if (lastDrinkTime == null || timeTaken > lastDrinkTime) {
                            lastDrinkTime = timeTaken;
                        }
                    }
                }

                for (String drinkId : drinksToRemove) {
                    drinksRef.child(drinkId).removeValue();
                }

                progressBar.setProgress((int) totalCaffeine);
                caffeineText.setText(String.format(Locale.getDefault(),
                        "%d / %d mg", (int) totalCaffeine, progressBar.getMax()));

                if (lastDrinkTime != null) {
                    long elapsedMillis = System.currentTimeMillis() - lastDrinkTime;
                    long hours = elapsedMillis / (1000 * 60 * 60);
                    long minutes = (elapsedMillis / (1000 * 60)) % 60;
                    long seconds = (elapsedMillis / 1000) % 60;

                    timeSinceText.setText(String.format(Locale.getDefault(),
                            "%s %02d:%02d:%02d", getString(R.string.time_since_last_drink), hours, minutes, seconds));
                } else {
                    timeSinceText.setText(getString(R.string.no_drinks_yet));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            updateUIFromFirebase();
            handler.postDelayed(this, 1000); // every 1 second
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        initUI();
        handler.post(updateTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTask);
    }






}
package si.uni_lj.fe.tnuv.cafflog3;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DrinksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DrinksAdapter adapter;
    private List<Drink> drinksList = new ArrayList<>();

    private DatabaseReference drinksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drinks);

        Toolbar toolbar = findViewById(R.id.drinksToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recyclerView = findViewById(R.id.recyclerViewDrinks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DrinksAdapter(drinksList, this);
        recyclerView.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        drinksRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("drinks");

        loadDrinks();

        FloatingActionButton fab = findViewById(R.id.buttonAddDrink);
        fab.setOnClickListener(v -> showAddDrinkDialog());
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDrinks() {
        drinksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                drinksList.clear();
                for (DataSnapshot drinkSnapshot : snapshot.getChildren()) {
                    Drink drink = drinkSnapshot.getValue(Drink.class);
                    if (drink != null) {
                        drink.setId(drinkSnapshot.getKey());
                        drinksList.add(drink);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DrinksActivity.this, "Failed to load drinks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDrinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Drink");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_drink, null);
        EditText inputName = dialogView.findViewById(R.id.editTextDrinkName);
        EditText inputCaffeine = dialogView.findViewById(R.id.editTextCaffeine);

        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String caffeineStr = inputCaffeine.getText().toString().trim();

            if (name.isEmpty() || caffeineStr.isEmpty()) {
                Toast.makeText(DrinksActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int caffeine;
            try {
                caffeine = Integer.parseInt(caffeineStr);
            } catch (NumberFormatException e) {
                Toast.makeText(DrinksActivity.this, "Caffeine must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            addDrinkToDatabase(new Drink(name, caffeine));
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void addDrinkToDatabase(Drink drink) {
        String key = drinksRef.push().getKey();
        if (key != null) {
            drinksRef.child(key).setValue(drink)
                    .addOnSuccessListener(aVoid -> Toast.makeText(DrinksActivity.this, "Drink added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(DrinksActivity.this, "Failed to add drink", Toast.LENGTH_SHORT).show());
        }
    }

    public void deleteDrink(String drinkId) {
        drinksRef.child(drinkId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(DrinksActivity.this, "Drink deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(DrinksActivity.this, "Failed to delete drink", Toast.LENGTH_SHORT).show());
    }
}

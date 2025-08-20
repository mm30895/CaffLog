package si.uni_lj.fe.tnuv.cafflog3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DrinksAdapter extends RecyclerView.Adapter<DrinksAdapter.DrinkViewHolder> {

    private final List<Drink> drinks;
    private final DrinksActivity activity;

    public DrinksAdapter(List<Drink> drinks, DrinksActivity activity) {
        this.drinks = drinks;
        this.activity = activity;
    }

    @NonNull
    @Override
    public DrinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drink, parent, false);
        return new DrinkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DrinkViewHolder holder, int position) {
        Drink drink = drinks.get(position);
        holder.nameText.setText(drink.getName());
        holder.caffeineText.setText(drink.getCaffeine() + " mg");

        holder.deleteButton.setOnClickListener(v -> {
            String id = drink.getId();
            if (id != null) {
                activity.deleteDrink(id);
            } else {
                Toast.makeText(holder.itemView.getContext(), "Cannot delete this drink", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return drinks.size();
    }

    static class DrinkViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, caffeineText;
        Button deleteButton;

        public DrinkViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textDrinkName);
            caffeineText = itemView.findViewById(R.id.textDrinkCaffeine);
            deleteButton = itemView.findViewById(R.id.buttonDeleteDrink);
        }
    }
}


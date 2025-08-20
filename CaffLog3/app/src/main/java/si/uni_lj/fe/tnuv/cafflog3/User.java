package si.uni_lj.fe.tnuv.cafflog3;

import java.util.List;

public class User {
    public String email;
    public String dateOfBirth;
    public double weight;
    public double dailyCaffeineLimit;
    public double currentCaffeine;
    public List<Drink> drinks;

    public User(String email, String dob, double weight, double limit, List<Drink> drinks) {
        this.email = email;
        this.dateOfBirth = dob;
        this.weight = weight;
        this.dailyCaffeineLimit = limit;
        this.currentCaffeine = 0;
        this.drinks = drinks;
    }
}

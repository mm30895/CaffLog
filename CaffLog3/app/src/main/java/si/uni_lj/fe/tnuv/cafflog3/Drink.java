package si.uni_lj.fe.tnuv.cafflog3;

public class Drink {
    private String id;
    private String name;
    private int caffeine;

    public Drink() {}

    public Drink(String name, int caffeine) {
        this.name = name;
        this.caffeine = caffeine;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCaffeine() { return caffeine; }
    public void setCaffeine(int caffeine) { this.caffeine = caffeine; }
}




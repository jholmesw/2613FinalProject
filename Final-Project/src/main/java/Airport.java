import com.opencsv.bean.CsvBindByName;

public class Airport {
    @CsvBindByName
    private double t;
    @CsvBindByName
    private double o;
    @CsvBindByName
    private double c;

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public double getO() {
        return o;
    }

    public void setO(double o) {
        this.o = o;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public Airport(double t, double o, double c) {
        this.t = t;
        this.o = o;
        this.c = c;
    }

    public Airport() {
        this.t = -99;
        this.c = -99;
        this.o = -99;
    }

    @Override
    public String toString() {
        return "Airport{" +
                "t=" + t +
                ", o=" + o +
                ", c=" + c +
                '}';
    }
}

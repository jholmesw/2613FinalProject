import com.opencsv.bean.CsvBindByName;

public class FlightRoute {
    @CsvBindByName
    private int i;
    @CsvBindByName
    private int j;
    @CsvBindByName
    private int u;
    @CsvBindByName
    private double f;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getJ() {
        return j;
    }

    public void setJ(int j) {
        this.j = j;
    }

    public int getU() {
        return u;
    }

    public void setU(int u) {
        this.u = u;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public FlightRoute(int i, int j, int u, double f) {
        this.i = i;
        this.j = j;
        this.u = u;
        this.f = f;
    }

    public FlightRoute() {
        this.i = -99;
        this.j = -99;
        this.u = -99;
        this.f = -99;
    }

    @Override
    public String toString() {
        return "FlightRoute{" +
                "i=" + i +
                ", j=" + j +
                ", u=" + u +
                ", f=" + f +
                '}';
    }
}

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Solver {
    public static void main(String[] args) throws IOException {
        ArrayList<Airport> airports = readAirportData();
        ArrayList<ArrayList<FlightRoute>> flightRoutes = readFlightData();
        ArrayList<ArrayList<ArrayList<Double>>> tempX = new ArrayList<>();

        //Testing to see if data read in correctly
        System.out.println(airports);
        System.out.println(flightRoutes);

        double obj = -1300;
        int open = 0;

        for (int i = 360; i <= 1020; i+= 60) {
            for (int j = 0; j < airports.size(); j++) {
                airports.get(j).setO(i);
            }
             obj = solveSchedule(airports,flightRoutes,tempX);
            if (obj > 0) {
                open = i;
                break;
            }
            open = i;
        }

        System.out.println(open);
        System.out.println(obj);
        System.out.println(tempX);
    }
    public static ArrayList<Airport> readAirportData() throws FileNotFoundException {
        return (ArrayList<Airport>) new CsvToBeanBuilder<Airport>(new FileReader("data/airport.csv"))
                .withType(Airport.class).build().parse();
    }

    public static ArrayList<ArrayList<FlightRoute>> readFlightData() throws IOException {
        ArrayList<ArrayList<FlightRoute>> flights = new ArrayList<>();

        FileInputStream fis = new FileInputStream("data/flight-route.csv");
        Scanner scnr = new Scanner(fis);

        scnr.nextLine();

        String[] line = scnr.nextLine().split(",");

        int i = 1;
        int j = 1;
        while (i == Integer.parseInt(line[0]) && scnr.hasNext()) {
            ArrayList<FlightRoute> fromI = new ArrayList<>();
            j = 1;
            while (j == Integer.parseInt(line[1]) && scnr.hasNext()) {
                fromI.add(new FlightRoute(
                        Integer.parseInt(line[0]),
                        Integer.parseInt(line[1]),
                        Integer.parseInt(line[2]),
                        Double.parseDouble(line[3])
                ));
                line = scnr.nextLine().split(",");
                j++;
            }
            flights.add(fromI);
            i++;
        }
        //Seeing how to add the final flight
        /*System.out.println(i);
        System.out.println(j);
        System.out.println(Arrays.toString(line));*/

        flights.get(i-2).add(new FlightRoute(Integer.parseInt(line[0]),
                Integer.parseInt(line[1]),
                Integer.parseInt(line[2]),
                Double.parseDouble(line[3])));

        scnr.close();
        fis.close();

        return flights;
    }

    public static double solveSchedule(ArrayList<Airport> airports, ArrayList<ArrayList<FlightRoute>> flightRoutes, ArrayList<ArrayList<ArrayList<Double>>> xVars) {
        Loader.loadNativeLibraries();

        MPSolver solver = MPSolver.createSolver("SCIP");
        //Lists of variables
        List<ArrayList<ArrayList<MPVariable>>> x = new ArrayList<>();
        List<ArrayList<ArrayList<ArrayList<ArrayList<MPVariable>>>>> y = new ArrayList<>();
        List<ArrayList<ArrayList<ArrayList<ArrayList<MPVariable>>>>> a = new ArrayList<>();

        //Parameters and constants
        double negInf = Double.NEGATIVE_INFINITY;
        double posInf = Double.POSITIVE_INFINITY;
        double M = 1440;
        double S = 40;


        // Setting variables //////////////////////////////////////////////////////////////////


        // x_{j,k,i}
        for (int j = 0; j < airports.size(); j++) {
            ArrayList<ArrayList<MPVariable>> temp = new ArrayList<>();
            for (int k = 0; k < airports.size(); k++) {
                ArrayList<MPVariable> temp2 = new ArrayList<>();
                if (flightRoutes.get(j).get(k).getU() != 0) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        temp2.add(solver.makeVar(0, posInf, false, "x_" + j + k + i));
                    }
                    temp.add(temp2);
                } else {
                    temp2.add(solver.makeVar(0,posInf,false,"x_0"));
                    temp.add(temp2);
                }
            }
            x.add(temp);
        }

        // y_{k,l,n,i,j}
        for (int k = 0; k < airports.size(); k++) {
            ArrayList<ArrayList<ArrayList<ArrayList<MPVariable>>>> temp = new ArrayList<>();
            for (int l = 0; l < airports.size(); l++) {
                ArrayList<ArrayList<ArrayList<MPVariable>>> temp2 = new ArrayList<>();
                for (int n = 0; n < airports.size(); n++) {
                    ArrayList<ArrayList<MPVariable>> temp3 = new ArrayList<>();
                    for (int i = 0; i < flightRoutes.get(k).get(l).getU(); i++) {
                        ArrayList<MPVariable> temp4 = new ArrayList<>();
                        if (flightRoutes.get(k).get(n).getU() != 0) {
                            for (int j = 0; j < flightRoutes.get(k).get(n).getU(); j++) {
                                temp4.add(solver.makeIntVar(0, 1, "y_" + k + l + n + i + j));
                            }
                        } else {
                            temp4.add(solver.makeIntVar(0,1,"y_0"));
                        }
                        temp3.add(temp4);
                    }
                    temp2.add(temp3);
                }
                temp.add(temp2);
            }
            y.add(temp);
        }

        // a_{k,l,n,i,j}
        for (int k = 0; k < airports.size(); k++) {
            ArrayList<ArrayList<ArrayList<ArrayList<MPVariable>>>> temp = new ArrayList<>();
            for (int l = 0; l < airports.size(); l++) {
                ArrayList<ArrayList<ArrayList<MPVariable>>> temp2 = new ArrayList<>();
                for (int n = 0; n < airports.size(); n++) {
                    ArrayList<ArrayList<MPVariable>> temp3 = new ArrayList<>();
                    for (int i = 0; i < flightRoutes.get(k).get(l).getU(); i++) {
                        ArrayList<MPVariable> temp4 = new ArrayList<>();
                        if (flightRoutes.get(n).get(l).getU() != 0) {
                            for (int j = 0; j < flightRoutes.get(n).get(l).getU(); j++) {
                                temp4.add(solver.makeIntVar(0,1,"a_" + k + l + n + i + j));
                            }
                        } else {
                            temp4.add(solver.makeIntVar(0,1,"a_0"));
                        }
                        temp3.add(temp4);
                    }
                    temp2.add(temp3);
                }
                temp.add(temp2);
            }
            a.add(temp);
        }

        MPVariable q = solver.makeVar(negInf,posInf,false,"q");


        // Setting Constraints //////////////////////////////////////////////////////////////////////////////////////


        //x[j,k,i] -  x[j,w,z] + M * y[j,k,w,i,z] <= - t[j] + M;
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int w = 0; w < airports.size(); w++) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        for (int z = 0; z < flightRoutes.get(j).get(w).getU(); z++) {
                            if (j!=k && j!=w && (w!=k && i!=z)) {
                                MPConstraint takeoff1 = solver.makeConstraint(negInf, -airports.get(j).getT() + M);
                                takeoff1.setCoefficient(x.get(j).get(k).get(i), 1);
                                takeoff1.setCoefficient(x.get(j).get(w).get(z), -1);
                                takeoff1.setCoefficient(y.get(j).get(k).get(w).get(i).get(z), M);
                            }
                        }
                    }
                }
            }
        }

        //x[j,w,z] -x[j,k,i] - M*y[j,k,w,i,z] <=  - t[j] ;
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int w = 0; w < airports.size(); w++) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        for (int z = 0; z < flightRoutes.get(j).get(w).getU(); z++) {
                            if (j!=k && j!=w && (w!=k && i!=z)) {
                                MPConstraint takeoff2 = solver.makeConstraint(negInf, -airports.get(j).getT());
                                takeoff2.setCoefficient(x.get(j).get(k).get(i), -1);
                                takeoff2.setCoefficient(x.get(j).get(w).get(z), 1);
                                takeoff2.setCoefficient(y.get(j).get(k).get(w).get(i).get(z), -M);
                            }
                        }
                    }
                }
            }
        }

        //x[j,k,i] -x[w,k,z] + M* a[j,k,w,i,z] <=  + t[w] + f[w,k] + M - ( t[j] + f[j,k] + t[k]);
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int w = 0; w < airports.size(); w++) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        for (int z = 0; z < flightRoutes.get(w).get(k).getU(); z++) {
                            if (j!=k && w!=k && (j!=w && i!=z)) {
                                MPConstraint landing1 = solver.makeConstraint(negInf, (airports.get(w).getT() + flightRoutes.get(w).get(k).getF() + M) - airports.get(j).getT() - flightRoutes.get(j).get(k).getF() - airports.get(k).getT());
                                landing1.setCoefficient(x.get(j).get(k).get(i), 1);
                                landing1.setCoefficient(x.get(w).get(k).get(z), -1);
                                landing1.setCoefficient(a.get(j).get(k).get(w).get(i).get(z), M);
                            }
                        }
                    }
                }
            }
        }

        // x[w,k,z] -x[j,k,i] -M*a[j,k,w,i,z] <=  + t[j] + f[j,k] +  - (+ t[w] + f[w,k] + t[k]);
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int w = 0; w < airports.size(); w++) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        for (int z = 0; z < flightRoutes.get(w).get(k).getU(); z++) {
                            if (j!=k && w!=k && (j!=w && i!=z)) {
                                MPConstraint landing2 = solver.makeConstraint(negInf, -(airports.get(w).getT() + flightRoutes.get(w).get(k).getF() + airports.get(k).getT()) + airports.get(j).getT() + flightRoutes.get(j).get(k).getF());
                                landing2.setCoefficient(x.get(j).get(k).get(i), -1);
                                landing2.setCoefficient(x.get(w).get(k).get(z), 1);
                                landing2.setCoefficient(a.get(j).get(k).get(w).get(i).get(z), -M);
                            }
                        }
                    }
                }
            }
        }


        // x[j,k,i] - x[j,k,z] + M * y[j,k,k,i,z] <= -S + M;
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int w = 0; w < airports.size(); w++) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        for (int z = 0; z < flightRoutes.get(j).get(k).getU(); z++) {
                            if (i!=z && j!=k) {
                                MPConstraint sameRoute1 = solver.makeConstraint(negInf, -S + M);
                                sameRoute1.setCoefficient(x.get(j).get(k).get(i), 1);
                                sameRoute1.setCoefficient(x.get(j).get(k).get(z), -1);
                                sameRoute1.setCoefficient(y.get(j).get(k).get(k).get(i).get(z), M);
                            }
                        }
                    }
                }
            }
        }

        // x[j,k,z] - x[j,k,i] - M*y[j,k,k,i,z] <= -S;
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int w = 0; w < airports.size(); w++) {
                    for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                        for (int z = 0; z < flightRoutes.get(j).get(k).getU(); z++) {
                            if (i!=z && j!=k) {
                                MPConstraint sameRoute1 = solver.makeConstraint(negInf, -S);
                                sameRoute1.setCoefficient(x.get(j).get(k).get(i), -1);
                                sameRoute1.setCoefficient(x.get(j).get(k).get(z), 1);
                                sameRoute1.setCoefficient(y.get(j).get(k).get(k).get(i).get(z), -M);
                            }
                        }
                    }
                }
            }
        }

        //x[j,k,i] >= o[j];
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                    if (j!=k) {
                        MPConstraint open = solver.makeConstraint(airports.get(j).getO(), posInf);
                        open.setCoefficient(x.get(j).get(k).get(i),1);
                    }
                }
            }
        }

        //x[j,k,i] <= c[j];
        for (int j = 0; j < airports.size(); j++) {
            for (int k = 0; k < airports.size(); k++) {
                for (int i = 0; i < flightRoutes.get(j).get(k).getU(); i++) {
                    if (j!=k) {
                        MPConstraint close = solver.makeConstraint(negInf, airports.get(j).getC());
                        close.setCoefficient(x.get(j).get(k).get(i),1);
                    }
                }
            }
        }


        //q -x[j, 1, i] >=   t[1] + t[j] + f[j, 1];
        for (int j = 1; j < airports.size(); j++) {
            for (int i = 0; i < flightRoutes.get(j).get(0).getU(); i++) {
                MPConstraint linking = solver.makeConstraint(airports.get(0).getT() + airports.get(j).getT() + flightRoutes.get(j).get(0).getF(),posInf);
                linking.setCoefficient(q,1);
                linking.setCoefficient(x.get(j).get(0).get(i), -1);
            }
        }


        // Setting objective //////////////////////////////////////////////////////////////////////////////////////////


        MPObjective latePenalty = solver.objective();

        latePenalty.setCoefficient(q, 1);
        latePenalty.setOffset(-airports.get(1).getC());

        latePenalty.setMinimization();

        solver.enableOutput();
        solver.setTimeLimit(10*1000);

        //Call solver
        final MPSolver.ResultStatus resultStatus = solver.solve();

        if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {

            for (int i = 0; i < x.size(); i++) {
                ArrayList<ArrayList<Double>> temp = new ArrayList<>();
                for (int j = 0; j < x.get(i).size(); j++) {
                    ArrayList<Double> temp2 = new ArrayList<>();
                    for (int k = 0; k < x.get(i).get(j).size(); k++) {
                        temp2.add(x.get(i).get(j).get(k).solutionValue());
                    }
                    temp.add(temp2);
                }
                xVars.add(temp);
            }
            return latePenalty.value();

        } else {
            System.out.println(resultStatus);
            for (int i = 0; i < x.size(); i++) {
                ArrayList<ArrayList<Double>> temp = new ArrayList<>();
                for (int j = 0; j < x.get(i).size(); j++) {
                    ArrayList<Double> temp2 = new ArrayList<>();
                    for (int k = 0; k < x.get(i).get(j).size(); k++) {
                       temp2.add(x.get(i).get(j).get(k).solutionValue());
                    }
                    temp.add(temp2);
                }
                xVars.add(temp);
            }

            return latePenalty.value();

        }

    }
}

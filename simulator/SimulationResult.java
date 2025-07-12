package simulator;

import java.util.List;

public class SimulationResult {
    private String log;
    private List<Double> tellerUtilizations;

    public SimulationResult(String log, List<Double> tellerUtilizations) {
        this.log = log;
        this.tellerUtilizations = tellerUtilizations;
    }

    public String getLog() {
        return log;
    }

    public List<Double> getTellerUtilizations() {
        return tellerUtilizations;
    }
} 
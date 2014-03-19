package net.sf.cpsolver.itc.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;

public abstract class ItcParameterWeightOscillation<V extends Variable<V, T>, T extends Value<V, T>> extends Extension<V, T> implements SolutionListener<V, T> {
    public long iUpdateInterval = 5000;
    public long iOscillationInterval = 100*iUpdateInterval;
    public long iLastUpdateIteration = 0;
    public long iLastImprovementIteration = 0;
    public int iLastImprovementWeight = 1;
    public int iWeight = 1;
    public int iWeightMin = 1;
    public int iWeightMax = 1000;
    public int iWeightInc = 1;
    public double iWeightOscillationCoef = 0.5;
    private double iCurrentValue = 0;
    private double iBestValue = 0;
    private List<OscillationListener<V,T>> iOscillationListeners = new ArrayList<OscillationListener<V,T>>();
    
    public ItcParameterWeightOscillation(Solver<V,T> solver, DataProperties properties) {
        super(solver, properties);
        iOscillationInterval = properties.getPropertyLong("Oscillation.OscillationInterval", iOscillationInterval);
        iUpdateInterval = properties.getPropertyLong("Oscillation.UpdateInterval", iUpdateInterval);
        iWeight = properties.getPropertyInt("Oscillation.Weight", iWeight);
        iWeightMin = properties.getPropertyInt("Oscillation.WeightMin", iWeightMin);
        iWeightMax = properties.getPropertyInt("Oscillation.WeightMax", iWeightMax);
        iWeightInc = properties.getPropertyInt("Oscillation.WeightInc", iWeightInc);
        iWeightOscillationCoef = properties.getPropertyDouble("Oscillation.WeightOscillationCoef", iWeightOscillationCoef);
    }
    
    public void register(Model<V,T> model) {
        super.register(model);
        getSolver().currentSolution().addSolutionListener(this);
    }
    
    public void unregister(Model<V,T> model) {
        super.unregister(model);
        getSolver().currentSolution().removeSolutionListener(this);
    }
    
    public boolean init(Solver<V,T> solver) {
        iCurrentValue = iBestValue = currentValue(solver.currentSolution().getAssignment());
        weightChanged(getWeight(),getWeight());
        return super.init(solver);
    }

    public void incWeight() {
        int oldWeight = iWeight;
        iWeight = Math.min(iWeight+iWeightInc, iWeightMax);
        weightChanged(oldWeight, iWeight);
        //sLog.info("W++ "+iWeight);
    }
    
    public void decWeight() {
        int oldWeight = iWeight;
        iWeight = Math.max((int)Math.round(iWeightOscillationCoef*iLastImprovementWeight), iWeightMin);
        weightChanged(oldWeight, iWeight);
        //sLog.info("W-- "+iWeight);
    }
    
    public int getWeight() {
        return iWeight;
    }
    
    public abstract double currentValue(Assignment<V, T> assignment);
    public abstract void changeWeight(double weight);
    
    public boolean isImproving(Solution<V,T> solution) {
        if (solution.getModel().getBestUnassignedVariables()!=0 && solution.getAssignment().nrAssignedVariables() == solution.getModel().variables().size()) {
            iCurrentValue = currentValue(solution.getAssignment());
            return true;
        }
        if (currentValue(solution.getAssignment())<iCurrentValue) {
            iCurrentValue = currentValue(solution.getAssignment());
            return true;
        }
        return false;
    }
    
    public void weightChanged(int oldWeight, int newWeight) {
        getSolver().currentSolution().getModel().setBestValue(getSolver().currentSolution().getModel().getBestValue() + (newWeight-oldWeight) * iBestValue);
        for (OscillationListener<V,T> listener: iOscillationListeners)
            listener.bestValueChanged(getSolver().currentSolution(), (newWeight-oldWeight) * iBestValue);
        changeWeight(newWeight);
    }
    
    public void solutionUpdated(Solution<V,T> solution) {
        if (isImproving(solution)) {
            iLastImprovementIteration = solution.getIteration();
            iLastImprovementWeight = iWeight;
            //sLog.info("W imp");
        }
        if (solution.getIteration()>=iLastUpdateIteration+iUpdateInterval) {
            incWeight();
            iLastUpdateIteration = solution.getIteration();
        }
        if (iOscillationInterval>0 && solution.getIteration()>=iLastImprovementIteration+iOscillationInterval) {
            decWeight();
            iLastImprovementIteration = solution.getIteration();
        }
    }
    
    public void getInfo(Solution<V,T> solution, Map<String, String> info) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info, Collection<V> variables) {}
    public void bestCleared(Solution<V,T> solution) {}
    public void bestSaved(Solution<V,T> solution) {
        iBestValue = currentValue(solution.getAssignment());
    }
    public void bestRestored(Solution<V,T> solution) {}
    
    public void addOscillationListener(OscillationListener<V,T> listener) {
        iOscillationListeners.add(listener);
    }
    
    public void removeOscillationListener(OscillationListener<V,T> listener) {
        iOscillationListeners.remove(listener);
    }
    
    public static interface OscillationListener<V extends Variable<V, T>, T extends Value<V, T>> {
        public void bestValueChanged(Solution<V,T> solution, double delta);
    }
}
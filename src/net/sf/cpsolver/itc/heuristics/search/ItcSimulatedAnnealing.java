package net.sf.cpsolver.itc.heuristics.search;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour.LazyNeighbourAcceptanceCriterion;
import net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcRandomMove;
import net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcSwapMove;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.NeighbourSelector;

import org.apache.log4j.Logger;

/**
 * Simulated annealing algorithm. A move is accepted with a probability
 * that is based on the current temperature.
 * <br><br>
 * The search is guided by the temperature, which starts at <i>SimulatedAnnealing.InitialTemperature</i>. 
 * After each <i>SimulatedAnnealing.TemperatureLength</i> (multiplied by the sum of domain sizes of 
 * all variables) iterations, the temperature is reduced 
 * by <i>SimulatedAnnealing.CoolingRate</i>. If there was no improvement in the past 
 * <i>SimulatedAnnealing.ReheatLengthCoef * SimulatedAnnealing.TemperatureLength</i> iterations, 
 * the temperature is increased by <i>SimulatedAnnealing.ReheatRate</i>.
 * If there was no improvement in the past 
 * <i>SimulatedAnnealing.RestoreBestLengthCoef * SimulatedAnnealing.TemperatureLength</i> iterations,
 * the best ever found solution is restored.
 * <br><br>
 * If <i>SimulatedAnnealing.StochasticHC</i> is true, the acceptance probability is computed using
 * stochastic hill climbing criterion, i.e., <code>1.0 / (1.0 + Math.exp(value/temperature))</code>,
 * otherwise it is cumputed using simlated annealing criterion, i.e.,
 * <code>(value&lt;=0.0?1.0:Math.exp(-value/temperature))</code>.
 * If <i>SimulatedAnnealing.RelativeAcceptance</i> neighbour value {@link ExamSimpleNeighbour#value(Assignment)} is taken
 * as the value of the selected neighbour (difference between the new and the current solution, if the
 * neighbour is accepted), otherwise the value is computed as the difference
 * between the value of the current solution if the neighbour is accepted and the best ever found solution.
 * <br><br>
 * Custom neighbours can be set using SimulatedAnnealing.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:<br>
 * <code>
 * SimulatedAnnealing.Neighbours=net.sf.cpsolver.itc.tim.neighbours.TimRoomMove;net.sf.cpsolver.itc.tim.neighbours.TimTimeMove;net.sf.cpsolver.itc.tim.neighbours.TimSwapMove;net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcSwapMove;net.sf.cpsolver.itc.tim.neighbours.TimPrecedenceMove@0.1
 * </code>
 * <br>
 * Selector TimPrecedenceMove is 10&times; less probable to be selected than other selectors.
 * When SimulatedAnnealing.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When Itc.NextHeuristicsOnReheat parameter is true, a chance is given to another
 * search strategy by returning once null in {@link ItcSimulatedAnnealing#selectNeighbour(Solution)}.
 * <br><br>
 * When SimulatedAnnealing.Update is true, {@link NeighbourSelector#update(Assignment, Neighbour, long)} is called 
 * after each iteration (on the selector that was used) and roulette wheel selection 
 * that is using {@link NeighbourSelector#getPoints()} is used to pick a selector in each iteration. 
 * See {@link NeighbourSelector} for more details. 
 * 
 *  
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not see
 * <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class ItcSimulatedAnnealing<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,ItcSimulatedAnnealing<V,T>.Context> implements SolutionListener<V,T>, LazyNeighbourAcceptanceCriterion<V,T> {
    private static Logger sLog = Logger.getLogger(ItcSimulatedAnnealing.class);
    private static boolean sInfo = sLog.isInfoEnabled();
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private static DecimalFormat sDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = 1.5;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;   
    private long iTemperatureLength = 0;
    private long iReheatLength = 0;
    private long iRestoreBestLength = 0;
    private double iTempLengthCoef = 10.0;
    private double iReheatLengthCoef = 10.0;
    private double iRestoreBestLengthCoef = -1;
    private boolean iStochasticHC = false;
    private int iMoves = 0;
    private double iAbsValue = 0;
    private boolean iNextHeuristicsOnReheat = false;
    
    private boolean iRelativeAcceptance = true;
    
    private List<NeighbourSelector<V,T>> iNeighbourSelectors = new ArrayList<NeighbourSelector<V,T>>();
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;
    
    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>SimulatedAnnealing.InitialTemperature ... initial temperature (default 1.5)
     * <li>SimulatedAnnealing.TemperatureLength ... temperature length (number of iterations between temperature decrements, default 25000)
     * <li>SimulatedAnnealing.CoolingRate ... temperature cooling rate (default 0.95)
     * <li>SimulatedAnnealing.ReheatLengthCoef ... temperature re-heat length coefficient (multiple of temperature length, default 5)
     * <li>SimulatedAnnealing.ReheatRate ... temperature re-heating rate (default (1/coolingRate)^(reheatLengthCoef*1.7))
     * <li>SimulatedAnnealing.RestoreBestLengthCoef ... restore best length coefficient (multiple of re-heat length, default reheatLengthCoef^2)
     * <li>SimulatedAnnealing.StochasticHC ... true for stochastic search acceptance criterion, false for simulated annealing acceptance (default false)
     * <li>SimulatedAnnealing.RelativeAcceptance ... true for relative acceptance (different between the new and the current solutions, if the neighbour is accepted), false for absolute acceptance (difference between the new and the best solutions, if the neighbour is accepted)
     * <li>SimulatedAnnealing.Neighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>SimulatedAnnealing.Random ... when true, a neighbour selector is selected randomly
     * <li>SimulatedAnnealing.Update ... when true, a neighbour selector is selected using {@link ItcHillClimber.NeighbourSelector#getPoints()} weights (roulette wheel selection)
     * <li>Itc.NextHeuristicsOnReheat ... when true, null is returned in {@link ItcSimulatedAnnealing#selectNeighbour(Solution)} once just after a reheat
     * </ul>
     * @param properties problem properties
     */
    public ItcSimulatedAnnealing(DataProperties properties) throws Exception {
        iInitialTemperature = properties.getPropertyDouble("SimulatedAnnealing.InitialTemperature", iInitialTemperature);
        iReheatRate = properties.getPropertyDouble("SimulatedAnnealing.ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble("SimulatedAnnealing.CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean("SimulatedAnnealing.RelativeAcceptance", iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean("SimulatedAnnealing.StochasticHC", iStochasticHC);
        iTempLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.TempLengthCoef", iTempLengthCoef);
        iReheatLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.ReheatLengthCoef", iReheatLengthCoef);
        iRestoreBestLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.RestoreBestLengthCoef", iRestoreBestLengthCoef);
        if (iReheatRate<0) iReheatRate = Math.pow(1/iCoolingRate,iReheatLengthCoef*1.7);
        if (iRestoreBestLengthCoef<0) iRestoreBestLengthCoef = iReheatLengthCoef * iReheatLengthCoef;
        iNextHeuristicsOnReheat = properties.getPropertyBoolean("Itc.NextHeuristicsOnReheat", iNextHeuristicsOnReheat);
        iRandomSelection = properties.getPropertyBoolean("SimulatedAnnealing.Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean("SimulatedAnnealing.Update", iUpdatePoints);
        String neighbours = properties.getProperty("SimulatedAnnealing.Neighbours",
                ItcSwapMove.class.getName()+"@1;"+
                ItcRandomMove.class.getName()+"@1");
        for (StringTokenizer s=new StringTokenizer(neighbours,";");s.hasMoreTokens();) {
            String nsClassName = s.nextToken();
            double bonus = 1.0;
            if (nsClassName.indexOf('@')>=0) {
                bonus = Double.parseDouble(nsClassName.substring(nsClassName.indexOf('@')+1));
                nsClassName = nsClassName.substring(0, nsClassName.indexOf('@'));
            }
            Class<?> nsClass = Class.forName(nsClassName);
            @SuppressWarnings("unchecked")
			NeighbourSelection<V,T> ns = (NeighbourSelection<V,T>)nsClass.getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[]{properties});
            addNeighbourSelection(ns,bonus);
        }
    }
    
    /** Initialization */
    public void init(Solver<V,T> solver) {
    	super.init(solver);
        long tl = getTemperatureLength(solver);
        iTemperatureLength = Math.round(iTempLengthCoef*tl);
        iReheatLength = Math.round(iReheatLengthCoef*iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef*iTemperatureLength);
        solver.currentSolution().addSolutionListener(this);
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbourSelectors) {
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }
    
    private void addNeighbourSelection(NeighbourSelection<V,T> ns, double bonus) {
        iNeighbourSelectors.add(new NeighbourSelector<V,T>(ns, bonus, iUpdatePoints));
    }
    
    private long getTemperatureLength(Solver<V,T> solver) {
        long len = 0;
        for (V variable: solver.currentSolution().getModel().variables())
            len += variable.values(solver.currentSolution().getAssignment()).size();
        sLog.info("Temperature length "+len);
        return len;
    }

    private double totalPoints() {
        if (!iUpdatePoints) return iTotalBonus;
        double total = 0;
        for (NeighbourSelector<V,T> ns: iNeighbourSelectors)
            total += ns.getPoints();
        return total;
    }

    private Neighbour<V,T> genMove(Solution<V,T> solution) {
    	Context context = getContext(solution.getAssignment());
        while (true) {
            if (context.incIter(solution)) return null;
            NeighbourSelector<V,T> ns = null;
            if (iRandomSelection) {
                ns = ToolBox.random(iNeighbourSelectors);
            } else {
                double points = (ToolBox.random()*totalPoints());
                for (Iterator<NeighbourSelector<V,T>> i = iNeighbourSelectors.iterator(); i.hasNext(); ) {
                    ns = i.next();
                    points -= (iUpdatePoints?ns.getPoints():ns.getBonus());
                    if (points<=0) break;
                }
            }
            Neighbour<V,T> n = ns.selectNeighbour(solution);
            if (n!=null) return n;
        }
    }
    
    private boolean accept(Solution<V,T> solution, Neighbour<V,T> neighbour) {
        if (neighbour instanceof ItcLazyNeighbour) {
            ((ItcLazyNeighbour<V,T>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        Model<V,T> m = solution.getModel();
        Assignment<V, T> a = solution.getAssignment();
        double value = (iRelativeAcceptance?neighbour.value(a):m.getTotalValue(a)+neighbour.value(a)-m.getBestValue());
        double prob = getContext(solution.getAssignment()).prob(value);
        if (prob>=1.0 || ToolBox.random()<prob) {
            if (sInfo) getContext(solution.getAssignment()).accepted(neighbour.value(a)); 
            return true;
        }
        return false;
    }
    
    /** Implementation of {@link net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour.LazyNeighbourAcceptanceCriterion} interface */
    public boolean accept(Assignment<V,T> assignment, ItcLazyNeighbour<V,T> neighbour, double value) {
        double prob = getContext(assignment).prob(value);
        if (prob>=1.0 || ToolBox.random()<prob) {
        	if (sInfo) getContext(assignment).accepted(value);
            return true;
        }
        return false;
    }
    
    /** Neighbour selection */
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        Neighbour<V,T> neighbour = null;
        while ((neighbour=genMove(solution))!=null) {
            if (sInfo) {
                iMoves++; iAbsValue += neighbour.value(solution.getAssignment()) * neighbour.value(solution.getAssignment());
            }
            if (accept(solution,neighbour)) break;
        }
        return (neighbour==null?null:neighbour);
    }
    
    public void bestSaved(Solution<V,T> solution) {
    	getContext(solution.getAssignment()).bestSaved(solution.getModel());
    }
    public void solutionUpdated(Solution<V,T> solution) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info, Collection<V> variables) {}
    public void bestCleared(Solution<V,T> solution) {}
    public void bestRestored(Solution<V,T> solution){}

    public class Context implements AssignmentContext {
        private double iTemperature = 0.0;
        private long iT0 = -1;
        private long iIter = 0;
        private long iLastImprovingIter = 0;
        private long iLastReheatIter = 0;
        private long iLastCoolingIter = 0;
        private int iAcceptIter[] = new int[] {0,0,0};

        private void cool(Solution<V,T> solution) {
            iTemperature *= iCoolingRate;
            if (sInfo) {
                sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
                sLog.info("Temperature decreased to "+sDF5.format(iTemperature)+" " +
            		"(#moves="+iMoves+", rms(value)="+sDF2.format(Math.sqrt(iAbsValue/iMoves))+", "+
            		"accept=-"+sDF2.format(100.0*iAcceptIter[0]/iTemperatureLength)+"/"+sDF2.format(100.0*iAcceptIter[1]/iTemperatureLength)+"/+"+sDF2.format(100.0*iAcceptIter[2]/iTemperatureLength)+"%, " +
            		(prob(-1)<1.0?"p(-1)="+sDF2.format(100.0*prob(-1))+"%, ":"")+
            		"p(+1)="+sDF2.format(100.0*prob(1))+"%, "+
            		"p(+10)="+sDF5.format(100.0*prob(10))+"%)");
                if (iUpdatePoints)
                    for (NeighbourSelector<V,T> ns: iNeighbourSelectors)
                        sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
                iAcceptIter=new int[] {0,0,0};
                iMoves = 0; iAbsValue = 0;
            }
            iLastCoolingIter=iIter;
        }
        
        private void reheat(Solution<V,T> solution) {
            iTemperature *= iReheatRate;
            if (sInfo) {
                sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
                sLog.info("Temperature increased to "+sDF5.format(iTemperature)+" "+
                    (prob(-1)<1.0?"p(-1)="+sDF2.format(100.0*prob(-1))+"%, ":"")+
                    "p(+1)="+sDF2.format(100.0*prob(1))+"%, "+
                    "p(+10)="+sDF5.format(100.0*prob(10))+"%, "+
                    "p(+100)="+sDF10.format(100.0*prob(100))+"%)");
            }
            iLastReheatIter=iIter;
        }
        
        private boolean incIter(Solution<V,T> solution) {
            if (iT0 < 0) {
            	iT0 = System.currentTimeMillis();
            	iTemperature = iInitialTemperature;
            }
            iIter++;
            if (iIter > iLastImprovingIter + iRestoreBestLength) {
            	restoreBest(solution);
            }
            if (iIter > Math.max(iLastReheatIter, iLastImprovingIter) + iReheatLength) {
                reheat(solution);
                return iNextHeuristicsOnReheat;
            }
            if (iIter > iLastCoolingIter + iTemperatureLength) cool(solution);
            return false;
        }
        
        private void restoreBest(Solution<V,T> solution) {
            iLastImprovingIter = iIter;
            solution.restoreBest();
        }

        private double prob(double value) {
            if (iStochasticHC)
                return 1.0 / (1.0 + Math.exp(value/iTemperature));
            else
                return (value<=0.0?1.0:Math.exp(-value/iTemperature));
        }
        
        private void accepted(double value) {
        	iAcceptIter[value < 0.0 ? 0 : value > 0.0 ? 2 : 1]++;
        }
        
        private void bestSaved(Model<V, T> model) {
        	iLastImprovingIter = iIter;
        }
    }

	@Override
	public Context createAssignmentContext(Assignment<V, T> assignment) {
		return new Context();
	}
}

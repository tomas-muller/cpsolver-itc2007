package net.sf.cpsolver.itc.heuristics.search;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
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
 * <code>(value<=0.0?1.0:Math.exp(-value/temperature))</code>.
 * If <i>SimulatedAnnealing.RelativeAcceptance</i> neighbour value {@link ExamSimpleNeighbour#value()} is taken
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
 * When SimulatedAnnealing.Update is true, {@link NeighbourSelector#update(Neighbour, long)} is called 
 * after each iteration (on the selector that was used) and roulette wheel selection 
 * that is using {@link NeighbourSelector#getPoints()} is used to pick a selector in each iteration. 
 * See {@link NeighbourSelector} for more details. 
 * 
 *  
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public class ItcSimulatedAnnealing implements NeighbourSelection, SolutionListener, LazyNeighbourAcceptanceCriterion {
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
    private double iTemperature = 0.0;
    private double iTempLengthCoef = 10.0;
    private double iReheatLengthCoef = 10.0;
    private double iRestoreBestLengthCoef = -1;
    private long iIter = 0;
    private long iLastImprovingIter = 0;
    private long iLastReheatIter = 0;
    private long iLastCoolingIter = 0;
    private int iAcceptIter[] = new int[] {0,0,0};
    private boolean iStochasticHC = false;
    private int iMoves = 0;
    private double iAbsValue = 0;
    private long iT0 = -1;
    private boolean iNextHeuristicsOnReheat = false;
    
    private boolean iRelativeAcceptance = true;
    
    private Vector iNeighbourSelectors = new Vector();
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
            Class nsClass = Class.forName(nsClassName);
            NeighbourSelection ns = (NeighbourSelection)nsClass.getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[]{properties});
            addNeighbourSelection(ns,bonus);
        }
    }
    
    /** Initialization */
    public void init(Solver solver) {
        iTemperature = iInitialTemperature;
        long tl = getTemperatureLength(solver);
        iTemperatureLength = Math.round(iTempLengthCoef*tl);
        iReheatLength = Math.round(iReheatLengthCoef*iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef*iTemperatureLength);
        solver.currentSolution().addSolutionListener(this);
        iTotalBonus = 0;
        for (Enumeration e=iNeighbourSelectors.elements();e.hasMoreElements();) {
            NeighbourSelector s = (NeighbourSelector)e.nextElement();
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }
    
    private void addNeighbourSelection(NeighbourSelection ns, double bonus) {
        iNeighbourSelectors.add(new NeighbourSelector(ns, bonus, iUpdatePoints));
    }
    
    private long getTemperatureLength(Solver solver) {
        long len = 0;
        for (Enumeration e=solver.currentSolution().getModel().variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            len += variable.values().size();
        }
        sLog.info("Temperature length "+len);
        return len;
    }

    private double totalPoints() {
        if (!iUpdatePoints) return iTotalBonus;
        double total = 0;
        for (Enumeration e=iNeighbourSelectors.elements();e.hasMoreElements();) {
            NeighbourSelector ns = (NeighbourSelector)e.nextElement();
            total += ns.getPoints();
        }
        return total;
    }

    private void cool(Solution solution) {
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
                for (Enumeration e=iNeighbourSelectors.elements();e.hasMoreElements();) {
                    NeighbourSelector ns = (NeighbourSelector)e.nextElement();
                    sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
                }
            iAcceptIter=new int[] {0,0,0};
            iMoves = 0; iAbsValue = 0;
        }
        iLastCoolingIter=iIter;
    }
    
    private void reheat(Solution solution) {
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
    
    private void restoreBest(Solution solution) {
        sLog.info("Best restored");
        iLastImprovingIter=iIter;
    }
    
    private Neighbour genMove(Solution solution) {
        while (true) {
            if (incIter(solution)) return null;
            NeighbourSelector ns = null;
            if (iRandomSelection) {
                ns = (NeighbourSelector)ToolBox.random(iNeighbourSelectors);
            } else {
                double points = (ToolBox.random()*totalPoints());
                for (Enumeration e=iNeighbourSelectors.elements();e.hasMoreElements();) {
                    ns = (NeighbourSelector)e.nextElement();
                    points -= (iUpdatePoints?ns.getPoints():ns.getBonus());
                    if (points<=0) break;
                }
            }
            Neighbour n = ns.selectNeighbour(solution);
            if (n!=null) return n;
        }
    }
    
    private double prob(double value) {
        if (iStochasticHC)
            return 1.0 / (1.0 + Math.exp(value/iTemperature));
        else
            return (value<=0.0?1.0:Math.exp(-value/iTemperature));
    }
    
    private boolean accept(Solution solution, Neighbour neighbour) {
        if (neighbour instanceof ItcLazyNeighbour) {
            ((ItcLazyNeighbour)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        double value = (iRelativeAcceptance?neighbour.value():solution.getModel().getTotalValue()+neighbour.value()-solution.getBestValue());
        double prob = prob(value);
        if (prob>=1.0 || ToolBox.random()<prob) {
            if (sInfo) iAcceptIter[neighbour.value()<0.0?0:neighbour.value()>0.0?2:1]++;
            return true;
        }
        return false;
    }
    
    /** Implementation of {@link net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour.LazyNeighbourAcceptanceCriterion} interface */
    public boolean accept(ItcLazyNeighbour neighbour, double value) {
        double prob = prob(value);
        if (prob>=1.0 || ToolBox.random()<prob) {
            if (sInfo) iAcceptIter[value<0.0?0:value>0.0?2:1]++;
            return true;
        }
        return false;
    }
    
    private boolean incIter(Solution solution) {
        if (iT0<0) iT0 = System.currentTimeMillis();
        iIter++;
        if (iIter>iLastImprovingIter+iRestoreBestLength) restoreBest(solution);
        if (iIter>Math.max(iLastReheatIter,iLastImprovingIter)+iReheatLength) {
            reheat(solution);
            return iNextHeuristicsOnReheat;
        }
        if (iIter>iLastCoolingIter+iTemperatureLength) cool(solution);
        return false;
    }
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        Neighbour neighbour = null;
        while ((neighbour=genMove(solution))!=null) {
            if (sInfo) {
                iMoves++; iAbsValue += neighbour.value() * neighbour.value();
            }
            if (accept(solution,neighbour)) break;
        }
        return (neighbour==null?null:neighbour);
    }
    
    public void bestSaved(Solution solution) {
        iLastImprovingIter = iIter;
    }
    public void solutionUpdated(Solution solution) {}
    public void getInfo(Solution solution, java.util.Dictionary info) {}
    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
    public void bestCleared(Solution solution) {}
    public void bestRestored(Solution solution){}    
    
}

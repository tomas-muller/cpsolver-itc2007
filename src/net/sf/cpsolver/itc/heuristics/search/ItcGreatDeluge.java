package net.sf.cpsolver.itc.heuristics.search;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.extension.Extension;
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
import net.sf.cpsolver.itc.heuristics.ItcParameterWeightOscillation;
import net.sf.cpsolver.itc.heuristics.ItcParameterWeightOscillation.OscillationListener;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour.LazyNeighbourAcceptanceCriterion;
import net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcNotConflictingMove;
import net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcSwapMove;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.NeighbourSelector;

/**
 * Great deluge algorithm. A move is accepted if the 
 * overall solution value of the new solution will not exceed 
 * given bound. 
 * <br><br>
 * Bound is initialized to value GreatDeluge.UpperBoundRate &times; value
 * of the best solution. Bound is decreased after each iteration,
 * it is multiplied by GreatDeluge.CoolRate (alternatively, GreatDeluge.CoolRateInv can be 
 * defined, which is GreatDeluge.CoolRate = 1 - (1 / GreatDeluge.CoolRateInv ) ). When
 * a limit GreatDeluge.LowerBoundRate &times; value of the best solution
 * is reached, the bound is increased back to GreatDeluge.UpperBoundRate &times;
 * value of the best solution.
 * <br><br>
 * If there was no improvement found between the increments of the bound, the new bound is changed to
 * GreatDeluge.UpperBoundRate^2 with the lower limit set to GreatDeluge.LowerBoundRate^2,
 * GreatDeluge.UpperBoundRate^3 and GreatDeluge.LowerBoundRate^3, etc. till there is an
 * improvement found.
 * <br><br>
 * Custom neighbours can be set using GreatDeluge.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:<br>
 * <code>
 * GreatDeluge.Neighbours=net.sf.cpsolver.itc.tim.neighbours.TimRoomMove;net.sf.cpsolver.itc.tim.neighbours.TimTimeMove;net.sf.cpsolver.itc.tim.neighbours.TimSwapMove;net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcSwapMove;net.sf.cpsolver.itc.tim.neighbours.TimPrecedenceMove@0.1
 * </code>
 * <br>
 * Selector TimPrecedenceMove is 10&times; less probable to be selected than other selectors.
 * When GreatDeluge.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When Itc.NextHeuristicsOnReheat parameter is true, a chance is given to another
 * search strategy by returning once null in {@link ItcGreatDeluge#selectNeighbour(Solution)}.
 * When Itc.NextHeuristicsOnReheat.AlterBound is also true, the bound is altered after the null
 * is returned (so that an improvement of the best solution made by some other search
 * strategy is considered as well).
 * <br><br>
 * When GreatDeluge.Update is true, {@link NeighbourSelector#update(Assignment, Neighbour, long)} is called 
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
public class ItcGreatDeluge<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,ItcGreatDeluge<V,T>.Context> implements SolutionListener<V,T>, LazyNeighbourAcceptanceCriterion<V,T>, OscillationListener<V, T> {
    private static Logger sLog = Logger.getLogger(ItcGreatDeluge.class);
    private static boolean sInfo = sLog.isInfoEnabled();
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private double iCoolRate = 0.9999995;
    private double iUpperBoundRate = 1.05;
    private double iLowerBoundRate = 0.97;
    private boolean iNextHeuristicsOnReheat = false;
    
    private List<NeighbourSelector<V,T>> iNeighbourSelectors = new ArrayList<NeighbourSelector<V,T>>();
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;
    private boolean iAlterBoundOnReheat = false;

    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>GreatDeluge.CoolRate ... bound cooling rate (default 0.9999995)
     * <li>GreatDeluge.CoolRateInv ... inverse cooling rate (i.e., GreatDeluge.CoolRate = 1 - (1 / GreatDeluge.CoolRateInv ) )s
     * <li>GreatDeluge.UpperBoundRate ... bound upper bound limit relative to best solution ever found (default 1.05)
     * <li>GreatDeluge.LowerBoundRate ... bound lower bound limit relative to best solution ever found (default 0.97)
     * <li>GreatDeluge.Neighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>GreatDeluge.Random ... when true, a neighbour selector is selected randomly
     * <li>GreatDeluge.Update ... when true, a neighbour selector is selected using {@link ItcHillClimber.NeighbourSelector#getPoints()} weights (roulette wheel selection)
     * <li>Itc.NextHeuristicsOnReheat ... when true, null is returned in {@link ItcGreatDeluge#selectNeighbour(Solution)} once just after a reheat
     * <li>Itc.NextHeuristicsOnReheat.AlterBound ... when true, bound is updated after null is returned
     * </ul>
     * @param properties problem properties
     */    public ItcGreatDeluge(DataProperties properties) throws Exception {
        iCoolRate = properties.getPropertyDouble("GreatDeluge.CoolRate", iCoolRate);
        if (properties.getProperty("GreatDeluge.CoolRateInv")!=null) {
            iCoolRate = 1.0 - (1.0 / properties.getPropertyDouble("GreatDeluge.CoolRateInv", 1.0 / (1.0 - iCoolRate)));
            sLog.info("Cool rate is "+iCoolRate+" (inv:"+properties.getProperty("GreatDeluge.CoolRateInv")+")");
        }
        iUpperBoundRate = properties.getPropertyDouble("GreatDeluge.UpperBoundRate", iUpperBoundRate);
        iLowerBoundRate = properties.getPropertyDouble("GreatDeluge.LowerBoundRate", iLowerBoundRate);
        iNextHeuristicsOnReheat = properties.getPropertyBoolean("Itc.NextHeuristicsOnReheat", iNextHeuristicsOnReheat);
        iAlterBoundOnReheat = properties.getPropertyBoolean("Itc.NextHeuristicsOnReheat.AlterBound", iAlterBoundOnReheat);
        iRandomSelection = properties.getPropertyBoolean("GreatDeluge.Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean("GreatDeluge.Update", iUpdatePoints);
        String neighbours = properties.getProperty("GreatDeluge.Neighbours",
                ItcSwapMove.class.getName()+"@1;"+
                ItcNotConflictingMove.class.getName()+"@1");
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
        solver.currentSolution().addSolutionListener(this);
        for (Extension<V, T> ext: solver.getExtensions()) {
            if (ext instanceof ItcParameterWeightOscillation)
                ((ItcParameterWeightOscillation<V,T>)ext).addOscillationListener(this);
        }
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbourSelectors) {
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }
    
    private void addNeighbourSelection(NeighbourSelection<V,T> ns, double bonus) {
        iNeighbourSelectors.add(new NeighbourSelector<V,T>(ns, bonus, iUpdatePoints));
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
            if (n != null) return n;
        }
    }
    
    private boolean accept(Solution<V,T> solution, Neighbour<V,T> neighbour) {
        if (neighbour instanceof ItcLazyNeighbour) {
            ((ItcLazyNeighbour<V,T>)neighbour).setAcceptanceCriterion(this);
            return true;
        } else return (neighbour.value(solution.getAssignment())<=0 || solution.getModel().getTotalValue(solution.getAssignment())+neighbour.value(solution.getAssignment())<= getContext(solution.getAssignment()).getBound());
    }
    
    /** Implementation of {@link net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour.LazyNeighbourAcceptanceCriterion} interface */
    public boolean accept(Assignment<V,T> assignment, ItcLazyNeighbour<V,T> neighbour, double value) {
        return (value <= 0 || neighbour.getModel().getTotalValue(assignment) <= getContext(assignment).getBound());
    }
    
    /** Neighbour selection */
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        Neighbour<V,T> neighbour = null;
        Context context = getContext(solution.getAssignment());
        while ((neighbour=genMove(solution))!=null) {
        	context.incMove();
            if (accept(solution,neighbour)) {
            	context.incAcceptMove();
                break;
            }
        }
        return (neighbour==null ? null : neighbour);
    }
    
    public void bestSaved(Solution<V,T> solution) {
    	getContext(solution.getAssignment()).bestSaved(solution.getModel());
    }
    public void solutionUpdated(Solution<V,T> solution) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info, Collection<V> variables) {}
    public void bestCleared(Solution<V,T> solution) {}
    public void bestRestored(Solution<V,T> solution){}
    /** Update bound when {@link ItcParameterWeightOscillation} is changed.*/
    public void bestValueChanged(Solution<V,T> solution, double delta) {
    	getContext(solution.getAssignment()).incBound(delta);
    }
    
    public class Context implements AssignmentContext {
        private int iLastImprovingIter = 0;
        private int iIter = -1;
        private long iT0 = 0;
        private boolean iAlterBound = false;
        private double iBound = 0.0;
        private int iMoves = 0;
        private int iAcceptedMoves = 0;
        private int iNrIdle = 0;

        protected void info(Solution<V,T> solution) {
            sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
            sLog.info("Bound is "+sDF2.format(iBound)+", " +
            		"best value is "+sDF2.format(solution.getModel().getBestValue())+" ("+sDF2.format(100.0*iBound/solution.getModel().getBestValue())+"%), " +
            		"current value is "+sDF2.format(solution.getModel().getTotalValue(solution.getAssignment()))+" ("+sDF2.format(100.0*iBound/solution.getModel().getTotalValue(solution.getAssignment()))+"%), "+
            		"#idle="+iNrIdle+", "+
            		"Pacc="+sDF5.format(100.0*iAcceptedMoves/iMoves)+"%");
            if (iUpdatePoints)
                for (NeighbourSelector<V,T> ns: iNeighbourSelectors)
                    sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
            iAcceptedMoves = iMoves = 0;
        }
        
        protected boolean incIter(Solution<V, T> solution) {
        	double best = solution.getModel().getBestValue();
            if (iIter < 0) {
                iIter = 0; iLastImprovingIter = 0;
                iT0 = System.currentTimeMillis();
                iBound = iUpperBoundRate * best;
            	if (iNrIdle > 0 || solution.getModel().getTotalValue(solution.getAssignment()) > iBound)
            		solution.restoreBest();
            } else {
                iIter++; iBound *= iCoolRate;
            }
            if (sInfo && iIter % 100000 == 0) {
                info(solution);
            }
            if (iIter % 1000 == 0 && iBound > Math.max(best + 2.0, Math.pow(iUpperBoundRate, 1 + iNrIdle) * best)) {
            	iBound = Math.max(best + 2.0, Math.pow(iUpperBoundRate, iNrIdle) * best);
            	if (solution.getModel().getTotalValue(solution.getAssignment()) > iBound)
            		solution.restoreBest();
            }
            if (iBound < Math.pow(iLowerBoundRate, 1 + iNrIdle) * best) {
                iNrIdle++;
                sLog.info(" -<["+iNrIdle+"]>- ");
                iBound = Math.max(best + 2.0, Math.pow(iUpperBoundRate, iNrIdle) * best);
                iAlterBound = iAlterBoundOnReheat;
                if (iNextHeuristicsOnReheat) return true;
            }
            if (iAlterBound) {
            	iBound = Math.max(best + 2.0, Math.pow(iUpperBoundRate, Math.max(1, iNrIdle)) * best);
            	iAlterBound = false;
            }
            return false;
        }
        
        protected double getBound() {
        	return iBound;
        }
        
        protected void incMove() {
        	iMoves++;
        }
        
        protected void incAcceptMove() {
        	iAcceptedMoves++;
        }
        
        protected void bestSaved(Model<V, T> model) {
            iNrIdle = 0;
            iLastImprovingIter = iIter;
        }
        
        protected void incBound(double delta) {
            iBound += delta;
        }
    }

	@Override
	public Context createAssignmentContext(Assignment<V, T> assignment) {
		return new Context();
	}
}

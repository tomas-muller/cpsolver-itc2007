package net.sf.cpsolver.itc.heuristics.search;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
import net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcNotConflictingMove;
import net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcSwapMove;

import org.apache.log4j.Logger;

/**
 * Hill climber algorithm. Any move that decreases the overall solution value is rejected.
 * <br><br>
 * The search is stopped ({@link ItcHillClimber#selectNeighbour(Solution)} returns null) after 
 * HillClimber.MaxIdle idle (not improving) iterations.
 * <br><br>
 * Custom neighbours can be set using HillClimber.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:<br>
 * <code>
 * HillClimber.Neighbours=net.sf.cpsolver.itc.tim.neighbours.TimRoomMove;net.sf.cpsolver.itc.tim.neighbours.TimTimeMove;net.sf.cpsolver.itc.tim.neighbours.TimSwapMove;net.sf.cpsolver.itc.heuristics.neighbour.selection.ItcSwapMove;net.sf.cpsolver.itc.tim.neighbours.TimPrecedenceMove@0.1
 * </code>
 * <br>
 * Selector TimPrecedenceMove is 10&times; less probable to be selected than other selectors.
 * When SimulatedAnnealing.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When HillClimber.Update is true, {@link NeighbourSelector#update(Assignment, Neighbour, long)} is called 
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
public class ItcHillClimber<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,ItcHillClimber<V,T>.Context> implements SolutionListener<V,T>, LazyNeighbourAcceptanceCriterion<V,T> {
    private static Logger sLog = Logger.getLogger(ItcHillClimber.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static boolean sInfo = sLog.isInfoEnabled();
    private int iMaxIdleIters = 200000;
    private List<NeighbourSelector<V,T>> iNeighbourSelectors = new ArrayList<NeighbourSelector<V,T>>();
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;
    
    /**
     * Constructor
     * <ul>
     * <li>HillClimber.MaxIdle ... maximum number of idle iterations (default is 200000)
     * <li>HillClimber.Neighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>HillClimber.Random ... when true, a neighbour selector is selected randomly
     * <li>HillClimber.Update ... when true, a neighbour selector is selected using {@link ItcHillClimber.NeighbourSelector#getPoints()} weights (roulette wheel selection)
     * </ul>
     */
    public ItcHillClimber(DataProperties properties) throws Exception {
        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
        iRandomSelection = properties.getPropertyBoolean("HillClimber.Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean("HillClimber.Update", iUpdatePoints);
        String neighbours = properties.getProperty("HillClimber.Neighbours",
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
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbourSelectors) {
            s.init(solver);
            if (s.selection() instanceof HillClimberSelection)
                ((HillClimberSelection)s.selection()).setHcMode(true);
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
    
    /**
     * Select one of the given neighbourhoods randomly (all with the same probability or using roulette wheel selection), 
     * select neighbour, return it if 
     * its value is below or equal to zero (continue with the next selection otherwise).
     * Return null when the given number of idle iterations is reached.
     */
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        Context context = getContext(solution.getAssignment());
        context.setT0();
        while (true) {
        	if (context.incIter(solution)) break;
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
            if (n!=null) {
                if (n instanceof ItcLazyNeighbour) {
                    ((ItcLazyNeighbour<V,T>)n).setAcceptanceCriterion(this);
                    return n;
                } else if (n.value(solution.getAssignment())<=0.0) return n;
            }
        }
        context.reset();
        return null;
    }
    
    /** Implementation of {@link net.sf.cpsolver.itc.heuristics.neighbour.ItcLazyNeighbour.LazyNeighbourAcceptanceCriterion} interface */
    public boolean accept(Assignment<V,T> assignment, ItcLazyNeighbour<V,T> neighbour, double value) {
        return value<=0;
    }

    /**
     * Memorize the iteration when the last best solution was found.
     */
    public void bestSaved(Solution<V,T> solution) {
    	getContext(solution.getAssignment()).bestSaved(solution.getModel());
    }
    public void solutionUpdated(Solution<V,T> solution) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info) {}
    public void getInfo(Solution<V,T> solution, Map<String, String> info, Collection<V> variables) {}
    public void bestCleared(Solution<V,T> solution) {}
    public void bestRestored(Solution<V,T> solution){}  
    
    public static interface HillClimberSelection {
        public void setHcMode(boolean hcMode);
    }
    
    /**
     * A wrapper for {@link NeighbourSelection} that keeps some stats about the 
     * given neighbour selector.
     *
     */
    protected static class NeighbourSelector<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V,T> {
        protected static DecimalFormat sDF = new DecimalFormat("0.00");
        private boolean iUpdate = false;
        private NeighbourSelection<V,T> iSelection;
        private int iNrCalls = 0;
        private int iNrNotNull = 0;
        private int iNrSideMoves = 0;
        private int iNrImprovingMoves = 0;
        private double iBonus = 1.0;
        private double iPoints = 0;
        private long iTime = 0;
        
        /**
         * Constructor 
         * @param sel neighbour selector
         * @param bonus initial bonus (default is 1, can be changed by &nbsp;@n parameter after 
         * the name of the selector in Xxx.Neigbours, e.g., net.sf.cpsolver.itc.tim.neighbours.TimPrecedenceMove@0.1
         * for initial bonus 0.1 
         * @param update update selector bonus after each iteration
         */
        public NeighbourSelector(NeighbourSelection<V,T> sel, double bonus, boolean update) {
            iSelection = sel;
            iBonus = bonus;
            iUpdate = update;
        }
        /** Initialization */
        public void init(Solver<V,T> solver) {
            iSelection.init(solver);
        }
        /** Neighbour selection -- use {@link NeighbourSelection#selectNeighbour(Solution)} 
         * update stats if desired.
         */
        public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
            if (iUpdate) {
                long t0 = System.currentTimeMillis();
                Neighbour<V,T> n = iSelection.selectNeighbour(solution);
                long t1 = System.currentTimeMillis();
                update(solution.getAssignment(), n, t1-t0);
                return n;
            } else
                return iSelection.selectNeighbour(solution);
        }

        /**
         * Update stats
         * @param n generated move
         * @param time time needed to generate the move (in milliseconds)
         */
        public void update(Assignment<V,T> a, Neighbour<V,T> n, long time) {
            iNrCalls ++;
            iTime += time;
            if (n!=null) {
                iNrNotNull++;
                if (n.value(a)==0) {
                    iNrSideMoves++;
                    iPoints += 0.1;
                } else if (n.value(a)<0) {
                    iNrImprovingMoves++;
                    iPoints -= n.value(a);
                } else {
                    iPoints *= 0.9999;
                }
            } else {
                iPoints *= 0.999;
            }
        }
        
        /** Weight of the selector in the roulette wheel selection of neighbour selectors */
        public double getPoints() { return iBonus * Math.min(100.0, 0.1+iPoints); }
        /** Initial bonus */
        public double getBonus() { return iBonus; }
        /** Given neighbour selection */
        public NeighbourSelection<V,T> selection() { return iSelection; }
        /** Number of calls of {@link NeighbourSelection#selectNeighbour(Solution)} */
        public int nrCalls() { return iNrCalls; }
        /** Number of returned not-null moves */
        public int nrNotNull() { return iNrNotNull; }
        /** Number of returned moves with zero improvement of the solution (i.e., {@link Neighbour#value(Assignment)} = 0)*/
        public int nrSideMoves() { return iNrSideMoves; }
        /** Number of returned improving moves (i.e., {@link Neighbour#value(Assignment)} < 0)*/
        public int nrImprovingMoves() { return iNrImprovingMoves; }
        /** Total time spend in {@link NeighbourSelection#selectNeighbour(Solution)} (in milliseconds) */
        public long time() { return iTime; }
        /** Avarage number of iterations per second (calls of {@link NeighbourSelection#selectNeighbour(Solution)}) */
        public double speed() { return 1000.0*nrCalls()/time(); }
        /** String representation */
        public String toString() {
            return iSelection.getClass().getName().substring(iSelection.getClass().getName().lastIndexOf('.')+1)+" "+
                nrCalls()+"x, "+
                sDF.format(100.0*(nrCalls()-nrNotNull())/nrCalls())+"% null, "+
                sDF.format(100.0*nrSideMoves()/nrCalls())+"% side, "+
                sDF.format(100.0*nrImprovingMoves()/nrCalls())+"% imp, "+
                sDF.format(speed())+" it/s";
        }
    }
    
    public class Context implements AssignmentContext {
        private int iLastImprovingIter = 0;
        private int iIter = 0;
        private long iT0 = -1;

        public void reset() {
            iIter = 0;
            iLastImprovingIter = 0;
            iT0 = -1;
        }

        protected void setT0() {
            if (iT0 < 0) iT0 = System.currentTimeMillis();
        }
        
        protected boolean incIter(Solution<V, T> solution) {
            iIter ++;
            if (iIter % 10000 == 0) {
                if (sInfo) {
                    sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
                    if (iUpdatePoints)
                        for (NeighbourSelector<V,T> ns: iNeighbourSelectors)
                            sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
                }
            }
            return iIter-iLastImprovingIter >= iMaxIdleIters; 
        }
        
        protected void bestSaved(Model<V, T> model) {
        	iLastImprovingIter = iIter;
        }
    }

	@Override
	public Context createAssignmentContext(Assignment<V, T> assignment) {
		return new Context();
	}
}

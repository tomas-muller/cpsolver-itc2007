package net.sf.cpsolver.itc.heuristics.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

/**
 * Tabu search algorithm. 
 * <br><br>
 * If used as {@link NeighbourSelection}, the most improving (re)assignment of a value to a variable
 * is returned (all variables and all their values are enumerated). If there are more than one of 
 * such assignments, one is selected randomly. A returned assignment can cause unassignment of
 * other existing assignments. The search is stopped ({@link ItcTabuSearch#selectNeighbour(Solution)} 
 * returns null) after TabuSearch.MaxIdle idle (not improving) iterations.
 * <br><br>
 * If used as {@link ValueSelection}, the most improving (re)assignment of a value to a given variable
 * is returned (all values of the given variable are enumerated). If there are more than one of 
 * such assignments, one is selected randomly. A returned assignment can cause unassignment of
 * other existing assignments.  
 * <br><br>
 * To avoid cycling, a tabu is maintainded during the search. It is the list of the last n
 * selected values or {@link TabuElement} if values are implementing this interface. A
 * selection of a value that is present in the tabu list is only allowed when it improves the 
 * best ever found solution.
 * <br><br>
 * The minimum size of the tabu list is TabuSearch.MinSize, maximum size is TabuSearch.MaxSize (tabu 
 * list is not used when both sizes are zero). The current size of the tabu list starts at
 * MinSize (and is reset to MinSize every time a new best solution is found), it is increased
 * by one up to the MaxSize after TabuSearch.MaxIdle / (MaxSize - MinSize) non-improving 
 * iterations.
 * <br><br>
 * Conflict-based Statistics {@link ConflictStatistics} (CBS) can be used instead of (or together with)
 * tabu list, when CBS is used as a solver extension.
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
public class ItcTabuSearch<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,ItcTabuSearch<V,T>.TabuList> implements ValueSelection<V,T> {
    private static Logger sLog = Logger.getLogger(ItcTabuSearch.class);
    private ConflictStatistics<V,T> iStat = null;

    private long iFirstIteration = -1;
    private long iMaxIdleIterations = 10000;

    private int iTabuMinSize = 20;
    private int iTabuMaxSize = 200;
    
    private double iConflictWeight = 100;
    private double iValueWeight = 1;
    
    /**
     * <ul>
     * <li>TabuSearch.MaxIdle ... maximum number of idle iterations (default is 10000)
     * <li>TabuSearch.MinSize ... minimum size of the tabu list
     * <li>TabuSearch.MaxSize ... maximum size of the tabu list
     * <li>Value.ValueWeight ... weight of a value (i.e., {@link Value#toDouble()})
     * <li>Value.ConflictWeight ... weight of a conflicting value (see {@link Model#conflictValues(Value)}), 
     * it is also weighted by the past occurrences when conflict-based statistics is used 
     * </ul>
     */
    public ItcTabuSearch(DataProperties properties) throws Exception {
        iTabuMinSize = properties.getPropertyInt("TabuSearch.MinSize", iTabuMinSize);
        iTabuMaxSize = properties.getPropertyInt("TabuSearch.MaxSize", iTabuMaxSize);
        iMaxIdleIterations = properties.getPropertyLong("TabuSearch.MaxIdle", iMaxIdleIterations);
        iConflictWeight = properties.getPropertyDouble("Value.ConflictWeight", iConflictWeight);
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
    }
    
    /** Initialization */
    public void init(Solver<V,T> solver) {
    	super.init(solver);
        for (Extension<V, T> extension: solver.getExtensions())
            if (extension instanceof ConflictStatistics)
                iStat = (ConflictStatistics<V,T>)extension;
    }
    
    /**
     * An element that is to be used to populate (and check) tabu list
     * @param value given value (to be assigned to its variable)
     * @return value or {@link TabuElement#tabuElement()} when the given value implements {@link TabuElement} interface 
     */
    public Object tabuElement(T value) {
        return (value instanceof TabuElement ? ((TabuElement)value).tabuElement() : value);
    }
    
    /**
     * Neighbor selection 
     */
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
    	Assignment<V, T> assignment = solution.getAssignment();
    	TabuList tabu = getContext(assignment);
        if (iFirstIteration<0)
            iFirstIteration = solution.getIteration();
        long idle = solution.getIteration()-Math.max(iFirstIteration,solution.getBestIteration()); 
        if (idle>iMaxIdleIterations) {
            sLog.debug("  [tabu]    max idle iterations reached");
            iFirstIteration=-1;
            if (tabu.size() > 0) tabu.clear();
            return null;
        }
        if (tabu.size() > 0 && iTabuMaxSize>iTabuMinSize) {
            if (idle==0) {
            	tabu.resize(iTabuMinSize);
            } else if (idle%(iMaxIdleIterations/(iTabuMaxSize-iTabuMinSize))==0) { 
            	tabu.resize(Math.min(iTabuMaxSize,tabu.size()+1));
            }
        }
        
        boolean acceptConflicts = solution.getModel().getBestUnassignedVariables()>0;
        Model<V,T> model = solution.getModel();
        double bestEval = 0.0;
        List<T> best = null;
        for (V variable: model.variables()) {
            T assigned = assignment.getValue(variable);
            double assignedVal = (assigned==null?iConflictWeight:iValueWeight*assigned.toDouble(assignment));
            for (T value: variable.values(assignment)) {
                if (value.equals(assigned)) continue;
                double eval = iValueWeight*value.toDouble(assignment) - assignedVal;
                if (acceptConflicts) {
                    Set<T> conflicts = model.conflictValues(assignment, value);
                    for (T conflict: conflicts) {
                        eval -= iValueWeight*conflict.toDouble(assignment);
                        eval += iConflictWeight * (1.0+(iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflict, value)));
                    }
                } else {
                    if (model.inConflict(assignment, value)) continue;
                }
                if (tabu.size() > 0 && tabu.contains(tabuElement(value))) {
                    int un = assignment.nrUnassignedVariables(model)-(assigned==null?0:1);
                    if (un>model.getBestUnassignedVariables()) continue;
                    if (un==model.getBestUnassignedVariables() && model.getTotalValue(assignment)+eval>=model.getBestValue()) continue;
                }
                if (best==null || bestEval>eval) {
                    if (best==null)
                        best = new ArrayList<T>();
                    else
                        best.clear();
                    best.add(value);
                    bestEval = eval;
                } else if (bestEval==eval) {
                    best.add(value);
                }
            }
        }
        
        if (best==null) {
            sLog.debug("  [tabu] --none--");
            iFirstIteration=-1;
            if (tabu.size() > 0) tabu.clear();
            return null;
        }
        T bestVal = ToolBox.random(best);
        
        if (sLog.isDebugEnabled()) {
            Set<T> conflicts = model.conflictValues(assignment, bestVal);
            double wconf = (iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflicts, bestVal));
            T old = assignment.getValue(bestVal.variable());
            sLog.debug("  [tabu] "+bestVal+" ("+(old==null?"":"was="+old+", ")+"val="+bestEval+(conflicts.isEmpty()?"":", conf="+(wconf+conflicts.size())+"/"+conflicts)+")");
        }
        
        if (tabu.size() > 0) 
            tabu.add(tabuElement(bestVal));

        return new SimpleNeighbour<V,T>(bestVal.variable(), bestVal);        
    }
    
    /**
     * Value selection 
     */
    public T selectValue(Solution<V,T> solution, V variable) {
        if (iFirstIteration<0)
            iFirstIteration = solution.getIteration();
        long idle = solution.getIteration()-Math.max(iFirstIteration,solution.getBestIteration()); 
    	Assignment<V, T> assignment = solution.getAssignment();
    	TabuList tabu = getContext(assignment);
        if (idle>iMaxIdleIterations) {
            sLog.debug("  [tabu]    max idle iterations reached");
            iFirstIteration=-1;
            if (tabu.size() > 0) tabu.clear();
            return null;
        }
        if (tabu.size() > 0 && iTabuMaxSize>iTabuMinSize) {
            if (idle==0) {
                tabu.resize(iTabuMinSize);
            } else if (idle%(iMaxIdleIterations/(iTabuMaxSize-iTabuMinSize))==0) { 
                tabu.resize(Math.min(iTabuMaxSize, tabu.size()+1));
            }
        }

        Model<V,T> model = solution.getModel();
        double bestEval = 0.0;
        List<T> best = null;

        T assigned = assignment.getValue(variable);
        //double assignedVal = (assigned==null?-iConflictWeight:iValueWeight*assigned.toDouble());
        double assignedVal = (assigned==null?iConflictWeight:iValueWeight*assigned.toDouble(assignment));
        for (T value: variable.values(assignment)) {
            if (value.equals(assigned)) continue;
            Set<T> conflicts = model.conflictValues(assignment, value);
            double eval = iValueWeight*value.toDouble(assignment) - assignedVal;
            for (T conflict: conflicts) {
                eval -= iValueWeight*conflict.toDouble(assignment);
                eval += iConflictWeight * (1.0+(iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflict, value)));
            }
            if (tabu.size() > 0 && tabu.contains(tabuElement(value))) {
                //if (model.getTotalValue()+eval>=solution.getBestValue()) continue;
                int un = assignment.nrUnassignedVariables(model)-(assigned==null?0:1);
                if (un>model.getBestUnassignedVariables()) continue;
                if (un==model.getBestUnassignedVariables() && model.getTotalValue(assignment)+eval>=model.getBestValue()) continue;
		    }
            if (best==null || bestEval>eval) {
                if (best==null)
                    best = new ArrayList<T>();
                else
                    best.clear();
                best.add(value);
                bestEval = eval;
            } else if (bestEval==eval) {
                best.add(value);
            }
        }
        
        if (best==null) return null;
        T bestVal = ToolBox.random(best);
        
        if (sLog.isDebugEnabled()) {
            Set<T> conflicts = model.conflictValues(assignment, bestVal);
            double wconf = (iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflicts, bestVal));
            T old = assignment.getValue(bestVal.variable());
            sLog.debug("  [tabu] "+bestVal+" ("+(old==null?"":"was="+old+", ")+"val="+bestEval+(conflicts.isEmpty()?"":", conf="+(wconf+conflicts.size())+"/"+conflicts)+")");
        }
        
        if (tabu.size() > 0) tabu.add(tabuElement(bestVal));
        
        return bestVal;
    }

    
    /** Tabu-list */
    public class TabuList implements AssignmentContext {
        private Set<TabuItem> iList = new HashSet<TabuItem>();
        private int iSize;
        private long iIteration = 0;
        
        public TabuList() {
        	iSize = (iTabuMaxSize > 0 ? iTabuMinSize : 0);
        }
        
        public Object add(Object object) {
            if (iSize==0) return object;
            if (contains(object)) {
                iList.remove(new TabuItem(object, 0));
                iList.add(new TabuItem(object, iIteration++));
                return null;
            } else {
                Object oldest = null;
                if (iList.size()>=iSize) oldest = removeOldest();
                iList.add(new TabuItem(object, iIteration++));
                return oldest;
            }
        }
        
        public void resize(int newSize) {
            iSize = newSize;
            while (iList.size()>newSize) removeOldest();
        }
        
        public boolean contains(Object object) {
            return iList.contains(new TabuItem(object,0));
        }
        
        public void clear() {
            iList.clear();
        }
        
        public int size() {
            return iSize;
        }
        
        public Object removeOldest() {
            TabuItem oldest = null;
            for (TabuItem element: iList) {
                if (oldest==null || oldest.getIteration()>element.getIteration())
                    oldest = element;
            }
            if (oldest==null) return null;
            iList.remove(oldest);
            return oldest.getObject();
        }
        
        public String toString() {
            return new TreeSet<TabuItem>(iList).toString();
        }
    }

    /** Tabu item (an item in {@link TabuList}) */
    private static class TabuItem implements Comparable<TabuItem> {
        private Object iObject;
        private long iIteration;
        public TabuItem(Object object, long iteration) {
            iObject = object; iIteration = iteration;
        }
        public Object getObject() {
            return iObject;
        }
        public long getIteration() {
            return iIteration;
        }
        public boolean equals(Object object) {
            if (object==null || !(object instanceof TabuItem)) return false;
            return getObject().equals(((TabuItem)object).getObject());
        }
        public int hashCode() {
            return getObject().hashCode();
        }
        public int compareTo(TabuItem o) {
            return Double.compare(getIteration(), o.getIteration());
        }
        public String toString() {
            return getObject().toString();
        }
    }
    
    /** This interface is used to populate and check tabu list when 
     * implemented by a value (instead of the value itself).
     * This way, e.g., all assignments of a class into the same time (but
     * various rooms) may be considered the same for the tabu list. 
     */
    public static interface TabuElement {
        /** Tabu element of a value */
        public Object tabuElement();
    }

	@Override
	public TabuList createAssignmentContext(Assignment<V, T> assignment) {
		return new TabuList();
	}
}

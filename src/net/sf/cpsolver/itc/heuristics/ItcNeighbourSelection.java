package net.sf.cpsolver.itc.heuristics;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.algorithms.ParallelConstruction;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;

import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber;
import net.sf.cpsolver.itc.heuristics.search.ItcTabuSearch;
import net.sf.cpsolver.itc.heuristics.search.ItcGreatDeluge;

/**
 * Core search strategy for all three tracks of the ITC 2007.
 * <br><br>
 * At first, a complete solution is to be found using
 * {@link StandardNeighbourSelection} with {@link ItcUnassignedVariableSelection} as 
 * variable selection criterion and {@link ItcTabuSearch} as value selection criterion.
 * A weight of a value can be set for this phase using Itc.Construction.ValueWeight 
 * parameter. Neighbour selection can be redefined using Itc.Construction parameter
 * (contains fully qualified class name of a {@link NeighbourSelection}), or just variable
 * selection or value selection criterion can be redefined using parameters
 * Itc.ConstructionValue and Itc.ConstructionVariable.
 * <br><br>
 * Once a complete solution is found (construction neigbhour selection returns null), two (or
 * three) neighbour selections are rotated. Each selection is used till it returns null 
 * value in {@link NeighbourSelection#selectNeighbour(Solution)}. Once it happens next selection
 * starts to be used for selection (up till it returns null as well). When the last neighbour selection
 * is used and exhausted, first selection starts to be used again.
 * <br><br>
 * First neighbour selection is {@link ItcHillClimber} and it can be redefined by Itc.First parameter.<br>
 * Second neighbour selection is {@link ItcGreatDeluge} and it can be redefined by Itc.Second parameter.<br>
 * Thidr neighbour selection can be set using Itc.Third parameter and it is not defined by default. Also note
 * that by default, {@link ItcGreatDeluge} never returns null. 
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
public class ItcNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,ItcNeighbourSelection<V,T>.Phase> {
    private static Logger sLog = Logger.getLogger(ItcNeighbourSelection.class);
    private NeighbourSelection<V,T> iConstruct, iFirst, iSecond, iThird;
    
    /** Constructor */
    @SuppressWarnings("unchecked")
	public ItcNeighbourSelection(DataProperties properties) throws Exception {
        super();
        
        double valueWeight = properties.getPropertyDouble("Itc.Construction.ValueWeight", 0);
        iConstruct = (NeighbourSelection<V,T>)Class.forName(properties.getProperty("Itc.Construction",StandardNeighbourSelection.class.getName())).
        		getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {properties});
        if (iConstruct instanceof StandardNeighbourSelection) {
            StandardNeighbourSelection<V,T> std = (StandardNeighbourSelection<V,T>)iConstruct;
            std.setValueSelection((ValueSelection<V,T>)
                    Class.forName(properties.getProperty("Itc.ConstructionValue",ItcTabuSearch.class.getName())).
                    getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {properties}));
            std.setVariableSelection((VariableSelection<V,T>)
                    Class.forName(properties.getProperty("Itc.ConstructionVariable",ItcUnassignedVariableSelection.class.getName())).
                    getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {properties}));
            try {
                std.getValueSelection().getClass().getMethod("setValueWeight", new Class[] {double.class}).
                	invoke(std.getValueSelection(), new Object[] {new Double(valueWeight)});
            } catch (NoSuchMethodException e) {}
        }
        try {
            iConstruct.getClass().getMethod("setValueWeight", new Class[] {double.class}).invoke(iConstruct, new Object[] {new Double(valueWeight)});
        } catch (NoSuchMethodException e) {}
        
        iFirst = (NeighbourSelection<V,T>)Class.forName(properties.getProperty("Itc.First",ItcHillClimber.class.getName())).
            getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {properties});
        
        iSecond = (NeighbourSelection<V,T>)Class.forName(properties.getProperty("Itc.Second",ItcGreatDeluge.class.getName())).
            getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {properties});
        
        if (properties.getProperty("Itc.Third")!=null)
            iThird = (NeighbourSelection<V,T>)Class.forName(properties.getProperty("Itc.Third")).
                getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {properties});
    }
    
    /** Initialization */
    public void init(Solver<V,T> solver) {
        super.init(solver);
        if (!solver.hasSingleSolution())
        	iConstruct = new ParallelConstruction<V, T>(solver.getProperties(), iConstruct);
        iConstruct.init(solver);
        iFirst.init(solver);
        iSecond.init(solver);
        if (iThird!=null) iThird.init(solver);
    }
    
    /** Neighbour selection  -- based on the phase, construction strategy is used first,
     * than it iterates between two or three given neighbour selections*/
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
    	if (!solution.isComplete() && solution.getModel().getBestUnassignedVariables() == 0)
    		return iConstruct.selectNeighbour(solution);
        
    	Neighbour<V,T> n = null;
        Phase phase = getContext(solution.getAssignment());
        switch (phase.getPhase()) {
            case 0 :
                n = iConstruct.selectNeighbour(solution);
                if (n!=null) return n;
                phase.setPhase(1, solution, "one");
            case 1 :
                n = iFirst.selectNeighbour(solution);
                if (n!=null) return n;
                phase.setPhase(2, solution, "two");
            case 2 :
                n = iSecond.selectNeighbour(solution);
                if (n!=null) return n;
                phase.setPhase(3, solution, "three");
            case 3 :
                n = (iThird==null?null:iThird.selectNeighbour(solution));
                if (n!=null) return n;
                phase.setPhase(4, solution, "one");
            default :
            	phase.setPhase(1, solution, "one");
                return selectNeighbour(solution);
        }
    }
    
    public class Phase implements AssignmentContext {
        private int iPhase = 0;

		public int getPhase() {
			return iPhase;
		}
		
		/** Change phase, i.e., what selector is to be used next */
		public synchronized void setPhase(int phase, Solution<V,T> solution, String name) {
			if (iPhase == phase) return;
	        iPhase = phase;
	        if (sLog.isInfoEnabled()) {
	            sLog.info("**CURR["+solution.getIteration()+"]** " + solution);
	            sLog.info("Phase "+name);
	        }
		}
    }

	@Override
	public Phase createAssignmentContext(Assignment<V, T> assignment) {
		return new Phase();
	}

}

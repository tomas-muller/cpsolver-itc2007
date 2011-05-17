package net.sf.cpsolver.itc.heuristics.neighbour.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;

/**
 * Systematically enumerate all variables and their values.
 * If parameter SystematicMove.RandomOrder is true, variables
 * and their values are enumerated in a random order. If 
 * SystematicMove.AllowSwaps is true, swaps between all pairs 
 * of variables are considered as well. Variables must implement
 * {@link Swapable} interface to be able to use this option. 
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
public class ItcSystematicMove<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V,T> {
    private double iValueWeight = 1;
    private double iConflictWeight = 100;
    private boolean iRandomOrder = true;
    private boolean iAllowSwaps = true;
    private Iterator<V> iVarEn = null;
    private Iterator<Object> iValEn = null;
    private V iVariable = null;
    
    /** Constructor */
    public ItcSystematicMove(DataProperties properties) {
        iConflictWeight = properties.getPropertyDouble("Value.ConflictWeight", iConflictWeight);
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
        iRandomOrder = properties.getPropertyBoolean("SystematicMove.RandomOrder", iRandomOrder);
        iAllowSwaps = properties.getPropertyBoolean("SystematicMove.AllowSwaps", iAllowSwaps);
    }
    
    /** Initialization */
    public void init(Solver<V,T> solver) {
    }
    
    /** Neighbour selection */
    @SuppressWarnings("unchecked")
	public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        Model<V,T> model = solution.getModel();
        if (iVarEn==null) {
            iVarEn = new RandomIterator<V>(model.variables(), iRandomOrder);
            iVariable = iVarEn.next();
            List<Object> v2 = new ArrayList<Object>(iVariable.values().size() + model.variables().size());
            if (iAllowSwaps && iVariable instanceof Swapable)
                v2.addAll(model.variables());
            v2.addAll(iVariable.values());
            iValEn = new RandomIterator<Object>(v2, iRandomOrder);
        }
        if (!iValEn.hasNext()) {
            if (!iVarEn.hasNext()) {
                iVarEn = new RandomIterator<V>(model.variables(), iRandomOrder);
            }
            iVariable = iVarEn.next();
            List<Object> v2 = new ArrayList<Object>(iVariable.values().size() + model.variables().size());
            if (iAllowSwaps && iVariable instanceof Swapable)
                v2.addAll(model.variables());
            v2.addAll(iVariable.values());
            iValEn = new RandomIterator<Object>(v2, iRandomOrder);
        }
        Object object = iValEn.next();
        if (object instanceof Variable) {
            V anotherVariable = (V)ToolBox.random(model.variables());
            if (iVariable.equals(anotherVariable)) return null;
            return ((Swapable<V,T>)iVariable).findSwap(anotherVariable);
        } else {
            T value = (T)object;
            if (value.equals(iVariable.getAssignment())) return null;
            double eval = iValueWeight * value.toDouble();
            if (iVariable.getAssignment()!=null)
                eval -= iValueWeight * iVariable.getAssignment().toDouble();
            else
                eval -= iConflictWeight;
            eval += iConflictWeight * model.conflictValues(value).size();
            return new ItcSimpleNeighbour<V,T>(iVariable,value,eval);
        }
    }
    
    /** Randomized enumeration */
    public static class RandomIterator<E> implements Iterator<E> {
        private Iterator<E> iEnum = null;
        /** Constructor 
         * @param collection a collection that should be enumerated
         **/
        public RandomIterator(Collection<E> collection) {
            this(collection, true);
        }
        /** Constructor 
         * @param collection a collection that should be enumerated
         * @param randomOrder if false, given collection is enumerated in normal order (i.e., same as {@link Vector#elements()}) 
         **/
        public RandomIterator(Collection<E> collection, boolean randomOrder) {
            if (!randomOrder) {
                iEnum = collection.iterator();
            } else {
                List<E> vect = new ArrayList<E>(collection);
                Collections.shuffle(vect, ToolBox.getRandom());
                iEnum = vect.iterator();
            }
        }
        /** True if there are more elements to enumerate */
        public boolean hasNext() {
            return iEnum.hasNext();
        }
        /** Next element */
        public E next() {
            return iEnum.next();
        }
        /** Remove an element */
        public void remove() {
        	iEnum.remove();
        }
    }
}

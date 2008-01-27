package net.sf.cpsolver.itc.heuristics.neighbour.selection;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
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
public class ItcSystematicMove implements NeighbourSelection {
    private double iValueWeight = 1;
    private double iConflictWeight = 100;
    private boolean iRandomOrder = true;
    private boolean iAllowSwaps = true;
    private Enumeration iVarEn = null, iValEn = null;
    private Variable iVariable = null;
    
    /** Constructor */
    public ItcSystematicMove(DataProperties properties) {
        iConflictWeight = properties.getPropertyDouble("Value.ConflictWeight", iConflictWeight);
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
        iRandomOrder = properties.getPropertyBoolean("SystematicMove.RandomOrder", iRandomOrder);
        iAllowSwaps = properties.getPropertyBoolean("SystematicMove.AllowSwaps", iAllowSwaps);
    }
    
    /** Initialization */
    public void init(Solver solver) {
    }
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        Model model = solution.getModel();
        if (iVarEn==null) {
            iVarEn = new RandomEnumeration(model.variables(), iRandomOrder);
            iVariable = (Variable)iVarEn.nextElement();
            Vector v2 = new Vector(iVariable.values().size()+model.variables().size());
            if (iAllowSwaps && iVariable instanceof Swapable)
                v2.addAll(model.variables());
            v2.addAll(iVariable.values());
            iValEn = new RandomEnumeration(v2, iRandomOrder);
        }
        SimpleNeighbour n = null;
        if (!iValEn.hasMoreElements()) {
            if (!iVarEn.hasMoreElements()) {
                iVarEn = new RandomEnumeration(model.variables(), iRandomOrder);
            }
            iVariable = (Variable)iVarEn.nextElement();
            Vector v2 = new Vector(iVariable.values().size()+model.variables().size());
            if (iAllowSwaps && iVariable instanceof Swapable)
                v2.addAll(model.variables());
            v2.addAll(iVariable.values());
            iValEn = new RandomEnumeration(v2, iRandomOrder);
        }
        Object object = iValEn.nextElement();
        if (object instanceof Variable) {
            Variable anotherVariable = (Variable)ToolBox.random(model.variables());
            if (iVariable.equals(anotherVariable)) return null;
            return ((Swapable)iVariable).findSwap(anotherVariable);
        } else {
            Value value = (Value)object;
            if (value.equals(iVariable.getAssignment())) return null;
            Set conflicts = model.conflictValues(value);
            double eval = iValueWeight * value.toDouble();
            if (iVariable.getAssignment()!=null)
                eval -= iValueWeight * iVariable.getAssignment().toDouble();
            else
                eval -= iConflictWeight;
            eval += iConflictWeight * model.conflictValues(value).size();
            return new ItcSimpleNeighbour(iVariable,value,eval);
        }
    }
    
    /** Randomized enumeration */
    public static class RandomEnumeration implements Enumeration {
        private Enumeration iEnum = null;
        /** Constructor 
         * @param collection a collection that should be enumerated
         **/
        public RandomEnumeration(Vector collection) {
            this(collection, true);
        }
        /** Constructor 
         * @param collection a collection that should be enumerated
         * @param randomOrder if false, given collection is enumerated in normal order (i.e., same as {@link Vector#elements()}) 
         **/
        public RandomEnumeration(Vector collection, boolean randomOrder) {
            if (!randomOrder) {
                iEnum = collection.elements();
            } else {
                Vector vect = new Vector(collection);
                Collections.shuffle(vect, ToolBox.getRandom());
                iEnum = vect.elements();
            }
        }
        /** True if there are more elements to enumerate */
        public boolean hasMoreElements() {
            return iEnum.hasMoreElements();
        }
        /** Next element */
        public Object nextElement() {
            return iEnum.nextElement();
        }
    }
}

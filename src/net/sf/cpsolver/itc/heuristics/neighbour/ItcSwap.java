package net.sf.cpsolver.itc.heuristics.neighbour;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Swap two variables. Variables must implement {@link Swapable} interface.
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
public class ItcSwap extends Neighbour {
    private Value iV1, iV2;
    private double iValue;
    
    /**
     * Constructor
     * @param v1 first variable
     * @param v2 second variable
     * @param value change of overall solution value
     */
    public ItcSwap(Value v1, Value v2, double value) {
        iV1 = v1;
        iV2 = v2;
        iValue = value;
    }
    
    /** Change of overall solution value */
    public double value() {
        return iValue;
    }
    
    /** First variable */
    public Value firstValue() { return iV1; }
    /** Second variable */
    public Value secondValue() { return iV2; }
    
    /** Perform assignment */
    public void assign(long iteration) {
        if (iV1.variable().getAssignment()!=null) iV1.variable().unassign(iteration);
        if (iV2.variable().getAssignment()!=null) iV2.variable().unassign(iteration);
        iV1.variable().assign(iteration, iV1);
        iV2.variable().assign(iteration, iV2);
    }
    
    /** String representation */
    public String toString() {
        return 
            "[swap] "+iV1.variable().getName()+" "+
            (iV1.variable().getAssignment()==null?"null":iV1.variable().getAssignment().getName())+" -> "+
            iV1.getName()+", "+
            iV2.variable().getName()+" "+
            (iV2.variable().getAssignment()==null?"null":iV2.variable().getAssignment().getName() )+" -> "+
            iV2.getName()+
            " ("+iValue+")";
    }
    
    /** Hash code */
    public int hashCode() {
        return iV1.hashCode() ^ iV2.hashCode();
    }
    
    /** Compare two swap neighbours for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ItcSwap)) return false;
        ItcSwap s = (ItcSwap)o;
        return (iV1.equals(s.iV1) && iV2.equals(s.iV2)) || (iV1.equals(s.iV2) && iV2.equals(s.iV1));
    }
    
    /**
     * Find a swap of two variables (if possible). To be implemented by a {@link Variable}.
     */
    public static interface Swapable {
        /**
         * Find a swap this variable with the given one (return null when no swap is found).
         */
        public Neighbour findSwap(Variable another);
    }
}

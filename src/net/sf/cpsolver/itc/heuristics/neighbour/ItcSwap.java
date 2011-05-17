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
public class ItcSwap<V extends Variable<V, T>, T extends Value<V, T>> extends Neighbour<V, T> {
    private T iV1, iV2;
    private double iValue;
    
    /**
     * Constructor
     * @param v1 first variable
     * @param v2 second variable
     * @param value change of overall solution value
     */
    public ItcSwap(T v1, T v2, double value) {
        iV1 = v1;
        iV2 = v2;
        iValue = value;
    }
    
    /** Change of overall solution value */
    public double value() {
        return iValue;
    }
    
    /** First variable */
    public T firstValue() { return iV1; }
    /** Second variable */
    public T secondValue() { return iV2; }
    
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
        ItcSwap<?,?> s = (ItcSwap<?,?>)o;
        return (iV1.equals(s.iV1) && iV2.equals(s.iV2)) || (iV1.equals(s.iV2) && iV2.equals(s.iV1));
    }
    
    /**
     * Find a swap of two variables (if possible). To be implemented by a {@link Variable}.
     */
    public static interface Swapable<V extends Variable<V, T>, T extends Value<V, T>>{
        /**
         * Find a swap this variable with the given one (return null when no swap is found).
         */
        public Neighbour<V,T> findSwap(V another);
    }
}

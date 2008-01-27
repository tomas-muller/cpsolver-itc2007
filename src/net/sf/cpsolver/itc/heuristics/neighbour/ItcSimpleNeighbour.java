package net.sf.cpsolver.itc.heuristics.neighbour;

import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Reassignment of a variable.
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
public class ItcSimpleNeighbour extends SimpleNeighbour {
    private double iValue;
    
    /**
     * Constructor
     * @param variable variable to be reassigned
     * @param value new value to be assigned 
     * @param doubleValue change in overall solution value
     */
    public ItcSimpleNeighbour(Variable variable, Value value, double doubleValue) {
        super(variable,value);
        iValue = doubleValue;
        if (variable.getAssignment()==null) iValue -= 5000;
    }
    
    /**
     * Constructor
     * @param variable variable to be reassigned
     * @param value new value to be assigned 
     */
    public ItcSimpleNeighbour(Variable variable, Value value) {
        super(variable,value);
        iValue = value.toDouble();
        if (variable.getAssignment()!=null) 
            iValue -= variable.getAssignment().toDouble();
        else
            iValue -= 5000;
    }
    
    /**
     * Change in overall solution value
     */
    public double value() {
        return iValue;
    }
    
    /** String representation */
    public String toString() {
        return "Simple "+getVariable().getName()+" "+
            (getVariable().getAssignment()==null?
                    "null"
                :
                    getVariable().getAssignment().getName()+
                    " (val="+getVariable().getAssignment().toDouble()+")"
              )+" -> "+
              getValue().getName()+
              " (val="+getValue().toDouble()+")";
    }
    
}

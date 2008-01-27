package net.sf.cpsolver.itc.heuristics.neighbour;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.itc.ItcModel;

/**
 * Lazy swap of two variables. See {@link ItcLazyNeighbour}.
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
public class ItcLazySwap extends ItcLazyNeighbour {
    private Value iV1, iV2, iOldV1, iOldV2; 
    
    /**
     * Constructor
     * @param v1 first variable
     * @param v2 second variable
     */
    public ItcLazySwap(Value v1, Value v2) {
        iV1 = v1;
        iV2 = v2;
        iOldV1 = v1.variable().getAssignment();
        iOldV2 = v2.variable().getAssignment();
    }
    
    /** Perform swap */
    protected void doAssign(long iteration) {
        if (iOldV1!=null) iOldV1.variable().unassign(iteration);
        if (iOldV2!=null) iOldV2.variable().unassign(iteration);
        iV1.variable().assign(iteration, iV1);
        iV2.variable().assign(iteration, iV2);
    }
    
    /** Undo the swap */
    protected void undoAssign(long iteration) {
        iV1.variable().unassign(iteration);
        iV2.variable().unassign(iteration);
        if (iOldV1!=null) iOldV1.variable().assign(iteration, iOldV1);
        if (iOldV2!=null) iOldV2.variable().assign(iteration, iOldV2);
    }
    /** Return problem model */
    public ItcModel getModel() {
        return (ItcModel)iV1.variable().getModel();
    }
    
    /** String representation */
    public String toString() {
        return "Lazy "+iOldV1+" -> "+iV1+", "+iOldV2+" -> "+iV2;
    }

}

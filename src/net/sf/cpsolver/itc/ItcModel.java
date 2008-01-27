package net.sf.cpsolver.itc;

import java.io.File;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Few model extension, common for all three tracks.
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
public abstract class ItcModel extends Model {
    private DataProperties iProperties;
    /** Set problem properties */
    public void setProperties(DataProperties properties) { iProperties = properties; }
    /** Get problem properties */
    public DataProperties getProperties() { return iProperties; } 
    /** Load input file */
    public abstract boolean load(File file) throws Exception;
    /** Save output solution */
    public abstract boolean save(File file) throws Exception;
    /** Print CSV header (comma separated value text file) */
    public String csvHeader() { return ""; }
    /** Print CSV line (problem specific solution properties) */
    public String csvLine() { return ""; }
    /** Return true if csv line should be printed (e.g., whether found solution is complete) */
    public boolean cvsPrint() {
        return nrUnassignedVariables()==0;
    }
    /** Make feasible, executed by {@link ItcTest} after the solver is stopped */
    public void makeFeasible() {}
}

package net.sf.cpsolver.itc.tim.heuristics;

import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.heuristics.ItcParameterWeightOscillation;
import net.sf.cpsolver.itc.tim.model.TimModel;

/**
 * Oscillation of weight of violated precedence constraints. It starts at one 
 * and is gradually increased as the search progresses. Some oscillation
 * of the weight may be defined using {@link ItcParameterWeightOscillation}.
 * <br><br>
 * To enable oscillations of this parameter, add this class to
 * Extensions.Classes parameter.
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
public class TimPrecedenceWeightOscillation extends ItcParameterWeightOscillation {
    /** Constructor */
    public TimPrecedenceWeightOscillation(Solver solver, DataProperties properties) {
        super(solver, properties);
    }
    
    /** Current value of the parameter weight */
    public double currentValue() {
        return ((TimModel)getModel()).precedenceViolations(false);
    }
    /** Update parameter weight with the new value */
    public void changeWeight(double weight) {
        TimModel.sPrecedenceViolationWeight = (int)Math.round(weight);
    }
}

package net.sf.cpsolver.itc.tim.heuristics;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.heuristics.ItcParameterWeightOscillation;
import net.sf.cpsolver.itc.tim.model.TimLocation;
import net.sf.cpsolver.itc.tim.model.TimEvent;
import net.sf.cpsolver.itc.tim.model.TimModel;

/**
 * Oscillation of weight of placements with no room assigned. It starts at one 
 * and is gradually increased as the search progresses. Some oscillation
 * of the weight may be defined using {@link ItcParameterWeightOscillation}.
 * <br><br>
 * Placements with no room assigned are not allowed by default. To allow 
 * placements with no room, set Tim.AllowNoRoom parameter to true.
 * To enable oscillations of this parameter, add this class to
 * Extensions.Classes parameter.
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
public class TimNoRoomWeightOscillation extends ItcParameterWeightOscillation<TimEvent, TimLocation> {
    /** Constructor */
    public TimNoRoomWeightOscillation(Solver<TimEvent, TimLocation> solver, DataProperties properties) {
        super(solver, properties);
    }
    
    /** Current value of the parameter weight */
    public double currentValue(Assignment<TimEvent, TimLocation> assignment) {
        return ((TimModel)getModel()).noRoomViolations(assignment, false);
    }
    /** Update parameter weight with the new value */
    public void changeWeight(double weight) {
        TimModel.sNoRoomWeight = (int)Math.round(weight);
    }
}

package net.sf.cpsolver.itc.exam.heuristics;

import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.exam.model.ExExam;
import net.sf.cpsolver.itc.exam.model.ExModel;
import net.sf.cpsolver.itc.exam.model.ExPlacement;
import net.sf.cpsolver.itc.heuristics.ItcParameterWeightOscillation;

/**
 * Oscillation of weight of violations of binary constraints. It starts at one 
 * and is gradually increased as the search progresses. Some oscillation
 * of the weight may be defined using {@link ItcParameterWeightOscillation}.
 * <br><br>
 * Violations of binary constraints are not allowed by default. To allow 
 * violations of binary constraints, set Exam.AllowBinaryViolations parameter to true.
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
public class ExBinaryViolationOscillation extends ItcParameterWeightOscillation<ExExam, ExPlacement> {
    
    /** Constructor */
    public ExBinaryViolationOscillation(Solver<ExExam, ExPlacement> solver, DataProperties properties) {
        super(solver, properties);
    }
    
    /** Current value of the parameter weight */
    public double currentValue() {
        return ((ExModel)getModel()).getBinaryViolations(false);
    }
    
    /** Update parameter weight with the new value */
    public void changeWeight(double weight) {
        ((ExModel)getModel()).setBinaryViolationWeight((int)Math.round(weight));
    }
}

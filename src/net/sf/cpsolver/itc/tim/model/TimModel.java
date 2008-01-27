package net.sf.cpsolver.itc.tim.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of Post Enrollment based Course Timetabling (tim) problem model.
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
public class TimModel extends TTComp02Model {
    private static Logger sLog = Logger.getLogger(TimModel.class);
    private Vector iPrecedences = new Vector();
    
    /** Weight of violated precedence constraints */
    public static int sPrecedenceViolationWeight = 0;
    /** Weight of events withou assigned room */
    public static int sNoRoomWeight = 0;
    
    private int iPrecedenceViolations = 0;
    private int iNoRoomViolations = 0;
    
    /** Constructor */
    public TimModel() {
        super();
    }
    
    private Boolean iAllowProhibitedTime = null;
    /** True if it is allowed to assign a time that is not avaiable (Tim.AllowProhibitedTime must be set to true)*/
    public boolean isAllowProhibitedTime() {
        if (iAllowProhibitedTime==null)
            iAllowProhibitedTime = getProperties().getPropertyBoolean("Tim.AllowProhibitedTime", Boolean.FALSE);
        return iAllowProhibitedTime.booleanValue();
    }

    private Boolean iAllowNoRoom = null;
    /** True if it is allowed to not assign an event a room (Tim.AllowNoRoom must be set to true) */
    public boolean isAllowNoRoom() {
        if (iAllowNoRoom==null)
            iAllowNoRoom = getProperties().getPropertyBoolean("Tim.AllowNoRoom", Boolean.FALSE);
        return iAllowNoRoom.booleanValue();
    }
    
    /** Load problem from given file. See <a href='http://www.cs.qub.ac.uk/itc2007/postenrolcourse/course_post_index_files/Inputformat.htm'>Input Forma</a> for more details. */
    public boolean load(File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		StringTokenizer stk = new StringTokenizer(in.readLine(), " ");
		int nrEvents = Integer.parseInt(stk.nextToken());
		int nrRooms = Integer.parseInt(stk.nextToken());
		int nrFeatures = Integer.parseInt(stk.nextToken());
		int nrStudents = Integer.parseInt(stk.nextToken());
		
		iRooms = new Vector();
		for (int i=0;i<nrRooms;i++)
			iRooms.add(new TimRoom(i, Integer.parseInt(in.readLine())));
		
		for (int i=0;i<nrEvents;i++)
			addVariable(new TimEvent(i));
		
		iStudents = new Vector();
		for (int i=0;i<nrStudents;i++)
			iStudents.add(new TimStudent(i));
		
		for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
		    TimStudent student = (TimStudent)e.nextElement();
		    for (Enumeration f=variables().elements();f.hasMoreElements();) {
		        TimEvent event = (TimEvent)f.nextElement();
				if (1==Integer.parseInt(in.readLine())) {
					event.students().add(student);
					student.addVariable(event);
				}
			}
		}
		
		boolean roomFeature[][] = new boolean[nrRooms][nrFeatures];
		for (int i=0;i<nrRooms;i++)
			for (int j=0;j<nrFeatures;j++)
				roomFeature[i][j] = (1==Integer.parseInt(in.readLine()));
		
		boolean eventFeature[][] = new boolean[nrEvents][nrFeatures];
		for (int i=0;i<nrEvents;i++)
			for (int j=0;j<nrFeatures;j++)
				eventFeature[i][j] = (1==Integer.parseInt(in.readLine()));
		
		for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
		    TimRoom room = (TimRoom)e.nextElement();
            for (Enumeration f=variables().elements();f.hasMoreElements();) {
                TimEvent event = (TimEvent)f.nextElement();
				if (event.students().size()>room.size()) continue;
				boolean hasAllFeatures = true;
				for (int j=0;j<nrFeatures;j++) {
					if (eventFeature[(int)event.getId()][j] && !roomFeature[(int)room.getId()][j]) {
						hasAllFeatures = false; break;
					}
				}
				if (!hasAllFeatures) continue;
				room.addVariable(event);
				event.rooms().add(room);
			}
		}
		
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            TimEvent event = (TimEvent)e.nextElement();
            for (int i=0;i<45;i++) {
                event.setAvailable(i, (1==Integer.parseInt(in.readLine())));
            }
        }

        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            TimEvent ev1 = (TimEvent)e.nextElement();
            for (Enumeration f=variables().elements();f.hasMoreElements();) {
                TimEvent ev2 = (TimEvent)f.nextElement();
                if (1==Integer.parseInt(in.readLine())) {
                    TimPrecedence precedence = new TimPrecedence(ev1,ev2);
                    iPrecedences.add(precedence);
                    addConstraint(precedence);
                    ev1.successors().add(ev2);
                    ev2.predecessors().add(ev1);
                }
            }
        }
		
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            TimStudent student = (TimStudent)e.nextElement();
			addConstraint(student);
		}

        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            TimRoom room = (TimRoom)e.nextElement();
			addConstraint(room);
		}
        
        for (int i=1;i<=getProperties().getPropertyInt("Tim.NrDummyRooms", 0);i++) {
            TimRoom room = new TimRoom(-i, Integer.MAX_VALUE);
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                TimEvent event = (TimEvent)e.nextElement();
                room.addVariable(event);
                event.rooms().add(room);
            }
            iRooms.add(room);
            addConstraint(room);
        }
		
        for (Enumeration f=variables().elements();f.hasMoreElements();) {
            TimEvent event = (TimEvent)f.nextElement();
            event.init(isAllowProhibitedTime(),isAllowNoRoom());
		}
		
		in.close();
		
		if ("true".equals(System.getProperty("stats"))) 
		    stats(file.getName());
		
		if (System.getProperty("solution")!=null)
		    loadSolution(new File(System.getProperty("solution")));
		
		return true;
	}
    
    /** Overall solution value (violated precedence constraints, used not available times, assignment without room are added as well)*/
    public double getTotalValue() {
        return 
            super.getTotalValue() + 
            sPrecedenceViolationWeight*precedenceViolations(false) + 
            sNoRoomWeight*noRoomViolations(false);
    }
    
    private void stats(String instance) {
        try {
            File file = new File("tim.csv");
            boolean ex = file.exists();
            PrintWriter pw = new PrintWriter(new FileWriter(file, true));
            int notAvail = 0; int avRooms = 0;
            int singleRoom = 0;
            int degree = 0;
            int dsize = 0;
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                TimEvent event = (TimEvent)e.nextElement();
                for (int i=0;i<45;i++)
                    if (!event.isAvailable(i)) notAvail++;
                avRooms += event.rooms().size();
                if (event.rooms().size()==1) singleRoom++;
                dsize += event.values().size();
            }
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                TimEvent e1 = (TimEvent)e.nextElement();
                for (Enumeration f=variables().elements();f.hasMoreElements();) {
                    TimEvent e2 = (TimEvent)f.nextElement();
                    boolean sameStudent = false;
                    for (Enumeration g=e1.students().elements();!sameStudent && g.hasMoreElements();) {
                        TimStudent s = (TimStudent)g.nextElement();
                        if (e2.students().contains(s)) sameStudent=true;
                    }
                    if (sameStudent) 
                        degree++;
                    else if (e1.rooms().size()==1 && e2.rooms().size()==1 && e1.rooms().firstElement().equals(e2.rooms().firstElement()))
                        degree++;
                    else if (e1.predecessors().contains(e2) || e1.successors().contains(e2))
                        degree++;
                }
            }
            int stEvs = 0;
            for (Enumeration e=students().elements();e.hasMoreElements();) {
                TimStudent student = (TimStudent)e.nextElement();
                stEvs += student.variables().size();
            }
            if (!ex) {
                pw.println(
                        "instance,"+
                        "events,"+
                        "rooms,"+
                        "students,"+
                        "precedences,"+
                        "timeAvail,"+
                        "roomAvail,"+
                        "singleRoomEvt,"+
                        "evtPerStd,"+
                        "avDegree,"+
                        "fill[%],"+
                        "dsize"
                        );
            }
            pw.println(
                    instance+","+
                    variables().size()+","+
                    rooms().size()+","+
                    students().size()+","+
                    getPrecedences().size()+","+
                    sDoubleFormat.format(45.0-((double)notAvail)/variables().size())+","+
                    sDoubleFormat.format(((double)avRooms)/variables().size())+","+
                    singleRoom+","+
                    sDoubleFormat.format(((double)stEvs)/variables().size())+","+
                    sDoubleFormat.format(((double)degree)/variables().size())+","+
                    sDoubleFormat.format(100.0*variables().size()/(rooms().size()*45))+","+
                    dsize
                    );
            pw.flush(); pw.close();
        } catch (Exception e) {}
    }

    /** CSV header:
     * df for distance to feasibility,
     * pv for violated precedence constraints,
     * nr for assignments without room,
     * 1d for one event a day,
     * lt for use of last time of a day,
     * 3+ for more three or more events consecutively
     */
    public String csvHeader() { 
        return "df,pv,nr,"+super.csvHeader();
    }
    
    /** CSV solution line:
     * distance to feasibility,
     * violated precedence constraints,
     * assignments without room,
     * one event a day,
     * use of last time of a day,
     * more three or more events consecutively
     */
    public String csvLine() {
        return 
            distanceToFeasibility()+","+
            precedenceViolations(false)+(precedenceViolations(false)>0 && sPrecedenceViolationWeight!=5000?"/"+sPrecedenceViolationWeight:"")+","+
            noRoomViolations(false)+(noRoomViolations(false)>0 && sNoRoomWeight!=5000?"/"+sNoRoomWeight:"")+","+
            super.csvLine();
    }
    
    /**
     * Distance to feasibility
     * @return sum of numbers of students of unassigned events
     */
    public int distanceToFeasibility() {
        if (nrUnassignedVariables()==0) return 0;
        int df=0;
        for (Enumeration e=unassignedVariables().elements();e.hasMoreElements();) {
            TimEvent event = (TimEvent)e.nextElement();
            df+=event.students().size();
        }
        return df;
    }
    
    /** Solution info -- added solution criteria (soft constraint violations)*/
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Precedence violations",String.valueOf(precedenceViolations(false)));
        info.put("No room violations",String.valueOf(noRoomViolations(false)));
        return info;
    }
    
    /** Solution info -- added solution criteria (soft constraint violations, precise computation)*/
    public Hashtable getExtendedInfo() {
        Hashtable info = super.getExtendedInfo();
        info.put("Precedence violations [p]",String.valueOf(precedenceViolations(true)));
        info.put("No room violations [p]",String.valueOf(noRoomViolations(true)));
        info.put("Distance to feasibility",String.valueOf(distanceToFeasibility()));
        return info;
    }

    /** List of precedence constraints */
    public Vector getPrecedences() {
        return iPrecedences;
    }
    
    /** Number of violated precedence constraints */
    public int precedenceViolations(boolean precise) {
        if (!precise) return iPrecedenceViolations;
        int violations = 0;
        for (Enumeration e=iPrecedences.elements();e.hasMoreElements();) {
            TimPrecedence precedence = (TimPrecedence)e.nextElement();
            if (precedence.isHardPrecedence()) continue;
            if (!precedence.isSatisfied())
                violations++;
                //violations+=Math.min(((TimEvent)precedence.first()).students().size(),((TimEvent)precedence.second()).students().size());
        }
        return violations;
    }
    
    /**
     * Number of assignments without room, i.e., placements with {@link TimLocation#room()} set to null
     * (use of assignments without is only permitted when parameter Tim.AllowNoRoom is set to true)
     */
    public int noRoomViolations(boolean precise) {
        if (!precise) return iNoRoomViolations;
        int violations = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            TimEvent event = (TimEvent)e.nextElement();
            TimLocation location = (TimLocation)event.getAssignment();
            if (location.room()==null) violations++;//violations+=event.students().size();
        }
        return violations;
    }

    /**
     * Update counters on unassignment of an event
     */
    public void afterUnassigned(long iteration, Value value) {
        super.afterUnassigned(iteration, value);
        TimLocation location = (TimLocation)value;
        TimEvent event = (TimEvent)location.variable();
        iPrecedenceViolations -= location.precedenceViolations();
        if (location.room()==null) iNoRoomViolations--;//-=event.students().size();
    }
    
    /**
     * Update counters on assignment of an event
     */
    public void beforeAssigned(long iteration, Value value) {
        super.beforeAssigned(iteration, value);
        TimLocation location = (TimLocation)value;
        TimEvent event = (TimEvent)location.variable();
        iPrecedenceViolations += location.precedenceViolations();
        if (location.room()==null) iNoRoomViolations++;//+=event.students().size();
    }
    
    /**
     * Set precedence constraint violations weight, assignments without room weight to 5000.
     * Also, unassign events that have no rooms or violate precedence constraints (unless
     * system property unassign is set to false).
     */
    public void makeFeasible() {
        sNoRoomWeight = 5000;
        sPrecedenceViolationWeight = 5000;
        if ("true".equals(System.getProperty("unassign","true"))) {
            sLog.info("**RESULT** V:"+nrAssignedVariables()+"/"+variables().size()+", P:"+Math.round(getTotalValue())+" ("+csvLine()+")");
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                TimEvent event = (TimEvent)e.nextElement();
                TimLocation loc = (TimLocation)event.getAssignment();
                if (loc==null) continue;
                if (loc.room()==null) event.unassign(0);
            }
            for (Enumeration e=getPrecedences().elements();e.hasMoreElements();) {
                TimPrecedence pr = (TimPrecedence)e.nextElement();
                if (pr.isSatisfied()) continue;
                TimEvent first = (TimEvent)pr.first();
                TimEvent second = (TimEvent)pr.second();
                if (first.students().size()<second.students().size())
                    first.unassign(0);
                else
                    second.unassign(0);
            }
            sLog.info("**RESULT** V:"+nrAssignedVariables()+"/"+variables().size()+", P:"+Math.round(getTotalValue())+" ("+csvLine()+")");
        }
    }
    
    /** Always print a csv line*/
    public boolean cvsPrint() {
        return true;
    }
}
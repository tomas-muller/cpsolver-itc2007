package net.sf.cpsolver.itc.tim.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;

import net.sf.cpsolver.itc.ItcModel;

/**
 * International Timetabling Competition 2002 Model. The {@link TimModel} is an extension
 * of this model. For more details about this problem see 
 * <a href='http://www.idsia.ch/Files/ttcomp2002/'>http://www.idsia.ch/Files/ttcomp2002/</a>.
 * <br><br>
 * Problems of this model can be solved using ttcomp02 property file, i.e.,
 * <pre><code>
 * java -Xmx256m -jar itc2007.jar ttcomp02 data\ttcomp02\competition01.tim competition01.sln
 * </code></pre>
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
public class TTComp02Model extends ItcModel<TimEvent, TimLocation> {
    /** Room constraints */
    protected List<TimRoom> iRooms = null;
    /** Student constraints */
    protected List<TimStudent> iStudents = null;
    
    /** Constructor */
    public TTComp02Model() {
        super();
        setContextUpdateType(ContextUpdateType.AfterUnassignedBeforeAssigned);
    }
    
    /**
     * Prohibited times are not allowed (there are no availabilities in this model) 
     */
    public boolean isAllowProhibitedTime() {
        return false;
    }
    
    /**
     * Assignments without rooms are not allowed 
     */
    public boolean isAllowNoRoom() {
        return false;
    }

    /**
     * Load problem file (see <a href='http://www.idsia.ch/Files/ttcomp2002/IC_Problem/node7.html'>Input Format</a> for more details).
     */
    public boolean load(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        
        StringTokenizer stk = new StringTokenizer(in.readLine(), " ");
        int nrEvents = Integer.parseInt(stk.nextToken());
        int nrRooms = Integer.parseInt(stk.nextToken());
        int nrFeatures = Integer.parseInt(stk.nextToken());
        int nrStudents = Integer.parseInt(stk.nextToken());
        
        iRooms = new ArrayList<TimRoom>();
        for (int i=0;i<nrRooms;i++)
            iRooms.add(new TimRoom(i, Integer.parseInt(in.readLine())));
        
        for (int i=0;i<nrEvents;i++)
            addVariable(new TimEvent(i));
        
        iStudents = new ArrayList<TimStudent>();
        for (int i=0;i<nrStudents;i++)
            iStudents.add(new TimStudent(i));
        
        for (TimStudent student: iStudents) {
            for (TimEvent event: variables()) {
                if (1 == Integer.parseInt(in.readLine())) {
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
        
        for (TimRoom room: iRooms) {
            for (TimEvent event: variables()) {
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
                
        for (TimStudent student: iStudents)
            addConstraint(student);

        for (TimRoom room: iRooms)
            addConstraint(room);
        
        for (TimEvent event: variables())
            event.init(getEmptyAssignment(), isAllowProhibitedTime(),isAllowNoRoom());
        
        in.close();
        
        return true;
    }
    
    /** List of room constraints */
    public List<TimRoom> rooms() {
        return iRooms;
    }
    
    /** List of student constraints */
    public List<TimStudent> students() {
        return iStudents;
    }

    /** Save solution into the given file. Same format is used for TTComp02 (see 
     * <a href='http://www.idsia.ch/Files/ttcomp2002/IC_Problem/Output_format.htm'>Output Format</a>)
     * as well as track 2 of ITC2007 (see <a href='http://www.cs.qub.ac.uk/itc2007/postenrolcourse/course_post_index_files/outputformat.htm'>
     * Output Format</a>).
     */ 
    public boolean save(Assignment<TimEvent, TimLocation> assignment, File file) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        
        for (TimEvent event: variables()) {
            TimLocation location = assignment.getValue(event);
            out.println(location==null?"-1 -1":location.time()+" "+(location.room()==null?-1:location.room().getId()));
        }
        
        out.flush(); out.close();
        
        return true;
    }
    
    /**
     * Return solution value
     * @param oneDay include single event on a day criteria
     * @param lastTime include last slot of the day criteria
     * @param threeMore include more than two events consecutively criteria
     */
    public int score(Assignment<TimEvent, TimLocation> assignment, boolean oneDay, boolean lastTime, boolean threeMore) {
        int score = 0;
        for (TimStudent student: iStudents)
            score += student.score(assignment, oneDay, lastTime, threeMore);
        return score;
    }
    
    /**
     * Solution value
     */
    public double getTotalValue(Assignment<TimEvent, TimLocation> assignment) {
        return ((TTComp02Context)getContext(assignment)).getTotalValue();
    }
    
    /** Solution info -- add values of solutions criteria */
    public Map<String, String> getInfo(Assignment<TimEvent, TimLocation> assignment) {
    	Map<String, String> info = super.getInfo(assignment);
    	TTComp02Context context = (TTComp02Context)getContext(assignment);
        info.put("One time", String.valueOf(context.oneTimePenalty()));
        info.put("Last time", String.valueOf(context.lastTimePenalty()));
        info.put("Three or more times", String.valueOf(context.threeOrMoreTimesPenalty()));
        return info;
    }
    
    /** Solution info -- add values of solutions criteria (precise computation) */
    public Map<String, String> getExtendedInfo(Assignment<TimEvent, TimLocation> assignment) {
    	Map<String, String> info = super.getExtendedInfo(assignment);
        info.put("One time [p]", String.valueOf(score(assignment, true, false, false)));
        info.put("Last time [p]", String.valueOf(score(assignment, false, true, false)));
        info.put("Three or more times [p]", String.valueOf(score(assignment, false, false, true)));
        return info;
    }
    
    /** CSV header: 
     * 1d for one event a day,
     * lt for use of last time of a day,
     * 3+ for more three or more events consecutively
     */
    public String csvHeader() { 
        return "1d,lt,3+";
    }
    
    /** CSV solution line:
     * one event a day,
     * use of last time of a day,
     * more three or more events consecutively
     */
    public String csvLine(Assignment<TimEvent, TimLocation> assignment) { 
        return ((TTComp02Context)getContext(assignment)).csvLine();
    }
    
    /** Load solution from given file */
    public boolean loadSolution(File file, Assignment<TimEvent, TimLocation> assignment) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        
        for (TimEvent event: variables()) {
            StringTokenizer s = new StringTokenizer(in.readLine()," ");
            int time = Integer.parseInt(s.nextToken());
            int roomId = Integer.parseInt(s.nextToken());
            if (time<0) continue;
            TimRoom room = null;
            for (TimRoom r: rooms())
                if (r.getId()==roomId) { room = r; break; }
            assignment.assign(0, new TimLocation(event, time, room));
        }
        
        in.close();
        
        return true;
    }
    
    public class TTComp02Context implements AssignmentConstraintContext<TimEvent, TimLocation> {
        /** Internal counter of objectives */
        private int iScore[] = new int[] {0,0,0};

    	public TTComp02Context(Assignment<TimEvent, TimLocation> assignment) {
    		for (TimEvent event: variables()) {
    			TimLocation location = assignment.getValue(event);
    			if (location != null)
    				assigned(assignment, location);
    		}
    	}

        /**
         * Update counters on assignment of an event
         */
		@Override
		public void assigned(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
	        int[] score = location.score(assignment);
	        iScore[0] += score[0];
	        iScore[1] += score[1];
	        iScore[2] += score[2];
		}

	    /**
	     * Update counters on unassignment of an event
	     */
		@Override
		public void unassigned(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
	        int[] score = location.score(assignment);
	        iScore[0] -= score[0];
	        iScore[1] -= score[1];
	        iScore[2] -= score[2];
		}

	    /** Value of single event on a day criterion */
	    public int oneTimePenalty() {
	        return iScore[0];
	    }

	    /** Value of last slot of the day criterion */
	    public int lastTimePenalty() {
	        return iScore[1];
	    }

	    /** Value of more than two events consecutively criterion */
	    public int threeOrMoreTimesPenalty() {
	        return iScore[2];
	    }
	    
	    /**
	     * Solution value
	     */
	    public double getTotalValue() {
	        return iScore[0]+iScore[1]+iScore[2];
	    }
	    
	    /** CSV solution line:
	     * one event a day,
	     * use of last time of a day,
	     * more three or more events consecutively
	     */
	    public String csvLine() { 
	        return 
	            iScore[0]+","+
	            iScore[1]+","+
	            iScore[2];
	    }
    }

	@Override
	public AssignmentConstraintContext<TimEvent, TimLocation> createAssignmentContext(Assignment<TimEvent, TimLocation> assignment) {
		return new TTComp02Context(assignment);
	}

	@Override
	public Assignment<TimEvent, TimLocation> createAssignment(int index, Assignment<TimEvent, TimLocation> assignment) {
		return new TimAssignment(this, index, assignment);
	}
}
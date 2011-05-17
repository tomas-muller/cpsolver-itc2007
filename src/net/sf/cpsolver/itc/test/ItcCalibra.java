package net.sf.cpsolver.itc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.test.ItcRace.RaceContext;
import net.sf.cpsolver.itc.test.ItcRace.RaceTestInstance;

/**
 * An interface for <A href='http://opalo.epsig.uniovi.es/~adenso/file_d.html'>WinCalibra</A>.
 * It reads the given PARAM.DAT file and writes COST.DAT at the end.
 * <br><br>
 * Additional parameters:
 * <ul>
 *  <li>Calibra.Name ... some name of the parameter optimization (e.g., ctt-sa)
 *  <li>Calibra.Register ... server:port of the register service ({@link ItcTestRegister}) when it is 
 *  to be used to solve problems on various machines in parallel. Values for these
 *  parameters will be read from PARAM.DAT file.
 *  <li>Calibra.Params ... comma separated list of parameters that are being optimized by calibra
 *  <li>Calibra.Labels ... comma separated list of parameter abbreviations
 *  <li>Calibra.NrAttempts ... number of runs of each instance with a set of parameters
 *  <li>Calibra.NrInstances ... number of instances to run
 *  <li>Calibra.FirstInstance ... index of the first instance to run (default is 1)
 *  <li>Calibra.InstanceFile ... input file of an instance (with %% to be replaced by instance number)
 *  <li>Calibra.InstanceFormat ... number format to be placed in the input file (e.g., 00 for 01, 02, 03, ...)
 * </ul>
 * <br><br>
 * Example race configuration (to be named calibra.properties and stored in the folder from which the calibra will be run):
 * <code>
 * #Implementation<br>
 * Model.Class=net.sf.cpsolver.itc.ctt.model.CttModel<br>
 * Model.Extension=out<br>
 * <br>
 * #Termination condition<br>
 * Termination.Class=net.sf.cpsolver.ifs.termination.GeneralTerminationCondition<br>
 * Termination.StopWhenComplete=false<br>
 * Termination.TimeOut=288<br>
 * <br>
 * #Neighbour selection criterion<br>
 * Neighbour.Class=net.sf.cpsolver.itc.heuristics.search.ItcSimulatedAnnealing<br>
 * SimulatedAnnealing.InitialTemperature=[0.5;1.0;1.5;2.0;3.0;4.0#IT]<br>
 * SimulatedAnnealing.CoolingRate=[0.90...0.98,0.02#CR]<br>
 * SimulatedAnnealing.TempLengthCoef=[1,2,5,10#TL]<br>
 * SimulatedAnnealing.ReheatLengthCoef=[2,5,10#RL]<br>
 * SimulatedAnnealing.Neighbours=net.sf.cpsolver.itc.ctt.neighbours.CttTimeMove;net.sf.cpsolver.itc.ctt.neighbours.CttRoomMove;net.sf.cpsolver.itc.ctt.neighbours.CttSwapMove;net.sf.cpsolver.itc.ctt.neighbours.CttCourseRoomChangeMove;net.sf.cpsolver.itc.ctt.neighbours.CttCourseMinDaysMove;net.sf.cpsolver.itc.ctt.neighbours.CttCurriculumCompactnessMove@0.1<br>
 * <br>
 * #Calibra<br>
 * Calibra.Register=oregano.smas.purdue.edu:1200<br>
 * Calibra.Params=SimulatedAnnealing.InitialTemperature,SimulatedAnnealing.CoolingRate,SimulatedAnnealing.TempLengthCoef,SimulatedAnnealing.ReheatLengthCoef<br>
 * Calibra.Labels=IT,CR,TL,RH<br>
 * Calibra.Name=ctt-sa<br>
 * Calibra.NrAttempts=1<br>
 * Calibra.NrInstances=6<br>
 * Calibra.InstanceFile=data/comp05.ctt<br>
 * Calibra.InstanceFormat=00
 * </code>
 * <br><br>
 * Example usage (calibra.bat to be run by WinCalibra):<br>
 * <ul>
 * java -cp itc2007.jar -Xmx256m net.sf.cpsolver.itc.test.ItcCalibra
 * </ul>
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
public class ItcCalibra {
    private static Logger sLog = Logger.getLogger(ItcCalibra.class);
    private DataProperties iProperties;
    private String[] iParam;
    private String[] iLabel;
    private double[] iValue;
    
    /** Constructor */
    public ItcCalibra() throws Exception {
        iProperties = new DataProperties();
        iProperties.load(new FileInputStream("calibra.properties"));
        iProperties.putAll(System.getProperties());
        ItcTestClient.init(iProperties.getProperty("Calibra.Register"));
        setupLogging(new File(iProperties.getProperty("Calibra.Name","itc")+".log"), false);
        sLog.warn("-------------------------------------------------------------------------");
        sLog.warn("Params:");
        BufferedReader r = new BufferedReader(new FileReader("PARAM.DAT"));
        iParam = new String[5]; iValue = new double[5];
        iLabel = new String[5];
        StringTokenizer stk = new StringTokenizer(iProperties.getProperty("Calibra.Params",""),",");
        StringTokenizer stk2 = new StringTokenizer(iProperties.getProperty("Calibra.Labels",""),",");
        for (int i=0;i<5;i++) {
            String line = r.readLine();
            iValue[i] = Double.parseDouble(line.trim());
            iParam[i] = (stk.hasMoreTokens()?stk.nextToken():"P"+(i+1));
            iLabel[i] = (stk2.hasMoreTokens()?stk2.nextToken():iParam[i]);
            iProperties.setProperty(iParam[i], String.valueOf(iValue[i]));
            sLog.warn("  "+iParam[i]+"="+iValue[i]);
        }
    }
    
    private String paramNames() {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<5;i++) {
            if (i>0) sb.append(",");
            sb.append(iLabel[i]);
        }
        return sb.toString();
    }
    
    private String paramValues() {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<5;i++) {
            if (i>0) sb.append(",");
            sb.append(iValue[i]);
        }
        return sb.toString();
    }
    
    /** Perform the test */
    public void test() throws Exception {
        RaceContext cx = new RaceContext(iProperties, "Calibra");
        List<RaceTestInstance> instances = new ArrayList<RaceTestInstance>();
        for (int a=0;a<cx.getNrAttemts();a++) {
            for (int i=0;i<cx.getNrInstances();i++) {
                instances.add(new RaceTestInstance(cx,a,i,iProperties));
            }
        }
        ItcTestClient.test(instances);
        PrintWriter pw = new PrintWriter(new FileWriter("COST.DAT"));
        for (int a=0;a<cx.getNrAttemts();a++) {
            //pw.println(cx.getAvgValue(a)); pw.flush();
            sLog.warn("  Attempt "+(1+a)+": average="+cx.getAvgValue(a)+", nrTests="+cx.getNrTests(a));
        }
        pw.println(cx.getMinValue()); pw.flush();
        sLog.warn("Test min value: "+cx.getMinValue()+" ----------------------------------------------");
        pw.close();
        for (int i=0;i<cx.getNrInstances();i++) {
            sLog.warn("  Instance "+cx.getInstance(i)+": average="+cx.getInstanceAvgValue(i)+
                    ", nrTests="+cx.getInstanceNrTests(i)+", min="+cx.getInstanceMinValue(i)+", seed="+cx.getInstanceMinSeed(i));
        }
        try {
            File csvFile = new File(iProperties.getProperty("Calibra.Name","race")+".csv");
            boolean header = !csvFile.exists();
            PrintWriter w = new PrintWriter(new FileWriter(csvFile,true));
            if (header) {
                w.print(paramNames());
                for (int i=0;i<cx.getNrInstances();i++) {
                    w.print(",Avg"+cx.getInstanceNumber(i));
                    w.print(",#Tst"+cx.getInstanceNumber(i));
                    if (cx.getNrAttemts()>1) w.print(",Min"+cx.getInstanceNumber(i));
                    w.print(",Seed"+cx.getInstanceNumber(i));
                }
                w.println(",NrTests,AvgValue"+(cx.getNrAttemts()>1?",TotMin":""));
            }
            w.print(paramValues());
            double min = 0;
            for (int i=0;i<cx.getNrInstances();i++) {
                w.print(","+cx.getInstanceAvgValue(i));
                w.print(","+cx.getInstanceNrTests(i));
                if (cx.getNrAttemts()>1) {
                    w.print(","+cx.getInstanceMinValue(i));
                    min += cx.getInstanceMinValue(i);
                }
                w.print(","+cx.getInstanceMinSeed(i));
            }
            w.println(","+cx.getNrTests()+","+cx.getAvgValue()+(cx.getNrAttemts()>1?","+min:""));
            w.flush(); w.close();
        } catch (IOException e) {
            sLog.error(e.getMessage(),e);
        }
    }
    
    private void setupLogging(File logFile, boolean debug) {
        Logger root = Logger.getRootLogger();
        ConsoleAppender console = new ConsoleAppender(new PatternLayout("%m%n"));//%-5p %c{1}> %m%n
        console.setThreshold(Level.INFO);
        root.addAppender(console);
        try {
            FileAppender file = new FileAppender(
                    new PatternLayout("%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n"), 
                    logFile.getPath(), 
                    true);
            file.setThreshold(Level.DEBUG);
            root.addAppender(file);
        } catch (IOException e) {
            sLog.fatal("Unable to configure logging, reason: "+e.getMessage(), e);
        }
        if (!debug) root.setLevel(Level.WARN);
    }

    /** Main method -- no parameters */
    public static void main(String[] args) {
        try {
            new ItcCalibra().test();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}

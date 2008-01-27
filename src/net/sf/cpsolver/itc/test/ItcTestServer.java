package net.sf.cpsolver.itc.test;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.ItcTest;

/**
 * Test server available to run ITC 2007 problems. In order
 * to use multiple computers, this class is used to solver problems on multiple
 * machines.
 * <br><br>
 * The server listens on the first available port from 1200, it is registered to 
 * {@link ItcTestRegister} (address:port of the register service are the
 * only paremeter of this program).
 * <br><br>
 * Example usage:<br>
 * <ul>
 * java -cp itc2007.jar -Xmx256m net.sf.cpsolver.itc.test.ItcTestServer some.server.my:1200
 * </ul>
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
public class ItcTestServer {
    private static Logger sLog = (Logger)Logger.getLogger(ItcTestServer.class);
    private static String sRegister = null;
    private static int sPort = 1200;
    private static ServerSocket sServer = null;
    
    /** Answer a request
     * <ul>
     *  <li>SOLVER instance properties ... solve given instance using given parameters
     * </ul>
     */
    public static Object answer(Object[] query) {
        if (query.length>0 && "SOLVE".equals(query[0])) {
            String instance = (String)query[1];
            DataProperties properties = (DataProperties)query[2];
            long seed = ((Long)query[3]).longValue();
            long timeout = ((Long)query[4]).longValue();
            Solution solution = ItcTest.test(instance, properties, seed, timeout);
            if (System.getProperty("timeout")!=null)
                timeout = Long.parseLong(System.getProperty("timeout"));
            if (solution!=null)
                return new Double(solution.getBestValue() + 1000*solution.getModel().getBestUnassignedVariables());
            else
                return null;
        }
        sLog.error("Unknown query: "+(query==null?"null":query.length>0?query[0]==null?"null":query[0].toString():query.toString()));
        return null;
    }
    
    /** Answer a request
     * <ul>
     *  <li>SOLVER instance properties ... solve given instance using given parameters
     *  <li>QUIT ... terminate service
     * </ul>
     */
    public static Object answer(Object query) {
        if ("QUIT".equals(query)) {
            System.exit(0);
        }
        if (query instanceof Object[])
            return answer((Object[])query);
        else
            return answer(new Object[] {query});
    }
    
    /**
     * Perform registration
     */
    public static boolean register() {
        try {
            String host = sRegister.substring(0, sRegister.indexOf(':'));
            int port = Integer.parseInt(sRegister.substring(sRegister.indexOf(':')+1));
            Socket client = new Socket(host, port);
            ItcRemoteIO.writeObject(client, new String[] {"ADD",String.valueOf(sPort)});
            boolean success = "ACK".equals(ItcRemoteIO.readObject(client)); 
            ItcRemoteIO.writeObject(client, null);
            client.close();
            return success;
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            return false;
        }
    }
    
    /**
     * Unregister test server
     */
    public static boolean unregister() {
        try {
            String host = sRegister.substring(0, sRegister.indexOf(':'));
            int port = Integer.parseInt(sRegister.substring(sRegister.indexOf(':')+1));
            Socket client = new Socket(host, port);
            ItcRemoteIO.writeObject(client, new String[] {"REM",String.valueOf(sPort)});
            boolean success = "ACK".equals(ItcRemoteIO.readObject(client)); 
            ItcRemoteIO.writeObject(client, null);
            client.close();
            return success;
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            return false;
        }
    }
    
    /**
     * Service termination hook -- unregister service  
     */
    public static class ShutdownHook extends Thread {
        public ShutdownHook() {
            setName("ShutdownHook");
        }
        public void run() {
            if (!unregister())
                sLog.warn("Unregistration failed.");
            else
                sLog.info("Server unregistered.");
            try {
                sServer.close();
            } catch (Exception e) {}
            sLog.info("Server shutdown.");
        }
    }
    
    /** Service startup */
    public static boolean start() {
        while (sServer==null) {
            try {
                sServer = new ServerSocket(sPort);
            } catch (Exception e) {
                sPort++;
            }
        }

        ItcTest.setupLogging(new File("server_"+sPort+".log"), false, false);
        
        sLog.info("Server listening at port "+sServer.getLocalPort());
        boolean success = register(); 
        if (!success)
            sLog.warn("Registration failed.");
        else
            sLog.info("Server registered.");
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        
        return success;
    }
    
    /** Main program -- server:port of the registration service is the only parameter */
    public static void main(String[] args) {
        sRegister = args[0];
        
        if (!start()) System.exit(1);
        
        while (true) {
            try {
                Socket client = sServer.accept();
                sLog.info("Client "+client.getInetAddress()+" connected.");
                try {
                    Object query = null;
                    while ((query = ItcRemoteIO.readObject(client))!=null) {
                        ItcRemoteIO.writeObject(client, answer(query));
                    }
                } catch (Exception e) {
                    sLog.error(e.getMessage(),e);
                }
                try {
                    client.close();
                } catch (Exception e) {}
                sLog.info("Client "+client.getInetAddress()+" disconnected.");
            } catch (Exception e) {
                sLog.error(e.getMessage(),e);
                break;
            }
        }
    }
}

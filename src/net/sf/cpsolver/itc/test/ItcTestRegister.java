package net.sf.cpsolver.itc.test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeSet;


/**
 * Register service for multiple test servers {@link ItcTestServer}. In order
 * to use multiple computers, this class is used to coordinate communication
 * between test servers {@link ItcTestServer} that run on different machine
 * and test client {@link ItcTestClient} that runs the tests.
 * <br><br>
 * The register service listens on the first available port from 1200.
 * <br><br>
 * Example usage:<br>
 * <pre><code>
 * java -cp itc2007.jar -Xmx256m net.sf.cpsolver.itc.test.ItcTestRegister
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
public class ItcTestRegister {
    private static int sPort = 1200;
    private static ServerSocket sServer = null;
    public static TreeSet<String> sServers = new TreeSet<String>(); 
    
    /** Answer a request
     * <ul>
     *  <li>ADD server:port ... register a test server
     *  <li>REM server:port ... unregister a test server
     *  <li>LIST ... list registered test servers
     * </ul>
     */
    public static Object answer(Object[] query, Socket client) {
        if (query.length>=2 && "ADD".equals(query[0])) {
            System.out.println("Server "+client.getInetAddress().getHostName()+":"+query[1]+" registered.");
            synchronized (sServers) {
                sServers.add(client.getInetAddress().getHostName()+":"+query[1]);
            }
            return "ACK";
        }
        if (query.length>=2 && "REM".equals(query[0])) {
            System.out.println("Server "+client.getInetAddress().getHostName()+":"+query[1]+" unregistered.");
            synchronized (sServers) {
                sServers.remove(client.getInetAddress().getHostName()+":"+query[1]);
            }
            return "ACK";
        }
        if (query.length>=1 && "LIST".equals(query[0])) {
            return sServers;
        }
        System.out.println("Unknown query: "+(query==null?"null":query.length>0?query[0]==null?"null":query[0].toString():query.toString()));
        return null;
    }
    
    /** Answer a request
     * <ul>
     *  <li>ADD server:port ... register a test server
     *  <li>REM server:port ... unregister a test server
     *  <li>LIST ... list registered test servers
     * </ul>
     */
    public static Object answer(Object query, Socket client) {
        if (query instanceof Object[])
            return answer((Object[])query, client);
        else
            return answer(new Object[] {query}, client);
    }
    
    /**
     * Terminate service
     */
    public static void halt(String server) {
        try {
            String host = server.substring(0, server.indexOf(':'));
            int port = Integer.parseInt(server.substring(server.indexOf(':')+1));
            Socket client = new Socket(host, port);
            ItcRemoteIO.writeObject(client, "QUIT");
            client.close();
        } catch (Exception e) {
            System.out.println("ERROR: "+e.getMessage());
        }
    }
    
    /** Service termination shutdown hook -- terminate all registered test servers */
    public static class ShutdownHook extends Thread {
        public ShutdownHook() {
            setName("ShutdownHook");
        }
        public void run() {
            synchronized (sServers) {
                for (String server: sServers) {
                    halt(server);
                }
            }
            try {
                sServer.close();
            } catch (Exception e) {}
            System.out.println("Server shutdown.");
        }
    }
    
    /** Service startup */
    public static void start() {
        while (sServer==null) {
            try {
                sServer = new ServerSocket(sPort);
            } catch (Exception e) {
                sPort++;
            }
        }
        System.out.println("Register service listening at port "+sServer.getLocalPort());
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }    
    
    /** Main class */
    public static void main(String[] args) {
        start();
        while (true) {
            try {
                Socket client = sServer.accept();
                System.out.println("Client "+client.getInetAddress()+" connected.");
                try {
                    Object query = null;
                    while ((query = ItcRemoteIO.readObject(client))!=null) {
                        ItcRemoteIO.writeObject(client, answer(query, client));
                    }
                } catch (Exception e) {
                    System.out.println("Error: "+e.getMessage());
                }
                try {
                    client.close();
                } catch (Exception e) {}
                System.out.println("Client "+client.getInetAddress()+" disconnected.");
            } catch (Exception e) {
                System.out.println("Error: "+e.getMessage());
                break;
            }
        }
    }
}

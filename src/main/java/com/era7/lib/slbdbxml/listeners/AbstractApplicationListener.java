/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.lib.slbdbxml.listeners;

import com.era7.lib.communication.util.ActiveSessions;
import com.era7.lib.communication.util.SessionTimeoutChecker;
import com.era7.lib.bdbxmlapi.ActiveBdbxmlManagerBuffer;
import com.era7.lib.bdbxmlapi.BdbxmlManager;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.util.SessionAttributes;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


/**
 *
 *
 * @author Pablo Pareja Tobes
 * @author Eduardo Pareja Tobes
 */
public abstract class AbstractApplicationListener implements ServletContextListener{

    protected final static int SESSION_TIMER_PERIOD = 60000;
    protected static Timer SESSION_TIMER = null;

    @Override
    public final void contextInitialized(ServletContextEvent sce) {
        try {

            ActiveSessions.init();

            TimerTask timeoutChecker = new SessionTimeoutChecker();

            SESSION_TIMER = new Timer();
            SESSION_TIMER.schedule(timeoutChecker,0,SESSION_TIMER_PERIOD);


            //---------------JDBC CONNECTION CONFIGURATION-----------------------
            configureDBConnectionInitializationParams();

            //---------------BERKELEY DBXML MANAGER CONFIGURATION--------------------------
            configureBdbxmlInitializationParams();
            /*BdbxmlManager manager = BdbxmlManagerFactory.getNewManager();
            sce.getServletContext().setAttribute(ServletContextAttributes.BERKELEY_DBXML_MANAGER_ATTRIBUTE, manager);
            ActiveBdbxmlManagerBuffer.addBerkeleyDbxmlManager(manager);*/
            //---------------------------------------------------------------------

            contextInitializedHandler(sce.getServletContext());

        }catch (Exception ex) {
            Logger.getLogger(AbstractApplicationListener.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @Override
    public final void contextDestroyed(ServletContextEvent sce)
    {        
        contextDestroyedHandler(sce.getServletContext());

        BdbxmlManager.close();
        BdbxmlManager.closeEnvironment();
    }

    /**
     * Method called when the servlet context has been initialized
     * @param context Servlet context
     */
    protected abstract void contextInitializedHandler(ServletContext context);
    /**
     * Method called when the servlet context has been destroyed
     * @param context Servlet context
     */
    protected abstract void contextDestroyedHandler(ServletContext context);

    /**
     * Initialization parameters for the Environment, container, configs... etc
     *  must be set up here
     */
    protected abstract void configureBdbxmlInitializationParams();

    /**
     * Initialization parameters for the DBConnection must be set up here
     */
    protected abstract void configureDBConnectionInitializationParams();

}

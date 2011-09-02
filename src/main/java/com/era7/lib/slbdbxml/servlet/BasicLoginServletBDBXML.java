package com.era7.lib.slbdbxml.servlet;

import com.era7.lib.bdbxmlapi.ActiveBdbxmlManagerBuffer;
import com.era7.lib.bdbxmlapi.BdbxmlManager;
import com.era7.lib.bdbxmlapi.BdbxmlManagerFactory;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.util.ActiveSessions;
import java.sql.Connection;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import com.era7.lib.communication.util.SessionAttributes;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import com.era7.lib.era7jdbcapi.DataBaseException;
import com.era7.lib.era7jdbcapi.MysqlConnection;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * Abstract class that should be extended for a Login service implementation
 * It does all the work for you including creating the session, storing the user permissions, etc...
 * <br>There are only three methods that must be implemented:
 * <br> 1.-  The login authentication itself
 * <br> 2.-  Storing as many objects as wished in the session (using the class {@link SessionAttributes} in the process)
 * <br> 3.-  Retrieving the permissions of the user for the application
 * @author Pablo Pareja Tobes
 *
 */
public abstract class BasicLoginServletBDBXML extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * Customizable message used when the login operation was NOT successful
     */
    public static String LOGIN_NOT_SUCCESSFUL_MESSAGE = "The user and/or password provided are not correct.";
    /**
     * Parameter name used in the GET/POST request to pass the xml request
     */
    public static String PARAMETER_NAME = "request";
    /**
     * Flag indicating whether the servlet should log the successful operations
     */
    public boolean loggableFlag = false;
    /**
     * Flag indicating whether the servlet should log the errors
     */
    public boolean loggableErrorsFlag = false;
    /**
     * Flag indicating whether the servlet should retrieve a connection with the DB system and pass
     * it to the processRequest() method or not (The sevlet logic does not require connecting to a DB).
     * If mysqlConnectionNeededFlag is set to false in the init method of the servlet, the instance
     * of {@link java.sql.Connection} passed to the processRequest() method will be <code>null</code>
     */
    public boolean mysqlConnectionNeededFlag = true;


    @Override
    public final void init() {

        loggableErrorsFlag = defineLoggableErrorsFlag();
        loggableFlag = defineLoggableFlag();
        mysqlConnectionNeededFlag = defineMysqlConnectionNeededFlag();

        initServlet();

    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        servletLogic(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        servletLogic(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Basic login service";
    }

    /**
     * Logic for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request Servlet request
     * @param response Servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private final void servletLogic(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        BasicSession session = null;
        Connection connection = null;
        BdbxmlManager manager = null;

        String requestString = (String) request.getParameter(PARAMETER_NAME);

        Request myRequest = null;
        Response myResponse = new Response();

        try {

            myRequest = new Request(requestString);

            if (mysqlConnectionNeededFlag) {

                if (MysqlConnection.SESSION_GUIDED_CONNECTIONS_FLAG) {
                    System.out.println("BasicLoginServletBDBXML: Getting the mysql loginConnection");
                    connection = MysqlConnection.getLoginConnection();
                } else {
                    //---> getting a new connection <--
                    System.out.println("BasicLoginServletBDBXML: Getting a new mysql connection");
                    connection = MysqlConnection.getNewConnection();
                }
            }

            manager = BdbxmlManager.getManager();

            boolean success = processLoginRequest(myRequest, connection, manager);

            if (success) {
                myResponse.setStatus(Response.SUCCESSFUL_RESPONSE);

                //--->Creating the session<---
                session = ActiveSessions.createNewSession();

                //--->Storing the objects in the session<---
                storeObjectsInSession(myRequest, connection, session);


                myResponse.setSessionID(session.getSessionId());


                //--->Storing the SESSION_ID in the session<----
                session.setAttribute(SessionAttributes.SESSION_ID_ATTRIBUTE, session.getSessionId());

                //--->Storing the user permissions<---
                session.setAttribute(SessionAttributes.PERMISSIONS_ATTRIBUTE, defineUserPermissions(myRequest, connection, manager));

                if (MysqlConnection.SESSION_GUIDED_CONNECTIONS_FLAG && mysqlConnectionNeededFlag) {
                    //---->Storing a new connection for the Session<----
                    session.setAttribute(SessionAttributes.CONNECTION_ATTRIBUTE, MysqlConnection.getNewConnection());
                }

            } else {
                myResponse.setStatus(Response.ERROR_RESPONSE);
                myResponse.setError(LOGIN_NOT_SUCCESSFUL_MESSAGE);
            }


            //--> Assigning the request id to its response
            myResponse.setId(myRequest.getId());
            //--> Assigning the request method to its response
            myResponse.setMethod(myRequest.getMethod());


            if (myResponse.getStatus().equals(Response.ERROR_RESPONSE)) {
                myRequest.detach();
                myResponse.setRequestSource(myRequest);
            } else {
                myResponse.setStatus(Response.SUCCESSFUL_RESPONSE);
            }


            if (loggableFlag) {

                if (myResponse.getStatus().equals(Response.SUCCESSFUL_RESPONSE)) {
                    /*
                     * The call to logSuccessfulOperation will include as many parameters as needed
                     * to perform the successful logging operation.
                     * (For example, the logged user could be passed as a parameter)
                     *
                     * this.logSuccessfulOperation(myRequest,myResponse,connection,user);
                     *
                     */
                    this.logSuccessfulOperation(myRequest, myResponse, connection, session, manager);
                } else if (myResponse.getStatus().equals(Response.ERROR_RESPONSE)) {
                    /*
                     * The call to logSuccessfulOperation will include as many parameters as needed
                     * to perform the error logging operation.
                     * (For example, the logged user could be passed as a parameter)
                     *
                     * this.logErrorResponseOperation(myRequest,myResponse,connection,user);
                     *
                     */
                    this.logErrorResponseOperation(myRequest, myResponse, connection, session, manager);
                }
            }

            response.setContentType("text/html");
            // write response
            PrintWriter writer = response.getWriter();
            writer.println(myResponse.toString());
            writer.close();

        } catch (Throwable e) {
            e.printStackTrace();
            if (loggableErrorsFlag) {
                /*
                 * The call to logErrorExceptionOperation will include as many parameters as needed
                 * to perform the error exception logging operation.
                 * (For example, the logged user could be passed as a parameter)
                 *
                 * this.logErrorExceptionOperation(myRequest,myResponse, user, e,connection);
                 *
                 */
                this.logErrorExceptionOperation(myRequest, myResponse, e, connection, manager);
            }

        } finally {

            if (!MysqlConnection.SESSION_GUIDED_CONNECTIONS_FLAG && connection != null) {
                try {
                    MysqlConnection.closeConnection(connection);
                } catch (DataBaseException e) {
                    System.out.println(e.toString());
                }
            }
            //temporal----> ActiveBdbxmlManagerBuffer.closeBdbxmlManager(manager);
        }
    }

    /**
     * This method must be implemented in order to define the loggable flag
     * @return True if requests must be logged, false otherwise.
     */
    protected abstract boolean defineLoggableFlag();

    /**
     * This method must be implemented in order to define the loggable errors flag
     * @return True if error requests must be logged, false otherwise.
     */
    protected abstract boolean defineLoggableErrorsFlag();

    /**
     * This method must be implemented in order to define the dbConnectionNeeded flag
     * @return True if this servlet needs a JDBC connection
     */
    protected abstract boolean defineMysqlConnectionNeededFlag();

    public abstract void initServlet();

    /**
     * Method to include the logic for the login process
     * @param request Object request
     * @param connection Connection with the DB
     * @return The login was successful
     */
    abstract protected boolean processLoginRequest(Request request, Connection connection,
            BdbxmlManager manager);

    /**
     * Method for storing the objects needed in the application
     * @param request Object request
     * @param connection Object request
     * @param session Valid session where the objects should be stored. (Always using the class SessionAttributes)
     */
    abstract protected void storeObjectsInSession(Request request, Connection connection, 
            BasicSession session);

    /**
     *
     * @param request Request object
     * @param connection Connection with the DB
     * @return An ArrayList of any kind of Objects representing the permissions associated with the user logged in
     * the application
     */
    abstract protected ArrayList<?> defineUserPermissions(Request request, Connection connection,
            BdbxmlManager manager);

    /**
     * Method called when the operation has been performed successfully plus the flag
     * 'loggableFlag' is true
     */
    protected abstract void logSuccessfulOperation(Request request, Response response, Connection connection,
            BasicSession session, BdbxmlManager manager);

    /**
     * Method called when the operation could not be performed because of an error ocurred
     * in the processRequest method
     * This method is called as long as 'loggableErrorsFlag' is true
     */
    protected abstract void logErrorResponseOperation(Request request, Response response, Connection connection,
            BasicSession session, BdbxmlManager manager);

    /**
     * Method called when the operation could not be performed because of an exception ocurred
     * This method is called as long as 'loggableErrorsFlag' is true
     */
    protected abstract void logErrorExceptionOperation(Request request, Response response, Throwable e, 
            Connection connection, BdbxmlManager manager);
}

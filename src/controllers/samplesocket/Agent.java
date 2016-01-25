package controllers.samplesocket;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;


import java.io.*;
import java.net.*;


//Paul - general includes for I don't know what buffered image and imageio and stuff
//don't care, jsut need it compiling
//this language can suck it
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.List;
import java.util.Arrays;
import javax.imageio.*;
import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {

    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;

    /**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];

    /**
     * block size
     */
    protected int block_size;

    protected Dimension pixelDimensions;
    protected Socket clientSocket;
    protected DataOutputStream socketOutStream;
    protected BufferedReader socketInStream;


    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        randomGenerator = new Random();
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();
        pixelDimensions = so.getWorldDimension();

        try {

            String sentence;
            String modifiedSentence;
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

            clientSocket = new Socket("localhost", 48674);
            socketOutStream = new DataOutputStream(clientSocket.getOutputStream());
            socketInStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            sentence = inFromUser.readLine();
            socketOutStream.writeBytes(sentence + '\n');
            modifiedSentence = socketInStream.readLine();
            System.out.println(modifiedSentence);

            // modifiedSentence = socketInStream.readLine();


            System.out.println("Agent allowed forward");
         }
        catch(Exception e)
        {

        }
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions();
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        ArrayList<Observation>[] resourcesPositions = stateObs.getResourcesPositions();
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions();
        grid = stateObs.getObservationGrid();

        /*printDebug(npcPositions,"npc");
        printDebug(fixedPositions,"fix");
        printDebug(movingPositions,"mov");
        printDebug(resourcesPositions,"res");
        printDebug(portalPositions,"por");
        System.out.println();               */

        Types.ACTIONS action = null;
        StateObservation stCopy = stateObs.copy();

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit)
        {
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
            int index = randomGenerator.nextInt(actions.size());
            action = actions.get(index);

            stCopy.advance(action);
            if(stCopy.isGameOver())
            {
                stCopy = stateObs.copy();
            }

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        return action;
    }

    /**
     * Prints the number of different types of sprites available in the "positions" array.
     * Between brackets, the number of observations of each type.
     * @param positions array with observations.
     * @param str identifier to print
     */
    private void printDebug(ArrayList<Observation>[] positions, String str)
    {
        if(positions != null){
            System.out.print(str + ":" + positions.length + "(");
            for (int i = 0; i < positions.length; i++) {
                System.out.print(positions[i].size() + ",");
            }
            System.out.print("); ");
        }else System.out.print(str + ": 0; ");
    }

        /*
     *  Create a BufferedImage for Swing components.
     *  The entire component will be captured to an image.
     *
     *  @param  component Swing component to create image from
     *  @return image the image for the given region
    */
    public static BufferedImage createImage(JComponent component)
    {
        Dimension d = component.getSize();

        if (d.width == 0 || d.height == 0)
        {
            d = component.getPreferredSize();
            component.setSize( d );
        }

        Rectangle region = new Rectangle(0, 0, d.width, d.height);
        return Agent.createImage(component, region);
    }

    /*
     *  Create a BufferedImage for Swing components.
     *  All or part of the component can be captured to an image.
     *
     *  @param  component Swing component to create image from
     *  @param  region The region of the component to be captured to an image
     *  @return image the image for the given region
    */
    public static BufferedImage createImage(JComponent component, Rectangle region)
    {
        //  Make sure the component has a size and has been layed out.
        //  (necessary check for components not added to a realized frame)

        if (! component.isDisplayable())
        {
            Dimension d = component.getSize();

            if (d.width == 0 || d.height == 0)
            {
                d = component.getPreferredSize();
                component.setSize( d );
            }

            layoutComponent( component );
        }

        BufferedImage image = new BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        //  Paint a background for non-opaque components,
        //  otherwise the background will be black

        if (! component.isOpaque())
        {
            g2d.setColor( component.getBackground() );
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.translate(-region.x, -region.y);
        component.paint( g2d );
        g2d.dispose();
        return image;
    }

    static void layoutComponent(Component component)
    {
        synchronized (component.getTreeLock())
        {
            component.doLayout();

            if (component instanceof Container)
            {
                for (Component child : ((Container)component).getComponents())
                {
                    layoutComponent(child);
                }
            }
        }
    }

    /**
     * Gets the player the control to draw something on the screen.
     * It can be used for debug purposes.
     * @param g Graphics device to draw to.
     */
    public void screenShot(BufferedImage img, int width, int height)
    {
        try {


        // socketOutStream.writeBytes("begin" + "\n");
            // int width = (int)pixelDimensions.getWidth();
            // int height = (int)pixelDimensions.getHeight();
            // int width = (int)jc.getWidth();
            // int height = (int)jc.getHeight();
        // System.out.println("W " + width + " he " + height);



        // BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        // String sentence = inFromUser.readLine();

            // BufferedImage img = Agent.createImage(jc);

        // Dimension size = //Toolkit.getDefaultToolkit().getScreenSize();
        // BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // g.drawImage(img, null, width, height);
        // Graphics2D img2d = img.createGraphics();
        // Graphics2D g2d = img.getGraphics();

        //  Paint a background for non-opaque components,
        //  otherwise the background will be black
        // jc.paint( img.getGraphics() );
        // g2d.dispose();
       

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "JPEG", baos);               

            baos.close();
            System.out.println("Write byte size = " + baos.size());
            socketOutStream.write((Integer.toString(baos.size()) + "\n").getBytes());

            //send the stream now
            socketOutStream.write(baos.toByteArray());
            System.out.println("Image sent, now info");

            socketOutStream.write((Integer.toString(width) + "," + Integer.toString(height) + "\n").getBytes());


            // socketOutStream.writeBytes("end" + "\n");

            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            String sentence = inFromUser.readLine();

        }
        catch (Exception e)
        {
            System.out.println("Error writing image");
            System.out.println(e.getMessage());

        }
    }

    public void draw(Graphics2D g)
    {

        int half_block = (int) (block_size*0.5);
        for(int j = 0; j < grid[0].length; ++j)
        {
            for(int i = 0; i < grid.length; ++i)
            {
                if(grid[i][j].size() > 0)
                {
                    Observation firstObs = grid[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    int print = firstObs.category; //firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i*block_size+half_block,j*block_size+half_block);
                }
            }
        }
    }
}

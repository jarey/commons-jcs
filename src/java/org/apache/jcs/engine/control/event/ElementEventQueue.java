package org.apache.jcs.engine.control.event;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jcs.engine.control.event.behavior.IElementEventQueue;
import org.apache.jcs.engine.control.event.behavior.IElementEventHandler;
import org.apache.jcs.engine.control.event.behavior.IElementEvent;
import org.apache.jcs.engine.control.event.behavior.IElementEventConstants;


/**
 * An event queue is used to propagate ordered cache events to one and only one
 * target listener.
 */
public class ElementEventQueue implements IElementEventQueue
{
    private final static Log log = LogFactory.getLog( ElementEventQueue.class );

    private static int processorInstanceCount = 0;

    private String cacheName;

    private boolean destroyed = false;
    private Thread t;

    // Internal queue implementation

    private Object queueLock = new Object();

    // Dummy node

    private Node head = new Node();
    private Node tail = head;

    /**
     * Constructor for the ElementEventQueue object
     *
     * @param cacheName
     */
    public ElementEventQueue(  String cacheName  )
    {

        this.cacheName = cacheName;

        t = new QProcessor();
        t.start();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Constructed: " + this );
        }
    }

    /**
     * Event Q is emtpy.
     */
    public synchronized void destroy()
    {
        if ( !destroyed )
        {
            destroyed = true;

            // sychronize on queue so the thread will not wait forever,
            // and then interrupt the QueueProcessor

            synchronized ( queueLock )
            {
                t.interrupt();
            }

            t = null;

            log.info( "Element event queue destroyed: " + this );
        }
    }

    /**
     * @return the region name for the event queue
     */
    public String toString()
    {
        return "cacheName=" + cacheName;
    }

    /**
     * @return The destroyed value
     */
    public boolean isAlive()
    {
        return ( !destroyed );
    }



    /**
     * Adds an  ElementEvent  to be handled
     *
     * @param hand The IElementEventHandler
     * @param event The IElementEventHandler IElementEvent event
     */
    public void addElementEvent( IElementEventHandler hand, IElementEvent event )
      throws IOException
    {

        if ( log.isDebugEnabled() )
        {
          log.debug( "Adding Event Handler to QUEUE, !destroyed = " + !destroyed );
        }

        if ( !destroyed )
        {
            ElementEventRunner runner = new ElementEventRunner( hand, event );
            put( runner );
        }
    }


    /**
     * Adds an event to the queue.
     *
     * @param event
     */
    private void put( AbstractElementEventRunner event )
    {
        Node newNode = new Node();

        newNode.event = event;

        synchronized ( queueLock )
        {
            tail.next = newNode;
            tail = newNode;

            queueLock.notify();
        }
    }

    private AbstractElementEventRunner take() throws InterruptedException
    {
        synchronized ( queueLock )
        {
            // wait until there is something to read

            while ( head == tail )
            {
              if ( log.isDebugEnabled() )
              {
                log.debug( "Waiting for something to come in to the Q" );
              }

                queueLock.wait();
            }

            // we have the lock, and the list is not empty

            Node node = head.next;

            AbstractElementEventRunner value = head.event;

            // Node becomes the new head (head is always empty)

            node.event = null;
            head = node;

            return value;
        }
    }

    ///////////////////////////// Inner classes /////////////////////////////

    private static class Node
    {
        Node next = null;
        AbstractElementEventRunner event = null;
    }

    /**
     * @author asmuts
     * @created January 15, 2002
     */
    private class QProcessor extends Thread
    {
        /**
         * Constructor for the QProcessor object
         */
        QProcessor()
        {
            super( "ElementEventQueue.QProcessor-" + ( ++processorInstanceCount ) );

            setDaemon( true );
        }

        /**
         * Main processing method for the QProcessor object
         */
        public void run()
        {
            AbstractElementEventRunner r = null;

            while ( !destroyed )
            {
                try
                {
                    r = take();
                }
                catch ( InterruptedException e )
                {
                    // We were interrupted, so terminate gracefully.

                    this.destroy();
                }

                if ( !destroyed && r != null )
                {
                    r.run();
                }
            }

            log.info( "QProcessor exiting for " + ElementEventQueue.this );
        }
    }

    /**
     * Retries before declaring failure.
     *
     * @author asmuts
     * @created January 15, 2002
     */
    private abstract class AbstractElementEventRunner implements Runnable
    {
        /**
         * Main processing method for the AbstractElementEvent object
         */
        public void run()
        {
            IOException ex = null;


                try
                {
                    ex = null;
                    doRun();
                    return;
                    // happy and done.
                }
                catch ( IOException e )
                {
                    ex = e;
                }


            // Too bad.  The handler has problems.
            if ( ex != null )
            {
                log.warn( "Giving up element event handling " + ElementEventQueue.this, ex );

            }
            return;
        }

        /**
         * Description of the Method
         *
         * @exception IOException
         */
        protected abstract void doRun()
            throws IOException;
    }

    /**
     * @author asmuts
     * @created January 15, 2002
     */
    private class ElementEventRunner extends AbstractElementEventRunner
    {

        private IElementEventHandler hand;
        private IElementEvent event;

        /**
         * Constructor for the PutEvent object
         *
         * @param ice
         * @exception IOException
         */
        ElementEventRunner( IElementEventHandler hand, IElementEvent event )
            throws IOException
        {
              if ( log.isDebugEnabled() )
              {
                log.debug( "Constructing " + this );
              }
            this.hand = hand;
            this.event = event;
        }

        /**
         * Description of the Method
         *
         * @exception IOException
         */
        protected void doRun()
            throws IOException
        {

          hand.handleElementEvent( event );
        }
    }

}




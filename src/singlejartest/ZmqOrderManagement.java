package singlejartest;

import com.dukascopy.api.*;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ZmqOrderManagement implements IStrategy {
    private IEngine engine = null;
    private IIndicators indicators = null;
    private int tagCounter = 0;
    private double[] ma1 = new double[Instrument.values().length];
    private IConsole console;
    private Socket socket;
    private ZContext zContext;

    static ZmqListener listener;

    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        indicators = context.getIndicators();
        this.console = context.getConsole();
        console.getOut().println("Started " + ZmqQouteServer.class.getSimpleName());

        zContext = new ZContext();
        socket = zContext.createSocket(ZMQ.REP);
        socket.bind("tcp://127.0.0.1:43002");

        listener = new ZmqListener(socket, context);
        Thread thread = new Thread(listener);
        thread.start();

    }

    public void onStop() throws JFException {
        console.getOut().println("Stopped " + ZmqQouteServer.class.getSimpleName());
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {

    }


    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }
}


package singlejartest;

import com.dukascopy.api.*;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ZmqQouteServer implements IStrategy {
    private IEngine engine = null;
    private IIndicators indicators = null;
    private int tagCounter = 0;
    private double[] ma1 = new double[Instrument.values().length];
    private IConsole console;
    private Socket socket;
    private ZContext zContext;

    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        indicators = context.getIndicators();
        this.console = context.getConsole();
        console.getOut().println("Started " + ZmqQouteServer.class.getSimpleName());

        zContext = new ZContext();
        socket = zContext.createSocket(ZMQ.PUB);
        socket.bind("tcp://127.0.0.1:43001");
    }

    public void onStop() throws JFException {
        console.getOut().println("Stopped " + ZmqQouteServer.class.getSimpleName());
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        if(period == Period.ONE_MIN) {

            String bar = period.toString() + " " +
                    Long.toString(bidBar.getTime()) + " " +
                    Double.toString(bidBar.getOpen()) + " " +
                    Double.toString(bidBar.getHigh()) + " " +
                    Double.toString(bidBar.getLow()) + " " +
                    Double.toString(bidBar.getClose()) + " " +
                    Double.toString(bidBar.getVolume()) + " " +
                    Double.toString(askBar.getOpen()) + " " +
                    Double.toString(askBar.getHigh()) + " " +
                    Double.toString(askBar.getLow()) + " " +
                    Double.toString(askBar.getClose()) + " " +
                    Double.toString(askBar.getVolume()) + " " +
                    instrument.toString();

            console.getOut().println(bar);
            socket.sendMore(instrument.toString());
            socket.send(bar);
        }
    }


    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }
}

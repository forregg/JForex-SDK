package singlejartest;

import com.dukascopy.api.*;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.zeromq.ZMQ.Socket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.*;

class ZmqListener
        implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ZmqListener.class);

    private IEngine engine = null;
    private Socket socket = null;
    private IContext context;

    private static String delimiter = "--";



    public ZmqListener(Socket socket, IContext context)
    {
        this.socket = socket;
        this.context = context;
        this.engine = context.getEngine();
    }
    public void run()
    {
        LOGGER.info("started");

        while (!Thread.currentThread().isInterrupted())
        {
            String request = new String(socket.recv());
            String[] parts = request.split(delimiter);

            String command = parts[0];

            switch (command)
            {
                case "sendOrder":
                    sendOrder(parts);
                    break;

                case "closeOrder":
                    closeOrder(parts);
                    break;

                case "getOrders":
                    getOrders(parts);
                    break;

                case "getPositions":
                    getPositions(parts);
                    break;

                case "getHistory":
                    getHistory(parts);
                    break;

                case "getHistoryFromDateUntilNow":
                    getHistoryFromDateUntilNow(parts);
                    break;

                case "getHistoryFromDateToDate":
                    getHistoryFromDateToDate(parts);
                    break;

                default:
                    socket.send((command+": not implemented yet").getBytes(), 0);
            }

        }

    }

    private void sendOrder(String[] parts)
    {
        final String oStrategyName = parts[1];
        final Instrument oInstrument = Instrument.fromString(parts[2]);
        final IEngine.OrderCommand oOrderCommand = IEngine.OrderCommand.valueOf(parts[3]);
        final Double oAmount = Double.parseDouble(parts[4]);
        final Double oPrice = Double.parseDouble(parts[5]);
        final Double oSlippage = Double.parseDouble(parts[6]);
        final Double oStopLossPrice = Double.parseDouble(parts[7]);
        final Double oTakeProfitPrice = Double.parseDouble(parts[8]);
        final Long oGoodTillTime = Long.parseLong(parts[9]);
        LOGGER.info("sending order");

        context.executeTask(new Callable<IOrder>() {
            @Override
            public IOrder call() throws Exception {
                try {
                    IOrder o = engine.submitOrder(Tools.getLabel(oStrategyName, oInstrument, System.currentTimeMillis()),
                            oInstrument, oOrderCommand, oAmount, oPrice, oSlippage, oStopLossPrice, oTakeProfitPrice, oGoodTillTime);
                    return o;
                }
                catch (Exception e)
                {
                    LOGGER.info("exception!!!");
                    LOGGER.error(e.toString());
                    socket.send((oStrategyName + ": "+e.toString()).getBytes(), 0);
                    //отладить обработку исключений!!!!!!!!!!!!!!!!!!!!
                    return null;
                }
            }
        });

        socket.send((oStrategyName + ": order sent").getBytes(), 0);
    }

    private void closeOrder(String[] parts)
    {
        final String cStrategyName = parts[1];
        final Instrument cInstrument = Instrument.fromString(parts[2]);
        final IEngine.OrderCommand cOrderCommand = IEngine.OrderCommand.valueOf(parts[3]);
        final Double cAmount = Double.parseDouble(parts[4]);
        final Double cPrice = Double.parseDouble(parts[5]);
        final Double cSlippage = Double.parseDouble(parts[6]);
        final Double cStopLossPrice = Double.parseDouble(parts[7]);
        final Double cTakeProfitPrice = Double.parseDouble(parts[8]);
        //final Long cGoodTillTime = Long.parseLong(parts[9]);
        LOGGER.info("closing order");

        context.executeTask(new Callable<IOrder>() {
            @Override
            public IOrder call() throws Exception {
                try {
                    List<IOrder> orders = context.getEngine().getOrders();
                    for (int i = 0; i < orders.size(); i++) {
                        if (Tools.orderFromThisStrategy(orders.get(i), cStrategyName, cInstrument) == true) {
                            if (orders.get(i).getState() == IOrder.State.CANCELED ||
                                    orders.get(i).getState() == IOrder.State.CLOSED)
                                continue;
                            if(orders.get(i).getOrderCommand() == cOrderCommand &&
                                    orders.get(i).getAmount() == cAmount &&
                                    orders.get(i).getOpenPrice() == cPrice &&
                                    orders.get(i).getStopLossPrice() == cStopLossPrice &&
                                    orders.get(i).getTakeProfitPrice() == cTakeProfitPrice)
                                orders.get(i).close();

                        }
                    }
                    return null;
                }
                catch (Exception e)
                {
                    LOGGER.info("exception!!!");
                    LOGGER.error(e.toString());
                    socket.send((cStrategyName + ": "+e.toString()).getBytes(), 0);
                    //отладить обработку исключений!!!!!!!!!!!!!!!!!!!!
                    return null;
                }
            }
        });

        socket.send((cStrategyName + ": order closed").getBytes(), 0);
    }

    private void getOrders(String[] parts)
    {
        String strategyName = parts[1];
        Instrument instrument = Instrument.fromString(parts[2]);
        try {
            List<IOrder> orders = context.getEngine().getOrders();
            String message = "";
            for (int i = 0; i < orders.size(); i++) {
                if (Tools.orderFromThisStrategy(orders.get(i), strategyName, instrument) == true) {
                    if (orders.get(i).getState() == IOrder.State.CANCELED ||
                            orders.get(i).getState() == IOrder.State.CLOSED ||
                            orders.get(i).getState() == IOrder.State.FILLED)
                        continue;
                    IEngine.OrderCommand orderCommand = orders.get(i).getOrderCommand();
                    Double amount = orders.get(i).getAmount();
                    Double price = orders.get(i).getOpenPrice();
                    Double stopLossPrice = orders.get(i).getStopLossPrice();
                    Double takeProfitPrice = orders.get(i).getTakeProfitPrice();
                    Long goodTillTime = orders.get(i).getGoodTillTime();
                    Long openTIme = orders.get(i).getCreationTime();

                    if (message != "")
                        message += "-+-";
                    message += strategyName + "--" + instrument.toString() + "--" +  orderCommand.toString() + "--" +  amount.toString() + "--" +  price.toString() + "--" +
                            stopLossPrice.toString() + "--" +  takeProfitPrice.toString() + "--" +  goodTillTime.toString() + "--" +  openTIme.toString();
                }
            }
            socket.send(message.getBytes(), 0);
        }
        catch (Exception e)
        {
            LOGGER.info("exception!!!");
            LOGGER.error(e.toString());
            socket.send((strategyName + ": "+e.toString()).getBytes(), 0);
            return;
        }
    }

    private void getPositions(String[] parts)
    {
        String strategyName = parts[1];
        Instrument instrument = Instrument.fromString(parts[2]);

        try {
            List<IOrder> orders = context.getEngine().getOrders();
            String message = "";
            for (int i = 0; i < orders.size(); i++) {
                if (Tools.orderFromThisStrategy(orders.get(i), strategyName, instrument) == true) {
                    if (orders.get(i).getState() != IOrder.State.FILLED)
                        continue;
                    IEngine.OrderCommand orderCommand = orders.get(i).getOrderCommand();
                    Double amount = orders.get(i).getAmount();
                    Double price = orders.get(i).getOpenPrice();
                    Double stopLossPrice = orders.get(i).getStopLossPrice();
                    Double takeProfitPrice = orders.get(i).getTakeProfitPrice();
                    Long goodTillTime = orders.get(i).getGoodTillTime();
                    Long openTIme = orders.get(i).getCreationTime();
                    Long fillTime = orders.get(i).getFillTime();

                    if (message != "")
                        message += "-+-";
                    message += strategyName + "--" + instrument.toString() + "--" +  orderCommand.toString() + "--" +  amount.toString() + "--" +  price.toString() + "--" +
                            stopLossPrice.toString() + "--" +  takeProfitPrice.toString() + "--" +  goodTillTime.toString() + "--" +  openTIme.toString() + "--" +  fillTime.toString();
                }
            }
            socket.send(message.getBytes(), 0);
        }
        catch (Exception e)
        {
            LOGGER.info("exception!!!");
            LOGGER.error(e.toString());
            socket.send((strategyName + ": "+e.toString()).getBytes(), 0);
            return;
        }
    }

    private void getHistory(String[] parts)
    {
        String strategyName = parts[1];
        try {
            Instrument instrument = Instrument.fromString(parts[2]);
            Period period = Period.valueOf(parts[3]);
            int barsBefore = Integer.parseInt(parts[4]);
            int shift = Integer.parseInt(parts[5]);
            int barsAfter = Integer.parseInt(parts[6]);
            String filterWeekends = parts[7];

            Filter filter = Filter.NO_FILTER;
            if(filterWeekends == "True")
                filter = Filter.WEEKENDS;

            List<IBar> bidBars = context.getHistory().getBars(instrument, period, OfferSide.BID, filter,
                    barsBefore, context.getHistory().getBar(instrument, period, OfferSide.BID, shift).getTime(), barsAfter); //учитывать, что шифт не фильтрует выходные

            List<IBar> askBars = context.getHistory().getBars(instrument, period, OfferSide.ASK, filter,
                    barsBefore, context.getHistory().getBar(instrument, period, OfferSide.ASK, shift).getTime(), barsAfter);

            if (askBars.size() != bidBars.size())
            {
                LOGGER.error("Ask and bid history have different size!");
                socket.send("".getBytes(), 0);
                return;
            }

            String message = "";

            for (int i = 0; i < bidBars.size(); i++) {
                if (message != "")
                    message += "-+-";



                String bar = period.toString() + "--"+
                        Long.toString(bidBars.get(i).getTime()) + "--"+
                        Double.toString(bidBars.get(i).getOpen()) + "--"+
                        Double.toString(bidBars.get(i).getHigh()) + "--"+
                        Double.toString(bidBars.get(i).getLow()) + "--"+
                        Double.toString(bidBars.get(i).getClose()) + "--"+
                        Double.toString(bidBars.get(i).getVolume()) + "--"+
                        Double.toString(askBars.get(i).getOpen()) + "--"+
                        Double.toString(askBars.get(i).getHigh()) + "--"+
                        Double.toString(askBars.get(i).getLow()) + "--"+
                        Double.toString(askBars.get(i).getClose()) + "--"+
                        Double.toString(askBars.get(i).getVolume()) + "--"+
                        instrument.toString();
                message += bar;
            }
            socket.send(message.getBytes(), 0);
        }
        catch (Exception e)
        {
            LOGGER.info("exception!!!");
            LOGGER.error(e.toString());
            socket.send((strategyName + ": "+e.toString()).getBytes(), 0);
            return;
        }
    }

    private void getHistoryFromDateUntilNow(String[] parts)
    {
        String strategyName = parts[1];
        try {
            Instrument instrument = Instrument.fromString(parts[2]);
            Period period = Period.valueOf(parts[3]);
            String dateFromS = parts[4];



            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");// HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date dateFrom = dateFormat.parse(dateFromS);// 00:00:00");
            long timeTo = context.getHistory().getBar(instrument, period, OfferSide.BID, 0).getTime();

            List<IBar> bidBars = context.getHistory().getBars(instrument, period, OfferSide.BID, dateFrom.getTime(), timeTo);
            List<IBar> askBars = context.getHistory().getBars(instrument, period, OfferSide.ASK, dateFrom.getTime(), timeTo);

            if (askBars.size() != bidBars.size())
            {
                LOGGER.error("Ask and bid history have different size!");
                socket.send("".getBytes(), 0);
                return;
            }

            String message = "";

            for (int i = 0; i < bidBars.size(); i++) {
                if (message != "")
                    message += "-+-";



                String bar = period.toString() + "--"+
                        Long.toString(bidBars.get(i).getTime()) + "--"+
                        Double.toString(bidBars.get(i).getOpen()) + "--"+
                        Double.toString(bidBars.get(i).getHigh()) + "--"+
                        Double.toString(bidBars.get(i).getLow()) + "--"+
                        Double.toString(bidBars.get(i).getClose()) + "--"+
                        Double.toString(bidBars.get(i).getVolume()) + "--"+
                        Double.toString(askBars.get(i).getOpen()) + "--"+
                        Double.toString(askBars.get(i).getHigh()) + "--"+
                        Double.toString(askBars.get(i).getLow()) + "--"+
                        Double.toString(askBars.get(i).getClose()) + "--"+
                        Double.toString(askBars.get(i).getVolume()) + "--"+
                        instrument.toString();
                message += bar;
            }
            socket.send(message.getBytes(), 0);
        }
        catch (Exception e)
        {
            LOGGER.info("exception!!!");
            LOGGER.error(e.toString());
            socket.send((strategyName + ": "+e.toString()).getBytes(), 0);
            return;
        }
    }

    private void getHistoryFromDateToDate(String[] parts)
    {
        String strategyName = parts[1];
        try {
            Instrument instrument = Instrument.fromString(parts[2]);
            Period period = Period.valueOf(parts[3]);
            String dateFromS = parts[4];
            String dateToS = parts[5];


            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date dateFrom = dateFormat.parse(dateFromS);
            Date dateTo = dateFormat.parse(dateToS);

            long currentBarTime = context.getHistory().getBar(instrument, period, OfferSide.BID, 1).getTime(); //1 tak kak uzhe sformirovanniu bar zabiraeb
            long timeTo =  dateTo.getTime();

            if(timeTo > currentBarTime) {
                timeTo = currentBarTime;
            }

            List<IBar> bidBars = context.getHistory().getBars(instrument, period, OfferSide.BID, dateFrom.getTime(), timeTo);
            List<IBar> askBars = context.getHistory().getBars(instrument, period, OfferSide.ASK, dateFrom.getTime(), timeTo);

            if (askBars.size() != bidBars.size())
            {
                LOGGER.error("Ask and bid history have different size!");
                socket.send("".getBytes(), 0);
                return;
            }

            String message = "";

            for (int i = 0; i < bidBars.size(); i++) {
                if (message != "")
                    message += "-+-";



                String bar = period.toString() + "--"+
                        Long.toString(bidBars.get(i).getTime()) + "--"+
                        Double.toString(bidBars.get(i).getOpen()) + "--"+
                        Double.toString(bidBars.get(i).getHigh()) + "--"+
                        Double.toString(bidBars.get(i).getLow()) + "--"+
                        Double.toString(bidBars.get(i).getClose()) + "--"+
                        Double.toString(bidBars.get(i).getVolume()) + "--"+
                        Double.toString(askBars.get(i).getOpen()) + "--"+
                        Double.toString(askBars.get(i).getHigh()) + "--"+
                        Double.toString(askBars.get(i).getLow()) + "--"+
                        Double.toString(askBars.get(i).getClose()) + "--"+
                        Double.toString(askBars.get(i).getVolume()) + "--"+
                        instrument.toString();
                message += bar;
            }
            socket.send(message.getBytes(), 0);
        }
        catch (Exception e)
        {
            LOGGER.info("exception!!!");
            LOGGER.error(e.toString());
            socket.send((strategyName + ": "+e.toString()).getBytes(), 0);
            return;
        }
    }
}

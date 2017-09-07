package singlejartest;

import com.dukascopy.api.*;

public class Tools {
    public static String getLabel(String strategyName, Instrument instrument, long time) {

        String label = strategyName;
        String inst = instrument.name();
        label = label + inst.substring(0, 2) + inst.substring(3, 5);
        label = label + time;
        label = label.toLowerCase();

        return label;
    }
    public static boolean orderFromThisStrategy(IOrder o, String strategyName, Instrument instrument)
    {
        if(o == null)
            return false;

        if(o.getInstrument() == instrument)
        {
            if(strategyName.length() <= o.getLabel().length())
            {
                if(strategyName.equalsIgnoreCase(o.getLabel().substring(0, strategyName.length())) == true)
                {
                    return true;
                }
            }
        }
        else
            return false;

        return false;


    }
}

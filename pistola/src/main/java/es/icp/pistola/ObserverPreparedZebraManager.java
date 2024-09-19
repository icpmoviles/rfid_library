package es.icp.pistola;

import com.zebra.rfid.api3.ReaderDevice;

import java.util.ArrayList;

public interface ObserverPreparedZebraManager {
    void onPrepared(ArrayList<ReaderDevice> lectores);
}

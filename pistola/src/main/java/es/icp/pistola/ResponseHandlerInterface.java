package es.icp.pistola;

import com.zebra.rfid.api3.TagData;

public interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);

        void handleBarcode(Barcode barcode);

        void handleTriggerPress(boolean pressed);
    }
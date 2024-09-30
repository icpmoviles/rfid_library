package es.icp.pistola;

public interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);

        void handleBarcode(Barcode barcode);

        void handleTriggerPress(boolean pressed);
    }
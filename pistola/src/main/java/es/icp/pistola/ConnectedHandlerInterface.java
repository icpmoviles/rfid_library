package es.icp.pistola;

public interface ConnectedHandlerInterface {
        void handleConnection(boolean connected);

        void handleBattery(int level, boolean charging, String cause);

//        void handleTemperatureAlarm(ALARM_LEVEL level, int current, int ambient, String cause);
    }
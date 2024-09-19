package es.icp.pistola;

public interface ConnectedSonHandlerInterface {
        void handleConnection(int connection, int mode);

        void handleBattery(int level, boolean charging, String cause);

//        void handleTemperatureAlarm(ALARM_LEVEL level, int current, int ambient, String cause);
    }
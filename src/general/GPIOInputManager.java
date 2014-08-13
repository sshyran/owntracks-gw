package general;

public class GPIOInputManager {

    public boolean gpio1;
    public boolean gpio3;
    public boolean gpio7;

    public GPIOInputManager() {
        /*
        * GPIO DRIVER ACTIVATION
        * 
        */
        ATManager.getInstance().executeCommand("at^spio=1\r");

        /*
         * Activate
         */
        ATManager.getInstance().executeCommand("at^scpin=1,0,0\r");
        ATManager.getInstance().executeCommand("at^scpin=1,2,0\r");
        ATManager.getInstance().executeCommand("at^scpin=1,6,0\r");

        /*
         * Activate polling
         */
        ATManager.getInstance().executeCommand("at^scpol=1,0\r");
        ATManager.getInstance().executeCommand("at^scpol=1,2\r");
        ATManager.getInstance().executeCommand("at^scpol=1,6\r");

        /*
         * Check initial values
         */
        ATManager.getInstance().executeCommand("at^sgio=0\r");
        ATManager.getInstance().executeCommand("at^sgio=2\r");
        ATManager.getInstance().executeCommand("at^sgio=6\r");
    }
    
    public static GPIOInputManager getInstance() {
        return GPIOInputManagerHolder.INSTANCE;
    }

    private static class GPIOInputManagerHolder {

        private static final GPIOInputManager INSTANCE = new GPIOInputManager();
    }

    public void processSCPOL(String message) {
        if (Settings.getInstance().getSetting("gpioDebug", false)) {
            System.out.println("Received SCPOL: " + message);
        }

        String[] lines = StringSplitter.split(message, "\r\n");
        if (Settings.getInstance().getSetting("gpioDebug", false)) {
            System.out.println("lines.length: " + lines.length);
        }

        if (lines.length < 1) {
            return;
        }

        /*
         * SCPOL event GPIO value changed
         * example: ^SCPOL: 6,1
         */
        if (lines[1].startsWith("^SCPOL: ")) {
            String[] values = StringSplitter.split(lines[1].substring(8), ",");
            if (values.length == 2) {
                int ioID;
                int value;

                try {
                    ioID = Integer.parseInt(values[0]);
                } catch (NumberFormatException e) {
                    ioID = -1;
                }

                try {
                    value = Integer.parseInt(values[1]);
                } catch (NumberFormatException e) {
                    value = -1;
                }

                process(ioID, (value == 1));
            }
        }
    }

    public void processSGIO(String message) {
        if (Settings.getInstance().getSetting("gpioDebug", false)) {
            System.out.println("Received SGIO: " + message);
        }

        String[] lines = StringSplitter.split(message, "\r\n");
        if (Settings.getInstance().getSetting("gpioDebug", false)) {
            System.out.println("lines.length: " + lines.length);
        }

        if (lines.length < 2) {
            return;
        }

        /*
         * AT^SGIO message read
         * example:  AT^SGIO=6
         *           ^SGIO: 1
         *           OK
         */
        if (lines[1].startsWith("^SGIO: ")) {
            String valueString = lines[1].substring(7);
            int value;
            if (valueString.length() > 0) {
                try {
                    value = Integer.parseInt(valueString);
                } catch (NumberFormatException nfe) {
                    System.err.println("SGIO value NumberFormatException");
                    value = -1;
                }
            } else {
                value = -1;
            }

            int ioID;
            int pos = lines[0].indexOf('=');
            if (pos > 0 && lines[0].length() > pos + 1) {
                try {
                    ioID = Integer.parseInt(lines[0].substring(pos + 1, lines[0].length() - 1));
                } catch (NumberFormatException nfe) {
                    System.err.println("SGIO index NumberFormatException");
                    ioID = -1;
                }
            } else {
                ioID = -1;
            }            
            process(ioID, (value == 1));
        }
    }
    
    private void process(int ioID, boolean value) {
        switch (ioID) {
            case 0:
                gpio1 = value;
                SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                    + "/gpio/1",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", false),
                    ("" + value).getBytes()
                );
                break;
            case 2:
                gpio3 = value;
                SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                    + "/gpio/3",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", false),
                    ("" + value).getBytes()
                );
                break;
            case 6:
                gpio7 = value;
                SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                    + "/gpio/7",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", false),
                    ("" + value).getBytes()
                );
                break;
            default:
                break;
        }
    }
}

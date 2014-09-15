package general;

import java.util.Vector;

/**
 *
 * @author christoph krey
 */
public class StringFunc {
    
    public static String[] split(String original, String separator) {
        Vector nodes = new Vector();
        int index = original.indexOf(separator);
        while (index >= 0) {
            nodes.addElement(original.substring(0, index));
            original = original.substring(index + separator.length());
            index = original.indexOf(separator);
        }
        nodes.addElement(original);

        String[] result = new String[nodes.size()];
        if (nodes.size() > 0) {
            for (int loop = 0; loop < nodes.size(); loop++) {
                result[loop] = (String) nodes.elementAt(loop);
            }
        }
        return result;
    }
    
    public static boolean isInStringArray(String string, String[] stringArray) {
        for (int i = 0; i < stringArray.length; i++) {
            if (string.equalsIgnoreCase(stringArray[i])) {
                return true;
            }
        }
        return false;
    }

    public static String replaceString(String originalString, String oldString, String newString) {
        String intermediateString = originalString;
        int indexOldString;

        if (newString.indexOf(oldString) >= 0) {
            return originalString;
        }
        do {
            indexOldString = intermediateString.indexOf(oldString);
            if (indexOldString >= 0) {
                String workString;
                if (indexOldString > 0) {
                    workString = intermediateString.substring(0, indexOldString);
                } else {
                    workString = "";
                }
                workString = workString.concat(newString);
                if (intermediateString.length() > indexOldString + oldString.length()) {
                    workString = workString.concat(intermediateString.substring(indexOldString + oldString.length()));
                }
                intermediateString = workString;
            }
        } while (indexOldString >= 0);
        return intermediateString;
    }


}

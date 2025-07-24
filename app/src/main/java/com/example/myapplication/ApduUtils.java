package com.example.myapplication;

/**
 * Utility class for APDU commands and responses used in the HCE NFC application.
 */
public class ApduUtils {
    
    // ISO-DEP command HEADER for selecting an AID
    private static final String SELECT_APDU_HEADER = "00A40400";
    
    // Status word for unsupported instruction
    public static final byte[] SW_INS_NOT_SUPPORTED = {(byte) 0x6D, (byte) 0x00};
    public static final byte[] SW_SUCCESS = {(byte) 0x90, (byte) 0x00};
    
    // Common APDU command headers
    public static final String GET_DATA_APDU_HEADER = "00CA"; // GET DATA command
    
    // Status word values
    public static final byte[] SW_UNKNOWN = {(byte) 0x6F, (byte) 0x02}; // Unknown/Internal error
    public static final byte[] SW_FILE_NOT_FOUND = {(byte) 0x6A, (byte) 0x82}; // File or application not found
    public static final byte[] SW_INCORRECT_PARAMS = {(byte) 0x6A, (byte) 0x86}; // Incorrect parameters P1-P2
    public static final byte[] SW_WRONG_LENGTH = {(byte) 0x67, (byte) 0x00}; // Wrong length
    public static final byte[] SW_CLA_NOT_SUPPORTED = {(byte) 0x6E, (byte) 0x00}; // Class not supported
    
    /**
     * Converts byte array to hexadecimal string.
     * @param bytes Byte array to convert
     * @return Hexadecimal string representation
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    
    /**
     * Converts hexadecimal string to byte array.
     * @param hex Hexadecimal string to convert
     * @return Byte array representation
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() == 0) {
            return new byte[0];
        }
        
        // Make sure we have an even number of characters
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        
        byte[] result = new byte[hex.length() / 2];
        try {
            for (int i = 0; i < hex.length(); i += 2) {
                result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
        } catch (NumberFormatException e) {
            // Return empty array if parsing fails
            android.util.Log.e("ApduUtils", "Error parsing hex string: " + e.getMessage());
            return new byte[0];
        }
        return result;
    }
    
    /**
     * Concatenates a response data with a status word.
     * @param data Data bytes
     * @param sw Status word
     * @return Combined response
     */
    public static byte[] buildResponse(byte[] data, byte[] sw) {
        if (data == null) {
            return sw;
        }
        
        byte[] response = new byte[data.length + sw.length];
        System.arraycopy(data, 0, response, 0, data.length);
        System.arraycopy(sw, 0, response, data.length, sw.length);
        return response;
    }
    
    /**
     * Checks if a command APDU is a SELECT by AID command.
     * @param apduCommand Command to check
     * @return True if it's a SELECT command
     */
    public static boolean isSelectCommand(byte[] apduCommand) {
        String hexCommand = bytesToHex(apduCommand);
        return hexCommand.startsWith(SELECT_APDU_HEADER);
    }
    
    /**
     * Extracts AID from a SELECT command APDU.
     * @param selectApdu SELECT APDU command
     * @return The AID part of the command, or empty string if not found
     */
    public static String extractAidFromSelect(byte[] selectApdu) {
        if (!isSelectCommand(selectApdu) || selectApdu.length < 6) {
            return "";
        }
        
        // Get the length byte (Lc field)
        int length = selectApdu[4] & 0xFF;
        
        // Check if the command has the right length
        if (selectApdu.length < 6 + length) {
            return "";
        }
        
        // Extract the AID bytes and convert to hex
        byte[] aidBytes = new byte[length];
        System.arraycopy(selectApdu, 5, aidBytes, 0, length);
        return bytesToHex(aidBytes);
    }
} 
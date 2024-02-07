package de.dertoaster.movecraft.bridges;

public class Constants {

    public static final String BRIDGE_SIGN_HEADER = "[Bridge]";
    
    public static class BridgeSignLineIndizes {
    	public static final int BRIDGE_SIGN_INDEX_HEADER = 0;
    	public static final int BRIDGE_SIGN_INDEX_NAME = 1;
    	public static final int BRIDGE_SIGN_INDEX_TARGET_BRIDGE = 2;
    	public static final int BRIDGE_SIGN_INDEX_COORD_MODIFIER = 3;
    }
    
    public static class Lang {
    	public static final String TRANSLATION_KEY_ERROR_NAME_BLANK = "bridge.error.name_blank";
    	public static final String TRANSLATION_KEY_ERROR_NAME_TAKEN = "bridge.error.name_duplicate";
    }
}

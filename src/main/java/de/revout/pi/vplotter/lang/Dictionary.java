package de.revout.pi.vplotter.lang;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Dictionary {

	private ResourceBundle RESOURCE_BUNDLE;

	
	private static Dictionary current;
	
	private Dictionary() {
		
        try {
			RESOURCE_BUNDLE = ResourceBundle.getBundle("de/revout/pi/vplotter/resources/lang/messages", Locale.getDefault()); //$NON-NLS-1$
			
			System.out.println("");
			
		} catch (Exception e1) {
			new RuntimeException(e1.getMessage());
		}
		
	}
	
	public static Dictionary getCurrent() {
		
		if(current==null) {
			current = new  Dictionary();
		}
		
		return current;
	}
	
	public  String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
		
	}
}

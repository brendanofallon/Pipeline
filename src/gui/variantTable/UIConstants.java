package gui.variantTable;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;

/**
 * Home for a few widely-used UI settings
 * @author brendan
 *
 */
public class UIConstants {

	public static final Color backgroundColor = Color.DARK_GRAY;
	
	
	
	/**
	 * Returns true if we're on a mac, false if windows or linux
	 * @return
	 */
	public static boolean isMac() {
		return System.getProperty("os.name").contains("Mac");
	}

	/**
	 * Returns true if we're on a Windows machine (any version), false if mac or linux
	 * @return
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name").contains("Windows");
	}
	
	/**
	 * Return an icon associated with the given url. For instance, if the url is icons/folder.png, we look in the
	 * package icons for the image folder.png, and create and return an icon from it. 
	 * @param url
	 * @return
	 */
	public static ImageIcon getIcon(String url) {
		ImageIcon icon = null;
		try {
			java.net.URL imageURL = UIConstants.class.getResource(url);
			icon = new ImageIcon(imageURL);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return icon;
	}
	
	public static Font getFont(String url) {
		Font font = null;
		try {
			InputStream fontStream = UIConstants.class.getResourceAsStream(url);
			font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			font = font.deriveFont(12f);
		} catch (FontFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return font;
	}

	
}

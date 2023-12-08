package weiqian.hardware;


import android.content.ContentResolver;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.Window;



public class CustomFunctions {

	static private final String UseStaticIP = "ethernet_use_static_ip";
	static private final String StaticIP = "ethernet_static_ip";
	static private final String StaticGateway = "ethernet_static_gateway";
	static private final String StaticNetMask = "ethernet_static_netmask";
	static private final String StaticDNS1 = "ethernet_static_dns1";
	static private final String StaticDNS2 = "ethernet_static_dns2";

	static public void UseStaticIp(ContentResolver contentResolver, String ip, String gateway, String netmask, String dns1, String dns2) {
		Settings.System.putInt(contentResolver, UseStaticIP, 1);
		Settings.System.putString(contentResolver, StaticIP, ip);
		Settings.System.putString(contentResolver, StaticGateway, gateway);
		Settings.System.putString(contentResolver, StaticNetMask, netmask);
		Settings.System.putString(contentResolver, StaticDNS1, dns1);
		Settings.System.putString(contentResolver, StaticDNS2, dns2);
	}

	static public void UseDynamicIp(ContentResolver contentResolver) {
		Settings.System.putInt(contentResolver, UseStaticIP, 0);
		Settings.System.putString(contentResolver, StaticIP, null);
		Settings.System.putString(contentResolver, StaticGateway, null);
		Settings.System.putString(contentResolver, StaticNetMask, null);
		Settings.System.putString(contentResolver, StaticDNS1, null);
		Settings.System.putString(contentResolver, StaticDNS2, null);
	}

	static public void FullScreenSticky(Window window) {
		window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}

	static public void FullScreenNoSticky(Window window) {
		window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	static public String getId(ContentResolver contentResolver) {
		return Secure.getString(contentResolver, Secure.ANDROID_ID);
	}



}

package weiqian.hardware;

import android.content.ContentResolver;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.view.View;
import android.view.Window;

public class CustomFunctions {
	
	static private String UseStaticIP = "ethernet_use_static_ip";
	static private String StaticIP = "ethernet_static_ip";
	static private String StaticGateway = "ethernet_static_gateway";
	static private String StaticNetMask = "ethernet_static_netmask";
	static private String StaticDNS1 = "ethernet_static_dns1";
	static private String StaticDNS2 = "ethernet_static_dns2";
	
	static public void UseStaticIp(ContentResolver contentResolver, String ip, String gateway, String netmask, String dns1, String dns2)
	{
		System.putInt(contentResolver, UseStaticIP, 1);
		System.putString(contentResolver, StaticIP, ip);
		System.putString(contentResolver, StaticGateway, gateway);
		System.putString(contentResolver, StaticNetMask, netmask);
		System.putString(contentResolver, StaticDNS1, dns1);
		System.putString(contentResolver, StaticDNS2, dns2);
	}
	
	static public void UseDynamicIp(ContentResolver contentResolver)
	{
		System.putInt(contentResolver, UseStaticIP, 0);
		System.putString(contentResolver, StaticIP, null);
		System.putString(contentResolver, StaticGateway, null);
		System.putString(contentResolver, StaticNetMask, null);
		System.putString(contentResolver, StaticDNS1, null);
		System.putString(contentResolver, StaticDNS2, null);
	}
	
	static public void FullScreenSticky(Window window)
	{
		window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	             | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	             | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	             | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
	             | View.SYSTEM_UI_FLAG_FULLSCREEN            
	             | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}
	
	static public void FullScreenNoSticky(Window window)
	{
		window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	             | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	             | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	             | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
	             | View.SYSTEM_UI_FLAG_FULLSCREEN            
	             | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}
	
	static public String getId(ContentResolver contentResolver)
	{
		return Secure.getString(contentResolver, Secure.ANDROID_ID);
	}
	
}

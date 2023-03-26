package com.whosgonnapoop;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WhosGonnaPoopTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhosGonnaPoop.class);
		RuneLite.main(args);
	}
}
package com.notloc.tilegrid;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("tilegrid")
public interface TileGridConfig extends Config
{
	@Alpha
	@ConfigItem(
			keyName = "gridcolor",
			name = "Grid Color",
			description = "The color of the tile grid.",
			position = 0
	)
	default Color gridColor() { return new Color(0, 0, 0, 29); }

	@Range(
			min = 1,
			max = 64
	)
	@ConfigItem(
			keyName = "griddistance",
			name = "Draw Distance",
			description = "The max distance from the player to draw the tile grid.",
			position = 2
	)
	default int gridDistance() { return 24; }
}

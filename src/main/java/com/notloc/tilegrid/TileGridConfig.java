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
	default Color gridColor() { return new Color(0, 0, 0, 48); }

	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "griddistance",
			name = "Draw Distance",
			description = "The max distance from the player to draw the tile grid.",
			position = 2
	)
	default int gridDistance() { return 16; }

	@Range(min = 0, max = 64)
	@ConfigItem(
			keyName = "fade-out-dist",
			name = "Fade Out Distance",
			description = "Grid tiles beyond this distance begin to fade out.",
			position = 3
	)
	default int fadeOutDistance() { return 2; }

	@Range(min = 1, max = 32)
	@ConfigItem(
			keyName = "fade-out-taper",
			name = "Fade Out Taper",
			description = "A stronger taper makes tiles take longer to fade out.",
			position = 4
	)
	default int fadeOutTaper() { return 8; }
}

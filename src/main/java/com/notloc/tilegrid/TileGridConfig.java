package com.notloc.tilegrid;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("tilegrid")
public interface TileGridConfig extends Config
{
	@ConfigSection(name = "Style", description = "", position = 0)
	static final String STYLE = "style";

	@ConfigSection(name = "Render Settings", description = "", position = 1)
	static final String RENDER = "render";

	@Alpha
	@ConfigItem(
			keyName = "gridcolor",
			name = "Overworld Color",
			description = "The color of the grid on Gielinor's surface.",
			position = 10,
			section = STYLE
	)
	default Color overworldGridColor() { return new Color(0, 0, 0, 48); }

	@Alpha
	@ConfigItem(
			keyName = "other-grid-color",
			name = "Other Color",
			description = "The color of the grid in caves and dungeons.",
			position = 15,
			section = STYLE
	)
	default Color otherGridColor() { return new Color(160, 160, 160, 48); }

	@Range(min = 1, max = 10)
	@ConfigItem(
			keyName = "line-thickness",
			name = "Line Thickness",
			description = "The width of the grid lines in pixels.",
			position = 20,
			section = STYLE
	)
	default int lineThickness() { return 1; }

	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "griddistance",
			name = "Draw Distance",
			description = "The radius around the player to draw the grid.",
			position = 20,
			section = RENDER
	)
	default int gridDistance() { return 20; }

	@ConfigItem(
			keyName = "do-walkable-check",
			name = "Walkable Area Only",
			description = "Only show the grid where the player can walk?<br>NOTE: sometimes unreachable areas are considered walkable",
			position = 25,
			section = RENDER
	)
	default boolean doWalkableCheck() { return true; }

	@ConfigItem(
			keyName = "do-fade-out",
			name = "Fade Out?",
			description = "Should the grid fade out with distance?",
			position = 30,
			section = RENDER
	)
	default boolean doFadeOut() { return true; }

	@Range(min = 0, max = 64)
	@ConfigItem(
			keyName = "fade-out-dist",
			name = "Fade Out Distance",
			description = "The minimum distance before the grid starts to fade out.",
			position = 40,
			section = RENDER
	)
	default int fadeOutDistance() { return 8; }

	@Range(min = 1, max = 32)
	@ConfigItem(
			keyName = "fade-out-taper",
			name = "Fade Out Taper",
			description = "Controls how fast the grid fades out.<br>Lower values make the grid fade out faster.<br> Higher values make the grid fade out slower.",
			position = 50,
			section = RENDER
	)
	default int fadeOutTaper() { return 8; }
}

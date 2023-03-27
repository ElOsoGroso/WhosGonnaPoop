package com.whosgonnapoop;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class WhosGonnaPoopOverlay extends Overlay {
    private final WhosGonnaPoop plugin;
    private final WhosGonnaPoopConfig config;

    private final Client client;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public WhosGonnaPoopOverlay(WhosGonnaPoop plugin, WhosGonnaPoopConfig config, Client client, ItemManager itemManager, InfoBoxManager infoBoxManager, TooltipManager tooltipManager, ModelOutlineRenderer modelOutlineRenderer) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        this.modelOutlineRenderer = modelOutlineRenderer;
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        for (Player player : client.getPlayers())
        {
            if(plugin.isinKephri(player) && plugin.currentNames().stream().filter(x->x.equals(player.getName())).findFirst().isPresent()){
            Color outlineColor = new Color(102,51,0);
            modelOutlineRenderer.drawOutline(player, 6, outlineColor, 2);
            }


        }
        return null;
    }
}
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
    public static BufferedImage resizeImage(BufferedImage image, int newWidth, int newHeight)
    {
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = scaledImage.createGraphics();
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaledImage;
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
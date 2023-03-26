package com.whosgonnapoop.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.whosgonnapoop.WhosGonnaPoop;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class WhosGonnaPoopPanel extends PluginPanel
{
    private final WhosGonnaPoop plugin;

    private final JPanel contentPanel;

    private ResetButton clearButton;
    private ResetButton clearButton2;

    private JTextArea createTextArea(String text)
    {
        JTextArea textArea = new JTextArea(5, 22);
        textArea.setText(text);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setOpaque(false);

        return textArea;
    }
    @Inject
    public WhosGonnaPoopPanel(WhosGonnaPoop plugin)
    {
        super(false);
        this.plugin = plugin;

        JLabel title = new JLabel("Who's Gonna Poop?");
        title.setBorder(new EmptyBorder(0, 0, BORDER_OFFSET, 0));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel = new JPanel();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
        add(title, BorderLayout.NORTH);
        create1Button();
        create2Button();
        add(contentPanel, BorderLayout.CENTER);
    }

    private void create1Button()
    {
        clearButton = new ResetButton("1 Pooper");
        clearButton.setPreferredSize(new Dimension(PANEL_WIDTH, 30));
        clearButton.addMouseButton1PressedHandler(() -> {plugin.howManyPoopers = 1; plugin.resetPoopers();});
        contentPanel.add(clearButton);

    }
    private void create2Button()
    {
        clearButton2 = new ResetButton("2 Poopers");
        clearButton2.setPreferredSize(new Dimension(PANEL_WIDTH, 30));
        clearButton2.addMouseButton2PressedHandler(() -> {plugin.howManyPoopers = 2; plugin.resetPoopers();});
        contentPanel.add(clearButton2);

    }



}
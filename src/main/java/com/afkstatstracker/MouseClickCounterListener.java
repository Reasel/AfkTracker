package com.afkstatstracker;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.input.MouseAdapter;

public class MouseClickCounterListener extends MouseAdapter
{
    private final List<Long> clickTimestamps = new ArrayList<>();
    private final Client client;
    MouseClickCounterListener(Client client)
    {
        this.client = client;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent)
    {
        addClick();
        return mouseEvent;
    }

    public List<Long> getClickCounter() { return this.clickTimestamps; }

    public void addClick()
    {
        this.clickTimestamps.add(System.currentTimeMillis());
    }

    public void resetMouseClickCounterListener()
    {
        this.clickTimestamps.clear();
    }
}
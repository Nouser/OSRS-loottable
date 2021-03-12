package com.loottable.controllers;

import java.awt.event.ActionEvent;

import java.util.function.Consumer;

import com.loottable.helpers.OsrsBoxApi;
import com.loottable.helpers.UiUtilities;
import com.loottable.views.LootTablePluginPanel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

public class LootTableController {
    private ClientToolbar clientToolbar;
    private LootTablePluginPanel lootTablePluginPanel;
    private NavigationButton navButton;
    private String monsterName;

    final private String LOOT_TABLE_MENU_OPTION = "Loot Table";

    public LootTableController(ClientToolbar clientToolbar, ItemManager itemManager) {
        this.clientToolbar = clientToolbar;
        Consumer<String> onSearchBarTextChangedListener = text -> onSearchBarTextChanged(text);
        lootTablePluginPanel = new LootTablePluginPanel(itemManager,
                (ActionEvent event) -> onSearchButtonPressed(event), onSearchBarTextChangedListener);
        setUpNavigationButton();
        this.monsterName = null;
    }

    public void onPluginShutdown() {
        clientToolbar.removeNavigation(navButton);
    }

    /**
     * Adds "Loot Table" option if "Attack" option is present
     * 
     * @todo issue with players when "Attack" option is available
     */
    public void onMenuOpened(MenuOpened event, Client client) {
        final NPC[] cachedNPCs = client.getCachedNPCs();
        MenuEntry[] menuEntries = event.getMenuEntries();

        for (MenuEntry menuEntry : menuEntries) {
            int id = menuEntry.getIdentifier();

            if (id < cachedNPCs.length) {
                NPC target = cachedNPCs[id];

                if (target != null) {
                    int combatLevel = target.getCombatLevel();
                    // If combatLevel is greater than 0, assume able to attack
                    if (combatLevel > 0) {
                        int widgetId = menuEntry.getParam1();
                        String monsterName = menuEntry.getTarget();
                        final MenuEntry lootTableMenuEntry = new MenuEntry();
                        lootTableMenuEntry.setOption(LOOT_TABLE_MENU_OPTION);
                        lootTableMenuEntry.setTarget(monsterName);
                        lootTableMenuEntry.setIdentifier(menuEntry.getIdentifier());
                        lootTableMenuEntry.setParam1(widgetId);
                        lootTableMenuEntry.setType(MenuAction.RUNELITE.getId());
                        client.setMenuEntries(ArrayUtils.addAll(menuEntries, lootTableMenuEntry));
                    }
                }
            }
        }
    }

    public void onMenuOptionClicked(MenuOptionClicked event, Client client) {
        final NPC[] cachedNPCs = client.getCachedNPCs();

        if (event.getMenuOption().equals(LOOT_TABLE_MENU_OPTION)) {
            int eventId = event.getId();
            if (eventId < cachedNPCs.length) {
                NPC target = cachedNPCs[eventId];
                int targetId = target.getId();
                this.monsterName = target.getName();

                JSONArray dropTable = OsrsBoxApi.getMonsterDropTable(targetId);
                lootTablePluginPanel.rebuildPanel(this.monsterName, dropTable);
            }
        }

    }

    public void onSearchButtonPressed(ActionEvent event) {
        int id = OsrsBoxApi.getMonsterId(this.monsterName);

        if (id == 0) {
            lootTablePluginPanel.rebuildPanel(this.monsterName, null);
            return;
        }

        JSONArray dropTable = OsrsBoxApi.getMonsterDropTable(id);
        lootTablePluginPanel.rebuildPanel(this.monsterName, dropTable);
    }

    public void onSearchBarTextChanged(String text) {
        this.monsterName = text;
    }

    private void setUpNavigationButton() {
        navButton = NavigationButton.builder().tooltip("Loot Table")
                .icon(ImageUtil.loadImageResource(getClass(), UiUtilities.lootTableNavIcon)).priority(5)
                .panel(lootTablePluginPanel).build();
        clientToolbar.addNavigation(navButton);
    }
}
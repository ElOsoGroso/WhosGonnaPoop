package com.whosgonnapoop;

import com.whosgonnapoop.ui.WhosGonnaPoopPanel;
import com.google.inject.Provides;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import static java.lang.Math.abs;
import static java.lang.Math.max;

@Slf4j
@PluginDescriptor(
		name = "Whos Gonna Poop?",
		description = "A plugin to keep track of who has pooped and should poop next in Kephri",
		tags = {"poop", "kephri", "toa", "tombs","amascut","hotkey"}
)
public class WhosGonnaPoop extends Plugin
{
	@Getter
	public ArrayList<PlayerWidgetIndex> playerArrayList;
	@Getter
	public ArrayList<Integer> currentPoopIndexes;
	public ArrayList<Integer> invalidOrbIndexes;
	@Inject
	@Getter
	@Setter
	private Client client;
	@Inject
	private WhosGonnaPoopConfig config;

    @Inject
    private KeyManager keyManager;
	@Inject
	private WhosGonnaPoopPanel panel;
	@Inject
	private ClientToolbar clientToolbar;
	private NavigationButton navigationButton;

	final int TOA_PARTY_1 = 31522822;
	final int TOA_PARTY_2 = 31522826;
	final int TOA_PARTY_3 = 31522830;
	final int TOA_PARTY_4 = 31522834;
	final int TOA_PARTY_5 = 31522838;
	final int TOA_PARTY_6 = 31522842;
	final int TOA_PARTY_7 = 31522846;
	final int TOA_PARTY_8 = 31522850;
	final int TOA_MIDDLE_ROOM = 14160;
	final int TOA_PUZZLE_ROOM = 14162;
	final int TOA_KEPHRI_ROOM = 14164;
	final int TOA_LOBBY_ROOM = 13454;
	final int KEPHRI_ID = 11719;
	@Getter
	public int howManyPoopers;
	private HotkeyListener hotkey = new HotkeyListener();

	@Provides
	WhosGonnaPoopConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WhosGonnaPoopConfig.class);
	}

	@Override
	public void startUp(){
		 hotkey = new HotkeyListener(() -> config.nextPhaseHotkey()) {
            @Override
            public void hotkeyPressed() {
				triggerPoopSwap();
            }
        };
		currentPoopIndexes = new ArrayList<Integer>();
		playerArrayList = new ArrayList<PlayerWidgetIndex>();
		invalidOrbIndexes = new ArrayList<Integer>();
		lastPoopedTime = System.currentTimeMillis();
		howManyPoopers = 1;
		phaseCount = 0;

		panel = new WhosGonnaPoopPanel(this);

		navigationButton = NavigationButton
				.builder()
				.tooltip("Who's Gonna Poop?")
				.icon(ImageUtil.loadImageResource(getClass(), "/poop.png"))
				.priority(1002)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navigationButton);

	}
	public void triggerPoopSwap(){
				int min = Collections.min(currentPoopIndexes);
				int max = Collections.max(currentPoopIndexes);

				if (playerArrayList.size() == howManyPoopers) {
					return; //We don't want to do anything to the indexes if it's 1 or 2 people and it matches the current pooper count
				}
				updatePoopIndexes(min,max);	
	}
	@Override
	public void shutDown()
	{
		keyManager.unregisterKeyListener(hotkey);
		clientToolbar.removeNavigation(navigationButton);
		playerArrayList.clear();
		currentPoopIndexes.clear();
	}
	@Subscribe(priority = 100)
	private void onClientShutdown(ClientShutdown e)
	{
	}

	public ArrayList<String> currentNames(){
		return playerArrayList.stream().filter(x->currentPoopIndexes.contains(x.index)).findFirst().isPresent() ?
				playerArrayList.stream().filter(x->currentPoopIndexes.contains(x.index))
				.collect(Collectors.toCollection((ArrayList::new))).stream().map(PlayerWidgetIndex::getName)
				.collect(Collectors.toCollection((ArrayList::new))) : new ArrayList<>();
	}


	public int getWidgetBaseID(int index){
		switch(index){
			case 0:
				return TOA_PARTY_1;
			case 1:
				return TOA_PARTY_2;
			case 2:
				return TOA_PARTY_3;
			case 3:
				return TOA_PARTY_4;
			case 4:
				return TOA_PARTY_5;
			case 5:
				return TOA_PARTY_6;
			case 6:
				return TOA_PARTY_7;
			case 7:
				return TOA_PARTY_8;
		}
		return TOA_PARTY_1;
	}
	public int getNextAvailableOrbIndexFrom(int startingIndex){

		int validCount = 0;
		int index = (startingIndex+1 > playerArrayList.size()-1) ? 0 : startingIndex+1;
		PlayerWidgetIndex pwi = null;
		while(validCount < 1){
			if(!invalidOrbIndexes.contains(playerArrayList.get(index).index)){
				validCount++;
			}
			if(validCount == 1){pwi = playerArrayList.get(index);}
			if(index == playerArrayList.size()-1){
				index = 0;
			}
			else{
				index++;
			}
		}
		return pwi.index;
	}
	public PlayerWidgetIndex traverseOrbs(int startingOrb){
		int validCount = 0;
		int index = (startingOrb+1 > playerArrayList.size()-1) ? 0 : startingOrb+1;
		PlayerWidgetIndex pwi = null;
		while(validCount < howManyPoopers){
			if(!invalidOrbIndexes.contains(playerArrayList.get(index).index)){
				validCount++;
			}
			if(validCount == howManyPoopers){pwi = playerArrayList.get(index);}
			if(index == playerArrayList.size()-1){
				index = 0;
			}
			else{
				index++;
			}
		}
		return pwi;
	}
	public void updatePoopIndexes(int min, int max){
		currentPoopIndexes.clear();
		PlayerWidgetIndex maxPWI = null;
		PlayerWidgetIndex minPWI = null;
		if(playerArrayList.size() == 1){ //in case there's only one guy left alive, no need to swap anymore
			return;
		}
		maxPWI = traverseOrbs(max);
		minPWI = traverseOrbs(min);
		if(minPWI !=null && howManyPoopers ==2){
//			log.info("Added minPooper of " + minPWI.name + ", " +minPWI.index);
			currentPoopIndexes.add(minPWI.index);
		}
		if(maxPWI !=null){
			currentPoopIndexes.add(maxPWI.index);
//			log.info("Added maxPooper of " + maxPWI.name + ", " +maxPWI.index);
		}
	}
	public void resetPoopers(){
		if (playerArrayList.size() ==0){
			return;
		}
		if(howManyPoopers == 2 && playerArrayList.size() == 1){
			howManyPoopers = 1;
			return;
		}
		if(howManyPoopers > currentPoopIndexes.size()){ //there was 1 pooper before, but now 2
			if (currentPoopIndexes.get(0) == playerArrayList.size()-1){ //If it's the last one, we grab index 0 (or the next valid)
				currentPoopIndexes.add(getNextAvailableOrbIndexFrom(0));
			}
			else{
				currentPoopIndexes.add(getNextAvailableOrbIndexFrom(currentPoopIndexes.get(0)));
			}
		}
		else if (howManyPoopers < currentPoopIndexes.size()) {
			currentPoopIndexes.remove(currentPoopIndexes.size() - 1);
		}

	}
	public void highlightPooperOrbs(){
		if (client.getWidget(TOA_PARTY_1) == null){return;}
		for (PlayerWidgetIndex pwi : playerArrayList){
			if (currentPoopIndexes.contains(pwi.index)){
				client.getWidget(pwi.widgetID+1).setSpriteId(1990);
				client.getWidget(pwi.widgetID+2).setHidden(true);
				client.getWidget(pwi.widgetID+3).setHidden(true);
			}
			else{
				client.getWidget(pwi.widgetID+1).setSpriteId(1697);
				client.getWidget(pwi.widgetID+2).setHidden(false);
				client.getWidget(pwi.widgetID+3).setHidden(false);
			}
		}
	}

	public boolean isinKephri(Player player)
	{
		if (player == null)
		{
			return false;
		}
		int regionId = WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
		return regionId == TOA_KEPHRI_ROOM;
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		//We're starting up another raid, reset the values
		if(e.getGroupId() == 772) {
//			log.info("Resetting playerlists after raid restart");
			if (playerArrayList != null) playerArrayList.clear();
			if (currentPoopIndexes != null) currentPoopIndexes.clear();
			if (invalidOrbIndexes != null) invalidOrbIndexes.clear();
			phaseCount = 0;
			lastPoopedTime = System.currentTimeMillis();
		}
	}

	public void syncUsers(){
		if(client.getVarcStrValue(1099)== "" || !isinKephri(client.getLocalPlayer()) ||
			client.getPlayers().size() == playerArrayList.size() || client.getPlayers().size() == 0){
			return;
		}
		else{
			//Handle when someone logs back in and they want to start again
			if(client.getNpcs().stream().filter(x->x.getId() == KEPHRI_ID).findFirst().isPresent()
				&& 	client.getNpcs().stream().filter(x->x.getId() == KEPHRI_ID).findFirst().get().getHealthRatio() == -1 // fight isnt in progress
				&& playerArrayList.size() <= client.getPlayers().size()){ //And there are less players in the player arraylist than in the boss
					resetValues();
					//repopulate the list in the scenario where some dced along with the ones that died
					regenerateViaOrbs();
			}
			else{ //If someone's gone for some reason, but they still stayed in the playerArrayList
				if (playerArrayList.size() > client.getPlayers().size()){
					playerArrayList.clear();
					log.info("Regenerating player orbs, there are less players in the boss than we have in our list");
					for(int i = 1099; i<1099+client.getPlayers().size();i++){
						playerArrayList.add(new PlayerWidgetIndex(client.getVarcStrValue(i).replace('\u00a0',' '),playerArrayList.size(),getWidgetBaseID(playerArrayList.size())));
						log.info(client.getVarcStrValue(i).replace('\u00a0',' '));
					}

				}
			}
		}
	}
	@Subscribe
	public void onGameTick(GameTick event)
	{

		if (WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID() != TOA_KEPHRI_ROOM
		&& WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID() != TOA_PUZZLE_ROOM
		&& WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID() != TOA_MIDDLE_ROOM
		&& WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID() != TOA_LOBBY_ROOM	)
		{
			keyManager.unregisterKeyListener(hotkey);
			return;
		}
		if(isinKephri(client.getLocalPlayer())){
			keyManager.registerKeyListener(hotkey);
			syncUsers();
			highlightPooperOrbs();
		}
		if(WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID() == TOA_MIDDLE_ROOM){
			if(playerArrayList.size() < client.getPlayers().size() && client.getWidget(TOA_PARTY_1)!=null){
				playerArrayList.clear();
				for(int i = 1099; i<1099+client.getPlayers().size();i++){
					playerArrayList.add(new PlayerWidgetIndex(client.getVarcStrValue(i).replace('\u00a0',' '),playerArrayList.size(),getWidgetBaseID(playerArrayList.size())));
					log.info(client.getVarcStrValue(i).replace('\u00a0',' '));
					if(currentPoopIndexes.size()<howManyPoopers){ //Also add poopers to the currentpoopers list
						currentPoopIndexes.add(playerArrayList.size()-1);
					}
				}
			}
		}
	}
	public void regenerateViaOrbs(){
		for(int i = 1099; i<1099+client.getPlayers().size();i++){
			playerArrayList.add(new PlayerWidgetIndex(client.getVarcStrValue(i).replace('\u00a0',' '),playerArrayList.size(),getWidgetBaseID(playerArrayList.size())));
			log.info(client.getVarcStrValue(i).replace('\u00a0',' '));
			if(currentPoopIndexes.size()<howManyPoopers){ //Also add poopers to the currentpoopers list
				currentPoopIndexes.add(playerArrayList.size()-1);
			}
		}
	}
	public void resetValues(){
		playerArrayList.clear();
		currentPoopIndexes.clear();
	}
	@Subscribe
	public void onActorDeath(ActorDeath e)
	{
		if(e.getActor() instanceof Player){
			if(isinKephri((Player) e.getActor())) {
				PlayerWidgetIndex temp = playerArrayList.stream().filter(x->x.name.equals(e.getActor().getName())).findFirst().get();
				invalidOrbIndexes.add(temp.index);
				if(currentPoopIndexes.contains(temp.index)){
					currentPoopIndexes.removeIf(x->x==temp.index);
					if(invalidOrbIndexes.size()<playerArrayList.size()){
						currentPoopIndexes.add(getNextAvailableOrbIndexFrom(temp.index));
					}
				}
				if(invalidOrbIndexes.size() == playerArrayList.size()){
					invalidOrbIndexes.clear();
					resetValues();
					regenerateViaOrbs();
				}
			}
		}

	}

}

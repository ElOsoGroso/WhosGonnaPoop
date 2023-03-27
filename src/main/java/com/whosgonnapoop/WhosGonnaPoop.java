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
		name = "Whos Gonna Poop",
		description = "When entering combat with an NPC, a display of the possible drops and which ones you're missing will appear",
		tags = {"display", "overlay", "collection", "log"}
)
public class WhosGonnaPoop extends Plugin
{
	@Getter
	public ArrayList<PlayerWidgetIndex> playerArrayList;
	@Getter
	public ArrayList<Integer> currentPoopIndexes;
	public ArrayList<Integer> invalidOrbIndexes;
	public int phaseCount;
	public boolean beenToKephri = false;
	public boolean firstTimePooping;
	@Inject
	@Getter
	@Setter
	private Client client;
	@Inject
	private WhosGonnaPoopConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WhosGonnaPoopOverlay whosGonnaPoopOverlay;
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
	private long lastPoopedTime;
	@Getter
	public int howManyPoopers;
	@Provides
	WhosGonnaPoopConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WhosGonnaPoopConfig.class);
	}

	@Override
	public void startUp(){
		overlayManager.add(whosGonnaPoopOverlay);
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

	@Override
	public void shutDown()
	{
		overlayManager.remove(whosGonnaPoopOverlay);
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
			log.info("Resetting playerlists after raid restart");
			if (playerArrayList != null) playerArrayList.clear();
			if (currentPoopIndexes != null) currentPoopIndexes.clear();
			if (invalidOrbIndexes != null) invalidOrbIndexes.clear();
			phaseCount = 0;
			lastPoopedTime = System.currentTimeMillis();
		}
	}
	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		if(e.getActor() instanceof NPC && ((NPC)e.getActor()).getId() == KEPHRI_ID){
			if (e.getActor().getAnimation() == 9579){
				phaseCount++; //Phasing up
				log.info("Phasing count up " + phaseCount);
			}
		}

	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged e)
	{
		if ((e.getActor() instanceof NPC)) {
			if(((NPC) e.getActor()).getId() == KEPHRI_ID){
//				log.info("Kephri Graphic: " + e.getActor().getGraphic());
//				log.info("Kephri Animation: " + e.getActor().getAnimation());
//				log.info("Kephri health ratio:  " + e.getActor().getHealthRatio());
			}
		}
		if ((e.getActor() instanceof Player))
		{
			if(isinKephri((Player)e.getActor())){
				boolean triggerOrbSwap = false;
				boolean skipEverybodyPoops = false;
				Player currentPooper = (Player) e.getActor();
				if ((currentPooper.getGraphic() == 2146 || currentPooper.getGraphic() == 245)) {
//					log.info("Phase count: " + phaseCount);
					log.info(currentPooper.getName() + (currentPooper.getGraphic() == 2146 ? " got flies" : " is poopy"));
//					log.info("Seconds from last poop time: " + Long.toString((System.currentTimeMillis() - lastPoopedTime) / 1000));
					//if we're in phase 2 and the graphic change happened when the kephri health is 0, we dont' care about it (everybody poops)
					if(phaseCount == 2 && (client.getNpcs().stream().filter(x->x.getId() == KEPHRI_ID).findFirst().isPresent() &&
							client.getNpcs().stream().filter(x->x.getId() ==KEPHRI_ID).findFirst().get().getHealthRatio() == 0)){
						skipEverybodyPoops = true;
						log.info("Skipping this pooper because everybody poops");
					}
					//Scenario for  multi poops
					//if on a graphic of flies or poop, 4 seconds have passed from the last displacement
					//-- if in phase 2, skip the multi-poop if we've counted flies or poop for every player, otherwise we swap
					//-- if not phase 2, we just will always poop on graphics that came 4 seconds from the previous one
					if(!skipEverybodyPoops){
						if ((System.currentTimeMillis() - lastPoopedTime) / 1000 > 4){
							triggerOrbSwap = true;
							lastPoopedTime = System.currentTimeMillis();
							log.info("We poop");
						}
						else{
							log.info("Skipped "+ currentPooper.getName() + " because of poop time delay");
						}
					}
					//if they have already pooped in this rotation, skip them (give them 4 seconds from last pooped time)
					if (triggerOrbSwap) {
//						log.info("We'd be swapping orbs");
						int min = Collections.min(currentPoopIndexes);
						int max = Collections.max(currentPoopIndexes);
//						log.info("Just pooped:");
//						log.info("min = " + min + ": (" + playerArrayList.stream().filter(x->x.index== min).findFirst().get().name + ")");
//						log.info("max = " + max + ": (" + playerArrayList.stream().filter(x->x.index== max).findFirst().get().name + ")");
						if (playerArrayList.size() == howManyPoopers) {
//							log.info("returning");
							return; //We don't want to do anything to the indexes if it's 1 or 2 people and it matches the current pooper count
						}
						updatePoopIndexes(min,max);
//						log.info("Pooper list");
//						for(int i : currentPoopIndexes){
//							log.info(Integer.toString(i));
//						}
					} else {
						log.info("Skipping swap on this pooper: " + currentPooper.getName());
//						log.info("Time from last poop: " + Long.toString((System.currentTimeMillis() - lastPoopedTime) / 1000));
					}
				}
			}
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
			return;
		}
		if(isinKephri(client.getLocalPlayer())){
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
		phaseCount = 0;
		firstTimePooping = true;
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
	@Subscribe
	public void onPlayerDespawned(PlayerDespawned e)
	{
		if(isinKephri(e.getPlayer())){
// 			log.info("Player that despawned:");
//			log.info(e.getActor().getName());
		}
	}

}
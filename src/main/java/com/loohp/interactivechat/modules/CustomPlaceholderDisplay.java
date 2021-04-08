package com.loohp.interactivechat.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.objectholders.CustomPlaceholder;
import com.loohp.interactivechat.objectholders.CustomPlaceholder.ClickEventAction;
import com.loohp.interactivechat.objectholders.CustomPlaceholder.ParsePlayer;
import com.loohp.interactivechat.objectholders.ICPlaceholder;
import com.loohp.interactivechat.objectholders.ICPlayer;
import com.loohp.interactivechat.objectholders.WebData;
import com.loohp.interactivechat.utils.ChatColorUtils;
import com.loohp.interactivechat.utils.CustomStringUtils;
import com.loohp.interactivechat.utils.PlaceholderParser;
import com.loohp.interactivechat.utils.PlayerUtils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class CustomPlaceholderDisplay {
	
	private static Map<UUID, Map<String, Long>> placeholderCooldowns = InteractiveChat.placeholderCooldowns;
	private static Map<UUID, Long> universalCooldowns = InteractiveChat.universalCooldowns;
	
	public static BaseComponent process(BaseComponent basecomponent, Optional<ICPlayer> optplayer, Player reciever, List<ICPlaceholder> placeholderList, long unix) {
		return process(basecomponent, optplayer, reciever, placeholderList, unix, false);
	}
			
	
	public static BaseComponent process(BaseComponent basecomponent, Optional<ICPlayer> optplayer, Player reciever, List<ICPlaceholder> placeholderList, long unix, boolean withoutCooldown) {
		for (int i = 0; i < placeholderList.size(); i++) {
			
			ICPlaceholder icplaceholder = placeholderList.get(i);
			if (icplaceholder.isBuildIn()) {
				continue;
			}
			CustomPlaceholder cp = icplaceholder.getCustomPlaceholder().get();
			
			ICPlayer parseplayer = (cp.getParsePlayer().equals(ParsePlayer.SENDER) && optplayer.isPresent()) ? optplayer.get() : new ICPlayer(reciever);
			boolean casesensitive = cp.isCaseSensitive();
			
			if (InteractiveChat.useCustomPlaceholderPermissions && optplayer.isPresent()) {
				ICPlayer sender = optplayer.get();
				if (!PlayerUtils.hasPermission(sender.getUniqueId(), cp.getPermission(), true, 5)) {
					continue;
				}
			}
			
			String placeholder = cp.getKeyword();
			placeholder = (cp.getParseKeyword()) ? PlaceholderParser.parse(parseplayer, placeholder) : placeholder;
			long cooldown = cp.getCooldown();
			boolean hoverEnabled = cp.getHover().isEnabled();
			String hoverText = cp.getHover().getText();
			boolean clickEnabled = cp.getClick().isEnabled();
			ClickEventAction clickAction = cp.getClick().getAction();
			String clickValue = cp.getClick().getValue();
			boolean replaceEnabled = cp.getReplace().isEnabled();
			String replaceText = cp.getReplace().getReplaceText();
			
			if (withoutCooldown) {
				basecomponent = processCustomPlaceholderWithoutCooldown(parseplayer, casesensitive, placeholder, cooldown, hoverEnabled, hoverText, clickEnabled, clickAction, clickValue, replaceEnabled, replaceText, basecomponent, optplayer, unix);
			} else {
				basecomponent = processCustomPlaceholder(parseplayer, casesensitive, placeholder, cooldown, hoverEnabled, hoverText, clickEnabled, clickAction, clickValue, replaceEnabled, replaceText, basecomponent, optplayer, unix);
			}
		}
		
		if (InteractiveChat.t && WebData.getInstance() != null) {
			for (CustomPlaceholder cp : WebData.getInstance().getSpecialPlaceholders()) {
				ICPlayer parseplayer = (cp.getParsePlayer().equals(ParsePlayer.SENDER) && optplayer.isPresent()) ? optplayer.get() : new ICPlayer(reciever);
				boolean casesensitive = cp.isCaseSensitive();			
				String placeholder = cp.getKeyword();
				placeholder = (cp.getParseKeyword()) ? PlaceholderParser.parse(parseplayer, placeholder) : placeholder;
				long cooldown = cp.getCooldown();
				boolean hoverEnabled = cp.getHover().isEnabled();
				String hoverText = cp.getHover().getText();
				boolean clickEnabled = cp.getClick().isEnabled();
				ClickEventAction clickAction = cp.getClick().getAction();
				String clickValue = cp.getClick().getValue();
				boolean replaceEnabled = cp.getReplace().isEnabled();
				String replaceText = cp.getReplace().getReplaceText();
				
				if (withoutCooldown) {
					basecomponent = processCustomPlaceholderWithoutCooldown(parseplayer, casesensitive, placeholder, cooldown, hoverEnabled, hoverText, clickEnabled, clickAction, clickValue, replaceEnabled, replaceText, basecomponent, optplayer, unix);
				} else {
					basecomponent = processCustomPlaceholder(parseplayer, casesensitive, placeholder, cooldown, hoverEnabled, hoverText, clickEnabled, clickAction, clickValue, replaceEnabled, replaceText, basecomponent, optplayer, unix);
				}
			}
		}
			
		return basecomponent;
	}
	
	public static BaseComponent processCustomPlaceholder(ICPlayer parseplayer, boolean casesensitive, String placeholder, long cooldown, boolean hoverEnabled, String hoverText, boolean clickEnabled, ClickEventAction clickAction, String clickValue, boolean replaceEnabled, String replaceText, BaseComponent basecomponent, Optional<ICPlayer> optplayer, long unix) {
		boolean contain = (casesensitive) ? (basecomponent.toPlainText().contains(placeholder)) : (basecomponent.toPlainText().toLowerCase().contains(placeholder.toLowerCase()));
		if (!InteractiveChat.cooldownbypass.get(unix).contains(placeholder) && contain) {
			if (optplayer.isPresent()) {
				ICPlayer player = optplayer.get();
				Long uc = universalCooldowns.get(player.getUniqueId());
				if (uc != null) {
					if (uc > unix) {
						return basecomponent;
					}
				}
				
				if (!placeholderCooldowns.containsKey(player.getUniqueId())) {
					placeholderCooldowns.put(player.getUniqueId(), new ConcurrentHashMap<String, Long>());
				}
				Map<String, Long> spmap = placeholderCooldowns.get(player.getUniqueId());
				if (spmap.containsKey(placeholder)) {
					if (spmap.get(placeholder) > unix) {
						if (!PlayerUtils.hasPermission(player.getUniqueId(), "interactivechat.cooldown.bypass", false, 5)) {
							return basecomponent;
						}
					}
				}
				spmap.put(placeholder, unix + cooldown);
				InteractiveChat.universalCooldowns.put(player.getUniqueId(), unix + InteractiveChat.universalCooldown);
			}
			InteractiveChat.cooldownbypass.get(unix).add(placeholder);
			InteractiveChat.cooldownbypass.put(unix, InteractiveChat.cooldownbypass.get(unix));
		}
		
		return processCustomPlaceholderWithoutCooldown(parseplayer, casesensitive, placeholder, cooldown, hoverEnabled, hoverText, clickEnabled, clickAction, clickValue, replaceEnabled, replaceText, basecomponent, optplayer, unix);
	}
	
	@SuppressWarnings("deprecation")
	public static BaseComponent processCustomPlaceholderWithoutCooldown(ICPlayer parseplayer, boolean casesensitive, String placeholder, long cooldown, boolean hoverEnabled, String hoverText, boolean clickEnabled, ClickEventAction clickAction, String clickValue, boolean replaceEnabled, String replaceText, BaseComponent basecomponent, Optional<ICPlayer> optplayer, long unix) {
		List<BaseComponent> basecomponentlist = CustomStringUtils.loadExtras(basecomponent);
		List<BaseComponent> newlist = new ArrayList<>();
		for (BaseComponent base : basecomponentlist) {
			if (!(base instanceof TextComponent)) {
				newlist.add(base);
			} else {
				TextComponent textcomponent = (TextComponent) base;
				String text = textcomponent.getText();
				if (casesensitive) {
					if (!ChatColorUtils.stripColor(text).contains(ChatColorUtils.stripColor(placeholder))) {
						newlist.add(textcomponent);
						continue;
					}
				} else {
					if (!ChatColorUtils.stripColor(text).toLowerCase().contains(ChatColorUtils.stripColor(placeholder).toLowerCase())) {
						newlist.add(textcomponent);
						continue;
					}
				}
				
				String regex = casesensitive ? "(?<!\u00a7)" + CustomStringUtils.getIgnoreColorCodeRegex(CustomStringUtils.escapeMetaCharacters(placeholder)) : "(?i)(?<!\u00a7)(" + CustomStringUtils.getIgnoreColorCodeRegex(CustomStringUtils.escapeMetaCharacters(placeholder)) + ")";
				List<String> trim = new LinkedList<String>(Arrays.asList(text.split(regex, -1)));
				if (trim.get(trim.size() - 1).equals("")) {
					trim.remove(trim.size() - 1);
				}

				String lastColor = "";
				
				for (int i = 0; i < trim.size(); i++) {
					TextComponent before = new TextComponent(textcomponent);
					before.setText(lastColor + trim.get(i));
					newlist.add(before);
					lastColor = ChatColorUtils.getLastColors(before.getText());
					
					boolean endwith = casesensitive ? text.matches(".*" + regex + "$") : text.toLowerCase().matches(".*" + regex.toLowerCase() + "$");
					if ((trim.size() - 1) > i || endwith) {
						if (trim.get(i).endsWith("\\") && !trim.get(i).endsWith("\\\\")) {
							String color = ChatColorUtils.getLastColors(newlist.get(newlist.size() - 1).toLegacyText());
							TextComponent message = new TextComponent(placeholder);
							message = (TextComponent) ChatColorUtils.applyColor(message, color);
							((TextComponent) newlist.get(newlist.size() - 1)).setText(trim.get(i).substring(0, trim.get(i).length() - 1));
							newlist.add(message);
						} else {
							if (trim.get(i).endsWith("\\\\")) {
								((TextComponent) newlist.get(newlist.size() - 1)).setText(trim.get(i).substring(0, trim.get(i).length() - 1));
							}
							ICPlayer player = parseplayer;
							
							String textComp = placeholder;
							if (replaceEnabled) {
								textComp = ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(player, replaceText));
							}
							BaseComponent[] bcJson = TextComponent.fromLegacyText(textComp);
			            	List<BaseComponent> baseJson = new ArrayList<>();
			            	baseJson = CustomStringUtils.loadExtras(Arrays.asList(bcJson));
			            	
			            	for (BaseComponent baseComponent : baseJson) {
			            		TextComponent message = (TextComponent) baseComponent;
			            		if (hoverEnabled) {
									message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(player, hoverText))).create()));
								}
								
								if (clickEnabled) {
									String clicktext = PlaceholderParser.parse(player, clickValue);
									message.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(clickAction.name()), clicktext));
								}
								
								newlist.add(message);
			            	}
						}
					}
				}
			}
		}
		
		TextComponent product = new TextComponent("");
		for (int i = 0; i < newlist.size(); i++) {
			BaseComponent each = newlist.get(i);
			product.addExtra(each);
		}
		return product;
	}

}
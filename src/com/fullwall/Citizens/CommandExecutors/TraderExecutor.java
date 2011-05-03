package com.fullwall.Citizens.CommandExecutors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.fullwall.Citizens.Citizens;
import com.fullwall.Citizens.Economy.EconomyHandler;
import com.fullwall.Citizens.Economy.IconomyInterface;
import com.fullwall.Citizens.Economy.Payment;
import com.fullwall.Citizens.NPCs.NPCManager;
import com.fullwall.Citizens.Traders.ItemPrice;
import com.fullwall.Citizens.Traders.Stockable;
import com.fullwall.Citizens.Utils.MessageUtils;
import com.fullwall.Citizens.Utils.StringUtils;
import com.fullwall.Citizens.Utils.TraderPropertyPool;
import com.fullwall.resources.redecouverte.NPClib.HumanNPC;

public class TraderExecutor implements CommandExecutor {

	@SuppressWarnings("unused")
	private Citizens plugin;

	public TraderExecutor(Citizens plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String commandLabel, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(MessageUtils.mustBeIngameMessage);
			return true;
		}
		Player player = (Player) sender;
		HumanNPC npc = null;
		boolean returnval = false;
		if (NPCManager.validateSelected((Player) sender))
			npc = NPCManager
					.getNPC(NPCManager.NPCSelected.get(player.getName()));
		else {
			player.sendMessage(ChatColor.RED
					+ MessageUtils.mustHaveNPCSelectedMessage);
			return true;
		}
		if (!NPCManager.validateOwnership(npc.getUID(), player)) {
			player.sendMessage(MessageUtils.notOwnerMessage);
			return true;

		}
		if (!npc.isTrader()) {
			player.sendMessage(ChatColor.RED + "Your NPC isn't a trader yet.");
			return true;
		} else {
			if (args.length == 3 && args[0].equals("balance")) {
				if (BasicExecutor.hasPermission("citizens.trader.balance",
						sender)) {
					try {
						if (!EconomyHandler.useIconomy())
							player.sendMessage(ChatColor.GRAY
									+ "This server is not using an economy plugin.");
						else
							changeBalance(player, npc, args);
					} catch (NumberFormatException e) {
						player.sendMessage(ChatColor.RED
								+ "Invalid balance change amount entered.");
					}
				} else
					player.sendMessage(MessageUtils.noPermissionsMessage);
				returnval = true;
			} else if (args.length == 3
					&& (args[0].contains("b") || args[0].contains("s"))) {
				// trader buy/sell item1/remove 2ndarg (if it has a ':' its an
				// item, else iconomy)
				if (BasicExecutor
						.hasPermission("citizens.trader.stock", sender)) {
					changeTraderBuySell(npc, player, args[1], args[2],
							args[0].contains("s"));
				} else
					player.sendMessage(MessageUtils.noPermissionsMessage);
				returnval = true;
			} else if (args.length == 2 && (args[0].contains("unl"))) {
				if (BasicExecutor.hasPermission(
						"citizens.admin.unlimitedtrader", sender)) {
					changeUnlimited(npc, sender, args[1]);
				} else
					player.sendMessage(MessageUtils.noPermissionsMessage);
				returnval = true;
			}
			TraderPropertyPool.saveTraderState(npc);
		}
		return returnval;
	}

	private void changeUnlimited(HumanNPC npc, CommandSender sender,
			String unlimited) {
		if (unlimited.equals("true") || unlimited.equals("on")) {
			npc.getTraderNPC().setUnlimited(true);
			sender.sendMessage(ChatColor.GREEN
					+ "The trader will now have unlimited stock!");
		} else if (unlimited.equals("false") || unlimited.equals("off")) {
			npc.getTraderNPC().setUnlimited(false);
			sender.sendMessage(ChatColor.GREEN
					+ "The trader has stopped having unlimited stock.");
		} else
			sender.sendMessage(ChatColor.GREEN
					+ "Incorrect unlimited type entered. Valid values are true, on, false, off.");
	}

	private void changeTraderBuySell(HumanNPC npc, Player player, String item,
			String price, boolean selling) {
		if (item.contains("rem")) {
			Material mat = StringUtils.parseMaterial(price);
			if (mat == null) {
				player.sendMessage(ChatColor.RED
						+ "Invalid item ID or name specified.");
				return;
			}
			if (selling) {
				if (npc.getTraderNPC().getStockable(mat.getId(), true) == null) {
					player.sendMessage(ChatColor.RED
							+ "The NPC is not currently selling that item.");
					return;
				} else {
					npc.traderNPC.removeStockable(mat.getId(), true);
					player.sendMessage(ChatColor.GREEN
							+ "Removed "
							+ StringUtils.yellowify(mat.name(), ChatColor.GREEN)
							+ " from the NPC's selling list.");
				}
			} else {
				if (npc.getTraderNPC().getStockable(mat.getId(), false) == null) {
					player.sendMessage(ChatColor.RED
							+ "The NPC is not currently buying that item.");
					return;
				} else {
					npc.traderNPC.removeStockable(mat.getId(), false);
					player.sendMessage(ChatColor.GREEN
							+ "Removed "
							+ StringUtils.yellowify(mat.name(), ChatColor.GREEN)
							+ " from the NPC's buying list.");
				}
			}
			return;
		}
		String[] split = item.split(":");
		ItemStack stack = createItemStack(split);
		if (stack == null) {
			player.sendMessage(ChatColor.RED
					+ "Invalid item ID or name specified.");
			return;
		}
		split = price.split(":");
		ItemStack cost = null;
		if (split.length != 1) {
			cost = createItemStack(split);
			if (cost == null) {
				player.sendMessage(ChatColor.RED
						+ "Invalid item ID or name specified.");
				return;
			}
		}
		int data = Citizens.MAGIC_DATA_VALUE;
		if (stack.getData() != null)
			data = stack.getData().getData();
		ItemPrice itemPrice = new ItemPrice(stack.getAmount(),
				stack.getTypeId(), data);
		itemPrice.setiConomy(cost == null);
		Stockable s = new Stockable(stack, itemPrice, false);
		if (selling) {
			s.setSelling(true);
			npc.getTraderNPC().addStockable(s);
			player.sendMessage(ChatColor.GREEN + "The NPC is now selling "
					+ MessageUtils.getStockableMessage(s, ChatColor.GREEN)
					+ ".");
		} else {
			npc.getTraderNPC().addStockable(s);
			player.sendMessage(ChatColor.GREEN + "The NPC is now buying "
					+ MessageUtils.getStockableMessage(s, ChatColor.GREEN)
					+ ".");
		}
	}

	private ItemStack createItemStack(String[] split) {
		int amount = 1;
		int data = 0;
		Material mat = StringUtils.parseMaterial(split[0]);
		if (mat == null) {
			return null;
		}
		if (split.length > 1)
			amount = Integer.parseInt(split[1]);
		if (split.length > 2)
			data = Integer.parseInt(split[2]);
		ItemStack stack = new ItemStack(mat, amount);
		if (data > 0) {
			MaterialData mdata = new MaterialData(data);
			stack.setData(mdata);
		}
		return stack;
	}

	private void changeBalance(Player player, HumanNPC npc, String[] args)
			throws NumberFormatException {
		int amount = Integer.valueOf(args[2]);
		if (args[1].equals("give")) {
			if (EconomyHandler.canBuy(new Payment(amount, true), player)) {
				EconomyHandler.pay(new Payment(-amount, true), npc);
				EconomyHandler.pay(new Payment(amount, true), player);
				player.sendMessage(ChatColor.GREEN
						+ "Gave "
						+ StringUtils.yellowify(
								IconomyInterface.getCurrency(amount),
								ChatColor.GREEN)
						+ " to "
						+ StringUtils.yellowify(npc.getStrippedName(),
								ChatColor.GREEN)
						+ ". Your balance is now "
						+ StringUtils.yellowify(
								IconomyInterface.getBalance(player.getName()),
								ChatColor.GREEN) + ".");
			} else {
				player.sendMessage(ChatColor.RED
						+ "You don't have enough money for that! Need "
						+ " "
						+ StringUtils.yellowify(
								IconomyInterface.getCurrency(amount
										- IconomyInterface.getBalance(player
												.getName())), ChatColor.RED)
						+ " more.");
			}
		} else if (args[1].equals("take")) {
			if (EconomyHandler.canBuy(new Payment(amount, true), npc)) {
				EconomyHandler.pay(new Payment(amount, true), npc);
				EconomyHandler.pay(new Payment(-amount, true), player);
				player.sendMessage(ChatColor.GREEN
						+ "Took "
						+ StringUtils.yellowify(
								IconomyInterface.getCurrency(amount),
								ChatColor.GREEN)
						+ " from "
						+ StringUtils.yellowify(npc.getStrippedName(),
								ChatColor.GREEN)
						+ ". Your balance is now "
						+ StringUtils.yellowify(
								IconomyInterface.getBalance(player.getName()),
								ChatColor.GREEN) + ".");
			} else {
				player.sendMessage(ChatColor.RED
						+ "The NPC doesn't have enough money for that! It needs "
						+ StringUtils.yellowify(
								IconomyInterface.getCurrency(amount
										- npc.getTraderNPC().getBalance()),
								ChatColor.RED) + " more in its balance.");
			}
		} else
			player.sendMessage(ChatColor.RED + "Invalid argument type "
					+ StringUtils.yellowify(args[1], ChatColor.RED) + ".");
	}
}

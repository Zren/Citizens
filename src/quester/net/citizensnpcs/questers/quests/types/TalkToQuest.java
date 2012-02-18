package net.citizensnpcs.questers.quests.types;

import net.citizensnpcs.api.CitizensManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.questers.QuestCancelException;
import net.citizensnpcs.questers.quests.progress.ObjectiveProgress;
import net.citizensnpcs.questers.quests.progress.QuestUpdater;
import net.citizensnpcs.utils.InventoryUtils;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class TalkToQuest implements QuestUpdater {
    private static final Class<? extends Event>[] EVENTS = new Class[] { NPCRightClickEvent.class };

    @Override
    public boolean update(Event event, ObjectiveProgress progress) {
        if (!(event instanceof NPCRightClickEvent))
            return false;
        NPCRightClickEvent e = (NPCRightClickEvent) event;
        if (e.getPlayer().getEntityId() != progress.getPlayer().getEntityId()
                || e.getNPC().getUID() != progress.getObjective().getDestNPCID())
            return false;
        
        e.setCancelled(true);
        return true;
    }

    @Override
    public Class<? extends Event>[] getEventTypes() {
        return EVENTS;
    }

    @Override
    public String getStatus(ObjectiveProgress progress) throws QuestCancelException {
        if (CitizensManager.getNPC(progress.getObjective().getDestNPCID()) == null) {
            throw new QuestCancelException(ChatColor.GRAY + "Cancelling quest due to missing destination NPC.");
        }
        int amount = progress.getObjective().getAmount();
        if (progress.getObjective().getMaterial() == null || progress.getObjective().getMaterial() == Material.AIR)
            return ChatColor.GREEN + "Talking to "
                    + StringUtils.wrap(CitizensManager.getNPC(progress.getObjective().getDestNPCID()).getName()) + ".";
        return ChatColor.GREEN + "Talk to "
                + StringUtils.wrap(CitizensManager.getNPC(progress.getObjective().getDestNPCID()).getName()) + ".";
    }
}
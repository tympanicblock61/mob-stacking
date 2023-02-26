package net.tympanicblock61.mobstacking;

import com.google.gson.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class MobStacking extends JavaPlugin implements Listener{
    public static List<String> mobsList = null;
    public static JsonObject config;
    public static int stackingDistance;
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Gson gson = new Gson();
        String filePath = Paths.get("").toAbsolutePath().toString() + "/mob-stacking.json";

        try (FileReader reader = new FileReader(filePath)) {
            // Load the JSON file into a custom MobStacking object
            config = gson.fromJson(reader, JsonObject.class);
            JsonArray mobsArray = config.get("mobs").getAsJsonArray();
             mobsList = new ArrayList<>();
            for (JsonElement element : mobsArray) {
                mobsList.add(element.getAsString());
            }
        } catch (JsonSyntaxException | JsonIOException | IOException ignored) {}

        if (mobsList != null && config.get("distance") != null) {
            stackingDistance = config.get("distance").getAsInt();
        } else {
            stackingDistance = 30;
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent entityEvent) {
        Boolean removedMob = false;
        if (entityEvent.getEntity() instanceof LivingEntity && mobsList != null && mobsList.contains(entityEvent.getEntityType().getKey().toString()) || mobsList == null && entityEvent.getEntity() instanceof LivingEntity) {
            for (Entity entity : entityEvent.getEntity().getWorld().getEntities()) {
                if (entity.getType() == entityEvent.getEntity().getType() && entity.getLocation().distanceSquared(entityEvent.getEntity().getLocation()) <= stackingDistance) {
                    PersistentDataContainer data = entity.getPersistentDataContainer();
                    Integer Amount = data.getOrDefault(new NamespacedKey(this, "amount"), PersistentDataType.INTEGER, 0) + 1;
                    data.set(new NamespacedKey(this, "amount"), PersistentDataType.INTEGER, Amount);
                    entity.setCustomName("%s%s%s:%s%s".formatted(ChatColor.RED, entityEvent.getEntityType().getKey().toString().split(":")[1], ChatColor.RESET, ChatColor.GREEN, Amount));
                    entity.setCustomNameVisible(true);
                    entityEvent.getEntity().remove();
                    removedMob = true;
                    break;
                }
            }
            if (!removedMob) {
                PersistentDataContainer data = entityEvent.getEntity().getPersistentDataContainer();
                Integer Amount = data.getOrDefault(new NamespacedKey(this, "amount"), PersistentDataType.INTEGER, 0) + 1;
                data.set(new NamespacedKey(this, "amount"), PersistentDataType.INTEGER, Amount);
                entityEvent.getEntity().setCustomName("%s%s%s:%s%s".formatted(ChatColor.RED, entityEvent.getEntityType().getKey().toString().split(":")[1], ChatColor.RESET, ChatColor.GREEN, Amount));
                entityEvent.getEntity().setCustomNameVisible(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent death) {
        if (death.getEntity().getKiller() != null && mobsList != null && mobsList.contains(death.getEntityType().getKey().toString()) || mobsList == null && death.getEntity().getKiller() != null) {
            PersistentDataContainer data = death.getEntity().getPersistentDataContainer();
            int amount = data.get(new NamespacedKey(this, "amount"), PersistentDataType.INTEGER) - 1;
            if (amount > 0) {
                Entity newEntity = death.getEntity().getWorld().spawnEntity(death.getEntity().getLocation(), death.getEntity().getType());
                if (newEntity instanceof LivingEntity) {
                    ((LivingEntity) newEntity).setHealth(((LivingEntity) newEntity).getMaxHealth());
                    newEntity.setCustomNameVisible(true);
                    newEntity.setCustomName("%s%s%s:%s%s".formatted(ChatColor.RED, death.getEntity().getType().getKey().toString().split(":")[1], ChatColor.RESET, ChatColor.GREEN, amount));
                    newEntity.getPersistentDataContainer().set(new NamespacedKey(this, "amount"), PersistentDataType.INTEGER, amount);
                }
            }
        }
        death.getEntity().remove();
    }
}

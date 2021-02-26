package net.velinquish.sentinelnocount;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import net.velinquish.utils.Common;
import net.velinquish.utils.VelinquishPlugin;
import net.velinquish.utils.lang.LangManager;

public class SentinelNoCount extends JavaPlugin implements Listener, VelinquishPlugin {

	@Getter
	private static SentinelNoCount instance;
	@Getter
	private LangManager langManager;

	@Getter
	private String prefix;
	@Getter
	private String permission;

	private NPCRegistry citizens;

	private static boolean debug;

	@Override
	public void onEnable() {
		instance = this;
		Common.setInstance(this);

		langManager = new LangManager();

		getServer().getPluginManager().registerEvents(this, this);
		debug = false;

		citizens = CitizensAPI.getNPCRegistry();
	}

	@Override
	public void onDisable() {
		instance = null;
	}

	public void loadFiles() throws IOException, InvalidConfigurationException {

	}

	@EventHandler
	public void onCrackShotHitNPC(final EntityDamageEvent e) {
		Entity victim = e.getEntity();

		if (e.getCause() != DamageCause.ENTITY_EXPLOSION || !citizens.isNPC(victim))
			return;

		debug("Explosion event triggered on an NPC");

		for (Trait trait : citizens.getNPC(victim).getTraits()) {
			debug("Looking through traits... " + trait.getName());
			if (trait.getName().equals("sentinel"))
				return;
		}
		Bukkit.getScheduler().runTask(this, () -> {
			victim.setVelocity(new Vector());
		});
		victim.setVelocity(new Vector());
		debug("Canceling the knockback (so it supposed to)");
	}

	@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
	public void onKill(final EntityDamageByEntityEvent e) {

		boolean isProjectile = e.getDamager() instanceof Projectile;

		if (!(e.getEntity() instanceof HumanEntity && (e.getDamager() instanceof HumanEntity || isProjectile)))
			return;

		debug("A human damaged a human");

		double damage = e.getFinalDamage();
		HumanEntity victim = (HumanEntity) e.getEntity();
		HumanEntity attacker = null;
		if (isProjectile) {
			debug("It is a projectile!");
			ProjectileSource shooter = ((Projectile) e.getDamager()).getShooter();
			if (shooter instanceof HumanEntity)
				attacker = (HumanEntity) shooter;
			else
				return;
		} else
			attacker = (HumanEntity) e.getDamager();

		if (damage < victim.getHealth())
			return;

		if (citizens.isNPC(attacker) || citizens.isNPC(victim)) {
			debug("Cancelling the death");
			e.setCancelled(true);
			Entity entity = victim.getWorld().spawnEntity(victim.getLocation(), EntityType.BAT);
			entity.setCustomName(attacker.getCustomName());
			victim.damage(damage, entity);
			entity.remove();
			return;
			//victim.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 1000, false, false), true); // says NPC killed using magic
			//victim.damage(damage); // still blames the NPC
			//victim.setHealth(0); // still blames the NPC
			//victim.remove(); // completely bugs everything out
			//victim.setLastDamageCause(new EntityDamageEvent(victim, DamageCause.CUSTOM, 100)); // doesn't make the attacker a different entity
			//Bukkit.getServer().getPluginManager().callEvent(new EntityDamageEvent(victim, DamageCause.CUSTOM, 100)); // doesn't actually damage
		}
	}

	@EventHandler(priority=EventPriority.HIGH,ignoreCancelled=false)
	public void onCrackShotKill(final WeaponDamageEntityEvent e) {

		if (!(e.getVictim() instanceof HumanEntity && e.getDamager() instanceof HumanEntity))
			return;

		double damage = e.getDamage();
		HumanEntity victim = (HumanEntity) e.getVictim();
		HumanEntity attacker = e.getPlayer();

		if (damage < victim.getHealth())
			return;

		if (citizens.isNPC(attacker) || citizens.isNPC(victim)) {
			debug("Cancelling the death");
			e.setCancelled(true);
			Entity entity = victim.getWorld().spawnEntity(victim.getLocation(), EntityType.BAT);
			entity.setCustomName(attacker.getCustomName());
			victim.damage(damage, entity);
			entity.remove();
			return;
			//victim.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 1000, false, false), true); // says NPC killed using magic
			//victim.damage(damage); // still blames the NPC
			//victim.setHealth(0); // still blames the NPC
			//victim.remove(); // completely bugs everything out
			//victim.setLastDamageCause(new EntityDamageEvent(victim, DamageCause.CUSTOM, 100)); // doesn't make the attacker a different entity
			//Bukkit.getServer().getPluginManager().callEvent(new EntityDamageEvent(victim, DamageCause.CUSTOM, 100)); // doesn't actually damage
		}
	}

	public static void debug(String message) {
		if (debug == true)
			Common.log(message);
	}
}

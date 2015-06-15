package io.github.archemedes.knockoutplus;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DamageApi
{
  public double getDamage(LivingEntity target, double rawDamage, EntityDamageEvent.DamageCause cause)
  {
    if (rawDamage <= 0.0D) return 0.0D;

    float dmg = (float)rawDamage;

    if (target.getNoDamageTicks() > target.getMaximumNoDamageTicks() / 2.0F) {
      dmg = (float)(dmg - target.getLastDamage());
      if (dmg <= 0.0F) return 0.0D;
    }

    if (isBlockedByArmor(cause)) {
      if (((target instanceof HumanEntity)) && 
        (((HumanEntity)target).isBlocking()) && (dmg > 0.0F)) dmg = (1.0F + dmg) * 0.5F;

      int armorValue = 0;
      for (ItemStack arms : target.getEquipment().getArmorContents()) {
        if (arms != null) armorValue += getArmorValue(arms);
      }
      dmg *= (1.0F - armorValue / 25.0F);
    }

    if ((cause != EntityDamageEvent.DamageCause.SUICIDE) && (cause != EntityDamageEvent.DamageCause.VOID)) {
      dmg = getPotionReduction(target, dmg);
      if (dmg <= 0.0F) return 0.0D;

    }

    dmg = getEnchantmentReduction(target, dmg, cause);

    return dmg;
  }

  private float getPotionReduction(LivingEntity target, float dmg)
  {
    for (PotionEffect effect : target.getActivePotionEffects()) {
      if (effect.getType() == PotionEffectType.DAMAGE_RESISTANCE) {
        int lvl = effect.getAmplifier() + 1;
        dmg *= (1.0F - lvl / 5.0F);
        break;
      }
    }

    return dmg;
  }

  private float getEnchantmentReduction(LivingEntity target, float dmg, EntityDamageEvent.DamageCause cause) {
    int epf = 0;

    for (ItemStack arms : target.getEquipment().getArmorContents()) {
      if (arms != null) epf += getEnchantmentEPF(arms, cause);
    }

    epf = (int)Math.ceil(Math.min(25, epf) * 0.5F);

    return dmg * (1.0F - epf / 25.0F);
  }

  private int getEnchantmentEPF(ItemStack is, EntityDamageEvent.DamageCause cause)
  {
    switch (cause) { case MAGIC:
    case POISON:
      return 0;
    case ENTITY_EXPLOSION:
    case FALL:
    case FIRE:
      if (is.containsEnchantment(Enchantment.PROTECTION_FIRE))
        return getEPFFor(Enchantment.PROTECTION_FIRE, is.getEnchantmentLevel(Enchantment.PROTECTION_FIRE));
      if (is.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL))
        return getEPFFor(Enchantment.PROTECTION_ENVIRONMENTAL, is.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
      return 0;
    case CUSTOM:
      if (is.containsEnchantment(Enchantment.PROTECTION_PROJECTILE))
        return getEPFFor(Enchantment.PROTECTION_PROJECTILE, is.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE));
      if (is.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL))
        return getEPFFor(Enchantment.PROTECTION_ENVIRONMENTAL, is.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
      return 0;
    case LAVA:
    case LIGHTNING:
      if (is.containsEnchantment(Enchantment.PROTECTION_EXPLOSIONS))
        return getEPFFor(Enchantment.PROTECTION_EXPLOSIONS, is.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS));
      if (is.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL))
        return getEPFFor(Enchantment.PROTECTION_ENVIRONMENTAL, is.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
      return 0;
    case ENTITY_ATTACK:
      int val = 0;
      if (is.containsEnchantment(Enchantment.PROTECTION_EXPLOSIONS))
        val += getEPFFor(Enchantment.PROTECTION_EXPLOSIONS, is.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS));
      if (is.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL)) {
        val += getEPFFor(Enchantment.PROTECTION_ENVIRONMENTAL, is.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
      }
      return val;
    case DROWNING:
    case FALLING_BLOCK:
    case FIRE_TICK:
    case MELTING: } if (is.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL))
      return getEPFFor(Enchantment.PROTECTION_ENVIRONMENTAL, is.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
    return 0;
  }

  private int getEPFFor(Enchantment enchant, int level)
  {
    return (int)Math.floor((6 + level * level) * getTypeEnchantmentModifier(enchant) / 3.0F);
  }

  private float getTypeEnchantmentModifier(Enchantment enchant) {
    if (Enchantment.PROTECTION_ENVIRONMENTAL.equals(enchant)) return 0.75F;
    if (Enchantment.PROTECTION_FIRE.equals(enchant)) return 1.25F;
    if (Enchantment.PROTECTION_EXPLOSIONS.equals(enchant)) return 1.5F;
    if (Enchantment.PROTECTION_PROJECTILE.equals(enchant)) return 1.5F;
    if (Enchantment.PROTECTION_FALL.equals(enchant)) return 2.5F;

    return 0.0F;
  }

  private boolean isBlockedByArmor(EntityDamageEvent.DamageCause cause) {
    switch (cause) { case BLOCK_EXPLOSION:
    case CONTACT:
    case CUSTOM:
    case ENTITY_EXPLOSION:
    case FIRE:
    case LAVA:
    case LIGHTNING:
    case MELTING:
    case THORNS:
      return true;
    case DROWNING:
    case ENTITY_ATTACK:
    case FALL:
    case FALLING_BLOCK:
    case FIRE_TICK:
    case MAGIC:
    case POISON:
    case PROJECTILE:
    case STARVATION:
    case SUFFOCATION:
    case SUICIDE: } return false;
  }

  private int getArmorValue(ItemStack armor)
  {
    Material mat = armor.getType();

    if ((mat == Material.LEATHER_HELMET) || (mat == Material.LEATHER_BOOTS) || 
      (mat == Material.GOLD_BOOTS) || (mat == Material.CHAINMAIL_BOOTS))
      return 1;
    if ((mat == Material.LEATHER_LEGGINGS) || (mat == Material.GOLD_HELMET) || 
      (mat == Material.CHAINMAIL_HELMET) || (mat == Material.IRON_HELMET) || 
      (mat == Material.IRON_BOOTS))
      return 2;
    if ((mat == Material.LEATHER_CHESTPLATE) || (mat == Material.GOLD_LEGGINGS) || 
      (mat == Material.DIAMOND_BOOTS) || (mat == Material.DIAMOND_HELMET))
      return 3;
    if (mat == Material.CHAINMAIL_LEGGINGS)
      return 4;
    if ((mat == Material.GOLD_CHESTPLATE) || (mat == Material.CHAINMAIL_CHESTPLATE) || (mat == Material.IRON_LEGGINGS))
      return 5;
    if ((mat == Material.IRON_CHESTPLATE) || (mat == Material.DIAMOND_LEGGINGS))
      return 6;
    if (mat == Material.DIAMOND_CHESTPLATE)
      return 8;
    return 0;
  }
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.DamageApi
 * JD-Core Version:    0.6.2
 */
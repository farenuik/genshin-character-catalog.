package com.myproject.game;

/** An item that modifies character stats and can grant an ability. */
public final class Equipment {
    private final String name;
    private final int attackBonus;
    private final int armorBonus;
    private final Ability grantedAbility;
    private final String description;
    private final int rarity;
    private final String weaponType;

    public Equipment(String name, int attackBonus, int armorBonus, Ability grantedAbility) {
        this(name, attackBonus, armorBonus, grantedAbility, "", 0, "");
    }

    public Equipment(String name, int attackBonus, int armorBonus, Ability grantedAbility,
                     String description, int rarity, String weaponType) {
        if (name == null || name.isBlank() || attackBonus < 0 || armorBonus < 0) {
            throw new IllegalArgumentException("Equipment name must not be blank and bonuses must be non-negative");
        }
        if (rarity < 0) throw new IllegalArgumentException("Rarity must not be negative");
        this.name = name;
        this.attackBonus = attackBonus;
        this.armorBonus = armorBonus;
        this.grantedAbility = grantedAbility;
        this.description = description == null ? "" : description;
        this.rarity = rarity;
        this.weaponType = weaponType == null ? "" : weaponType;
    }

    public String getName() { return name; }
    public int getAttackBonus() { return attackBonus; }
    public int getArmorBonus() { return armorBonus; }
    public Ability getGrantedAbility() { return grantedAbility; }
    public String getDescription() { return description; }
    public int getRarity() { return rarity; }
    public String getWeaponType() { return weaponType; }
}
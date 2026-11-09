package com.myproject.game;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** A game participant with stats, equipment, combat and special abilities. */
public final class Character {
    private final String name;
    private final int maxHealth;
    private int health;
    private int armor;
    private int attack;
    private boolean defending;
    private final Map<String, Ability> abilities = new LinkedHashMap<>();
    private final CharacterProfile profile;

    public Character(String name, int health, int armor, int attack) {
        this(name, health, armor, attack,
                new CharacterProfile(name, "", 0, "", "", List.of()));
    }

    public Character(String name, int health, int armor, int attack, CharacterProfile profile) {
        if (health <= 0 || armor < 0 || attack < 0) {
            throw new IllegalArgumentException("Stats must be non-negative and health must be positive");
        }
        this.name = name;
        this.maxHealth = health;
        this.health = health;
        this.armor = armor;
        this.attack = attack;
        this.profile = java.util.Objects.requireNonNull(profile, "profile");
    }

    public String getName() { return name; }
    public int getMaxHealth() { return maxHealth; }
    public int getHealth() { return health; }
    public int getArmor() { return armor; }
    public int getAttack() { return attack; }
    public boolean isDefending() { return defending; }
    public CharacterProfile getProfile() { return profile; }
    public Map<String, Ability> getAbilities() {
        return Collections.unmodifiableMap(abilities);
    }

    /** Deals attack damage reduced by the target's armor. */
    public String attack(Character target) {
        int damage = Math.max(1, attack - target.armor);
        if (target.defending) {
            damage = Math.max(1, damage / 2);
            target.defending = false;
        }
        target.takeDamage(damage);
        return name + " attacks " + target.name + " for " + damage + " damage.";
    }

    /** Restores health without exceeding the maximum. */
    public String heal(int amount) {
        int restored = Math.min(Math.max(0, amount), maxHealth - health);
        health += restored;
        return name + " heals for " + restored + " health.";
    }

    /** Reduces the next incoming basic attack's damage by half, with a minimum of 1. */
    public String defend() {
        defending = true;
        return name + " braces for the next attack.";
    }

    /** Uses one of this character's named abilities. */
    public String useAbility(String abilityName, Character target) {
        Ability ability = abilities.get(abilityName);
        if (ability == null) throw new IllegalArgumentException(name + " does not know " + abilityName);
        return ability.use(this, target);
    }

    /** Equips an item and applies its stat and ability changes. */
    public String equip(Equipment equipment) {
        attack += equipment.getAttackBonus();
        armor += equipment.getArmorBonus();
        if (equipment.getGrantedAbility() != null) {
            abilities.put(equipment.getName(), equipment.getGrantedAbility());
        }
        return name + " equips " + equipment.getName() + ".";
    }

    /** Gives the character a named ability. */
    public void learn(String abilityName, Ability ability) {
        if (abilityName == null || abilityName.isBlank() || ability == null) {
            throw new IllegalArgumentException("Ability name and ability must not be null or blank");
        }
        abilities.put(abilityName, ability);
    }

    /** Restores mutable combat values from a persisted game snapshot. */
    public void restoreCombatState(int health, int armor, int attack, boolean defending) {
        if (health < 0 || health > maxHealth || armor < 0 || attack < 0) {
            throw new IllegalArgumentException("Invalid combat state");
        }
        this.health = health;
        this.armor = armor;
        this.attack = attack;
        this.defending = defending;
    }

    State saveState() {
        return new State(health, armor, attack, defending, new LinkedHashMap<>(abilities));
    }

    void restoreState(State state) {
        health = state.health();
        armor = state.armor();
        attack = state.attack();
        defending = state.defending();
        abilities.clear();
        abilities.putAll(state.abilities());
    }

    record State(int health, int armor, int attack, boolean defending, Map<String, Ability> abilities) { }

    void takeDamage(int damage) { health = Math.max(0, health - Math.max(0, damage)); }
}
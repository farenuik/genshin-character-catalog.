package com.myproject.game;

/** Factory for abilities used by the demonstration. */
public final class Abilities {
    private Abilities() { }

    /** Creates a damaging fire ability. */
    public static Ability fireball() {
        return (user, target) -> {
            int damage = 12;
            target.takeDamage(damage);
            return user.getName() + " casts Fireball on " + target.getName() + " for " + damage + " damage.";
        };
    }

    /** Creates an ability that restores the user's health. */
    public static Ability healingLight() { return (user, target) -> user.heal(18); }
}
package com.myproject;

import com.myproject.game.Character;
import com.myproject.game.CharacterProfile;
import com.myproject.game.CharacterSkill;
import com.myproject.text.StructuredTextDirector;
import com.myproject.text.Text;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class CharacterTextRenderer {
    public String render(Character character) {
        CharacterProfile profile = character.getProfile();
        Text document = new StructuredTextDirector().construct(builder -> {
            builder.heading(1, character.getName());
            if (!profile.apiId().isBlank()) builder.paragraph("API id: " + profile.apiId());
            if (profile.rarity() > 0) builder.paragraph("Rarity: " + profile.rarity() + " stars");
            if (!profile.element().isBlank()) builder.paragraph("Element: " + profile.element());
            if (!profile.weaponType().isBlank()) builder.paragraph("Weapon: " + profile.weaponType());
            String imageUrl = graphicUrl(character);
            if (!imageUrl.isBlank()) builder.link("Graphic", imageUrl);
            builder.paragraph("HP: " + character.getHealth()
                    + " | Armor: " + character.getArmor()
                    + " | Attack: " + character.getAttack());
            if (!profile.description().isBlank()) {
                builder.heading(2, "Description").paragraph(profile.description());
            }
            if (!profile.skills().isEmpty()) {
                builder.heading(2, "Abilities");
                for (CharacterSkill skill : profile.skills()) {
                    if (!skill.name().isBlank()) builder.heading(3, skill.name());
                    if (!skill.type().isBlank()) builder.paragraph("Type: " + skill.type());
                    if (!skill.description().isBlank()) builder.paragraph(skill.description());
                }
            }
        });
        return document.toString();
    }

    public static String graphicUrl(Character character) {
        CharacterProfile profile = character.getProfile();
        if (!profile.imageUrl().isBlank()) return profile.imageUrl();
        if (profile.apiId().isBlank()) return "";
        String encodedId = URLEncoder.encode(profile.apiId(), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://genshin.jmp.blue/characters/" + encodedId + "/icon";
    }
}
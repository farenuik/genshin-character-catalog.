package com.myproject.api;

import com.myproject.game.Character;
import com.myproject.game.Equipment;
import java.io.IOException;
import java.util.List;

public interface GenshinDataSource {
    List<String> listCharacterIds() throws IOException;

    List<String> listWeaponIds() throws IOException;

    Character getCharacter(String id) throws IOException;

    Equipment getWeapon(String id) throws IOException;
}
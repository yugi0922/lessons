package org.example.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class GameTest {

    @Test
    void testHumanWarrior() {
        Profession warrior = new Warrior();
        Race humanWarrior = new Human(warrior);
        assertEquals("剣で斬りつける（汎用性の高い攻撃）", humanWarrior.attack());
        assertEquals("盾で防御する（素早い反応）", humanWarrior.defend());
        assertEquals("バーサーク状態になる（適応力を活かして）", humanWarrior.useSpecial());
    }

    @Test
    void testElfMage() {
        Profession mage = new Mage();
        Race elfMage = new Elf(mage);
        assertEquals("魔法の弾を放つ 優雅に", elfMage.attack());
        assertEquals("魔法の障壁を張る 軽やかに", elfMage.defend());
        assertEquals("強力な範囲魔法を唱える 自然の力を借りて", elfMage.useSpecial());
    }

    @Test
    void testDwarfArcher() {
        Profession archer = new Archer();
        Race dwarfArcher = new Dwarf(archer);
        assertEquals("矢を放つ 力強く", dwarfArcher.attack());
        assertEquals("素早く回避する 頑丈に", dwarfArcher.defend());
        assertEquals("貫通矢を放つ 職人技を活かして", dwarfArcher.useSpecial());
    }

    @Test
    void testChangingProfession() {
        Profession warrior = new Warrior();
        Profession mage = new Mage();
        Race human = new Human(warrior);
        assertEquals("剣で斬りつける 汎用性の高い攻撃", human.attack());
        
        // 職業を変更
        human = new Human(mage);
        assertEquals("魔法の弾を放つ 汎用性の高い攻撃", human.attack());
    }

    @Test
    void testAllRacesWithWarrior() {
        Profession warrior = new Warrior();
        Race human = new Human(warrior);
        Race elf = new Elf(warrior);
        Race dwarf = new Dwarf(warrior);

        assertEquals("剣で斬りつける 汎用性の高い攻撃", human.attack());
        assertEquals("剣で斬りつける 優雅に", elf.attack());
        assertEquals("剣で斬りつける 力強く", dwarf.attack());
    }

    @Test
    void testAllProfessionsWithElf() {
        Race elfWarrior = new Elf(new Warrior());
        Race elfMage = new Elf(new Mage());
        Race elfArcher = new Elf(new Archer());

        assertEquals("剣で斬りつける 優雅に", elfWarrior.attack());
        assertEquals("魔法の弾を放つ 優雅に", elfMage.attack());
        assertEquals("矢を放つ 優雅に", elfArcher.attack());
    }
}

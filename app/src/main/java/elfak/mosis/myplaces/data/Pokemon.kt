package elfak.mosis.myplaces.data


data class Pokemon(
    var id: String = "",
    var name: String = "",
    var maxhp: Int = 100,
    var currenthp: Int = 100,
    var attack: Int = 10,
    var level: Int = 1,
    var xp: Int = 0,
    var ownerId: String = "",
    var alive: Boolean = true
) {
    // Dodaje XP posle pobede u borbi
    fun addBattleXP(amount: Int = 100) {
        // 1️⃣ Heal za pobedu: +50% od trenutnog maxHP pre level-up-a
        currenthp = (currenthp + (maxhp * 0.5)).toInt().coerceAtMost(maxhp)

        xp += amount
        var xpThreshold = level * 100

        // Level-up dok god imamo dovoljno XP
        while (xp >= xpThreshold) {
            xp -= xpThreshold
            levelUp()
            xpThreshold = level * 100
        }
    }

    // Funkcija koja povecava level, maxHP i attack
    private fun levelUp() {
        val oldMaxHp = maxhp

        // 2️⃣ Povećavamo level, maxHP i attack
        level += 1
        maxhp += 100
        attack += 10

        // 3️⃣ Trenutni HP proporcionalno novom maxHP
        val hpPercent = currenthp.toFloat() / oldMaxHp
        currenthp = (maxhp * hpPercent).toInt()
    }
}

package com.prayerquest.app.domain.content

/**
 * A Scripture passage curated for Lectio Divina: short enough to hold in
 * attention across the four movements (Lectio → Meditatio → Oratio →
 * Contemplatio), rich enough to reward meditation.
 *
 * All passages quoted here are from public-domain translations (KJV / WEB /
 * ASV mix, chosen for natural English rhythm) — no licensing review required.
 * Sprint 6 will replace these with a larger curated 50+ pack and let the user
 * switch translation per their Bible-app preference.
 */
data class LectioPassage(
    val id: String,
    val reference: String,
    val text: String,
    val category: Category,
    val prompt: String
) {
    enum class Category(val displayName: String) {
        PSALMS("Psalms"),
        GOSPELS("Gospels"),
        EPISTLES("Epistles"),
        PROPHETS("Prophets"),
        WISDOM("Wisdom")
    }
}

/**
 * Sprint 3 author-lite pack: 12 foundational passages that work well for
 * Lectio Divina across traditions. Sprint 6 pass will bring this to 50+ with
 * public-domain-verified sourcing and category-balanced breadth.
 *
 * IDs are stable — do not rename once released.
 */
object LectioPassageLibrary {

    val passages: List<LectioPassage> = listOf(
        LectioPassage(
            id = "psalm_23",
            reference = "Psalm 23",
            text = "The Lord is my shepherd; I shall not want. He makes me to lie down in green pastures: he leads me beside the still waters. He restores my soul: he leads me in the paths of righteousness for his name's sake. Yea, though I walk through the valley of the shadow of death, I will fear no evil: for you are with me; your rod and your staff they comfort me.",
            category = LectioPassage.Category.PSALMS,
            prompt = "Which image — the pasture, the waters, the valley, the staff — is the Lord inviting you into today?"
        ),
        LectioPassage(
            id = "psalm_46_1_5",
            reference = "Psalm 46:1–5",
            text = "God is our refuge and strength, a very present help in trouble. Therefore will we not fear, though the earth be removed, and though the mountains be carried into the midst of the sea. There is a river, whose streams shall make glad the city of God, the holy place of the tabernacles of the Most High. God is in the midst of her; she shall not be moved.",
            category = LectioPassage.Category.PSALMS,
            prompt = "Where do you need God to be a refuge today? What fear are you being invited to release?"
        ),
        LectioPassage(
            id = "psalm_91_1_6",
            reference = "Psalm 91:1–6",
            text = "He who dwells in the secret place of the Most High shall abide under the shadow of the Almighty. I will say of the Lord, 'He is my refuge and my fortress: my God; in him will I trust.' Surely he shall deliver you from the snare of the fowler, and from the deadly pestilence. He shall cover you with his feathers, and under his wings shall you trust.",
            category = LectioPassage.Category.PSALMS,
            prompt = "What does it look like to dwell in the secret place today? Sit with one image that draws you."
        ),
        LectioPassage(
            id = "psalm_139_1_10",
            reference = "Psalm 139:1–10",
            text = "O Lord, you have searched me, and known me. You know my sitting down and my rising up, you understand my thought afar off. You compass my path and my lying down, and are acquainted with all my ways. For there is not a word in my tongue, but, lo, O Lord, you know it altogether. Where shall I go from your Spirit? Or where shall I flee from your presence?",
            category = LectioPassage.Category.PSALMS,
            prompt = "God already sees all of you. Which part of yourself are you being invited to bring honestly before him?"
        ),
        LectioPassage(
            id = "isaiah_40_28_31",
            reference = "Isaiah 40:28–31",
            text = "Have you not known? Have you not heard, that the everlasting God, the Lord, the Creator of the ends of the earth, does not faint, nor is weary? There is no searching of his understanding. He gives power to the faint; and to them that have no might he increases strength. They that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.",
            category = LectioPassage.Category.PROPHETS,
            prompt = "Where are you faint or weary? What would it look like to 'wait upon the Lord' in that place?"
        ),
        LectioPassage(
            id = "matthew_5_3_10",
            reference = "Matthew 5:3–10 (Beatitudes)",
            text = "Blessed are the poor in spirit: for theirs is the kingdom of heaven. Blessed are they that mourn: for they shall be comforted. Blessed are the meek: for they shall inherit the earth. Blessed are they who hunger and thirst after righteousness: for they shall be filled. Blessed are the merciful: for they shall obtain mercy. Blessed are the pure in heart: for they shall see God. Blessed are the peacemakers: for they shall be called children of God. Blessed are they who are persecuted for righteousness' sake: for theirs is the kingdom of heaven.",
            category = LectioPassage.Category.GOSPELS,
            prompt = "Which beatitude feels closest to where you are? Which feels most foreign?"
        ),
        LectioPassage(
            id = "matthew_6_25_33",
            reference = "Matthew 6:25–33",
            text = "Take no thought for your life, what you shall eat, or what you shall drink; nor for your body, what you shall put on. Is not the life more than food, and the body than clothing? Behold the birds of the air: for they sow not, neither do they reap, nor gather into barns; yet your heavenly Father feeds them. Are you not much better than they? Seek first the kingdom of God, and his righteousness; and all these things shall be added to you.",
            category = LectioPassage.Category.GOSPELS,
            prompt = "Which worry are you being invited to hand over today?"
        ),
        LectioPassage(
            id = "john_15_4_9",
            reference = "John 15:4–9",
            text = "Abide in me, and I in you. As the branch cannot bear fruit of itself, except it abide in the vine; no more can you, except you abide in me. I am the vine, you are the branches: He who abides in me, and I in him, the same brings forth much fruit: for without me you can do nothing. As the Father has loved me, so have I loved you: continue in my love.",
            category = LectioPassage.Category.GOSPELS,
            prompt = "What does 'abide' mean for you today? Where might you be trying to bear fruit apart from the Vine?"
        ),
        LectioPassage(
            id = "romans_8_35_39",
            reference = "Romans 8:35–39",
            text = "Who shall separate us from the love of Christ? Shall tribulation, or distress, or persecution, or famine, or nakedness, or peril, or sword? No, in all these things we are more than conquerors through him who loved us. For I am persuaded, that neither death, nor life, nor angels, nor principalities, nor powers, nor things present, nor things to come, nor height, nor depth, nor any other creature, shall be able to separate us from the love of God, which is in Christ Jesus our Lord.",
            category = LectioPassage.Category.EPISTLES,
            prompt = "Name the thing that feels like it could separate you from God's love. Sit with this promise against it."
        ),
        LectioPassage(
            id = "philippians_4_4_9",
            reference = "Philippians 4:4–9",
            text = "Rejoice in the Lord always: and again I say, Rejoice. Let your gentleness be known to all. The Lord is at hand. Be anxious for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known to God. And the peace of God, which passes all understanding, shall guard your hearts and minds through Christ Jesus.",
            category = LectioPassage.Category.EPISTLES,
            prompt = "What anxiety are you holding? Practice releasing it with thanksgiving — name one thing you're grateful for as you do."
        ),
        LectioPassage(
            id = "ephesians_3_14_19",
            reference = "Ephesians 3:14–19",
            text = "For this cause I bow my knees before the Father of our Lord Jesus Christ, that he would grant you, according to the riches of his glory, to be strengthened with might by his Spirit in the inner man; that Christ may dwell in your hearts by faith; that you, being rooted and grounded in love, may be able to comprehend with all saints what is the breadth, and length, and depth, and height; and to know the love of Christ, which passes knowledge.",
            category = LectioPassage.Category.EPISTLES,
            prompt = "Pray Paul's prayer for yourself — that you might 'know the love of Christ' in a new way today."
        ),
        LectioPassage(
            id = "proverbs_3_5_6",
            reference = "Proverbs 3:5–6",
            text = "Trust in the Lord with all your heart, and lean not unto your own understanding. In all your ways acknowledge him, and he shall direct your paths.",
            category = LectioPassage.Category.WISDOM,
            prompt = "Which 'way' are you being invited to acknowledge him in today?"
        )
    )

    val default: LectioPassage = passages.first { it.id == "psalm_23" }

    fun byId(id: String): LectioPassage? = passages.firstOrNull { it.id == id }
}

package com.prayerquest.app.domain.tagging

/**
 * On-device auto-tagger for newly created prayer items. Implements the
 * lightweight keyword heuristic described in DD §3.5.1 — strictly regex-based,
 * no network, no AI, no on-device ML. Runs in < 1 ms on even large inputs and
 * has zero runtime dependencies, which is why it lives under `domain/` as a
 * pure Kotlin singleton.
 *
 * Usage:
 *
 *     val tags = PrayerTagger.suggest(title, description)
 *
 * The caller is expected to render up to [MAX_SUGGESTIONS] chips and persist
 * whichever the user accepts. Manual category selection always wins — this
 * tagger is a *suggestion* surface, never an authority.
 */
object PrayerTagger {

    /**
     * Twelve categories surfaced as suggestion chips. [OTHER] is a catch-all
     * that the tagger itself never emits (it has no keyword group) — it exists
     * in the enum so downstream code has a fallback value when the user wants
     * to opt out of the suggested set and pick something generic.
     */
    enum class Category(val displayName: String) {
        FAMILY("Family"),
        HEALTH("Health"),
        WORK_VOCATION("Work/Vocation"),
        FINANCES("Finances"),
        SPIRITUAL_GROWTH("Spiritual Growth"),
        RELATIONSHIPS("Relationships"),
        COMMUNITY_FRIENDS("Community/Friends"),
        NATION_WORLD("Nation/World"),
        TRAVEL("Travel"),
        GRIEF_LOSS("Grief/Loss"),
        THANKSGIVING("Thanksgiving"),
        OTHER("Other")
    }

    /**
     * A single suggested tag with the regex-derived confidence (0f–1f) and
     * the first keyword that matched. The matched-keyword field is mostly
     * for UX affordances (accessibility hints, debug overlays) and unit-test
     * assertions — the UI binds to [category] and [confidence] only.
     */
    data class SuggestedTag(
        val category: Category,
        val confidence: Float,
        val matchedKeyword: String
    )

    /**
     * Minimum confidence a category must clear to be emitted. Tuned against
     * the DD §3.5.1 examples ("Healing for Mom's surgery" → Family + Health,
     * "Promotion at work" → Work/Vocation): a single keyword hit on a short
     * 3–4 word title lands at 0.25–0.33 so 0.15 comfortably admits them while
     * still filtering 1-in-100-words noise in long journal-style descriptions.
     */
    const val DEFAULT_THRESHOLD: Float = 0.15f

    /** We only ever surface the top three chips — more would overwhelm the form. */
    const val MAX_SUGGESTIONS: Int = 3

    /**
     * Per-match score weight. Kept at 1.0f for v1 — if a future sprint wants
     * to bias high-signal phrases (e.g., "cancer" > "pain") we can lift this
     * to a per-regex weight without changing the suggest() contract.
     */
    private const val MATCH_WEIGHT: Float = 1.0f

    /**
     * Immutable keyword catalogue. Each category has 20–40 word-boundary
     * patterns covering synonym families. All regexes are compiled with
     * [RegexOption.IGNORE_CASE] so user input capitalisation is irrelevant.
     *
     * A few curation principles that drove the word choices here:
     * - Synonym families live together (mom/mother/mama/mommy) so a user who
     *   writes informally gets the same tag as one who writes formally.
     * - Overlap between categories is resolved by "dominant meaning" — e.g.,
     *   "tithe" is Finances (monetary act) not Spiritual Growth; "retirement"
     *   is Work/Vocation (life transition) but "401k" is Finances.
     * - Possessives like "Mom's" are handled implicitly: `\b` sits between
     *   the word and the apostrophe, so `\bmom\b` still matches.
     * - Multi-word phrases (e.g., "passed away") are included verbatim — the
     *   regex engine treats them as a single match and a single keyword hit.
     */
    private val KEYWORDS: Map<Category, List<Regex>> = buildMap {
        put(
            Category.FAMILY,
            wordPatterns(
                "mom", "mother", "mum", "mommy", "mama",
                "dad", "father", "daddy", "papa", "pop",
                "son", "daughter", "child", "children", "kid", "kids", "baby", "babies",
                "sister", "brother", "sibling", "siblings", "twin", "twins",
                "aunt", "uncle", "cousin", "cousins", "nephew", "niece",
                "grandma", "grandpa", "grandmother", "grandfather",
                "grandparent", "grandparents", "grandchild", "grandchildren",
                "grandson", "granddaughter",
                "wife", "husband", "spouse",
                "family", "families", "household", "parent", "parents",
                "stepdad", "stepmom", "stepfather", "stepmother",
                "stepson", "stepdaughter", "stepchild",
                "relatives", "in-law", "in-laws"
            )
        )
        put(
            Category.HEALTH,
            wordPatterns(
                "health", "healing", "heal", "heals", "cure", "cured",
                "sick", "sickness", "illness", "ill", "disease", "diseases",
                "pain", "painful", "suffering", "hurts", "hurting",
                "surgery", "operation", "procedure",
                "doctor", "doctors", "physician", "nurse", "nurses",
                "cancer", "tumor", "tumour", "chemo", "chemotherapy", "radiation",
                "hospital", "clinic", "emergency",
                "recovery", "recovering", "recuperate",
                "medicine", "medication", "meds", "treatment", "therapy",
                "depression", "anxiety", "panic", "stress",
                "diagnosis", "diagnosed", "chronic",
                "covid", "flu", "virus", "infection",
                "pregnancy", "pregnant", "miscarriage", "stillbirth",
                "dementia", "alzheimer", "alzheimers", "stroke", "diabetes",
                "addiction", "addict", "sobriety", "sober"
            )
        )
        put(
            Category.WORK_VOCATION,
            wordPatterns(
                "job", "jobs", "career", "careers", "work", "working", "workplace", "workload",
                "boss", "manager", "supervisor", "colleague", "coworker", "co-worker",
                "promotion", "promoted", "raise", "interview", "interviews",
                "hiring", "hired",
                "fired", "layoff", "laid off", "unemployed", "unemployment",
                "meeting", "meetings", "project", "projects", "deadline", "deadlines",
                "presentation", "presentations",
                "business", "company", "office",
                "vocation", "calling", "ministry", "pastoring",
                "entrepreneur", "startup",
                "resume", "applying",
                "school", "college", "university", "classes", "exam", "exams",
                "grades", "graduation", "student", "students", "teacher", "professor",
                "tuition", "scholarship", "degree",
                "retirement", "retire", "retired"
            )
        )
        put(
            Category.FINANCES,
            wordPatterns(
                "money", "finances", "financial", "finance",
                "debt", "debts", "loan", "loans", "mortgage", "rent",
                "bills", "bill", "budget", "budgeting",
                "savings", "saving",
                "bankruptcy", "broke", "poverty",
                "income", "paycheck", "wages", "salary", "pay",
                "credit", "overdraft",
                "tax", "taxes",
                "investment", "investments", "401k",
                "stock", "stocks",
                "afford", "affording",
                "tithe", "tithing", "offering",
                "paycheck-to-paycheck", "paying", "owe", "owed"
            )
        )
        put(
            Category.SPIRITUAL_GROWTH,
            wordPatterns(
                "faith", "faithfulness", "belief", "believe", "believing",
                "trust", "trusting",
                "spiritual", "spiritually", "spirituality",
                "growth", "grow", "mature", "maturity",
                "holy spirit",
                "scripture", "bible", "gospel",
                "worship", "worshipping", "worshiping",
                "obedience", "obey", "surrender", "surrendering",
                "holiness", "sanctification", "righteousness",
                "sin", "sins", "repentance", "repent", "repenting",
                "strength", "courage", "wisdom", "patience", "self-control",
                "guidance", "discernment", "purpose",
                "discipleship", "disciple",
                "devotion", "devotional",
                "salvation", "saved",
                "fasting", "fast"
            )
        )
        put(
            Category.RELATIONSHIPS,
            wordPatterns(
                "marriage", "married", "wedding", "engaged", "engagement",
                "dating", "boyfriend", "girlfriend", "partner",
                "relationship", "relationships",
                "divorce", "divorced", "separated", "separation",
                "breakup", "break up", "broken up",
                "romance", "romantic",
                "fiance", "fiancee", "fiancé", "fiancée",
                "conflict", "argument", "quarrel",
                "reconciliation", "reconcile",
                "forgiveness", "forgive", "forgiving"
            )
        )
        put(
            Category.COMMUNITY_FRIENDS,
            wordPatterns(
                "friend", "friends", "friendship", "friendships",
                "community", "church", "congregation", "parish",
                "neighbor", "neighbors", "neighbour", "neighbours", "neighborhood",
                "small group", "life group", "bible study",
                "club", "team", "teammate", "teammates",
                "fellowship",
                "mentor", "mentee", "mentorship",
                "classmate", "classmates",
                "brethren", "sisters in christ", "brothers in christ"
            )
        )
        put(
            Category.NATION_WORLD,
            wordPatterns(
                "nation", "country", "nations", "world", "global",
                "government", "president", "congress", "senate", "senator",
                "election", "elections", "voting", "vote",
                "war", "wars", "refugee", "refugees", "peace",
                "israel", "ukraine", "russia", "china", "america", "usa",
                "persecution", "persecuted",
                "politicians", "leaders",
                "revival", "awakening",
                "missions", "missionaries", "missionary",
                "racism", "justice", "injustice",
                "hunger", "famine",
                "disaster", "earthquake", "hurricane", "flood", "wildfire", "wildfires",
                "pandemic"
            )
        )
        put(
            Category.TRAVEL,
            wordPatterns(
                "travel", "traveling", "travelling", "trip", "trips",
                "vacation", "holiday", "holidays",
                "flight", "flights", "flying", "airplane", "plane",
                "driving", "drive", "road trip",
                "journey", "journeying",
                "abroad", "overseas",
                "relocation", "moving", "relocating", "relocate",
                "cruise",
                "international",
                "mission trip", "missions trip",
                "passport", "visa"
            )
        )
        put(
            Category.GRIEF_LOSS,
            wordPatterns(
                "grief", "grieving", "mourning", "mourn",
                "loss", "losing",
                "death", "died", "dying", "dead",
                "funeral", "passed away", "passing",
                "widow", "widower",
                "bereaved", "bereavement",
                "heartbroken", "heartbreak",
                "tragedy", "tragic",
                "memorial", "remembrance",
                "lonely", "loneliness"
            )
        )
        put(
            Category.THANKSGIVING,
            wordPatterns(
                "thanks", "thank", "thankful", "thankfulness",
                "grateful", "gratitude", "appreciation", "appreciate", "appreciating",
                "blessing", "blessings", "blessed",
                "praise", "praising",
                "rejoice", "rejoicing",
                "celebrate", "celebration", "celebrating"
            )
        )
        // Category.OTHER intentionally omitted — see KDoc on [Category.OTHER].
    }

    /**
     * Build case-insensitive word-boundary regexes from a raw word list.
     *
     * Words containing spaces, hyphens, or apostrophes are wrapped so that
     * the `\b` anchors still land on word characters at each end (e.g.,
     * "passed away" compiles to `\bpassed away\b`). Non-word characters
     * inside the keyword are escaped so "co-worker" matches literally rather
     * than as a regex range.
     */
    private fun wordPatterns(vararg keywords: String): List<Regex> =
        keywords.map { kw ->
            val escaped = Regex.escape(kw)
            Regex("""\b$escaped\b""", RegexOption.IGNORE_CASE)
        }

    /**
     * Score `title + description` against every keyword group and return the
     * top [MAX_SUGGESTIONS] categories whose confidence clears [threshold].
     *
     * Confidence formula (per DD §3.5.1): `(matches * weight) / totalWords`,
     * clamped to `[0f, 1f]`. Word count is taken from the combined text split
     * on whitespace, NOT from unique tokens — this is deliberate so that a
     * 3-word input with one keyword hit lands at 0.33 (confident enough to
     * suggest) while the same single hit in a 50-word journal lands at 0.02
     * (noise, correctly filtered).
     *
     * Returns an empty list for blank input — the caller should hide its chip
     * row in that case rather than rendering zero-height placeholders.
     */
    fun suggest(
        title: String,
        description: String = "",
        threshold: Float = DEFAULT_THRESHOLD
    ): List<SuggestedTag> {
        val combined = buildString {
            append(title.trim())
            if (description.isNotBlank()) {
                if (isNotEmpty()) append(' ')
                append(description.trim())
            }
        }
        if (combined.isBlank()) return emptyList()

        val words = combined.split(WHITESPACE).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val totalWords = words.size

        val suggestions = mutableListOf<SuggestedTag>()
        for ((category, regexes) in KEYWORDS) {
            var matchCount = 0
            var firstMatch: String? = null
            for (regex in regexes) {
                for (mr in regex.findAll(combined)) {
                    matchCount++
                    if (firstMatch == null) firstMatch = mr.value
                }
            }
            if (matchCount == 0 || firstMatch == null) continue
            val score = (matchCount * MATCH_WEIGHT / totalWords).coerceIn(0f, 1f)
            if (score < threshold) continue
            suggestions += SuggestedTag(
                category = category,
                confidence = score,
                matchedKeyword = firstMatch!!
            )
        }

        return suggestions
            .sortedWith(
                // Primary: descending confidence. Secondary: stable enum order
                // so deterministic output for equally-scoring categories (e.g.,
                // "my sister's surgery" → Family and Health both 1/3, Family
                // wins the tiebreak because it's declared first in the enum).
                compareByDescending<SuggestedTag> { it.confidence }
                    .thenBy { it.category.ordinal }
            )
            .take(MAX_SUGGESTIONS)
    }

    private val WHITESPACE = Regex("""\s+""")
}

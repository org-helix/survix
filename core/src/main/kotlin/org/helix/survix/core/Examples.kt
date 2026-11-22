package org.helix.survix.core

/** The example offered on first launch and from the library: it uses every kind of block. */
object Examples {

    fun visitFeedback(french: Boolean, now: Long): Survey {
        fun t(fr: String, en: String) = if (french) fr else en
        return Survey(
            title = t("Votre avis après la consultation", "Your feedback after the visit"),
            description = t(
                "Cinq minutes pour nous aider à mieux vous accueillir. Les réponses sont anonymes.",
                "Five minutes to help us welcome you better. Answers are anonymous.",
            ),
            createdAt = now,
            updatedAt = now,
            blocks = listOf(
                SectionBlock(
                    title = t("Votre venue", "Your visit"),
                    text = t("Quelques questions sur votre passage aujourd'hui.", "A few questions about your visit today."),
                ),
                DateQuestion(label = t("Date de la consultation", "Date of the visit"), required = true),
                ChoiceQuestion(
                    label = t("Comment êtes-vous venu·e ?", "How did you get here?"),
                    options = listOf(
                        Choice(label = t("À pied", "On foot")),
                        Choice(label = t("En transport en commun", "Public transport")),
                        Choice(label = t("En voiture ou à moto", "By car or motorbike")),
                    ),
                    allowOther = true,
                ),
                NumberQuestion(
                    label = t("Combien de temps avez-vous attendu ?", "How long did you wait?"),
                    min = 0.0,
                    unit = t("minutes", "minutes"),
                ),
                SectionBlock(title = t("L'accueil", "How you were received")),
                RatingQuestion(
                    label = t("Comment jugez-vous l'accueil ?", "How would you rate the welcome?"),
                    required = true,
                    max = 5,
                    minLabel = t("Mauvais", "Poor"),
                    maxLabel = t("Excellent", "Excellent"),
                ),
                YesNoQuestion(
                    label = t("Les explications étaient-elles claires ?", "Were the explanations clear?"),
                    required = true,
                ),
                ChoiceQuestion(
                    label = t("Qu'est-ce qui pourrait être amélioré ?", "What could be improved?"),
                    help = t("Plusieurs réponses possibles.", "Pick as many as you like."),
                    multiple = true,
                    options = listOf(
                        Choice(label = t("Le temps d'attente", "Waiting time")),
                        Choice(label = t("La signalétique", "Signs and directions")),
                        Choice(label = t("Le confort de la salle d'attente", "Waiting room comfort")),
                        Choice(label = t("L'accessibilité", "Accessibility")),
                    ),
                ),
                TextQuestion(
                    label = t("Un mot pour l'équipe ?", "Anything to tell the team?"),
                    long = true,
                    placeholder = t("Votre message", "Your message"),
                ),
            ),
        )
    }
}

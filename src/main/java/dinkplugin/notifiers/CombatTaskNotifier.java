package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.CombatAchievementData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CombatTaskNotifier extends BaseNotifier {
    private static final Pattern ACHIEVEMENT_PATTERN = Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) combat task: (?<task>.+)\\.");

    public CombatTaskNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyCombatTask() && super.isEnabled();
    }

    public void onGameMessage(String message) {
        if (isEnabled())
            parse(message).ifPresent(pair -> handle(pair.getLeft(), pair.getRight()));
    }

    private void handle(CombatAchievementTier tier, String task) {
        if (tier.ordinal() < plugin.getConfig().minCombatAchievementTier().ordinal())
            return;

        String player = Utils.getPlayerName(plugin.getClient());
        String message = StringUtils.replaceEach(
            plugin.getConfig().combatTaskMessage(),
            new String[] { "%USERNAME%", "%TIER%", "%TASK%" },
            new String[] { player, tier.toString(), task }
        );

        createMessage(DinkPluginConfig::combatTaskSendImage, NotificationBody.<CombatAchievementData>builder()
            .type(NotificationType.COMBAT_ACHIEVEMENT)
            .content(message)
            .playerName(player)
            .extra(new CombatAchievementData(tier, task))
            .build());
    }

    @VisibleForTesting
    static Optional<Pair<CombatAchievementTier, String>> parse(String message) {
        Matcher matcher = ACHIEVEMENT_PATTERN.matcher(message);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(matcher.group("tier"))
            .map(CombatAchievementTier.TIER_BY_LOWER_NAME::get)
            .map(tier -> Pair.of(tier, matcher.group("task")));
    }
}
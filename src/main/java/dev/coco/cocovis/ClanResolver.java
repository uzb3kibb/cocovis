package dev.coco.cocovis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Reads a player's clan name from the TAB list text. */
public final class ClanResolver {
    private static final long CACHE_NANOS = 1_000_000_000L;

    private record Entry(String clan, long expires) {}

    private static final Map<UUID, Entry> CACHE = new HashMap<>();
    private static String compiledFrom;
    private static Pattern compiled;

    private ClanResolver() {}

    private static Pattern pattern() {
        String src = CocoVisConfig.get().clanPattern;
        if (src == null || src.isEmpty()) {
            return null;
        }
        if (!src.equals(compiledFrom)) {
            compiledFrom = src;
            CACHE.clear();
            try {
                compiled = Pattern.compile(src);
            } catch (PatternSyntaxException e) {
                CocoVisClient.LOGGER.warn("Bad clanPattern in cocovis.json: {}", e.getMessage());
                compiled = null;
            }
        }
        return compiled;
    }

    /** @return clan name or null if the player has none / cannot be resolved. */
    public static String resolve(Player player) {
        long now = System.nanoTime();
        UUID id = player.getUUID();
        Entry cached = CACHE.get(id);
        if (cached != null && cached.expires() > now) {
            return cached.clan();
        }
        String clan = compute(player);
        CACHE.put(id, new Entry(clan, now + CACHE_NANOS));
        return clan;
    }

    private static String compute(Player player) {
        Pattern pat = pattern();
        if (pat == null) {
            return null;
        }
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) {
            return null;
        }
        PlayerInfo info = conn.getPlayerInfo(player.getUUID());
        if (info == null) {
            return null;
        }
        Component name = info.getTabListDisplayName();
        if (name == null) {
            name = Component.literal(player.getName().getString());
        }
        PlayerTeam team = info.getTeam();
        if (team != null) {
            name = PlayerTeam.formatNameForTeam(team, name);
        }
        Matcher m = pat.matcher(name.getString());
        if (m.find() && m.groupCount() >= 1 && m.group(1) != null) {
            String clan = m.group(1).trim();
            return clan.isEmpty() ? null : clan;
        }
        return null;
    }
}

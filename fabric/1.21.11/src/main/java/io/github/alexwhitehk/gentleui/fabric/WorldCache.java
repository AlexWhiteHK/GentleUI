package io.github.alexwhitehk.gentleui.fabric;

import io.github.alexwhitehk.gentleui.core.host.WorldInfo;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads singleplayer world summaries in the background for quick-resume entries. */
public final class WorldCache {
	private static final Logger LOG = LoggerFactory.getLogger("GentleUI");
	private static volatile List<WorldInfo> worlds = List.of();
	private static volatile List<LevelSummary> summaries = List.of();
	private static CompletableFuture<?> loading;
	private static long loadedAt;

	private WorldCache() {
	}

	public static List<WorldInfo> recent() {
		refreshIfStale();
		return worlds;
	}

	public static LevelSummary summary(String folder) {
		for (LevelSummary s : summaries) {
			if (s.getLevelId().equals(folder)) {
				return s;
			}
		}
		return null;
	}

	public static void invalidate() {
		loadedAt = 0L;
	}

	private static synchronized void refreshIfStale() {
		long now = System.currentTimeMillis();
		if (loading != null && !loading.isDone() || now - loadedAt < 15_000L) {
			return;
		}
		loadedAt = now;
		Minecraft mc = Minecraft.getInstance();
		try {
			LevelStorageSource source = mc.getLevelSource();
			LevelStorageSource.LevelCandidates candidates = source.findLevelCandidates();
			if (candidates.isEmpty()) {
				worlds = List.of();
				summaries = List.of();
				return;
			}
			loading = source.loadLevelSummaries(candidates).thenAccept(list -> {
				List<LevelSummary> sorted = new ArrayList<>(list);
				sorted.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());
				List<WorldInfo> out = new ArrayList<>();
				DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
				for (LevelSummary s : sorted) {
					if (s.isDisabled()) {
						continue;
					}
					String detail = fmt.format(Instant.ofEpochMilli(s.getLastPlayed()));
					out.add(new WorldInfo(s.getLevelId(), s.getLevelName(), s.getLastPlayed(), detail, null));
				}
				summaries = sorted;
				worlds = List.copyOf(out);
			});
		} catch (Exception e) {
			LOG.warn("GentleUI: could not list worlds", e);
		}
	}
}

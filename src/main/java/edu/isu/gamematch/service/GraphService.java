package edu.isu.gamematch.service;

import edu.isu.gamematch.*;
import org.jfree.chart.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphService {

    public byte[] generatePlaytimeChart(User user) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<Game> games = Collections.emptyList();
        if (user.getAchievementData() != null) {
            games = user.getAchievementData().stream()
                    .map(GameAchievement::getGame)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.comparingInt(Game::getPlaytime))   // ascending
                    .collect(Collectors.toList());
        }
        for (Game game : games) {
            // Truncate long names to avoid clutter
            String name = game.getGameName();
            if (name.length() > 20) name = name.substring(0, 18) + "_";
            double hours = game.getPlaytime() / 60.0;
            dataset.addValue(hours, "Hours", name);
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Game Playtime (ascending)", "Game", "Hours", dataset);
        // Rotate x-axis labels
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(
            org.jfree.chart.axis.CategoryLabelPositions.UP_45);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(baos, chart, 800, 600);
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
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

    public byte[] generatePlaytimeChart(List<Game> games) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    if (games == null || games.isEmpty()) {
        // return empty chart
        JFreeChart chart = ChartFactory.createBarChart("Game Playtime", "Game", "Hours", dataset);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(baos, chart, 800, 600);
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    // Sort by playtime ascending
    games.sort(Comparator.comparingInt(Game::getPlaytime));
    for (Game game : games) {
        String name = game.getGameName();
        if (name.length() > 20) name = name.substring(0, 18) + "…";
        double hours = game.getPlaytime() / 60.0;
        dataset.addValue(hours, "Hours", name);
    }
    JFreeChart chart = ChartFactory.createBarChart(
        "Game Playtime (ascending)", "Game", "Hours", dataset);
    chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(
        org.jfree.chart.axis.CategoryLabelPositions.UP_45);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
        ChartUtils.writeChartAsPNG(baos, chart, 800, 600);
        return baos.toByteArray();
    } catch (IOException e) { throw new RuntimeException(e); }
}
}
package com.gpsvariant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class GpsImageProcessingService {

    private final String apiKey;
    private final Path uploadDirectory;

    public GpsImageProcessingService(
            @Value("${google.maps.api-key:}") String apiKey,
            @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.apiKey = apiKey;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String generateGpsTaggedImage(
            String originalImagePath,
            String latitude,
            String longitude,
            String address) throws Exception {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps API key is not configured");
        }

        Path originalPath = Paths.get(originalImagePath).toAbsolutePath().normalize();
        if (!Files.exists(originalPath)) {
            throw new IllegalArgumentException("Original image not found");
        }

        BufferedImage originalImage = ImageIO.read(originalPath.toFile());
        if (originalImage == null) {
            throw new IllegalArgumentException("Uploaded file is not a readable image");
        }

        String mapUrl = "https://maps.googleapis.com/maps/api/staticmap"
                + "?center=" + latitude + "," + longitude
                + "&zoom=17"
                + "&size=600x400"
                + "&maptype=roadmap"
                + "&markers=color:red%7C" + latitude + "," + longitude
                + "&key=" + apiKey;

        BufferedImage mapImage = downloadMap(mapUrl);

        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();

        int panelWidth = Math.max(220, Math.min(imageWidth / 3, 320));
        int panelHeight = Math.max(260, Math.min(imageHeight / 2, 380));
        int panelX = Math.max(10, imageWidth - panelWidth - 20);
        int panelY = Math.max(10, imageHeight - panelHeight - 20);

        Graphics2D g = originalImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(new Color(0, 0, 0, 80));
            g.fillRoundRect(panelX + 8, panelY + 8, panelWidth, panelHeight, 35, 35);

            g.setColor(new Color(20, 20, 20, 220));
            g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 35, 35);

            int mapX = panelX + 20;
            int mapY = panelY + 20;
            int mapWidth = panelWidth - 40;
            int mapHeight = Math.min(190, panelHeight - 150);

            Shape oldClip = g.getClip();
            RoundRectangle2D round = new RoundRectangle2D.Float(
                    mapX, mapY, mapWidth, mapHeight, 25, 25);
            g.setClip(round);
            g.drawImage(mapImage, mapX, mapY, mapWidth, mapHeight, null);
            g.setClip(oldClip);

            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(mapX, mapY, mapWidth, mapHeight, 25, 25);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("GPS Location Details", panelX + 20, mapY + mapHeight + 34);

            g.setFont(new Font("Arial", Font.PLAIN, 15));
            g.drawString("Latitude : " + latitude, panelX + 20, mapY + mapHeight + 62);
            g.drawString("Longitude : " + longitude, panelX + 20, mapY + mapHeight + 88);

            String safeAddress = address == null ? "" : address;
            if (safeAddress.length() > 36) safeAddress = safeAddress.substring(0, 36) + "...";
            g.drawString(safeAddress, panelX + 20, mapY + mapHeight + 114);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"));
            g.drawString(timestamp, panelX + 20, mapY + mapHeight + 140);
        } finally {
            g.dispose();
        }

        Path outputFolder = uploadDirectory.resolve("final").normalize();
        Files.createDirectories(outputFolder);

        String finalFileName = UUID.randomUUID() + "_gps.png";
        Path outputPath = outputFolder.resolve(finalFileName).normalize();
        if (!outputPath.startsWith(uploadDirectory)) {
            throw new SecurityException("Invalid output path");
        }

        ImageIO.write(originalImage, "png", outputPath.toFile());
        return "/uploads/final/" + finalFileName;
    }

    private BufferedImage downloadMap(String mapUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(mapUrl).toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IllegalStateException("Google Maps request failed with HTTP " + status);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            BufferedImage mapImage = ImageIO.read(inputStream);
            if (mapImage == null) {
                throw new IllegalStateException("Google Maps did not return a valid image");
            }
            return mapImage;
        } finally {
            connection.disconnect();
        }
    }
}

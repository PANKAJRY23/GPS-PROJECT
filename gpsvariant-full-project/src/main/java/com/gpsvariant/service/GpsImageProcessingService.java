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

        /*
         * Google Static Maps supports up to 640x640 for the standard API.
         * scale=2 requests a 1280x1280 pixel image while keeping the same
         * geographic viewport. This is substantially sharper than the old
         * 600x400 image, especially when the map is placed inside the GPS
         * information panel.
         *
         * Zoom 18 gives a much closer view around the exact coordinate while
         * still showing enough surrounding roads/buildings to identify the
         * location clearly. The red marker remains centered on the supplied
         * longitude/latitude.
         */
        String mapUrl = "https://maps.googleapis.com/maps/api/staticmap"
                + "?center=" + latitude + "," + longitude
                + "&zoom=18"
                + "&size=640x640"
                + "&scale=2"
                + "&maptype=roadmap"
                + "&markers=color:red%7Clabel:L%7C" + latitude + "," + longitude
                + "&key=" + apiKey;

        BufferedImage mapImage = downloadMap(mapUrl);

        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();

        // Make the GPS panel materially larger than the previous 1/3-width
        // panel so that the map and location details remain readable.
        int panelWidth = Math.max(320, Math.min((int) (imageWidth * 0.40), 520));
        int panelHeight = Math.max(380, Math.min((int) (imageHeight * 0.52), 500));

        // Never allow the overlay to exceed the source image dimensions.
        panelWidth = Math.min(panelWidth, Math.max(1, imageWidth - 20));
        panelHeight = Math.min(panelHeight, Math.max(1, imageHeight - 20));

        int panelX = Math.max(10, imageWidth - panelWidth - 20);
        int panelY = Math.max(10, imageHeight - panelHeight - 20);

        Graphics2D g = originalImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

            g.setColor(new Color(0, 0, 0, 90));
            g.fillRoundRect(panelX + 8, panelY + 8, panelWidth, panelHeight, 35, 35);

            g.setColor(new Color(20, 20, 20, 230));
            g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 35, 35);

            int contentPadding = 20;
            int mapX = panelX + contentPadding;
            int mapY = panelY + contentPadding;
            int mapWidth = panelWidth - (contentPadding * 2);

            // Keep the map large enough to read while reserving space below it
            // for the exact coordinates, address and timestamp.
            int mapHeight = Math.min(280, panelHeight - 175);
            mapHeight = Math.max(180, mapHeight);

            Shape oldClip = g.getClip();
            RoundRectangle2D round = new RoundRectangle2D.Float(
                    mapX, mapY, mapWidth, mapHeight, 25, 25);
            g.setClip(round);

            // Draw the high-resolution Google map into the larger panel using
            // high-quality bicubic interpolation.
            g.drawImage(mapImage, mapX, mapY, mapWidth, mapHeight, null);
            g.setClip(oldClip);

            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(mapX, mapY, mapWidth, mapHeight, 25, 25);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("GPS Location Details", panelX + 20, mapY + mapHeight + 34);

            g.setFont(new Font("Arial", Font.PLAIN, 15));
            g.drawString("Latitude : " + latitude, panelX + 20, mapY + mapHeight + 62);
            g.drawString("Longitude : " + longitude, panelX + 20, mapY + mapHeight + 88);

            String safeAddress = address == null ? "" : address.trim();
            if (safeAddress.length() > 40) {
                safeAddress = safeAddress.substring(0, 40) + "...";
            }
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

        // PNG keeps the map labels/text lossless. Do not convert the final
        // GPS-tagged image to JPEG, which would introduce additional blur.
        ImageIO.write(originalImage, "png", outputPath.toFile());
        return "/uploads/final/" + finalFileName;
    }

    private BufferedImage downloadMap(String mapUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(mapUrl).toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "GPSVariant/1.0");

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

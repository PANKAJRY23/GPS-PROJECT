package com.gpsvariant.controller;

import com.gpsvariant.DTO.SaveGpsRequest;
import com.gpsvariant.entity.GpsFinalData;
import com.gpsvariant.entity.GpsImage;
import com.gpsvariant.entity.User;
import com.gpsvariant.repository.GpsFinalDataRepository;
import com.gpsvariant.repository.GpsImageRepository;
import com.gpsvariant.repository.UserRepository;
import com.gpsvariant.service.GpsImageProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final GpsImageRepository imageRepository;
    private final GpsImageProcessingService gpsImageProcessingService;
    private final GpsFinalDataRepository gpsFinalDataRepository;
    private final UserRepository userRepository;

    public MapController(
            GpsImageRepository imageRepository,
            GpsImageProcessingService gpsImageProcessingService,
            GpsFinalDataRepository gpsFinalDataRepository,
            UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.gpsImageProcessingService = gpsImageProcessingService;
        this.gpsFinalDataRepository = gpsFinalDataRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateMap(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) throws Exception {

        User user = currentUser(authentication);
        Long imageId = requiredLong(payload, "imageId");
        String latitude = requiredString(payload, "latitude");
        String longitude = requiredString(payload, "longitude");
        String address1 = stringValue(payload.get("address1"));
        String address2 = stringValue(payload.get("address2"));

        double lat = parseLatitude(latitude);
        double lng = parseLongitude(longitude);
        String fullAddress = (address1 + " " + address2).trim();

        GpsImage image = imageRepository.findByIdAndUserId(imageId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Main image not found"));

        String finalImagePath = gpsImageProcessingService.generateGpsTaggedImage(
                image.getImagePath(),
                Double.toString(lat),
                Double.toString(lng),
                fullAddress);

        image.setLatitude(lat);
        image.setLongitude(lng);
        image.setAddress(fullAddress);
        image.setMapImageUrl(finalImagePath);
        imageRepository.save(image);

        return ResponseEntity.ok(Map.of("mapImagePath", finalImagePath));
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(
            @RequestBody SaveGpsRequest request,
            Authentication authentication) {

        User user = currentUser(authentication);

        if (request.getMainImageId() == null) {
            throw new IllegalArgumentException("Main image ID is required");
        }
        if (request.getSecondImageId() == null) {
            throw new IllegalArgumentException("Second image ID is required");
        }
        if (request.getMainImageId().equals(request.getSecondImageId())) {
            throw new IllegalArgumentException("Main and second images must be different");
        }
        if (request.getMapImagePath() == null || request.getMapImagePath().isBlank()) {
            throw new IllegalArgumentException("Map image is required");
        }

        GpsImage mainImage = imageRepository.findByIdAndUserId(
                        request.getMainImageId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Main image not found"));

        GpsImage secondImage = imageRepository.findByIdAndUserId(
                        request.getSecondImageId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Second image not found"));

        GpsFinalData finalData = new GpsFinalData();
        finalData.setUser(user);
        finalData.setMainImage(mainImage);
        finalData.setSecondImage(secondImage);
        finalData.setMapImagePath(request.getMapImagePath());
        finalData.setLatitude(request.getLatitude());
        finalData.setLongitude(request.getLongitude());
        finalData.setAddress1(request.getAddress1());
        finalData.setAddress2(request.getAddress2());
        finalData.setCreatedOn(LocalDateTime.now());

        gpsFinalDataRepository.save(finalData);

        return ResponseEntity.ok(Map.of(
                "message", "Saved Successfully",
                "id", finalData.getId()
        ));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName().trim())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));
    }

    private Long requiredLong(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private String requiredString(Map<String, Object> payload, String name) {
        String value = stringValue(payload.get(name));
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private double parseLatitude(String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < -90 || parsed > 90) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Latitude must be a number between -90 and 90");
        }
    }

    private double parseLongitude(String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < -180 || parsed > 180) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Longitude must be a number between -180 and 180");
        }
    }
}

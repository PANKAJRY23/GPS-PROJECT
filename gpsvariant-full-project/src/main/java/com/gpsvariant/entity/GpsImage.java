
package com.gpsvariant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class GpsImage {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String imagePath;
 private String mapImageUrl;
 private Double latitude;
 private Double longitude;
 private String address;
 private LocalDateTime uploadedAt = LocalDateTime.now();
 

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "user_id")
 private User user;

 public Long getId(){ return id; }
 public User getUser(){ return user; }
 public void setUser(User user){ this.user=user; }

 public String getImagePath(){ return imagePath; }
 public void setImagePath(String imagePath){ this.imagePath=imagePath; }

 public String getMapImageUrl(){ return mapImageUrl; }
 public void setMapImageUrl(String mapImageUrl){ this.mapImageUrl=mapImageUrl; }

 public Double getLatitude(){ return latitude; }
 public void setLatitude(Double latitude){ this.latitude=latitude; }

 public Double getLongitude(){ return longitude; }
 public void setLongitude(Double longitude){ this.longitude=longitude; }

 public String getAddress(){ return address; }
 public void setAddress(String address){ this.address=address; }
}

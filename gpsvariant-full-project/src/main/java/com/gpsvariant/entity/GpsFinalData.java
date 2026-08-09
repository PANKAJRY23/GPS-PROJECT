package com.gpsvariant.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;

@Entity
public class GpsFinalData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne
    @JoinColumn(name="main_image_id")
    private GpsImage mainImage;



    @OneToOne
    @JoinColumn(name="second_image_id")
    private GpsImage secondImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;



    private String mapImagePath;


    private String latitude;


    private String longitude;


    private String address1;


    private String address2;


    private LocalDateTime createdOn;


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public GpsImage getMainImage() {
		return mainImage;
	}


	public void setMainImage(GpsImage mainImage) {
		this.mainImage = mainImage;
	}


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

	public GpsImage getSecondImage() {
		return secondImage;
	}


	public void setSecondImage(GpsImage secondImage) {
		this.secondImage = secondImage;
	}


	public String getMapImagePath() {
		return mapImagePath;
	}


	public void setMapImagePath(String mapImagePath) {
		this.mapImagePath = mapImagePath;
	}


	public String getLatitude() {
		return latitude;
	}


	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}


	public String getLongitude() {
		return longitude;
	}


	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}


	public String getAddress1() {
		return address1;
	}


	public void setAddress1(String address1) {
		this.address1 = address1;
	}


	public String getAddress2() {
		return address2;
	}


	public void setAddress2(String address2) {
		this.address2 = address2;
	}


	public LocalDateTime getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(LocalDateTime createdOn) {
		this.createdOn = createdOn;
	}


    // getters setters

}
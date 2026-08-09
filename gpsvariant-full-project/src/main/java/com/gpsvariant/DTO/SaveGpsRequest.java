package com.gpsvariant.DTO;

public class SaveGpsRequest {


    private Long mainImageId;


    private Long secondImageId;


    private String mapImagePath;


    private String latitude;


    private String longitude;


    private String address1;


    private String address2;



    public Long getMainImageId() {
        return mainImageId;
    }


    public void setMainImageId(Long mainImageId) {
        this.mainImageId = mainImageId;
    }


    public Long getSecondImageId() {
        return secondImageId;
    }


    public void setSecondImageId(Long secondImageId) {
        this.secondImageId = secondImageId;
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

}
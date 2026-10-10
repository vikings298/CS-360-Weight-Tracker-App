package com.example.mcnaneyprojectone.model;

/**
 * Carries one weight measurement, its owner, and its record ID.
 * Dates used by the current app are ISO calendar dates in yyyy-MM-dd format.
 */
public class WeightEntry {

    private int id;
    private int userId;
    private String date;
    private double weight;



    public WeightEntry(int id, int userId, String date, double weight){
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.weight = weight;
    }


    public int getId(){
        return id;
    }

    public int getUserId(){
        return userId;
    }

    public String getDate(){
        return date;
    }

    public double getWeight(){
        return weight;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }


}

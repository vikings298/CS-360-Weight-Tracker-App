package com.example.mcnaneyprojectone.model;



//Goal represents one goal record. It stores an id, the userId of who owns it, and their target weight.
public class Goal {

    private int id; //Database record Id
    private int userId; //User who owns it
    private double targetWeight; //Their target weight

    public Goal(int id, int userId, double targetWeight){

        this.id = id;
        this.userId = userId;
        this.targetWeight = targetWeight;

    }

    public int getId(){
        return id;
    }

    public int getUserId(){
        return userId;
    }

    public double getTargetWeight(){
        return targetWeight;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setUserId(int userId){
        this.userId = userId;
    }

    public void setTargetWeight(double targetWeight){
        this.targetWeight = targetWeight;
    }

}

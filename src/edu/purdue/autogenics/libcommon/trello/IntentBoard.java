package edu.purdue.autogenics.libcommon.trello;

import android.os.Parcel;
import android.os.Parcelable;

public class IntentBoard implements Parcelable {
	
	//Key
	public static final String KEY = "key_trello_board";
	
    private String id = null;
	private String name = null;
	private String owner = null;
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub		
		dest.writeString(id);
		dest.writeString(name);
	}

	public IntentBoard() {
		//Empty constructor
    }
	public IntentBoard(Parcel in){
		this.id = in .readString();
		this.name = in.readString();
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public IntentBoard createFromParcel(Parcel in)
        {
            return new IntentBoard(in);
        }
        
        public IntentBoard[] newArray(int size)
        {
            return new IntentBoard[size];
        }
    };
    
    public String getId(){
    	return id;
    }
    public String getName(){
    	return name;
    }
    public String getOwner(){
    	return owner; //Package name of owner
    }
    
    public void setId(String newId){
		id = newId;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setOwner(String newOwner){
		owner = newOwner;
	}
}

package edu.purdue.autogenics.libcommon.trello;

import android.os.Parcel;
import android.os.Parcelable;

public class IntentList implements Parcelable {
	
	//Key
	public static final String KEY = "key_trello_list";
	
    private String id = null;
	private String name = null;
	private String desc = null;
	private String boardId = null;
	private String owner = null;
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub		
		dest.writeString(id);
		dest.writeString(name);
		dest.writeString(desc);
		dest.writeString(boardId);
	}

	public IntentList() {
		//Empty constructor
    }
	public IntentList(Parcel in){
		this.id = in .readString();
		this.name = in.readString();
		this.desc = in.readString();
		this.boardId = in.readString();
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public IntentList createFromParcel(Parcel in)
        {
            return new IntentList(in);
        }
        
        public IntentList[] newArray(int size)
        {
            return new IntentList[size];
        }
    };
    
    public String getId(){
    	return id;
    }
    public String getName(){
    	return name;
    }
    public String getDesc(){
    	return desc;
    }
    public String getBoardId(){
    	return boardId;
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
    public void setDesc(String newDesc){
    	desc = newDesc;
	}
    public void setBoardId(String newBoardId){
    	boardId = newBoardId;
	}
    public void setOwner(String newOwner){
		owner = newOwner;
	}
}

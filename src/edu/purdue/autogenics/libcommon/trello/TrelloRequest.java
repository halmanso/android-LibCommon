package edu.purdue.autogenics.libcommon.trello;

import android.os.Parcel;
import android.os.Parcelable;

public class TrelloRequest implements Parcelable {
	//Key
	public static final String KEY = "key_trello_request";
	
	//Requests
	public static final String REQUEST_DATA = "req_data";
    public static final String REQUEST_PUSH = "req_push";
    public static final String REQUEST_UPDATE_ID = "req_update_id";
	
	//Types
    public static final String TYPE_BOARD = "type_board";
    public static final String TYPE_LIST = "type_list";
    public static final String TYPE_CARD = "type_card";
	
	private String request = null;
	private String type = null;
	private String id = null;
	private String new_id = null;
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(request);
		dest.writeString(type);
		dest.writeString(id);
		dest.writeString(new_id);
	}

	public TrelloRequest() {
		//Empty constructor
    }
	public TrelloRequest(Parcel in){
		this.request = in.readString();
		this.type = in.readString();
		this.id = in .readString();
		this.new_id = in .readString();
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public TrelloRequest createFromParcel(Parcel in)
        {
            return new TrelloRequest(in);
        }
        
        public TrelloRequest[] newArray(int size)
        {
            return new TrelloRequest[size];
        }
    };
    
    public String getRequest(){
    	return request;
    }
    public String getType(){
    	return type;
    }
    public String getId(){
    	return id;
    }
    public String getNewId(){
    	return new_id;
    }
    
    public void setRequest(String newRequest){
    	request = newRequest;
    }
    public void setType(String newType){
    	type = newType;
    }
    public void setId(String newId){
    	id = newId;
    }
    public void setNewId(String newId){
    	new_id = newId;
    }
}

package com.whosgonnapoop;

public class PlayerWidgetIndex {
String name;
int index;
int widgetID;
PlayerWidgetIndex(String name, int index, int widgetID){
    this.name = name;
    this.index = index;
    this.widgetID = widgetID;
}
public int getIndex(){
    return index;
}

public String getName() {return name;}
}
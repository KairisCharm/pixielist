package com.example.pixielist_test.test2;


import java.util.ArrayList;
import java.util.List;

public class RecyclerViewTestDataObject
{
    String mText;

    RecyclerViewTestDataObject(String text)
    {
        mText = text;
    }


    public String GetText()
    {
        return mText;
    }


    public static List<RecyclerViewTestDataObject> GetData()
    {
        List<RecyclerViewTestDataObject> result = new ArrayList<>();

        result.add(new RecyclerViewTestDataObject("Test 1"));
        result.add(new RecyclerViewTestDataObject("Test 2"));
        result.add(new RecyclerViewTestDataObject("Test 3"));

        return result;
    }
}

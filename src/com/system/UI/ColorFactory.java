package com.system.UI;

import java.awt.*;

public class ColorFactory{
    public static Color makeColor(int index){
        switch (index){
            case 1:return Color.blue;
            case 2:return Color.red;
            case 3:return Color.green;
        }
        return Color.black;
    }
}
